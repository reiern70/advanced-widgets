/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import type { Snippet } from 'svelte';

/**
 * An option of a Dropdown.
 */
export interface DropdownOption {
	value: string;
	label: string;
	disabled?: boolean;
}

/**
 * A type of window, which the launcher of the taskbar opens: the desktop renders a new window of
 * it in the pane shown, each time the user opens one, and takes it away when the user closes it.
 */
export type DesktopWindowType = {
	/** The name of the type. */
	type: string;
	/** What the launcher says for it, and the title of its windows. */
	title: string;
	/** The markup of its icon, an svg, in the launcher and on its windows. */
	icon?: string;
	/** The body of a window of the type, given the id of the window. */
	content: Snippet<[string]>;
	/** Where its windows start and their size, in pixels, and the like. */
	window?: { x?: number; y?: number; width?: number; height?: number; minWidth?: number;
		minHeight?: number; resizable?: boolean; maximizable?: boolean; minimizable?: boolean;
		foldable?: boolean; modified?: boolean; class?: string; bodyClass?: string };
};

/**
 * A tab of Tabs: its name, its title and what the user may do with it. The panel, the icon and
 * the actions are snippets of Tabs, given the tab.
 */
export interface TabItem {
	/** The name of the tab, which the tabs know it by. */
	id: string;
	/** The title of the tab. */
	title: string;
	/** Whether the tab has a button closing it, false when left out. */
	closable?: boolean;
	/**
	 * Whether the panel has changes not saved, which the user is asked about before the tab
	 * closes; when left out, whether a field in it holds another value than the one it was
	 * rendered with.
	 */
	modified?: boolean;
	/** CSS classes of the tab, next to antilia-tab. */
	class?: string;
	/** CSS classes of the panel, next to antilia-tabs-panel. */
	panelClass?: string;
}
