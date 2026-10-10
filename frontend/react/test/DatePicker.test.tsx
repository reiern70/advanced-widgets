/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { StrictMode, useState } from 'react';
import { act, cleanup, fireEvent, render } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { DatePicker, DateTimePicker } from '../src/index.js';

function field(container: HTMLElement): HTMLInputElement {
	return container.querySelector('.antilia-datepicker-text')!;
}

function toggle(container: HTMLElement): HTMLButtonElement {
	return container.querySelector('.antilia-datepicker-toggle')!;
}

function day(container: HTMLElement, iso: string): HTMLButtonElement {
	return container.querySelector(`button[data-value="${iso}"]`)!;
}

afterEach(cleanup);

describe('DatePicker', () => {
	it('draws the core date picker inside the wrapper React renders', () => {
		const { container } = render(<DatePicker name="born" value="2026-10-09" locale="de-DE"
			onChange={() => {}} wrapperClassName="mine" className="native" />);

		const wrapper = container.firstElementChild!;
		expect(wrapper.className).toBe('antilia-datepicker mine');
		expect(wrapper.firstElementChild?.tagName).toBe('INPUT');
		expect(wrapper.firstElementChild?.className).toBe('antilia-datepicker-native native');
		expect(field(container).value).toBe('09.10.2026');
	});

	it('reports a choice to onChange and shows the value it gets back', () => {
		function Controlled() {
			const [date, setDate] = useState('2026-10-09');
			return <><DatePicker value={date} onChange={setDate} locale="en-US" /><p>{date}</p></>;
		}
		const { container } = render(<Controlled />);

		act(() => toggle(container).click());
		act(() => day(container, '2026-10-20').click());

		expect(container.querySelector('p')?.textContent).toBe('2026-10-20');
		expect(field(container).value).toBe('10/20/2026');
	});

	it('keeps the value its parent gives, whatever is chosen', () => {
		const onChange = vi.fn();
		const { container } = render(<DatePicker value="2026-10-09" onChange={onChange}
			locale="en-US" />);

		act(() => toggle(container).click());
		act(() => day(container, '2026-10-20').click());

		expect(onChange).toHaveBeenCalledWith('2026-10-20');
		expect(container.querySelector('input')?.value).toBe('2026-10-09');
		expect(field(container).value).toBe('10/09/2026');
	});

	it('keeps its value itself without one', () => {
		const onChange = vi.fn();
		const { container } = render(<DatePicker defaultValue="2026-10-09" onChange={onChange}
			locale="de-DE" />);

		fireEvent.change(field(container), { target: { value: '1.2.2027' } });

		expect(onChange).toHaveBeenCalledWith('2027-02-01');
		expect(container.querySelector('input')?.value).toBe('2027-02-01');
		expect(field(container).value).toBe('01.02.2027');
	});

	it('follows a new value, locale and texts', () => {
		const { container, rerender } = render(<DatePicker value="2026-10-09" locale="de-DE"
			open="Datum wählen" />);
		expect(toggle(container).title).toBe('Datum wählen');

		rerender(<DatePicker value="2026-12-24" locale="en-GB" open="Choose" disabled />);

		expect(field(container).value).toBe('24/12/2026');
		expect(toggle(container).title).toBe('Choose');
		expect(field(container).disabled).toBe(true);
	});

	it('survives the double effects of StrictMode, and leaves the wrapper clean', () => {
		const { container, unmount } = render(<StrictMode><DatePicker value="2026-10-09"
			onChange={() => {}} /></StrictMode>);

		expect(container.querySelectorAll('.antilia-datepicker-field').length).toBe(1);
		unmount();
		expect(container.innerHTML).toBe('');
	});
});

describe('DateTimePicker', () => {
	it('takes a time too', () => {
		const onChange = vi.fn();
		const { container } = render(<DateTimePicker value="2026-10-09T08:05" onChange={onChange}
			locale="de-DE" minuteStep={15} />);

		expect(field(container).value).toBe('09.10.2026, 08:05');
		act(() => toggle(container).click());
		const hour = container.querySelector<HTMLInputElement>('.antilia-datepicker-hour')!;
		hour.value = '17';
		act(() => {
			hour.dispatchEvent(new Event('change'));
		});

		expect(onChange).toHaveBeenCalledWith('2026-10-09T17:05');
		expect(container.querySelector<HTMLInputElement>('.antilia-datepicker-minute')?.step)
			.toBe('15');
	});
});
