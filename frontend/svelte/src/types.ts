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
