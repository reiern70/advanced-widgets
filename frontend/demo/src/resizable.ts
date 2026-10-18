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
