<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  @component
  A dynamic data table: Svelte renders the table's skeleton in the markup of the Wicket table, the
  navigation, the headers and the column chooser, and the core renders the rows from JSON with the
  templates of the columns into the body Svelte leaves empty. Sorting, paging or scrolling
  infinitely, the filter and the page size reload the rows through loadRows; the columns can be
  moved, resized, hidden and shown. Bound with bind:this, the component reloads the rows with
  reload(), repaints one with renderRow(key, data) and tells the selected rows with selectedKeys().
  A private component: without a license key activating it, it shows the placeholder Wicket shows.
-->
<script lang="ts">
	import { untrack, type Snippet } from 'svelte';
	import { DATA_TABLE_COMPONENTS, DATA_TABLE_TEXTS, DynamicDataTable, isLicensed, localRows,
		MovableColumns, pageLinks, ResizableColumns, selectionColumn, SELECTION_ACTIONS,
		SHOW_COLUMNS, UNLICENSED_CLASS, type DataTableColumnDef, type DataTableData,
		type DataTableFilter, type DataTablePush, type DataTableQuery, type DataTableRow,
		type DataTableSort, type DataTableTexts, type ResizeMode } from '@advanced-widgets/core';

	import Dropdown from './Dropdown.svelte';

	type Props = {
		/** The columns, in their first order; the user may move, hide and show them. */
		columns: DataTableColumnDef[];
		/**
		 * Loads the rows asked for: [{key, data}] and how many there are in all, from a mounted
		 * resource for example. A table given its rows instead needs none.
		 */
		loadRows?: (query: DataTableQuery) => Promise<{ rows: DataTableRow[]; itemCount: number }>;
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
		pageSizes?: number[];
		/** Whether the rows load as the table scrolls, in place of pages. */
		infinite?: boolean;
		selection?: 'none' | 'one' | 'many';
		onselectionchange?: (keys: string[]) => void;
		/** Called with the action of a clicked [data-dt-action] element and the key of its row. */
		onaction?: (action: string, key: string | null) => void;
		/** The text the rows are filtered by, handed to loadRows; another one starts over. */
		filter?: string;
		/** The sort at first. */
		sort?: DataTableSort | null;
		onsortchange?: (sort: DataTableSort | null) => void;
		movableColumns?: boolean;
		/** How columns are resized, or false when they are not. */
		resizableColumns?: ResizeMode | false;
		columnChooser?: boolean;
		/** The height of the body, which then scrolls the rows. */
		bodyHeight?: { value: number; unit: string };
		adjustToParent?: boolean;
		engine?: string;
		/** The texts of the table, in English by default. */
		texts?: Partial<DataTableTexts>;
		/** What the navigation shows before the paging, a filter for example. */
		toolbar?: Snippet;
		/** The buttons of the toolbar, before the column chooser. */
		actions?: Snippet;
		/** Subscribes the table to pushed changes of its rows, returning the unsubscription. */
		subscribe?: (listener: (message: DataTablePush) => void) => () => void;
		/** A license key of its own; the one given to activate() when left out. */
		licenseKey?: string;
		id?: string;
		class?: string;
	};

	let {
		columns,
		loadRows,
		rows,
		filterRows,
		rowsPerPage = 25,
		pageSizes = [25, 50, 100],
		infinite = false,
		selection = 'none',
		onselectionchange,
		onaction,
		filter,
		sort: initialSort = null,
		onsortchange,
		movableColumns = true,
		resizableColumns = false,
		columnChooser = true,
		bodyHeight,
		adjustToParent,
		engine,
		texts: givenTexts,
		toolbar,
		actions,
		subscribe,
		licenseKey,
		id: givenId,
		class: className = ''
	}: Props = $props();

	/** The time a load may take before the veil shows, as Wicket's veil of the table waits. */
	const VEIL_DELAY = 300;

	const GRIP = 'M2 1h2v2H2zm4 0h2v2H6zM2 7h2v2H2zm4 0h2v2H6zm-4 6h2v2H2zm4 0h2v2H6z';

	const generatedId = $props.id();
	const id = $derived(givenId ?? 'data-table-' + generatedId.replace(/[^\w-]/g, ''));
	const texts = $derived<DataTableTexts>({ ...DATA_TABLE_TEXTS, ...givenTexts });

	let licensed = $state<boolean | null>(null);
	let order = $state(untrack(() => columns.map((column) => column.id)));
	let shown = $state(untrack(() => columns.filter((column) => column.visible !== false)
		.map((column) => column.id)));
	let page = $state(0);
	let pageSize = $state(untrack(() => rowsPerPage));
	let sort = $state<DataTableSort | null>(untrack(() => initialSort));
	let itemCount = $state(0);
	let range = $state({ first: 0, count: 0 });
	let busy = $state(false);
	let veiled = $state(false);

	let instance: DynamicDataTable | null = null;
	let shownRows = 0;
	let startOver = false;

	$effect(() => {
		const key = licenseKey;
		let live = true;
		isLicensed(DATA_TABLE_COMPONENTS, key).then((active) => {
			if (live) {
				licensed = active;
			}
		});
		return () => {
			live = false;
		};
	});

	const byId = $derived(new Map(columns.map((column) => [column.id, column])));
	const visible = $derived(order.filter((column) => shown.includes(column) && byId.has(column))
		.map((column) => byId.get(column)!));
	const withSelection = $derived(selection !== 'none');
	const columnsKey = $derived(JSON.stringify((withSelection ? [selectionColumn(texts)] : [])
		.concat(visible.map((column) => ({ template: column.template,
			cssClass: column.cssClass })))));
	const filler = $derived(Boolean(bodyHeight));
	const hideable = $derived(columns.filter((column) => column.hideable));
	const pages = $derived(Math.max(1, Math.ceil(itemCount / pageSize)));
	const label = $derived(texts.showing.replace('{0}', String(range.count ? range.first + 1 : 0))
		.replace('{1}', String(range.first + range.count)).replace('{2}', String(itemCount)));
	const colSpan = $derived(visible.length + (withSelection ? 1 : 0) + (filler ? 1 : 0));

	async function load(more: boolean): Promise<DataTableData> {
		const current = untrack(() => ({ page, pageSize, sort: $state.snapshot(sort), filter,
			infinite }));
		if (!more && startOver) {
			shownRows = 0;
		}
		const first = more ? shownRows : current.infinite ? 0 : current.page * current.pageSize;
		const count = more || !current.infinite ? current.pageSize :
			Math.max(shownRows, current.pageSize);
		const load = untrack(() => loadRows ?? localRows(() => untrack(() => rows) ?? [], filterRows));
		const loaded = await load({ first, count, sort: current.sort, filter: current.filter });
		shownRows = (more ? shownRows : 0) + loaded.rows.length;
		const top = startOver;
		startOver = false;
		itemCount = loaded.itemCount;
		range = current.infinite ? { first: 0, count: shownRows } :
			{ first, count: loaded.rows.length };
		return {
			rows: loaded.rows.map((row, index) => ({ ...row, number: row.number ?? first + index + 1 })),
			itemCount: loaded.itemCount,
			currentPage: current.page,
			top
		};
	}

	function act(table: DynamicDataTable, action: string, key: string | null) {
		if (SELECTION_ACTIONS.includes(action)) {
			if (selection === 'one' && action === SELECTION_ACTIONS[0] && key !== null) {
				table.selectedKeys = new Set([key]);
			}
			onselectionchange?.(Array.from(table.selectedKeys));
		} else if (action === SHOW_COLUMNS) {
			const chosen = new Set((key ?? '').split(',').filter(Boolean));
			shown = columns.filter((column) => !column.hideable || chosen.has(column.id))
				.map((column) => column.id);
		} else {
			onaction?.(action, key);
		}
	}

	// the instance starts over when what it renders with changes
	$effect(() => {
		if (!licensed) {
			return;
		}
		const config = { id, columns: JSON.parse(columnsKey), engine, filler,
			bodyHeight: $state.snapshot(bodyHeight), selection, infinite,
			layout: adjustToParent ? { adjustToParent } : undefined,
			resize: resizableColumns ? {} : undefined };
		const mode = resizableColumns;
		const movable = movableColumns;
		const ids = visible.map((column) => column.id);
		const movableIndexes = visible.flatMap((column, index) => column.movable ?
			[index + (withSelection ? 1 : 0)] : []);
		const offset = withSelection ? 1 : 0;
		const moveLabel = texts.moveColumn;
		return untrack(() => {
			const table: DynamicDataTable = new DynamicDataTable({ ...config,
				onBusy: (value) => (busy = value), load },
				(action, key) => act(table, action, key), () => (itemCount = table.itemCount ?? 0));
			instance = table;
			if (mode) {
				ResizableColumns.attach(config.id, { mode });
			}
			if (movable) {
				MovableColumns.attach(config.id, {
					movable: movableIndexes,
					label: moveLabel,
					onMove: (from, to) => {
						const moved = ids[from - offset];
						const before = ids[to - offset];
						const rest = order.filter((column) => column !== moved);
						const at = before === undefined ? rest.length : rest.indexOf(before);
						order = [...rest.slice(0, at), moved, ...rest.slice(at)];
					}
				});
			}
			return () => {
				MovableColumns.detach(config.id);
				table.destroy();
				instance = null;
			};
		});
	});

	// another page, page size, sort or filter loads the rows again; the last three from the start
	let queried = { page: 0, pageSize: 0, sort: '', filter: undefined as string | undefined,
		first: true };
	$effect(() => {
		const now = { page, pageSize, sort: JSON.stringify(sort), filter, first: false };
		untrack(() => {
			const before = queried;
			queried = now;
			if (before.first) {
				return;
			}
			if (before.page === now.page && before.pageSize === now.pageSize &&
				before.sort === now.sort && before.filter === now.filter) {
				return;
			}
			const restart = before.pageSize !== now.pageSize || before.sort !== now.sort ||
				before.filter !== now.filter;
			if (restart && page !== 0) {
				page = 0;
				return;
			}
			startOver = true;
			instance?.load();
		});
	});

	// another list of rows is shown, on the page shown unless it has no rows any more
	let listed: readonly DataTableRow[] | undefined | null = null;
	$effect(() => {
		const now = rows;
		untrack(() => {
			const before = listed;
			listed = now;
			if (before !== null && before !== now) {
				instance?.load();
			}
		});
	});

	$effect(() => subscribe?.((message) => {
		if (message.type === 'updateRow') {
			instance?.renderRow(message.key, message.data);
		} else if (message.type === 'updateRows') {
			instance?.render(message.rows);
		} else {
			instance?.load();
		}
	}));

	$effect(() => {
		if (!busy) {
			veiled = false;
			return;
		}
		const timer = window.setTimeout(() => (veiled = true), VEIL_DELAY);
		return () => window.clearTimeout(timer);
	});

	/** Loads the rows shown again. */
	export function reload(): Promise<void> {
		return instance?.load() ?? Promise.resolve();
	}

	/** Repaints one row with new data, in place; a row the table does not show is ignored. */
	export function renderRow(key: string, data: unknown): void {
		instance?.renderRow(key, data);
	}

	/** The keys of the selected rows. */
	export function selectedKeys(): string[] {
		return Array.from(instance?.selectedKeys ?? []);
	}

	function go(target: number, event: MouseEvent) {
		event.preventDefault();
		if (target >= 0 && target < pages && target !== page) {
			page = target;
		}
	}

	function sortBy(property: string, event: MouseEvent) {
		event.preventDefault();
		sort = { property, ascending: sort?.property === property ? !sort.ascending : true };
		onsortchange?.($state.snapshot(sort));
	}

	function sorted(column: DataTableColumnDef): 'none' | 'ascending' | 'descending' | undefined {
		return column.sort && sort?.property === column.sort ?
			(sort.ascending ? 'ascending' : 'descending') : column.sort ? 'none' : undefined;
	}

	const ORDER_CLASSES = { none: 'wicket_orderNone', ascending: 'wicket_orderUp',
		descending: 'wicket_orderDown' };

	/** A link of the navigator that leads nowhere, as Wicket's disabled links are. */
	const DISABLED: Record<string, string> = { disabled: 'disabled' };
</script>

{#snippet link(target: number, linkClass: string, text: string, title: string, rel?: string)}
	{#if target === page || target < 0 || target >= pages}
		<a class={linkClass || undefined} {rel} {...DISABLED} {title}>{text}</a>
	{:else}
		<a class={linkClass || undefined} {rel} href="#top" {title}
			onclick={(event) => go(target, event)}>{text}</a>
	{/if}
{/snippet}

{#snippet navigation()}
	<tr class="navigation">
		<td colspan={colSpan}>
			<div class="dynamic-data-table-navigation">
				{#if toolbar}
					<div class="dynamic-data-table-navigation-prefix">{@render toolbar()}</div>
				{/if}
				<div class="dynamic-data-table-paging">
					<div class="navigatorLabel"><div>{label}</div></div>
					{#if !infinite}
						<div class="navigator"><div>
							{@render link(0, 'first', '<<', texts.first)}
							{@render link(page - 1, 'prev', '<', texts.previous, 'prev')}
							{#each pageLinks(page, pages) as target (target)}
								<span class="goto">
									{#if target === page}
										<a {...DISABLED} title={texts.page.replace('{0}',
											String(target + 1))}><span>{target + 1}</span></a>
									{:else}
										<a href="#top" title={texts.page.replace('{0}', String(target + 1))}
											onclick={(event) => go(target, event)}><span>{target + 1}</span></a>
									{/if}
								</span>
							{/each}
							{@render link(page + 1, 'next', '>', texts.next, 'next')}
							{@render link(pages - 1, 'last', '>>', texts.last)}
						</div></div>
					{/if}
				</div>
				<div class="dynamic-data-table-actions">
					{@render actions?.()}
					{#if columnChooser && hideable.length > 0}
						<span>
							<details class="dynamic-data-table-column-chooser">
								<summary class="dynamic-data-table-action" title={texts.columns}
									aria-label={texts.columns}>
									<svg aria-hidden="true" focusable="false" viewBox="0 0 24 24" width="18"
										height="18" fill="none" stroke="currentColor" stroke-width="2"
										stroke-linecap="round"><path d="M4 5h16v14H4z M10 5v14 M15 5v14" /></svg>
								</summary>
								<div class="dynamic-data-table-column-chooser-menu" role="group"
									aria-label={texts.columns}>
									<ul>
										{#each hideable as column (column.id)}
											<li>
												<button type="button" class="dynamic-data-table-column-chooser-column"
													data-dt-column={column.id} aria-pressed={shown.includes(column.id)}>
													<span class="dynamic-data-table-column-chooser-tick"
														aria-hidden="true">✓</span>
													<span>{column.header}</span>
												</button>
											</li>
										{/each}
									</ul>
									<button type="button" data-dt-apply="true"
										class="dynamic-data-table-button dynamic-data-table-button-primary">
										{texts.apply}
									</button>
								</div>
							</details>
						</span>
					{/if}
				</div>
				{#if !infinite}
					<label class="dynamic-data-table-page-size">
						<span>{texts.rowsPerPage}</span>
						<Dropdown options={pageSizes.map((size) => ({ value: String(size),
							label: String(size) }))} value={String(pageSize)}
							onchange={(value) => (pageSize = Number(value))} />
					</label>
				{/if}
			</div>
		</td>
	</tr>
{/snippet}

{#if licensed === false}
	<div class={UNLICENSED_CLASS} role="note" data-component="DataTable">{texts.unlicensed}</div>
{:else if licensed}
	<div class="dynamic-data-table-host">
		<table {id} class={['dynamic-dataview dynamic-data-table', className]} cellspacing="0">
			<thead>
				{@render navigation()}
				<tr class="dynamic-data-table-headers">
					{#if withSelection}
						<th class="dynamic-data-table-selection" data-resizable="false">
							<div class="dynamic-data-table-header">
								<span class="dynamic-data-table-sort-title">
									{#if selection === 'many'}
										<input type="checkbox" class="dynamic-data-table-select-page"
											data-dt-action={SELECTION_ACTIONS[2]}
											data-dt-action-unchecked={SELECTION_ACTIONS[3]}
											aria-label={texts.selectPage} />
									{/if}
								</span>
							</div>
						</th>
					{/if}
					{#each visible as column (column.id)}
						{@const sorting = sorted(column)}
						<th data-movable={movableColumns && column.movable ? 'true' : undefined}
							data-resizable={column.resizable === false ? 'false' : undefined}
							aria-sort={sorting} class={sorting ? ORDER_CLASSES[sorting] : undefined}>
							<div class="dynamic-data-table-header">
								{#if movableColumns && column.movable}
									<button type="button" class="wicket-col-mover" title={texts.moveColumn}
										aria-label={texts.moveColumn}>
										<svg aria-hidden="true" focusable="false" viewBox="0 0 10 16" width="8"
											height="13"><path fill="currentColor" d={GRIP} /></svg>
									</button>
								{/if}
								{#if column.sort}
									{@const property = column.sort}
									<a class="dynamic-data-table-sort" href="#top"
										onclick={(event) => sortBy(property, event)}>
										<span class="dynamic-data-table-sort-title">{column.header}</span>
									</a>
									<a class="dynamic-data-table-sort-arrows" aria-hidden="true" tabindex="-1"
										href="#top" onclick={(event) => sortBy(property, event)}>
										<span class="dynamic-data-table-sort-asc">▲</span>
										<span class="dynamic-data-table-sort-desc">▼</span>
									</a>
								{:else}
									<span class="dynamic-data-table-sort-title">{column.header}</span>
								{/if}
							</div>
						</th>
					{/each}
					{#if filler}
						<th class="dynamic-data-table-filler" data-resizable-skip="true" aria-hidden="true"></th>
					{/if}
				</tr>
			</thead>
			<tbody></tbody>
			<tfoot>{@render navigation()}</tfoot>
		</table>
		{#if veiled}
			<div class="dynamic-data-table-veil" aria-hidden="true"></div>
		{/if}
	</div>
{/if}
