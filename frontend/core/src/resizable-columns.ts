/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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
import { lookOf, partClasses, type Look } from './look.js';

export type ResizeMode = 'fit' | 'stretch';

/**
 * How the columns of a table are resized.
 */
export interface ResizableColumnsOptions {
	/** 'fit' keeps the width of the table, 'stretch' lets it follow its columns; 'fit' by default. */
	mode?: ResizeMode;
	/** The look of the table; that of the element it is in when left out (see lookOf). */
	look?: Look;
	/** The narrowest a column gets, in pixels; 80 by default. */
	minWidth?: number;
	/** The pixels an arrow key resizes by; 10 by default. */
	step?: number;
	/** The accessible name of the handles, in the language of the page; "Resize column" by default. */
	label?: string;
	/** Saved widths, in pixels. */
	widths?: number[];
	/** Relative widths of the columns. */
	proportions?: number[];
	/** Whether the table takes the width of its parent. */
	adjustToParent?: boolean;
	/** Called with the widths after the user resized a column. */
	onResize?: (widths: number[]) => void;
	/**
	 * Whether a table rendered again with the same id, by an Ajax update for example, keeps the
	 * widths its columns had, rather than being laid out by its content again; unless it is given
	 * widths or proportions, its mode changed or its header cells did.
	 */
	keepWidths?: boolean;
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

const HANDLE_CLASS = 'antilia-col-resizer';
const GUIDE_CLASS = 'antilia-col-resize-guide';
const BAND_CLASS = 'antilia-col-resize-band';
const RESIZING_CLASS = 'antilia-resizing';
const COLGROUP_CLASS = 'antilia-resizable-colgroup';
/** Marks a header cell that is not a column of its own, and its col. */
const SKIP_ATTRIBUTE = 'data-resizable-skip';
/** Marks a table whose columns were laid out, which no longer follows the width of its parent. */
const LAID_OUT_CLASS = 'antilia-resizable-columns-laid-out';

/** The widths a table keeping them had when it was detached, with its mode and header cells. */
interface KeptWidths {
	mode: ResizeMode;
	headers: string;
	widths: number[];
}

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

	/** The widths of the tables keeping them, by their ids, until they are attached again. */
	private static readonly kept: Record<string, KeptWidths> = {};

	private static stopWatchingAdditions: Unsubscribe | null = null;

	/** The host whose additions are watched, which changes when another one is set. */
	private static watchedHost: Host | null = null;

	readonly table: HTMLTableElement;

	/** The look of the table, whose classes the elements made for it get. */
	private readonly look: Look;

	readonly mode: ResizeMode;

	readonly minWidth: number;

	readonly step: number;

	/** The accessible name of the handles. */
	readonly label: string;

	onResize: ResizableColumnsOptions['onResize'];

	readonly adjustToParent: boolean;

	private readonly measureContent: ResizableColumnsOptions['measure'];

	private readonly keepWidths: boolean;

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

	/** Lays the columns out again when the parent changes width, the window or not. */
	private parentObserver: ResizeObserver | undefined = undefined;

	/** Waits for a hidden table to be shown, to lay its columns out by their content then. */
	private whenShown: ResizeObserver | undefined = undefined;

	/**
	 * @param table the table
	 * @param options how the columns are resized
	 */
	constructor(table: HTMLTableElement, options: ResizableColumnsOptions = {}) {
		this.table = table;
		this.look = options.look ?? lookOf(table);
		this.mode = options.mode === 'stretch' ? 'stretch' : 'fit';
		this.minWidth = options.minWidth !== undefined ? options.minWidth : 80;
		this.step = options.step !== undefined ? options.step : 10;
		this.label = options.label || 'Resize column';
		this.onResize = options.onResize;
		this.adjustToParent = options.adjustToParent === true;
		this.measureContent = options.measure;
		this.keepWidths = options.keepWidths === true;

		this.table.addEventListener('pointerdown', this.onPointerDown);
		this.table.addEventListener('keydown', this.onKeyDown);

		this.table.classList.add('antilia-resizable-columns', 'antilia-resizable-columns-' + this.mode);
		this.ensureHandles();
		const count = this.headerCells().length;
		this.sized = positiveWidths(options.widths, count);
		this.proportions = positiveWidths(options.proportions, count) ?
			options.proportions.slice() : undefined;
		const kept = this.sized || this.proportions ? undefined : this.keptWidths();
		delete ResizableColumns.kept[this.table.id];
		if (this.sized) {
			this.table.style.tableLayout = 'fixed';
			this.apply(options.widths!);
		} else if (this.proportions) {
			this.base = this.proportions;
			this.table.style.tableLayout = 'fixed';
			this.apply(this.layout(this.base));
		} else if (kept) {
			this.base = kept;
			this.table.style.tableLayout = 'fixed';
			this.apply(this.base);
		} else {
			this.layOutWhenShown(() => this.layOutByContent());
		}

		if (this.adjustToParent && !this.sized) {
			this.onWindowResize = () => this.refit();
			window.addEventListener('resize', this.onWindowResize);
			this.followParent();
		}
	}

	/**
	 * Refits the columns whenever the width of the parent changes, not only with the window: a
	 * table in a pane of a split panel, for example, which takes its width from the divider, or
	 * gets it only once the split panel's script has run.
	 */
	private followParent(): void {
		const parent = this.table.parentElement;
		if (!parent || typeof ResizeObserver === 'undefined') {
			return;
		}
		let width = Math.round(parent.getBoundingClientRect().width);
		this.parentObserver = new ResizeObserver(() => {
			const now = Math.round(parent.getBoundingClientRect().width);
			if (now !== width && now > 0) {
				width = now;
				this.refit();
			}
		});
		this.parentObserver.observe(parent);
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
	 * @param restore whether the widths go too, the table being laid out as it was rendered,
	 *        rather than staying
	 */
	static detach(id: string, restore = false): void {
		const instance = ResizableColumns.instances[id];
		if (instance) {
			instance.destroy();
			if (restore) {
				instance.restore();
			} else if (instance.keepWidths && instance.widths) {
				ResizableColumns.kept[id] = { mode: instance.mode,
					headers: instance.headerTexts(), widths: instance.widths.slice() };
			}
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
		return this.headerRowCells().filter((cell) => !cell.hasAttribute(SKIP_ATTRIBUTE));
	}

	/**
	 * @returns the texts of the header cells, telling whether a table rendered again has the same
	 *          columns
	 */
	headerTexts(): string {
		return this.headerCells().map((cell) => (cell.textContent || '').trim()).join('\n');
	}

	/**
	 * @returns the widths this table kept when it was detached, if it keeps them and has the same
	 *          mode and columns
	 */
	private keptWidths(): number[] | undefined {
		const kept = ResizableColumns.kept[this.table.id];
		return kept && this.keepWidths && kept.mode === this.mode &&
			kept.headers === this.headerTexts() ? kept.widths : undefined;
	}

	/**
	 * @returns the th cells of the header row carrying data-resizable-skip, which are not columns
	 *          of their own but keep their width, such as an empty column at the end
	 */
	skippedCells(): HTMLTableCellElement[] {
		return this.headerRowCells().filter((cell) => cell.hasAttribute(SKIP_ATTRIBUTE));
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
		this.layOutWhenShown(() => {
			this.cols().forEach((col) => {
				col.style.width = '';
			});
			this.table.style.width = '';
			this.table.classList.remove(LAID_OUT_CLASS);
			this.layOutByContent();
		});
	}

	/**
	 * Lays the columns out by their content, measured as the table is laid out automatically,
	 * before its layout is fixed.
	 */
	private layOutByContent(): void {
		this.table.style.tableLayout = 'auto';
		this.base = this.measureFit();
		this.table.style.tableLayout = 'fixed';
		this.apply(this.layout(this.base));
	}

	/**
	 * Lays the columns out now, or, while the table is hidden, in a tab not shown for example, as
	 * soon as it is shown: a hidden table has no widths to measure, and would keep columns of none.
	 */
	private layOutWhenShown(layOut: () => void): void {
		this.whenShown?.disconnect();
		this.whenShown = undefined;
		if (this.table.getClientRects().length > 0 || typeof ResizeObserver === 'undefined') {
			layOut();
			return;
		}
		const observer = new ResizeObserver(() => {
			if (this.table.getClientRects().length > 0) {
				observer.disconnect();
				this.whenShown = undefined;
				layOut();
			}
		});
		this.whenShown = observer;
		observer.observe(this.table);
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

	/**
	 * @returns the cols of the columns, one for each header cell not carrying
	 *          data-resizable-skip; the colgroup has one for those too, marked with the attribute
	 */
	cols(): HTMLTableColElement[] {
		return this.allCols().filter((col) => !col.hasAttribute(SKIP_ATTRIBUTE));
	}

	/**
	 * @returns the cols of all header cells, in their order, those of the cells carrying
	 *          data-resizable-skip marked with it
	 */
	private allCols(): HTMLTableColElement[] {
		let colgroup = this.table.querySelector('colgroup.' + COLGROUP_CLASS);
		const cells = this.headerRowCells();
		const skips = cells.map((cell) => cell.hasAttribute(SKIP_ATTRIBUTE));
		if (!colgroup || colgroup.children.length !== cells.length ||
			Array.from(colgroup.children).some((col, i) => col.hasAttribute(SKIP_ATTRIBUTE) !== skips[i])) {
			colgroup?.remove();
			colgroup = document.createElement('colgroup');
			colgroup.className = COLGROUP_CLASS;
			skips.forEach((skip) => {
				const col = document.createElement('col');
				if (skip) {
					col.setAttribute(SKIP_ATTRIBUTE, '');
				}
				colgroup!.appendChild(col);
			});
			this.table.insertBefore(colgroup, this.table.firstChild);
		}
		return Array.from(colgroup.children) as HTMLTableColElement[];
	}

	/**
	 * Keeps the cells carrying data-resizable-skip as wide as they are, in pixels.
	 */
	private fixSkippedCols(): void {
		const cells = this.skippedCells();
		this.allCols().filter((col) => col.hasAttribute(SKIP_ATTRIBUTE)).forEach((col, i) => {
			const width = cells[i] ? ResizableColumns.measure(cells[i]) : 0;
			if (width > 0) {
				col.style.width = width + 'px';
			}
		});
	}

	/**
	 * Sets the widths of the columns: as shares of the table in 'fit' mode, in pixels in 'stretch'
	 * mode, where the table takes their sum.
	 */
	apply(widths: number[]): void {
		this.widths = widths.slice();
		this.fixSkippedCols();
		const cols = this.cols();
		const total = sum(widths);
		const fit = this.mode === 'fit';
		const skipped = this.skipped();
		// in 'fit' mode, browsers shrink the shares of the columns to leave the skipped cols their
		// pixels; they would lay out shares written as calc(100% - pixels) as if they had no width
		cols.forEach((col, index) => {
			const width = widths[index] || 0;
			if (!fit) {
				col.style.width = width + 'px';
			} else if (total <= 0) {
				col.style.width = '';
			} else {
				col.style.width = (width / total * 100) + '%';
			}
		});
		this.table.style.width = fit ? '100%' : (total + skipped) + 'px';
		this.table.classList.add(LAID_OUT_CLASS);
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
				handle.className = partClasses(this.look, 'column-resizer');
				handle.setAttribute('role', 'separator');
				handle.setAttribute('aria-orientation', 'vertical');
				handle.tabIndex = 0;
				cell.appendChild(handle);
			}
			handle.setAttribute('aria-label', this.label);
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
	 *          the footer, and the rows scrolled out of a box the table scrolls in
	 */
	private extent(cell: HTMLTableCellElement): { top: number; height: number } {
		let top = (cell.parentElement ?? cell).getBoundingClientRect().top;
		const bodies = this.table.tBodies;
		let bottom = bodies.length > 0 ?
			bodies[bodies.length - 1].getBoundingClientRect().bottom :
			this.table.getBoundingClientRect().bottom;
		for (let box = this.table.parentElement; box && box !== document.body;
			box = box.parentElement) {
			if (getComputedStyle(box).overflowY !== 'visible') {
				const rect = box.getBoundingClientRect();
				const inner = rect.top + box.clientTop;
				top = Math.max(top, inner);
				bottom = Math.min(bottom, inner + box.clientHeight);
			}
		}
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
		drag.guide.className = partClasses(this.look, 'column-resize-guide');
		drag.guide.style.top = (extent.top + window.scrollY) + 'px';
		drag.guide.style.height = extent.height + 'px';
		drag.guide.style.left = drag.edge + 'px';
		drag.label.className = partClasses(this.look, 'column-resize-label');
		drag.guide.appendChild(drag.label);
		drag.band.className = partClasses(this.look, 'column-resize-band');
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
		['--antilia-theme-accent', '--antilia-theme-on-primary'].forEach((name) => {
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
		this.whenShown?.disconnect();
		this.whenShown = undefined;
		if (this.onWindowResize) {
			window.removeEventListener('resize', this.onWindowResize);
		}
		this.parentObserver?.disconnect();
		this.parentObserver = undefined;
		this.table.removeEventListener('pointerdown', this.onPointerDown);
		this.table.removeEventListener('keydown', this.onKeyDown);
		this.table.querySelectorAll('.' + HANDLE_CLASS).forEach((handle) => handle.remove());
	}

	/**
	 * Takes the widths of the columns and the classes the table got away, so that the table is
	 * laid out as it was rendered, for example by a framework starting it again without resizable
	 * columns.
	 */
	restore(): void {
		this.table.querySelector(':scope > colgroup.' + COLGROUP_CLASS)?.remove();
		this.table.classList.remove('antilia-resizable-columns',
			'antilia-resizable-columns-' + this.mode, LAID_OUT_CLASS);
		this.table.style.width = '';
		this.table.style.tableLayout = '';
	}
}
