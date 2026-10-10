/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The date picker demo: a birthday and the start of a contract, an appointment and a departure,
 * the locale of the calendars and a disabled picker.
 */

import { parseIso, type DatePickerOptions } from '@antilia/core';

import { format, type Choice, type DropdownTexts } from './data.js';

/** The texts of the demo, by the keys of the page's bundle. */
export interface DatePickerDemoTexts extends DropdownTexts {
	dates: string;
	datesAndTimes: string;
	localeTitle: string;
	born: string;
	start: string;
	appointment: string;
	departure: string;
	calendarLocale: string;
	disabledTitle: string;
	send: string;
	nothing: string;
	/** What is chosen: {0} the birthday, {1} the start, {2} the appointment, {3} the departure. */
	choices: string;
	/** What was sent, {0} standing for it. */
	sent: string;
}

/** The values of the demo as ISO text, empty for none, as the page has them. */
export interface DatePickerValues {
	born: string;
	start: string;
	appointment: string;
	departure: string;
	disabled: string;
	/** The locale of the calendars, 'page' for the language of the page. */
	calendarLocale: string;
}

/** What the page gives the demo. */
export interface DatePickerConfig {
	texts: DatePickerDemoTexts;
	/** The texts of a date picker, and of a date and time picker, as Wicket has them. */
	dateTexts: DatePickerOptions;
	dateTimeTexts: DatePickerOptions;
	/** The language of the page, a BCP 47 tag. */
	language: string;
	/** The locales of the calendars, 'page' standing for the language of the page. */
	locales: Choice[];
	bounds: { bornMax: string; startMin: string; startMax: string; appointmentMin: string };
	values: DatePickerValues;
}

/**
 * @returns a value as the language of the page writes it, text that is no date as typed
 */
function describeValue(config: DatePickerConfig, value: string, time: boolean): string {
	if (!value) {
		return config.texts.nothing;
	}
	const parts = parseIso(value);
	if (!parts) {
		return value;
	}
	return new Intl.DateTimeFormat(config.language, { dateStyle: 'medium', timeZone: 'UTC',
		...(time ? { timeStyle: 'short' } : {}) })
		.format(Date.UTC(parts.year, parts.month - 1, parts.day, parts.hour, parts.minute));
}

/**
 * @returns what is chosen, in words, as the Wicket page says it
 */
export function describe(config: DatePickerConfig, values: DatePickerValues): string {
	return format(config.texts.choices, describeValue(config, values.born, false),
		describeValue(config, values.start, false), describeValue(config, values.appointment, true),
		describeValue(config, values.departure, true));
}

/**
 * @returns the locale of the calendars: the one chosen, else the language of the page
 */
export function calendarLocale(config: DatePickerConfig, chosen: string): string {
	return chosen && chosen !== 'page' ? chosen : config.language;
}
