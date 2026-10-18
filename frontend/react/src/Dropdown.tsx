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

import { useLayoutEffect, useRef, type ChangeEvent, type SelectHTMLAttributes } from 'react';

import { Dropdown as CoreDropdown, type DropdownOptions, partClasses } from '@antilia/core';

import { useLook } from './Look.js';

/**
 * An option of a {@link Dropdown}.
 */
export interface DropdownOption {
	value: string;
	label: string;
	disabled?: boolean;
}

type SelectProps = Omit<SelectHTMLAttributes<HTMLSelectElement>,
	'value' | 'defaultValue' | 'onChange' | 'multiple' | 'children'>;

interface CommonProps extends SelectProps, DropdownOptions {
	/** The options, in their order. */
	options: DropdownOption[];
	/** CSS classes of the div wrapping the select, next to antilia-dropdown. */
	wrapperClassName?: string;
}

interface SingleProps extends CommonProps {
	multiple?: false;
	/** The chosen value; left out, the drop-down keeps its choice itself. */
	value?: string;
	onChange?(value: string): void;
}

interface MultipleProps extends CommonProps {
	multiple: true;
	/** The chosen values; left out, the drop-down keeps its choice itself. */
	value?: string[];
	onChange?(value: string[]): void;
}

export type DropdownProps = SingleProps | MultipleProps;

/**
 * A select drawn as the drop-down of the core: React renders the wrapper and the select, the core
 * adds the button and the list next to the select and keeps out of the elements React owns. A
 * choice fires the select's change event, which calls onChange with the value, or the values of a
 * multiple drop-down.
 */
export function Dropdown(props: DropdownProps) {
	const { options, value, onChange, multiple, placeholder, filter, noMatch, more, remove,
		filterThreshold, wrapperClassName, className, ...rest } = props;
	const select = useRef<HTMLSelectElement>(null);
	const instance = useRef<CoreDropdown | null>(null);
	const look = useLook();
	const texts: DropdownOptions = { placeholder, filter, noMatch, more, remove, filterThreshold };

	// the core reads whether the select is multiple once, so it starts over when that changes
	useLayoutEffect(() => {
		const dropdown = new CoreDropdown(select.current!, { ...texts, look });
		instance.current = dropdown;
		return () => {
			dropdown.destroy();
			instance.current = null;
		};
	}, [multiple, look]);

	useLayoutEffect(() => {
		instance.current?.configure(texts);
	}, [placeholder, filter, noMatch, more, remove, filterThreshold]);

	// compared by content, so that options made anew on every render leave an open list alone
	const optionsKey = JSON.stringify(options.map((option) =>
		[option.value, option.label, Boolean(option.disabled)]));
	useLayoutEffect(() => {
		instance.current?.refresh();
	}, [optionsKey]);

	useLayoutEffect(() => {
		instance.current?.update();
	}, [value, rest.disabled]);

	const change = (event: ChangeEvent<HTMLSelectElement>) => {
		const element = event.currentTarget;
		if (multiple) {
			(onChange as MultipleProps['onChange'])?.(
				Array.from(element.selectedOptions, (option) => option.value));
		} else {
			(onChange as SingleProps['onChange'])?.(element.value);
		}
	};

	const controlled = value === undefined ? {} : { value };
	return (
		<div className={[partClasses(look, 'dropdown'), wrapperClassName].filter(Boolean).join(' ')}>
			<select {...rest} {...controlled} ref={select} multiple={multiple} onChange={change}
				className={['antilia-dropdown-native', className].filter(Boolean).join(' ')}
				tabIndex={-1} aria-hidden="true">
				{options.map((option) => (
					<option key={option.value} value={option.value} disabled={option.disabled}>
						{option.label}
					</option>
				))}
			</select>
		</div>
	);
}
