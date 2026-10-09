/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * Lets the user move the columns of a table whose header cells carry data-movable, by dragging the
 * handle in their header with the mouse, a pen or a finger, or with the arrow keys on that handle.
 * While dragging, a marker shows where the column will land; positions that would move a column
 * that is not movable show none. The move itself is left to whoever renders the table: onMove
 * reports it, and the table is rendered again in the new order (by the server in Wicket, by the
 * framework's state in React and Svelte).
 */

import { getHost, type Host, type Unsubscribe } from './host.js';

/**
 * How the columns of a table can be moved.
 */
export interface MovableColumnsOptions {
	/** The indexes of the movable columns, marking their header cells with data-movable. */
	movable?: number[];
	/** The label of a handle, {0} standing for the column's header. */
	label?: string;
	/**
	 * Called with the index of a moved column and the index of the column it goes in front of, or
	 * the number of columns for the end.
	 */
	onMove?: (from: number, to: number) => void;
}

interface Move {
	from: number;
	header: HTMLTableCellElement;
	startX: number;
	startY: number;
	started: boolean;
	to: number;
	ghost?: HTMLDivElement;
	marker?: HTMLDivElement;
	pointerMove(event: PointerEvent): void;
	pointerUp(event: PointerEvent): void;
	cancel(): void;
	key(event: KeyboardEvent): void;
}

const HANDLE_CLASS = 'antilia-col-mover';
const DRAGGING_CLASS = 'antilia-col-dragging';
const NO_DROP_CLASS = 'antilia-col-no-drop';
const MOVING_CLASS = 'antilia-col-moving';
const GHOST_CLASS = 'antilia-col-move-ghost';
const MARKER_CLASS = 'antilia-col-drop-marker';

/** How far, in pixels, the pointer has to travel on a column's handle before it drags the column. */
const DRAG_THRESHOLD = 4;

const SVG = 'http://www.w3.org/2000/svg';

function grip(): SVGSVGElement {
	const svg = document.createElementNS(SVG, 'svg');
	svg.setAttribute('aria-hidden', 'true');
	svg.setAttribute('focusable', 'false');
	svg.setAttribute('viewBox', '0 0 10 16');
	svg.setAttribute('width', '8');
	svg.setAttribute('height', '13');
	const path = document.createElementNS(SVG, 'path');
	path.setAttribute('fill', 'currentColor');
	path.setAttribute('d', 'M2 1h2v2H2zm4 0h2v2H6zM2 7h2v2H2zm4 0h2v2H6zm-4 6h2v2H2zm4 0h2v2H6z');
	svg.appendChild(path);
	return svg;
}

/**
 * The header cells of the header row of a table: the last row of its head holding th cells,
 * except those carrying data-resizable-skip, which are no columns of their own.
 *
 * @param table the table
 * @returns the header cells
 */
export function headerCells(table: HTMLTableElement): HTMLTableCellElement[] {
	const rows = table.tHead ? table.tHead.rows : [];
	for (let i = rows.length - 1; i >= 0; i--) {
		const cells = Array.from(rows[i].cells).filter((cell) => cell.tagName === 'TH');
		if (cells.length > 0) {
			return cells.filter((cell) => !cell.hasAttribute('data-resizable-skip'));
		}
	}
	return [];
}

/**
 * The movable columns of a table.
 */
export class MovableColumns {
	/** The tables whose columns can be moved, by id. */
	static readonly instances: Record<string, MovableColumns> = {};

	/** Where to put the focus after the table was rendered again for a move. */
	static focusAfterMove: { id: string; index: number } | undefined = undefined;

	private static stopWatchingAdditions: Unsubscribe | null = null;

	/** The host whose additions are watched, which changes when another one is set. */
	private static watchedHost: Host | null = null;

	readonly table: HTMLTableElement;

	readonly id: string;

	onMove: MovableColumnsOptions['onMove'];

	label: string;

	private moving: Move | undefined = undefined;

	private readonly onPointerDown = (event: PointerEvent) => this.pointerDown(event);

	private readonly onKeyDown = (event: KeyboardEvent) => this.keyDown(event);

	/**
	 * @param table the table
	 * @param options how the columns can be moved
	 */
	constructor(table: HTMLTableElement, options: MovableColumnsOptions = {}) {
		this.table = table;
		this.id = table.id;
		this.onMove = options.onMove;
		this.label = options.label || 'Move {0}';
		if (Array.isArray(options.movable)) {
			const cells = this.headerCells();
			options.movable.forEach((index) => cells[index]?.setAttribute('data-movable', 'true'));
		}
		this.ensureHandles();

		this.table.addEventListener('pointerdown', this.onPointerDown);
		this.table.addEventListener('keydown', this.onKeyDown);

		const focus = MovableColumns.focusAfterMove;
		if (focus && focus.id === this.id) {
			MovableColumns.focusAfterMove = undefined;
			this.headerCells()[focus.index]?.querySelector<HTMLElement>('.' + HANDLE_CLASS)?.focus();
		}
	}

	/**
	 * Lets the user move the columns of a table, replacing an earlier attachment to it.
	 *
	 * @param table the table or its id
	 * @param options how the columns can be moved
	 * @returns the instance, or undefined if there is no such table
	 */
	static attach(table: HTMLTableElement | string, options?: MovableColumnsOptions):
		MovableColumns | undefined {
		const element = typeof table === 'string' ? document.getElementById(table) : table;
		if (!(element instanceof HTMLTableElement)) {
			return undefined;
		}
		MovableColumns.detach(element.id);
		const instance = new MovableColumns(element, options);
		MovableColumns.instances[instance.id] = instance;
		MovableColumns.watchAdditions();
		return instance;
	}

	/**
	 * Stops moving the columns of a table.
	 *
	 * @param id the id of the table
	 */
	static detach(id: string): void {
		const instance = MovableColumns.instances[id];
		if (instance) {
			instance.destroy();
			delete MovableColumns.instances[id];
		}
	}

	/**
	 * @returns whether moving the column at from in front of the one at to, or to the end for the
	 *          number of columns, moves a movable column and leaves every other column in its place
	 */
	static isValidMove(movable: boolean[], from: number, to: number): boolean {
		if (from < 0 || from >= movable.length || to < 0 || to > movable.length ||
			to === from || to === from + 1 || !movable[from]) {
			return false;
		}
		const order = movable.map((_value, index) => index);
		order.splice(to > from ? to - 1 : to, 0, order.splice(from, 1)[0]);
		return order.every((column, index) => movable[column] || column === index);
	}

	/**
	 * Puts handles into the header cells an update of the page added to a table, and forgets the
	 * tables no longer in the page.
	 */
	private static watchAdditions(): void {
		const host = getHost();
		if (MovableColumns.watchedHost === host) {
			return;
		}
		MovableColumns.stopWatchingAdditions?.();
		MovableColumns.watchedHost = host;
		MovableColumns.stopWatchingAdditions = host.onNodeAdded((element) => {
			Object.keys(MovableColumns.instances).forEach((id) => {
				const instance = MovableColumns.instances[id];
				if (!document.body.contains(instance.table)) {
					MovableColumns.detach(id);
				} else if (element && instance.table.contains(element)) {
					instance.ensureHandles();
				}
			});
		});
	}

	headerCells(): HTMLTableCellElement[] {
		return headerCells(this.table);
	}

	movable(): boolean[] {
		return this.headerCells().map((cell) => cell.getAttribute('data-movable') === 'true');
	}

	/**
	 * Puts a handle into every movable header cell lacking one, at its start.
	 */
	ensureHandles(): void {
		this.headerCells().forEach((cell) => {
			if (cell.getAttribute('data-movable') !== 'true' ||
				cell.querySelector('.' + HANDLE_CLASS)) {
				return;
			}
			const handle = document.createElement('button');
			handle.type = 'button';
			handle.className = HANDLE_CLASS;
			const label = this.label.replace('{0}', (cell.textContent ?? '').trim());
			handle.title = label;
			handle.setAttribute('aria-label', label);
			handle.appendChild(grip());
			cell.insertBefore(handle, cell.firstChild);
		});
	}

	private pointerDown(event: PointerEvent): void {
		const handle = (event.target as Element | null)?.closest?.<HTMLElement>('.' + HANDLE_CLASS);
		const header = handle?.closest('th') ?? null;
		const from = header ? this.headerCells().indexOf(header) : -1;
		if (!handle || !header || from < 0 || (event.button !== undefined && event.button !== 0)) {
			return;
		}
		event.preventDefault();
		handle.focus();
		this.endMove(false);
		const move: Move = {
			from,
			header,
			startX: event.clientX,
			startY: event.clientY,
			started: false,
			to: -1,
			pointerMove: (e) => this.dragMove(e.clientX, e.clientY),
			pointerUp: (e) => {
				this.dragMove(e.clientX, e.clientY);
				this.endMove(true);
			},
			cancel: () => this.endMove(false),
			key: (e) => {
				if (e.key === 'Escape') {
					e.preventDefault();
					this.endMove(false);
				}
			}
		};
		document.addEventListener('pointermove', move.pointerMove);
		document.addEventListener('pointerup', move.pointerUp);
		document.addEventListener('pointercancel', move.cancel);
		document.addEventListener('keydown', move.key);
		window.addEventListener('blur', move.cancel);
		this.moving = move;
	}

	/**
	 * Follows the pointer while a column is dragged: starts the drag once the pointer left the
	 * handle's threshold, moves the ghost and places the drop marker.
	 */
	dragMove(clientX: number, clientY: number): void {
		const move = this.moving;
		if (!move) {
			return;
		}
		if (!move.started) {
			if (Math.abs(clientX - move.startX) < DRAG_THRESHOLD &&
				Math.abs(clientY - move.startY) < DRAG_THRESHOLD) {
				return;
			}
			this.startMove(move);
		}
		move.ghost!.style.left = (clientX + window.scrollX + 12) + 'px';
		move.ghost!.style.top = (clientY + window.scrollY + 12) + 'px';
		const to = this.dropPosition(move.from, clientX);
		move.to = to;
		this.table.classList.toggle(NO_DROP_CLASS, to < 0);
		this.placeMarker(move.marker!, to);
	}

	private startMove(move: Move): void {
		move.started = true;
		this.table.classList.add(DRAGGING_CLASS);
		this.columnCells(move.from).forEach((cell) => cell.classList.add(MOVING_CLASS));
		move.ghost = document.createElement('div');
		move.ghost.className = GHOST_CLASS;
		move.ghost.textContent = (move.header.textContent ?? '').trim();
		move.marker = document.createElement('div');
		move.marker.className = MARKER_CLASS;
		move.marker.style.display = 'none';
		document.body.appendChild(move.ghost);
		document.body.appendChild(move.marker);
		this.copyTheme(move.ghost);
		this.copyTheme(move.marker);
	}

	/**
	 * Copies the theme's colors to an element appended to the body, outside of the table.
	 */
	private copyTheme(element: HTMLElement): void {
		const style = window.getComputedStyle(this.table);
		['--antilia-theme-primary', '--antilia-theme-on-primary', '--antilia-theme-accent']
			.forEach((name) => {
				const value = style.getPropertyValue(name);
				if (value) {
					element.style.setProperty(name, value.trim());
				}
			});
	}

	/**
	 * @returns the cells of the column at index: its header cell and the cells of the rows, also of
	 *          rows in a table nested in the body
	 */
	columnCells(index: number): HTMLTableCellElement[] {
		const cells: Array<HTMLTableCellElement | undefined> = [this.headerCells()[index]];
		this.table.querySelectorAll<HTMLTableRowElement>('tbody > tr').forEach((tr) => {
			if (tr.cells.length > 1 && tr.cells[index]) {
				cells.push(tr.cells[index]);
			}
		});
		return cells.filter((cell): cell is HTMLTableCellElement => Boolean(cell));
	}

	/**
	 * @returns the index of the column a column dragged from the index from goes in front of when
	 *          dropped with the pointer at clientX, the number of columns for the end, or -1 where it
	 *          may not go
	 */
	dropPosition(from: number, clientX: number): number {
		const headers = this.headerCells();
		let to = headers.length;
		for (let i = 0; i < headers.length; i++) {
			const rect = headers[i].getBoundingClientRect();
			if (clientX < rect.left + rect.width / 2) {
				to = i;
				break;
			}
		}
		return MovableColumns.isValidMove(this.movable(), from, to) ? to : -1;
	}

	/**
	 * Shows the drop marker on the left edge of the column at index, on the right edge of the last
	 * column for the end, or hides it for -1. It reaches down to the end of the bodies.
	 */
	private placeMarker(marker: HTMLDivElement, index: number): void {
		const headers = this.headerCells();
		if (index < 0 || headers.length === 0) {
			marker.style.display = 'none';
			return;
		}
		const end = index >= headers.length;
		const rect = headers[end ? headers.length - 1 : index].getBoundingClientRect();
		const bodies = this.table.tBodies;
		const bottom = bodies.length > 0 ?
			bodies[bodies.length - 1].getBoundingClientRect().bottom : rect.bottom;
		marker.style.display = '';
		marker.style.left = ((end ? rect.right : rect.left) + window.scrollX) + 'px';
		marker.style.top = (rect.top + window.scrollY) + 'px';
		marker.style.height = Math.max(0, bottom - rect.top) + 'px';
	}

	/**
	 * Ends dragging a column, moving it to where the marker was if drop is true.
	 */
	endMove(drop: boolean): void {
		const move = this.moving;
		if (!move) {
			return;
		}
		this.moving = undefined;
		document.removeEventListener('pointermove', move.pointerMove);
		document.removeEventListener('pointerup', move.pointerUp);
		document.removeEventListener('pointercancel', move.cancel);
		document.removeEventListener('keydown', move.key);
		window.removeEventListener('blur', move.cancel);
		if (!move.started) {
			return;
		}
		move.ghost?.remove();
		move.marker?.remove();
		this.table.classList.remove(DRAGGING_CLASS, NO_DROP_CLASS);
		this.table.querySelectorAll('.' + MOVING_CLASS).forEach((cell) => {
			cell.classList.remove(MOVING_CLASS);
		});
		if (drop && move.to >= 0) {
			this.move(move.from, move.to);
		}
	}

	/**
	 * Moves the column whose handle has the focus one place to the left or right with the arrow
	 * keys, past a movable neighbour.
	 */
	private keyDown(event: KeyboardEvent): void {
		const target = event.target as Element | null;
		if ((event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') ||
			!target?.matches?.('.' + HANDLE_CLASS)) {
			return;
		}
		event.preventDefault();
		const header = target.closest('th');
		const from = header ? this.headerCells().indexOf(header) : -1;
		const to = event.key === 'ArrowLeft' ? from - 1 : from + 2;
		if (from >= 0 && MovableColumns.isValidMove(this.movable(), from, to)) {
			this.move(from, to);
		}
	}

	/**
	 * Reports a move to onMove(from, to), keeping the focus on the moved column's handle once the
	 * table is rendered again.
	 */
	move(from: number, to: number): void {
		MovableColumns.focusAfterMove = { id: this.id, index: to > from ? to - 1 : to };
		this.onMove?.(from, to);
	}

	/**
	 * Stops listening; the handles stay.
	 */
	destroy(): void {
		this.endMove(false);
		this.table.removeEventListener('pointerdown', this.onPointerDown);
		this.table.removeEventListener('keydown', this.onKeyDown);
	}
}
