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
 * The Dropdown demo: a size and a country, skills and languages, and a disabled drop-down; on the
 * drop-down page, a unit of measure chosen in a dialog from a long list of long, grouped options.
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
	/** The label of the group of the countries chosen most, first in the list. */
	'country.common': string;
	/** The label of the group of the other countries, under them. */
	'country.others': string;
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

/** An option in a group, the label of the group with it. */
export interface GroupedChoice extends Choice {
	group?: string;
}

/** The texts of the dialog of the units, by the keys of the page's bundle. */
export interface UnitsTexts {
	'units.title': string;
	/** The unit chosen, {0} standing for it. */
	'units.chosen': string;
	'units.choose': string;
	/** The title of the dialog. */
	'units.dialog': string;
	'units.text': string;
	'units.unit': string;
	'units.ok': string;
	'units.cancel': string;
	/** The label of the close button of the dialog. */
	'units.close': string;
}

/** The icons of the buttons of the dialog of the units, inline SVG. */
export interface UnitsIcons {
	choose: string;
	cancel: string;
	ok: string;
}

/**
 * The unit of measure chosen in a dialog, which only the drop-down page has: the units in their
 * groups, the few used most first, each in words too long for a line of the list.
 */
export interface UnitsConfig {
	texts: UnitsTexts;
	options: GroupedChoice[];
	/** The code of the unit chosen at first. */
	value: string;
	icons: UnitsIcons;
}

/** What the page gives the demo. */
export interface DropdownConfig {
	texts: DropdownDemoTexts;
	sizes: string[];
	skills: Choice[];
	languages: Choice[];
	/** The codes of the countries chosen most, first in the list in a group of their own. */
	commonCountries: string[];
	values: DropdownValues;
	/** The URL of the choices resource, which names the countries. */
	choicesUrl: string;
	/** The dialog of the units, on the drop-down page only. */
	units?: UnitsConfig;
}

/**
 * @param countries the countries as the choices resource names them, sorted by their names
 * @returns the countries in two groups, as the Wicket drop-down has them: those chosen most
 *          first, then all the others
 */
export function groupCountries(config: DropdownConfig, countries: Choice[]): GroupedChoice[] {
	const { texts, commonCountries } = config;
	const common = (country: Choice) => commonCountries.includes(country.value);
	return [
		...countries.filter(common).map((country) => ({ ...country,
			group: texts['country.common'] })),
		...countries.filter((country) => !common(country)).map((country) => ({ ...country,
			group: texts['country.others'] }))
	];
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
