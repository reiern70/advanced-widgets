/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { createRef, StrictMode } from 'react';
import { cleanup, fireEvent, render, waitFor } from '@testing-library/react';
import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';

import { activate, ResizableColumns, Scrollbar } from '@antilia/core';

import { Tree, TreeGrid, type TreeHandle, type TreeNode } from '../src/index.js';
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
		const { container } = render(<Tree load={load} template="{{name}}"
			licenseKey="AW1.not.valid" texts={{ unlicensed: 'Needs a key' }} />);

		await waitFor(() => expect(container.querySelector('.antilia-unlicensed')?.textContent)
			.toBe('Needs a key'));
		expect(container.querySelector('[role="tree"]')).toBeNull();
	});

	it('renders the nodes load gives, and the children of a folder clicked', async () => {
		const onToggle = vi.fn();
		const { container } = render(<StrictMode><Tree load={load} template="{{name}}"
			label="Files" onToggle={onToggle} /></StrictMode>);

		await waitFor(() => expect(shown(container)).toEqual(['docs', 'notes.txt']));
		expect(container.querySelector('[role="tree"]')!.getAttribute('aria-label')).toBe('Files');
		expect(container.firstElementChild!.className).toBe('json-tree antilia-tree');

		fireEvent.click(item(container, 'docs'));
		await waitFor(() => expect(shown(container)).toEqual(['docs', 'readme.md', 'notes.txt']));
		expect(onToggle).toHaveBeenCalledWith('docs', true);
	});

	it('reports the leaf selected and follows the selected prop', async () => {
		const onSelect = vi.fn();
		const { container, rerender } = render(<Tree load={load} template="{{name}}"
			onSelect={onSelect} />);
		await waitFor(() => expect(shown(container).length).toBe(2));

		fireEvent.click(item(container, 'notes'));
		expect(onSelect).toHaveBeenCalledWith('notes', { name: 'notes.txt', size: '12 B' });
		expect(item(container, 'notes').getAttribute('aria-selected')).toBe('true');

		rerender(<Tree load={load} template="{{name}}" onSelect={onSelect} selected={null} />);
		expect(item(container, 'notes').getAttribute('aria-selected')).toBe('false');
		expect(onSelect).toHaveBeenCalledTimes(1);
	});

	it('selects a folder too with selectBranches, expanding it', async () => {
		const onSelect = vi.fn();
		const { container } = render(<Tree load={load} template="{{name}}" selectBranches
			onSelect={onSelect} />);
		await waitFor(() => expect(shown(container).length).toBe(2));

		fireEvent.click(item(container, 'docs'));
		expect(onSelect).toHaveBeenCalledWith('docs', { name: 'docs', size: '' });
		expect(item(container, 'docs').getAttribute('aria-selected')).toBe('true');
		await waitFor(() => expect(shown(container)).toEqual(['docs', 'readme.md', 'notes.txt']));
	});

	it('expands and repaints through its ref', async () => {
		const ref = createRef<TreeHandle>();
		const { container } = render(<Tree ref={ref} load={load} template="{{name}}" />);
		await waitFor(() => expect(shown(container).length).toBe(2));

		await ref.current!.expand('docs');
		ref.current!.renderNode('readme', { name: 'README.md' });

		expect(shown(container)).toEqual(['docs', 'README.md', 'notes.txt']);
	});
});

describe('TreeGrid', () => {
	it('renders a treegrid with the columns', async () => {
		const { container } = render(<TreeGrid load={load} template="{{name}}" header="Name"
			columns={[{ header: 'Size', template: '{{size}}', cssClass: 'size' }]}
			expanded={['docs']} />);

		await waitFor(() => expect(shown(container)).toEqual(['docs', 'readme.md', 'notes.txt']));
		expect(container.firstElementChild!.className).toBe('json-tree antilia-tree-grid');
		expect(Array.from(container.querySelectorAll('th'), (th) => th.textContent))
			.toEqual(['Name', 'Size']);
		expect(item(container, 'readme').querySelector('td.size')!.textContent).toBe('2 KB');
	});

	it('scrolls in the wrapper of its scroll bars, its columns resizable', async () => {
		const { container, unmount } = render(<StrictMode><TreeGrid load={load}
			template="{{name}}" header="Name" columns={[{ header: 'Size', template: '{{size}}' }]}
			height={{ value: 20, unit: 'em' }} resizableColumns="fit" /></StrictMode>);

		await waitFor(() => expect(shown(container)).toEqual(['docs', 'notes.txt']));
		const tree = container.querySelector<HTMLElement>('.json-tree')!;
		expect(tree.parentElement).toBe(container.firstElementChild);
		expect(tree.parentElement!.classList.contains('antilia-scrollbar')).toBe(true);
		expect(tree.style.height).toBe('20em');
		expect(Scrollbar.of(tree)?.options.header).toBe(':scope > table > thead');
		expect(ResizableColumns.instances[tree.id + '-grid']?.mode).toBe('fit');

		unmount();
		expect(Scrollbar.of(tree)).toBeUndefined();
		expect(ResizableColumns.instances[tree.id + '-grid']).toBeUndefined();
	});
});
