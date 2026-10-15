/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The Resizable demo: a note resizing its height, a side panel its width and a card both, which a
 * button takes away and puts back, coming back in the size it had; the sizes, which the app keeps,
 * said below them.
 */

import type { ResizableSize } from '@antilia/core';

import { format } from './data.js';

/** The texts of the demo, by the keys of the page's bundle. */
export interface ResizableDemoTexts {
	noteTitle: string;
	noteText: string;
	sideTitle: string;
	cardTitle: string;
	cardText: string;
	takeCardAway: string;
	putCardBack: string;
	/** The sizes: {0} the height of the note, {1} the width of the side panel, {2} the card's. */
	sizes: string;
	pageSize: string;
	away: string;
	[key: string]: string;
}

/** What the page gives the demo. */
export interface ResizableConfig {
	texts: ResizableDemoTexts;
	/** The accessible name of the handles. */
	handle: string;
	/** The items of the side panel, the keys of their texts. */
	items: string[];
	/** The size the card starts with. */
	card: { width?: number; height?: number };
}

/**
 * @returns the sizes the app knows, in words, as the Wicket page says them
 */
export function describe(config: ResizableConfig, note: number | null, side: number | null,
	card: ResizableSize | null): string {
	const { texts } = config;
	const pixels = (value: number | null) => value === null ? texts.pageSize : value + ' px';
	return format(texts.sizes, pixels(note), pixels(side), card ? pixels(card.width) + ' × ' +
		pixels(card.height) : texts.away);
}
