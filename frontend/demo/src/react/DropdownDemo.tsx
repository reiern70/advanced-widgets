/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { useEffect, useState, type FormEvent } from 'react';

import { Dropdown } from '@advanced-widgets/react';

import { format, loadChoices, type Choice } from '../data.js';
import { describe, type DropdownConfig, type DropdownValues } from '../dropdown.js';
import { Feedback, type Message } from './Feedback.js';

/**
 * The Dropdown demo in React: the form of the Wicket page, in its markup, the sentence below it
 * following every choice in the browser.
 */
export function DropdownDemo({ config }: { config: DropdownConfig }) {
	const { texts } = config;
	const [countries, setCountries] = useState<Choice[]>([]);
	const [values, setValues] = useState<DropdownValues>(config.values);
	const [messages, setMessages] = useState<Message[]>([]);

	useEffect(() => {
		loadChoices(config.choicesUrl).then((choices) => setCountries(choices.countries),
			(error) => console.error(error));
	}, [config.choicesUrl]);

	const set = <K extends keyof DropdownValues>(key: K) => (value: DropdownValues[K]) =>
		setValues((current) => ({ ...current, [key]: value }));
	const sizes = config.sizes.map((size) => ({ value: size, label: size }));
	const common = { filter: texts.filter, noMatch: texts.noMatch, more: texts.more,
		remove: texts.remove };
	const choices = describe(config, countries, values);
	const send = (event: FormEvent) => {
		event.preventDefault();
		setMessages([{ level: 'info', text: format(texts.sent, choices) }]);
	};

	return (
		<div className="dropdown-demo">
			<form className="dropdown-demo__form" onSubmit={send}>
				<section>
					<h3>{texts.single}</h3>
					<label className="dropdown-demo__field">
						{texts.size}{' '}
						<Dropdown name="size" options={sizes} value={values.size}
							onChange={set('size')} {...common} />
					</label>
					<label className="dropdown-demo__field">
						{texts.country}{' '}
						<Dropdown name="country" options={countries} value={values.country}
							onChange={set('country')} placeholder={texts['country.none']}
							{...common} />
					</label>
				</section>
				<section>
					<h3>{texts.multiple}</h3>
					<label className="dropdown-demo__field">
						{texts.skills}{' '}
						<Dropdown name="skills" multiple options={config.skills} value={values.skills}
							onChange={set('skills')} {...common} />
					</label>
					<label className="dropdown-demo__field">
						{texts.languages}{' '}
						<Dropdown name="languages" multiple options={config.languages}
							value={values.languages} onChange={set('languages')}
							placeholder={texts['languages.none']} filterThreshold={0} {...common} />
					</label>
				</section>
				<section>
					<h3>{texts.disabledTitle}</h3>
					<label className="dropdown-demo__field">
						{texts.size}{' '}
						<Dropdown name="disabled" options={sizes} value={values.disabled} disabled
							{...common} />
					</label>
				</section>
				<p><button type="submit" className="dropdown-demo__send">{texts.send}</button></p>
			</form>
			<p className="dropdown-demo__choices">{choices}</p>
			<Feedback messages={messages} />
		</div>
	);
}
