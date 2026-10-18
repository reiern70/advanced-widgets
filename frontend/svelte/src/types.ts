/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

/**
 * An item of a Menu: a link when it has href, a button calling onclick otherwise, or the item
 * opening a submenu when it has items.
 */
export interface MenuItem {
	/** The name of the item, the key Svelte knows it by; its place in its list when left out. */
	id?: string;
	/** The label of the item. */
	label: string;
	/** The markup of an icon before the label, an svg, written as is. */
	icon?: string;
	/** The tooltip of the item, its title. */
	tooltip?: string;
	/** Where the item leads: it is a link then. */
	href?: string;
	/** Where the link opens, _blank for a new window. */
	target?: string;
	/** Called when the user chose the item, a link or a button. */
	onclick?: (event: MouseEvent) => void;
	/** Whether the user cannot choose the item, which shows dimmed; false when left out. */
	disabled?: boolean;
	/** Whether the item leads to the page shown, which the menu marks, and the items holding it. */
	current?: boolean;
	/** The items of the submenu the item opens; it has neither href nor onclick then. */
	items?: MenuEntry[];
	/** CSS classes of the link or the button of the item. */
	class?: string;
}

/** A line between the items of a submenu, or upright between those of a bar along the top. */
export interface MenuSeparator {
	separator: true;
	/** The key Svelte knows it by; its place in its list when left out. */
	id?: string;
}

/** An item or a separator of a Menu. */
export type MenuEntry = MenuItem | MenuSeparator;
