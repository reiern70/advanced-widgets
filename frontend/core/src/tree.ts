/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * A tree, or a tree grid, rendering its nodes from JSON with a template, loading the children of
 * a node the first time it is expanded. As a tree it is a WAI-ARIA tree of nested lists, as a tree
 * table a treegrid whose first column holds the tree; both are moved through with the arrow keys,
 * Home and End, and Enter or Space selects a leaf or expands and collapses a folder. Only leaves
 * are selected, unless cfg.selectBranches has folders selected too: a click on a folder opens or
 * closes it, or, with cfg.selectBranches, selects it and opens it if it is closed, its arrow still
 * opening and closing it.
 *
 * Given a height, the tree scrolls itself, with the slim scroll bars of the core; a tree grid keeps
 * its head in place above the rows scrolling. The columns of a tree grid can be made resizable.
 *
 * In Wicket it is the client side of JsonBasedTree and JsonBasedTreeGrid; the children come from
 * cfg.dataUrl, or from cfg.load in React and Svelte.
 */

import { getHost, onRemoving, type Unsubscribe } from './host.js';
import { ANTILIA_LOOK, lookOf, partClasses, toggleParts, type Look, type LookPart } from './look.js';
import { ResizableColumns, type ResizableColumnsOptions,
	type ResizeMode } from './resizable-columns.js';
import { Scrollbar, type ScrollbarHandle, type ScrollbarOptions } from './scrollbar.js';
import { templates, type CompiledTemplate } from './templates.js';

/** A node: its key, unique in the tree, whether it is a leaf, and the data its template renders. */
export interface TreeNode {
	key: string;
	leaf: boolean;
	data: unknown;
	/** The children, when they are sent along; loaded when the node is expanded otherwise. */
	children?: TreeNode[];
	/** The markup of the node's own icon, trusted; the tree's folder or leaf icon when left out. */
	icon?: string;
}

/** A column of a tree grid besides the tree column. */
export interface TreeColumn {
	header: string;
	/** The template of the column's cells, rendered from the node's data. */
	template: string;
	cssClass?: string;
}

/** The icons of a tree, as markup. */
export interface TreeIcons {
	/** The arrow of a folder, pointing to the end of the line; turned down when expanded. */
	toggle: string;
	folder: string;
	folderOpen: string;
	leaf: string;
}

/** What a tree says: while it loads, and when it has no nodes. */
export interface TreeTexts {
	loading: string;
	empty: string;
	/** The placeholder React and Svelte show without a license key activating the tree. */
	unlicensed?: string;
	/** The accessible name of the handles resizing the columns of a tree grid. */
	resizeColumn?: string;
}

/**
 * How a tree loads and renders its nodes.
 */
export interface TreeConfig {
	/** The id of the tree's element. */
	id: string;
	/** The look of the tree; that of the element it is in when left out (see lookOf). */
	look?: Look;
	/** The URL answering with {nodes: TreeNode[]}: the roots, or the children of ?parent=key. */
	dataUrl?: string;
	/** Loads the roots, for null, or the children of a node, in place of dataUrl. */
	load?: (parent: string | null) => Promise<TreeNode[]>;
	/** The name of the template engine. */
	engine?: string;
	/** The template of a node, in the tree column of a tree grid; {{@key}} and {{@level}} are its key and level. */
	template: string;
	/** The header of the tree column and the other columns: a tree grid when given. */
	table?: { header: string; columns: TreeColumn[] };
	/** The name of the tree for screen readers. */
	label?: string;
	/** The keys of the nodes expanded at first. */
	expanded?: string[];
	/** The key of the leaf selected at first. */
	selected?: string | null;
	/** Whether folders are selected too, not only leaves. */
	selectBranches?: boolean;
	/** Icons of the application's own, in place of those of the tree. */
	icons?: Partial<TreeIcons>;
	/** The height of the tree, which then scrolls itself; the page scrolls when left out. */
	height?: { value: number; unit: string };
	/** How the columns of a tree grid are resized; they are not when left out. */
	resizableColumns?: ResizeMode;
	texts: TreeTexts;
}

/** What a tree grid needs of the resizable columns. */
export interface TreeColumnsRegistry {
	attach(table: HTMLTableElement, options?: ResizableColumnsOptions): unknown;
	detach(id: string): void;
}

/**
 * What a tree reports.
 */
export interface TreeCallbacks {
	/** A leaf, or a folder when they are selected too, was selected by the user. */
	select?(key: string, data: unknown): void;
	/** An element of a node with a data-dt-action attribute was clicked. */
	action?(action: string, key: string, data: unknown): void;
	/** The user expanded or collapsed a folder. */
	toggle?(key: string, expanded: boolean): void;
}

const svg = (paths: string, width = '2') => '<svg viewBox="0 0 24 24" width="1em" height="1em" ' +
	'aria-hidden="true" focusable="false" fill="none" stroke="currentColor" stroke-width="' + width +
	'" stroke-linecap="round" stroke-linejoin="round">' + paths + '</svg>';

/** The icons of the trees of Antilia's Advanced Widgets, the same the Wicket trees render. */
export const TREE_ICONS: TreeIcons = {
	toggle: svg('<path d="M9 6l6 6-6 6"/>', '2.5'),
	folder: svg('<path d="M3 6.5A1.5 1.5 0 0 1 4.5 5H9l2 2.5h8.5A1.5 1.5 0 0 1 21 9v8.5a1.5 1.5 ' +
		'0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 17.5z"/>'),
	folderOpen: svg('<path d="M3 17.5v-11A1.5 1.5 0 0 1 4.5 5H9l2 2.5h7A1.5 1.5 0 0 1 19.5 9v1.5"/>' +
		'<path d="M3 17.5l2.6-6.1A1.5 1.5 0 0 1 7 10.5h13.2a1 1 0 0 1 .9 1.4l-2.6 6.2a1.5 1.5 0 0 ' +
		'1-1.4.9H4.5A1.5 1.5 0 0 1 3 17.5z"/>'),
	leaf: svg('<path d="M6 3h8l4 4v13a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z"/>' +
		'<path d="M14 3v4h4M8.5 12h7M8.5 16h7"/>')
};

/** The texts of a tree in English, as the Wicket trees have them. */
export const TREE_TEXTS: Required<TreeTexts> = {
	loading: 'Loading…',
	empty: 'Nothing to show',
	unlicensed: 'This is a private component of Antilia\'s Advanced Widgets: it needs a license key, ' +
		'which you get by registering.',
	resizeColumn: 'Resize column'
};

/** The names a license key activates the trees by: their own and those of the Wicket ones. */
export const TREE_COMPONENTS = ['Tree', 'JsonBasedTree'];

/** The names a license key activates the tree grids by. */
export const TREE_GRID_COMPONENTS = ['TreeGrid', 'JsonBasedTreeGrid', 'JsonBasedTree'];

const ITEM_CLASS = 'antilia-tree-item';

const LOADING_CLASS = 'antilia-tree-toggle--loading';

const SCROLLING_CLASS = 'antilia-tree--scrolling';

/** A node the tree knows of, shown or not. */
interface Entry {
	node: TreeNode;
	parent: string | null;
	level: number;
	/** The keys of the children, null until they are loaded. */
	children: string[] | null;
	expanded: boolean;
	loading: Promise<void> | null;
	/** The item: the li of a tree, the tr of a tree grid. */
	element: HTMLElement | null;
}

/**
 * A tree or a tree grid.
 */
export class JsonTree {
	/** The live trees, by the id of their element. */
	static readonly instances: Record<string, JsonTree> = {};

	/** Gives a tree of a height its scroll bars; Wicket gives those of Wicket.Scrollbar, if any. */
	static attachScrollbar: (element: HTMLElement, options: ScrollbarOptions) =>
		ScrollbarHandle | undefined = (element, options) => Scrollbar.attach(element, options);

	/** The resizable columns of the page; Wicket gives Wicket.ResizableColumns, if any. */
	static resizableColumns: () => TreeColumnsRegistry | undefined = () => ResizableColumns;

	readonly id: string;

	readonly cfg: TreeConfig;

	readonly element: HTMLElement | null;

	/** The look of the tree, whose classes the elements it makes get. */
	private look: Look = ANTILIA_LOOK;

	private readonly callbacks: TreeCallbacks;

	private readonly icons: TreeIcons;

	private readonly entries = new Map<string, Entry>();

	private roots: string[] = [];

	private selected: string | null;

	private node!: CompiledTemplate;

	private cells: CompiledTemplate[] = [];

	/** The list of the roots of a tree, the body of a tree grid. */
	private container!: HTMLElement;

	private status!: HTMLDivElement;

	private busy = 0;

	private scrollbar: ScrollbarHandle | undefined = undefined;

	/** The table of a tree grid whose columns are resizable, once they are. */
	private resizable: HTMLTableElement | null = null;

	private readonly clickHandler = (event: MouseEvent) => this.onClick(event);

	private readonly keyHandler = (event: KeyboardEvent) => this.onKeyDown(event);

	private readonly focusHandler = (event: FocusEvent) => this.onFocusIn(event);

	private stops: Unsubscribe[] = [];

	/**
	 * @param cfg how the tree loads and renders its nodes
	 * @param callbacks what the tree reports
	 * @param roots the roots, with the children of those expanded, when the server sends them
	 *            along; loaded otherwise
	 */
	constructor(cfg: TreeConfig, callbacks?: TreeCallbacks | null, roots?: TreeNode[] | null) {
		JsonTree.instances[cfg.id]?.destroy();

		this.id = cfg.id;
		this.cfg = cfg;
		this.callbacks = callbacks ?? {};
		this.icons = { ...TREE_ICONS, ...cfg.icons };
		this.selected = cfg.selected ?? null;
		this.element = document.getElementById(cfg.id);
		if (!this.element) {
			return;
		}
		this.look = cfg.look ?? lookOf(this.element);
		const engine = templates().engine(cfg.engine);
		if (!engine) {
			return;
		}
		this.node = engine.compile(cfg.template);
		this.cells = (cfg.table?.columns ?? []).map((column) => engine.compile(column.template));

		this.status = document.createElement('div');
		this.status.className = this.css('tree-status');
		this.status.setAttribute('role', 'status');
		if (cfg.table) {
			this.element.replaceChildren(this.buildTable(cfg.table), this.status);
		} else {
			this.container = document.createElement('ul');
			this.container.className = this.css('tree-group', 'tree-roots');
			this.container.setAttribute('role', 'tree');
			if (cfg.label) {
				this.container.setAttribute('aria-label', cfg.label);
			}
			this.element.replaceChildren(this.container, this.status);
		}
		if (cfg.height) {
			this.element.classList.add(SCROLLING_CLASS);
			this.element.style.height = cfg.height.value + cfg.height.unit;
			this.scrollbar = JsonTree.attachScrollbar(this.element,
				cfg.table ? { header: ':scope > table > thead', look: this.look } : { look: this.look });
		}

		this.element.addEventListener('click', this.clickHandler);
		this.element.addEventListener('keydown', this.keyHandler);
		this.element.addEventListener('focusin', this.focusHandler);
		JsonTree.instances[this.id] = this;
		this.stops.push(onRemoving(this.element, () => this.destroy()));

		if (roots) {
			this.show(roots, new Set(cfg.expanded ?? []));
		} else {
			this.reload(cfg.expanded);
		}
	}

	/**
	 * @returns the classes of parts of the tree in its look
	 */
	private css(...parts: (LookPart | false)[]): string {
		return partClasses(this.look, ...parts);
	}

	private isLive(): boolean {
		return JsonTree.instances[this.id] === this;
	}

	/**
	 * @returns the table of a tree grid, its body the container of the rows
	 */
	private buildTable(table: NonNullable<TreeConfig['table']>): HTMLTableElement {
		const element = document.createElement('table');
		element.id = this.id + '-grid';
		element.className = this.css('tree-grid-grid');
		element.setAttribute('role', 'treegrid');
		if (this.cfg.label) {
			element.setAttribute('aria-label', this.cfg.label);
		}
		const head = element.createTHead().insertRow();
		[table.header, ...table.columns.map((column) => column.header)].forEach((text, index) => {
			const th = document.createElement('th');
			th.scope = 'col';
			th.textContent = text;
			const cssClass = index === 0 ? this.css('tree-grid-tree-cell') :
				table.columns[index - 1].cssClass;
			if (cssClass) {
				th.className = cssClass;
			}
			head.appendChild(th);
		});
		this.container = element.createTBody();
		return element;
	}

	/**
	 * Loads the children of a node, or the roots.
	 *
	 * @param parent the key of the node, null for the roots
	 * @returns a promise of the nodes, or of nothing if the tree is gone
	 */
	fetch(parent: string | null): Promise<TreeNode[] | undefined> {
		let loaded: Promise<TreeNode[]>;
		if (this.cfg.load) {
			loaded = this.cfg.load(parent);
		} else {
			const url = this.cfg.dataUrl ?? '';
			loaded = window.fetch(parent === null ? url : url + (url.indexOf('?') >= 0 ? '&' : '?') +
				'parent=' + encodeURIComponent(parent), {
				credentials: 'same-origin',
				headers: { Accept: 'application/json' }
			}).then((response) => {
				if (!response.ok) {
					throw new Error('HTTP ' + response.status);
				}
				return response.json() as Promise<{ nodes: TreeNode[] }>;
			}).then((json) => json.nodes);
		}
		return loaded.then((nodes) => this.isLive() ? nodes : undefined);
	}

	/**
	 * Loads the roots again, and the children of the folders that are expanded, keeping the
	 * selection, as after the nodes changed.
	 *
	 * @param expanded the keys of the folders to expand, those expanded now when left out
	 */
	reload(expanded?: string[]): Promise<void> {
		const keep = new Set(expanded ?? Array.from(this.entries.values())
			.filter((entry) => entry.expanded).map((entry) => entry.node.key));
		this.setBusy(true);
		return this.fetch(null).then((nodes) => {
			if (nodes) {
				this.show(nodes, keep);
				return this.restore(this.roots, keep);
			}
		}).catch((error) => {
			getHost().log.error('JsonTree: loading the nodes of #' + this.id + ' failed', error);
		}).finally(() => this.setBusy(false));
	}

	/**
	 * Expands, level by level, the folders whose children were not sent along.
	 */
	private async restore(keys: string[], expanded: Set<string>): Promise<void> {
		for (const key of keys) {
			const entry = this.entries.get(key);
			if (entry && expanded.has(key) && !entry.node.leaf) {
				await this.expand(key, false);
				await this.restore(entry.children ?? [], expanded);
			}
		}
	}

	/**
	 * Shows the roots given, forgetting every node shown before.
	 */
	private show(nodes: TreeNode[], expanded: Set<string>): void {
		this.entries.clear();
		this.roots = this.remember(nodes, null, 1, expanded);
		this.container.replaceChildren(...this.roots.flatMap((key) => this.cfg.table ?
			[this.build(key), ...this.rows(key)] : [this.build(key)]));
		this.resetTabStop();
		this.updateStatus();
		this.makeResizable();
	}

	/**
	 * Makes the columns of a tree grid resizable, once the first rows are shown, which the widths
	 * the columns start with are measured by.
	 */
	private makeResizable(): void {
		const mode = this.cfg.resizableColumns;
		const table = this.container.parentElement;
		if (!mode || this.resizable || !(table instanceof HTMLTableElement)) {
			return;
		}
		this.resizable = table;
		const label = this.cfg.texts.resizeColumn;
		JsonTree.resizableColumns()?.attach(table, label ? { mode, label } : { mode });
	}

	/**
	 * Remembers nodes and, recursively, the children sent along with them.
	 *
	 * @returns the keys of the nodes
	 */
	private remember(nodes: TreeNode[], parent: string | null, level: number,
		expanded: Set<string>): string[] {
		return nodes.map((node) => {
			const children = node.children ?
				this.remember(node.children, node.key, level + 1, expanded) : null;
			this.entries.set(node.key, {
				node, parent, level, children,
				expanded: !node.leaf && children !== null && expanded.has(node.key),
				loading: null,
				element: null
			});
			return node.key;
		});
	}

	/**
	 * Builds the item of a node and, in a tree, those of its children if it is expanded; in a
	 * tree grid the rows of the children follow the row of the node, see rows().
	 */
	private build(key: string): HTMLElement {
		const entry = this.entries.get(key)!;
		const table = Boolean(this.cfg.table);
		const item = document.createElement(table ? 'tr' : 'li');
		item.className = this.css('tree-item', table && 'tree-grid-row');
		item.setAttribute('role', table ? 'row' : 'treeitem');
		item.setAttribute('data-key', key);
		item.setAttribute('aria-level', String(entry.level));
		item.tabIndex = -1;
		entry.element = item;
		this.fill(entry);
		if (!table && entry.expanded && entry.children) {
			item.appendChild(this.group(entry.children));
		}
		return item;
	}

	/**
	 * Renders the state and the content of an item: its row, and the cells of a tree grid.
	 */
	private fill(entry: Entry): void {
		const item = entry.element!;
		const node = entry.node;
		toggleParts(item, this.look, node.leaf, 'tree-item-leaf');
		if (node.leaf) {
			item.removeAttribute('aria-expanded');
		} else {
			item.setAttribute('aria-expanded', String(entry.expanded));
		}
		if (this.isSelectable(node)) {
			item.setAttribute('aria-selected', String(node.key === this.selected));
		} else {
			item.removeAttribute('aria-selected');
		}
		toggleParts(item, this.look, this.isSelectable(node) && node.key === this.selected,
			'tree-item-selected');

		const row = document.createElement('div');
		row.className = this.css('tree-row');
		row.style.setProperty('--antilia-tree-level', String(entry.level));
		const toggle = document.createElement('span');
		toggle.className = this.css('tree-toggle');
		toggle.setAttribute('aria-hidden', 'true');
		if (!node.leaf) {
			toggle.innerHTML = this.icons.toggle;
			toggle.classList.toggle(LOADING_CLASS, entry.loading !== null);
		}
		const icon = document.createElement('span');
		icon.className = this.css('tree-icon');
		icon.setAttribute('aria-hidden', 'true');
		icon.innerHTML = node.icon ?? (node.leaf ? this.icons.leaf : entry.expanded ?
			this.icons.folderOpen : this.icons.folder);
		const content = document.createElement('span');
		content.className = this.css('tree-content');
		content.innerHTML = this.node(node.data, { key: node.key, level: entry.level });
		row.append(toggle, icon, content);

		if (this.cfg.table) {
			const cells = [row, ...this.cells.map((cell) => {
				const div = document.createElement('div');
				div.innerHTML = cell(node.data, { key: node.key, level: entry.level });
				return div;
			})].map((child, index) => {
				const td = document.createElement('td');
				td.setAttribute('role', 'gridcell');
				const cssClass = index === 0 ? this.css('tree-grid-tree-cell') :
					this.cfg.table!.columns[index - 1].cssClass;
				if (cssClass) {
					td.className = cssClass;
				}
				td.append(...(index === 0 ? [child] : Array.from(child.childNodes)));
				return td;
			});
			item.replaceChildren(...cells);
		} else {
			const group = Array.from(item.children).find((child) =>
				child.classList.contains('antilia-tree-group'));
			item.replaceChildren(row, ...(group ? [group] : []));
		}
	}

	/**
	 * @returns the group of a tree holding the items of nodes
	 */
	private group(keys: string[]): HTMLElement {
		const group = document.createElement('ul');
		group.className = this.css('tree-group');
		group.setAttribute('role', 'group');
		group.append(...keys.map((key) => this.build(key)));
		return group;
	}

	/**
	 * @returns the rows of a tree grid showing the descendants of a node: its children and,
	 *          after each one expanded, its own
	 */
	private rows(key: string): HTMLElement[] {
		const entry = this.entries.get(key)!;
		if (!entry.expanded || !entry.children) {
			return [];
		}
		return entry.children.flatMap((child) => [this.build(child), ...this.rows(child)]);
	}

	/**
	 * @returns the keys of the nodes shown, in order
	 */
	visibleKeys(): string[] {
		const keys: string[] = [];
		const walk = (level: string[]) => level.forEach((key) => {
			keys.push(key);
			const entry = this.entries.get(key)!;
			if (entry.expanded && entry.children) {
				walk(entry.children);
			}
		});
		walk(this.roots);
		return keys;
	}

	/**
	 * @returns whether a folder is expanded
	 */
	isExpanded(key: string): boolean {
		return this.entries.get(key)?.expanded ?? false;
	}

	/**
	 * @returns the key of the leaf selected, null if none is
	 */
	getSelected(): string | null {
		return this.selected;
	}

	/**
	 * Expands a folder, loading its children the first time.
	 *
	 * @param key the key of the folder
	 * @param report whether to report it, as when the user expands it
	 */
	expand(key: string, report = true): Promise<void> {
		const entry = this.entries.get(key);
		if (!entry || entry.node.leaf || entry.expanded) {
			return entry?.loading ?? Promise.resolve();
		}
		if (entry.children === null) {
			if (!entry.loading) {
				entry.loading = this.loadChildren(entry, report);
				this.refill(entry);
			}
			return entry.loading;
		}
		this.open(entry, report);
		return Promise.resolve();
	}

	private loadChildren(entry: Entry, report: boolean): Promise<void> {
		const key = entry.node.key;
		this.setBusy(true);
		entry.element?.setAttribute('aria-busy', 'true');
		return this.fetch(key).then((nodes) => {
			if (nodes && this.entries.get(key) === entry) {
				entry.children = this.remember(nodes, key, entry.level + 1, new Set());
				entry.loading = null;
				this.open(entry, report);
			}
		}).catch((error) => {
			getHost().log.error('JsonTree: loading the children of "' + key + '" in #' + this.id +
				' failed', error);
		}).finally(() => {
			entry.loading = null;
			entry.element?.removeAttribute('aria-busy');
			if (this.entries.get(key) === entry && !entry.expanded) {
				this.refill(entry);
			}
			this.setBusy(false);
		});
	}

	/**
	 * Shows the children of a folder whose children are loaded.
	 */
	private open(entry: Entry, report: boolean): void {
		entry.expanded = true;
		const item = entry.element;
		if (item?.isConnected) {
			this.refill(entry);
			if (this.cfg.table) {
				item.after(...this.rows(entry.node.key));
			} else {
				item.appendChild(this.group(entry.children ?? []));
			}
		}
		if (report) {
			this.callbacks.toggle?.(entry.node.key, true);
		}
	}

	/**
	 * Collapses a folder, hiding its descendants; the focus, if on one of them, goes to it.
	 *
	 * @param key the key of the folder
	 * @param report whether to report it, as when the user collapses it
	 */
	collapse(key: string, report = true): void {
		const entry = this.entries.get(key);
		if (!entry || !entry.expanded) {
			return;
		}
		const item = entry.element;
		const focused = document.activeElement;
		if (this.cfg.table) {
			const hidden = new Set(this.rows(key).map((row) => row.getAttribute('data-key')));
			const rows = Array.from(this.container.children) as HTMLElement[];
			const moveFocus = rows.some((row) => hidden.has(row.getAttribute('data-key')) &&
				row.contains(focused));
			rows.filter((row) => hidden.has(row.getAttribute('data-key')))
				.forEach((row) => row.remove());
			entry.expanded = false;
			this.refill(entry);
			if (moveFocus) {
				this.focus(key);
			}
		} else {
			const moveFocus = item !== focused && Boolean(item?.contains(focused));
			entry.expanded = false;
			item?.querySelector(':scope > .antilia-tree-group')?.remove();
			this.refill(entry);
			if (moveFocus) {
				this.focus(key);
			}
		}
		this.forgetElements(entry.children ?? []);
		this.resetTabStop();
		if (report) {
			this.callbacks.toggle?.(key, false);
		}
	}

	private forgetElements(keys: string[]): void {
		keys.forEach((key) => {
			const entry = this.entries.get(key);
			if (entry) {
				entry.element = null;
				this.forgetElements(entry.children ?? []);
			}
		});
	}

	/**
	 * Expands a collapsed folder, collapses an expanded one.
	 */
	toggle(key: string): Promise<void> {
		if (this.isExpanded(key)) {
			this.collapse(key);
			return Promise.resolve();
		}
		return this.expand(key);
	}

	/**
	 * @returns whether a node is selected when the user clicks it: a leaf, or a folder when they
	 *          are selected too
	 */
	private isSelectable(node: TreeNode): boolean {
		return node.leaf || this.cfg.selectBranches === true;
	}

	/**
	 * Selects a leaf, or a folder when they are selected too, unselecting the node selected before.
	 *
	 * @param key the key of the node, null to select none
	 * @param report whether to report it, as when the user selects it
	 */
	select(key: string | null, report = false): void {
		const entry = key === null ? null : this.entries.get(key);
		if (entry && !this.isSelectable(entry.node)) {
			return;
		}
		const previous = this.selected === null ? undefined : this.entries.get(this.selected);
		this.selected = key;
		if (previous?.element) {
			previous.element.setAttribute('aria-selected', 'false');
			toggleParts(previous.element, this.look, false, 'tree-item-selected');
		}
		if (entry?.element) {
			entry.element.setAttribute('aria-selected', 'true');
			toggleParts(entry.element, this.look, true, 'tree-item-selected');
		}
		this.resetTabStop();
		if (report && entry) {
			this.callbacks.select?.(entry.node.key, entry.node.data);
		}
	}

	/**
	 * Repaints a node with new data, in place; a node the tree does not know is ignored.
	 */
	renderNode(key: string, data: unknown): void {
		const entry = this.entries.get(key);
		if (entry) {
			entry.node = { ...entry.node, data };
			if (entry.element) {
				this.refill(entry);
			}
		}
	}

	/**
	 * Moves the focus to the item of a node shown.
	 */
	focus(key: string): void {
		const element = this.entries.get(key)?.element;
		if (element) {
			this.setTabStop(element);
			element.focus();
		}
	}

	/**
	 * Renders an item again, keeping the focus on it.
	 */
	private refill(entry: Entry): void {
		if (!entry.element) {
			return;
		}
		const focused = document.activeElement === entry.element;
		this.fill(entry);
		if (focused) {
			entry.element.focus();
		}
	}

	/**
	 * @returns the items shown
	 */
	private items(): HTMLElement[] {
		return Array.from(this.container.querySelectorAll<HTMLElement>('.' + ITEM_CLASS));
	}

	/**
	 * Makes one item the one Tab moves the focus to: the focused one, else the leaf selected, if
	 * shown, else the first.
	 */
	private resetTabStop(): void {
		const items = this.items();
		const current = items.find((item) => item.contains(document.activeElement)) ??
			items.find((item) => item.getAttribute('data-key') === this.selected) ?? items[0];
		if (current) {
			this.setTabStop(current);
		}
	}

	private setTabStop(element: HTMLElement): void {
		this.items().forEach((item) => {
			item.tabIndex = item === element ? 0 : -1;
		});
	}

	private setBusy(busy: boolean): void {
		this.busy += busy ? 1 : -1;
		this.element?.setAttribute('aria-busy', String(this.busy > 0));
		this.updateStatus();
	}

	/**
	 * Says below the nodes that they are loading, or that there are none.
	 */
	private updateStatus(): void {
		if (!this.status) {
			return;
		}
		const loadingRoots = this.busy > 0 && this.roots.length === 0;
		const text = loadingRoots ? this.cfg.texts.loading :
			this.busy === 0 && this.roots.length === 0 ? this.cfg.texts.empty : '';
		this.status.classList.toggle('antilia-tree-status--loading', loadingRoots);
		this.status.textContent = text;
		this.status.hidden = !text;
	}

	/**
	 * @returns the key of the node of an element of the tree, and its item
	 */
	private itemOf(target: EventTarget | null): HTMLElement | null {
		const item = target instanceof Element ? target.closest<HTMLElement>('.' + ITEM_CLASS) :
			null;
		return item && this.element!.contains(item) ? item : null;
	}

	/**
	 * A click on an element with a data-dt-action attribute sends the action; on the arrow of a
	 * folder expands or collapses it, as anywhere else on it does, unless folders are selected
	 * too: then it selects the folder, expanding it if it is collapsed. A click anywhere on a leaf
	 * selects it.
	 */
	private onClick(event: MouseEvent): void {
		const target = event.target as Element;
		const item = this.itemOf(target);
		const key = item?.getAttribute('data-key');
		const entry = key ? this.entries.get(key) : undefined;
		if (!item || !entry) {
			return;
		}
		const actionElement = target.closest<HTMLElement>('[data-dt-action]');
		if (actionElement && item.contains(actionElement)) {
			event.preventDefault();
			this.callbacks.action?.(actionElement.getAttribute('data-dt-action') ?? '',
				entry.node.key, entry.node.data);
			return;
		}
		if (target.closest('a[href]:not([href="#"]), button, input, select, textarea, label')) {
			// a control of the template does what it does
			return;
		}
		this.setTabStop(item);
		const onToggle = target.closest('.antilia-tree-toggle') !== null;
		if (this.isSelectable(entry.node) && !(onToggle && !entry.node.leaf)) {
			this.choose(entry);
		} else {
			this.toggle(entry.node.key);
		}
	}

	/**
	 * Selects a node, as the user does, expanding a folder that is collapsed.
	 */
	private choose(entry: Entry): void {
		this.select(entry.node.key, true);
		if (!entry.node.leaf && !entry.expanded) {
			this.expand(entry.node.key);
		}
	}

	private onFocusIn(event: FocusEvent): void {
		const item = this.itemOf(event.target);
		if (item && item === event.target) {
			this.setTabStop(item);
		}
	}

	/**
	 * Moves through the tree as WAI-ARIA has it: Up and Down to the item before and after, Right
	 * expands a folder or moves to its first child, Left collapses it or moves to the parent,
	 * Home and End to the first and the last item; Enter and Space select a leaf, or expand and
	 * collapse a folder, or, when folders are selected too, select it, expanding it if collapsed.
	 */
	private onKeyDown(event: KeyboardEvent): void {
		const item = this.itemOf(event.target);
		if (!item || item !== event.target) {
			return;
		}
		const key = item.getAttribute('data-key')!;
		const entry = this.entries.get(key);
		if (!entry) {
			return;
		}
		const visible = this.visibleKeys();
		const index = visible.indexOf(key);
		let next: string | undefined = undefined;
		switch (event.key) {
			case 'ArrowDown':
				next = visible[index + 1];
				break;
			case 'ArrowUp':
				next = visible[index - 1];
				break;
			case 'Home':
				next = visible[0];
				break;
			case 'End':
				next = visible[visible.length - 1];
				break;
			case 'ArrowRight':
				if (!entry.node.leaf && !entry.expanded) {
					this.expand(key);
				} else if (entry.expanded) {
					next = entry.children?.[0];
				}
				break;
			case 'ArrowLeft':
				if (entry.expanded) {
					this.collapse(key);
				} else {
					next = entry.parent ?? undefined;
				}
				break;
			case 'Enter':
			case ' ':
				if (this.isSelectable(entry.node)) {
					this.choose(entry);
				} else {
					this.toggle(key);
				}
				break;
			default:
				return;
		}
		event.preventDefault();
		if (next !== undefined) {
			this.focus(next);
		}
	}

	destroy(): void {
		this.element?.removeEventListener('click', this.clickHandler);
		this.element?.removeEventListener('keydown', this.keyHandler);
		this.element?.removeEventListener('focusin', this.focusHandler);
		if (this.resizable) {
			JsonTree.resizableColumns()?.detach(this.resizable.id);
			this.resizable = null;
		}
		if (this.cfg.height && this.element) {
			this.scrollbar?.destroy();
			this.scrollbar = undefined;
			this.element.classList.remove(SCROLLING_CLASS);
			this.element.style.height = '';
		}
		this.stops.forEach((stop) => stop());
		this.stops = [];
		if (this.isLive()) {
			delete JsonTree.instances[this.id];
		}
	}
}
