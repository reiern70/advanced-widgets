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
  The palette of the core: the available choices on the left, the chosen ones on the right and the
  arrows between them. Svelte renders the element, the core draws the lists into it. The value,
  the values of the chosen ones in their order, binds both ways (bind:value); onchange is called
  with it after every change the user made.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { Palette, type PaletteChoice, type PaletteMatch, type PaletteTexts, partClasses } from '@antilia/core';

	import { getLook } from './look.js';

	const look = getLook();

	type Props = PaletteTexts & {
		/** The choices, in their order. */
		options: PaletteChoice[];
		/** The values of the chosen ones, in their order. */
		value?: string[];
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
		 * How the filter matches a choice: 'start', 'anywhere' or 'end' of the label, 'words' (each
		 * word typed starts a word of the label), the name of a matcher in Palette.matchers, or a matcher; 'anywhere' when left out. The matched parts of
		 * the labels are underlined.
		 */
		match?: PaletteMatch;
		/**
		 * How many rows a list draws at first, and adds each time it is scrolled to its end, 30 when
		 * left out; 0 draws them all.
		 */
		pageSize?: number;
		/** The name a form posts the chosen ones under, one hidden field each, in their order. */
		name?: string;
		id?: string;
		/** CSS classes of the palette, next to antilia-palette. */
		class?: string;
		onchange?: (value: string[]) => void;
	};

	let {
		options,
		value = $bindable([]),
		ordered = false,
		numbered = false,
		disabled = false,
		filterable = true,
		filterThreshold,
		match,
		pageSize,
		name,
		id,
		class: className = '',
		availableTitle,
		selectedTitle,
		add,
		addAll,
		remove,
		removeAll,
		up,
		down,
		filter,
		empty,
		onchange
	}: Props = $props();

	let element = $state<HTMLDivElement>();
	let instance: Palette | null = null;

	const settings = () => ({ ordered, numbered, filterable, filterThreshold, match, pageSize,
		availableTitle, selectedTitle, add, addAll, remove, removeAll, up, down, filter, empty });

	$effect(() => {
		if (!element) {
			return;
		}
		const palette = new Palette(element, untrack(() => ({
			...settings(),
			look,
			choices: options,
			selected: value,
			disabled,
			onChange: (selected: string[]) => {
				value = selected;
				onchange?.(selected);
			}
		})));
		instance = palette;
		return () => {
			palette.destroy();
			instance = null;
		};
	});

	$effect(() => {
		const next = settings();
		untrack(() => instance?.configure(next));
	});

	// compared by content, so that options made anew leave the lists alone
	const optionsKey = $derived(JSON.stringify(options.map((option) =>
		[option.value, option.label, Boolean(option.disabled)])));
	$effect(() => {
		void optionsKey;
		untrack(() => {
			instance?.refresh(options);
			instance?.update(value);
		});
	});

	$effect(() => {
		const next = value;
		untrack(() => instance?.update(next));
	});

	$effect(() => {
		const next = disabled;
		untrack(() => instance?.setDisabled(next));
	});
</script>

<div bind:this={element} {id} class={[partClasses(look, 'palette'), className]}></div>
{#if name}
	{#each value as each (each)}
		<input type="hidden" {name} value={each} />
	{/each}
{/if}
