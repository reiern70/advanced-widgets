/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The Popovers demo: buttons around a target opening a popover on each side of it, in the design and
 * the variant chosen; a gallery of the designs, each drawn open and opening for real; content the
 * app renders into a popover, facts of the browser and a form renaming a project; help on the
 * fields of a form and more on the rows of a table.
 */

import type { PopoverDesign, PopoverPlacement, PopoverVariant } from '@antilia/core';

import { format } from './data.js';

/** The texts of the demo, by the keys of the page's bundle. */
export interface PopoversDemoTexts {
	placementsTitle: string;
	placementsText: string;
	design: string;
	variant: string;
	position: string;
	/** What a popover around the target says, {0} its placement. */
	placementText: string;
	galleryTitle: string;
	try: string;
	richTitle: string;
	statusTitle: string;
	statusText: string;
	project: string;
	rename: string;
	renameTitle: string;
	name: string;
	save: string;
	cancel: string;
	required: string;
	fieldsTitle: string;
	/** The name of a button giving help on a field, {0} its label. */
	help: string;
	tableTitle: string;
	country: string;
	language: string;
	currency: string;
	/** The name of a button telling more of a country, {0} its name. */
	aboutCountry: string;
	close: string;
	[key: string]: string;
}

/** A field of the form: the key of its label, its label and its help. */
export interface FieldConfig {
	key: string;
	label: string;
	help: string;
}

/** A country of the table, named in the language of the page, and more of it. */
export interface CountryConfig {
	code: string;
	name: string;
	language: string;
	currency: string;
	about: string;
}

/** What the page gives the demo. */
export interface PopoversConfig {
	texts: PopoversDemoTexts;
	/** Every placement, in the order of the buttons around the target, auto last. */
	placements: PopoverPlacement[];
	designs: PopoverDesign[];
	variants: PopoverVariant[];
	design: PopoverDesign;
	variant: PopoverVariant;
	/** The widest the popovers around the target get. */
	placementWidth: string;
	/** The positions of the gallery, every placement but auto, and the one it starts with. */
	positions: PopoverPlacement[];
	position: PopoverPlacement;
	/** The widest the popovers of the gallery get, drawn open or opened. */
	galleryWidth: string;
	project: string;
	fields: FieldConfig[];
	countries: CountryConfig[];
	/** The markup of the icons, as Wicket renders them. */
	icons: { info: string; help: string; rename: string; status: string; auto: string };
}

/**
 * @returns the name of a placement, a design or a variant, as the page has it under its enum's name
 */
export function nameOf(config: PopoversConfig, kind: 'placement' | 'design' | 'variant',
	value: string): string {
	return config.texts[kind + '.' + value.toUpperCase().replace('-', '_')] ?? value;
}

/**
 * @returns the side a popover of a placement other than auto opens on, with room, and where along
 *          it: from the start edge of its trigger, centered, or from the end edge
 */
export function sideOf(placement: PopoverPlacement): { side: string; align: string } {
	const [side, edge] = placement.split('-');
	return { side, align: edge === 'left' || edge === 'top' ? 'start' :
		edge === 'right' || edge === 'bottom' ? 'end' : 'center' };
}

/**
 * @returns what a popover around the target says, as the Wicket page has it
 */
export function placementText(config: PopoversConfig, placement: PopoverPlacement): string {
	return format(config.texts.placementText, nameOf(config, 'placement', placement));
}

/**
 * @returns what the browser says of itself, by the keys of their names: the time, its window, its
 *          language, and how many times the popover opened
 */
export function facts(opened: number): Array<[string, string]> {
	return [['time', new Date().toLocaleTimeString(document.documentElement.lang || undefined)],
		['window', window.innerWidth + ' × ' + window.innerHeight],
		['language', navigator.language],
		['opened', String(opened)]];
}
