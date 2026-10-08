<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  A country, skills and colleagues chosen with the Svelte components, from data the server sends,
  and a list of all users scrolling infinitely.
-->
<script lang="ts">
	import { Autocomplete, Dropdown, InfiniteList,
		type AutocompleteValue } from '@advanced-widgets/svelte';

	import { describe, loadChoices, pagesOfUsers, searchUsers, SUGGESTION_TEMPLATE, USER_TEMPLATE,
		type Choices, type Texts, type Urls } from './data.js';

	let { urls, texts }: { urls: Urls; texts: Texts } = $props();

	let choices = $state<Choices | null>(null);
	let country = $state('');
	let skills = $state<string[]>([]);
	let colleagues = $state<AutocompleteValue[]>([]);
	let picked = $state<string | null>(null);
	const search = $derived(searchUsers(urls.users));
	const load = $derived(pagesOfUsers(urls.users));

	$effect(() => {
		loadChoices(urls.choices).then((loaded) => (choices = loaded),
			(error) => console.error(error));
	});
</script>

{#if choices}
	<div class="framework-demo__form">
		<label class="framework-demo__field">
			{texts.country}
			<Dropdown name="country" options={[{ value: '', label: texts.countryNone },
				...choices.countries]} bind:value={country} placeholder={texts.countryNone}
				filter={texts.filter} noMatch={texts.noMatch} more={texts.more} remove={texts.remove} />
		</label>
		<label class="framework-demo__field">
			{texts.skills}
			<Dropdown name="skills" multiple options={choices.skills} bind:value={skills}
				placeholder={texts.skillsNone} filterThreshold={0} filter={texts.filter}
				noMatch={texts.noMatch} more={texts.more} remove={texts.remove} />
		</label>
		<p class="framework-demo__chosen">{describe(texts, choices, country, skills)}</p>
		<div class="framework-demo__field">
			<span>{texts.colleagues}</span>
			<Autocomplete name="colleagues" multiple {search} bind:value={colleagues}
				template={SUGGESTION_TEMPLATE} placeholder={texts.colleaguesNone}
				searching={texts.searching} noMatch={texts.noMatch} remove={texts.remove} />
		</div>
		<p class="framework-demo__colleagues">
			{colleagues.map((colleague) => colleague.label).join(', ') || texts.nothing}
		</p>
		<h4 class="framework-demo__heading">{texts.people}</h4>
		<InfiniteList {load} template={USER_TEMPLATE} height={{ value: 240, unit: 'px' }}
			texts={{ loading: texts.listLoading, end: texts.listEnd, empty: texts.listEmpty }}
			onaction={(_action, key) => (picked = key)} />
		<p class="framework-demo__picked">
			{picked ? texts.picked.replace('{0}', picked) : texts.nothing}
		</p>
	</div>
{:else}
	<p class="framework-demo__loading">{texts.loading}</p>
{/if}
