/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The Progress bar demo: a bar advanced by a timer, a finished one and an indeterminate one.
 */

/** The texts of the demo, by the keys of the page's bundle. */
export interface ProgressTexts {
	known: string;
	unknown: string;
	finished: string;
	reset: string;
	start: string;
	pause: string;
}

/** What the page gives the demo. */
export interface ProgressConfig {
	texts: ProgressTexts;
	/** The progress of the first bar, as the page has it. */
	progress: number;
}

/** How often the bar advances, and by how much, as the Ajax timer of the Wicket page does. */
export const TICK_MILLIS = 300;

export const STEP = 3;

/**
 * @returns the progress after a tick
 */
export function advance(progress: number): number {
	return Math.min(100, progress + STEP);
}
