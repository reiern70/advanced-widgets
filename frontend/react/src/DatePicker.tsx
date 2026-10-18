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

import { useLayoutEffect, useRef, type ChangeEvent, type InputHTMLAttributes } from 'react';

import { DatePicker as CoreDatePicker, type DatePickerOptions, partClasses } from '@antilia/core';

import { useLook } from './Look.js';

type InputProps = Omit<InputHTMLAttributes<HTMLInputElement>, 'value' | 'defaultValue' |
	'onChange' | 'type' | 'min' | 'max' | 'placeholder' | 'children'>;

export interface DatePickerProps extends InputProps, Omit<DatePickerOptions, 'time'> {
	/** The value as ISO text, 2026-10-09; left out, the picker keeps its value itself. */
	value?: string;
	/** The value to start with, when the picker keeps its value itself. */
	defaultValue?: string;
	/**
	 * Called with the value as ISO text, empty when cleared, or with the text typed when it is no
	 * date.
	 */
	onChange?(value: string): void;
	/** CSS classes of the div wrapping the input, next to antilia-datepicker. */
	wrapperClassName?: string;
}

export type DateTimePickerProps = DatePickerProps;

/**
 * An input drawn as the date picker of the core: React renders the wrapper and the input, the
 * core adds the text field and the calendar next to the input and keeps out of the elements React
 * owns. A choice fires the input's change event, which calls onChange with the value.
 */
export function DatePicker(props: DatePickerProps) {
	return <Picker {...props} time={false} />;
}

/**
 * A {@link DatePicker} taking a time too: its value is ISO text such as 2026-10-09T14:30.
 */
export function DateTimePicker(props: DateTimePickerProps) {
	return <Picker {...props} time />;
}

function Picker(props: DatePickerProps & { time: boolean }) {
	const { value, defaultValue, onChange, wrapperClassName, className, time, locale, min, max,
		minuteStep, firstDayOfWeek, hour12, placeholder, symbols, open, previousMonth, nextMonth,
		previousYear, nextYear, previousYears, nextYears, chooseMonth, chooseYear, today, now, clear,
		close, hour, minute, ...rest } = props;
	const input = useRef<HTMLInputElement>(null);
	const instance = useRef<CoreDatePicker | null>(null);
	const look = useLook();
	const options: DatePickerOptions = { time, locale, min, max, minuteStep, firstDayOfWeek,
		hour12, placeholder, symbols, open, previousMonth, nextMonth, previousYear, nextYear,
		previousYears, nextYears, chooseMonth, chooseYear, today, now, clear, close, hour, minute };
	const optionsKey = JSON.stringify(options);
	const latest = useRef(options);
	latest.current = options;

	useLayoutEffect(() => {
		const picker = new CoreDatePicker(input.current!, { ...latest.current, look });
		instance.current = picker;
		return () => {
			picker.destroy();
			instance.current = null;
		};
	}, [look]);

	useLayoutEffect(() => {
		instance.current?.configure(latest.current);
	}, [optionsKey]);

	useLayoutEffect(() => {
		instance.current?.update();
	}, [value, rest.disabled, rest.readOnly, rest.required]);

	const change = (event: ChangeEvent<HTMLInputElement>) => onChange?.(event.currentTarget.value);
	const controlled = value === undefined ? { defaultValue } : { value };
	return (
		<div className={[partClasses(look, 'datepicker'), wrapperClassName].filter(Boolean)
			.join(' ')}>
			<input {...rest} {...controlled} ref={input} type="text" onChange={change}
				className={['antilia-datepicker-native', className].filter(Boolean).join(' ')}
				tabIndex={-1} aria-hidden="true" />
		</div>
	);
}
