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
 * The Dropdown demo: a size and a country, skills and languages, and a disabled drop-down.
 */

import { format, labelOf, type Choice, type DropdownTexts } from './data.js';

/** The texts of the demo, by the keys of the page's bundle. */
export interface DropdownDemoTexts extends DropdownTexts {
	single: string;
	multiple: string;
	disabledTitle: string;
	size: string;
	country: string;
	'country.none': string;
	skills: string;
	languages: string;
	'languages.none': string;
	send: string;
	nothing: string;
	/** What is chosen: {0} the size, {1} the country, {2} the skills and {3} the languages. */
	choices: string;
	/** What was sent, {0} standing for it. */
	sent: string;
}

/** The choices of the demo, as the page has them. */
export interface DropdownValues {
	size: string;
	country: string;
	skills: string[];
	languages: string[];
	disabled: string;
}

/** What the page gives the demo. */
export interface DropdownConfig {
	texts: DropdownDemoTexts;
	sizes: string[];
	skills: Choice[];
	languages: Choice[];
	values: DropdownValues;
	/** The URL of the choices resource, which names the countries. */
	choicesUrl: string;
}

/**
 * @param countries the countries, once loaded
 * @returns what is chosen, in words, as the Wicket page says it
 */
export function describe(config: DropdownConfig, countries: Choice[], values: DropdownValues):
	string {
	const { texts } = config;
	const list = (chosen: string[], choices: Choice[]) => chosen.length ?
		chosen.map((value) => labelOf(choices, value)).join(', ') : texts.nothing;
	return format(texts.choices, values.size,
		values.country ? labelOf(countries, values.country) : texts.nothing,
		list(values.skills, config.skills), list(values.languages, config.languages));
}
