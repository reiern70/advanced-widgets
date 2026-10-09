/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { useMemo, useState, type FormEvent } from 'react';

import { Autocomplete } from '@antilia/react';

import { describe, MAX_SUGGESTIONS, searchCountries, type AutocompleteConfig,
	type AutocompleteValues } from '../autocomplete.js';
import { format, searchUsers } from '../data.js';
import { Feedback, type Message } from './Feedback.js';

/**
 * The Autocomplete demo in React: the form of the Wicket page, in its markup, the users searched
 * through the users resource, the countries filtered in the browser.
 */
export function AutocompleteDemo({ config }: { config: AutocompleteConfig }) {
	const { texts } = config;
	const [values, setValues] = useState<AutocompleteValues>(config.values);
	const [messages, setMessages] = useState<Message[]>([]);
	const users = useMemo(() => searchUsers(config.usersUrl, MAX_SUGGESTIONS),
		[config.usersUrl]);
	const countries = useMemo(() => searchCountries(config.choicesUrl), [config.choicesUrl]);

	const set = (key: keyof AutocompleteValues) => (value: AutocompleteValues[typeof key]) =>
		setValues((current) => ({ ...current, [key]: value }));
	const common = { noMatch: texts.noMatch, searching: texts.searching, remove: texts.remove };
	const choices = describe(texts, values);
	const send = (event: FormEvent) => {
		event.preventDefault();
		setMessages([values.owner.length ? { level: 'info', text: format(texts.sent, choices) }
			: { level: 'error', text: texts.required }]);
	};

	return (
		<div className="autocomplete-demo">
			<form className="autocomplete-demo__form" onSubmit={send}>
				<section>
					<h3>{texts.single}</h3>
					<label className="autocomplete-demo__field">
						{texts.owner}{' '}
						<Autocomplete name="owner" search={users} value={values.owner}
							onChange={set('owner')} template={config.userTemplate} minLength={1}
							placeholder={texts['owner.placeholder']} {...common} />
					</label>
				</section>
				<section>
					<h3>{texts.multiple}</h3>
					<label className="autocomplete-demo__field">
						{texts.team}{' '}
						<Autocomplete name="team" multiple search={users} value={values.team}
							onChange={set('team')} template={config.userTemplate} minLength={1}
							placeholder={texts['team.placeholder']} {...common} />
					</label>
					<label className="autocomplete-demo__field">
						{texts.countries}{' '}
						<Autocomplete name="countries" multiple search={countries}
							value={values.countries} onChange={set('countries')}
							template={config.countryTemplate} minLength={0}
							placeholder={texts['countries.placeholder']} {...common} />
					</label>
				</section>
				<p><button type="submit" className="autocomplete-demo__send">{texts.send}</button></p>
			</form>
			<p className="autocomplete-demo__choices">{choices}</p>
			<Feedback messages={messages} />
		</div>
	);
}
