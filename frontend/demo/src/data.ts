/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * What the Wicket page gives the React and Svelte demos: the element they render into carries
 * the URL of the choices, a resource mounted by the demo application, and the texts in the
 * language of the session.
 */

export interface Choice {
	value: string;
	label: string;
}

/** The choices served by the mounted resource, named in the language of the session. */
export interface Choices {
	countries: Choice[];
	skills: Choice[];
}

/** The texts of a demo, in the language of the session. */
export interface Texts {
	country: string;
	countryNone: string;
	skills: string;
	skillsNone: string;
	filter: string;
	noMatch: string;
	more: string;
	/** The label of the x of a chip, {0} standing for its text. */
	remove: string;
	/** What is chosen, with {0} for the country and {1} for the skills. */
	chosen: string;
	nothing: string;
	loading: string;
}

/**
 * @param element the element a demo renders into
 * @returns the URL of the choices and the texts
 */
export function settingsOf(element: HTMLElement): { url: string; texts: Texts } {
	return {
		url: element.dataset.choices ?? '',
		texts: JSON.parse(element.dataset.texts ?? '{}') as Texts
	};
}

/**
 * @param url the URL of the mounted resource
 * @returns the choices
 */
export async function loadChoices(url: string): Promise<Choices> {
	const response = await fetch(url, { headers: { Accept: 'application/json' } });
	if (!response.ok) {
		throw new Error(`Loading ${url} failed: ${response.status}`);
	}
	return response.json() as Promise<Choices>;
}

/**
 * @returns what is chosen, in words
 */
export function describe(texts: Texts, choices: Choices | null, country: string,
	skills: string[]): string {
	const label = (list: Choice[] | undefined, value: string) =>
		list?.find((choice) => choice.value === value)?.label ?? value;
	return texts.chosen
		.replace('{0}', country ? label(choices?.countries, country) : texts.nothing)
		.replace('{1}', skills.length ? skills.map((skill) => label(choices?.skills, skill))
			.join(', ') : texts.nothing);
}
