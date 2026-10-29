<!--
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
  The breadcrumb of the core: where the page is, a row of crumbs from the home page down to the
  page shown, the last crumb. A nav, named, around an ordered list; the style sheet draws the
  separators. A long trail collapses: maxItems crumbs show, the others behind a button "…" that
  shows them in place. Svelte renders the crumbs, from the list items, which may change, the core
  collapses them, in the look of the closest setLook. A crumb is a link when it has href, a button
  calling onclick otherwise, its label alone when it has neither; its icon is a text or a snippet,
  as in React, and its iconMarkup markup the page trusts, written as is. bind:this gets the
  functions below.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { Breadcrumb, DEFAULT_BREADCRUMB_TEXTS, partClasses, type BreadcrumbSeparator,
		type BreadcrumbTexts } from '@antilia/core';

	import { getLook } from './look.js';
	import type { BreadcrumbItem } from './types.js';

	const look = getLook();

	type Props = {
		/** The crumbs, from the home page down to the page shown. */
		items: BreadcrumbItem[];
		/** The separator drawn between the crumbs, chevron when left out. */
		separator?: BreadcrumbSeparator;
		/**
		 * How many crumbs show at most: a longer trail shows its first crumb and its last
		 * maxItems - 1, the others behind a button "…" that shows them; every crumb when left out.
		 */
		maxItems?: number;
		/** The texts: the accessible name and the label of the button "…"; English when left out. */
		texts?: Partial<BreadcrumbTexts>;
		id?: string;
		/** CSS classes of the breadcrumb, next to antilia-breadcrumb. */
		class?: string;
		/** Called with each crumb the user chooses, after its own onclick. */
		onselect?: (item: BreadcrumbItem) => void;
		/** Called when the user showed the crumbs hidden. */
		onexpand?: () => void;
	};

	let {
		items,
		separator = 'chevron',
		maxItems = 0,
		texts,
		id,
		class: className = '',
		onselect,
		onexpand
	}: Props = $props();

	let element = $state<HTMLElement>();
	let instance: Breadcrumb | null = null;

	const classes = $derived([partClasses(look, 'breadcrumb', `breadcrumb-${separator}`),
		className].filter(Boolean).join(' '));
	const label = $derived(texts?.label ?? DEFAULT_BREADCRUMB_TEXTS.label);

	$effect(() => {
		if (!element) {
			return;
		}
		const core = new Breadcrumb(element, untrack(() => ({ separator, maxItems, look, texts,
			onExpand: () => onexpand?.() })));
		instance = core;
		return () => {
			core.destroy();
			instance = null;
		};
	});

	$effect(() => {
		const next = { separator, maxItems, texts: texts ? { ...texts } : undefined };
		untrack(() => instance?.configure(next));
	});

	// the crumbs as the page has them now: the core collapses them, or keeps them as the user left
	// them
	$effect(() => {
		items.forEach((item) => void [item.id, item.label, item.href, item.disabled, item.class]);
		void classes;
		untrack(() => instance?.refresh());
	});

	function choose(item: BreadcrumbItem, event: MouseEvent): void {
		item.onclick?.(event);
		onselect?.(item);
	}

	/** @returns the breadcrumb of the core, null before it is mounted */
	export function core(): Breadcrumb | null {
		return instance;
	}

	/** Shows the crumbs hidden, the focus on the first of them. */
	export function expand(): void {
		instance?.expand();
	}
</script>

{#snippet content(item: BreadcrumbItem)}
	{#if item.iconMarkup}<span class={partClasses(look, 'breadcrumb-icon')}>{@html item.iconMarkup}</span>
	{:else if typeof item.icon === 'function'}<span class={partClasses(look, 'breadcrumb-icon')}>{@render item.icon()}</span>
	{:else if item.icon}<span class={partClasses(look, 'breadcrumb-icon')}>{item.icon}</span>{/if}
	<span class={partClasses(look, 'breadcrumb-label')}>{item.label}</span>
{/snippet}

<nav bind:this={element} {id} class={classes} aria-label={label}>
	<ol class={partClasses(look, 'breadcrumb-list')}>
		{#each items as item, index (item.id ?? index)}
			{@const current = index === items.length - 1}
			{@const linkClasses = [partClasses(look, 'breadcrumb-link'), item.class].filter(Boolean)
				.join(' ')}
			{@const disabled = item.disabled ? 'true' : undefined}
			<li class={partClasses(look, 'breadcrumb-item', current && 'breadcrumb-current')}>
				{#if current}
					<span class={partClasses(look, 'breadcrumb-page')} aria-current="page"
						title={item.tooltip}>{@render content(item)}</span>
				{:else if item.href !== undefined}
					<a href={item.disabled ? undefined : item.href} target={item.target}
						class={linkClasses} title={item.tooltip} aria-disabled={disabled}
						onclick={item.disabled ? undefined : (event) => choose(item, event)}>{@render content(item)}</a>
				{:else if item.onclick}
					<button type="button" class={linkClasses} title={item.tooltip}
						aria-disabled={disabled}
						onclick={item.disabled ? undefined : (event) => choose(item, event)}>{@render content(item)}</button>
				{:else}
					<span class={partClasses(look, 'breadcrumb-text')}
						title={item.tooltip}>{@render content(item)}</span>
				{/if}
			</li>
		{/each}
	</ol>
</nav>
