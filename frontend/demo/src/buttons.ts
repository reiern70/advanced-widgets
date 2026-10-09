/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The Buttons demo: every variant in every size, with a text, an icon and a text, or an icon only,
 * telling which one was clicked; disabled ones; and a link drawn as a button.
 */

import { BUTTON_SIZES, BUTTON_VARIANTS, type ButtonSize,
	type ButtonVariant } from '@antilia/core';

import { format } from './data.js';

/** The texts of the demo, by the keys of the page's bundle. */
export interface ButtonsTexts {
	variant: string;
	/** Which button was clicked, {0} standing for it. */
	clicked: string;
	'clicked.none': string;
	disabled: string;
	asLink: string;
	'asLink.text': string;
	'size.small': string;
	'size.medium': string;
	'size.large': string;
	/** The label of the drop-down choosing the icons, and those of its choices. */
	icons: string;
	'icons.SVG': string;
	'icons.FONT_AWESOME': string;
	/** What the drop-down says. */
	filter: string;
	noMatch: string;
	more: string;
	remove: string;
}

/**
 * The icons of the buttons, as the drop-down of the demo chooses them: the Font Awesome icons as
 * inline SVG, or the same drawn by the Font Awesome font.
 */
export type ButtonIconSet = 'SVG' | 'FONT_AWESOME';

/** The choices of the drop-down choosing the icons, in their order. */
export const BUTTON_ICON_SETS: ButtonIconSet[] = ['SVG', 'FONT_AWESOME'];

/** A variant as the page shows it: its name, the text of its buttons and their icon. */
export interface VariantSample {
	name: string;
	text: string;
	/** The markup of the icon in each set, as the server wrote it. */
	icons: Record<ButtonIconSet, string>;
}

/** What the page gives the demo. */
export interface ButtonsConfig {
	texts: ButtonsTexts;
	variants: Record<ButtonVariant, VariantSample>;
	/** The icons chosen at first, those the Wicket buttons of the page show. */
	icons: ButtonIconSet;
}

export { BUTTON_SIZES, BUTTON_VARIANTS };

/**
 * @returns what the demo says once a button was clicked: its variant, size and text
 */
export function clickedText(config: ButtonsConfig, variant: ButtonVariant,
	size: ButtonSize): string {
	const sample = config.variants[variant];
	return format(config.texts.clicked, sample.name + ' · ' + sizeName(config, size) + ' · ' +
		sample.text);
}

/**
 * @returns the options of the drop-down choosing the icons of the buttons
 */
export function iconSetOptions(texts: ButtonsTexts): { value: string; label: string }[] {
	return BUTTON_ICON_SETS.map((value) => ({ value, label: texts[`icons.${value}`] }));
}

/**
 * @returns the name of a size in the language of the page
 */
export function sizeName(config: ButtonsConfig, size: ButtonSize): string {
	return config.texts[`size.${size}`];
}
