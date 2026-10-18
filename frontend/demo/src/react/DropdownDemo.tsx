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

import { useEffect, useRef, useState, type FormEvent } from 'react';

import { DESKTOP_DISCARD_EVENT } from '@antilia/core';
import { Dropdown } from '@antilia/react';

import { format, loadChoices, type Choice } from '../data.js';
import { describe, type DropdownConfig, type DropdownValues } from '../dropdown.js';
import { Feedback, type Message } from './Feedback.js';

/**
 * The Dropdown demo in React: the form of the Wicket page, in its markup, the sentence below it
 * following every choice in the browser. Choices not sent are changes a window of a desktop asks
 * about before it closes, and drops when told to.
 */
export function DropdownDemo({ config }: { config: DropdownConfig }) {
	const { texts } = config;
	const [countries, setCountries] = useState<Choice[]>([]);
	const [values, setValues] = useState<DropdownValues>(config.values);
	// the choices sent last, the page's at first
	const [sent, setSent] = useState<DropdownValues>(config.values);
	const root = useRef<HTMLDivElement>(null);
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
		setSent(values);
		setMessages([{ level: 'info', text: format(texts.sent, choices) }]);
	};
	const modified = JSON.stringify(values) !== JSON.stringify(sent);

	// the window of a desktop the demo is in drops the choices not sent when it closes
	useEffect(() => {
		const window = root.current?.closest('.antilia-window');
		const discard = () => setValues(sent);
		window?.addEventListener(DESKTOP_DISCARD_EVENT, discard);
		return () => window?.removeEventListener(DESKTOP_DISCARD_EVENT, discard);
	}, [sent]);

	return (
		<div ref={root} className="dropdown-demo" data-modified={String(modified)}>
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
