/*
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
 * The Scrollbar demo: boxes scrolling with the browser's bar and with the slim ones, and a box
 * that grows, scrolled to the first of the lines added, to its top, its bottom, a position and a
 * line.
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
	toTop: string;
	toBottom: string;
	toPosition: string;
	toLine: string;
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

/** The pixels from the top the growing box scrolls to, as on the Wicket page. */
export const POSITION = 100;

/** The line the growing box scrolls to, as on the Wicket page. */
export const LINE = 10;

/**
 * @returns the CSS selector of a line of the growing box, numbered from 1
 */
export function line(number: number): string {
	return '[data-line="' + number + '"]';
}

/** The tiles of the box scrolling horizontally. */
export const TILES = Array.from({ length: 12 }, (_, index) => index + 1);

/**
 * @returns the lines of a pattern, numbered from 1
 */
export function lines(pattern: string, count: number): string[] {
	return Array.from({ length: count }, (_, index) => format(pattern, index + 1));
}
