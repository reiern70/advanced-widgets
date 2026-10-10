<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  The Palette demo in Svelte: the form of the Wicket page, in its markup, the sentence below it
  following every change in the browser.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { Palette } from '@antilia/svelte';

	import { format } from '../data.js';
	import { describe, describeCountries, type PaletteConfig } from '../palette.js';
	import Feedback from './Feedback.svelte';
	import type { Message } from './feedback.js';

	let { config }: { config: PaletteConfig } = $props();

	const texts = $derived(config.texts);
	// the choices start as the page has them, then are the app's
	const initial = untrack(() => config.values);
	let skills = $state(initial.skills);
	let columns = $state(initial.columns);
	let ordered = $state(initial.ordered);
	let numbered = $state(initial.numbered);
	let filterable = $state(initial.filterable);
	let countries = $state(initial.countries);
	let messages = $state<Message[]>([]);

	const choices = $derived(describe(config, { skills, columns, ordered, numbered, filterable,
		countries, disabled: config.values.disabled }));

	// without an order of their own, the columns take that of the choices again
	function order(event: Event & { currentTarget: HTMLInputElement }) {
		ordered = event.currentTarget.checked;
		if (!ordered) {
			columns = config.columns.map((column) => column.value)
				.filter((column) => columns.includes(column));
		}
	}

	function send(event: SubmitEvent) {
		event.preventDefault();
		messages = [{ level: 'info', text: format(texts.sent, choices) }];
	}
</script>

<div class="palette-demo">
	<form class="palette-demo__form" onsubmit={send}>
		<section>
			<h3>{texts.unordered}</h3>
			<label class="palette-demo__check">
				<input type="checkbox" bind:checked={filterable} />
				{texts.filterable}
			</label>
			<div class="palette-demo__field">
				<span>{texts.skills}</span>
				<Palette name="skills" options={config.skills} bind:value={skills} {filterable}
					{...config.palette} />
			</div>
		</section>
		<section>
			<h3>{texts.orderedTitle}</h3>
			<div class="palette-demo__checks">
				<label class="palette-demo__check">
					<input type="checkbox" checked={ordered} onchange={order} />
					{texts.ordered}
				</label>
				<label class="palette-demo__check">
					<input type="checkbox" bind:checked={numbered} />
					{texts.numbered}
				</label>
			</div>
			<div class="palette-demo__field">
				<span>{texts.columns}</span>
				<Palette name="columns" options={config.columns} bind:value={columns} {ordered}
					{numbered} {...config.palette} availableTitle={texts.columnsAvailable}
					selectedTitle={texts.columnsSelected} />
			</div>
		</section>
		<section>
			<h3>{texts.manyTitle}</h3>
			<div class="palette-demo__field">
				<span>{texts.countries}</span>
				<Palette name="countries" options={config.countries} bind:value={countries}
					{...config.palette} />
			</div>
			<p class="palette-demo__count">{describeCountries(config, countries)}</p>
		</section>
		<section>
			<h3>{texts.disabledTitle}</h3>
			<div class="palette-demo__field">
				<span>{texts.skills}</span>
				<Palette options={config.skills} value={config.values.disabled} disabled
					{...config.palette} />
			</div>
		</section>
		<p><button type="submit" class="palette-demo__send">{texts.send}</button></p>
	</form>
	<p class="palette-demo__choices">{choices}</p>
	<Feedback {messages} />
</div>
