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

/*
 * The Menus demo: the menu bar of an application, its items telling the app they were chosen and
 * one leading to the home page; the navigation of a site, its links followed by the app as a router
 * does, the section shown marked as current; a side bar opening its submenus in place, the view
 * chosen marked as current; a menu whose orientation, design and ways of opening its submenus
 * controls switch; and a gallery of the designs, along the top and down the side.
 */

import type { MenuDesign } from '@antilia/core';

import { format, type DropdownTexts } from './data.js';

/** The texts of the demo, by the keys of the page's bundle: the labels of the items among them. */
export interface MenusDemoTexts extends DropdownTexts {
	/** The accessible name of the pills of the cases. */
	cases: string;
	applicationTitle: string;
	application: string;
	applicationAppIntro: string;
	applicationStart: string;
	documentText: string;
	/** What the app says of an item chosen, {0} its labels and those of the items holding it. */
	picked: string;
	navigationTitle: string;
	navigation: string;
	navigationAppIntro: string;
	/** Where the navigation is, {0} the labels of the section. */
	at: string;
	/** What the app keeps, {0} the name of the section. */
	routed: string;
	sidebarTitle: string;
	sidebar: string;
	sidebarAppText: string;
	settingsTitle: string;
	settingsAppStart: string;
	vertical: string;
	design: string;
	inline: string;
	hover: string;
	galleryTitle: string;
	alongTheTop: string;
	downTheSide: string;
	[key: string]: string;
}

/**
 * An item of the menus of the demo as the page gives it: its name, the key of its label and, under
 * name.tooltip, of its tooltip; the markup of its icon, what it does on the Wicket page, whether it
 * is disabled and its items. A separator has no name.
 */
export interface EntryConfig {
	separator?: boolean;
	name?: string;
	/** ajax and link call the server on the Wicket page, page leads to the home page. */
	kind?: 'ajax' | 'link' | 'page';
	icon?: string;
	disabled?: boolean;
	items?: EntryConfig[];
}

/** What the page gives the demo. */
export interface MenusConfig {
	texts: MenusDemoTexts;
	application: EntryConfig[];
	navigation: EntryConfig[];
	sidebar: EntryConfig[];
	/** The address of the home page, where Help › Home page leads. */
	homeUrl: string;
	/** The section of the navigation, and the view of the side bar, shown first. */
	section: string;
	view: string;
	vertical: boolean;
	design: MenuDesign;
	inline: boolean;
	hover: boolean;
	/** Every design, in the order of the drop-down. */
	designs: MenuDesign[];
}

/**
 * An item of a menu of the demo before an app makes it its own: a separator, or an item with its
 * label, its tooltip and what it does, a link or an action, or its items.
 */
export interface DemoEntry {
	id: string;
	separator?: boolean;
	label?: string;
	/** The markup of its icon. */
	icon?: string;
	tooltip?: string;
	disabled?: boolean;
	href?: string;
	current?: boolean;
	action?: (event: Event) => void;
	items?: DemoEntry[];
}

/** What an item without a submenu does: lead somewhere, or act, and whether it is current. */
export type Leaf = Pick<DemoEntry, 'href' | 'action' | 'current'>;

/** The separator of the labels of an item and of the items holding it. */
export const PATH_SEPARATOR = ' › ';

/**
 * @returns the items of a menu of the demo, an item without a submenu doing what leaf says
 * @param leaf given an entry and its name and those of the entries holding it, from the bar down
 */
export function menuEntries(config: MenusConfig, entries: EntryConfig[],
	leaf: (entry: EntryConfig, path: string[]) => Leaf, path: string[] = []): DemoEntry[] {
	const { texts } = config;
	return entries.map((entry, index) => {
		if (entry.separator || !entry.name) {
			return { id: 'separator-' + index, separator: true };
		}
		const here = [...path, entry.name];
		const item: DemoEntry = { id: entry.name, label: texts[entry.name], icon: entry.icon,
			tooltip: texts[entry.name + '.tooltip'], disabled: entry.disabled };
		return entry.items ? { ...item, items: menuEntries(config, entry.items, leaf, here) } :
			{ ...item, ...leaf(entry, here) };
	});
}

/**
 * @returns the names of an entry and of those holding it, from the bar down; empty when there is
 *          no entry of the name
 */
export function pathOf(entries: EntryConfig[], name: string): string[] {
	for (const entry of entries) {
		if (entry.name === name) {
			return [name];
		}
		const inside = entry.items ? pathOf(entry.items, name) : [];
		if (inside.length) {
			return [entry.name!, ...inside];
		}
	}
	return [];
}

/**
 * @returns the labels of items, from the bar down, as the page says them
 */
export function describe(config: MenusConfig, path: string[]): string {
	return path.map((name) => config.texts[name]).join(PATH_SEPARATOR);
}

/**
 * @returns what the app says of an item chosen
 */
export function picked(config: MenusConfig, path: string[]): string {
	return format(config.texts.picked, describe(config, path));
}

/**
 * @returns where the navigation is, in words
 */
export function at(config: MenusConfig, section: string): string {
	return format(config.texts.at, describe(config, pathOf(config.navigation, section)));
}

/**
 * @returns the items of the application: Help › Home page leads to the home page, the others say
 *          they were chosen
 */
export function applicationEntries(config: MenusConfig, say: (message: string) => void): DemoEntry[] {
	return menuEntries(config, config.application, (entry, path) => entry.kind === 'page' ?
		{ href: config.homeUrl } : { action: () => say(picked(config, path)) });
}

/**
 * @returns the items of the navigation: links the app follows itself, as a router does, the one of
 *          the section current
 */
export function navigationEntries(config: MenusConfig, section: string,
	go: (section: string) => void): DemoEntry[] {
	return menuEntries(config, config.navigation, (entry) => ({ href: '#' + entry.name,
		current: entry.name === section, action: (event) => {
			event.preventDefault();
			go(entry.name!);
		} }));
}

/**
 * @returns the items of the side bar: each shows its view, the one shown current
 */
export function sidebarEntries(config: MenusConfig, view: string,
	show: (view: string) => void): DemoEntry[] {
	return menuEntries(config, config.sidebar, (entry) => ({ current: entry.name === view,
		action: () => show(entry.name!) }));
}

/**
 * @returns the items of the gallery: those of the side bar, the dashboard current, doing nothing
 */
export function galleryEntries(config: MenusConfig): DemoEntry[] {
	return menuEntries(config, config.sidebar, (entry) => ({ current: entry.name === 'dashboard',
		action: () => {} }));
}
