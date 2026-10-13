<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  @component
  The tabs of the core: a list of tabs, along the top of their panels or down their left side, and
  the panel of the tab selected. A click or the arrow keys select a tab. A closable tab has a
  button closing it, as do Delete and the middle button; closing one with changes not saved asks
  first, and its tab shows a mark meanwhile. Svelte renders the tabs, from the list tabs, which may
  change, and their panels, the snippet panel given each tab; the snippets icon and actions put an
  icon before a title and buttons of the page's own after it, before the button closing the tab.
  The tab selected binds both ways (bind:selected). bind:this gets the functions below, which change
  the tabs by their names without calling back. The tabs are in the look of the closest setLook.
-->
<script lang="ts">
	import { untrack, type Snippet } from 'svelte';
	import { LOOK_PARTS, partClasses, Tabs, type LookPart, type TabsDesign, type TabsOrientation,
		type TabsTexts } from '@antilia/core';

	import { getLook } from './look.js';
	import type { TabItem } from './types.js';

	const look = getLook();

	type Props = {
		/** The tabs, in order. */
		tabs: TabItem[];
		/** The panel of a tab. */
		panel: Snippet<[TabItem]>;
		/** An icon before the title of a tab; an svg, for example. */
		icon?: Snippet<[TabItem]>;
		/**
		 * Buttons of the page's own after the title of a tab, before the one closing it: one telling
		 * about what the panel holds, for example. A click on them does not select the tab.
		 */
		actions?: Snippet<[TabItem]>;
		/** The name of the tab selected, the first when left out. */
		selected?: string | null;
		/** horizontal (the default) for the tabs along the top, vertical for down the left side. */
		orientation?: TabsOrientation;
		/** The design of the tabs, tabs when left out. */
		design?: TabsDesign;
		/** The accessible name of the list of tabs. */
		label?: string;
		/** What the tabs say, in English when left out. */
		texts?: Partial<TabsTexts>;
		/**
		 * Asked before the user closes a tab with changes not saved, instead of the dialog of the
		 * tabs; resolving to true closes it.
		 */
		confirmClose?: (tab: string, title: string) => Promise<boolean> | boolean;
		id?: string;
		/** CSS classes of the tabs, next to antilia-tabs. */
		class?: string;
		/** Called with the name of the tab the user selected. */
		onselect?: (tab: string) => void;
		/**
		 * Called with the name of the tab the user closed: hidden until opened again, it is for the
		 * page to take it away from tabs.
		 */
		onclose?: (tab: string) => void;
	};

	let {
		tabs,
		panel,
		icon,
		actions,
		selected = $bindable(),
		orientation = 'horizontal',
		design = 'tabs',
		label,
		texts,
		confirmClose,
		id,
		class: className = '',
		onselect,
		onclose
	}: Props = $props();

	let element = $state<HTMLDivElement>();
	let instance: Tabs | null = null;

	const classes = $derived([partClasses(look, 'tabs', `tabs-${orientation}`, `tabs-${design}`),
		className].filter(Boolean).join(' '));

	// the design of the list in the look, a nav in Bootstrap's
	const listClasses = $derived.by(() => {
		const part = 'tabs-list-' + design;
		return partClasses(look, 'tabs-list', part in LOOK_PARTS && part as LookPart);
	});

	const flag = (value: boolean | undefined) => value === undefined ? undefined : String(value);

	$effect(() => {
		if (!element) {
			return;
		}
		const core: Tabs = new Tabs(element, untrack(() => ({
			orientation, design, look, label, texts, selected,
			confirmClose: (tab: string, title: string): Promise<boolean> | boolean => confirmClose ?
				confirmClose(tab, title) : core.askToClose(title),
			onSelect: (tab: string) => {
				selected = tab;
				onselect?.(tab);
			},
			onClose: (tab: string) => {
				selected = core.selected();
				onclose?.(tab);
			}
		})));
		instance = core;
		if (untrack(() => selected) === undefined) {
			selected = core.selected();
		}
		return () => {
			core.destroy();
			instance = null;
		};
	});

	$effect(() => {
		const next = { orientation, design, label, texts };
		untrack(() => instance?.configure(next));
	});

	// the tabs as the page has them now, and the one it selects
	$effect(() => {
		const next = selected;
		void tabs.map((tab) => [tab.id, tab.title, tab.closable, tab.modified, tab.class]);
		void classes;
		untrack(() => {
			if (!instance) {
				return;
			}
			if (next != null && next !== instance.selected()) {
				instance.configure({ selected: next });
			} else {
				instance.refresh();
			}
		});
	});

	/** @returns the tabs of the core, null before they are mounted */
	export function core(): Tabs | null {
		return instance;
	}

	/** Selects a tab, opening it when closed. */
	export function select(tab: string): void {
		instance?.select(tab);
		selected = instance?.selected() ?? selected;
	}

	/** Closes a tab at once. */
	export function close(tab: string): void {
		instance?.close(tab);
		selected = instance?.selected() ?? null;
	}

	/**
	 * Closes a tab as its close button does, asking first when it has changes not saved; resolves
	 * to whether it closed.
	 */
	export function requestClose(tab: string): Promise<boolean> {
		return instance?.requestClose(tab) ?? Promise.resolve(false);
	}

	/** Opens a closed tab again, selecting it. */
	export function open(tab: string): void {
		select(tab);
	}

	/** Whether the panel of a tab has changes not saved. */
	export function isModified(tab: string): boolean {
		return instance?.isModified(tab) ?? false;
	}
</script>

<div bind:this={element} {id} class={classes}>
	<div class={listClasses}>
		{#each tabs as tab (tab.id)}
			<div class={[partClasses(look, 'tab'), tab.class].filter(Boolean).join(' ')} data-tab={tab.id}
				data-closable={tab.closable ? 'true' : undefined}>
				<button type="button" class={partClasses(look, 'tab-button')}>
					{#if icon}<span class={partClasses(look, 'tab-icon')}>{@render icon(tab)}</span>{/if}
					<span class={partClasses(look, 'tab-title')}>{tab.title}</span>
				</button>
				{#if actions}<span class={partClasses(look, 'tab-actions')}>{@render actions(tab)}</span>{/if}
			</div>
		{/each}
	</div>
	<div class={partClasses(look, 'tabs-panels')}>
		{#each tabs as tab (tab.id)}
			<div class={[partClasses(look, 'tabs-panel'), tab.panelClass].filter(Boolean).join(' ')}
				data-tab={tab.id} data-modified={flag(tab.modified)}>
				{@render panel(tab)}
			</div>
		{/each}
	</div>
</div>
