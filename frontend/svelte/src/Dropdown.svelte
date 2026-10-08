<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
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
	import { Dropdown, type DropdownOptions } from '@advanced-widgets/core';
	import type { DropdownOption } from './types.js';

	type Props = Omit<HTMLSelectAttributes, 'value' | 'multiple' | 'children' | 'onchange'> &
		DropdownOptions & {
			/** The options, in their order. */
			options: DropdownOption[];
			/** The chosen value, or values of a multiple drop-down. */
			value?: string | string[];
			multiple?: boolean;
			/** CSS classes of the div wrapping the select, next to wicket-dropdown. */
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
		const dropdown = new Dropdown(select, untrack(texts));
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
		untrack(() => instance?.refresh());
	});

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

<div class={['wicket-dropdown', wrapperClass]}>
	{#if multiple}
		<select {...rest} multiple bind:value bind:this={select} onchange={changed}
			class={['wicket-dropdown-native', className]} tabindex="-1" aria-hidden="true">
			{#each options as option (option.value)}
				<option value={option.value} disabled={option.disabled}>{option.label}</option>
			{/each}
		</select>
	{:else}
		<select {...rest} bind:value bind:this={select} onchange={changed}
			class={['wicket-dropdown-native', className]} tabindex="-1" aria-hidden="true">
			{#each options as option (option.value)}
				<option value={option.value} disabled={option.disabled}>{option.label}</option>
			{/each}
		</select>
	{/if}
</div>
