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

import { useMemo, useRef, useState, type FormEvent } from 'react';

import { submitForm, type AutocompleteSuggestion } from '@antilia/core';
import { Autocomplete } from '@antilia/react';

import { calling, describe, flag, MAX_SUGGESTIONS, searchActions, searchCountries, searched,
	userActions, type AutocompleteConfig, type AutocompleteValues } from '../autocomplete.js';
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
	// whom the phone at the end of a row of a user called last
	const [call, setCall] = useState('');
	const actions = useMemo(() => userActions(config), [config]);
	const infoAction = useMemo(() => searchActions(config), [config]);
	// the text of the search box, and what it sent last
	const [query, setQuery] = useState('');
	const [result, setResult] = useState('');
	const searchForm = useRef<HTMLFormElement>(null);
	const onAction = (_action: string, user: AutocompleteSuggestion) =>
		setCall(calling(texts, user));
	const users = useMemo(() => searchUsers(config.usersUrl, MAX_SUGGESTIONS),
		[config.usersUrl]);
	const countries = useMemo(() => searchCountries(config.choicesUrl), [config.choicesUrl]);
	// the find bar leaves out the countries listed already
	const visits = useRef(values.visits);
	visits.current = values.visits;
	const unlisted = useMemo(() => searchCountries(config.choicesUrl, () => visits.current),
		[config.choicesUrl]);

	const set = (key: keyof AutocompleteValues) => (value: AutocompleteValues[typeof key]) =>
		setValues((current) => ({ ...current, [key]: value }));
	const common = { noMatch: texts.noMatch, searching: texts.searching, remove: texts.remove };
	const choices = describe(texts, values);
	const send = (event: FormEvent) => {
		event.preventDefault();
		setMessages([values.owner.length ? { level: 'info', text: format(texts.sent, choices) }
			: { level: 'error', text: texts.required }]);
	};
	// the text the form holds: a suggestion chosen sends it before React has rendered it
	const search = (event: FormEvent<HTMLFormElement>) => {
		event.preventDefault();
		const text = String(new FormData(event.currentTarget).get('search') ?? '').trim();
		if (text) {
			void searched(config, text).then(setResult);
		}
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
							actions={actions} onAction={onAction}
							placeholder={texts['owner.placeholder']} {...common} />
					</label>
				</section>
				<section>
					<h3>{texts.multiple}</h3>
					<label className="autocomplete-demo__field">
						{texts.team}{' '}
						<Autocomplete name="team" multiple search={users} value={values.team}
							onChange={set('team')} template={config.userTemplate} minLength={1}
							actions={actions} onAction={onAction}
							placeholder={texts['team.placeholder']} {...common} />
					</label>
					<label className="autocomplete-demo__field">
						{texts.countries}{' '}
						<Autocomplete name="countries" multiple search={countries}
							value={values.countries} onChange={set('countries')}
							template={config.countryTemplate} highlightMatch minLength={0}
							placeholder={texts['countries.placeholder']} {...common} />
					</label>
				</section>
				<section>
					<h3>{texts.disabledTitle}</h3>
					<label className="autocomplete-demo__field">
						{texts.reviewer}{' '}
						<Autocomplete name="reviewer" search={users} value={values.reviewer} disabled
							template={config.userTemplate} {...common} />
					</label>
					<label className="autocomplete-demo__field">
						{texts.markets}{' '}
						<Autocomplete name="markets" multiple search={countries} value={values.markets}
							disabled template={config.countryTemplate} {...common} />
					</label>
				</section>
				<section>
					<h3>{texts.find}</h3>
					<label className="autocomplete-demo__field">
						{texts.visits}{' '}
						<Autocomplete name="visitFind" pick search={unlisted}
							onPick={(country) => set('visits')(values.visits.concat([country]))}
							template={config.countryTemplate} highlightMatch minLength={0}
							placeholder={texts['visits.placeholder']} {...common} />
					</label>
					{values.visits.length ? (
						<ul className="autocomplete-demo__list">
							{values.visits.map((country) => (
								<li key={country.id}>
									<span className="ac-country">
										<span className="ac-flag" aria-hidden="true">
											{flag(String(country.id))}
										</span>
										{country.label}
									</span>
									<button type="button" className="autocomplete-demo__remove"
										aria-label={format(texts.remove, country.label)}
										onClick={() => set('visits')(values.visits.filter(
											(listed) => listed.id !== country.id))}>×</button>
								</li>
							))}
						</ul>
					) : <p className="autocomplete-demo__none">{texts['visits.none']}</p>}
				</section>
				<p><button type="submit" className="autocomplete-demo__send">{texts.send}</button></p>
			</form>
			<form ref={searchForm} className="autocomplete-demo__form autocomplete-demo__search"
				onSubmit={search}>
				<section>
					<h3>{texts.searchTitle}</h3>
					<label className="autocomplete-demo__field">
						{texts.search}{' '}
						<Autocomplete name="search" freeText search={users} text={query}
							onTextChange={setQuery} template={config.searchTemplate} highlightMatch
							minLength={1} actions={infoAction}
							onChoose={() => submitForm(searchForm.current!)}
							placeholder={texts['search.placeholder']} {...common} />
					</label>
					<p><button type="submit" className="autocomplete-demo__send">{texts.searchSend}</button></p>
					<p className="autocomplete-demo__searched" role="status">{result}</p>
				</section>
			</form>
			<p className="autocomplete-demo__choices">{choices}</p>
			{call && <p className="autocomplete-demo__calling" role="status">{call}</p>}
			<Feedback messages={messages} />
		</div>
	);
}
