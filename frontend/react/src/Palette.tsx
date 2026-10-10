/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { useLayoutEffect, useRef, useState } from 'react';

import { Palette as CorePalette, type PaletteChoice, type PaletteTexts } from '@antilia/core';

export interface PaletteProps extends PaletteTexts {
	/** The choices, in their order. */
	options: PaletteChoice[];
	/** The values of the chosen ones, in their order; left out, the palette keeps them itself. */
	value?: string[];
	/** The chosen ones at first, while the palette keeps them itself. */
	defaultValue?: string[];
	/** Called with the values of the chosen ones, in their order, after the user changed them. */
	onChange?(value: string[]): void;
	/** Whether the order of the chosen ones matters: they can be moved up and down. */
	ordered?: boolean;
	/** Whether the rows of both lists show their number, from 1, before their text. */
	numbered?: boolean;
	disabled?: boolean;
	/** Whether the available ones can have a filter, true when left out. */
	filterable?: boolean;
	/** The number of choices from which on the available ones have a filter, 8 when left out. */
	filterThreshold?: number;
	/**
	 * How many rows a list draws at first, and adds each time it is scrolled to its end, 30 when
	 * left out; 0 draws them all.
	 */
	pageSize?: number;
	/** The name a form posts the chosen ones under, one hidden field each, in their order. */
	name?: string;
	id?: string;
	/** CSS classes of the palette, next to antilia-palette. */
	className?: string;
}

/**
 * The palette of the core: the available choices on the left, the chosen ones on the right and
 * the arrows between them. React renders the element, the core draws the lists into it.
 */
export function Palette(props: PaletteProps) {
	const { options, value, defaultValue, ordered, numbered, disabled, filterable,
		filterThreshold, pageSize, name, id, className, availableTitle, selectedTitle, add, addAll, remove, removeAll, up, down,
		filter, empty } = props;
	const element = useRef<HTMLDivElement>(null);
	const instance = useRef<CorePalette | null>(null);
	const latest = useRef(props);
	latest.current = props;
	const [own, setOwn] = useState<string[]>(defaultValue ?? []);
	// a change the parent of a controlled palette did not take renders again, which sets it back
	const [changes, setChanges] = useState(0);
	const current = value ?? own;
	const settings = { ordered, numbered, filterable, filterThreshold, pageSize, availableTitle,
		selectedTitle, add, addAll, remove, removeAll, up, down, filter, empty };

	useLayoutEffect(() => {
		const palette = new CorePalette(element.current!, {
			...settings,
			choices: options,
			selected: current,
			disabled,
			onChange: (selected) => {
				if (latest.current.value === undefined) {
					setOwn(selected);
				}
				setChanges((count) => count + 1);
				latest.current.onChange?.(selected);
			}
		});
		instance.current = palette;
		return () => {
			palette.destroy();
			instance.current = null;
		};
	}, []);

	useLayoutEffect(() => {
		instance.current?.configure(settings);
	}, [ordered, numbered, filterable, filterThreshold, pageSize, availableTitle, selectedTitle,
		add, addAll, remove, removeAll, up, down, filter, empty]);

	// compared by content, so that options made anew on every render leave the lists alone
	const optionsKey = JSON.stringify(options.map((option) =>
		[option.value, option.label, Boolean(option.disabled)]));
	useLayoutEffect(() => {
		instance.current?.refresh(options);
		instance.current?.update(current);
	}, [optionsKey]);

	useLayoutEffect(() => {
		instance.current?.update(current);
	}, [JSON.stringify(current), changes]);

	useLayoutEffect(() => {
		instance.current?.setDisabled(Boolean(disabled));
	}, [disabled]);

	return (
		<>
			<div ref={element} id={id}
				className={['antilia-palette', className].filter(Boolean).join(' ')} />
			{name && current.map((each) => (
				<input key={each} type="hidden" name={name} value={each} />
			))}
		</>
	);
}
