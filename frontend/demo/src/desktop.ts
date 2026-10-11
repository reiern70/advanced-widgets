/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The Desktop demo: a desktop with the JSON grid of the framework, notes, the drop-downs of the
 * framework, six more demos of the framework and the keyboard shortcuts in its windows, and a
 * choice of the side of its taskbar.
 */

import type { DesktopTexts, TaskbarPosition } from '@antilia/core';

import type { AutocompleteConfig } from './autocomplete.js';
import type { ButtonsConfig } from './buttons.js';
import type { DatePickerConfig } from './date-picker.js';
import type { DropdownConfig } from './dropdown.js';
import type { JsonListConfig } from './json-list.js';
import type { JsonTableConfig } from './json-table.js';
import type { ProgressConfig } from './progress.js';
import type { ModalDemoConfig } from './team.js';

/** The texts of the demo, by the keys of the page's bundle. */
export interface DesktopDemoTexts {
	taskbar: string;
	'taskbar.bottom': string;
	'taskbar.top': string;
	'taskbar.left': string;
	'taskbar.right': string;
	openKeyboard: string;
	reactUsers: string;
	svelteUsers: string;
	reactDropdowns: string;
	svelteDropdowns: string;
	keyboard: string;
	notes: string;
	notesText: string;
	'key.f11': string;
	'key.next': string;
	'key.previous': string;
	'key.doubleClick': string;
	'key.fold': string;
	'key.taskbar': string;
	'key.taskbarHelp': string;
	'key.menu': string;
	'key.menuHelp': string;
	'key.close': string;
	'key.closeHelp': string;
	'key.dragTaskbar': string;
	'key.dragTaskbarHelp': string;
	'key.escape': string;
}

/** What the page gives the demo. */
export interface DesktopConfig {
	texts: DesktopDemoTexts;
	/** What the desktop says, in the language of the page. */
	desktop: DesktopTexts;
	/** The grid of a window, as the JSON grid demo has it, without its settings. */
	jsonTable: JsonTableConfig;
	/** The drop-downs of a window, as the Dropdown demo has them. */
	dropdown: DropdownConfig;
	/** The other demos in windows, as their pages give them. */
	demos: DesktopDemos;
}

/** A demo in a window: the title of the window, and the demo's configuration. */
export interface DesktopDemo<C> {
	title: string;
	config: C;
}

/** The other demos in windows, by the names of their apps. */
export interface DesktopDemos {
	autocomplete: DesktopDemo<AutocompleteConfig>;
	buttons: DesktopDemo<ButtonsConfig>;
	datePicker: DesktopDemo<DatePickerConfig>;
	jsonList: DesktopDemo<JsonListConfig>;
	modalDialog: DesktopDemo<ModalDemoConfig>;
	progressBar: DesktopDemo<ProgressConfig>;
}

/** The names of the windows of the demo in a framework. */
export type DesktopWindowName = 'users' | 'notes' | 'dropdowns' | 'keyboard' | keyof DesktopDemos;

/**
 * @returns where the window of the other demo at an index starts, and its size: cascading from
 *          the right of the drop-downs
 */
export function demoBounds(index: number): { x: number; y: number; width: number;
	height: number } {
	return { x: 300 + index * 40, y: 20 + index * 30, width: 700, height: 420 };
}

/** The sides of the desktop the taskbar can be on, in the order the choice offers them. */
export const TASKBAR_POSITIONS: TaskbarPosition[] = ['bottom', 'top', 'left', 'right'];

/**
 * @returns the id of a window of the demo in a framework, unique in the page, which shows the
 *          demos of all frameworks
 */
export function windowId(framework: string, name: DesktopWindowName): string {
	return framework + '-desktop-' + name;
}

/** A keyboard shortcut: the keys, or the words for what the user does, and what it does. */
export interface Shortcut {
	keys?: string[];
	action?: string;
	text: string;
}

/**
 * @returns the keyboard shortcuts, as the Wicket page lists them
 */
export function shortcuts(texts: DesktopDemoTexts): Shortcut[] {
	return [
		{ keys: ['F11'], text: texts['key.f11'] },
		{ keys: ['Ctrl', 'F6'], text: texts['key.next'] },
		{ keys: ['Ctrl', 'Shift', 'F6'], text: texts['key.previous'] },
		{ action: texts['key.doubleClick'], text: texts['key.fold'] },
		{ action: texts['key.taskbar'], text: texts['key.taskbarHelp'] },
		{ action: texts['key.menu'], text: texts['key.menuHelp'] },
		{ action: texts['key.close'], text: texts['key.closeHelp'] },
		{ action: texts['key.dragTaskbar'], text: texts['key.dragTaskbarHelp'] },
		{ keys: ['Esc'], text: texts['key.escape'] }
	];
}

/** The paths of the icons of the windows, in a 24 by 24 box: a sheet of notes, a keyboard. */
export const NOTES_ICON = 'M6 3h9l4 4v14H6z M9 11h7 M9 15h7';
export const KEYBOARD_ICON = 'M2 6h20v12H2z M6 10h1 M10 10h1 M14 10h1 M18 10h0 M7 14h10';

/** The color of the badge of each framework, as the Wicket page has them. */
export const FRAMEWORK_COLORS: Record<string, string> = { react: '#087ea4', svelte: '#ff3e00' };
