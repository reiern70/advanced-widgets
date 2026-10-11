<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  @component
  The desktop of the core: floating windows that are not modal, many at once, and a taskbar. A
  click brings a window to the front; its title bar drags it and a double click folds it; its
  buttons minimize, maximize and close it, as it allows, and its edges resize it. F11 maximizes the
  window the focus is in, Ctrl+F6 goes round the windows. The user drags the taskbar to another
  side, and a button of it lets the desktop fill the whole page; its menu lists the windows open
  and closed. Closing a window with changes not saved asks first. Svelte renders the windows,
  DesktopWindow components in the children, which may come and go; the core moves them and fills
  the taskbar. bind:this gets the functions below, which change the windows by their ids without
  calling back. Give it a height. A private component: it needs a license key.
-->
<script lang="ts">
	import { untrack, type Snippet } from 'svelte';
	import { DEFAULT_DESKTOP_TEXTS, Desktop, DESKTOP_COMPONENTS, isLicensed, UNLICENSED_CLASS,
		type DesktopTexts, type TaskbarPosition, type TileLayout, type WindowState } from '@antilia/core';

	type Props = {
		/** The windows, DesktopWindow components. */
		children?: Snippet;
		/** The side of the desktop the taskbar is on, bottom when left out. */
		taskbar?: TaskbarPosition;
		/** Whether the taskbar has the buttons tiling the windows, true when left out. */
		tiling?: boolean;
		/** Whether the taskbar has the button letting the desktop fill the page, true when left out. */
		expandable?: boolean;
		/** Whether the desktop fills the whole page, false when left out; the user changes it too. */
		expanded?: boolean;
		/** Whether the user may move the taskbar to another side, true when left out. */
		movableTaskbar?: boolean;
		/** Whether the taskbar has the menu listing the windows open and closed, true when left out. */
		menu?: boolean;
		/**
		 * Asked before the user closes a window with changes not saved, instead of the dialog of the
		 * desktop; resolving to true closes it.
		 */
		confirmClose?: (id: string, title: string) => Promise<boolean> | boolean;
		/** What the desktop says, in English when left out. */
		texts?: Partial<DesktopTexts>;
		/**
		 * A license key activating the desktop, a private component; the one given to activate() of
		 * the core when left out. Without one the desktop shows a placeholder.
		 */
		licenseKey?: string;
		id?: string;
		/** CSS classes of the desktop, next to antilia-desktop; one giving it a height, say. */
		class?: string;
		/**
		 * Called with the states of all windows, by their ids, once the user has changed any, moved
		 * the taskbar or let the desktop fill the page.
		 */
		onchange?: (states: Record<string, WindowState>) => void;
		/** Called with the side the user moved the taskbar to. */
		ontaskbar?: (position: TaskbarPosition) => void;
		/** Called with whether the desktop fills the page, once the user changed it. */
		onexpand?: (expanded: boolean) => void;
		/** Called with the id of the window the user brought to the front. */
		onactivate?: (id: string) => void;
		/** Called with the id of the window the user closed. */
		onclose?: (id: string) => void;
	};

	let {
		children,
		taskbar = 'bottom',
		tiling = true,
		expandable = true,
		expanded = false,
		movableTaskbar = true,
		menu = true,
		confirmClose,
		texts,
		licenseKey,
		id,
		class: className = '',
		onchange,
		ontaskbar,
		onexpand,
		onactivate,
		onclose
	}: Props = $props();

	let element = $state<HTMLDivElement>();
	let instance: Desktop | null = null;
	let licensed = $state<boolean | null>(null);
	// the side of the taskbar and whether the desktop fills the page: as the props say, or as the
	// user changed them since
	let position = $state<TaskbarPosition>(untrack(() => taskbar));
	let filling = $state(untrack(() => expanded));
	// the props as they were, which only override the user when they change
	let givenTaskbar = untrack(() => taskbar);
	let givenExpanded = untrack(() => expanded);

	$effect.pre(() => {
		if (taskbar !== givenTaskbar) {
			givenTaskbar = taskbar;
			position = taskbar;
		}
	});

	$effect.pre(() => {
		if (expanded !== givenExpanded) {
			givenExpanded = expanded;
			filling = expanded;
		}
	});

	$effect(() => {
		const key = licenseKey;
		let live = true;
		isLicensed(DESKTOP_COMPONENTS, key).then((active) => {
			if (live) {
				licensed = active;
			}
		});
		return () => {
			live = false;
		};
	});

	const classes = $derived(['antilia-desktop', 'antilia-desktop-taskbar-' + position, className]
		.filter(Boolean).join(' '));

	$effect(() => {
		if (!element) {
			return;
		}
		const desktop: Desktop = new Desktop(element, untrack(() => ({
			taskbar: position, tiling, expandable, expanded: filling, movableTaskbar, menu, texts,
			confirmClose: (window: string, title: string): Promise<boolean> | boolean => confirmClose ?
				confirmClose(window, title) : desktop.askToClose(title),
			onChange: (states: Record<string, WindowState>) => onchange?.(states),
			onActivate: (window: string) => onactivate?.(window),
			onClose: (window: string) => onclose?.(window),
			onTaskbar: (side: TaskbarPosition) => {
				position = side;
				ontaskbar?.(side);
			},
			onExpand: (value: boolean) => {
				filling = value;
				onexpand?.(value);
			}
		})));
		instance = desktop;
		return () => {
			desktop.destroy();
			instance = null;
		};
	});

	$effect(() => {
		const next = { taskbar: position, tiling, expandable, expanded: filling, movableTaskbar, menu,
			texts };
		untrack(() => instance?.configure(next));
	});

	/** @returns the desktop of the core, null before it is mounted */
	export function desktop(): Desktop | null {
		return instance;
	}

	/** @returns the states of all windows, by their ids */
	export function states(): Record<string, WindowState> {
		return instance?.states() ?? {};
	}

	/** Brings a window to the front, restoring it when minimized and opening it when closed. */
	export function activate(window: string): void {
		instance?.activate(window);
	}

	export function minimize(window: string): void {
		instance?.minimize(window);
	}

	export function maximize(window: string, maximized = true): void {
		instance?.maximize(window, maximized);
	}

	export function restore(window: string): void {
		instance?.restore(window);
	}

	export function fold(window: string, folded = true): void {
		instance?.fold(window, folded);
	}

	export function close(window: string): void {
		instance?.close(window);
	}

	/**
	 * Closes a window as its close button does, asking first when it has changes not saved;
	 * resolves to whether it closed.
	 */
	export function requestClose(window: string): Promise<boolean> {
		return instance?.requestClose(window) ?? Promise.resolve(false);
	}

	export function open(window: string): void {
		instance?.open(window);
	}

	/** Lays out the windows shown in a grid, side by side, one above the other, or cascading. */
	export function tile(layout: TileLayout = 'grid'): void {
		instance?.tile(layout);
	}

	/** Brings the next window to the front, or the previous one. */
	export function cycle(direction: 1 | -1 = 1): void {
		instance?.cycle(direction);
	}

	/** Lets the desktop fill the whole page, or takes it back into the page. */
	export function expand(value = true): void {
		instance?.expand(value);
		filling = value;
	}
</script>

{#if licensed === false}
	<div {id} class={UNLICENSED_CLASS} role="note" data-component="Desktop">
		{texts?.unlicensed ?? DEFAULT_DESKTOP_TEXTS.unlicensed}</div>
{:else if licensed}
	<div bind:this={element} {id} class={classes}>
		<div class="antilia-desktop-area">{@render children?.()}</div>
		<div class="antilia-desktop-taskbar"></div>
	</div>
{/if}
