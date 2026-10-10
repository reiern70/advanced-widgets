<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
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
	import { Palette, type PaletteChoice, type PaletteTexts } from '@antilia/core';

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

	const settings = () => ({ ordered, numbered, filterable, filterThreshold, availableTitle,
		selectedTitle, add, addAll, remove, removeAll, up, down, filter, empty });

	$effect(() => {
		if (!element) {
			return;
		}
		const palette = new Palette(element, untrack(() => ({
			...settings(),
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

<div bind:this={element} {id} class={['antilia-palette', className]}></div>
{#if name}
	{#each value as each (each)}
		<input type="hidden" {name} value={each} />
	{/each}
{/if}
