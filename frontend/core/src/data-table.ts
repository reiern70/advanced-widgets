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

/*
 * The dynamic data tables: lays out the columns and the body, renders the rows from JSON with
 * per-column templates, and reports clicks on [data-dt-action] elements. In Wicket it is the
 * client side of JsonBasedDynamicDataTable and BeanBasedDynamicDataTable, whose rows come with the
 * markup; the rows come from cfg.dataUrl, or from cfg.load in React and Svelte. The template
 * engines are those of templates().
 */

import { getHost, onRemoving, type Unsubscribe } from './host.js';
import { ANTILIA_LOOK, lookOf, partClasses, toggleParts, type Look } from './look.js';
import { ResizableColumns, type ResizableColumnsOptions } from './resizable-columns.js';
import { Scrollbar } from './scrollbar.js';
import { templates, type CompiledTemplate, type TemplateEngine } from './templates.js';

/** A row: its key, its number counting from 1, whether it is selected, and its data. */
export interface DataTableRow {
	key: string;
	number?: number;
	selected?: boolean;
	data: unknown;
}

/** Rows to show: those of a page, how many there are in all, the page, and whether to scroll up. */
export interface DataTableData {
	rows: DataTableRow[];
	itemCount?: number;
	currentPage?: number;
	top?: boolean;
}

/** A column: the template of its cells and their CSS class. */
export interface DataTableColumn {
	template: string;
	cssClass?: string;
}

/** How the columns are laid out: their proportions, and whether the table fills its parent. */
export interface DataTableLayout {
	proportions?: number[];
	adjustToParent?: boolean;
}

/** A pushed message: the table it is for, its type, and what the type's handler needs. */
export interface DataTableMessage {
	tableId: string;
	typeId: string;
	[name: string]: unknown;
}

/**
 * How a table loads, renders and lays out its rows.
 */
export interface DataTableConfig {
	/** The id of the table element. */
	id: string;
	/** The look of the table; that of the element it is in when left out (see lookOf). */
	look?: Look;
	/** Whether the rows come with the markup instead of as JSON. */
	serverRows?: boolean;
	/** The URL answering with the rows shown, or with the next page of them for ?more=true. */
	dataUrl?: string;
	/** The URL of the next page of rows, when it is not dataUrl. */
	moreUrl?: string;
	/** Loads the rows shown, or the next page of them, in place of dataUrl. */
	load?: (more: boolean) => Promise<DataTableData>;
	/** The name of the template engine. */
	engine?: string;
	/** The columns of the rows, in order. */
	columns: DataTableColumn[];
	/** Whether messages pushed to the table are handled. */
	push?: boolean;
	/** How often the rows shown are loaded again, in milliseconds; never when left out. */
	pollMillis?: number;
	/** The number of rows there are and the page shown, until the rows tell. */
	itemCount?: number;
	currentPage?: number;
	/** The keys of the selected rows, for rows that do not tell. */
	selectedKeys?: Array<string | number>;
	/** 'one' when only one row is selected at a time. */
	selection?: string;
	/** Whether the next page of rows is loaded when the last row comes into view. */
	infinite?: boolean;
	/** Whether the rows end with an empty cell, below the room of a scroll bar. */
	filler?: boolean;
	/** The height of the body, which then scrolls the rows; the page scrolls when left out. */
	bodyHeight?: { value: number; unit: string };
	/** Present when the columns are resizable: the widths the user chose before, if any. */
	resize?: { widths?: number[] };
	layout?: DataTableLayout;
	/** Called when the table starts and stops loading rows, for a veil over it. */
	onBusy?: (busy: boolean) => void;
}

/** The scroll bars of a body, as Scrollbar.attach gives them. */
export interface Bars {
	update(): void;
	destroy(): void;
}

/** What the table needs of the resizable columns: their instances and options by table id. */
export interface ResizableColumnsRegistry {
	instances: Record<string, { table: HTMLTableElement }>;
	tableOptions: Record<string, ResizableColumnsOptions>;
	fit(id: string): void;
	detach(id: string, restore?: boolean): void;
}

/** A veil blocking an element while it loads, as Wicket.Veil. */
export interface Veil {
	show(id: string): void;
	hide(id: string): void;
}

const ROW_CHECKBOX = 'input.ant-data-table-select';

const PAGE_CHECKBOX = 'input.ant-data-table-select-page';

const OVERLAY_CLASS = 'ant-data-table-overlay';

const CLOSE_OVERLAY_ACTION = '_antilia-close-overlay';

const SHOW_COLUMNS_ACTION = '_antilia-show-columns';

const HEADERS_CLASS = 'ant-data-table-headers';

const FILLER_WIDTH = '3em';

const COLGROUP_CLASS = 'ant-data-table-colgroup';

const CHOOSER = '.ant-data-table-column-chooser';

/**
 * A dynamic data table.
 */
export class DynamicDataTable {
	/** The live tables, by the id of their table element. */
	static readonly instances: Record<string, DynamicDataTable> = {};

	/** Gives a scrolling body its scroll bars; Wicket gives those of Wicket.Scrollbar, if any. */
	static attachScrollbar: (element: HTMLElement, look?: Look) => Bars | undefined =
		(element, look) => Scrollbar.attach(element, { axis: 'y', look });

	/** The resizable columns of the page; Wicket gives Wicket.ResizableColumns, if any. */
	static resizableColumns: () => ResizableColumnsRegistry | undefined = () => ResizableColumns;

	/** The veil shown while more rows load; Wicket gives Wicket.Veil, if any. */
	static veil: () => Veil | undefined = () => undefined;

	/** Escapes text for markup, as the template engines do. */
	static escapeHtml(value: unknown): string {
		return templates().escapeHtml(value);
	}

	/** The template engines, by the name a table refers to them by. */
	static get engines(): Record<string, TemplateEngine> {
		return templates().engines;
	}

	readonly id: string;

	readonly cfg: DataTableConfig;

	readonly action: (action: string, key: string | null) => void;

	readonly navigation: (() => void) | null | undefined;

	readonly messages: Record<string, (message: DataTableMessage) => void>;

	itemCount: number | undefined;

	currentPage: number | undefined;

	/** The selected rows of the page, for rows from a resource, which does not know them. */
	selectedKeys: Set<string>;

	readonly table: HTMLTableElement;

	/** The look of the table, whose classes the elements it makes get. */
	private look: Look = ANTILIA_LOOK;

	loading = false;

	private cells: CompiledTemplate[] = [];

	private rowsTable: HTMLTableElement | undefined = undefined;

	private body: HTMLDivElement | undefined = undefined;

	private bodyCell: HTMLTableCellElement | undefined = undefined;

	private scrollbar: Bars | undefined = undefined;

	private bodyObserver: ResizeObserver | undefined = undefined;

	private endObserver: IntersectionObserver | undefined = undefined;

	private timer: number | undefined = undefined;

	private fitted = false;

	private readonly clickHandler = (event: MouseEvent) => this.onClick(event);

	private readonly keyHandler = (event: KeyboardEvent) => this.onKeyDown(event);

	private readonly documentClickHandler = (event: MouseEvent) => this.closeChoosers(event);

	private stops: Unsubscribe[] = [];

	/**
	 * @param cfg how the table loads, renders and lays out its rows
	 * @param action called with the action of a clicked [data-dt-action] element and the key of
	 *            its row
	 * @param navigation re-renders the table's navigation, after the rows told another number of
	 *            rows or page
	 * @param messages the handlers of the messages pushed to the table, by type id
	 * @param data the rows of the current page, when the server sends them along, for example
	 *            when it re-renders the table; loaded otherwise
	 */
	constructor(cfg: DataTableConfig, action: (action: string, key: string | null) => void,
		navigation?: (() => void) | null,
		messages?: Record<string, (message: DataTableMessage) => void> | null,
		data?: DataTableData | null) {
		DynamicDataTable.instances[cfg.id]?.destroy();

		this.id = cfg.id;
		this.cfg = cfg;
		this.action = action;
		this.navigation = navigation;
		this.messages = messages || {};
		this.itemCount = cfg.itemCount;
		this.currentPage = cfg.currentPage;
		this.selectedKeys = new Set((cfg.selectedKeys || []).map(String));
		this.table = document.getElementById(cfg.id) as HTMLTableElement;
		if (!this.table) {
			return;
		}
		this.look = cfg.look ?? lookOf(this.table);

		if (!cfg.serverRows) {
			const engine = templates().engines[cfg.engine ?? 'builtin'];
			if (!engine) {
				getHost().log.error('Wicket.DynamicDataTable: no template engine registered as "' +
					cfg.engine + '"');
				return;
			}
			this.cells = cfg.columns.map((column) => engine.compile(column.template));
		}

		this.table.addEventListener('click', this.clickHandler);
		this.table.addEventListener('keydown', this.keyHandler);
		document.addEventListener('click', this.documentClickHandler);
		DynamicDataTable.instances[this.id] = this;
		this.stops.push(onRemoving(this.table, () => this.destroy()));
		if (cfg.push) {
			this.stops.push(getHost().onMessage((message) => this.onMessage(message)));
		}
		this.applyBodyHeight();
		this.shareLayout();
		this.applyLayout();

		if (cfg.serverRows) {
			this.rowsReplaced();
		} else if (data) {
			this.update(data);
		} else {
			this.load();
		}

		if (cfg.pollMillis) {
			this.timer = window.setInterval(() => this.poll(), cfg.pollMillis);
		}
	}

	private isLive(): boolean {
		return DynamicDataTable.instances[this.id] === this;
	}

	/**
	 * Fetches the rows shown, or the next page of them.
	 */
	private fetch(more: boolean): Promise<DataTableData> {
		if (this.cfg.load) {
			return this.cfg.load(more);
		}
		// rows from a resource: the further rows come from the page, which counts them
		const base = (more && this.cfg.moreUrl) || this.cfg.dataUrl || '';
		const url = more ? base + (base.indexOf('?') >= 0 ? '&' : '?') + 'more=true' : base;
		return window.fetch(url, {
			credentials: 'same-origin',
			headers: { Accept: 'application/json' }
		}).then((response) => {
			if (!response.ok) {
				throw new Error('HTTP ' + response.status);
			}
			return response.json() as Promise<DataTableData>;
		});
	}

	/**
	 * Fetches the rows from the table's data URL, or its load, and renders them.
	 *
	 * @param done called once the rows are rendered, if given
	 */
	load(done?: () => void): Promise<void> {
		this.cfg.onBusy?.(true);
		return this.fetch(false).then((json) => {
			if (this.isLive()) {
				// the number of rows first, so that scrolling infinitely watches the last one
				const changed = json.itemCount !== undefined && (json.itemCount !== this.itemCount ||
					json.currentPage !== this.currentPage);
				if (json.itemCount !== undefined) {
					this.setPaging(json.itemCount, json.currentPage);
				}
				this.render(json.rows);
				if (changed) {
					this.navigation?.();
				}
				if (json.top) {
					this.scrollToTop();
				}
				done?.();
			}
		}).catch((error) => {
			getHost().log.error('Wicket.DynamicDataTable: loading the rows of #' + this.id +
				' failed', error);
		}).finally(() => this.cfg.onBusy?.(false));
	}

	/**
	 * Fetches the rows of another page, or of the same page sorted otherwise, from the resource
	 * serving them, as the server tells after paging, sorting or refreshing.
	 */
	reload(state: { dataUrl?: string; selectedKeys?: Array<string | number>; top?: boolean }):
		Promise<void> {
		if (state.dataUrl !== undefined) {
			this.cfg.dataUrl = state.dataUrl;
		}
		this.selectedKeys = new Set((state.selectedKeys || []).map(String));
		return this.load(() => {
			if (state.top) {
				this.scrollToTop();
			}
		});
	}

	/**
	 * Replaces the rows of the table with the rows of a page the server sent along with an Ajax
	 * response, whose toolbars match its paging state already.
	 */
	update(data: DataTableData): void {
		this.setPaging(data.itemCount, data.currentPage);
		this.render(data.rows);
		if (data.top) {
			this.scrollToTop();
		}
	}

	/**
	 * Scrolls the rows back to the first one: the body, if it scrolls, or else the page to the top
	 * of the table, if it is above the window.
	 */
	scrollToTop(): void {
		if (this.body) {
			this.body.scrollTop = 0;
		} else if (this.table.getBoundingClientRect().top < 0) {
			this.table.scrollIntoView();
		}
	}

	/**
	 * Scrolling infinitely, watches the last row, loading the next page of rows once it comes into
	 * view, in the body, if it scrolls, or else in the window.
	 */
	watchEnd(): void {
		if (!this.cfg.infinite || !window.IntersectionObserver) {
			return;
		}
		if (!this.endObserver) {
			this.endObserver = new window.IntersectionObserver((entries) => {
				if (entries.some((entry) => entry.isIntersecting)) {
					this.loadMore();
				}
			}, { root: this.body || null, rootMargin: '0px 0px 40px 0px' });
		}
		this.endObserver.disconnect();
		const rows = this.rowsBody().rows;
		if (rows.length > 0 && rows.length < (this.itemCount ?? 0)) {
			this.endObserver.observe(rows[rows.length - 1]);
		}
	}

	/**
	 * Fetches the next page of rows and adds them after the last row, blocking the table with its
	 * veil meanwhile, then has the navigation tell how many rows are shown.
	 */
	loadMore(): Promise<void> {
		if (this.loading) {
			return Promise.resolve();
		}
		this.loading = true;
		this.endObserver?.disconnect();
		const veil = this.table.id ? DynamicDataTable.veil() : undefined;
		veil?.show(this.table.id);
		this.cfg.onBusy?.(true);
		return this.fetch(true).then((json) => {
			if (!this.isLive()) {
				return;
			}
			this.append(json.rows);
			this.itemCount = json.itemCount;
			this.navigation?.();
		}).catch((error) => {
			getHost().log.error('Wicket.DynamicDataTable: loading more rows of #' + this.id +
				' failed', error);
		}).finally(() => {
			this.loading = false;
			veil?.hide(this.table.id);
			this.cfg.onBusy?.(false);
			if (this.isLive()) {
				this.watchEnd();
			}
		});
	}

	/**
	 * Adds rows after the last one.
	 */
	append(rows: DataTableRow[]): void {
		const body = this.rowsBody();
		const fragment = document.createDocumentFragment();
		(rows || []).forEach((row) => {
			const number = row.number !== undefined ? row.number : body.rows.length + 1;
			if (!document.getElementById(this.rowId(row.key))) {
				fragment.appendChild(this.buildRow(row.key, number, row.data, row.selected));
			}
		});
		body.appendChild(fragment);
		this.updateSelection();
		this.layoutBody();
	}

	/**
	 * Replaces the rows of the table.
	 *
	 * @param rows the rows, number being the 1-based position of a row across all pages; without
	 *            it the position in the given rows is used
	 */
	render(rows: DataTableRow[]): void {
		const fragment = document.createDocumentFragment();
		(rows || []).forEach((row, index) => {
			const number = row.number !== undefined ? row.number : index + 1;
			fragment.appendChild(this.buildRow(row.key, number, row.data, row.selected));
		});
		this.rowsBody().replaceChildren(fragment);
		this.rowsReplaced();
	}

	/**
	 * Lays the table out for new rows: marks the selected ones, sizes the body and, the first time
	 * there are rows, fits the columns to them. The server calls it after it replaced the rows of a
	 * table whose rows come with the markup.
	 */
	rowsReplaced(): void {
		this.updateSelection();
		this.layoutBody();
		if (!this.fitted && this.rowsBody().rows.length > 0) {
			this.fitted = true;
			this.fitColumns();
		}
		if (!this.loading) {
			this.watchEnd();
		}
	}

	/**
	 * @returns the body holding the rows: the table's own, or that of the table in the scrolling
	 *          body when the table has a body height
	 */
	rowsBody(): HTMLTableSectionElement {
		return this.rowsTable ? this.rowsTable.tBodies[0] : this.table.tBodies[0];
	}

	/**
	 * Gives the body the height cfg.bodyHeight. The body of the rows moves into a table of its own,
	 * in an element of that height in a body of the table, which scrolls them vertically; its
	 * columns take the widths of the header cells. A table wider than its space is as wide as its
	 * columns, as any table, and the page scrolls sideways.
	 */
	private applyBodyHeight(): void {
		if (!this.cfg.bodyHeight) {
			return;
		}
		const table = this.table;
		const rowsBody = table.tBodies[0];
		const rows = document.createElement('table');
		rows.className = table.className + ' ' + partClasses(this.look, 'table-rows');
		rows.setAttribute('role', 'presentation');
		rows.appendChild(document.createElement('colgroup'));
		const body = document.createElement('div');
		body.className = partClasses(this.look, 'table-body');
		body.appendChild(rows);
		const cell = document.createElement('td');
		cell.className = partClasses(this.look, 'table-body-cell');
		cell.appendChild(body);
		const host = document.createElement('tr');
		host.className = partClasses(this.look, 'table-body-row');
		host.appendChild(cell);
		const hostBody = document.createElement('tbody');
		hostBody.appendChild(host);
		table.insertBefore(hostBody, rowsBody);
		rows.appendChild(rowsBody);
		this.rowsTable = rows;
		this.body = body;
		this.bodyCell = cell;
		// the same slim scroll bar in every browser, over the room the empty column leaves
		this.scrollbar = DynamicDataTable.attachScrollbar(body, this.look);

		if (window.ResizeObserver) {
			this.bodyObserver = new window.ResizeObserver(() => this.layoutBody());
			this.bodyObserver.observe(table);
			this.headerCells().forEach((header) => this.bodyObserver!.observe(header));
		}
		this.layoutBody();
	}

	/**
	 * @returns all cells of the header row, the empty one at the end too
	 */
	headerCells(): HTMLTableCellElement[] {
		const row = this.table.tHead?.querySelector<HTMLTableRowElement>('tr.' + HEADERS_CLASS);
		return row ? Array.from(row.cells) : [];
	}

	/**
	 * Lays the scrolling body out: as high as cfg.bodyHeight and its columns as wide as the header
	 * cells above them. The empty column at the end leaves the room the vertical scroll bar takes.
	 */
	layoutBody(): void {
		const body = this.body;
		if (!body || !this.rowsTable || !this.bodyCell || !this.table.isConnected) {
			return;
		}
		this.copyClasses();
		const height = this.cfg.bodyHeight!;
		body.style.height = height.unit === '%' ? this.percentOfParent(height.value) :
			height.value + height.unit;

		const headers = this.headerCells();
		this.bodyCell.colSpan = Math.max(1, headers.length);
		const widths = headers.map((header) => header.getBoundingClientRect().width);
		const scrollBar = body.offsetWidth - body.clientWidth;
		if (this.cfg.filler && widths.length > 0) {
			widths[widths.length - 1] = Math.max(0, widths[widths.length - 1] - scrollBar);
		}
		const colgroup = this.rowsTable.querySelector('colgroup')!;
		while (colgroup.children.length < widths.length) {
			colgroup.appendChild(document.createElement('col'));
		}
		while (colgroup.children.length > widths.length) {
			colgroup.lastChild!.remove();
		}
		widths.forEach((width, index) => {
			(colgroup.children[index] as HTMLElement).style.width = width + 'px';
		});
		const style = window.getComputedStyle(this.table);
		const spacing = style.borderCollapse === 'collapse' ? 0 :
			parseFloat(style.borderSpacing) || 0;
		this.rowsTable.style.borderSpacing = style.borderSpacing;
		this.rowsTable.style.marginLeft = -spacing + 'px';
		this.rowsTable.style.width = widths.reduce((sum, width) => sum + width, 0) +
			spacing * (widths.length + 1) + 'px';
	}

	/**
	 * Gives the table of the rows the CSS classes of the table, which scripts such as that of the
	 * resizable columns add after the rows were put into it, so the same rules apply.
	 */
	private copyClasses(): void {
		const classes = this.table.className + ' ' + partClasses(this.look, 'table-rows');
		if (this.rowsTable && this.rowsTable.className !== classes) {
			this.rowsTable.className = classes;
		}
	}

	/**
	 * @returns the given percent of the height of the element the table is in, if that element
	 *          has a height of its own, which does not depend on the table; else nothing, the body
	 *          then taking the height of its rows
	 */
	private percentOfParent(percent: number): string {
		const parent = this.table.parentElement;
		if (!parent) {
			return '';
		}
		const height = parent.clientHeight;
		this.table.style.display = 'none';
		const without = parent.clientHeight;
		this.table.style.display = '';
		return without === height && height > 0 ? (height * percent / 100) + 'px' : '';
	}

	/**
	 * Measures the widths the columns take by their content when the rows are in a table of their
	 * own: that table is laid out by the browser with a copy of the header cells above the rows,
	 * as wide as its content, or at most as wide as the parent of the table when the table takes
	 * the width of its parent.
	 *
	 * @param cells the header cells to measure
	 * @returns the width of each, in pixels
	 */
	measureColumns(cells: Element[]): number[] {
		const headers = this.headerCells();
		const rows = this.rowsTable!;
		this.copyClasses();
		const cols = Array.from(rows.querySelectorAll('col'));
		const saved = cols.map((col) => col.style.width);
		const width = rows.style.width;
		cols.forEach((col) => {
			col.style.width = '';
		});
		const parent = this.table.parentElement;
		this.body!.style.width = (parent ? parent.clientWidth : 0) + 'px';
		rows.style.width = (this.cfg.layout || {}).adjustToParent ? '' : 'max-content';
		rows.style.tableLayout = 'auto';
		const head = rows.createTHead();
		const copies = head.insertRow();
		headers.forEach((header) => {
			const copy = header.cloneNode(true) as HTMLElement;
			copy.removeAttribute('id');
			copy.querySelectorAll('[id]').forEach((element) => element.removeAttribute('id'));
			copies.appendChild(copy);
		});
		const result = cells.map((cell) => {
			const copy = copies.cells[headers.indexOf(cell as HTMLTableCellElement)];
			return copy ? copy.getBoundingClientRect().width : cell.getBoundingClientRect().width;
		});
		head.remove();
		rows.style.tableLayout = '';
		rows.style.width = width;
		this.body!.style.width = '';
		cols.forEach((col, index) => {
			col.style.width = saved[index];
		});
		return result;
	}

	/**
	 * Lets resizable columns take the widths their content needs, unless the server sent widths
	 * the user chose before.
	 */
	private fitColumns(): void {
		const resize = this.cfg.resize;
		const registry = resize ? DynamicDataTable.resizableColumns() : undefined;
		const resizable = registry?.instances[this.id];
		if (resizable && resizable.table === this.table && !resize!.widths) {
			registry!.fit(this.id);
		} else if (!resize) {
			this.fitRows();
		}
		this.layoutBody();
	}

	/**
	 * Sizes the columns of a table whose rows are in a table of their own by the content of the
	 * header cells and the rows, which the header row alone does not tell, unless the columns have
	 * proportions.
	 */
	private fitRows(): void {
		const layout = this.cfg.layout || {};
		if (!this.rowsTable || layout.proportions) {
			return;
		}
		let colgroup = this.table.querySelector('colgroup.' + COLGROUP_CLASS);
		colgroup?.remove();
		this.table.style.tableLayout = 'auto';
		this.table.style.width = '';
		const widths = this.measureColumns(this.headerCells());
		const total = widths.reduce((sum, width) => sum + width, 0);
		colgroup = document.createElement('colgroup');
		colgroup.className = COLGROUP_CLASS;
		widths.forEach((width) => {
			const col = document.createElement('col');
			col.style.width = layout.adjustToParent ? (width / total * 100) + '%' : width + 'px';
			colgroup!.appendChild(col);
		});
		this.table.insertBefore(colgroup, this.table.firstChild);
		this.table.style.tableLayout = 'fixed';
		this.table.style.width = layout.adjustToParent ? '100%' : total + 'px';
	}

	/**
	 * Repaints one row in place, keeping its number and selection. A row the table does not show
	 * is ignored.
	 *
	 * @param key the key of the row
	 * @param data the row's data
	 */
	renderRow(key: string, data: unknown): void {
		const tr = document.getElementById(this.rowId(key));
		if (tr && this.rowsBody().contains(tr)) {
			const number = Number(tr.getAttribute('data-number'));
			const last = !tr.nextElementSibling;
			tr.replaceWith(this.buildRow(key, number, data, tr.classList.contains('selected')));
			if (last && !this.loading) {
				// the last row is watched for scrolling infinitely
				this.watchEnd();
			}
		}
	}

	rowId(key: string): string {
		return this.id + '-row-' + key;
	}

	/**
	 * Builds the element of a row. The column templates are evaluated as cell(data, meta), meta
	 * holding the row's key, number and selection, for example for {{@number}}.
	 */
	private buildRow(key: string, number: number, data: unknown, selected?: boolean):
		HTMLTableRowElement {
		if (selected === undefined) {
			// a row from a resource: selected if the page says so
			selected = this.selectedKeys.has(String(key));
		}
		const meta = { key, number, selected: !!selected };
		const tr = document.createElement('tr');
		tr.id = this.rowId(key);
		tr.setAttribute('data-key', key);
		tr.setAttribute('data-number', String(number));
		tr.className = partClasses(this.look, number % 2 === 1 ? 'table-row-even' : 'table-row-odd');
		this.cells.forEach((cell, index) => {
			const td = document.createElement('td');
			const cssClass = this.cfg.columns[index].cssClass;
			if (cssClass) {
				td.className = cssClass;
			}
			td.innerHTML = cell(data, meta);
			tr.appendChild(td);
		});
		if (this.cfg.filler) {
			const filler = document.createElement('td');
			filler.className = partClasses(this.look, 'table-filler');
			filler.setAttribute('aria-hidden', 'true');
			tr.appendChild(filler);
		}
		tr.querySelectorAll<HTMLInputElement>(ROW_CHECKBOX).forEach((checkbox) => {
			checkbox.checked = meta.selected;
		});
		toggleParts(tr, this.look, meta.selected, 'table-row-selected');
		return tr;
	}

	/**
	 * Checks or unchecks the selection checkboxes of all rows.
	 *
	 * @param selected whether the rows are selected
	 */
	selectRows(selected: boolean): void {
		this.rowsBody().querySelectorAll<HTMLInputElement>(ROW_CHECKBOX).forEach((checkbox) => {
			checkbox.checked = selected;
		});
		this.updateSelection();
	}

	/**
	 * Marks the rows whose selection checkbox is checked as selected, and shows in the header
	 * checkbox whether all, some or none of them are.
	 */
	updateSelection(): void {
		let count = 0;
		let checked = 0;
		const selectedKeys = this.selectedKeys;
		Array.from(this.rowsBody().rows).forEach((tr) => {
			const checkbox = tr.querySelector<HTMLInputElement>(ROW_CHECKBOX);
			if (checkbox) {
				count++;
				checked += checkbox.checked ? 1 : 0;
				// remembered for the rows a resource sends again, which it does not know
				const key = tr.getAttribute('data-key') ?? '';
				if (checkbox.checked) {
					selectedKeys.add(key);
				} else {
					selectedKeys.delete(key);
				}
			}
			toggleParts(tr, this.look, !!checkbox && checkbox.checked, 'table-row-selected');
		});
		const header = this.table.tHead?.querySelector<HTMLInputElement>(PAGE_CHECKBOX);
		if (header) {
			header.checked = count > 0 && checked === count;
			header.indeterminate = checked > 0 && checked < count;
		}
	}

	/**
	 * Re-renders the navigation if the number of rows or the current page changed.
	 */
	updatePaging(itemCount: number | undefined, currentPage: number | undefined): void {
		if (itemCount === undefined) {
			return;
		}
		const changed = itemCount !== this.itemCount || currentPage !== this.currentPage;
		this.setPaging(itemCount, currentPage);
		if (changed) {
			this.navigation?.();
		}
	}

	/**
	 * Records the paging state the navigation was rendered with.
	 */
	setPaging(itemCount: number | undefined, currentPage: number | undefined): void {
		this.itemCount = itemCount;
		this.currentPage = currentPage;
	}

	private poll(): void {
		if (document.getElementById(this.id) !== this.table) {
			this.destroy();
		} else {
			this.load();
		}
	}

	/**
	 * Hands a pushed message for this table to the handler of its type.
	 */
	private onMessage(message: unknown): void {
		let parsed: DataTableMessage | undefined = undefined;
		if (typeof message === 'string') {
			if (message.charAt(0) !== '{') {
				return;
			}
			try {
				parsed = JSON.parse(message) as DataTableMessage;
			} catch {
				return;
			}
		} else if (message && typeof message === 'object') {
			parsed = message as DataTableMessage;
		}
		if (!parsed || parsed.tableId !== this.id) {
			return;
		}
		const handler = this.messages[parsed.typeId];
		if (handler) {
			handler(parsed);
		} else {
			getHost().log.error('Wicket.DynamicDataTable: no handler for messages of type "' +
				parsed.typeId + '"');
		}
	}

	/**
	 * Sends the action of a clicked [data-dt-action] element. A checkbox keeps its new state and
	 * sends its data-dt-action-unchecked action, if any, when it was unchecked.
	 */
	private onClick(event: MouseEvent): void {
		if (this.onChooserClick(event)) {
			return;
		}
		const target = event.target as Element;
		const actionElement = target.closest<HTMLElement>('[data-dt-action]');
		if (!actionElement || !this.table.contains(actionElement)) {
			return;
		}
		let action = actionElement.getAttribute('data-dt-action');
		if (actionElement instanceof HTMLInputElement && actionElement.type === 'checkbox') {
			if (!actionElement.checked) {
				action = actionElement.getAttribute('data-dt-action-unchecked');
			}
			if (actionElement.matches(PAGE_CHECKBOX)) {
				this.selectRows(actionElement.checked);
			} else {
				if (this.cfg.selection === 'one' && actionElement.checked) {
					// one row at a time: the server deselects the others too
					this.rowsBody().querySelectorAll<HTMLInputElement>(ROW_CHECKBOX)
						.forEach((checkbox) => {
							checkbox.checked = checkbox === actionElement;
						});
				}
				this.updateSelection();
			}
			if (!action) {
				return;
			}
		} else {
			event.preventDefault();
		}
		const row = actionElement.closest('tr[data-key]');
		this.action(action ?? '', row ? row.getAttribute('data-key') : null);
	}

	/**
	 * Closes an open column chooser on Escape, and asks to close the overlay when Escape is
	 * pressed in it, unless the user changed something in it.
	 */
	private onKeyDown(event: KeyboardEvent): void {
		const target = event.target as Element;
		const chooser = event.key === 'Escape' && target.closest ?
			target.closest<HTMLDetailsElement>(CHOOSER) : null;
		if (chooser && chooser.open) {
			event.preventDefault();
			chooser.open = false;
			chooser.querySelector<HTMLElement>('summary')?.focus();
			return;
		}
		const overlay = event.key === 'Escape' && target.closest ?
			target.closest<HTMLElement>('.' + OVERLAY_CLASS) : null;
		if (overlay) {
			event.preventDefault();
			if (!DynamicDataTable.isChanged(overlay)) {
				this.action(CLOSE_OVERLAY_ACTION, null);
			}
		}
	}

	/**
	 * Handles a click in a column chooser: a column's button toggles whether it is shown, the
	 * apply button sends the shown columns, as SHOW_COLUMNS_ACTION with the values of their
	 * data-dt-column attributes, comma separated, as key.
	 *
	 * @returns whether the click was in a column chooser's menu
	 */
	private onChooserClick(event: MouseEvent): boolean {
		const target = event.target as Element;
		const chooser = target.closest<HTMLDetailsElement>(CHOOSER);
		if (!chooser || !this.table.contains(chooser)) {
			return false;
		}
		const column = target.closest('[data-dt-column]');
		if (column) {
			column.setAttribute('aria-pressed',
				String(column.getAttribute('aria-pressed') !== 'true'));
			this.updateChooser(chooser);
			return true;
		}
		if (target.closest('[data-dt-apply]')) {
			const shown = Array.from(chooser.querySelectorAll('[data-dt-column]'))
				.filter((button) => button.getAttribute('aria-pressed') === 'true')
				.map((button) => button.getAttribute('data-dt-column'));
			chooser.open = false;
			this.action(SHOW_COLUMNS_ACTION, shown.join(','));
			return true;
		}
		return false;
	}

	/**
	 * Disables the apply button of a column chooser while no column is chosen.
	 */
	private updateChooser(chooser: Element): void {
		const apply = chooser.querySelector<HTMLButtonElement>('[data-dt-apply]');
		if (apply) {
			apply.disabled = !chooser.querySelector('[data-dt-column][aria-pressed="true"]');
		}
	}

	/**
	 * Closes the open column choosers of the table a click went past.
	 */
	private closeChoosers(event: MouseEvent): void {
		this.table.querySelectorAll<HTMLDetailsElement>(CHOOSER + '[open]').forEach((chooser) => {
			if (!chooser.contains(event.target as Node)) {
				chooser.open = false;
			}
		});
	}

	/**
	 * Hands the layout of the columns to the resizable columns of the table, which attach after
	 * the table: the proportions, whether the table takes the width of its parent, and how to
	 * measure the columns when the rows are in a table of their own.
	 */
	private shareLayout(): void {
		const registry = DynamicDataTable.resizableColumns();
		if (!registry) {
			return;
		}
		const layout = this.cfg.layout || {};
		registry.tableOptions[this.id] = {
			proportions: layout.proportions,
			adjustToParent: layout.adjustToParent,
			measure: this.rowsTable ? (cells) => this.measureColumns(cells) : undefined
		};
	}

	/**
	 * Lays out the columns of a table whose columns cannot be resized: by their proportions, if
	 * any, and over the full width of the parent if the table adjusts to it.
	 */
	private applyLayout(): void {
		const layout = this.cfg.layout;
		if (!layout || this.cfg.resize) {
			return;
		}
		if (layout.adjustToParent) {
			this.table.style.width = '100%';
		}
		const proportions = layout.proportions;
		if (Array.isArray(proportions) && proportions.length > 0) {
			const total = proportions.reduce((sum, proportion) => sum + proportion, 0);
			let colgroup = this.table.querySelector('colgroup.' + COLGROUP_CLASS);
			if (!colgroup) {
				colgroup = document.createElement('colgroup');
				colgroup.className = COLGROUP_CLASS;
				this.table.insertBefore(colgroup, this.table.firstChild);
			}
			colgroup.replaceChildren();
			const filler = this.cfg.filler;
			proportions.forEach((proportion) => {
				const col = document.createElement('col');
				const share = proportion / total;
				// browsers shrink the shares to leave the filler its width; they would lay out
				// shares written as calc(100% - FILLER_WIDTH) as if they had no width
				col.style.width = (share * 100) + '%';
				colgroup!.appendChild(col);
			});
			if (filler) {
				const col = document.createElement('col');
				col.style.width = FILLER_WIDTH;
				colgroup.appendChild(col);
			}
			this.table.style.tableLayout = 'fixed';
		}
	}

	/**
	 * Puts the body of the rows back where it was before applyBodyHeight, so that the table can
	 * be started again, as React's StrictMode does.
	 */
	private restoreBody(): void {
		const rowsBody = this.rowsTable?.tBodies[0];
		const hostBody = this.bodyCell?.closest('tbody');
		if (rowsBody && hostBody && hostBody.parentNode === this.table) {
			this.table.insertBefore(rowsBody, hostBody);
			hostBody.remove();
		}
		this.rowsTable = undefined;
		this.body = undefined;
		this.bodyCell = undefined;
	}

	/**
	 * Takes away the widths the columns were laid out with, so that the table can be started
	 * again with another layout, as a framework does when its columns stop being resizable.
	 */
	private restoreLayout(): void {
		if (this.table) {
			this.table.querySelector(':scope > colgroup.' + COLGROUP_CLASS)?.remove();
			this.table.style.width = '';
			this.table.style.tableLayout = '';
		}
	}

	destroy(): void {
		this.bodyObserver?.disconnect();
		this.scrollbar?.destroy();
		this.endObserver?.disconnect();
		if (this.cfg.resize) {
			DynamicDataTable.resizableColumns()?.detach(this.id, true);
		}
		if (this.timer) {
			window.clearInterval(this.timer);
		}
		this.stops.forEach((stop) => stop());
		this.stops = [];
		this.restoreBody();
		this.restoreLayout();
		if (this.table) {
			this.table.removeEventListener('click', this.clickHandler);
			this.table.removeEventListener('keydown', this.keyHandler);
		}
		document.removeEventListener('click', this.documentClickHandler);
		if (DynamicDataTable.instances[this.id] === this) {
			delete DynamicDataTable.instances[this.id];
		}
	}

	/**
	 * Tells whether the user changed a form field inside an element, or an element in it carries
	 * the attribute data-dt-changed, for example a form the server rendered again with errors.
	 *
	 * @param element the element to look into
	 * @returns whether something in it was changed
	 */
	static isChanged(element: Element): boolean {
		if (element.querySelector('[data-dt-changed]')) {
			return true;
		}
		return Array.from(element.querySelectorAll<HTMLInputElement | HTMLSelectElement |
			HTMLTextAreaElement>('input, select, textarea')).some((field) => {
			if (field instanceof HTMLInputElement &&
				(field.type === 'checkbox' || field.type === 'radio')) {
				return field.checked !== field.defaultChecked;
			}
			if (field instanceof HTMLSelectElement) {
				const options = Array.from(field.options);
				const preset = options.some((option) => option.defaultSelected);
				return options.some((option, index) => {
					const initial = option.defaultSelected ||
						(!preset && !field.multiple && index === 0);
					return option.selected !== initial;
				});
			}
			return field.value !== (field as HTMLInputElement).defaultValue;
		});
	}

	/**
	 * Moves the window of a table's overlay into the part of the table the user sees, so it is not
	 * shown above or below the screen of a long table. An overlay over the whole page centers its
	 * window by CSS.
	 *
	 * @param id the id of the overlay's window
	 */
	static placeOverlay(id: string): void {
		const box = document.getElementById(id);
		const table = box?.closest('table');
		if (!box || !table || box.closest('.' + OVERLAY_CLASS + '-body')) {
			return;
		}
		const margin = 16;
		const tableTop = table.getBoundingClientRect().top;
		const highest = Math.max(margin, table.offsetHeight - box.offsetHeight - margin);
		box.style.top = Math.min(Math.max(margin, margin - tableTop), highest) + 'px';
	}
}
