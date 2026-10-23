/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { cleanup, fireEvent, render, waitFor } from '@testing-library/svelte';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { ResizableColumns, Scrollbar } from '@antilia/core';

import { Tree, TreeGrid, type TreeNode } from '../src/index.js';

const NODES: Record<string, TreeNode[]> = {
	'': [
		{ key: 'docs', leaf: false, data: { name: 'docs', size: '' } },
		{ key: 'notes', leaf: true, data: { name: 'notes.txt', size: '12 B' } }
	],
	docs: [{ key: 'readme', leaf: true, data: { name: 'readme.md', size: '2 KB' } }]
};

const load = (parent: string | null) => Promise.resolve(NODES[parent ?? ''] ?? []);

function shown(container: HTMLElement): string[] {
	return Array.from(container.querySelectorAll('.antilia-tree-content'),
		(content) => content.textContent ?? '');
}

function item(container: HTMLElement, key: string): HTMLElement {
	return container.querySelector<HTMLElement>(`[data-key="${key}"]`)!;
}

afterEach(cleanup);

describe('Tree', () => {
	it('renders the nodes, expands a folder and reports the leaf selected', async () => {
		const onselect = vi.fn();
		const ontoggle = vi.fn();
		const { container } = render(Tree, { load, template: '{{name}}', label: 'Files',
			onselect, ontoggle });

		await waitFor(() => expect(shown(container)).toEqual(['docs', 'notes.txt']));
		expect(container.querySelector('[role="tree"]')!.getAttribute('aria-label')).toBe('Files');

		await fireEvent.click(item(container, 'docs'));
		await waitFor(() => expect(shown(container)).toEqual(['docs', 'readme.md', 'notes.txt']));
		expect(ontoggle).toHaveBeenCalledWith('docs', true);

		await fireEvent.click(item(container, 'readme'));
		expect(onselect).toHaveBeenCalledWith('readme', { name: 'readme.md', size: '2 KB' });
		expect(item(container, 'readme').getAttribute('aria-selected')).toBe('true');
	});

	it('selects a folder too with selectBranches, expanding it', async () => {
		const onselect = vi.fn();
		const { container } = render(Tree, { load, template: '{{name}}', selectBranches: true,
			onselect });
		await waitFor(() => expect(shown(container).length).toBe(2));

		await fireEvent.click(item(container, 'docs'));
		expect(onselect).toHaveBeenCalledWith('docs', { name: 'docs', size: '' });
		expect(item(container, 'docs').getAttribute('aria-selected')).toBe('true');
		await waitFor(() => expect(shown(container)).toEqual(['docs', 'readme.md', 'notes.txt']));
	});

	it('follows the selected prop', async () => {
		const { container, rerender } = render(Tree, { load, template: '{{name}}',
			selected: 'notes' });
		await waitFor(() => expect(item(container, 'notes')?.getAttribute('aria-selected'))
			.toBe('true'));

		await rerender({ selected: null });
		expect(item(container, 'notes').getAttribute('aria-selected')).toBe('false');
	});
});

describe('TreeGrid', () => {
	it('renders a treegrid with the columns', async () => {
		const { container } = render(TreeGrid, { load, template: '{{name}}', header: 'Name',
			columns: [{ header: 'Size', template: '{{size}}', cssClass: 'size' }],
			expanded: ['docs'] });

		await waitFor(() => expect(shown(container)).toEqual(['docs', 'readme.md', 'notes.txt']));
		expect(container.querySelector('.json-tree')!.classList.contains('antilia-tree-grid'))
			.toBe(true);
		expect(Array.from(container.querySelectorAll('th'), (th) => th.textContent))
			.toEqual(['Name', 'Size']);
		expect(item(container, 'readme').querySelector('td.size')!.textContent).toBe('2 KB');
	});

	it('scrolls in the wrapper of its scroll bars, its columns resizable', async () => {
		const { container, unmount } = render(TreeGrid, { load, template: '{{name}}',
			header: 'Name', columns: [{ header: 'Size', template: '{{size}}' }],
			height: { value: 20, unit: 'em' }, resizableColumns: 'fit' });

		await waitFor(() => expect(shown(container)).toEqual(['docs', 'notes.txt']));
		const tree = container.querySelector<HTMLElement>('.json-tree')!;
		expect(tree.parentElement!.classList.contains('antilia-scrollbar')).toBe(true);
		expect(tree.style.height).toBe('20em');
		expect(Scrollbar.of(tree)?.options.header).toBe(':scope > table > thead');
		expect(ResizableColumns.instances[tree.id + '-grid']?.mode).toBe('fit');

		unmount();
		expect(Scrollbar.of(tree)).toBeUndefined();
		expect(ResizableColumns.instances[tree.id + '-grid']).toBeUndefined();
	});
});
