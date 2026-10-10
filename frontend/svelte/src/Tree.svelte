<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  @component
  A tree rendering its nodes from JSON with a template, loading the children of a folder the first
  time it is expanded: a WAI-ARIA tree moved through with the arrow keys. Only leaves are selected,
  a click on a folder expanding or collapsing it, unless selectBranches has a click on a folder
  select it, and expand it if it is collapsed. Given a height, it scrolls itself with slim scroll
  bars. Given columns and a header, it is a tree grid, as TreeGrid is. A private component:
  without a license key activating it, it shows the placeholder Wicket shows. Bound with
  bind:this, the component reloads the nodes with reload(), repaints one with
  renderNode(key, data), and expands and collapses a folder with expand(key) and collapse(key).
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { isLicensed, JsonTree, TREE_COMPONENTS, TREE_GRID_COMPONENTS, TREE_TEXTS,
		UNLICENSED_CLASS, type ResizeMode, type TreeColumn, type TreeConfig, type TreeIcons,
		type TreeNode, type TreeTexts } from '@antilia/core';

	type Props = {
		/** Loads the roots, for null, or the children of a folder, the first time it is expanded. */
		load: (parent: string | null) => Promise<TreeNode[]>;
		/** The template of a node; {{@key}} and {{@level}} are its key and level. */
		template: string;
		engine?: string;
		/** The name of the tree for screen readers. */
		label?: string;
		/** The keys of the folders expanded at first. */
		expanded?: string[];
		/** The key of the node selected; the tree follows when it changes. */
		selected?: string | null;
		/** Whether folders are selected too, not only leaves: a click on one selects it. */
		selectBranches?: boolean;
		/** Icons of the application's own, in place of those of the tree. */
		icons?: Partial<TreeIcons>;
		/** What the tree says, English by default. */
		texts?: Partial<TreeTexts>;
		/** The height of the tree, which then scrolls itself with slim scroll bars. */
		height?: TreeConfig['height'];
		/** The header of the tree column of a tree grid. */
		header?: string;
		/** The columns of a tree grid after the tree column; a tree when left out. */
		columns?: TreeColumn[];
		/** How the columns of a tree grid are resized, or false, as by default, when they are not. */
		resizableColumns?: ResizeMode | false;
		/** Called when the user selects a leaf, or a folder with selectBranches. */
		onselect?: (key: string, data: unknown) => void;
		/** Called with the action of a clicked [data-dt-action] element of a node. */
		onaction?: (action: string, key: string, data: unknown) => void;
		/** Called when the user expands or collapses a folder. */
		ontoggle?: (key: string, expanded: boolean) => void;
		/** A license key of its own; the one given to activate() when left out. */
		licenseKey?: string;
		class?: string;
	};

	let { load, template, engine, label, expanded, selected, selectBranches, icons,
		texts: givenTexts, height,
		header, columns, resizableColumns = false, onselect, onaction, ontoggle, licenseKey,
		class: className = '' }: Props = $props();

	const generatedId = $props.id();
	const id = 'json-tree-' + generatedId.replace(/[^\w-]/g, '');
	const texts = $derived<Required<TreeTexts>>({ ...TREE_TEXTS, ...givenTexts });
	const table = $derived(Boolean(columns));

	let licensed = $state<boolean | null>(null);
	let instance: JsonTree | null = null;

	$effect(() => {
		const key = licenseKey;
		const names = table ? TREE_GRID_COMPONENTS : TREE_COMPONENTS;
		let live = true;
		isLicensed(names, key).then((active) => {
			if (live) {
				licensed = active;
			}
		});
		return () => {
			live = false;
		};
	});

	$effect(() => {
		if (!licensed) {
			return;
		}
		const config = { id, template, engine, label, selectBranches, icons: $state.snapshot(icons),
			texts: $state.snapshot(texts), height: $state.snapshot(height),
			table: columns ? { header: header ?? '', columns: $state.snapshot(columns) } : undefined,
			resizableColumns: columns && resizableColumns ? resizableColumns : undefined };
		const tree = untrack(() => new JsonTree({ ...config, expanded: $state.snapshot(expanded),
			selected, load: (parent) => load(parent) }, {
			select: (key, data) => onselect?.(key, data),
			action: (action, key, data) => onaction?.(action, key, data),
			toggle: (key, open) => ontoggle?.(key, open)
		}));
		instance = tree;
		return () => {
			tree.destroy();
			instance = null;
		};
	});

	$effect(() => {
		const key = selected;
		untrack(() => {
			if (key !== undefined && instance && instance.getSelected() !== key) {
				instance.select(key);
			}
		});
	});

	/** Loads the nodes again, keeping the folders expanded and the leaf selected. */
	export function reload(): Promise<void> {
		return instance?.reload() ?? Promise.resolve();
	}

	/** Repaints one node with new data, in place. */
	export function renderNode(key: string, data: unknown): void {
		instance?.renderNode(key, data);
	}

	/** Expands a folder, loading its children the first time. */
	export function expand(key: string): Promise<void> {
		return instance?.expand(key) ?? Promise.resolve();
	}

	/** Collapses a folder. */
	export function collapse(key: string): void {
		instance?.collapse(key);
	}
</script>

{#if licensed === false}
	<div class={UNLICENSED_CLASS} role="note" data-component={table ? 'TreeGrid' : 'Tree'}>
		{texts.unlicensed}</div>
{:else if licensed && height}
	<!-- a tree of a height scrolls in the wrapper of its scroll bars, which Svelte renders -->
	<div class="antilia-scrollbar">
		<div {id} class={['json-tree', table ? 'antilia-tree-grid' : 'antilia-tree', className]}></div>
	</div>
{:else if licensed}
	<div {id} class={['json-tree', table ? 'antilia-tree-grid' : 'antilia-tree', className]}></div>
{/if}
