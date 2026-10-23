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

/*
 * A date picker, or a date and time picker, drawn over an <input> that holds its value as ISO
 * text: 2026-10-09 for a date, 2026-10-09T14:30 for a date and time, as <input type="date"> and
 * <input type="datetime-local"> have them.
 *
 * new DatePicker(input, options) puts the input into a div.antilia-datepicker, or uses the one it
 * is in already, and adds a text field, a button and a calendar to it; the input itself is hidden
 * but stays in the page, holds the value and fires its 'input' and 'change' events when it
 * changes, so forms, Ajax behaviors and the bindings of a framework work with it as with any input.
 *
 * Everything the user reads follows the locale of the picker: the text field shows the value in the
 * order and with the separators of the locale (09.10.2026, 10/09/2026, 2026/10/09) and reads it
 * back typed so, or as ISO text; the calendar names the months and the days of the week in the
 * language of the locale and starts the week on its first day; a time has the clock of the locale,
 * 24 hours or 12 with AM and PM. The texts of the buttons are options, given in the language of
 * the page.
 *
 * Text that is no date is kept in the input as typed, so that a form posts it and the server
 * reports it as invalid; the field is then marked aria-invalid, as is a date outside min and max.
 *
 * A framework rendering the input renders the wrapper too, so that the picker never moves an
 * element the framework owns: <div class="antilia-datepicker"><input>...</div>.
 */

import { onRemoving, type Unsubscribe } from './host.js';
import { lookOf, partClasses, toggleParts, type Look, type LookPart } from './look.js';

/**
 * The texts of a date picker, the locale it follows and the dates it takes.
 */
export interface DatePickerOptions {
	/** The locale, a BCP 47 tag such as de-DE; the language of the page when left out. */
	locale?: string;
	/** The look of the picker; that of the element it is in when left out (see lookOf). */
	look?: Look;
	/** Whether the picker takes a time too. */
	time?: boolean;
	/** The first date that can be chosen, ISO text. */
	min?: string;
	/** The last date that can be chosen, ISO text. */
	max?: string;
	/** The minutes the minute field steps by, 1 when left out. */
	minuteStep?: number;
	/** The first day of the week, 0 for Sunday to 6 for Saturday; the locale's when left out. */
	firstDayOfWeek?: number;
	/** Whether the clock has 12 hours and AM and PM; the locale's when left out. */
	hour12?: boolean;
	/** The text of the empty field; the format of the locale, written with symbols, when left out. */
	placeholder?: string;
	/** The symbols of the day, month, year, hour and minute, apart by spaces: 'dd mm yyyy hh mm'. */
	symbols?: string;
	/** The label of the button opening the calendar. */
	open?: string;
	/** The label of the button showing the month before. */
	previousMonth?: string;
	/** The label of the button showing the month after. */
	nextMonth?: string;
	/** The label of the button showing the year before. */
	previousYear?: string;
	/** The label of the button showing the year after. */
	nextYear?: string;
	/** The label of the button showing the years before. */
	previousYears?: string;
	/** The label of the button showing the years after. */
	nextYears?: string;
	/** The label of the title, which shows the months of the year to choose from. */
	chooseMonth?: string;
	/** The label of the title, which shows the years to choose from. */
	chooseYear?: string;
	/** The button choosing today. */
	today?: string;
	/** The button choosing now, in a date and time picker. */
	now?: string;
	/** The button emptying the value. */
	clear?: string;
	/** The button closing the calendar of a date and time picker. */
	close?: string;
	/** The label of the hour field. */
	hour?: string;
	/** The label of the minute field. */
	minute?: string;
}

/** The parts of a date and time, the month counting from 1. */
export interface DateParts {
	year: number;
	month: number;
	day: number;
	hour: number;
	minute: number;
}

/** What the calendar shows: the days of a month, the months of a year, or twelve years. */
type View = 'days' | 'months' | 'years';

const WRAPPER_CLASS = 'antilia-datepicker';

/** The regions whose week starts on Sunday, and on Saturday, as CLDR has them. */
const SUNDAY_REGIONS = new Set(('AG AS BD BR BS BT BW BZ CA CN CO DM DO ET GT GU HK HN ID IL IN ' +
	'JM JP KE KH KR LA MH MM MO MT MX MZ NI NP PA PE PH PK PR PT PY SA SG SV TH TT TW UM US VE VI ' +
	'WS YE ZA ZW').split(' '));

const SATURDAY_REGIONS = new Set('AE AF BH DJ DZ EG IQ IR JO KW LY OM QA SD SY'.split(' '));

/** The years the years view shows at once. */
const YEARS_SHOWN = 12;

/** The columns of the months and years views, which the up and down keys skip. */
const COLUMNS = 3;

/** The date pickers by their wrapper. */
const instances = new WeakMap<Element, DatePicker>();

let counter = 0;

/**
 * Sets the value of an input past the value tracker of React, so that React sees the change.
 */
function setNativeValue(input: HTMLInputElement, value: string): void {
	Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!.call(input, value);
}

function create<K extends keyof HTMLElementTagNameMap>(tag: K, className?: string,
	text?: string): HTMLElementTagNameMap[K] {
	const element = document.createElement(tag);
	if (className) {
		element.className = className;
	}
	if (text !== undefined) {
		element.textContent = text;
	}
	return element;
}

function button(className: string, text?: string, label?: string): HTMLButtonElement {
	const element = create('button', className, text);
	element.type = 'button';
	if (label) {
		element.setAttribute('aria-label', label);
		element.title = label;
	}
	return element;
}

function resolve(input: HTMLInputElement | string | null | undefined): HTMLInputElement | null {
	const element = typeof input === 'string' ? document.getElementById(input) : input;
	return element instanceof HTMLInputElement ? element : null;
}

function pad(value: number, length = 2): string {
	return String(value).padStart(length, '0');
}

/**
 * @param month counting from 1
 * @returns the days of the month
 */
export function daysInMonth(year: number, month: number): number {
	return new Date(Date.UTC(year, month, 0)).getUTCDate();
}

/**
 * @param text ISO text: 2026-10-09, 2026-10-09T14:30 or 2026-10-09 14:30, seconds left out
 * @returns its parts, null when it is no date
 */
export function parseIso(text: string | null | undefined): DateParts | null {
	const match = /^(\d{4})-(\d{1,2})-(\d{1,2})(?:[T ](\d{1,2}):(\d{2})(?::\d{2}(?:\.\d+)?)?)?$/
		.exec((text ?? '').trim());
	if (!match) {
		return null;
	}
	return valid({ year: Number(match[1]), month: Number(match[2]), day: Number(match[3]),
		hour: Number(match[4] ?? 0), minute: Number(match[5] ?? 0) });
}

/**
 * @param time whether to write the time too
 * @returns the parts as ISO text
 */
export function formatIso(parts: DateParts, time: boolean): string {
	const date = `${pad(parts.year, 4)}-${pad(parts.month)}-${pad(parts.day)}`;
	return time ? `${date}T${pad(parts.hour)}:${pad(parts.minute)}` : date;
}

function valid(parts: DateParts): DateParts | null {
	const { year, month, day, hour, minute } = parts;
	return year >= 1 && year <= 9999 && month >= 1 && month <= 12 && day >= 1 &&
		day <= daysInMonth(year, month) && hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59 ?
		parts : null;
}

/** Compares two dates, and their times when asked to. */
function compare(a: DateParts, b: DateParts, time: boolean): number {
	const value = (parts: DateParts) => ((parts.year * 13 + parts.month) * 32 + parts.day) *
		(time ? 1440 : 1) + (time ? parts.hour * 60 + parts.minute : 0);
	return value(a) - value(b);
}

function utc(parts: Pick<DateParts, 'year' | 'month' | 'day'> & Partial<DateParts>): Date {
	const date = new Date(Date.UTC(2000, parts.month - 1, parts.day, parts.hour ?? 0,
		parts.minute ?? 0));
	// years before 100 are not taken as 19xx
	date.setUTCFullYear(parts.year);
	return date;
}

/** Moves a date by days, keeping its time. */
function addDays(parts: DateParts, days: number): DateParts {
	const date = utc(parts);
	date.setUTCDate(date.getUTCDate() + days);
	return { ...parts, year: date.getUTCFullYear(), month: date.getUTCMonth() + 1,
		day: date.getUTCDate() };
}

/** Moves a date by months, keeping its day where the month has it, else taking the last one. */
function addMonths(parts: DateParts, months: number): DateParts {
	const index = parts.year * 12 + parts.month - 1 + months;
	const year = Math.floor(index / 12);
	const month = index - year * 12 + 1;
	return { ...parts, year, month, day: Math.min(parts.day, daysInMonth(year, month)) };
}

/** Today and now, in the time zone of the browser. */
function current(): DateParts {
	const now = new Date();
	return { year: now.getFullYear(), month: now.getMonth() + 1, day: now.getDate(),
		hour: now.getHours(), minute: now.getMinutes() };
}

/**
 * @param locale a BCP 47 tag
 * @returns the first day of the week of the locale's region, 0 for Sunday to 6 for Saturday
 */
export function firstDayOfWeek(locale: string): number {
	try {
		const tag = new Intl.Locale(locale) as Intl.Locale & {
			getWeekInfo?(): { firstDay: number };
			weekInfo?: { firstDay: number };
		};
		const info = tag.getWeekInfo?.() ?? tag.weekInfo;
		if (info) {
			return info.firstDay % 7;
		}
		const region = tag.maximize().region ?? '';
		return SUNDAY_REGIONS.has(region) ? 0 : SATURDAY_REGIONS.has(region) ? 6 : 1;
	} catch {
		return 1;
	}
}

/**
 * Writes and reads dates and times the way a locale does, with Western digits.
 */
export class DateFormat {
	readonly locale: string;

	readonly time: boolean;

	readonly hour12: boolean;

	/** The order of the numbers in the text, such as day, month, year, hour, minute. */
	readonly order: Array<'day' | 'month' | 'year' | 'hour' | 'minute'>;

	/** The text of AM and PM, as the locale writes them. */
	readonly dayPeriods: [string, string];

	private readonly format: Intl.DateTimeFormat;

	/**
	 * @param locale a BCP 47 tag
	 * @param time whether to write the time too
	 * @param hour12 whether the clock has 12 hours; the locale's when left out
	 */
	constructor(locale: string, time: boolean, hour12?: boolean) {
		this.locale = locale;
		this.time = time;
		const clock = new Intl.DateTimeFormat(locale, { hour: 'numeric', timeZone: 'UTC' });
		const cycle = clock.resolvedOptions().hourCycle;
		this.hour12 = hour12 ?? (cycle === 'h11' || cycle === 'h12');
		this.format = new Intl.DateTimeFormat(locale, {
			calendar: 'gregory',
			numberingSystem: 'latn',
			timeZone: 'UTC',
			year: 'numeric',
			month: '2-digit',
			day: '2-digit',
			...(time ? { hour: '2-digit', minute: '2-digit', hourCycle: this.hour12 ? 'h12' : 'h23' }
				: {})
		});
		this.order = this.parts({ year: 2026, month: 10, day: 9, hour: 14, minute: 30 })
			.map((part) => part.type)
			.filter((type): type is DateFormat['order'][number] =>
				['day', 'month', 'year', 'hour', 'minute'].includes(type));
		const period = (hour: number) => new Intl.DateTimeFormat(locale, { hour: 'numeric',
			hourCycle: 'h12', timeZone: 'UTC' }).formatToParts(Date.UTC(2026, 0, 1, hour))
			.find((part) => part.type === 'dayPeriod')?.value ?? (hour < 12 ? 'AM' : 'PM');
		this.dayPeriods = [period(9), period(21)];
	}

	private parts(parts: DateParts): Intl.DateTimeFormatPart[] {
		return this.format.formatToParts(utc(parts));
	}

	/**
	 * @returns the date as the locale writes it
	 */
	write(parts: DateParts): string {
		// the year is written with four digits, which some locales leave out for early years
		return this.parts(parts).map((part) => part.type === 'year' ? pad(parts.year, 4) : part.value)
			.join('');
	}

	/**
	 * @param symbols the symbols of the day, month, year, hour and minute, apart by spaces
	 * @returns the format, written with the symbols: dd.mm.yyyy, mm/dd/yyyy, hh:mm AM/PM...
	 */
	pattern(symbols = 'dd mm yyyy hh mm'): string {
		const [day, month, year, hour, minute] = symbols.split(/\s+/);
		const names: Record<string, string> = { day, month, year, hour, minute,
			dayPeriod: this.dayPeriods.join('/') };
		return this.parts({ year: 2026, month: 10, day: 9, hour: 14, minute: 30 })
			.map((part) => part.type === 'literal' ? part.value : names[part.type] ?? part.value)
			.join('');
	}

	/**
	 * Reads a date as the locale writes it, or as ISO text. The numbers are taken in the order of
	 * the locale, whatever stands between them: 9.10.26 and 09 10 2026 are both the 9th of
	 * October 2026 in Germany. A year of two digits is taken in the century that puts it at most
	 * twenty years ahead. A date and time picker takes a date without time as its midnight.
	 *
	 * @returns its parts, null when it is no date
	 */
	read(text: string): DateParts | null {
		const trimmed = text.trim();
		const iso = parseIso(trimmed);
		if (iso) {
			return iso;
		}
		let numbers: string[] = trimmed.match(/\d+/g) ?? [];
		const dateOrder = this.order.filter((type) => type === 'day' || type === 'month' ||
			type === 'year');
		if (numbers.length === 1 && (numbers[0].length === 6 || numbers[0].length === 8)) {
			// typed without separators: 09102026, 091026
			const digits = numbers[0];
			const yearLength = digits.length - 4;
			numbers = [];
			let at = 0;
			for (const type of dateOrder) {
				const length = type === 'year' ? yearLength : 2;
				numbers.push(digits.slice(at, at + length));
				at += length;
			}
		}
		// a date and time picker takes the date alone, or the time without its minutes
		const order = numbers.length > 3 && this.time ? this.order : dateOrder;
		if (numbers.length !== order.length && !(order === this.order &&
			numbers.length === order.length - 1)) {
			return null;
		}
		const parts: DateParts = { year: 0, month: 0, day: 0, hour: 0, minute: 0 };
		order.forEach((type, index) => {
			parts[type] = Number(numbers[index] ?? 0);
		});
		const yearText = numbers[order.indexOf('year')];
		if (yearText.length <= 2) {
			const now = new Date().getFullYear();
			parts.year += 2000;
			if (parts.year > now + 20) {
				parts.year -= 100;
			}
		}
		if (order.includes('hour') && this.hour12) {
			if (parts.hour < 1 || parts.hour > 12) {
				return null;
			}
			const pm = this.isPm(trimmed);
			parts.hour = parts.hour % 12 + (pm ? 12 : 0);
		}
		return valid(parts);
	}

	private isPm(text: string): boolean {
		const normalize = (value: string) => value.toLowerCase().replace(/[\s.  ]/g, '');
		const typed = normalize(text);
		const [am, pm] = this.dayPeriods.map(normalize);
		if (typed.includes(pm)) {
			return true;
		}
		if (typed.includes(am)) {
			return false;
		}
		return /p/.test(typed.replace(/[^a-z]/g, ''));
	}
}

/**
 * The date picker of an input.
 */
export class DatePicker {
	readonly input: HTMLInputElement;

	options: DatePickerOptions;

	wrapper!: HTMLElement;

	id!: string;

	/** The text field the user reads and types the value in. */
	field!: HTMLInputElement;

	/** The button opening the calendar. */
	toggle!: HTMLButtonElement;

	/** The calendar, a non-modal dialog. */
	popup!: HTMLDivElement;

	title!: HTMLButtonElement;

	previous!: HTMLButtonElement;

	next!: HTMLButtonElement;

	/** The days, months or years to choose from. */
	body!: HTMLDivElement;

	hourField: HTMLInputElement | null = null;

	minuteField: HTMLInputElement | null = null;

	dayPeriod: HTMLButtonElement | null = null;

	format!: DateFormat;

	/** What the calendar shows. */
	view: View = 'days';

	/** The date the keyboard is on, whose month, year or years the calendar shows. */
	focused!: DateParts;

	/** Whether the wrapper was made by the picker, and is taken away again by destroy(). */
	private ownWrapper = false;

	/** The look of the picker, whose classes the elements it makes get. */
	private look!: Look;

	/** Whether the text field holds text typed since the value was last taken or shown. */
	private typing = false;

	private listeners: Array<[EventTarget, string, EventListener]> = [];

	private stopWatchingRemoval: Unsubscribe | null = null;

	/**
	 * @param input the input
	 * @param options the texts, the locale and the dates taken
	 */
	constructor(input: HTMLInputElement, options?: DatePickerOptions) {
		this.input = input;
		this.options = { ...options };
		this.attach();
	}

	/**
	 * Draws the date picker of an input.
	 *
	 * @param input the input or its id
	 * @param options the texts, the locale and the dates taken
	 * @returns the date picker, null when there is no such input
	 */
	static attach(input: HTMLInputElement | string, options?: DatePickerOptions): DatePicker | null {
		const element = resolve(input);
		return element ? new DatePicker(element, options) : null;
	}

	/**
	 * @param input the input or its id
	 * @returns the date picker of an input, if it has one
	 */
	static of(input: HTMLInputElement | string | null | undefined): DatePicker | null {
		const element = resolve(input);
		const wrapper = element?.parentElement;
		const picker = wrapper ? instances.get(wrapper) : undefined;
		return picker && picker.input === element ? picker : null;
	}

	/**
	 * @returns the classes of parts of the picker in its look
	 */
	private css(...parts: LookPart[]): string {
		return partClasses(this.look, ...parts);
	}

	private attach(): void {
		const input = this.input;
		this.look = this.options.look ?? lookOf(input);
		let wrapper = input.parentElement;
		if (!wrapper || !wrapper.classList.contains(WRAPPER_CLASS)) {
			wrapper = create('div', this.css('datepicker'));
			input.parentNode?.insertBefore(wrapper, input);
			wrapper.appendChild(input);
			this.ownWrapper = true;
		}
		instances.get(wrapper)?.detach();
		Array.from(wrapper.children).forEach((child) => {
			if (child !== input) {
				child.remove();
			}
		});
		instances.set(wrapper, this);
		this.wrapper = wrapper;
		this.id = input.id || ('antilia-datepicker-' + (++counter));
		input.classList.add('antilia-datepicker-native');
		input.tabIndex = -1;
		input.setAttribute('aria-hidden', 'true');

		const box = create('div', this.css('datepicker-field'));
		this.field = create('input', this.css('datepicker-text'));
		this.field.type = 'text';
		this.field.id = this.id + '-text';
		this.field.autocomplete = 'off';
		this.field.spellcheck = false;
		box.appendChild(this.field);
		this.toggle = button(this.css('datepicker-toggle'));
		this.toggle.setAttribute('aria-haspopup', 'dialog');
		this.toggle.setAttribute('aria-expanded', 'false');
		this.toggle.setAttribute('aria-controls', this.id + '-calendar');
		this.toggle.appendChild(create('span', this.css('datepicker-icon')));
		box.appendChild(this.toggle);
		wrapper.appendChild(box);

		this.popup = create('div', this.css('datepicker-popup'));
		this.popup.id = this.id + '-calendar';
		this.popup.setAttribute('role', 'dialog');
		this.popup.hidden = true;
		const header = create('div', this.css('datepicker-header'));
		this.previous = button(this.css('datepicker-previous'));
		this.title = button(this.css('datepicker-title'));
		this.title.id = this.id + '-title';
		this.title.setAttribute('aria-live', 'polite');
		this.next = button(this.css('datepicker-next'));
		header.append(this.previous, this.title, this.next);
		this.popup.appendChild(header);
		this.body = create('div', this.css('datepicker-body'));
		this.popup.appendChild(this.body);
		wrapper.appendChild(this.popup);

		this.configure(this.options);
		this.bind();
		this.stopWatchingRemoval = onRemoving(wrapper, () => this.detach());
	}

	/**
	 * Takes other texts, another locale or other dates.
	 *
	 * @param options the texts, the locale and the dates taken
	 */
	configure(options?: DatePickerOptions): void {
		this.options = { ...options };
		const open = this.isOpen();
		this.close(false);
		const locale = this.locale();
		this.format = new DateFormat(locale, this.isTime(), this.options.hour12);
		this.wrapper.classList.toggle('antilia-datepicker-time', this.isTime());
		this.field.placeholder = this.options.placeholder ?? this.format.pattern(this.options.symbols);
		this.field.lang = locale;
		this.setLabel(this.toggle, this.options.open || (this.isTime() ? 'Choose date and time'
			: 'Choose date'));
		this.renderFooter();
		this.update();
		if (open) {
			this.open();
		}
	}

	/**
	 * @returns the locale the picker follows: its option, else the language of the page or browser
	 */
	locale(): string {
		const wanted = this.options.locale || this.input.closest<HTMLElement>('[lang]')?.lang ||
			navigator.language || 'en';
		try {
			return Intl.getCanonicalLocales(wanted)[0] ?? 'en';
		} catch {
			return 'en';
		}
	}

	isTime(): boolean {
		return Boolean(this.options.time);
	}

	/**
	 * Takes the picker away and shows the native input again; a wrapper the picker made is taken
	 * away too.
	 */
	destroy(): void {
		this.detach();
		const input = this.input;
		this.wrapper.querySelectorAll(':scope > .antilia-datepicker-field, ' +
			':scope > .antilia-datepicker-popup').forEach((element) => element.remove());
		input.classList.remove('antilia-datepicker-native');
		if (!input.classList.length) {
			input.removeAttribute('class');
		}
		input.removeAttribute('tabindex');
		input.removeAttribute('aria-hidden');
		this.wrapper.classList.remove('antilia-datepicker-time', 'antilia-datepicker-disabled',
			'antilia-datepicker-open', 'antilia-datepicker-up', 'antilia-datepicker-end',
			'antilia-datepicker-invalid');
		if (instances.get(this.wrapper) === this) {
			instances.delete(this.wrapper);
		}
		if (this.ownWrapper && this.wrapper.parentNode) {
			this.wrapper.parentNode.insertBefore(input, this.wrapper);
			this.wrapper.remove();
		}
	}

	private setLabel(element: HTMLElement, label: string): void {
		element.setAttribute('aria-label', label);
		element.title = label;
	}

	/**
	 * @returns the text of the labels of the input, if it has any
	 */
	labelText(): string {
		const texts: string[] = [];
		Array.from(this.input.labels ?? []).forEach((label) => {
			const copy = label.cloneNode(true) as HTMLElement;
			copy.querySelectorAll('input, .' + WRAPPER_CLASS).forEach((element) => element.remove());
			const text = (copy.textContent ?? '').replace(/\s+/g, ' ').trim();
			if (text) {
				texts.push(text);
			}
		});
		return texts.join(' ') || this.input.getAttribute('aria-label') || this.input.title || '';
	}

	/**
	 * @returns the value of the input, its parts, null when it is empty or no date
	 */
	value(): DateParts | null {
		return parseIso(this.input.value);
	}

	private min(): DateParts | null {
		return parseIso(this.options.min ?? this.input.min);
	}

	private max(): DateParts | null {
		return parseIso(this.options.max ?? this.input.max);
	}

	/**
	 * @param time whether to compare the time too
	 * @returns whether a date can be chosen: whether it is within min and max
	 */
	allows(parts: DateParts, time = false): boolean {
		const min = this.min();
		const max = this.max();
		const exact = time && this.isTime();
		return (!min || compare(parts, min, exact) >= 0) && (!max || compare(parts, max, exact) <= 0);
	}

	/**
	 * Shows the value of the input in the text field, and whether it is valid.
	 */
	update(): void {
		const input = this.input;
		const label = this.labelText();
		if (label) {
			this.field.setAttribute('aria-label', label);
			this.popup.setAttribute('aria-label', label);
		} else {
			this.field.removeAttribute('aria-label');
			this.popup.removeAttribute('aria-label');
		}
		const disabled = input.disabled;
		this.field.disabled = disabled;
		this.field.readOnly = input.readOnly;
		this.field.required = input.required;
		this.toggle.disabled = disabled || input.readOnly;
		this.wrapper.classList.toggle('antilia-datepicker-disabled', disabled);
		// text being typed is left alone
		if (this.typing) {
			this.markValidity(this.value());
		} else {
			this.showValue();
		}
	}

	/**
	 * Shows the value in the text field: a date as the locale writes it, other text as it is.
	 */
	private showValue(): void {
		this.typing = false;
		const parts = this.value();
		const text = parts ? this.format.write(parts) : this.input.value;
		if (this.field.value !== text) {
			this.field.value = text;
		}
		this.markValidity(parts);
	}

	private markValidity(parts: DateParts | null): void {
		const invalid = Boolean(this.input.value) && (!parts || !this.allows(parts, true));
		this.wrapper.classList.toggle('antilia-datepicker-invalid', invalid);
		if (invalid) {
			this.field.setAttribute('aria-invalid', 'true');
		} else {
			this.field.removeAttribute('aria-invalid');
		}
	}

	private listen<E extends Event>(target: EventTarget, type: string,
		listener: (event: E) => void): void {
		target.addEventListener(type, listener as EventListener);
		this.listeners.push([target, type, listener as EventListener]);
	}

	private bind(): void {
		this.listen(this.toggle, 'click', () => {
			if (this.isOpen()) {
				this.close(true);
			} else {
				this.open();
			}
		});
		// typed text becomes the value when the field is left, or on Enter
		this.listen(this.field, 'change', () => this.commitText());
		this.listen<KeyboardEvent>(this.field, 'keydown', (event) => {
			if (event.key === 'Enter') {
				this.commitText();
				this.close(false);
			} else if (event.key === 'ArrowDown' && event.altKey) {
				event.preventDefault();
				this.open();
			} else if (event.key === 'Escape' && this.isOpen()) {
				event.preventDefault();
				this.close(true);
			}
		});
		this.listen(this.field, 'input', () => {
			this.typing = true;
			const parts = this.format.read(this.field.value);
			if (parts && this.isOpen()) {
				this.focused = parts;
				this.render(false);
			}
		});
		this.listen(this.previous, 'click', () => this.page(-1));
		this.listen(this.next, 'click', () => this.page(1));
		this.listen(this.title, 'click', () => {
			this.view = this.view === 'days' ? 'months' : 'years';
			this.render(true);
		});
		this.listen(this.body, 'click', (event) => {
			const cell = (event.target as Element).closest<HTMLButtonElement>('button[data-value]');
			if (cell) {
				this.choose(cell);
			}
		});
		this.listen<KeyboardEvent>(this.body, 'keydown', (event) => this.onKey(event));
		this.listen<KeyboardEvent>(this.popup, 'keydown', (event) => {
			if (event.key === 'Escape') {
				event.preventDefault();
				event.stopPropagation();
				this.close(true);
			}
		});
		// a click on the label of the hidden input focuses the text field
		this.listen(this.input, 'focus', () => this.field.focus());
		// the input changed by someone else, for example a script resetting a form
		this.listen(this.input, 'change', () => this.update());
		const form = this.input.form;
		if (form) {
			this.listen(form, 'reset', () => setTimeout(() => this.update()));
		}
		const outside = (event: Event) => {
			if (this.isOpen() && !this.wrapper.contains(event.target as Node)) {
				this.close(false);
			}
		};
		this.listen(document, 'mousedown', outside);
		this.listen(document, 'focusin', outside);
	}

	/**
	 * Stops listening, leaving the elements in place.
	 */
	private detach(): void {
		this.listeners.forEach(([target, type, listener]) => {
			target.removeEventListener(type, listener);
		});
		this.listeners = [];
		this.stopWatchingRemoval?.();
		this.stopWatchingRemoval = null;
	}

	/**
	 * Takes the text of the field as the value: a date as ISO text, other text as it is.
	 */
	private commitText(): void {
		const text = this.field.value.trim();
		const parts = text ? this.format.read(text) : null;
		this.setValue(parts ? formatIso(parts, this.isTime()) : text);
	}

	/**
	 * Sets the value of the input and fires its input and change events, then shows the value it
	 * holds: a listener may have set it back, as React does with a controlled input whose value its
	 * parent did not change.
	 *
	 * @param value ISO text, or empty
	 */
	setValue(value: string): void {
		if (value !== this.input.value) {
			setNativeValue(this.input, value);
			this.input.dispatchEvent(new Event('input', { bubbles: true }));
			this.input.dispatchEvent(new Event('change', { bubbles: true }));
		}
		this.showValue();
	}

	isOpen(): boolean {
		return !this.popup.hidden;
	}

	open(): void {
		if (this.input.disabled || this.input.readOnly || this.isOpen()) {
			return;
		}
		this.focused = this.format.read(this.field.value) ?? this.value() ?? this.clamp(current());
		this.view = 'days';
		this.popup.hidden = false;
		this.toggle.setAttribute('aria-expanded', 'true');
		this.wrapper.classList.add('antilia-datepicker-open');
		this.render(true);
		this.renderTime();
		// open upwards when there is no room below but there is above, and to the left at the edge
		this.wrapper.classList.remove('antilia-datepicker-up', 'antilia-datepicker-end');
		const box = this.popup.getBoundingClientRect();
		const field = this.wrapper.getBoundingClientRect();
		if (box.bottom > window.innerHeight && field.top > box.height) {
			this.wrapper.classList.add('antilia-datepicker-up');
		}
		if (box.right > document.documentElement.clientWidth && field.right > box.width) {
			this.wrapper.classList.add('antilia-datepicker-end');
		}
	}

	/**
	 * @param focus whether to focus the text field again
	 */
	close(focus: boolean): void {
		if (!this.isOpen()) {
			return;
		}
		this.popup.hidden = true;
		this.toggle.setAttribute('aria-expanded', 'false');
		this.wrapper.classList.remove('antilia-datepicker-open', 'antilia-datepicker-up',
			'antilia-datepicker-end');
		if (focus) {
			this.field.focus();
		}
	}

	private clamp(parts: DateParts): DateParts {
		const min = this.min();
		const max = this.max();
		if (min && compare(parts, min, false) < 0) {
			return { ...parts, year: min.year, month: min.month, day: min.day };
		}
		if (max && compare(parts, max, false) > 0) {
			return { ...parts, year: max.year, month: max.month, day: max.day };
		}
		return parts;
	}

	/** Shows the month, year or years before or after. */
	private page(direction: number): void {
		this.focused = addMonths(this.focused, direction * this.pageMonths());
		this.render(false);
	}

	/** The months a page of the view spans. */
	private pageMonths(): number {
		return { days: 1, months: 12, years: 12 * YEARS_SHOWN }[this.view];
	}

	/**
	 * Draws the calendar as the view and the focused date have it.
	 *
	 * @param focus whether to focus the focused day, month or year
	 */
	render(focus: boolean): void {
		const options = this.options;
		const { year, month } = this.focused;
		const locale = this.format.locale;
		const labels = {
			days: [options.previousMonth || 'Previous month', options.nextMonth || 'Next month',
				options.chooseMonth || 'Choose month'],
			months: [options.previousYear || 'Previous year', options.nextYear || 'Next year',
				options.chooseYear || 'Choose year'],
			years: [options.previousYears || 'Previous years', options.nextYears || 'Next years', '']
		}[this.view];
		this.setLabel(this.previous, labels[0]);
		this.setLabel(this.next, labels[1]);
		this.title.disabled = this.view === 'years';
		if (labels[2]) {
			this.title.title = labels[2];
		} else {
			this.title.removeAttribute('title');
		}
		this.body.textContent = '';
		this.body.dataset.view = this.view;
		if (this.view === 'days') {
			// "mayo de 1990" becomes "Mayo de 1990": only the first letter is made upper case
			const title = new Intl.DateTimeFormat(locale, { calendar: 'gregory',
				numberingSystem: 'latn', timeZone: 'UTC', month: 'long', year: 'numeric' })
				.format(utc({ year, month, day: 1 }));
			this.title.textContent = title.charAt(0).toLocaleUpperCase(locale) + title.slice(1);
			this.body.appendChild(this.renderDays());
		} else if (this.view === 'months') {
			this.title.textContent = String(year);
			this.body.appendChild(this.renderMonths());
		} else {
			const first = year - mod(year - 1, YEARS_SHOWN);
			this.title.textContent = `${first} – ${first + YEARS_SHOWN - 1}`;
			this.body.appendChild(this.renderYears(first));
		}
		const target = this.body.querySelector<HTMLButtonElement>('button[tabindex="0"]');
		if (focus && target) {
			target.focus();
		}
	}

	private renderDays(): HTMLTableElement {
		const locale = this.format.locale;
		const { year, month } = this.focused;
		const firstDay = this.options.firstDayOfWeek ?? firstDayOfWeek(locale);
		const table = create('table', this.css('datepicker-days'));
		table.setAttribute('role', 'grid');
		table.setAttribute('aria-labelledby', this.title.id);
		const head = table.createTHead().insertRow();
		const short = new Intl.DateTimeFormat(locale, { weekday: 'short', timeZone: 'UTC' });
		const long = new Intl.DateTimeFormat(locale, { weekday: 'long', timeZone: 'UTC' });
		for (let i = 0; i < 7; i++) {
			// the 4th of January 1970 was a Sunday
			const day = Date.UTC(1970, 0, 4 + (firstDay + i) % 7);
			const cell = create('th', undefined, short.format(day));
			cell.scope = 'col';
			cell.abbr = long.format(day);
			head.appendChild(cell);
		}
		const full = new Intl.DateTimeFormat(locale, { calendar: 'gregory', numberingSystem: 'latn',
			timeZone: 'UTC', dateStyle: 'full' });
		const value = this.value();
		const today = current();
		const offset = mod(utc({ year, month, day: 1 }).getUTCDay() - firstDay, 7);
		let day = addDays({ ...this.focused, day: 1 }, -offset);
		const body = table.createTBody();
		for (let week = 0; week < 6; week++) {
			const row = body.insertRow();
			for (let i = 0; i < 7; i++) {
				const cell = row.insertCell();
				cell.setAttribute('role', 'gridcell');
				const item = button(this.css('datepicker-day'), String(day.day));
				item.dataset.value = formatIso(day, false);
				item.setAttribute('aria-label', full.format(utc(day)));
				item.tabIndex = compare(day, this.focused, false) === 0 ? 0 : -1;
				if (day.month !== month) {
					toggleParts(item, this.look, true, 'datepicker-day-outside');
				}
				if (compare(day, today, false) === 0) {
					item.setAttribute('aria-current', 'date');
				}
				const selected = Boolean(value) && compare(day, value!, false) === 0;
				cell.setAttribute('aria-selected', String(selected));
				item.setAttribute('aria-pressed', String(selected));
				if (!this.allows(day)) {
					item.setAttribute('aria-disabled', 'true');
				}
				cell.appendChild(item);
				day = addDays(day, 1);
			}
		}
		return table;
	}

	private renderMonths(): HTMLDivElement {
		const grid = create('div', this.css('datepicker-months'));
		const names = new Intl.DateTimeFormat(this.format.locale, { month: 'short', timeZone: 'UTC' });
		const longNames = new Intl.DateTimeFormat(this.format.locale, { month: 'long',
			year: 'numeric', timeZone: 'UTC' });
		const value = this.value();
		const { year } = this.focused;
		for (let month = 1; month <= 12; month++) {
			const item = button(this.css('datepicker-month'), names.format(utc({ year, month, day: 1 })));
			item.dataset.value = String(month);
			item.setAttribute('aria-label', longNames.format(utc({ year, month, day: 1 })));
			item.tabIndex = month === this.focused.month ? 0 : -1;
			item.setAttribute('aria-pressed', String(value?.year === year && value.month === month));
			if (!this.spans(year, month)) {
				item.setAttribute('aria-disabled', 'true');
			}
			grid.appendChild(item);
		}
		return grid;
	}

	/** Whether a month has days within min and max. */
	private spans(year: number, month: number): boolean {
		const first = { year, month, day: 1, hour: 0, minute: 0 };
		const last = { ...first, day: daysInMonth(year, month) };
		const min = this.min();
		const max = this.max();
		return (!min || compare(last, min, false) >= 0) && (!max || compare(first, max, false) <= 0);
	}

	private renderYears(first: number): HTMLDivElement {
		const grid = create('div', this.css('datepicker-years'));
		const value = this.value();
		const min = this.min();
		const max = this.max();
		for (let year = first; year < first + YEARS_SHOWN; year++) {
			const item = button(this.css('datepicker-year'), String(year));
			item.dataset.value = String(year);
			item.tabIndex = year === this.focused.year ? 0 : -1;
			item.setAttribute('aria-pressed', String(value?.year === year));
			if ((min && year < min.year) || (max && year > max.year)) {
				item.setAttribute('aria-disabled', 'true');
			}
			grid.appendChild(item);
		}
		return grid;
	}

	/**
	 * Draws the time fields of a date and time picker and the buttons below the calendar.
	 */
	private renderFooter(): void {
		this.popup.querySelectorAll(':scope > .antilia-datepicker-clock, ' +
			':scope > .antilia-datepicker-footer').forEach((element) => element.remove());
		this.hourField = null;
		this.minuteField = null;
		this.dayPeriod = null;
		const options = this.options;
		if (this.isTime()) {
			const clock = create('div', this.css('datepicker-clock'));
			const number = (part: LookPart, label: string, max: number, step: number) => {
				const field = create('input', this.css(part));
				field.type = 'number';
				field.inputMode = 'numeric';
				field.min = this.format.hour12 && part === 'datepicker-hour' ? '1' : '0';
				field.max = String(max);
				field.step = String(step);
				this.setLabel(field, label);
				this.listen(field, 'change', () => this.changeTime());
				this.listen<KeyboardEvent>(field, 'keydown', (event) => {
					if (event.key === 'Enter') {
						event.preventDefault();
						this.changeTime();
						this.close(true);
					}
				});
				return field;
			};
			this.hourField = number('datepicker-hour', options.hour || 'Hour',
				this.format.hour12 ? 12 : 23, 1);
			this.minuteField = number('datepicker-minute', options.minute || 'Minute', 59,
				Math.max(1, Math.min(59, options.minuteStep ?? 1)));
			clock.append(this.hourField, create('span', this.css('datepicker-colon'), ':'),
				this.minuteField);
			if (this.format.hour12) {
				this.dayPeriod = button(this.css('datepicker-period'));
				this.listen(this.dayPeriod, 'click', () => {
					this.dayPeriod!.dataset.pm = String(this.dayPeriod!.dataset.pm !== 'true');
					this.dayPeriod!.textContent = this.format.dayPeriods[
						this.dayPeriod!.dataset.pm === 'true' ? 1 : 0];
					this.changeTime();
				});
				clock.appendChild(this.dayPeriod);
			}
			this.popup.appendChild(clock);
		}
		const footer = create('div', this.css('datepicker-footer'));
		const today = button(this.css('datepicker-today'), this.isTime() ? options.now || 'Now'
			: options.today || 'Today');
		this.listen(today, 'click', () => {
			const now = this.clamp(current());
			const step = Math.max(1, options.minuteStep ?? 1);
			now.minute -= now.minute % step;
			this.setValue(formatIso(now, this.isTime()));
			this.close(true);
		});
		const clear = button(this.css('datepicker-clear'), options.clear || 'Clear');
		this.listen(clear, 'click', () => {
			this.setValue('');
			this.close(true);
		});
		footer.append(today, clear);
		if (this.isTime()) {
			const close = button(this.css('datepicker-close'), options.close || 'Close');
			this.listen(close, 'click', () => this.close(true));
			footer.appendChild(close);
		}
		this.popup.appendChild(footer);
	}

	/**
	 * Shows the time of the value in the time fields, or now, rounded down to the step, without one.
	 */
	private renderTime(): void {
		if (!this.hourField || !this.minuteField) {
			return;
		}
		const parts = this.value() ?? current();
		const step = Math.max(1, this.options.minuteStep ?? 1);
		const minute = this.value() ? parts.minute : parts.minute - parts.minute % step;
		this.hourField.value = pad(this.format.hour12 ? (parts.hour + 11) % 12 + 1 : parts.hour);
		this.minuteField.value = pad(minute);
		if (this.dayPeriod) {
			this.dayPeriod.dataset.pm = String(parts.hour >= 12);
			this.dayPeriod.textContent = this.format.dayPeriods[parts.hour >= 12 ? 1 : 0];
		}
	}

	/**
	 * @returns the hour and minute of the time fields, null when they hold no time
	 */
	private time(): Pick<DateParts, 'hour' | 'minute'> | null {
		const hour = Number(this.hourField?.value);
		const minute = Number(this.minuteField?.value);
		if (!this.hourField?.value || !this.minuteField?.value || !Number.isInteger(hour) ||
			!Number.isInteger(minute) || minute < 0 || minute > 59) {
			return null;
		}
		if (this.format.hour12) {
			if (hour < 1 || hour > 12) {
				return null;
			}
			return { hour: hour % 12 + (this.dayPeriod?.dataset.pm === 'true' ? 12 : 0), minute };
		}
		return hour >= 0 && hour <= 23 ? { hour, minute } : null;
	}

	/** Takes the time of the time fields into the value, on the day chosen or else today. */
	private changeTime(): void {
		const time = this.time();
		if (!time) {
			return;
		}
		const day = this.value() ?? this.clamp(current());
		this.setValue(formatIso({ ...day, ...time }, true));
		this.hourField!.value = pad(this.format.hour12 ? (time.hour + 11) % 12 + 1 : time.hour);
		this.minuteField!.value = pad(time.minute);
	}

	/**
	 * Chooses a day, month or year: a day becomes the value, a month or year shows its days or
	 * months.
	 */
	choose(item: HTMLButtonElement): void {
		if (item.getAttribute('aria-disabled') === 'true') {
			return;
		}
		const value = item.dataset.value!;
		if (this.view === 'years') {
			this.focused = this.clampDay({ ...this.focused, year: Number(value) });
			this.view = 'months';
			this.render(true);
			return;
		}
		if (this.view === 'months') {
			this.focused = this.clampDay({ ...this.focused, month: Number(value) });
			this.view = 'days';
			this.render(true);
			return;
		}
		const day = parseIso(value)!;
		if (this.isTime()) {
			const time = this.time() ?? { hour: 0, minute: 0 };
			this.focused = { ...day, ...time };
			this.setValue(formatIso(this.focused, true));
			this.render(false);
			this.hourField?.focus();
			return;
		}
		this.setValue(value);
		this.close(true);
	}

	private clampDay(parts: DateParts): DateParts {
		return { ...parts, day: Math.min(parts.day, daysInMonth(parts.year, parts.month)) };
	}

	private onKey(event: KeyboardEvent): void {
		const target = event.target as HTMLElement;
		if (!target.matches('button[data-value]')) {
			return;
		}
		const days = this.view === 'days';
		let moved: DateParts | null = null;
		const step = (count: number) => days ? addDays(this.focused, count)
			: this.view === 'months' ? addMonths(this.focused, count)
			: addMonths(this.focused, count * 12);
		switch (event.key) {
			case 'ArrowLeft':
				moved = step(-1);
				break;
			case 'ArrowRight':
				moved = step(1);
				break;
			case 'ArrowUp':
				moved = step(days ? -7 : -COLUMNS);
				break;
			case 'ArrowDown':
				moved = step(days ? 7 : COLUMNS);
				break;
			case 'Home':
			case 'End': {
				if (!days) {
					return;
				}
				const firstDay = this.options.firstDayOfWeek ?? firstDayOfWeek(this.format.locale);
				const weekday = mod(utc(this.focused).getUTCDay() - firstDay, 7);
				moved = addDays(this.focused, event.key === 'Home' ? -weekday : 6 - weekday);
				break;
			}
			case 'PageUp':
			case 'PageDown': {
				const direction = event.key === 'PageUp' ? -1 : 1;
				moved = addMonths(this.focused, direction * (event.shiftKey && days ? 12
					: this.pageMonths()));
				break;
			}
			case 'Enter':
			case ' ':
				event.preventDefault();
				this.choose(target as HTMLButtonElement);
				return;
			default:
				return;
		}
		event.preventDefault();
		this.focused = moved;
		this.render(true);
	}
}

function mod(value: number, divisor: number): number {
	return ((value % divisor) + divisor) % divisor;
}

/**
 * Draws the date picker of an input, in the shape of a Svelte action: use:datePicker={options}.
 *
 * @param input the input
 * @param options the texts, the locale and the dates taken
 * @returns update() taking other options, destroy() taking the picker away
 */
export function datePicker(input: HTMLInputElement, options?: DatePickerOptions): {
	update(options?: DatePickerOptions): void;
	destroy(): void;
} {
	const instance = new DatePicker(input, options);
	return {
		update: (next) => instance.configure(next),
		destroy: () => instance.destroy()
	};
}
