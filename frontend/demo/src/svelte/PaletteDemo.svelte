<!--
  Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)

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
  The Palette demo in Svelte: the form of the Wicket page, in its markup, the sentence below it
  following every change in the browser.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { Palette, Tabs, type TabItem } from '@antilia/svelte';

	import { format } from '../data.js';
	import { describe, describeCountries, MATCHES, type PaletteConfig } from '../palette.js';
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

	// the cases, one at a time
	const cases = $derived<TabItem[]>(['unordered', 'orderedTitle', 'manyTitle', 'matchTitle',
		'disabledTitle'].map((id) => ({ id, title: texts[id as keyof typeof texts],
		modified: false })));

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
		<Tabs design="pills" label={texts.cases} tabs={cases}>
			{#snippet panel(tab)}
				{#if tab.id === 'unordered'}
					<section>
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
				{:else if tab.id === 'orderedTitle'}
					<section>
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
				{:else if tab.id === 'manyTitle'}
					<section>
						<div class="palette-demo__field">
							<span>{texts.countries}</span>
							<Palette name="countries" options={config.countries} bind:value={countries}
								{...config.palette} />
						</div>
						<p class="palette-demo__count">{describeCountries(config, countries)}</p>
					</section>
				{:else if tab.id === 'matchTitle'}
					<section>
						<p class="palette-demo__hint">{texts.matchHint}</p>
						<div class="palette-demo__matches">
							{#each MATCHES as [match, label] (match)}
								<div class="palette-demo__field">
									<span>{texts[label]}</span>
									<Palette options={config.countries} {match} {...config.palette} />
								</div>
							{/each}
						</div>
					</section>
				{:else if tab.id === 'disabledTitle'}
					<section>
						<div class="palette-demo__field">
							<span>{texts.skills}</span>
							<Palette options={config.skills} value={config.values.disabled} disabled
								{...config.palette} />
						</div>
					</section>
				{/if}
			{/snippet}
		</Tabs>
		<p><button type="submit" class="palette-demo__send">{texts.send}</button></p>
	</form>
	<p class="palette-demo__choices">{choices}</p>
	<Feedback {messages} />
</div>
