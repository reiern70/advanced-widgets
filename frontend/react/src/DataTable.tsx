/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { forwardRef, useEffect, useId, useImperativeHandle, useLayoutEffect, useRef, useState,
	type MouseEvent, type ReactNode } from 'react';

import { DATA_TABLE_COMPONENTS, DATA_TABLE_TEXTS, DynamicDataTable, isLicensed, localRows,
	MovableColumns, pageLinks, ResizableColumns, selectionColumn, SELECTION_ACTIONS, SHOW_COLUMNS,
	UNLICENSED_CLASS, type DataTableColumnDef, type DataTableData, type DataTableFilter,
	type DataTablePush, type DataTableQuery, type DataTableRow, type DataTableSort,
	type DataTableTexts, type ResizeMode } from '@advanced-widgets/core';

import { Dropdown } from './Dropdown.js';

export type { DataTableColumnDef, DataTableFilter, DataTablePush, DataTableQuery, DataTableSort,
	DataTableTexts };

export interface DataTableProps {
	/** The columns, in their first order; the user may move, hide and show them. */
	columns: DataTableColumnDef[];
	/**
	 * Loads the rows asked for: [{key, data}] and how many there are in all, from a mounted
	 * resource for example. A table given its rows instead needs none.
	 */
	loadRows?(query: DataTableQuery): Promise<{ rows: DataTableRow[]; itemCount: number }>;
	/**
	 * The rows, a local list of JSON, when the table does not load them: it filters, sorts and
	 * pages them in the browser (see localRows of the core), and shows them again when another
	 * list is given.
	 */
	rows?: readonly DataTableRow[];
	/** Whether the data of a row matches the filter, for rows; whether a text contains it by default. */
	filterRows?: DataTableFilter;
	/** The rows of a page, or loaded at a time when scrolling infinitely; 25 by default. */
	rowsPerPage?: number;
	/** The numbers of rows per page the user chooses from. */
	pageSizes?: number[];
	/** Whether the rows load as the table scrolls, in place of pages. */
	infinite?: boolean;
	/** Whether, and how many, rows are selected with checkboxes. */
	selection?: 'none' | 'one' | 'many';
	/** Called with the keys of the selected rows after every change. */
	onSelectionChange?(keys: string[]): void;
	/** Called with the action of a clicked [data-dt-action] element and the key of its row. */
	onAction?(action: string, key: string | null): void;
	/** The text the rows are filtered by, handed to loadRows; another one starts over. */
	filter?: string;
	/** The sort at first. */
	sort?: DataTableSort | null;
	/** Called after the user sorted otherwise. */
	onSortChange?(sort: DataTableSort | null): void;
	/** Whether the columns marked movable can be moved; true by default. */
	movableColumns?: boolean;
	/** How columns are resized, or false when they are not. */
	resizableColumns?: ResizeMode | false;
	/** Whether the toolbar has the chooser of the columns marked hideable; true by default. */
	columnChooser?: boolean;
	/** The height of the body, which then scrolls the rows. */
	bodyHeight?: { value: number; unit: string };
	/** Whether the table takes the width of its parent. */
	adjustToParent?: boolean;
	/** The name of the template engine, the built-in one by default. */
	engine?: string;
	/** The texts of the table, in English by default. */
	texts?: Partial<DataTableTexts>;
	/** What the navigation shows before the paging, a filter for example. */
	toolbar?: ReactNode;
	/** The buttons of the toolbar, before the column chooser. */
	actions?: ReactNode;
	/** Subscribes the table to pushed changes of its rows, returning the unsubscription. */
	subscribe?(listener: (message: DataTablePush) => void): () => void;
	/** A license key of its own; the one given to activate() when left out. */
	licenseKey?: string;
	/** The id of the table element. */
	id?: string;
	/** CSS classes of the table, next to dynamic-dataview dynamic-data-table. */
	className?: string;
}

/**
 * What a ref to a {@link DataTable} can do with the table.
 */
export interface DataTableHandle {
	/** Loads the rows shown again. */
	reload(): Promise<void>;
	/** Repaints one row with new data, in place; a row the table does not show is ignored. */
	renderRow(key: string, data: unknown): void;
	/** The keys of the selected rows. */
	selectedKeys(): string[];
}

/** The time a load may take before the veil shows, as Wicket's veil of the table waits. */
const VEIL_DELAY = 300;

const GRIP = 'M2 1h2v2H2zm4 0h2v2H6zM2 7h2v2H2zm4 0h2v2H6zm-4 6h2v2H2zm4 0h2v2H6z';

function Grip() {
	return (
		<svg aria-hidden="true" focusable="false" viewBox="0 0 10 16" width="8" height="13">
			<path fill="currentColor" d={GRIP} />
		</svg>
	);
}

function ColumnsIcon() {
	return (
		<svg aria-hidden="true" focusable="false" viewBox="0 0 24 24" width="18" height="18"
			fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
			<path d="M4 5h16v14H4z M10 5v14 M15 5v14" />
		</svg>
	);
}

/**
 * A dynamic data table: React renders the table's skeleton in the markup of the Wicket table, the
 * navigation, the headers and the column chooser, and the core renders the rows from JSON with
 * the templates of the columns into the body React leaves empty. Sorting, paging or scrolling
 * infinitely, the filter and the page size reload the rows through loadRows; the columns can be
 * moved, resized, hidden and shown. A private component: without a license key activating it, it
 * shows the placeholder Wicket shows.
 */
export const DataTable = forwardRef<DataTableHandle, DataTableProps>(function DataTable(props,
	ref) {
	const { columns, rowsPerPage = 25, pageSizes = [25, 50, 100], infinite = false,
		selection = 'none', filter, movableColumns = true, resizableColumns = false,
		columnChooser = true, bodyHeight, adjustToParent, engine, toolbar, actions, licenseKey,
		className } = props;
	const texts: DataTableTexts = { ...DATA_TABLE_TEXTS, ...props.texts };
	const generatedId = useId();
	const id = props.id ?? 'data-table-' + generatedId.replace(/[^\w-]/g, '');

	const [licensed, setLicensed] = useState<boolean | null>(null);
	const [order, setOrder] = useState(() => columns.map((column) => column.id));
	const [shown, setShown] = useState(() => new Set(columns.filter((column) =>
		column.visible !== false).map((column) => column.id)));
	const [page, setPage] = useState(0);
	const [pageSize, setPageSize] = useState(rowsPerPage);
	const [sort, setSort] = useState<DataTableSort | null>(props.sort ?? null);
	const [itemCount, setItemCount] = useState(0);
	const [range, setRange] = useState({ first: 0, count: 0 });
	const [busy, setBusy] = useState(false);

	const instance = useRef<DynamicDataTable | null>(null);
	const shownRows = useRef(0);
	const startOver = useRef(false);
	const latest = useRef(props);
	latest.current = props;
	const state = useRef({ page, pageSize, sort, filter, infinite });
	state.current = { page, pageSize, sort, filter, infinite };

	useEffect(() => {
		let live = true;
		isLicensed(DATA_TABLE_COMPONENTS, licenseKey).then((active) => live && setLicensed(active));
		return () => {
			live = false;
		};
	}, [licenseKey]);

	const byId = new Map(columns.map((column) => [column.id, column]));
	const visible = order.filter((column) => shown.has(column) && byId.has(column))
		.map((column) => byId.get(column)!);
	const withSelection = selection !== 'none';
	const coreColumns = (withSelection ? [selectionColumn(texts)] : []).concat(visible.map(
		(column) => ({ template: column.template, cssClass: column.cssClass })));
	const columnsKey = JSON.stringify(coreColumns);
	const filler = Boolean(bodyHeight);
	const heightKey = bodyHeight ? bodyHeight.value + bodyHeight.unit : '';

	const reportSelection = (table: DynamicDataTable) =>
		latest.current.onSelectionChange?.(Array.from(table.selectedKeys));

	useLayoutEffect(() => {
		if (!licensed) {
			return;
		}
		const table = new DynamicDataTable({
			id,
			columns: JSON.parse(columnsKey),
			engine,
			filler,
			bodyHeight,
			selection,
			infinite,
			layout: adjustToParent ? { adjustToParent } : undefined,
			resize: resizableColumns ? {} : undefined,
			onBusy: setBusy,
			load: async (more): Promise<DataTableData> => {
				const current = state.current;
				if (!more && startOver.current) {
					shownRows.current = 0;
				}
				const first = more ? shownRows.current : current.infinite ? 0 :
					current.page * current.pageSize;
				const count = more || !current.infinite ? current.pageSize :
					Math.max(shownRows.current, current.pageSize);
				const props = latest.current;
				const load = props.loadRows ?? localRows(() => latest.current.rows ?? [],
					props.filterRows);
				const loaded = await load({ first, count, sort: current.sort,
					filter: current.filter });
				shownRows.current = (more ? shownRows.current : 0) + loaded.rows.length;
				const top = startOver.current;
				startOver.current = false;
				setItemCount(loaded.itemCount);
				setRange(current.infinite ? { first: 0, count: shownRows.current } :
					{ first, count: loaded.rows.length });
				return {
					rows: loaded.rows.map((row, index) => ({ ...row,
						number: row.number ?? first + index + 1 })),
					itemCount: loaded.itemCount,
					currentPage: current.page,
					top
				};
			}
		}, (action, key) => {
			if (SELECTION_ACTIONS.includes(action)) {
				if (selection === 'one' && action === SELECTION_ACTIONS[0] && key !== null) {
					table.selectedKeys = new Set([key]);
				}
				reportSelection(table);
			} else if (action === SHOW_COLUMNS) {
				const chosen = new Set((key ?? '').split(',').filter(Boolean));
				setShown(new Set(columns.filter((column) => !column.hideable ||
					chosen.has(column.id)).map((column) => column.id)));
			} else {
				latest.current.onAction?.(action, key);
			}
		}, () => setItemCount(table.itemCount ?? 0));
		instance.current = table;

		const offset = withSelection ? 1 : 0;
		const ids = visible.map((column) => column.id);
		if (resizableColumns) {
			ResizableColumns.attach(id, { mode: resizableColumns });
		}
		if (movableColumns) {
			MovableColumns.attach(id, {
				movable: visible.flatMap((column, index) => column.movable ? [index + offset] : []),
				label: texts.moveColumn,
				onMove: (from, to) => {
					const moved = ids[from - offset];
					const before = ids[to - offset];
					setOrder((current) => {
						const rest = current.filter((column) => column !== moved);
						const at = before === undefined ? rest.length : rest.indexOf(before);
						return [...rest.slice(0, at), moved, ...rest.slice(at)];
					});
				}
			});
		}
		return () => {
			MovableColumns.detach(id);
			table.destroy();
			instance.current = null;
		};
		// the instance starts over when what it renders with changes
	}, [licensed, id, columnsKey, engine, filler, heightKey, selection, infinite, adjustToParent,
		resizableColumns, movableColumns]);

	// another page, page size, sort or filter loads the rows again; the last three from the start
	const queried = useRef({ page, pageSize, sort: JSON.stringify(sort), filter });
	useEffect(() => {
		const before = queried.current;
		const now = { page, pageSize, sort: JSON.stringify(sort), filter };
		queried.current = now;
		if (before.page === now.page && before.pageSize === now.pageSize &&
			before.sort === now.sort && before.filter === now.filter) {
			return;
		}
		const restart = before.pageSize !== now.pageSize || before.sort !== now.sort ||
			before.filter !== now.filter;
		if (restart && page !== 0) {
			setPage(0);
			return;
		}
		startOver.current = true;
		instance.current?.load();
	}, [page, pageSize, sort, filter]);

	// another list of rows is shown, on the page shown unless it has no rows any more
	const listed = useRef(props.rows);
	useEffect(() => {
		if (listed.current === props.rows) {
			return;
		}
		listed.current = props.rows;
		instance.current?.load();
	}, [props.rows]);

	useEffect(() => props.subscribe?.((message) => {
		const table = instance.current;
		if (message.type === 'updateRow') {
			table?.renderRow(message.key, message.data);
		} else if (message.type === 'updateRows') {
			table?.render(message.rows);
		} else {
			table?.load();
		}
	}), [props.subscribe]);

	useImperativeHandle(ref, () => ({
		reload: () => instance.current?.load() ?? Promise.resolve(),
		renderRow: (key, data) => instance.current?.renderRow(key, data),
		selectedKeys: () => Array.from(instance.current?.selectedKeys ?? [])
	}), []);

	const [veiled, setVeiled] = useState(false);
	useEffect(() => {
		if (!busy) {
			setVeiled(false);
			return;
		}
		const timer = window.setTimeout(() => setVeiled(true), VEIL_DELAY);
		return () => window.clearTimeout(timer);
	}, [busy]);

	if (licensed === null) {
		return null;
	}
	if (!licensed) {
		return <div className={UNLICENSED_CLASS} role="note" data-component="DataTable">
			{texts.unlicensed}</div>;
	}

	const pages = Math.max(1, Math.ceil(itemCount / pageSize));
	const go = (target: number) => (event: MouseEvent) => {
		event.preventDefault();
		if (target >= 0 && target < pages && target !== page) {
			setPage(target);
		}
	};
	const sortBy = (property: string) => (event: MouseEvent) => {
		event.preventDefault();
		const next = { property, ascending: sort?.property === property ? !sort.ascending : true };
		setSort(next);
		latest.current.onSortChange?.(next);
	};
	const link = (target: number, className: string, label: ReactNode, title: string,
		rel?: string) => target === page || target < 0 || target >= pages ?
		<a className={className || undefined} rel={rel} {...{ disabled: 'disabled' }} title={title}>{label}</a> :
		<a className={className || undefined} rel={rel} href="#" title={title} onClick={go(target)}>{label}</a>;
	const label = texts.showing.replace('{0}', String(range.count ? range.first + 1 : 0))
		.replace('{1}', String(range.first + range.count)).replace('{2}', String(itemCount));
	const colSpan = visible.length + (withSelection ? 1 : 0) + (filler ? 1 : 0);
	const hideable = columns.filter((column) => column.hideable);

	const navigation = (
		<tr className="navigation">
			<td colSpan={colSpan}>
				<div className="dynamic-data-table-navigation">
					{toolbar && <div className="dynamic-data-table-navigation-prefix">{toolbar}</div>}
					<div className="dynamic-data-table-paging">
						<div className="navigatorLabel"><div>{label}</div></div>
						{!infinite && (
							<div className="navigator"><div>
								{link(0, 'first', '<<', texts.first)}
								{link(page - 1, 'prev', '<', texts.previous, 'prev')}
								{pageLinks(page, pages).map((target) => (
									<span className="goto" key={target}>
										{link(target, '', <span>{target + 1}</span>,
											texts.page.replace('{0}', String(target + 1)))}
									</span>
								))}
								{link(page + 1, 'next', '>', texts.next, 'next')}
								{link(pages - 1, 'last', '>>', texts.last)}
							</div></div>
						)}
					</div>
					<div className="dynamic-data-table-actions">
						{actions}
						{columnChooser && hideable.length > 0 && (
							<span>
								<details className="dynamic-data-table-column-chooser">
									<summary className="dynamic-data-table-action" title={texts.columns}
										aria-label={texts.columns}><ColumnsIcon /></summary>
									<div className="dynamic-data-table-column-chooser-menu" role="group"
										aria-label={texts.columns}>
										<ul>
											{hideable.map((column) => (
												<li key={column.id}>
													<button type="button" className="dynamic-data-table-column-chooser-column"
														data-dt-column={column.id} aria-pressed={shown.has(column.id)}>
														<span className="dynamic-data-table-column-chooser-tick"
															aria-hidden="true">✓</span>
														<span>{column.header}</span>
													</button>
												</li>
											))}
										</ul>
										<button type="button" data-dt-apply="true"
											className="dynamic-data-table-button dynamic-data-table-button-primary">
											{texts.apply}
										</button>
									</div>
								</details>
							</span>
						)}
					</div>
					{!infinite && (
						<label className="dynamic-data-table-page-size">
							<span>{texts.rowsPerPage}</span>{' '}
							<Dropdown options={pageSizes.map((size) => ({ value: String(size),
								label: String(size) }))} value={String(pageSize)}
								onChange={(value) => setPageSize(Number(value))} />
						</label>
					)}
				</div>
			</td>
		</tr>
	);

	return (
		<div className="dynamic-data-table-host">
			<table id={id} className={['dynamic-dataview dynamic-data-table', className]
				.filter(Boolean).join(' ')} cellSpacing={0}>
				<thead>
					{navigation}
					<tr className="dynamic-data-table-headers">
						{withSelection && (
							<th className="dynamic-data-table-selection" data-resizable="false">
								<div className="dynamic-data-table-header">
									<span className="dynamic-data-table-sort-title">
										{selection === 'many' && <input type="checkbox"
											className="dynamic-data-table-select-page"
											data-dt-action={SELECTION_ACTIONS[2]}
											data-dt-action-unchecked={SELECTION_ACTIONS[3]}
											aria-label={texts.selectPage} />}
									</span>
								</div>
							</th>
						)}
						{visible.map((column) => {
							const sorted = column.sort && sort?.property === column.sort ?
								(sort.ascending ? 'ascending' : 'descending') : column.sort ? 'none' :
									undefined;
							return (
								<th key={column.id} data-movable={movableColumns && column.movable ?
									'true' : undefined} data-resizable={column.resizable === false ?
									'false' : undefined} aria-sort={sorted}
									className={sorted ? { none: 'wicket_orderNone', ascending: 'wicket_orderUp',
										descending: 'wicket_orderDown' }[sorted] : undefined}>
									<div className="dynamic-data-table-header">
										{movableColumns && column.movable && (
											<button type="button" className="wicket-col-mover"
												title={texts.moveColumn} aria-label={texts.moveColumn}><Grip /></button>
										)}
										{column.sort ? (
											<>
												<a className="dynamic-data-table-sort" href="#"
													onClick={sortBy(column.sort)}>
													<span className="dynamic-data-table-sort-title">{column.header}</span>
												</a>
												<a className="dynamic-data-table-sort-arrows" aria-hidden="true"
													tabIndex={-1} href="#" onClick={sortBy(column.sort)}>
													<span className="dynamic-data-table-sort-asc">▲</span>
													<span className="dynamic-data-table-sort-desc">▼</span>
												</a>
											</>
										) : <span className="dynamic-data-table-sort-title">{column.header}</span>}
									</div>
								</th>
							);
						})}
						{filler && <th className="dynamic-data-table-filler" data-resizable-skip="true"
							aria-hidden="true"></th>}
					</tr>
				</thead>
				<tbody></tbody>
				<tfoot>{navigation}</tfoot>
			</table>
			{veiled && <div className="dynamic-data-table-veil" aria-hidden="true" />}
		</div>
	);
});
