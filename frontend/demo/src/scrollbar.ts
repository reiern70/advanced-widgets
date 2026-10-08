/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The Scrollbar demo: boxes scrolling with the browser's bar and with the slim ones, and a box
 * that grows.
 */

import { format } from './data.js';

/** The texts of the demo, by the keys of the page's bundle. */
export interface ScrollbarDemoTexts {
	native: string;
	vertical: string;
	horizontal: string;
	both: string;
	growing: string;
	addLines: string;
	/** A line of the growing box, {0} standing for its number. */
	line: string;
	/** A long line, {0} standing for its number. */
	'line.long': string;
}

/** What the page gives the demo. */
export interface ScrollbarConfig {
	texts: ScrollbarDemoTexts;
	/** The lines of the growing box, as the page shows them. */
	lines: number;
}

/** The lines added to the growing box at a time, as on the Wicket page. */
export const MORE_LINES = 20;

/** The tiles of the box scrolling horizontally. */
export const TILES = Array.from({ length: 12 }, (_, index) => index + 1);

/**
 * @returns the lines of a pattern, numbered from 1
 */
export function lines(pattern: string, count: number): string[] {
	return Array.from({ length: count }, (_, index) => format(pattern, index + 1));
}
