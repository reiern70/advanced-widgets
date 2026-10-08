/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The Buttons demo: every variant in every size, with a text, an icon and a text, or an icon only,
 * telling which one was clicked; disabled ones; and a link drawn as a button.
 */

import { BUTTON_SIZES, BUTTON_VARIANTS, type ButtonSize,
	type ButtonVariant } from '@advanced-widgets/core';

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
}

/** A variant as the page shows it: its name, the text of its buttons and their icon. */
export interface VariantSample {
	name: string;
	text: string;
	/** The markup of the icon, an inline SVG the server wrote. */
	icon: string;
}

/** What the page gives the demo. */
export interface ButtonsConfig {
	texts: ButtonsTexts;
	variants: Record<ButtonVariant, VariantSample>;
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
 * @returns the name of a size in the language of the page
 */
export function sizeName(config: ButtonsConfig, size: ButtonSize): string {
	return config.texts[`size.${size}`];
}
