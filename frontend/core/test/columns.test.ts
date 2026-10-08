/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { afterEach, describe, expect, it, vi } from 'vitest';

import { MovableColumns, ResizableColumns, setHost, standaloneHost } from '../src/index.js';

function table(headers: string[], attributes: string[] = []): HTMLTableElement {
	document.body.innerHTML = '<table id="t"><thead><tr>' +
		headers.map((header, i) => `<th ${attributes[i] ?? ''}>${header}</th>`).join('') +
		'</tr></thead><tbody><tr>' + headers.map((header) => `<td>${header}1</td>`).join('') +
		'</tr></tbody></table>';
	return document.getElementById('t') as HTMLTableElement;
}

function key(target: Element, name: string): void {
	target.dispatchEvent(new KeyboardEvent('keydown', { key: name, bubbles: true }));
}

afterEach(() => {
	Object.keys(MovableColumns.instances).forEach(MovableColumns.detach);
	Object.keys(ResizableColumns.instances).forEach(ResizableColumns.detach);
	MovableColumns.focusAfterMove = undefined;
	vi.restoreAllMocks();
	setHost();
	document.body.innerHTML = '';
});

describe('MovableColumns', () => {
	it('allows only moves that leave the fixed columns in their places', () => {
		const movable = [false, true, true, false];

		expect(MovableColumns.isValidMove(movable, 1, 3)).toBe(true);
		expect(MovableColumns.isValidMove(movable, 2, 1)).toBe(true);
		expect(MovableColumns.isValidMove(movable, 1, 0)).toBe(false);
		expect(MovableColumns.isValidMove(movable, 1, 4)).toBe(false);
		expect(MovableColumns.isValidMove(movable, 0, 2)).toBe(false);
		expect(MovableColumns.isValidMove(movable, 1, 2)).toBe(false);
	});

	it('puts a labelled handle into the movable header cells', () => {
		table(['Name', 'City', 'Born']);
		MovableColumns.attach('t', { movable: [0, 2], label: 'Move {0}' });

		const handles = Array.from(document.querySelectorAll('.wicket-col-mover'));
		expect(handles.map((handle) => handle.getAttribute('aria-label')))
			.toEqual(['Move Name', 'Move Born']);
		expect(handles[0].querySelector('svg')).not.toBeNull();
	});

	it('reports a move with the arrow keys and keeps the focus for the new table', () => {
		const element = table(['Name', 'City', 'Born'], ['data-movable="true"',
			'data-movable="true"', 'data-movable="true"']);
		const onMove = vi.fn();
		MovableColumns.attach(element, { onMove });

		key(document.querySelectorAll('.wicket-col-mover')[0], 'ArrowRight');
		expect(onMove).toHaveBeenCalledWith(0, 2);
		expect(MovableColumns.focusAfterMove).toEqual({ id: 't', index: 1 });

		// the table rendered again in the new order, attached again
		table(['City', 'Name', 'Born'], ['data-movable="true"', 'data-movable="true"',
			'data-movable="true"']);
		MovableColumns.attach('t', { onMove });
		expect(document.activeElement).toBe(document.querySelectorAll('.wicket-col-mover')[1]);
		expect(MovableColumns.focusAfterMove).toBeUndefined();

		key(document.querySelectorAll('.wicket-col-mover')[0], 'ArrowLeft');
		expect(onMove).toHaveBeenCalledTimes(1);
	});

	it('adds handles to header cells the host says were added', () => {
		let added: ((element: Element) => void) | undefined;
		setHost({ ...standaloneHost, onNodeAdded: (listener) => ((added = listener), () => {}) });
		const element = table(['Name'], ['data-movable="true"']);
		MovableColumns.attach(element);
		element.tHead!.rows[0].innerHTML = '<th data-movable="true">Name</th>';

		added!(element.tHead!.rows[0]);

		expect(document.querySelectorAll('.wicket-col-mover').length).toBe(1);
	});
});

describe('ResizableColumns', () => {
	it('takes the width of a column from its neighbour in fit mode, down to the minimum', () => {
		expect(ResizableColumns.resize('fit', [100, 100, 100], 0, 1, 30, 30)).toEqual([130, 70, 100]);
		expect(ResizableColumns.resize('fit', [100, 100, 100], 0, 1, 500, 30)).toEqual([170, 30, 100]);
		expect(ResizableColumns.resize('fit', [100, 100], 1, -1, 20, 30)).toEqual([100, 100]);
		expect(ResizableColumns.resize('stretch', [100, 100], 1, -1, 500, 30)).toEqual([100, 600]);
		expect(ResizableColumns.resize('stretch', [100, 100], 0, -1, -500, 30)).toEqual([30, 100]);
	});

	it('puts a handle on every column that can give or take width', () => {
		table(['Name', 'City', 'Born'], ['', 'data-resizable="false"', '']);
		ResizableColumns.attach('t', { mode: 'fit' });

		const handles = Array.from(document.querySelectorAll('.wicket-col-resizer'));
		expect(handles.map((handle) => [handle.getAttribute('data-index'),
			handle.getAttribute('data-neighbour')])).toEqual([['0', '2']]);
		expect(document.querySelectorAll('colgroup.wicket-resizable-colgroup col').length).toBe(3);
	});

	it('lays given widths out as shares in fit mode and in pixels in stretch mode', () => {
		const element = table(['A', 'B']);
		ResizableColumns.attach(element, { mode: 'fit', widths: [100, 300] });
		const cols = () => Array.from(element.querySelectorAll('col'), (col) => col.style.width);
		expect(cols()).toEqual(['25%', '75%']);
		expect(element.style.width).toBe('100%');

		ResizableColumns.attach(element, { mode: 'stretch', widths: [100, 300] });
		expect(cols()).toEqual(['100px', '300px']);
		expect(element.style.width).toBe('400px');
	});

	it('resizes with the arrow keys and reports the widths', () => {
		table(['A', 'B']);
		vi.spyOn(ResizableColumns, 'measure').mockReturnValue(100);
		const onResize = vi.fn();
		ResizableColumns.attach('t', { mode: 'fit', step: 10, onResize });

		key(document.querySelector('.wicket-col-resizer')!, 'ArrowRight');

		expect(onResize).toHaveBeenCalledWith([110, 90]);
		expect(ResizableColumns.instances.t.sized).toBe(true);
	});

	it('keeps a column at least 80 pixels wide by default', () => {
		table(['A', 'B']);
		vi.spyOn(ResizableColumns, 'measure').mockReturnValue(100);
		const onResize = vi.fn();
		ResizableColumns.attach('t', { mode: 'fit', step: 50, onResize });

		key(document.querySelector('.wicket-col-resizer')!, 'ArrowRight');

		expect(onResize).toHaveBeenCalledWith([120, 80]);
	});

	it('takes the options a table gave for itself', () => {
		ResizableColumns.tableOptions.t = { mode: 'stretch', proportions: [1, 3] };
		const element = table(['A', 'B']);
		ResizableColumns.attach('t', {});
		delete ResizableColumns.tableOptions.t;

		expect(ResizableColumns.instances.t.mode).toBe('stretch');
		expect(ResizableColumns.instances.t.proportions).toEqual([1, 3]);
		expect(element.classList.contains('wicket-resizable-columns-stretch')).toBe(true);
	});

	it('takes the handles away when detached', () => {
		table(['A', 'B']);
		ResizableColumns.attach('t');
		ResizableColumns.detach('t');

		expect(document.querySelectorAll('.wicket-col-resizer').length).toBe(0);
		expect(ResizableColumns.instances.t).toBeUndefined();
	});
});
