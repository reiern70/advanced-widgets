<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  @component
  A list rendering its items from JSON with a template, loading the next page of them when the
  last one comes into view. The items are rendered by the core into the list Svelte renders empty;
  another template loads them again. Bound with bind:this, the component reloads the items with
  reload(), repaints one with renderItem(key, data) and scrolls to the first with scrollToTop().
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { type InfiniteListConfig, type InfiniteListData, JsonInfiniteList, partClasses } from '@antilia/core';

	import { getLook } from './look.js';

	const look = getLook();

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

	let instance: JsonInfiniteList | null = null;

	$effect(() => {
		const config = { id, template, engine, height: $state.snapshot(height),
			texts: $state.snapshot(texts), locale };
		const list = untrack(() => new JsonInfiniteList({ ...config, look, load: (more) => load(more) },
			(action, key) => onaction?.(action, key)));
		instance = list;
		return () => {
			list.destroy();
			instance = null;
		};
	});

	/** Loads the items shown again, as after a filter or a change of them. */
	export function reload(): Promise<void> {
		return instance?.load() ?? Promise.resolve();
	}

	/** Repaints one item with new data, in place; an item the list does not show is ignored. */
	export function renderItem(key: string, data: unknown): void {
		instance?.renderItem(key, data);
	}

	/** Scrolls the list back to its first item. */
	export function scrollToTop(): void {
		instance?.scrollToTop();
	}
</script>

{#if height}
	<!-- a list of a height scrolls in the wrapper of its scroll bars, which Svelte renders -->
	<div class={partClasses(look, 'scrollbar')}>
		<div {id} class={[partClasses(look, 'list'), className]}></div>
	</div>
{:else}
	<div {id} class={[partClasses(look, 'list'), className]}></div>
{/if}
