<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  @component
  An input drawn as the date picker of the core: Svelte renders the wrapper and the input, the core
  adds the text field and the calendar next to the input. The value, ISO text such as 2026-10-09,
  binds both ways (bind:value); onchange is called with it after every change, with the text typed
  when it is no date. With time it takes a time too: 2026-10-09T14:30.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import type { HTMLInputAttributes } from 'svelte/elements';
	import { DatePicker, type DatePickerOptions, partClasses } from '@antilia/core';

	import { getLook } from './look.js';

	const look = getLook();

	type Props = Omit<HTMLInputAttributes, 'value' | 'type' | 'min' | 'max' | 'placeholder' |
		'children' | 'onchange'> & DatePickerOptions & {
			/** The value as ISO text, empty without one. */
			value?: string;
			/** CSS classes of the div wrapping the input, next to antilia-datepicker. */
			wrapperClass?: string;
			onchange?: (value: string) => void;
		};

	let {
		value = $bindable(''),
		time = false,
		locale,
		min,
		max,
		minuteStep,
		firstDayOfWeek,
		hour12,
		placeholder,
		symbols,
		open,
		previousMonth,
		nextMonth,
		previousYear,
		nextYear,
		previousYears,
		nextYears,
		chooseMonth,
		chooseYear,
		today,
		now,
		clear,
		close,
		hour,
		minute,
		wrapperClass = '',
		class: className = '',
		onchange,
		...rest
	}: Props = $props();

	let input = $state<HTMLInputElement>();
	let instance: DatePicker | null = null;

	const options = (): DatePickerOptions => ({ time, locale, min, max, minuteStep, firstDayOfWeek,
		hour12, placeholder, symbols, open, previousMonth, nextMonth, previousYear, nextYear,
		previousYears, nextYears, chooseMonth, chooseYear, today, now, clear, close, hour, minute });

	$effect(() => {
		if (!input) {
			return;
		}
		const picker = new DatePicker(input, { ...untrack(options), look });
		instance = picker;
		return () => {
			picker.destroy();
			instance = null;
		};
	});

	// compared by content, so that options made anew leave an open calendar alone
	const optionsKey = $derived(JSON.stringify(options()));
	$effect(() => {
		void optionsKey;
		untrack(() => instance?.configure(options()));
	});

	$effect(() => {
		void value;
		void rest.disabled;
		void rest.readonly;
		void rest.required;
		untrack(() => instance?.update());
	});

	function changed(event: Event & { currentTarget: HTMLInputElement }) {
		onchange?.(event.currentTarget.value);
	}
</script>

<div class={[partClasses(look, 'datepicker'), wrapperClass]}>
	<input {...rest} type="text" bind:value bind:this={input} onchange={changed}
		class={['antilia-datepicker-native', className]} tabindex="-1" aria-hidden="true" />
</div>
