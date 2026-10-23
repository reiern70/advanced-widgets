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

import { useState, type FormEvent } from 'react';

import { DatePicker, DateTimePicker, Dropdown } from '@antilia/react';

import { format } from '../data.js';
import { calendarLocale, describe, type DatePickerConfig,
	type DatePickerValues } from '../date-picker.js';
import { Feedback, type Message } from './Feedback.js';

/**
 * The date picker demo in React: the form of the Wicket page, in its markup, the sentence below it
 * following every change in the browser.
 */
export function DatePickerDemo({ config }: { config: DatePickerConfig }) {
	const { texts, bounds } = config;
	const [values, setValues] = useState<DatePickerValues>(config.values);
	const [messages, setMessages] = useState<Message[]>([]);

	const set = (key: keyof DatePickerValues) => (value: string) =>
		setValues((current) => ({ ...current, [key]: value }));
	const locale = calendarLocale(config, values.calendarLocale);
	const date = { ...config.dateTexts, locale };
	const dateTime = { ...config.dateTimeTexts, locale };
	const choices = describe(config, values);
	const send = (event: FormEvent) => {
		event.preventDefault();
		setMessages([{ level: 'info', text: format(texts.sent, choices) }]);
	};

	return (
		<div className="datepicker-demo">
			<form className="datepicker-demo__form" onSubmit={send}>
				<section>
					<h3>{texts.dates}</h3>
					<label className="datepicker-demo__field">
						{texts.born}{' '}
						<DatePicker name="born" value={values.born} onChange={set('born')}
							max={bounds.bornMax} {...date} />
					</label>
					<label className="datepicker-demo__field">
						{texts.start}{' '}
						<DatePicker name="start" value={values.start} onChange={set('start')}
							min={bounds.startMin} max={bounds.startMax} {...date} />
					</label>
				</section>
				<section>
					<h3>{texts.datesAndTimes}</h3>
					<label className="datepicker-demo__field">
						{texts.appointment}{' '}
						<DateTimePicker name="appointment" value={values.appointment}
							onChange={set('appointment')} min={bounds.appointmentMin} minuteStep={15}
							{...dateTime} />
					</label>
					<label className="datepicker-demo__field">
						{texts.departure}{' '}
						<DateTimePicker name="departure" value={values.departure}
							onChange={set('departure')} {...dateTime} />
					</label>
				</section>
				<section>
					<h3>{texts.localeTitle}</h3>
					<label className="datepicker-demo__field">
						{texts.calendarLocale}{' '}
						<Dropdown name="calendarLocale" options={config.locales}
							value={values.calendarLocale} onChange={set('calendarLocale')}
							filter={texts.filter} noMatch={texts.noMatch} more={texts.more}
							remove={texts.remove} />
					</label>
					<label className="datepicker-demo__field">
						{texts.disabledTitle}{' '}
						<DatePicker name="disabled" value={values.disabled} disabled {...date} />
					</label>
				</section>
				<p><button type="submit" className="datepicker-demo__send">{texts.send}</button></p>
			</form>
			<p className="datepicker-demo__choices">{choices}</p>
			<Feedback messages={messages} />
		</div>
	);
}
