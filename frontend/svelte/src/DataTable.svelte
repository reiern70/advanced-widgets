<!--
  Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<!--
  @component
  A dynamic data table: Svelte renders the table's skeleton in the markup of the Wicket table, the
  navigation, the headers and the column chooser, and the core renders the rows from JSON with the
  templates of the columns into the body Svelte leaves empty. Sorting, paging or scrolling
  infinitely, the filter and the page size reload the rows through loadRows; the columns can be
  moved, resized, hidden and shown. Bound with bind:this, the component reloads the rows with
  reload(), repaints one with renderRow(key, data), tells the selected rows with selectedKeys()
  and deselects them all with clearSelection(), as the button of the count of the selected rows
  in the navigation does.
-->
<script lang="ts">
	import { untrack, type Snippet } from 'svelte';
	import { CLEAR_SELECTION_ICON, DATA_TABLE_TEXTS, DynamicDataTable, localRows,
		MovableColumns, pageLinks, ResizableColumns, selectionColumn, SELECTION_ACTIONS,
		SHOW_COLUMNS, type DataTableColumnDef, type DataTableData,
		type DataTableFilter, type DataTablePush, type DataTableQuery, type DataTableRow,
		type DataTableSort, type DataTableTexts, type LookPart, partClasses,
		type ResizeMode } from '@antilia/core';

	import Dropdown from './Dropdown.svelte';
	import { getLook } from './look.js';

	const look = getLook();
	const css = (...parts: LookPart[]) => partClasses(look, ...parts);

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
		/**
		 * Whether going to another page, or choosing another page size, deselects every row; false
		 * by default, the selection surviving paging.
		 */
		clearSelectionOnNavigation?: boolean;
		/**
		 * Whether the navigation shows how many rows are selected, while any are, with a button
		 * that deselects every row; true by default.
		 */
		selectionCount?: boolean;
		/** Where the navigation is shown: above the headers, the default, below the rows or both. */
		navigationPosition?: 'top' | 'bottom' | 'both';
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
		selectionCount = true,
		clearSelectionOnNavigation = false,
		navigationPosition = 'top',
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
		id: givenId,
		class: className = ''
	}: Props = $props();

	/** The time a load may take before the veil shows, as Wicket's veil of the table waits. */
	const VEIL_DELAY = 300;

	const GRIP = 'M2 1h2v2H2zm4 0h2v2H6zM2 7h2v2H2zm4 0h2v2H6zm-4 6h2v2H2zm4 0h2v2H6z';

	const generatedId = $props.id();
	const id = $derived(givenId ?? 'data-table-' + generatedId.replace(/[^\w-]/g, ''));
	const texts = $derived<DataTableTexts>({ ...DATA_TABLE_TEXTS, ...givenTexts });

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
	let selected = $state(0);

	let instance: DynamicDataTable | null = null;
	let shownRows = 0;
	let startOver = false;

	const byId = $derived(new Map(columns.map((column) => [column.id, column])));
	const visible = $derived(order.filter((column) => shown.includes(column) && byId.has(column))
		.map((column) => byId.get(column)!));
	const withSelection = $derived(selection !== 'none');
	const columnsKey = $derived(JSON.stringify((withSelection ? [selectionColumn(texts, look)] : [])
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

	function reportSelection(table: DynamicDataTable) {
		selected = table.selectedKeys.size;
		onselectionchange?.(Array.from(table.selectedKeys));
	}

	function act(table: DynamicDataTable, action: string, key: string | null) {
		if (SELECTION_ACTIONS.includes(action)) {
			if (selection === 'one' && action === SELECTION_ACTIONS[0] && key !== null) {
				table.selectedKeys = new Set([key]);
			}
			reportSelection(table);
		} else if (action === SHOW_COLUMNS) {
			const chosen = new Set((key ?? '').split(',').filter(Boolean));
			shown = columns.filter((column) => !column.hideable || chosen.has(column.id))
				.map((column) => column.id);
		} else {
			onaction?.(action, key);
		}
	}

	// what the instance renders with, as text: props given again with the same values keep it
	const setup = $derived(JSON.stringify({
		config: { id, columns: JSON.parse(columnsKey), engine, filler,
			bodyHeight: $state.snapshot(bodyHeight), selection, infinite,
			layout: adjustToParent ? { adjustToParent } : undefined,
			resize: resizableColumns ? {} : undefined },
		mode: resizableColumns,
		movable: movableColumns,
		ids: visible.map((column) => column.id),
		movableIndexes: visible.flatMap((column, index) => column.movable ?
			[index + (withSelection ? 1 : 0)] : []),
		offset: withSelection ? 1 : 0,
		moveLabel: texts.moveColumn
	}));

	// the instance starts over when what it renders with changes
	$effect(() => {
		const { config, mode, movable, ids, movableIndexes, offset, moveLabel } =
			JSON.parse(setup) as {
				config: ConstructorParameters<typeof DynamicDataTable>[0];
				mode: ResizeMode | false;
				movable: boolean;
				ids: string[];
				movableIndexes: number[];
				offset: number;
				moveLabel: string;
			};
		return untrack(() => {
			const table: DynamicDataTable = new DynamicDataTable({ ...config, look,
				onBusy: (value) => (busy = value), load },
				(action, key) => act(table, action, key), () => (itemCount = table.itemCount ?? 0));
			instance = table;
			selected = table.selectedKeys.size;
			if (mode) {
				ResizableColumns.attach(config.id, { mode, label: texts.resizeColumn });
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

	/** Deselects every row, unchecking those shown; the rows loaded next come unchecked. */
	export function clearSelection(): void {
		if (instance && instance.selectedKeys.size > 0) {
			instance.selectedKeys = new Set();
			instance.selectRows(false);
			reportSelection(instance);
		}
	}

	function choosePageSize(value: string) {
		if (clearSelectionOnNavigation && Number(value) !== pageSize) {
			clearSelection();
		}
		pageSize = Number(value);
	}

	function go(target: number, event: MouseEvent) {
		event.preventDefault();
		if (target >= 0 && target < pages && target !== page) {
			if (clearSelectionOnNavigation) {
				clearSelection();
			}
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

	const ORDER_PARTS = { none: 'table-header-unsorted', ascending: 'table-header-ascending',
		descending: 'table-header-descending' } as const;

	/** A link of the navigator that leads nowhere, as Wicket's disabled links are. */
	const DISABLED: Record<string, string> = { disabled: 'disabled' };
</script>

{#snippet link(target: number, linkClass: string, text: string, title: string, rel?: string)}
	{#if target === page || target < 0 || target >= pages}
		<a class={[linkClass, css('table-page-link')]} {rel} {...DISABLED} {title}>{text}</a>
	{:else}
		<a class={[linkClass, css('table-page-link')]} {rel} href="#top" {title}
			onclick={(event) => go(target, event)}>{text}</a>
	{/if}
{/snippet}

{#snippet navigation()}
	<tr class={css('table-navigation-row')}>
		<td colspan={colSpan}>
			<div class={css('table-navigation')}>
				{#if toolbar}
					<div class={css('table-navigation-prefix')}>{@render toolbar()}</div>
				{/if}
				{#if !infinite}
					<label class={css('table-page-size')}>
						<span>{texts.rowsPerPage}</span>
						<Dropdown options={pageSizes.map((size) => ({ value: String(size),
							label: String(size) }))} value={String(pageSize)}
							onchange={(value) => choosePageSize(String(value))} />
					</label>
				{/if}
				<div class={css('table-paging')}>
					<div class={css('table-navigator-label')}><div>{label}</div></div>
					{#if !infinite}
						<div class={css('table-navigator')}><div class={css('table-pages') || undefined}>
							{@render link(0, 'first', '<<', texts.first)}
							{@render link(page - 1, 'prev', '<', texts.previous, 'prev')}
							{#each pageLinks(page, pages) as target (target)}
								<span class={css('table-page')}>
									{#if target === page}
										<a class={css('table-page-link')} {...DISABLED}
											title={texts.page.replace('{0}', String(target + 1))}
											><span>{target + 1}</span></a>
									{:else}
										<a class={css('table-page-link')} href="#top"
											title={texts.page.replace('{0}', String(target + 1))}
											onclick={(event) => go(target, event)}><span>{target + 1}</span></a>
									{/if}
								</span>
							{/each}
							{@render link(page + 1, 'next', '>', texts.next, 'next')}
							{@render link(pages - 1, 'last', '>>', texts.last)}
						</div></div>
					{/if}
				</div>
				<div class={css('table-actions')}>
					{@render actions?.()}
					{#if withSelection && selectionCount && selected > 0}
						<span class={css('table-selection-count')}>
							<span role="status">{texts.selectedCount.replace('{0}', String(selected))}</span>
							<button type="button" class={css('table-selection-clear')}
								title={texts.clearSelection} aria-label={texts.clearSelection}
								onclick={clearSelection}>
								<svg aria-hidden="true" focusable="false" viewBox="0 0 24 24" width="18"
									height="18" fill="none" stroke="currentColor" stroke-width="2"
									stroke-linecap="round" stroke-linejoin="round"><path
									d={CLEAR_SELECTION_ICON} /></svg>
							</button>
						</span>
					{/if}
					{#if columnChooser && hideable.length > 0}
						<span>
							<details class={css('table-column-chooser')}>
								<summary class={css('table-action')} title={texts.columns}
									aria-label={texts.columns}>
									<svg aria-hidden="true" focusable="false" viewBox="0 0 24 24" width="18"
										height="18" fill="none" stroke="currentColor" stroke-width="2"
										stroke-linecap="round"><path d="M4 5h16v14H4z M10 5v14 M15 5v14" /></svg>
								</summary>
								<div class={css('table-column-chooser-menu')} role="group"
									aria-label={texts.columns}>
									<ul>
										{#each hideable as column (column.id)}
											<li>
												<button type="button" class={css('table-column-chooser-column')}
													data-dt-column={column.id} aria-pressed={shown.includes(column.id)}>
													<span class={css('table-column-chooser-tick')}
														aria-hidden="true">✓</span>
													<span>{column.header}</span>
												</button>
											</li>
										{/each}
									</ul>
									<button type="button" data-dt-apply="true"
										class={css('table-button', 'table-button-primary')}>
										{texts.apply}
									</button>
								</div>
							</details>
						</span>
					{/if}
				</div>
			</div>
		</td>
	</tr>
{/snippet}

<div class={css('table-host')}>
	<table {id} class={['ant-dataview', css('table'), className]} cellspacing="0">
		<thead>
			{#if navigationPosition !== 'bottom'}{@render navigation()}{/if}
			<tr class={css('table-headers')}>
				{#if withSelection}
					<th class={css('table-selection')} data-resizable="false">
						<div class={css('table-header')}>
							<span class={css('table-sort-title')}>
								{#if selection === 'many'}
									<input type="checkbox" class={css('table-select-page')}
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
						aria-sort={sorting} class={sorting ? css(ORDER_PARTS[sorting]) : undefined}>
						<div class={css('table-header')}>
							{#if movableColumns && column.movable}
								<button type="button" class={css('column-mover')} title={texts.moveColumn}
									aria-label={texts.moveColumn}>
									<svg aria-hidden="true" focusable="false" viewBox="0 0 10 16" width="8"
										height="13"><path fill="currentColor" d={GRIP} /></svg>
								</button>
							{/if}
							{#if column.sort}
								{@const property = column.sort}
								<a class={css('table-sort')} href="#top"
									onclick={(event) => sortBy(property, event)}>
									<span class={css('table-sort-title')}>{column.header}</span>
								</a>
								<a class={css('table-sort-arrows')} aria-hidden="true" tabindex="-1"
									href="#top" onclick={(event) => sortBy(property, event)}>
									<span class={css('table-sort-ascending')}>▲</span>
									<span class={css('table-sort-descending')}>▼</span>
								</a>
							{:else}
								<span class={css('table-sort-title')}>{column.header}</span>
							{/if}
						</div>
					</th>
				{/each}
				{#if filler}
					<th class={css('table-filler')} data-resizable-skip="true" aria-hidden="true"></th>
				{/if}
			</tr>
		</thead>
		<tbody></tbody>
		{#if navigationPosition !== 'top'}<tfoot>{@render navigation()}</tfoot>{/if}
	</table>
	{#if veiled}
		<div class={css('table-veil')} aria-hidden="true"></div>
	{/if}
</div>
