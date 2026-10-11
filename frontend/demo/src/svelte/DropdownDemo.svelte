<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  The Dropdown demo in Svelte: the form of the Wicket page, in its markup, the sentence below it
  following every choice in the browser. Choices not sent are changes a window of a desktop asks
  about before it closes, and drops when told to.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { DESKTOP_DISCARD_EVENT } from '@antilia/core';
	import { Dropdown } from '@antilia/svelte';

	import { format, loadChoices, type Choice } from '../data.js';
	import { describe, type DropdownConfig } from '../dropdown.js';
	import Feedback from './Feedback.svelte';
	import type { Message } from './feedback.js';

	let { config }: { config: DropdownConfig } = $props();

	const texts = $derived(config.texts);
	let countries = $state<Choice[]>([]);
	// the choices start as the page has them, then are the app's
	const initial = untrack(() => config.values);
	let size = $state(initial.size);
	let country = $state(initial.country);
	let skills = $state(initial.skills);
	let languages = $state(initial.languages);
	let messages = $state<Message[]>([]);
	// the choices sent last, the page's at first
	let sent = $state({ size: initial.size, country: initial.country, skills: initial.skills,
		languages: initial.languages });
	let root = $state<HTMLDivElement>();
	const modified = $derived(JSON.stringify({ size, country, skills, languages }) !==
		JSON.stringify(sent));

	// the window of a desktop the demo is in drops the choices not sent when it closes
	$effect(() => {
		const window = root?.closest('.antilia-window');
		const discard = () => {
			size = sent.size;
			country = sent.country;
			skills = [...sent.skills];
			languages = [...sent.languages];
		};
		window?.addEventListener(DESKTOP_DISCARD_EVENT, discard);
		return () => window?.removeEventListener(DESKTOP_DISCARD_EVENT, discard);
	});

	const sizes = $derived(config.sizes.map((value) => ({ value, label: value })));
	const choices = $derived(describe(config, countries, { size, country, skills, languages,
		disabled: config.values.disabled }));

	$effect(() => {
		loadChoices(config.choicesUrl).then((loaded) => (countries = loaded.countries),
			(error) => console.error(error));
	});

	function send(event: SubmitEvent) {
		event.preventDefault();
		sent = { size, country, skills: [...skills], languages: [...languages] };
		messages = [{ level: 'info', text: format(texts.sent, choices) }];
	}
</script>

<div bind:this={root} class="dropdown-demo" data-modified={String(modified)}>
	<form class="dropdown-demo__form" onsubmit={send}>
		<section>
			<h3>{texts.single}</h3>
			<label class="dropdown-demo__field">
				{texts.size}
				<Dropdown name="size" options={sizes} bind:value={size} filter={texts.filter}
					noMatch={texts.noMatch} more={texts.more} remove={texts.remove} />
			</label>
			<label class="dropdown-demo__field">
				{texts.country}
				<Dropdown name="country" options={countries} bind:value={country}
					placeholder={texts['country.none']} filter={texts.filter} noMatch={texts.noMatch}
					more={texts.more} remove={texts.remove} />
			</label>
		</section>
		<section>
			<h3>{texts.multiple}</h3>
			<label class="dropdown-demo__field">
				{texts.skills}
				<Dropdown name="skills" multiple options={config.skills} bind:value={skills}
					filter={texts.filter} noMatch={texts.noMatch} more={texts.more}
					remove={texts.remove} />
			</label>
			<label class="dropdown-demo__field">
				{texts.languages}
				<Dropdown name="languages" multiple options={config.languages} bind:value={languages}
					placeholder={texts['languages.none']} filterThreshold={0} filter={texts.filter}
					noMatch={texts.noMatch} more={texts.more} remove={texts.remove} />
			</label>
		</section>
		<section>
			<h3>{texts.disabledTitle}</h3>
			<label class="dropdown-demo__field">
				{texts.size}
				<Dropdown name="disabled" options={sizes} value={config.values.disabled} disabled
					filter={texts.filter} noMatch={texts.noMatch} more={texts.more}
					remove={texts.remove} />
			</label>
		</section>
		<p><button type="submit" class="dropdown-demo__send">{texts.send}</button></p>
	</form>
	<p class="dropdown-demo__choices">{choices}</p>
	<Feedback {messages} />
</div>
