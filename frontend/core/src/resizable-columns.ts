/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * Lets the columns of a table be resized by dragging the right edge of their header cells, or with
 * the arrow keys on that edge.
 *
 * In 'fit' mode the table keeps its width and a column takes its width from the next resizable one;
 * in 'stretch' mode a column grows or shrinks on its own and the table follows. While dragging, a
 * vertical guide with arrows shows where the edge will land, a band the extent of the column and a
 * label its new width; the width is applied on release.
 */

import { getHost, type Host, type Unsubscribe } from './host.js';

export type ResizeMode = 'fit' | 'stretch';

/**
 * How the columns of a table are resized.
 */
export interface ResizableColumnsOptions {
	/** 'fit' keeps the width of the table, 'stretch' lets it follow its columns; 'fit' by default. */
	mode?: ResizeMode;
	/** The narrowest a column gets, in pixels; 80 by default. */
	minWidth?: number;
	/** The pixels an arrow key resizes by; 10 by default. */
	step?: number;
	/** Saved widths, in pixels. */
	widths?: number[];
	/** Relative widths of the columns. */
	proportions?: number[];
	/** Whether the table takes the width of its parent. */
	adjustToParent?: boolean;
	/** Called with the widths after the user resized a column. */
	onResize?: (widths: number[]) => void;
	/**
	 * The widths the columns of the given header cells take by their content, where the header
	 * cells alone do not tell.
	 */
	measure?: (cells: HTMLTableCellElement[]) => number[];
}

interface Drag {
	index: number;
	neighbour: number;
	startX: number;
	widths: number[];
	edge: number;
	delta: number;
	guide: HTMLDivElement;
	band: HTMLDivElement;
	label: HTMLSpanElement;
	move(event: PointerEvent): void;
	up(event: PointerEvent): void;
	cancel(): void;
	key(event: KeyboardEvent): void;
}

const HANDLE_CLASS = 'wicket-col-resizer';
const GUIDE_CLASS = 'wicket-col-resize-guide';
const BAND_CLASS = 'wicket-col-resize-band';
const LABEL_CLASS = 'wicket-col-resize-label';
const RESIZING_CLASS = 'wicket-resizing';
const COLGROUP_CLASS = 'wicket-resizable-colgroup';

function sum(widths: number[]): number {
	return widths.reduce((total, width) => total + width, 0);
}

function positiveWidths(widths: number[] | undefined, count: number): widths is number[] {
	return Array.isArray(widths) && widths.length === count && widths.every((width) => width > 0);
}

function scale(widths: number[], total: number): number[] {
	const current = sum(widths);
	return current > 0 ? widths.map((width) => width * total / current) : widths;
}

/**
 * Stops the click a browser fires when a drag ends, so it reaches no button under the pointer.
 */
function swallowClick(): void {
	const swallow = (event: Event) => {
		event.preventDefault();
		event.stopPropagation();
	};
	document.addEventListener('click', swallow, true);
	window.setTimeout(() => document.removeEventListener('click', swallow, true), 0);
}

/**
 * The resizable columns of a table.
 */
export class ResizableColumns {
	/** The resizable tables, by id. */
	static readonly instances: Record<string, ResizableColumns> = {};

	/**
	 * Options a table gives for its own columns, by id, which attach takes where it is not given
	 * them: proportions, adjustToParent and measure, for example.
	 */
	static readonly tableOptions: Record<string, ResizableColumnsOptions> = {};

	private static stopWatchingAdditions: Unsubscribe | null = null;

	/** The host whose additions are watched, which changes when another one is set. */
	private static watchedHost: Host | null = null;

	readonly table: HTMLTableElement;

	readonly mode: ResizeMode;

	readonly minWidth: number;

	readonly step: number;

	onResize: ResizableColumnsOptions['onResize'];

	readonly adjustToParent: boolean;

	private readonly measureContent: ResizableColumnsOptions['measure'];

	/** Whether the widths were given or set by the user, rather than laid out. */
	sized: boolean;

	proportions: number[] | undefined;

	/** The widths the layout starts from: the proportions or the widths by content. */
	base: number[] | undefined = undefined;

	widths: number[] | undefined = undefined;

	private skippedWidth = 0;

	private drag: Drag | undefined = undefined;

	private readonly onPointerDown = (event: PointerEvent) => this.pointerDown(event);

	private readonly onKeyDown = (event: KeyboardEvent) => this.keyDown(event);

	private onWindowResize: (() => void) | undefined = undefined;

	/**
	 * @param table the table
	 * @param options how the columns are resized
	 */
	constructor(table: HTMLTableElement, options: ResizableColumnsOptions = {}) {
		this.table = table;
		this.mode = options.mode === 'stretch' ? 'stretch' : 'fit';
		this.minWidth = options.minWidth !== undefined ? options.minWidth : 80;
		this.step = options.step !== undefined ? options.step : 10;
		this.onResize = options.onResize;
		this.adjustToParent = options.adjustToParent === true;
		this.measureContent = options.measure;

		this.table.addEventListener('pointerdown', this.onPointerDown);
		this.table.addEventListener('keydown', this.onKeyDown);

		this.table.classList.add('wicket-resizable-columns', 'wicket-resizable-columns-' + this.mode);
		this.ensureHandles();
		this.table.style.tableLayout = 'fixed';
		const count = this.headerCells().length;
		this.sized = positiveWidths(options.widths, count);
		this.proportions = positiveWidths(options.proportions, count) ?
			options.proportions.slice() : undefined;
		if (this.sized) {
			this.apply(options.widths!);
		} else {
			this.base = this.proportions || this.measureFit();
			this.apply(this.layout(this.base));
		}

		if (this.adjustToParent && !this.sized) {
			this.onWindowResize = () => this.refit();
			window.addEventListener('resize', this.onWindowResize);
		}
	}

	/**
	 * Makes the columns of a table resizable, replacing an earlier attachment to it.
	 *
	 * @param table the table or its id
	 * @param options how the columns are resized, over those in tableOptions
	 * @returns the instance, or undefined if there is no such table
	 */
	static attach(table: HTMLTableElement | string, options?: ResizableColumnsOptions):
		ResizableColumns | undefined {
		const element = typeof table === 'string' ? document.getElementById(table) : table;
		if (!(element instanceof HTMLTableElement)) {
			return undefined;
		}
		const id = element.id;
		ResizableColumns.detach(id);
		const instance = new ResizableColumns(element,
			Object.assign({}, ResizableColumns.tableOptions[id], options));
		ResizableColumns.instances[id] = instance;
		ResizableColumns.watchAdditions();
		return instance;
	}

	/**
	 * Stops resizing the columns of a table, taking their handles away.
	 *
	 * @param id the id of the table
	 */
	static detach(id: string): void {
		const instance = ResizableColumns.instances[id];
		if (instance) {
			instance.destroy();
			delete ResizableColumns.instances[id];
		}
	}

	/**
	 * Lays the columns of a table out by their content again and keeps those widths, unless the
	 * table was given widths or the user resized a column.
	 *
	 * @param id the id of the table
	 */
	static fit(id: string): void {
		ResizableColumns.instances[id]?.fitContent();
	}

	/**
	 * Limits how much the column at index may change: no column gets narrower than min, and in
	 * 'fit' mode the neighbour, the next resizable column, gives what the column takes. A column
	 * already narrower than min is not made narrower still, nor its neighbour.
	 *
	 * @param neighbour the index of the neighbour, or -1 if there is none
	 * @returns the allowed change, in pixels
	 */
	static clamp(mode: ResizeMode, widths: number[], index: number, neighbour: number,
		delta: number, min: number): number {
		const lowest = Math.min(0, min - widths[index]);
		let highest = Infinity;
		if (mode === 'fit') {
			highest = neighbour >= 0 ? Math.max(0, widths[neighbour] - min) : 0;
		}
		return Math.max(lowest, Math.min(highest, delta));
	}

	/**
	 * @returns the widths after changing the column at index by delta, within the limits of clamp
	 */
	static resize(mode: ResizeMode, widths: number[], index: number, neighbour: number,
		delta: number, min: number): number[] {
		const allowed = ResizableColumns.clamp(mode, widths, index, neighbour, delta, min);
		const result = widths.slice();
		result[index] += allowed;
		if (mode === 'fit' && allowed !== 0) {
			result[neighbour] -= allowed;
		}
		return result;
	}

	/**
	 * @returns the rendered width of a cell, in pixels
	 */
	static measure(cell: Element): number {
		return cell.getBoundingClientRect().width;
	}

	/**
	 * Puts handles into the header cells an update of the page added to a table, and forgets the
	 * tables no longer in the page.
	 */
	private static watchAdditions(): void {
		const host = getHost();
		if (ResizableColumns.watchedHost === host) {
			return;
		}
		ResizableColumns.stopWatchingAdditions?.();
		ResizableColumns.watchedHost = host;
		ResizableColumns.stopWatchingAdditions = host.onNodeAdded((element) => {
			Object.keys(ResizableColumns.instances).forEach((id) => {
				const instance = ResizableColumns.instances[id];
				if (!document.body.contains(instance.table)) {
					ResizableColumns.detach(id);
				} else if (element && instance.table.contains(element)) {
					instance.ensureHandles();
				}
			});
		});
	}

	/**
	 * @returns the cells of the header row, the last row of the head holding th cells, except those
	 *          carrying data-resizable-skip
	 */
	headerCells(): HTMLTableCellElement[] {
		return this.headerRowCells().filter((cell) => !cell.hasAttribute('data-resizable-skip'));
	}

	/**
	 * @returns the th cells of the header row carrying data-resizable-skip, which are not columns
	 *          of their own but keep their width, such as an empty column at the end
	 */
	skippedCells(): HTMLTableCellElement[] {
		return this.headerRowCells().filter((cell) => cell.hasAttribute('data-resizable-skip'));
	}

	/**
	 * @returns the width of the cells carrying data-resizable-skip; the last known one while it
	 *          cannot be measured, for example while the table is hidden
	 */
	skipped(): number {
		const width = sum(this.skippedCells().map(ResizableColumns.measure));
		if (width > 0) {
			this.skippedWidth = width;
		}
		return this.skippedWidth;
	}

	private headerRowCells(): HTMLTableCellElement[] {
		const rows = this.table.tHead ? this.table.tHead.rows : [];
		for (let i = rows.length - 1; i >= 0; i--) {
			const cells = Array.from(rows[i].cells).filter((cell) => cell.tagName === 'TH');
			if (cells.length > 0) {
				return cells;
			}
		}
		return [];
	}

	/**
	 * @returns the current widths of the columns; the last known ones if they cannot be measured,
	 *          for example while the table is hidden
	 */
	measureAll(): number[] {
		const widths = this.headerCells().map((cell) => ResizableColumns.measure(cell));
		const measured = widths.length > 0 && widths.every((width) => width > 0);
		return measured || !this.widths ? widths : this.widths.slice();
	}

	/**
	 * @returns the widths the columns take by their content, measured by options.measure if given,
	 *          else the widths of the header cells
	 */
	measureFit(): number[] {
		if (!this.measureContent) {
			return this.measureAll();
		}
		const widths = this.measureContent(this.headerCells());
		return widths.length > 0 && widths.every((width) => width > 0) ? widths :
			this.measureAll();
	}

	fitContent(): void {
		if (this.sized || this.proportions) {
			return;
		}
		this.cols().forEach((col) => {
			col.style.width = '';
		});
		this.table.style.width = '';
		this.table.style.tableLayout = 'auto';
		this.base = this.measureFit();
		this.table.style.tableLayout = 'fixed';
		this.apply(this.layout(this.base));
	}

	/**
	 * @returns the widths for base widths or proportions: in 'stretch' mode scaled to the width of
	 *          the parent if the table adjusts to it, and proportions scaled to the width the table
	 *          has
	 */
	layout(base: number[]): number[] {
		if (this.mode !== 'stretch') {
			return base.slice();
		}
		let total = 0;
		if (this.adjustToParent) {
			total = this.parentWidth();
		} else if (this.proportions) {
			total = this.table.getBoundingClientRect().width;
		}
		total -= this.skipped();
		return total > 0 ? scale(base, total) : base.slice();
	}

	/**
	 * @returns the width of the content box of the table's parent, in pixels, without a vertical
	 *          scroll bar of the parent
	 */
	parentWidth(): number {
		const parent = this.table.parentElement;
		if (!parent) {
			return 0;
		}
		const style = window.getComputedStyle(parent);
		const borders = (parseFloat(style.borderLeftWidth) || 0) +
			(parseFloat(style.borderRightWidth) || 0);
		const scrollBar = Math.max(0, parent.offsetWidth - parent.clientWidth - borders);
		return parent.getBoundingClientRect().width - borders - scrollBar -
			(parseFloat(style.paddingLeft) || 0) - (parseFloat(style.paddingRight) || 0);
	}

	/**
	 * Lays the columns out for the parent's width again, unless the user resized one.
	 */
	refit(): void {
		if (!this.sized && this.base) {
			this.apply(this.layout(this.base));
		}
	}

	cols(): HTMLTableColElement[] {
		let colgroup = this.table.querySelector('colgroup.' + COLGROUP_CLASS);
		const count = this.headerCells().length;
		if (!colgroup || colgroup.children.length !== count) {
			colgroup?.remove();
			colgroup = document.createElement('colgroup');
			colgroup.className = COLGROUP_CLASS;
			for (let i = 0; i < count; i++) {
				colgroup.appendChild(document.createElement('col'));
			}
			this.table.insertBefore(colgroup, this.table.firstChild);
		}
		return Array.from(colgroup.children) as HTMLTableColElement[];
	}

	/**
	 * Sets the widths of the columns: as shares of the table in 'fit' mode, in pixels in 'stretch'
	 * mode, where the table takes their sum.
	 */
	apply(widths: number[]): void {
		this.widths = widths.slice();
		const cols = this.cols();
		const total = sum(widths);
		const fit = this.mode === 'fit';
		const skipped = this.skipped();
		cols.forEach((col, index) => {
			const width = widths[index] || 0;
			if (!fit) {
				col.style.width = width + 'px';
			} else if (total <= 0) {
				col.style.width = '';
			} else if (skipped > 0) {
				col.style.width = 'calc((100% - ' + skipped + 'px) * ' + (width / total) + ')';
			} else {
				col.style.width = (width / total * 100) + '%';
			}
		});
		this.table.style.width = fit ? '100%' : (total + skipped) + 'px';
	}

	/**
	 * @returns the index of the column giving or taking width when the column at index is resized
	 *          in 'fit' mode, the next one that is resizable, or -1
	 */
	neighbourOf(index: number, cells: HTMLTableCellElement[]): number {
		for (let i = index + 1; i < cells.length; i++) {
			if (cells[i].getAttribute('data-resizable') !== 'false') {
				return i;
			}
		}
		return -1;
	}

	isResizable(index: number, cells: HTMLTableCellElement[]): boolean {
		if (cells[index].getAttribute('data-resizable') === 'false') {
			return false;
		}
		return this.mode === 'stretch' || this.neighbourOf(index, cells) >= 0;
	}

	/**
	 * Applies new widths, and reports them to onResize.
	 */
	commit(widths: number[]): void {
		this.sized = true;
		this.apply(widths);
		this.onResize?.(widths.slice());
	}

	/**
	 * Puts a handle on the right edge of every resizable header cell that lacks one, for example
	 * after the header row was rendered again.
	 */
	ensureHandles(): void {
		const cells = this.headerCells();
		cells.forEach((cell, index) => {
			let handle = Array.from(cell.children).find((child) =>
				child.classList.contains(HANDLE_CLASS)) as HTMLElement | undefined;
			if (!this.isResizable(index, cells)) {
				handle?.remove();
				return;
			}
			if (!handle) {
				handle = document.createElement('span');
				handle.className = HANDLE_CLASS;
				handle.setAttribute('role', 'separator');
				handle.setAttribute('aria-orientation', 'vertical');
				handle.setAttribute('aria-label', 'Resize column');
				handle.tabIndex = 0;
				cell.appendChild(handle);
			}
			handle.setAttribute('data-index', String(index));
			handle.setAttribute('data-neighbour',
				String(this.mode === 'fit' ? this.neighbourOf(index, cells) : -1));
		});
		if (this.widths &&
			(this.cols().length !== cells.length || this.widths.length !== cells.length)) {
			this.apply(this.measureAll());
		}
	}

	private handleOf(event: Event): HTMLElement | null {
		const handle = (event.target as Element | null)?.closest?.<HTMLElement>('.' + HANDLE_CLASS);
		return handle && this.table.contains(handle) ? handle : null;
	}

	private pointerDown(event: PointerEvent): void {
		const handle = this.handleOf(event);
		if (!handle || (event.button !== undefined && event.button !== 0)) {
			return;
		}
		event.preventDefault();
		this.startDrag(Number(handle.getAttribute('data-index')),
			Number(handle.getAttribute('data-neighbour')), event.clientX);
	}

	/**
	 * @returns the vertical extent of the header row and the body, leaving out navigation rows and
	 *          the footer
	 */
	private extent(cell: HTMLTableCellElement): { top: number; height: number } {
		const top = (cell.parentElement ?? cell).getBoundingClientRect().top;
		const bodies = this.table.tBodies;
		const bottom = bodies.length > 0 ?
			bodies[bodies.length - 1].getBoundingClientRect().bottom :
			this.table.getBoundingClientRect().bottom;
		return { top, height: Math.max(0, bottom - top) };
	}

	startDrag(index: number, neighbour: number, clientX: number): void {
		this.endDrag(false);
		document.querySelectorAll('.' + GUIDE_CLASS + ', .' + BAND_CLASS)
			.forEach((leftover) => leftover.remove());
		const cell = this.headerCells()[index];
		const extent = this.extent(cell);
		const cellRect = cell.getBoundingClientRect();
		const drag: Drag = {
			index,
			neighbour,
			startX: clientX,
			widths: this.measureAll(),
			edge: cellRect.right + window.scrollX,
			delta: 0,
			guide: document.createElement('div'),
			band: document.createElement('div'),
			label: document.createElement('span'),
			move: (event) => this.moveDrag(event.clientX),
			up: (event) => {
				try {
					this.moveDrag(event.clientX);
				} finally {
					this.endDrag(true);
				}
			},
			cancel: () => this.endDrag(false),
			key: (event) => {
				if (event.key === 'Escape') {
					this.endDrag(false);
				}
			}
		};
		drag.guide.className = GUIDE_CLASS;
		drag.guide.style.top = (extent.top + window.scrollY) + 'px';
		drag.guide.style.height = extent.height + 'px';
		drag.guide.style.left = drag.edge + 'px';
		drag.label.className = LABEL_CLASS;
		drag.guide.appendChild(drag.label);
		drag.band.className = BAND_CLASS;
		drag.band.style.top = drag.guide.style.top;
		drag.band.style.height = drag.guide.style.height;
		drag.band.style.left = (cellRect.left + window.scrollX) + 'px';
		this.copyTheme(drag.guide);
		this.copyTheme(drag.band);
		document.body.appendChild(drag.band);
		document.body.appendChild(drag.guide);
		this.showDrag(drag);
		this.table.classList.add(RESIZING_CLASS);

		document.addEventListener('pointermove', drag.move);
		document.addEventListener('pointerup', drag.up);
		document.addEventListener('pointercancel', drag.cancel);
		document.addEventListener('keydown', drag.key);
		window.addEventListener('blur', drag.cancel);
		this.drag = drag;
	}

	moveDrag(clientX: number): void {
		const drag = this.drag;
		if (!drag) {
			return;
		}
		drag.delta = ResizableColumns.clamp(this.mode, drag.widths, drag.index, drag.neighbour,
			clientX - drag.startX, this.minWidth);
		this.showDrag(drag);
	}

	/**
	 * Places the guide on the edge the column would get, and shows its extent and width.
	 */
	private showDrag(drag: Drag): void {
		const width = drag.widths[drag.index] + drag.delta;
		drag.guide.style.left = (drag.edge + drag.delta) + 'px';
		drag.band.style.width = Math.max(0, width) + 'px';
		drag.label.textContent = Math.round(width) + ' px';
	}

	/**
	 * Copies the theme's colors to an element appended to the body, outside of the table.
	 */
	private copyTheme(element: HTMLElement): void {
		const style = window.getComputedStyle(this.table);
		['--wicket-theme-accent', '--wicket-theme-on-primary'].forEach((name) => {
			const value = style.getPropertyValue(name);
			if (value) {
				element.style.setProperty(name, value.trim());
			}
		});
	}

	/**
	 * Ends dragging, applying the new width if commit is true.
	 */
	endDrag(commit: boolean): void {
		const drag = this.drag;
		if (!drag) {
			return;
		}
		this.drag = undefined;
		document.removeEventListener('pointermove', drag.move);
		document.removeEventListener('pointerup', drag.up);
		document.removeEventListener('pointercancel', drag.cancel);
		document.removeEventListener('keydown', drag.key);
		window.removeEventListener('blur', drag.cancel);
		drag.guide.remove();
		drag.band.remove();
		this.table.classList.remove(RESIZING_CLASS);
		swallowClick();
		if (commit && drag.delta !== 0) {
			this.commit(ResizableColumns.resize(this.mode, drag.widths, drag.index, drag.neighbour,
				drag.delta, this.minWidth));
		}
	}

	private keyDown(event: KeyboardEvent): void {
		const handle = this.handleOf(event);
		if (!handle) {
			return;
		}
		let delta;
		if (event.key === 'ArrowLeft') {
			delta = -this.step;
		} else if (event.key === 'ArrowRight') {
			delta = this.step;
		} else {
			return;
		}
		event.preventDefault();
		this.commit(ResizableColumns.resize(this.mode, this.measureAll(),
			Number(handle.getAttribute('data-index')), Number(handle.getAttribute('data-neighbour')),
			delta, this.minWidth));
	}

	/**
	 * Stops listening and takes the handles away; the widths stay.
	 */
	destroy(): void {
		this.endDrag(false);
		if (this.onWindowResize) {
			window.removeEventListener('resize', this.onWindowResize);
		}
		this.table.removeEventListener('pointerdown', this.onPointerDown);
		this.table.removeEventListener('keydown', this.onKeyDown);
		this.table.querySelectorAll('.' + HANDLE_CLASS).forEach((handle) => handle.remove());
	}
}
