<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  The date picker demo in Svelte: the form of the Wicket page, in its markup, the sentence below it
  following every change in the browser.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { DatePicker, DateTimePicker, Dropdown } from '@antilia/svelte';

	import { format } from '../data.js';
	import { calendarLocale, describe, type DatePickerConfig } from '../date-picker.js';
	import Feedback from './Feedback.svelte';
	import type { Message } from './feedback.js';

	let { config }: { config: DatePickerConfig } = $props();

	const texts = $derived(config.texts);
	const bounds = $derived(config.bounds);
	// the values start as the page has them, then are the app's
	const initial = untrack(() => config.values);
	let born = $state(initial.born);
	let start = $state(initial.start);
	let appointment = $state(initial.appointment);
	let departure = $state(initial.departure);
	let chosenLocale = $state(initial.calendarLocale);
	let messages = $state<Message[]>([]);

	const locale = $derived(calendarLocale(config, chosenLocale));
	const choices = $derived(describe(config, { born, start, appointment, departure,
		disabled: config.values.disabled, calendarLocale: chosenLocale }));

	function send(event: SubmitEvent) {
		event.preventDefault();
		messages = [{ level: 'info', text: format(texts.sent, choices) }];
	}
</script>

<div class="datepicker-demo">
	<form class="datepicker-demo__form" onsubmit={send}>
		<section>
			<h3>{texts.dates}</h3>
			<label class="datepicker-demo__field">
				{texts.born}
				<DatePicker name="born" bind:value={born} max={bounds.bornMax} {...config.dateTexts}
					{locale} />
			</label>
			<label class="datepicker-demo__field">
				{texts.start}
				<DatePicker name="start" bind:value={start} min={bounds.startMin} max={bounds.startMax}
					{...config.dateTexts} {locale} />
			</label>
		</section>
		<section>
			<h3>{texts.datesAndTimes}</h3>
			<label class="datepicker-demo__field">
				{texts.appointment}
				<DateTimePicker name="appointment" bind:value={appointment} min={bounds.appointmentMin}
					minuteStep={15} {...config.dateTimeTexts} {locale} />
			</label>
			<label class="datepicker-demo__field">
				{texts.departure}
				<DateTimePicker name="departure" bind:value={departure} {...config.dateTimeTexts}
					{locale} />
			</label>
		</section>
		<section>
			<h3>{texts.localeTitle}</h3>
			<label class="datepicker-demo__field">
				{texts.calendarLocale}
				<Dropdown name="calendarLocale" options={config.locales} bind:value={chosenLocale}
					filter={texts.filter} noMatch={texts.noMatch} more={texts.more}
					remove={texts.remove} />
			</label>
			<label class="datepicker-demo__field">
				{texts.disabledTitle}
				<DatePicker name="disabled" value={config.values.disabled} disabled
					{...config.dateTexts} {locale} />
			</label>
		</section>
		<p><button type="submit" class="datepicker-demo__send">{texts.send}</button></p>
	</form>
	<p class="datepicker-demo__choices">{choices}</p>
	<Feedback {messages} />
</div>
