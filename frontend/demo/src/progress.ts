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
