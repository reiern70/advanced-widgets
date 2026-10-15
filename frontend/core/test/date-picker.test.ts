/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { DateFormat, DatePicker, datePicker, daysInMonth, firstDayOfWeek, formatIso,
	parseIso } from '../src/index.js';

function page(html: string): void {
	document.body.innerHTML = html;
}

function input(attributes = '', value = ''): HTMLInputElement {
	page(`<form id="f"><label for="d">Born</label><input id="d" name="d" value="${value}" ` +
		`${attributes}></form>`);
	return document.getElementById('d') as HTMLInputElement;
}

function key(target: Element, name: string, init: KeyboardEventInit = {}): void {
	target.dispatchEvent(new KeyboardEvent('keydown', { key: name, bubbles: true, ...init }));
}

function type(picker: DatePicker, text: string): void {
	picker.field.value = text;
	picker.field.dispatchEvent(new Event('input', { bubbles: true }));
	picker.field.dispatchEvent(new Event('change', { bubbles: true }));
}

function day(picker: DatePicker, iso: string): HTMLButtonElement {
	return picker.body.querySelector<HTMLButtonElement>(`button[data-value="${iso}"]`)!;
}

function focusedDay(picker: DatePicker): string | undefined {
	return picker.body.querySelector<HTMLButtonElement>('button[tabindex="0"]')?.dataset.value;
}

beforeEach(() => {
	vi.useFakeTimers({ toFake: ['Date'] });
	vi.setSystemTime(new Date(2026, 9, 9, 14, 37));
});

afterEach(() => {
	vi.useRealTimers();
	document.body.innerHTML = '';
});

describe('ISO text', () => {
	it('reads dates and times, and refuses what is no date', () => {
		expect(parseIso('2026-10-09')).toEqual({ year: 2026, month: 10, day: 9, hour: 0, minute: 0 });
		expect(parseIso('2026-10-09T14:30:15')).toEqual({ year: 2026, month: 10, day: 9, hour: 14,
			minute: 30 });
		expect(parseIso('2026-02-30')).toBeNull();
		expect(parseIso('2026-10-09T24:00')).toBeNull();
		expect(parseIso('tomorrow')).toBeNull();
		expect(parseIso(null)).toBeNull();
	});

	it('writes dates and times', () => {
		const parts = { year: 26, month: 1, day: 2, hour: 3, minute: 4 };
		expect(formatIso(parts, false)).toBe('0026-01-02');
		expect(formatIso(parts, true)).toBe('0026-01-02T03:04');
	});

	it('knows the days of a month', () => {
		expect(daysInMonth(2024, 2)).toBe(29);
		expect(daysInMonth(2026, 2)).toBe(28);
		expect(daysInMonth(2026, 12)).toBe(31);
	});
});

describe('DateFormat', () => {
	const october = { year: 2026, month: 10, day: 9, hour: 14, minute: 30 };

	it('writes dates the way the locale does', () => {
		expect(new DateFormat('de-DE', false).write(october)).toBe('09.10.2026');
		expect(new DateFormat('en-US', false).write(october)).toBe('10/09/2026');
		expect(new DateFormat('en-GB', false).write(october)).toBe('09/10/2026');
		expect(new DateFormat('ja-JP', false).write(october)).toBe('2026/10/09');
	});

	it('writes times with the clock of the locale', () => {
		expect(new DateFormat('de-DE', true).write(october)).toBe('09.10.2026, 14:30');
		const us = new DateFormat('en-US', true);
		expect(us.hour12).toBe(true);
		expect(us.write(october)).toMatch(/^10\/09\/2026, 02:30\sPM$/);
		expect(new DateFormat('en-US', true, false).write(october)).toBe('10/09/2026, 14:30');
	});

	it('shows its format with symbols', () => {
		expect(new DateFormat('de-DE', false).pattern('TT MM JJJJ hh mm')).toBe('TT.MM.JJJJ');
		expect(new DateFormat('en-US', false).pattern()).toBe('mm/dd/yyyy');
		expect(new DateFormat('en-US', true).pattern()).toMatch(/^mm\/dd\/yyyy, hh:mm\sAM\/PM$/);
	});

	it('reads dates the way the locale writes them', () => {
		const de = new DateFormat('de-DE', false);
		expect(de.read('9.10.2026')).toMatchObject({ year: 2026, month: 10, day: 9 });
		expect(de.read('09 10 26')).toMatchObject({ year: 2026, month: 10, day: 9 });
		expect(de.read('09102026')).toMatchObject({ year: 2026, month: 10, day: 9 });
		expect(de.read('091026')).toMatchObject({ year: 2026, month: 10, day: 9 });
		expect(de.read('2026-10-09')).toMatchObject({ year: 2026, month: 10, day: 9 });
		expect(de.read('9.10.60')).toMatchObject({ year: 1960 });
		expect(de.read('31.2.2026')).toBeNull();
		expect(de.read('9.10')).toBeNull();
		expect(new DateFormat('en-US', false).read('10/9/2026')).toMatchObject({ month: 10, day: 9 });
		expect(new DateFormat('ja-JP', false).read('2026/10/9')).toMatchObject({ month: 10, day: 9 });
	});

	it('reads times, with AM and PM on a 12 hour clock', () => {
		const de = new DateFormat('de-DE', true);
		expect(de.read('9.10.2026 14:30')).toEqual(october);
		expect(de.read('9.10.2026 14')).toEqual({ ...october, minute: 0 });
		expect(de.read('9.10.2026')).toEqual({ ...october, hour: 0, minute: 0 });
		const us = new DateFormat('en-US', true);
		expect(us.read('10/9/2026 2:30 pm')).toEqual(october);
		expect(us.read('10/9/2026 2:30 PM')).toEqual(october);
		expect(us.read('10/9/2026 12:30 AM')).toEqual({ ...october, hour: 0 });
		expect(us.read('10/9/2026 12:30 p')).toEqual({ ...october, hour: 12 });
		expect(us.read('10/9/2026 2:30')).toEqual({ ...october, hour: 2 });
		expect(us.read('10/9/2026 13:30 PM')).toBeNull();
		const es = new DateFormat('es-ES', true, true);
		expect(es.read('9/10/2026 2:30 p. m.')).toEqual(october);
	});

	it('takes the first day of the week from the region', () => {
		expect(firstDayOfWeek('de-DE')).toBe(1);
		expect(firstDayOfWeek('en-US')).toBe(0);
		expect(firstDayOfWeek('en')).toBe(0);
		expect(firstDayOfWeek('ar-EG')).toBe(6);
		expect(firstDayOfWeek('not a locale')).toBe(1);
	});
});

describe('DatePicker', () => {
	it('wraps the input and hides it, showing its value as the locale writes it', () => {
		const element = input('', '2026-10-09');
		const picker = new DatePicker(element, { locale: 'de-DE' });

		expect(element.parentElement?.className).toBe('antilia-datepicker');
		expect(element.classList.contains('antilia-datepicker-native')).toBe(true);
		expect(element.getAttribute('aria-hidden')).toBe('true');
		expect(element.tabIndex).toBe(-1);
		expect(picker.field.value).toBe('09.10.2026');
		expect(picker.field.placeholder).toBe('dd.mm.yyyy');
		expect(picker.field.getAttribute('aria-label')).toBe('Born');
		expect(picker.toggle.getAttribute('aria-label')).toBe('Choose date');
		expect(DatePicker.of('d')).toBe(picker);
		expect(DatePicker.of('nothing')).toBeNull();
		expect(DatePicker.attach('nothing')).toBeNull();
	});

	it('takes its texts and its locale as options, the locale else from the page', () => {
		page('<div lang="es"><input id="d"></div>');
		const picker = DatePicker.attach('d', { open: 'Elegir fecha', symbols: 'dd mm aaaa hh mm' })!;

		expect(picker.locale()).toBe('es');
		expect(picker.field.placeholder).toBe('dd/mm/aaaa');
		expect(picker.toggle.title).toBe('Elegir fecha');

		picker.configure({ locale: 'en-US', placeholder: 'When?' });
		expect(picker.field.placeholder).toBe('When?');
	});

	it('uses the wrapper the input is in already, and leaves it on destroy', () => {
		page('<div class="antilia-datepicker" id="w"><input id="d" value="2026-10-09"></div>');
		const element = document.getElementById('d') as HTMLInputElement;
		const picker = new DatePicker(element);

		picker.destroy();

		expect(document.getElementById('w')?.children.length).toBe(1);
		expect(element.hasAttribute('aria-hidden')).toBe(false);
		expect(element.hasAttribute('class')).toBe(false);
		expect(DatePicker.of(element)).toBeNull();
	});

	it('takes its own wrapper away on destroy', () => {
		const element = input();
		new DatePicker(element).destroy();

		expect(element.parentElement?.id).toBe('f');
		expect(document.querySelector('.antilia-datepicker')).toBeNull();
	});

	it('draws itself anew when attached again, as after an Ajax update', () => {
		const element = input();
		new DatePicker(element);
		const again = new DatePicker(element);

		expect(element.parentElement!.querySelectorAll('.antilia-datepicker-field').length).toBe(1);
		expect(DatePicker.of(element)).toBe(again);
	});

	it('upper-cases only the first letter of the month, the week of a language as in its region', () => {
		const picker = new DatePicker(input('', '1990-05-17'), { locale: 'es' });
		picker.toggle.click();

		expect(picker.title.textContent).toBe('Mayo de 1990');
		const heads = Array.from(picker.body.querySelectorAll('th'), (cell) => cell.textContent);
		expect(heads[0]).toBe('lun');
	});

	it('opens on the month of its value, the week starting on the first day of the locale', () => {
		const picker = new DatePicker(input('', '2026-10-09'), { locale: 'de-DE' });
		picker.toggle.click();

		expect(picker.isOpen()).toBe(true);
		expect(picker.toggle.getAttribute('aria-expanded')).toBe('true');
		expect(picker.title.textContent).toBe('Oktober 2026');
		const heads = Array.from(picker.body.querySelectorAll('th'), (cell) => cell.textContent);
		expect(heads).toEqual(['Mo', 'Di', 'Mi', 'Do', 'Fr', 'Sa', 'So']);
		// the 1st of October 2026 is a Thursday: three days of September come first
		expect(picker.body.querySelector('td button')?.getAttribute('data-value')).toBe('2026-09-28');
		expect(day(picker, '2026-10-09').getAttribute('aria-pressed')).toBe('true');
		expect(day(picker, '2026-10-09').getAttribute('aria-current')).toBe('date');
		expect(day(picker, '2026-10-09').getAttribute('aria-label')).toBe('Freitag, 9. Oktober 2026');
		expect(day(picker, '2026-09-30').classList.contains('antilia-datepicker-outside')).toBe(true);
		expect(document.activeElement).toBe(day(picker, '2026-10-09'));

		picker.toggle.click();
		expect(picker.isOpen()).toBe(false);
		expect(document.activeElement).toBe(picker.field);
	});

	it('starts the week where told to', () => {
		const picker = new DatePicker(input(), { locale: 'de-DE', firstDayOfWeek: 0 });
		picker.open();

		expect(picker.body.querySelector('th')?.textContent).toBe('So');
		expect(focusedDay(picker)).toBe('2026-10-09');
	});

	it('chooses a day, firing the input and change events, and closes', () => {
		const element = input();
		const events: string[] = [];
		element.addEventListener('input', () => events.push('input:' + element.value));
		element.addEventListener('change', () => events.push('change:' + element.value));
		const picker = new DatePicker(element, { locale: 'en-GB' });
		picker.open();

		day(picker, '2026-10-21').click();

		expect(element.value).toBe('2026-10-21');
		expect(events).toEqual(['input:2026-10-21', 'change:2026-10-21']);
		expect(picker.field.value).toBe('21/10/2026');
		expect(picker.isOpen()).toBe(false);
	});

	it('is driven by the keyboard', () => {
		const element = input('', '2026-10-09');
		const picker = new DatePicker(element, { locale: 'de-DE' });
		picker.open();
		const at = () => document.activeElement as HTMLElement;

		key(at(), 'ArrowRight');
		expect(focusedDay(picker)).toBe('2026-10-10');
		key(at(), 'ArrowDown');
		expect(focusedDay(picker)).toBe('2026-10-17');
		key(at(), 'ArrowUp');
		key(at(), 'ArrowLeft');
		expect(focusedDay(picker)).toBe('2026-10-09');
		key(at(), 'Home');
		expect(focusedDay(picker)).toBe('2026-10-05');
		key(at(), 'End');
		expect(focusedDay(picker)).toBe('2026-10-11');
		key(at(), 'PageDown');
		expect(focusedDay(picker)).toBe('2026-11-11');
		expect(picker.title.textContent).toBe('November 2026');
		key(at(), 'PageUp', { shiftKey: true });
		expect(focusedDay(picker)).toBe('2025-11-11');
		key(at(), 'x');
		expect(focusedDay(picker)).toBe('2025-11-11');
		key(at(), 'Enter');

		expect(element.value).toBe('2025-11-11');
		expect(picker.isOpen()).toBe(false);

		picker.open();
		key(at(), 'Escape');
		expect(picker.isOpen()).toBe(false);
		expect(document.activeElement).toBe(picker.field);
	});

	it('pages through the months with its buttons', () => {
		const picker = new DatePicker(input('', '2026-01-31'), { locale: 'en-US' });
		picker.open();

		picker.next.click();
		expect(picker.title.textContent).toBe('February 2026');
		expect(focusedDay(picker)).toBe('2026-02-28');
		picker.previous.click();
		picker.previous.click();
		expect(picker.title.textContent).toBe('December 2025');
		expect(picker.previous.getAttribute('aria-label')).toBe('Previous month');
	});

	it('chooses the year and the month from their views', () => {
		const element = input('', '2026-10-09');
		const picker = new DatePicker(element, { locale: 'en-US', chooseMonth: 'Pick a month' });
		picker.open();
		expect(picker.title.title).toBe('Pick a month');

		picker.title.click();
		expect(picker.view).toBe('months');
		expect(picker.title.textContent).toBe('2026');
		expect(picker.body.querySelectorAll('button').length).toBe(12);
		expect(picker.body.querySelector('[aria-pressed="true"]')?.textContent).toBe('Oct');

		picker.title.click();
		expect(picker.view).toBe('years');
		expect(picker.title.textContent).toBe('2017 – 2028');
		expect(picker.title.disabled).toBe(true);
		picker.next.click();
		expect(picker.title.textContent).toBe('2029 – 2040');
		picker.previous.click();
		picker.body.querySelector<HTMLButtonElement>('[tabindex="0"]')!.focus();
		key(document.activeElement!, 'ArrowUp');
		key(document.activeElement!, 'ArrowLeft');
		key(document.activeElement!, 'ArrowRight');
		key(document.activeElement!, 'ArrowDown');
		key(document.activeElement!, 'ArrowDown');
		expect(picker.title.textContent).toBe('2029 – 2040');
		key(document.activeElement!, 'Home');
		key(document.activeElement!, 'PageDown');
		expect(picker.title.textContent).toBe('2041 – 2052');
		key(document.activeElement!, 'PageUp');
		expect(picker.title.textContent).toBe('2029 – 2040');
		picker.body.querySelector<HTMLButtonElement>('[data-value="2029"]')!.click();

		expect(picker.view).toBe('months');
		expect(picker.title.textContent).toBe('2029');
		key(document.activeElement!, 'ArrowDown');
		key(document.activeElement!, ' ');
		expect(picker.view).toBe('days');
		expect(picker.title.textContent).toBe('January 2030');

		day(picker, '2030-01-09').click();
		expect(element.value).toBe('2030-01-09');
	});

	it('takes no day outside min and max', () => {
		const element = input('min="2026-10-05"');
		const picker = new DatePicker(element, { locale: 'en-US', max: '2026-11-15' });
		picker.open();

		expect(day(picker, '2026-10-04').getAttribute('aria-disabled')).toBe('true');
		expect(day(picker, '2026-10-05').hasAttribute('aria-disabled')).toBe(false);
		day(picker, '2026-10-04').click();
		expect(element.value).toBe('');
		expect(picker.isOpen()).toBe(true);

		picker.title.click();
		const months = Array.from(picker.body.querySelectorAll('button'),
			(item) => item.getAttribute('aria-disabled') === 'true');
		expect(months.indexOf(false)).toBe(9);
		expect(months.lastIndexOf(false)).toBe(10);
		picker.title.click();
		expect(picker.body.querySelector('[data-value="2027"]')?.getAttribute('aria-disabled'))
			.toBe('true');
	});

	it('opens on the nearest day it takes, when today is out of bounds', () => {
		const picker = new DatePicker(input(), { locale: 'en-US', min: '2027-03-15' });
		picker.open();

		expect(focusedDay(picker)).toBe('2027-03-15');
	});

	it('takes typed text as the value, keeping text that is no date for the server to refuse', () => {
		const element = input();
		const picker = new DatePicker(element, { locale: 'de-DE', max: '2026-12-31' });

		type(picker, '1.2.26');
		expect(element.value).toBe('2026-02-01');
		expect(picker.field.value).toBe('01.02.2026');
		expect(picker.field.hasAttribute('aria-invalid')).toBe(false);

		type(picker, 'gestern');
		expect(element.value).toBe('gestern');
		expect(picker.field.value).toBe('gestern');
		expect(picker.field.getAttribute('aria-invalid')).toBe('true');
		expect(picker.wrapper.classList.contains('antilia-datepicker-invalid')).toBe(true);

		type(picker, '1.2.2027');
		expect(element.value).toBe('2027-02-01');
		expect(picker.field.getAttribute('aria-invalid')).toBe('true');

		type(picker, '');
		expect(element.value).toBe('');
		expect(picker.field.hasAttribute('aria-invalid')).toBe(false);
	});

	it('follows the text being typed while open, and takes it on Enter', () => {
		const element = input();
		const picker = new DatePicker(element, { locale: 'de-DE' });
		picker.field.focus();
		key(picker.field, 'ArrowDown', { altKey: true });
		expect(picker.isOpen()).toBe(true);

		picker.field.value = '3.4.2027';
		picker.field.dispatchEvent(new Event('input'));
		expect(picker.title.textContent).toBe('April 2027');
		key(picker.field, 'Enter');

		expect(element.value).toBe('2027-04-03');
		expect(picker.isOpen()).toBe(false);

		picker.open();
		key(picker.field, 'Escape');
		expect(picker.isOpen()).toBe(false);
	});

	it('chooses today, and clears', () => {
		const element = input('', '2020-01-01');
		const picker = new DatePicker(element, { locale: 'en-US', today: 'Now!', clear: 'Empty' });
		picker.open();
		const today = picker.popup.querySelector<HTMLButtonElement>('.antilia-datepicker-today')!;
		const clear = picker.popup.querySelector<HTMLButtonElement>('.antilia-datepicker-clear')!;
		expect(today.textContent).toBe('Now!');
		expect(clear.textContent).toBe('Empty');

		today.click();
		expect(element.value).toBe('2026-10-09');
		picker.open();
		clear.click();
		expect(element.value).toBe('');
		expect(picker.field.value).toBe('');
	});

	it('leaves text being typed alone, until it is taken', () => {
		const element = input('', '2026-10-09');
		const picker = new DatePicker(element, { locale: 'de-DE' });
		picker.field.value = '1.2.';
		picker.field.dispatchEvent(new Event('input'));

		picker.update();
		expect(picker.field.value).toBe('1.2.');
		element.value = '2026-12-24';
		element.dispatchEvent(new Event('change'));
		expect(picker.field.value).toBe('1.2.');

		picker.field.value = '1.2.2027';
		picker.field.dispatchEvent(new Event('change'));
		expect(picker.field.value).toBe('01.02.2027');
		element.value = '2026-12-24';
		element.dispatchEvent(new Event('change'));
		expect(picker.field.value).toBe('24.12.2026');
	});

	it('shows a value set by someone else', () => {
		const element = input();
		const picker = new DatePicker(element, { locale: 'de-DE' });

		element.value = '2026-12-24';
		element.dispatchEvent(new Event('change'));
		expect(picker.field.value).toBe('24.12.2026');

		(document.getElementById('f') as HTMLFormElement).reset();
		vi.useRealTimers();
		return new Promise<void>((done) => setTimeout(() => {
			expect(picker.field.value).toBe('');
			done();
		}, 10));
	});

	it('follows the input being disabled or read only', () => {
		const element = input('disabled', '2026-10-09');
		const picker = new DatePicker(element);

		expect(picker.field.disabled).toBe(true);
		expect(picker.toggle.disabled).toBe(true);
		expect(picker.wrapper.classList.contains('antilia-datepicker-disabled')).toBe(true);
		picker.open();
		expect(picker.isOpen()).toBe(false);

		element.disabled = false;
		element.readOnly = true;
		picker.update();
		expect(picker.field.readOnly).toBe(true);
		expect(picker.toggle.disabled).toBe(true);
	});

	it('closes on a click or focus outside', () => {
		const picker = new DatePicker(input());
		picker.open();

		document.body.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
		expect(picker.isOpen()).toBe(false);
	});

	it('focuses the text field for the label of the hidden input', () => {
		const element = input();
		const picker = new DatePicker(element);

		element.dispatchEvent(new FocusEvent('focus'));
		expect(document.activeElement).toBe(picker.field);
	});

	it('is a Svelte action', () => {
		const element = input('', '2026-10-09');
		const action = datePicker(element, { locale: 'de-DE' });
		const picker = DatePicker.of(element)!;

		action.update({ locale: 'en-US' });
		expect(picker.field.value).toBe('10/09/2026');
		action.destroy();
		expect(DatePicker.of(element)).toBeNull();
	});
});

describe('DatePicker with a time', () => {
	it('chooses a day, then the time, on the clock of the locale', () => {
		const element = input('', '2026-10-09T08:05');
		const picker = new DatePicker(element, { locale: 'de-DE', time: true, hour: 'Stunde' });
		expect(picker.field.value).toBe('09.10.2026, 08:05');
		expect(picker.toggle.getAttribute('aria-label')).toBe('Choose date and time');
		picker.open();
		expect(picker.hourField?.value).toBe('08');
		expect(picker.hourField?.getAttribute('aria-label')).toBe('Stunde');
		expect(picker.dayPeriod).toBeNull();

		day(picker, '2026-10-12').click();
		expect(element.value).toBe('2026-10-12T08:05');
		expect(picker.isOpen()).toBe(true);
		expect(document.activeElement).toBe(picker.hourField);

		picker.hourField!.value = '17';
		picker.hourField!.dispatchEvent(new Event('change'));
		picker.minuteField!.value = '45';
		key(picker.minuteField!, 'Enter');
		expect(element.value).toBe('2026-10-12T17:45');
		expect(picker.field.value).toBe('12.10.2026, 17:45');
		expect(picker.isOpen()).toBe(false);
	});

	it('takes a time without a day as today, and refuses hours that are none', () => {
		const element = input();
		const picker = new DatePicker(element, { locale: 'de-DE', time: true, minuteStep: 15 });
		picker.open();
		expect(picker.hourField?.value).toBe('14');
		expect(picker.minuteField?.value).toBe('30');
		expect(picker.minuteField?.step).toBe('15');

		picker.hourField!.value = '25';
		picker.hourField!.dispatchEvent(new Event('change'));
		expect(element.value).toBe('');
		picker.hourField!.value = '9';
		picker.hourField!.dispatchEvent(new Event('change'));
		expect(element.value).toBe('2026-10-09T09:30');
		expect(picker.hourField?.value).toBe('09');
	});

	it('has AM and PM on a 12 hour clock', () => {
		const element = input('', '2026-10-09T08:05');
		const picker = new DatePicker(element, { locale: 'en-US', time: true });
		picker.open();
		expect(picker.hourField?.min).toBe('1');
		expect(picker.hourField?.max).toBe('12');
		expect(picker.dayPeriod?.textContent).toBe('AM');

		picker.dayPeriod!.click();
		expect(element.value).toBe('2026-10-09T20:05');
		expect(picker.dayPeriod?.textContent).toBe('PM');
		picker.hourField!.value = '12';
		picker.hourField!.dispatchEvent(new Event('change'));
		expect(element.value).toBe('2026-10-09T12:05');
		picker.hourField!.value = '13';
		picker.hourField!.dispatchEvent(new Event('change'));
		expect(element.value).toBe('2026-10-09T12:05');
	});

	it('chooses now, rounded down to the step, and closes with its button', () => {
		const element = input();
		const picker = new DatePicker(element, { locale: 'de-DE', time: true, minuteStep: 5,
			now: 'Jetzt', close: 'Fertig' });
		picker.open();
		const now = picker.popup.querySelector<HTMLButtonElement>('.antilia-datepicker-today')!;
		expect(now.textContent).toBe('Jetzt');

		now.click();
		expect(element.value).toBe('2026-10-09T14:35');
		picker.open();
		picker.popup.querySelector<HTMLButtonElement>('.antilia-datepicker-close')!.click();
		expect(picker.isOpen()).toBe(false);
	});

	it('marks a time before min as invalid, the day itself being taken', () => {
		const element = input();
		const picker = new DatePicker(element, { locale: 'de-DE', time: true,
			min: '2026-10-09T15:00' });
		picker.open();
		expect(day(picker, '2026-10-09').hasAttribute('aria-disabled')).toBe(false);

		type(picker, '9.10.2026 14:00');
		expect(picker.field.getAttribute('aria-invalid')).toBe('true');
		type(picker, '9.10.2026 15:00');
		expect(picker.field.hasAttribute('aria-invalid')).toBe(false);
	});
});
