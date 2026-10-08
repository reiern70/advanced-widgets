/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { useEffect, useState } from 'react';

import { Dropdown } from '@advanced-widgets/react';

import { describe, loadChoices, type Choices, type Texts } from './data.js';

/**
 * A country and skills chosen with the React drop-downs, from choices the server sends.
 */
export function ReactDemo({ url, texts }: { url: string; texts: Texts }) {
	const [choices, setChoices] = useState<Choices | null>(null);
	const [country, setCountry] = useState('');
	const [skills, setSkills] = useState<string[]>([]);

	useEffect(() => {
		loadChoices(url).then(setChoices, (error) => console.error(error));
	}, [url]);

	if (!choices) {
		return <p className="framework-demo__loading">{texts.loading}</p>;
	}
	const common = { filter: texts.filter, noMatch: texts.noMatch, more: texts.more,
		remove: texts.remove };
	return (
		<div className="framework-demo__form">
			<label className="framework-demo__field">
				{texts.country}{' '}
				<Dropdown name="country" options={[{ value: '', label: texts.countryNone },
					...choices.countries]} value={country} onChange={setCountry}
					placeholder={texts.countryNone} {...common} />
			</label>
			<label className="framework-demo__field">
				{texts.skills}{' '}
				<Dropdown name="skills" multiple options={choices.skills} value={skills}
					onChange={setSkills} placeholder={texts.skillsNone} filterThreshold={0}
					{...common} />
			</label>
			<p className="framework-demo__chosen">{describe(texts, choices, country, skills)}</p>
		</div>
	);
}
