<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  @component
  An autocomplete of the core: typing asks search, or url, for suggestions, rendered with the
  template; the chosen values show as chips with an x removing them. The values bind both ways
  (bind:value); onchange is called with them after every choice or removal.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { Autocomplete, type AutocompleteSuggestion,
		type AutocompleteValue } from '@antilia/core';

	type Props = {
		/** Finds the suggestions for a text: [{id, label, data}]. */
		search?: (term: string) => Promise<AutocompleteSuggestion[]>;
		/** The URL answering ?term= with the suggestions as JSON, in place of search. */
		url?: string;
		multiple?: boolean;
		/** The chosen values. */
		value?: AutocompleteValue[];
		onchange?: (value: AutocompleteValue[]) => void;
		/** The template of a suggestion's row, rendered from its data; '{{@label}}' by default. */
		template?: string;
		engine?: string;
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
		class?: string;
	};

	let {
		search,
		url,
		multiple = false,
		value = $bindable([]),
		onchange,
		template,
		engine,
		minLength,
		disabled,
		placeholder,
		noMatch,
		searching,
		remove,
		name,
		id,
		class: className = ''
	}: Props = $props();

	let input = $state<HTMLInputElement>();
	let instance: Autocomplete | null = null;

	const options = () => ({
		url,
		search: search || !url ? (term: string) => (search ?? (() => Promise.resolve([])))(term) :
			undefined,
		template, engine, minLength, disabled, placeholder, noMatch, searching, remove
	});

	$effect(() => {
		const element = input;
		if (!element) {
			return;
		}
		const autocomplete = untrack(() => new Autocomplete(element, { ...options(), multiple,
			chosen: $state.snapshot(value) }));
		instance = autocomplete;
		const changed = () => {
			value = autocomplete.chosen.slice();
			onchange?.(autocomplete.chosen.slice());
		};
		element.addEventListener('change', changed);
		return () => {
			element.removeEventListener('change', changed);
			autocomplete.destroy();
			instance = null;
		};
	});

	$effect(() => {
		const next = options();
		untrack(() => instance?.configure(next));
	});

	// compared by content, so that the values the autocomplete reported are not drawn again
	const valueKey = $derived(JSON.stringify((value ?? []).map((chosen) =>
		[chosen.id, chosen.label])));
	$effect(() => {
		void valueKey;
		untrack(() => {
			const current = JSON.stringify((instance?.chosen ?? []).map((chosen) =>
				[chosen.id, chosen.label]));
			if (instance && current !== valueKey) {
				instance.setChosen($state.snapshot(value) ?? []);
			}
		});
	});
</script>

<div class={['antilia-autocomplete', className]}>
	{#key multiple}
		<input bind:this={input} type="hidden" {name} {id} />
	{/key}
</div>
