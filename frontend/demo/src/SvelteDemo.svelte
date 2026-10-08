<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!-- A country and skills chosen with the Svelte drop-downs, from choices the server sends. -->
<script lang="ts">
	import { Dropdown } from '@advanced-widgets/svelte';

	import { describe, loadChoices, type Choices, type Texts } from './data.js';

	let { url, texts }: { url: string; texts: Texts } = $props();

	let choices = $state<Choices | null>(null);
	let country = $state('');
	let skills = $state<string[]>([]);

	$effect(() => {
		loadChoices(url).then((loaded) => (choices = loaded), (error) => console.error(error));
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
	</div>
{:else}
	<p class="framework-demo__loading">{texts.loading}</p>
{/if}
