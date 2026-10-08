/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { useLayoutEffect, useRef } from 'react';

import { Autocomplete as CoreAutocomplete, type AutocompleteSuggestion,
	type AutocompleteValue } from '@advanced-widgets/core';

export interface AutocompleteProps {
	/** Finds the suggestions for a text: [{id, label, data}]. */
	search?: (term: string) => Promise<AutocompleteSuggestion[]>;
	/** The URL answering ?term= with the suggestions as JSON, in place of search. */
	url?: string;
	/** Whether many values can be chosen. */
	multiple?: boolean;
	/** The chosen values; left out, the autocomplete keeps them itself. */
	value?: AutocompleteValue[];
	/** Called with the chosen values after every choice or removal. */
	onChange?(value: AutocompleteValue[]): void;
	/** The template of a suggestion's row, rendered from its data; '{{@label}}' by default. */
	template?: string;
	/** The name of the template engine, the built-in one by default. */
	engine?: string;
	/** The number of characters typed from which on suggestions are asked for. */
	minLength?: number;
	disabled?: boolean;
	placeholder?: string;
	noMatch?: string;
	searching?: string;
	/** The label of the x of a chip, '{0}' standing for its text. */
	remove?: string;
	/** The name and id of the hidden input holding the ids of the chosen values as JSON. */
	name?: string;
	id?: string;
	/** CSS classes of the div wrapping the autocomplete, next to wicket-autocomplete. */
	className?: string;
}

/**
 * An autocomplete of the core: typing asks search, or url, for suggestions, rendered with the
 * template; the chosen values show as chips with an x removing them. React renders the wrapper and
 * the hidden input, the core the rest.
 */
export function Autocomplete({ search, url, multiple = false, value, onChange, template, engine,
	minLength, disabled, placeholder, noMatch, searching, remove, name, id, className }:
	AutocompleteProps) {
	const input = useRef<HTMLInputElement>(null);
	const instance = useRef<CoreAutocomplete | null>(null);
	// the latest functions, so that new ones on every render leave an open list alone
	const latest = useRef({ search, onChange });
	latest.current = { search, onChange };

	const options = () => ({
		url,
		search: latest.current.search || !url ? (term: string) =>
			(latest.current.search ?? (() => Promise.resolve([])))(term) : undefined,
		template, engine, minLength, disabled, placeholder, noMatch, searching, remove
	});

	// the core reads multiple once, so it starts over when that changes
	useLayoutEffect(() => {
		const element = input.current!;
		const autocomplete = new CoreAutocomplete(element, { ...options(), multiple,
			chosen: value });
		instance.current = autocomplete;
		const changed = () => latest.current.onChange?.(autocomplete.chosen.slice());
		element.addEventListener('change', changed);
		return () => {
			element.removeEventListener('change', changed);
			autocomplete.destroy();
			instance.current = null;
		};
	}, [multiple]);

	useLayoutEffect(() => {
		instance.current?.configure(options());
	}, [url, Boolean(search), template, engine, minLength, disabled, placeholder, noMatch,
		searching, remove]);

	// compared by content, so that values made anew on every render are not drawn again
	const valueKey = value === undefined ? undefined : JSON.stringify(value.map((chosen) =>
		[chosen.id, chosen.label]));
	useLayoutEffect(() => {
		if (value !== undefined) {
			instance.current?.setChosen(value);
		}
	}, [valueKey]);

	return (
		<div className={['wicket-autocomplete', className].filter(Boolean).join(' ')}>
			<input ref={input} type="hidden" name={name} id={id} />
		</div>
	);
}
