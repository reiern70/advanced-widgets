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
  The Autocomplete demo in Svelte: the form of the Wicket page, in its markup, the users searched
  through the users resource, the countries filtered in the browser.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import type { AutocompleteSuggestion } from '@antilia/core';
	import { Autocomplete } from '@antilia/svelte';

	import { calling, describe, flag, MAX_SUGGESTIONS, searchCountries, userActions,
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
	// whom the phone at the end of a row of a user called last
	let call = $state('');
	const actions = $derived(userActions(config));
	const onaction = (_action: string, user: AutocompleteSuggestion) => {
		call = calling(texts, user);
	};

	const users = $derived(searchUsers(config.usersUrl, MAX_SUGGESTIONS));
	const searchCountry = $derived(searchCountries(config.choicesUrl));
	// the find bar leaves out the countries listed already
	const unlisted = $derived(searchCountries(config.choicesUrl, () => visits));
	const choices = $derived(describe(texts, { owner, team, countries, visits,
		reviewer: initial.reviewer, markets: initial.markets }));

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
					template={config.userTemplate} minLength={1} {actions} {onaction}
					placeholder={texts['owner.placeholder']} noMatch={texts.noMatch}
					searching={texts.searching} remove={texts.remove} />
			</label>
		</section>
		<section>
			<h3>{texts.multiple}</h3>
			<label class="autocomplete-demo__field">
				{texts.team}
				<Autocomplete name="team" multiple search={users} bind:value={team}
					template={config.userTemplate} minLength={1} {actions} {onaction}
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
			<h3>{texts.disabledTitle}</h3>
			<label class="autocomplete-demo__field">
				{texts.reviewer}
				<Autocomplete name="reviewer" search={users} value={initial.reviewer} disabled
					template={config.userTemplate} noMatch={texts.noMatch}
					searching={texts.searching} remove={texts.remove} />
			</label>
			<label class="autocomplete-demo__field">
				{texts.markets}
				<Autocomplete name="markets" multiple search={searchCountry} value={initial.markets}
					disabled template={config.countryTemplate} noMatch={texts.noMatch}
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
	{#if call}
		<p class="autocomplete-demo__calling" role="status">{call}</p>
	{/if}
	<Feedback {messages} />
</div>
