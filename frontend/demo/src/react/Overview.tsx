/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { useEffect, useMemo, useState } from 'react';

import type { AutocompleteValue } from '@advanced-widgets/core';
import { Autocomplete, Dropdown, InfiniteList } from '@advanced-widgets/react';

import { loadChoices, pagesOfUsers, searchUsers, type Choices } from '../data.js';
import { describe, SUGGESTION_TEMPLATE, USER_TEMPLATE, type OverviewConfig } from '../overview.js';

/**
 * A country, skills and colleagues chosen with the React components, from data the server sends,
 * and a list of all users scrolling infinitely.
 */
export function Overview({ config }: { config: OverviewConfig }) {
	const { texts } = config;
	const [choices, setChoices] = useState<Choices | null>(null);
	const [country, setCountry] = useState('');
	const [skills, setSkills] = useState<string[]>([]);
	const [colleagues, setColleagues] = useState<AutocompleteValue[]>([]);
	const [picked, setPicked] = useState<string | null>(null);
	const search = useMemo(() => searchUsers(config.usersUrl), [config.usersUrl]);
	const load = useMemo(() => pagesOfUsers(config.usersUrl), [config.usersUrl]);

	useEffect(() => {
		loadChoices(config.choicesUrl).then(setChoices, (error) => console.error(error));
	}, [config.choicesUrl]);

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
			<div className="framework-demo__field">
				<span>{texts.colleagues}</span>
				<Autocomplete name="colleagues" multiple search={search} value={colleagues}
					onChange={setColleagues} template={SUGGESTION_TEMPLATE}
					placeholder={texts.colleaguesNone} searching={texts.searching}
					noMatch={texts.noMatch} remove={texts.remove} />
			</div>
			<p className="framework-demo__colleagues">
				{colleagues.map((colleague) => colleague.label).join(', ') || texts.nothing}
			</p>
			<h4 className="framework-demo__heading">{texts.people}</h4>
			<InfiniteList load={load} template={USER_TEMPLATE} height={{ value: 240, unit: 'px' }}
				texts={{ loading: texts.listLoading, end: texts.listEnd, empty: texts.listEmpty }}
				onAction={(_action, key) => setPicked(key)} />
			<p className="framework-demo__picked">
				{picked ? texts.picked.replace('{0}', picked) : texts.nothing}
			</p>
		</div>
	);
}
