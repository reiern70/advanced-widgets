<!--
  Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<!--
  @component
  An autocomplete of the core: typing asks search, or url, for suggestions, rendered with the
  template, with the buttons of the actions at the end of every row; the chosen values show as
  chips with an x removing them. The values bind both ways
  (bind:value); onchange is called with them after every choice or removal. With pick it is a
  find bar: a choice is handed to onpick, not kept, and the text field is empty again.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { Autocomplete, type AutocompleteAction, type AutocompleteSuggestion,
		type AutocompleteValue, partClasses } from '@antilia/core';

	import { getLook } from './look.js';

	const look = getLook();

	type Props = {
		/** Finds the suggestions for a text: [{id, label, data}]. */
		search?: (term: string) => Promise<AutocompleteSuggestion[]>;
		/** The URL answering ?term= with the suggestions as JSON, in place of search. */
		url?: string;
		multiple?: boolean;
		/** Whether a choice is only handed to onpick, not kept as a chip. */
		pick?: boolean;
		/** Called with the value picked, when pick is set. */
		onpick?: (value: AutocompleteValue) => void;
		/** The chosen values. */
		value?: AutocompleteValue[];
		onchange?: (value: AutocompleteValue[]) => void;
		/** The template of a suggestion's row, rendered from its data; '{{@label}}' by default. */
		template?: string;
		engine?: string;
		/**
		 * The buttons at the end of every row, their content a template; one with a tooltip
		 * shows it while hovered, and F1 toggles it for the active row.
		 */
		actions?: AutocompleteAction[];
		/** Called when an action without a tooltip is clicked, with the suggestion of its row. */
		onaction?: (action: string, suggestion: AutocompleteSuggestion) => void;
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
		pick = false,
		onpick,
		value = $bindable([]),
		onchange,
		template,
		engine,
		actions,
		onaction,
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
		template, engine, actions: $state.snapshot(actions), minLength, disabled, placeholder,
		noMatch, searching, remove,
		onAction: (action: string, suggestion: AutocompleteSuggestion) => onaction?.(action,
			suggestion)
	});

	$effect(() => {
		const element = input;
		if (!element) {
			return;
		}
		const autocomplete = untrack(() => new Autocomplete(element, { ...options(), multiple,
			pick, chosen: $state.snapshot(value), look }));
		instance = autocomplete;
		const changed = () => {
			if (autocomplete.pick) {
				if (autocomplete.picked) {
					onpick?.(autocomplete.picked);
				}
				return;
			}
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

<div class={[partClasses(look, 'autocomplete'), className]}>
	{#key [multiple, pick].join()}
		<input bind:this={input} type="hidden" {name} {id} />
	{/key}
</div>
