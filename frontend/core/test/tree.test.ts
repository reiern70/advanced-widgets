/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { afterEach, describe, expect, it, vi } from 'vitest';

import { JsonTree, ResizableColumns, Scrollbar, setHost, TREE_ICONS, type TreeConfig,
	type TreeNode } from '../src/index.js';

const TEXTS = { loading: 'Loading…', empty: 'Nothing here' };

/** docs (src (a.ts, b.ts), readme.md), notes.txt */
const CHILDREN: Record<string, TreeNode[]> = {
	docs: [
		{ key: 'src', leaf: false, data: { name: 'src' } },
		{ key: 'readme', leaf: true, data: { name: 'readme.md' } }
	],
	src: [
		{ key: 'a', leaf: true, data: { name: 'a.ts' } },
		{ key: 'b', leaf: true, data: { name: 'b.ts' } }
	]
};

const ROOTS: TreeNode[] = [
	{ key: 'docs', leaf: false, data: { name: 'docs' } },
	{ key: 'notes', leaf: true, data: { name: 'notes.txt' } }
];

function config(overrides: Partial<TreeConfig> = {}): TreeConfig {
	document.body.innerHTML = '<div id="tree"></div>';
	return { id: 'tree', template: '{{name}}', texts: TEXTS, label: 'Files', ...overrides };
}

const load = vi.fn((parent: string | null) => Promise.resolve(parent === null ? ROOTS :
	CHILDREN[parent] ?? []));

function shown(): string[] {
	return Array.from(document.querySelectorAll('.antilia-tree-item'), (item) =>
		item.getAttribute('aria-level') + ':' +
		item.querySelector('.antilia-tree-content')!.textContent);
}

function item(key: string): HTMLElement {
	return document.querySelector<HTMLElement>(`[data-key="${key}"]`)!;
}

/**
 * @returns the markup as the DOM writes it back
 */
function markup(html: string): string {
	const element = document.createElement('span');
	element.innerHTML = html;
	return element.innerHTML;
}

function press(key: string): void {
	(document.activeElement as HTMLElement).dispatchEvent(new KeyboardEvent('keydown',
		{ key, bubbles: true }));
}

afterEach(() => {
	Object.values(JsonTree.instances).forEach((tree) => tree.destroy());
	setHost();
	load.mockClear();
	document.body.innerHTML = '';
});

describe('JsonTree', () => {
	it('renders the roots it is given as a tree of the template', () => {
		new JsonTree(config(), null, ROOTS);

		const tree = document.querySelector('[role="tree"]')!;
		expect(tree.getAttribute('aria-label')).toBe('Files');
		expect(shown()).toEqual(['1:docs', '1:notes.txt']);
		expect(item('docs').getAttribute('role')).toBe('treeitem');
		expect(item('docs').getAttribute('aria-expanded')).toBe('false');
		expect(item('docs').hasAttribute('aria-selected')).toBe(false);
		expect(item('notes').getAttribute('aria-selected')).toBe('false');
		expect(item('notes').hasAttribute('aria-expanded')).toBe(false);
		expect(item('docs').querySelector('.antilia-tree-icon')!.innerHTML)
			.toBe(markup(TREE_ICONS.folder));
		expect(item('notes').querySelector('.antilia-tree-icon')!.innerHTML)
			.toBe(markup(TREE_ICONS.leaf));
		expect(item('docs').tabIndex).toBe(0);
		expect(item('notes').tabIndex).toBe(-1);
	});

	it('loads the roots, and the children of a folder the first time it is expanded', async () => {
		const toggle = vi.fn();
		const tree = new JsonTree(config({ load }), { toggle });
		expect(document.querySelector('.antilia-tree-status')!.textContent).toBe('Loading…');
		await vi.waitFor(() => expect(shown()).toEqual(['1:docs', '1:notes.txt']));
		expect(document.querySelector<HTMLElement>('.antilia-tree-status')!.hidden).toBe(true);

		await tree.expand('docs');

		expect(shown()).toEqual(['1:docs', '2:src', '2:readme.md', '1:notes.txt']);
		expect(item('docs').getAttribute('aria-expanded')).toBe('true');
		expect(item('docs').querySelector('.antilia-tree-icon')!.innerHTML)
			.toBe(markup(TREE_ICONS.folderOpen));
		expect(item('docs').querySelector('[role="group"]')).not.toBeNull();
		expect(toggle).toHaveBeenCalledWith('docs', true);

		tree.collapse('docs');
		await tree.expand('docs');

		expect(load.mock.calls.map((call) => call[0])).toEqual([null, 'docs']);
		expect(shown()).toEqual(['1:docs', '2:src', '2:readme.md', '1:notes.txt']);
	});

	it('expands the folders it is told to, from the children sent along', () => {
		const roots = [{ ...ROOTS[0], children: CHILDREN.docs }, ROOTS[1]];
		new JsonTree(config({ expanded: ['docs'] }), null, roots);

		expect(shown()).toEqual(['1:docs', '2:src', '2:readme.md', '1:notes.txt']);
	});

	it('shows the rows of the children sent along in a tree grid', () => {
		const roots = [{ ...ROOTS[0], children: CHILDREN.docs }, ROOTS[1]];
		new JsonTree(config({ expanded: ['docs'], table: { header: 'Name', columns: [] } }), null,
			roots);

		expect(shown()).toEqual(['1:docs', '2:src', '2:readme.md', '1:notes.txt']);
	});

	it('selects a leaf on a click and toggles a folder', async () => {
		const select = vi.fn();
		new JsonTree(config({ load }), { select }, ROOTS);

		item('notes').querySelector<HTMLElement>('.antilia-tree-content')!.click();
		expect(select).toHaveBeenCalledWith('notes', { name: 'notes.txt' });
		expect(item('notes').getAttribute('aria-selected')).toBe('true');
		expect(item('notes').classList.contains('antilia-tree-item--selected')).toBe(true);

		item('docs').querySelector<HTMLElement>('.antilia-tree-toggle')!.click();
		await vi.waitFor(() => expect(shown().length).toBe(4));
		expect(select).toHaveBeenCalledTimes(1);

		item('readme').click();
		expect(item('notes').getAttribute('aria-selected')).toBe('false');
		expect(item('readme').getAttribute('aria-selected')).toBe('true');
	});

	it('sends the actions of the elements of a node', () => {
		const action = vi.fn();
		const select = vi.fn();
		new JsonTree(config({ template: '{{name}} <button data-dt-action="open">Open</button>' }),
			{ action, select }, ROOTS);

		item('notes').querySelector('button')!.click();

		expect(action).toHaveBeenCalledWith('open', 'notes', { name: 'notes.txt' });
		expect(select).not.toHaveBeenCalled();
	});

	it('moves with the arrow keys as a WAI-ARIA tree', async () => {
		const select = vi.fn();
		new JsonTree(config({ load }), { select }, ROOTS);
		item('docs').focus();

		press('ArrowRight');
		await vi.waitFor(() => expect(shown().length).toBe(4));
		press('ArrowRight');
		expect(document.activeElement).toBe(item('src'));
		press('ArrowDown');
		expect(document.activeElement).toBe(item('readme'));
		press('Enter');
		expect(select).toHaveBeenCalledWith('readme', { name: 'readme.md' });
		press('End');
		expect(document.activeElement).toBe(item('notes'));
		press('Home');
		press('ArrowDown');
		press('ArrowLeft');
		expect(document.activeElement).toBe(item('docs'));
		press('ArrowLeft');
		expect(shown()).toEqual(['1:docs', '1:notes.txt']);
		expect(item('docs').tabIndex).toBe(0);
	});

	it('renders a tree grid: a treegrid whose rows follow their parents', async () => {
		const tree = new JsonTree(config({ load, table: { header: 'Name', columns: [
			{ header: 'Size', template: '{{size}}', cssClass: 'size' }] } }), null,
		[{ key: 'docs', leaf: false, data: { name: 'docs', size: '–' } },
			{ key: 'notes', leaf: true, data: { name: 'notes.txt', size: '12 B' } }]);

		const grid = document.querySelector('table[role="treegrid"]')!;
		expect(Array.from(grid.querySelectorAll('th'), (th) => th.textContent))
			.toEqual(['Name', 'Size']);
		expect(item('notes').tagName).toBe('TR');
		expect(item('notes').querySelector('td.size')!.textContent).toBe('12 B');

		await tree.expand('docs');
		await tree.expand('src');
		expect(shown()).toEqual(['1:docs', '2:src', '3:a.ts', '3:b.ts', '2:readme.md',
			'1:notes.txt']);
		expect(item('a').querySelector<HTMLElement>('.antilia-tree-row')!.style
			.getPropertyValue('--antilia-tree-level')).toBe('3');

		item('src').focus();
		tree.collapse('docs');
		expect(shown()).toEqual(['1:docs', '1:notes.txt']);
		expect(document.activeElement).toBe(item('docs'));

		await tree.expand('docs');
		expect(shown().length).toBe(6);
	});

	it('reloads keeping the folders expanded and the leaf selected', async () => {
		const tree = new JsonTree(config({ load, selected: 'a' }), null, ROOTS);
		await tree.expand('docs');
		await tree.expand('src');

		await tree.reload();

		expect(shown()).toEqual(['1:docs', '2:src', '3:a.ts', '3:b.ts', '2:readme.md',
			'1:notes.txt']);
		expect(item('a').getAttribute('aria-selected')).toBe('true');
	});

	it('repaints a node and says when there are none', () => {
		const tree = new JsonTree(config(), null, ROOTS);
		tree.renderNode('notes', { name: 'todo.txt' });
		expect(shown()).toEqual(['1:docs', '1:todo.txt']);

		new JsonTree(config(), null, []);
		expect(document.querySelector('.antilia-tree-status')!.textContent).toBe('Nothing here');
	});

	it('takes icons of the application and of a node', () => {
		new JsonTree(config({ icons: { leaf: '<i>leaf</i>' } }), null,
			[...ROOTS, { key: 'img', leaf: true, data: { name: 'logo.svg' }, icon: '<b>img</b>' }]);

		expect(item('notes').querySelector('.antilia-tree-icon')!.innerHTML).toBe('<i>leaf</i>');
		expect(item('img').querySelector('.antilia-tree-icon')!.innerHTML).toBe('<b>img</b>');
	});

	it('loads from its URL, the children by ?parent=', async () => {
		const fetch = vi.spyOn(window, 'fetch').mockImplementation((url) => Promise.resolve(
			new Response(JSON.stringify({ nodes: String(url).includes('parent=') ?
				CHILDREN.docs : ROOTS }))));
		const tree = new JsonTree(config({ dataUrl: '/tree?x=1' }));
		await vi.waitFor(() => expect(shown().length).toBe(2));
		await tree.expand('docs');

		expect(fetch.mock.calls.map((call) => call[0])).toEqual(['/tree?x=1',
			'/tree?x=1&parent=docs']);
		fetch.mockRestore();
	});

	it('scrolls itself with the scroll bars of the core when it has a height', () => {
		const tree = new JsonTree(config({ height: { value: 20, unit: 'em' } }), null, ROOTS);
		const element = document.getElementById('tree')!;

		expect(element.style.height).toBe('20em');
		expect(element.classList.contains('antilia-tree--scrolling')).toBe(true);
		expect(Scrollbar.of(element)?.options).toEqual({});
		expect(element.parentElement!.classList.contains('antilia-scrollbar')).toBe(true);

		tree.destroy();
		expect(Scrollbar.of(element)).toBeUndefined();
		expect(element.style.height).toBe('');
		expect(element.classList.contains('antilia-tree--scrolling')).toBe(false);
	});

	it('keeps the head of a tree grid of a height above its scroll bar', () => {
		new JsonTree(config({ height: { value: 300, unit: 'px' },
			table: { header: 'Name', columns: [] } }), null, ROOTS);

		expect(Scrollbar.of('tree')?.options.header).toBe(':scope > table > thead');
	});

	it('makes the columns of a tree grid resizable once its rows are shown', () => {
		const tree = new JsonTree(config({ resizableColumns: 'fit',
			table: { header: 'Name', columns: [{ header: 'Size', template: '{{size}}' }] } }),
		null, ROOTS);

		const resizable = ResizableColumns.instances['tree-grid'];
		expect(resizable.table).toBe(document.querySelector('table.antilia-tree-grid-grid'));
		expect(resizable.mode).toBe('fit');
		expect(document.querySelectorAll('#tree-grid th .antilia-col-resizer').length).toBe(1);

		tree.destroy();
		expect(ResizableColumns.instances['tree-grid']).toBeUndefined();
	});
});
