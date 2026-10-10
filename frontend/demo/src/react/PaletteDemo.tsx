/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { useState, type FormEvent } from 'react';

import { Palette } from '@antilia/react';

import { format } from '../data.js';
import { describe, describeCountries, type PaletteConfig, type PaletteValues } from '../palette.js';
import { Feedback, type Message } from './Feedback.js';

/**
 * The Palette demo in React: the form of the Wicket page, in its markup, the sentence below it
 * following every change in the browser.
 */
export function PaletteDemo({ config }: { config: PaletteConfig }) {
	const { texts } = config;
	const [values, setValues] = useState<PaletteValues>(config.values);
	const [messages, setMessages] = useState<Message[]>([]);

	const set = <K extends keyof PaletteValues>(key: K) => (value: PaletteValues[K]) =>
		setValues((current) => ({ ...current, [key]: value }));
	// without an order of their own, the columns take that of the choices again
	const order = (ordered: boolean) => setValues((current) => ({ ...current, ordered,
		columns: ordered ? current.columns : config.columns.map((column) => column.value)
			.filter((column) => current.columns.includes(column)) }));
	const choices = describe(config, values);
	const send = (event: FormEvent) => {
		event.preventDefault();
		setMessages([{ level: 'info', text: format(texts.sent, choices) }]);
	};

	return (
		<div className="palette-demo">
			<form className="palette-demo__form" onSubmit={send}>
				<section>
					<h3>{texts.unordered}</h3>
					<label className="palette-demo__check">
						<input type="checkbox" checked={values.filterable}
							onChange={(event) => set('filterable')(event.currentTarget.checked)} />
						{' '}{texts.filterable}
					</label>
					<div className="palette-demo__field">
						<span>{texts.skills}</span>
						<Palette name="skills" options={config.skills} value={values.skills}
							filterable={values.filterable} onChange={set('skills')}
							{...config.palette} />
					</div>
				</section>
				<section>
					<h3>{texts.orderedTitle}</h3>
					<div className="palette-demo__checks">
						<label className="palette-demo__check">
							<input type="checkbox" checked={values.ordered}
								onChange={(event) => order(event.currentTarget.checked)} />
							{' '}{texts.ordered}
						</label>
						<label className="palette-demo__check">
							<input type="checkbox" checked={values.numbered}
								onChange={(event) => set('numbered')(event.currentTarget.checked)} />
							{' '}{texts.numbered}
						</label>
					</div>
					<div className="palette-demo__field">
						<span>{texts.columns}</span>
						<Palette name="columns" options={config.columns} value={values.columns}
							ordered={values.ordered} numbered={values.numbered}
							onChange={set('columns')} {...config.palette}
							availableTitle={texts.columnsAvailable}
							selectedTitle={texts.columnsSelected} />
					</div>
				</section>
				<section>
					<h3>{texts.manyTitle}</h3>
					<div className="palette-demo__field">
						<span>{texts.countries}</span>
						<Palette name="countries" options={config.countries}
							value={values.countries} onChange={set('countries')}
							{...config.palette} />
					</div>
					<p className="palette-demo__count">
						{describeCountries(config, values.countries)}
					</p>
				</section>
				<section>
					<h3>{texts.disabledTitle}</h3>
					<div className="palette-demo__field">
						<span>{texts.skills}</span>
						<Palette options={config.skills} value={values.disabled} disabled
							{...config.palette} />
					</div>
				</section>
				<p><button type="submit" className="palette-demo__send">{texts.send}</button></p>
			</form>
			<p className="palette-demo__choices">{choices}</p>
			<Feedback messages={messages} />
		</div>
	);
}
