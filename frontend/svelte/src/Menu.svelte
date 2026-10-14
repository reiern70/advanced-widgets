<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  @component
  The menu of the core: a bar of items along the top or down the side, links and buttons, and items
  opening submenus, nested as deep as one likes. A click opens a submenu, and while one of the bar is
  open the pointer opens the others; the keys move along the bar and through the submenus. The
  submenus pop up, or open in place down the side. Svelte renders the items, from the list items,
  which may change, the core opens and closes the submenus, in the look of the closest setLook. An
  item is a link when it has href, a button calling onclick otherwise, and opens a submenu when it
  has items; its icon is markup, an svg, written as is. bind:this gets the functions below.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { Menu, partClasses, type MenuDesign, type MenuOrientation } from '@antilia/core';

	import { getLook } from './look.js';
	import type { MenuEntry, MenuItem } from './types.js';

	const look = getLook();

	type Props = {
		/** The items of the bar, in order. */
		items: MenuEntry[];
		/** horizontal (the default) for the bar along the top, vertical for down the side. */
		orientation?: MenuOrientation;
		/** The design of the menu, bar when left out. */
		design?: MenuDesign;
		/**
		 * Whether the submenus of a menu down the side open in place, pushing the items after them
		 * down, instead of popping up beside their items; false when left out.
		 */
		inline?: boolean;
		/** Whether the submenus of the bar open as the pointer rests on their items. */
		openOnHover?: boolean;
		/** The accessible name of the bar. */
		label?: string;
		id?: string;
		/** CSS classes of the menu, next to antilia-menu. */
		class?: string;
		/** Called with each item the user chooses, after its own onclick. */
		onselect?: (item: MenuItem) => void;
	};

	let {
		items,
		orientation = 'horizontal',
		design = 'bar',
		inline = false,
		openOnHover = false,
		label,
		id,
		class: className = '',
		onselect
	}: Props = $props();

	let element = $state<HTMLDivElement>();
	let instance: Menu | null = null;

	const classes = $derived([partClasses(look, 'menu', `menu-${orientation}`, `menu-${design}`,
		inline && orientation === 'vertical' && 'menu-inline'), className].filter(Boolean).join(' '));

	$effect(() => {
		if (!element) {
			return;
		}
		const core = new Menu(element, untrack(() => ({ orientation, design, look, inline,
			openOnHover, label })));
		instance = core;
		return () => {
			core.destroy();
			instance = null;
		};
	});

	$effect(() => {
		const next = { orientation, design, inline, openOnHover, label };
		untrack(() => instance?.configure(next));
	});

	/** Reads every item, so that the effect below follows them. */
	function follow(entries: MenuEntry[]): void {
		entries.forEach((entry) => {
			if (!('separator' in entry)) {
				void [entry.id, entry.label, entry.current, entry.disabled, entry.class, entry.href];
				follow(entry.items ?? []);
			}
		});
	}

	// the items as the page has them now: the states the core keeps on them come back
	$effect(() => {
		follow(items);
		void classes;
		untrack(() => instance?.refresh());
	});

	function choose(item: MenuItem, event: MouseEvent): void {
		item.onclick?.(event);
		onselect?.(item);
	}

	/** @returns the menu of the core, null before it is mounted */
	export function core(): Menu | null {
		return instance;
	}

	/** Closes the submenus popping up. */
	export function closeAll(): void {
		instance?.closeAll();
	}
</script>

{#snippet content(item: MenuItem)}
	{#if item.icon}<span class={partClasses(look, 'menu-icon')}>{@html item.icon}</span>{/if}
	<span class={partClasses(look, 'menu-label')}>{item.label}</span>
{/snippet}

{#snippet list(entries: MenuEntry[], depth: number)}
	{#each entries as entry, index (entry.id ?? index)}
		{#if 'separator' in entry}
			<li class={partClasses(look, 'menu-separator')}></li>
		{:else}
			{@const top = depth === 0}
			{@const linkClasses = [partClasses(look, top ? 'menu-link' : 'menu-submenu-link',
				entry.current && 'menu-current'), entry.class].filter(Boolean).join(' ')}
			{@const disabled = entry.disabled ? 'true' : undefined}
			<li class={partClasses(look, top ? 'menu-item' : 'menu-submenu-item')}>
				{#if entry.items}
					<button type="button" class={linkClasses} title={entry.tooltip}
						aria-disabled={disabled}>{@render content(entry)}</button>
					<ul class={partClasses(look, 'menu-submenu')}>{@render list(entry.items, depth + 1)}</ul>
				{:else if entry.href !== undefined}
					<a href={entry.disabled ? undefined : entry.href} target={entry.target}
						class={linkClasses} title={entry.tooltip} aria-disabled={disabled}
						aria-current={entry.current ? 'page' : undefined}
						onclick={entry.disabled ? undefined : (event) => choose(entry, event)}>{@render content(entry)}</a>
				{:else}
					<button type="button" class={linkClasses} title={entry.tooltip}
						aria-disabled={disabled} aria-current={entry.current ? 'page' : undefined}
						onclick={entry.disabled ? undefined : (event) => choose(entry, event)}>{@render content(entry)}</button>
				{/if}
			</li>
		{/if}
	{/each}
{/snippet}

<div bind:this={element} {id} class={classes}>
	<ul class={partClasses(look, 'menu-list', `menu-list-${design}`)}>{@render list(items, 0)}</ul>
</div>
