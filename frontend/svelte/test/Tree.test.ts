/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { cleanup, fireEvent, render, waitFor } from '@testing-library/svelte';
import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';

import { ResizableColumns, Scrollbar } from '@antilia/core';

import { activate, Tree, TreeGrid, type TreeNode } from '../src/index.js';
// the key of the demo application, activating every component
import demoKey from '../../../demo-app/src/main/resources/META-INF/antilia-widgets.license?raw';

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

beforeAll(async () => {
	await activate(demoKey);
});

afterEach(cleanup);

describe('Tree', () => {
	it('shows the placeholder of a private component without a license key', async () => {
		const { container } = render(Tree, { load, template: '{{name}}',
			licenseKey: 'AW1.not.valid', texts: { unlicensed: 'Needs a key' } });

		await waitFor(() => expect(container.querySelector('.antilia-unlicensed')?.textContent
			?.trim()).toBe('Needs a key'));
		expect(container.querySelector('[role="tree"]')).toBeNull();
	});

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
