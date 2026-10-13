/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The Tabs demo: documents in closable tabs after a welcome tab that is not, each telling about
 * itself with an action and marked while it has changes not saved, which Save takes away; a button
 * opening new ones; tabs whose orientation and design a check box and a drop-down switch; and a
 * gallery of the designs made for tabs along the top and of those made for tabs down the side.
 */

import type { PopoverOptions, TabsDesign, TabsTexts } from '@antilia/core';

import { format, type DropdownTexts } from './data.js';

/** The texts of the demo, by the keys of the page's bundle. */
export interface TabsDemoTexts extends DropdownTexts {
	/** The accessible name of the pills of the cases. */
	cases: string;
	documentsTitle: string;
	documents: string;
	welcome: string;
	welcomeText: string;
	newDocument: string;
	save: string;
	about: string;
	/** What is known of a document: {0} its name, {1} its lines, {2} its characters, {3} saved. */
	info: string;
	unsaved: string;
	allSaved: string;
	/** The name of a new document, {0} its number. */
	untitled: string;
	closed: string;
	saved: string;
	opened: string;
	start: string;
	settingsTitle: string;
	settings: string;
	vertical: string;
	design: string;
	selected: string;
	[key: string]: string;
}

/** A document as the page gives it. */
export interface DocumentConfig {
	name: string;
	text: string;
}

/** A part of the gallery: the designs it shows, the tabs it shows them with and the one selected. */
export interface GalleryConfig {
	designs: TabsDesign[];
	/** The tabs: the keys of their titles, their texts under key.text, and their icons' markup. */
	tabs: Array<{ name: string; icon: string }>;
	selected?: string;
}

/** What the page gives the demo. */
export interface TabsConfig {
	texts: TabsDemoTexts;
	/** What the tabs say. */
	tabs: TabsTexts;
	documents: DocumentConfig[];
	/** The sections of the second tabs, the keys of their titles; their texts under key.text. */
	sections: string[];
	/** The markup of the icons, as Wicket renders them. */
	icons: { welcome: string; document: string; info: string };
	vertical: boolean;
	design: TabsDesign;
	/** Every design, in the order of the drop-down. */
	designs: TabsDesign[];
	/** The designs made for tabs along the top and those made for tabs down the side. */
	gallery: { top: GalleryConfig; side: GalleryConfig };
}

/** A document of the app: its name, its text and the text it was saved with. */
export interface Document {
	/** The name of its tab. */
	id: string;
	name: string;
	text: string;
	saved: string;
}

/**
 * @returns the documents the page gives, as the app keeps them
 */
export function documentsOf(config: TabsConfig): Document[] {
	return config.documents.map((document, index) => ({ id: 'document-' + index,
		name: document.name, text: document.text, saved: document.text }));
}

/**
 * @returns a new document, empty
 */
export function newDocument(config: TabsConfig, number: number): Document {
	return { id: 'untitled-' + number, name: format(config.texts.untitled, number), text: '',
		saved: '' };
}

/**
 * @returns what is known of a document, in words, as the Wicket page says it
 */
export function describe(config: TabsConfig, document: Document): string {
	const lines = document.text === '' ? 0 : document.text.split('\n').length;
	return format(config.texts.info, document.name, lines, document.text.length,
		document.text !== document.saved ? config.texts.unsaved : config.texts.allSaved);
}

/**
 * @returns what a popover about a document shows, as the Wicket page has it: above its tab, its
 *          name and what is known of it, in amber with changes not saved
 */
export function popoverOf(config: TabsConfig, document: Document): PopoverOptions {
	return { title: document.name, body: describe(config, document), placement: 'top-center',
		variant: document.text !== document.saved ? 'warning' : 'default', closeLabel: config.tabs.close };
}
