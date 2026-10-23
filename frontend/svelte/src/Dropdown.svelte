<!--
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
  A select drawn as the drop-down of the core: Svelte renders the wrapper and the select, the core
  adds the button and the list next to the select. The value binds both ways (bind:value), an
  array for a multiple drop-down; onchange is called with it after every choice.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import type { HTMLSelectAttributes } from 'svelte/elements';
	import { Dropdown, type DropdownOptions, partClasses } from '@antilia/core';
	import type { DropdownOption } from './types.js';
	import { getLook } from './look.js';

	const look = getLook();

	type Props = Omit<HTMLSelectAttributes, 'value' | 'multiple' | 'children' | 'onchange'> &
		DropdownOptions & {
			/** The options, in their order. */
			options: DropdownOption[];
			/** The chosen value, or values of a multiple drop-down. */
			value?: string | string[];
			multiple?: boolean;
			/** CSS classes of the div wrapping the select, next to antilia-dropdown. */
			wrapperClass?: string;
			onchange?: (value: string | string[]) => void;
		};

	let {
		options,
		value = $bindable(),
		multiple = false,
		placeholder,
		filter,
		noMatch,
		more,
		remove,
		filterThreshold,
		wrapperClass = '',
		class: className = '',
		onchange,
		...rest
	}: Props = $props();

	let select = $state<HTMLSelectElement>();
	let instance: Dropdown | null = null;

	const texts = (): DropdownOptions => ({ placeholder, filter, noMatch, more, remove,
		filterThreshold });

	// a new select, when multiple changes, gets a new drop-down
	$effect(() => {
		if (!select) {
			return;
		}
		const dropdown = new Dropdown(select, { ...untrack(texts), look });
		instance = dropdown;
		return () => {
			dropdown.destroy();
			instance = null;
		};
	});

	$effect(() => {
		const next = texts();
		untrack(() => instance?.configure(next));
	});

	// compared by content, so that options made anew leave an open list alone
	const optionsKey = $derived(JSON.stringify(options.map((option) =>
		[option.value, option.label, Boolean(option.disabled)])));
	$effect(() => {
		void optionsKey;
		untrack(() => {
			selectValue();
			instance?.refresh();
		});
	});

	/**
	 * Selects the options of the value: options arriving after it, as when they are loaded, are
	 * not selected by bind:value.
	 */
	function selectValue() {
		if (!select || value === undefined) {
			return;
		}
		const chosen = Array.isArray(value) ? value : [value];
		for (const option of Array.from(select.options)) {
			option.selected = chosen.includes(option.value);
		}
	}

	$effect(() => {
		void value;
		void rest.disabled;
		untrack(() => instance?.update());
	});

	function changed(event: Event & { currentTarget: HTMLSelectElement }) {
		const element = event.currentTarget;
		onchange?.(multiple ? Array.from(element.selectedOptions, (option) => option.value)
			: element.value);
	}
</script>

<div class={[partClasses(look, 'dropdown'), wrapperClass]}>
	{#if multiple}
		<select {...rest} multiple bind:value bind:this={select} onchange={changed}
			class={['antilia-dropdown-native', className]} tabindex="-1" aria-hidden="true">
			{#each options as option (option.value)}
				<option value={option.value} disabled={option.disabled}>{option.label}</option>
			{/each}
		</select>
	{:else}
		<select {...rest} bind:value bind:this={select} onchange={changed}
			class={['antilia-dropdown-native', className]} tabindex="-1" aria-hidden="true">
			{#each options as option (option.value)}
				<option value={option.value} disabled={option.disabled}>{option.label}</option>
			{/each}
		</select>
	{/if}
</div>
