<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  @component
  A list rendering its items from JSON with a template, loading the next page of them when the
  last one comes into view. The items are rendered by the core into the list Svelte renders empty;
  another template loads them again.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { JsonInfiniteList, type InfiniteListConfig,
		type InfiniteListData } from '@advanced-widgets/core';

	type Props = {
		/** Loads the items shown, or the next page of them when more is true. */
		load: (more: boolean) => Promise<InfiniteListData>;
		/** The template of an item; {{@key}} and {{@number}} are its key and number. */
		template: string;
		engine?: string;
		/** The height of the list, which then scrolls itself with slim scroll bars. */
		height?: InfiniteListConfig['height'];
		/** What the list says below its items; ${count} stands for the number of items. */
		texts: InfiniteListConfig['texts'];
		locale?: string;
		/** Called with the action of a clicked [data-dt-action] element and the key of its item. */
		onaction?: (action: string, key: string | null) => void;
		class?: string;
	};

	let { load, template, engine, height, texts, locale, onaction, class: className = '' }:
		Props = $props();

	const id = $props.id();

	$effect(() => {
		const config = { id, template, engine, height: $state.snapshot(height),
			texts: $state.snapshot(texts), locale };
		const list = untrack(() => new JsonInfiniteList({ ...config, load: (more) => load(more) },
			(action, key) => onaction?.(action, key)));
		return () => list.destroy();
	});
</script>

{#if height}
	<!-- a list of a height scrolls in the wrapper of its scroll bars, which Svelte renders -->
	<div class="wicket-scrollbar">
		<div {id} class={['json-infinite-list', className]}></div>
	</div>
{:else}
	<div {id} class={['json-infinite-list', className]}></div>
{/if}
