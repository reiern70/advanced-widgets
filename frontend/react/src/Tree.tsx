/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { forwardRef, useEffect, useId, useImperativeHandle, useLayoutEffect, useRef,
	useState } from 'react';

import { isLicensed, JsonTree, TREE_COMPONENTS, TREE_GRID_COMPONENTS, TREE_TEXTS,
	UNLICENSED_CLASS, type ResizeMode, type TreeColumn, type TreeConfig, type TreeIcons,
	type TreeNode, type TreeTexts } from '@advanced-widgets/core';

export interface TreeProps {
	/** Loads the roots, for null, or the children of a folder, the first time it is expanded. */
	load: (parent: string | null) => Promise<TreeNode[]>;
	/** The template of a node, rendered from its data; {{@key}} and {{@level}} are its key and level. */
	template: string;
	/** The name of the template engine, the built-in one by default. */
	engine?: string;
	/** The name of the tree for screen readers. */
	label?: string;
	/** The keys of the folders expanded at first. */
	expanded?: string[];
	/** The key of the leaf selected; the tree follows when it changes. */
	selected?: string | null;
	/** Icons of the application's own, in place of those of the tree. */
	icons?: Partial<TreeIcons>;
	/** What the tree says, English by default. */
	texts?: Partial<TreeTexts>;
	/** The height of the tree, which then scrolls itself with slim scroll bars. */
	height?: TreeConfig['height'];
	/** Called when the user selects a leaf. */
	onSelect?(key: string, data: unknown): void;
	/** Called with the action of a clicked [data-dt-action] element of a node. */
	onAction?(action: string, key: string, data: unknown): void;
	/** Called when the user expands or collapses a folder. */
	onToggle?(key: string, expanded: boolean): void;
	/** A license key of its own; the one given to activate() when left out. */
	licenseKey?: string;
	/** CSS classes of the tree, next to json-tree. */
	className?: string;
}

export interface TreeGridProps extends TreeProps {
	/** The header of the tree column, the first one. */
	header: string;
	/** The columns after the tree column. */
	columns: TreeColumn[];
	/** How the columns are resized, or false, as by default, when they are not. */
	resizableColumns?: ResizeMode | false;
}

/**
 * What a ref to a {@link Tree} or a {@link TreeGrid} can do with it.
 */
export interface TreeHandle {
	/** Loads the nodes again, keeping the folders expanded and the leaf selected. */
	reload(): Promise<void>;
	/** Repaints one node with new data, in place. */
	renderNode(key: string, data: unknown): void;
	/** Expands a folder, loading its children the first time. */
	expand(key: string): Promise<void>;
	/** Collapses a folder. */
	collapse(key: string): void;
}

/**
 * The tree or the tree grid: the core renders the nodes into the element React renders empty.
 */
const JsonTreeView = forwardRef<TreeHandle, TreeProps & { table?: TreeGridProps['columns'] } &
	Partial<Pick<TreeGridProps, 'header' | 'resizableColumns'>>>(function JsonTreeView(props, ref) {
	const { template, engine, label, icons, height, licenseKey, className, table, header,
		resizableColumns } = props;
	const texts = { ...TREE_TEXTS, ...props.texts };
	const id = 'json-tree-' + useId().replace(/[^\w-]/g, '');
	const [licensed, setLicensed] = useState<boolean | null>(null);
	const latest = useRef(props);
	latest.current = props;
	const instance = useRef<JsonTree | null>(null);

	useEffect(() => {
		let live = true;
		isLicensed(table ? TREE_GRID_COMPONENTS : TREE_COMPONENTS, licenseKey)
			.then((active) => live && setLicensed(active));
		return () => {
			live = false;
		};
	}, [licenseKey, Boolean(table)]);

	useImperativeHandle(ref, () => ({
		reload: () => instance.current?.reload() ?? Promise.resolve(),
		renderNode: (key, data) => instance.current?.renderNode(key, data),
		expand: (key) => instance.current?.expand(key) ?? Promise.resolve(),
		collapse: (key) => instance.current?.collapse(key)
	}), []);

	const tableKey = table ? JSON.stringify({ header, table }) : '';
	const iconsKey = JSON.stringify(icons ?? {});
	const textsKey = JSON.stringify(texts);
	const heightKey = height ? height.value + height.unit : '';
	useLayoutEffect(() => {
		if (!licensed) {
			return;
		}
		const tree = new JsonTree({
			id, template, engine, label, icons, texts, height,
			table: table ? { header: header ?? '', columns: table } : undefined,
			resizableColumns: table && resizableColumns ? resizableColumns : undefined,
			expanded: latest.current.expanded,
			selected: latest.current.selected,
			load: (parent) => latest.current.load(parent)
		}, {
			select: (key, data) => latest.current.onSelect?.(key, data),
			action: (action, key, data) => latest.current.onAction?.(action, key, data),
			toggle: (key, expanded) => latest.current.onToggle?.(key, expanded)
		});
		instance.current = tree;
		return () => {
			tree.destroy();
			instance.current = null;
		};
	}, [licensed, id, template, engine, label, tableKey, iconsKey, textsKey, heightKey,
		resizableColumns]);

	useEffect(() => {
		if (props.selected !== undefined && instance.current &&
			instance.current.getSelected() !== props.selected) {
			instance.current.select(props.selected);
		}
	}, [props.selected]);

	if (licensed === null) {
		return null;
	}
	if (!licensed) {
		return <div className={UNLICENSED_CLASS} role="note"
			data-component={table ? 'TreeGrid' : 'Tree'}>{texts.unlicensed}</div>;
	}
	const tree = <div id={id} className={['json-tree', table ? 'wicket-tree-grid' : 'wicket-tree',
		className].filter(Boolean).join(' ')} />;
	// a tree of a height scrolls in the wrapper of its scroll bars, which React renders
	return height ? <div className="wicket-scrollbar">{tree}</div> : tree;
});

/**
 * A tree rendering its nodes from JSON with a template, loading the children of a folder the
 * first time it is expanded: a WAI-ARIA tree moved through with the arrow keys. Only leaves are
 * selected; a click on a folder expands or collapses it. A private component: without a license
 * key activating it, it shows the placeholder Wicket shows. A ref gets a {@link TreeHandle}.
 */
export const Tree = forwardRef<TreeHandle, TreeProps>(function Tree(props, ref) {
	return <JsonTreeView ref={ref} {...props} />;
});

/**
 * A tree grid: a {@link Tree} shown as a treegrid, its first column the tree, the others
 * rendered from the same data by the templates of the columns. Given a height, its head stays in
 * place above the rows scrolling; its columns can be made resizable. A private component, as the
 * tree.
 */
export const TreeGrid = forwardRef<TreeHandle, TreeGridProps>(function TreeGrid(
	{ columns, header, resizableColumns, ...props }, ref) {
	return <JsonTreeView ref={ref} {...props} table={columns} header={header}
		resizableColumns={resizableColumns} />;
});
