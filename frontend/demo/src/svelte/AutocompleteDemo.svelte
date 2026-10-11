<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  The Autocomplete demo in Svelte: the form of the Wicket page, in its markup, the users searched
  through the users resource, the countries filtered in the browser.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { Autocomplete } from '@antilia/svelte';

	import { describe, flag, MAX_SUGGESTIONS, searchCountries,
		type AutocompleteConfig } from '../autocomplete.js';
	import { format, searchUsers } from '../data.js';
	import Feedback from './Feedback.svelte';
	import type { Message } from './feedback.js';

	let { config }: { config: AutocompleteConfig } = $props();

	const texts = $derived(config.texts);
	// the values start as the page has them, then are the app's
	const initial = untrack(() => config.values);
	let owner = $state(initial.owner);
	let team = $state(initial.team);
	let countries = $state(initial.countries);
	let visits = $state(initial.visits);
	let messages = $state<Message[]>([]);

	const users = $derived(searchUsers(config.usersUrl, MAX_SUGGESTIONS));
	const searchCountry = $derived(searchCountries(config.choicesUrl));
	// the find bar leaves out the countries listed already
	const unlisted = $derived(searchCountries(config.choicesUrl, () => visits));
	const choices = $derived(describe(texts, { owner, team, countries, visits }));

	function send(event: SubmitEvent) {
		event.preventDefault();
		messages = [owner.length ? { level: 'info', text: format(texts.sent, choices) }
			: { level: 'error', text: texts.required }];
	}
</script>

<div class="autocomplete-demo">
	<form class="autocomplete-demo__form" onsubmit={send}>
		<section>
			<h3>{texts.single}</h3>
			<label class="autocomplete-demo__field">
				{texts.owner}
				<Autocomplete name="owner" search={users} bind:value={owner}
					template={config.userTemplate} minLength={1}
					placeholder={texts['owner.placeholder']} noMatch={texts.noMatch}
					searching={texts.searching} remove={texts.remove} />
			</label>
		</section>
		<section>
			<h3>{texts.multiple}</h3>
			<label class="autocomplete-demo__field">
				{texts.team}
				<Autocomplete name="team" multiple search={users} bind:value={team}
					template={config.userTemplate} minLength={1}
					placeholder={texts['team.placeholder']} noMatch={texts.noMatch}
					searching={texts.searching} remove={texts.remove} />
			</label>
			<label class="autocomplete-demo__field">
				{texts.countries}
				<Autocomplete name="countries" multiple search={searchCountry} bind:value={countries}
					template={config.countryTemplate} minLength={0}
					placeholder={texts['countries.placeholder']} noMatch={texts.noMatch}
					searching={texts.searching} remove={texts.remove} />
			</label>
		</section>
		<section>
			<h3>{texts.find}</h3>
			<label class="autocomplete-demo__field">
				{texts.visits}
				<Autocomplete name="visitFind" pick search={unlisted}
					onpick={(country) => (visits = visits.concat([country]))}
					template={config.countryTemplate} minLength={0}
					placeholder={texts['visits.placeholder']} noMatch={texts.noMatch}
					searching={texts.searching} remove={texts.remove} />
			</label>
			{#if visits.length}
				<ul class="autocomplete-demo__list">
					{#each visits as country (country.id)}
						<li>
							<span class="ac-country">
								<span class="ac-flag" aria-hidden="true">{flag(String(country.id))}</span>
								{country.label}
							</span>
							<button type="button" class="autocomplete-demo__remove"
								aria-label={format(texts.remove, country.label)}
								onclick={() => (visits = visits.filter((listed) => listed.id !== country.id))}
							>×</button>
						</li>
					{/each}
				</ul>
			{:else}
				<p class="autocomplete-demo__none">{texts['visits.none']}</p>
			{/if}
		</section>
		<p><button type="submit" class="autocomplete-demo__send">{texts.send}</button></p>
	</form>
	<p class="autocomplete-demo__choices">{choices}</p>
	<Feedback {messages} />
</div>
