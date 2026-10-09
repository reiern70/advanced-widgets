<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  @component
  A tree grid: a Tree shown as a treegrid, its first column the tree, headed by header, the
  others rendered from the same data by the templates of the columns. Given a height, its head
  stays in place above the rows scrolling; resizableColumns makes its columns resizable. A private
  component, as the tree. Bound with bind:this, it does what a bound Tree does.
-->
<script lang="ts">
	import type { ComponentProps } from 'svelte';
	import type { TreeColumn } from '@advanced-widgets/core';

	import Tree from './Tree.svelte';

	type Props = ComponentProps<typeof Tree> & {
		/** The header of the tree column, the first one. */
		header: string;
		/** The columns after the tree column. */
		columns: TreeColumn[];
	};

	let props: Props = $props();

	let tree: Tree | undefined = $state();

	/** Loads the nodes again, keeping the folders expanded and the leaf selected. */
	export function reload(): Promise<void> {
		return tree?.reload() ?? Promise.resolve();
	}

	/** Repaints one node with new data, in place. */
	export function renderNode(key: string, data: unknown): void {
		tree?.renderNode(key, data);
	}

	/** Expands a folder, loading its children the first time. */
	export function expand(key: string): Promise<void> {
		return tree?.expand(key) ?? Promise.resolve();
	}

	/** Collapses a folder. */
	export function collapse(key: string): void {
		tree?.collapse(key);
	}
</script>

<Tree bind:this={tree} {...props} />
