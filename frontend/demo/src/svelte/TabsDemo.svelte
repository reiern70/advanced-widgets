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
  The Tabs demo in Svelte: the tabs of the Wicket page, in its markup. The app keeps the documents:
  a tab is marked while its text differs from the one saved, and closing it takes the document
  away. Its action shows what the app knows of the document in a popover pointing at it.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { Button, Dropdown, Popover, Tabs, type TabItem, type TabsDesign } from '@antilia/svelte';

	import { dropdownTexts, format } from '../data.js';
	import { afterSelecting, ALERTS, documentsOf, LIVE, newDocument, popoverOf, REPORT,
		sectionTitle, withAddedSection, type CreatedPanels, type Document, type GalleryConfig,
		type TabsConfig } from '../tabs.js';

	let { config }: { config: TabsConfig } = $props();

	const texts = $derived(config.texts);
	const icons = $derived(config.icons);
	// the documents and the controls start as the page has them, then are the app's
	const initial = untrack(() => config);
	let documents = $state<Document[]>(documentsOf(initial));
	let selected = $state<string | null>('welcome');
	let untitled = $state(0);
	let message = $state(initial.texts.start);
	let vertical = $state(initial.vertical);
	let design = $state<TabsDesign>(initial.design);
	let section = $state<string | null>(initial.sections[0]);
	let sectionNames = $state<string[]>(initial.sections);
	let alerts = $state(0);
	let report = $state<string | null>('summary');
	let created = $state<CreatedPanels>({ panels: 0, report: 0, live: 0 });

	const tabs = $derived<TabItem[]>([{ id: 'welcome', title: texts.welcome },
		...documents.map((document) => ({ id: document.id, title: document.name, closable: true,
			modified: document.text !== document.saved }))]);
	// the cases, one at a time
	const cases = $derived<TabItem[]>(['documentsTitle', 'settingsTitle', 'lazyTitle', 'monthsTitle',
		'galleryTitle']
		.map((id) => ({ id, title: texts[id], modified: false })));
	const sections = $derived<TabItem[]>(sectionNames.map((name) => ({ id: name,
		title: sectionTitle(config, name, alerts) })));

	// the panel of the report is created when first selected, the live one each time
	const reports = $derived<TabItem[]>([{ id: 'summary', title: texts.summary },
		{ id: REPORT, title: texts.report, lazy: true }, { id: LIVE, title: texts.live }]);

	// more tabs than fit, with the menu of all tabs
	const months = $derived<TabItem[]>(config.months.map((month, index) => ({ id: 'month-' + index,
		title: month })));

	const galleryTabs = (part: GalleryConfig): TabItem[] => part.tabs.map((tab) => ({ id: tab.name,
		title: texts[tab.name] }));

	function documentOf(id: string): Document | undefined {
		return documents.find((document) => document.id === id);
	}

	function open() {
		untitled += 1;
		const document = newDocument(config, untitled);
		documents.push(document);
		selected = document.id;
		message = format(texts.opened, document.name);
	}

	function close(id: string) {
		const closed = documentOf(id);
		documents = documents.filter((document) => document.id !== id);
		if (closed) {
			message = format(texts.closed, closed.name);
		}
	}

	function addSection() {
		sectionNames = withAddedSection(config, sectionNames);
		section = config.addedSection;
	}

	function selectSection(name: string) {
		if (name === ALERTS) {
			alerts = 0;
		}
	}

	function save(event: SubmitEvent, document: Document) {
		event.preventDefault();
		document.saved = document.text;
		message = format(texts.saved, document.name);
	}
</script>

<div class="tabs-demo">
	<Tabs design="pills" label={texts.cases} tabs={cases}>
		{#snippet panel(tab)}
			{#if tab.id === 'documentsTitle'}
				<section>
					<div class="tabs-demo__heading">
						<Button label={texts.newDocument} size="small" class="tabs-demo__new" onclick={open} />
					</div>
					<Tabs class="tabs-demo__documents" label={texts.documents} texts={config.tabs} {tabs}
						bind:selected onclose={close}>
						{#snippet icon(tab)}
							<span class="tabs-demo__icon">{@html tab.id === 'welcome' ? icons.welcome :
								icons.document}</span>
						{/snippet}
						{#snippet actions(tab)}
							{@const document = documentOf(tab.id)}
							{#if document}
								<button type="button" title={texts.about} aria-label={texts.about}
									onclick={(event) => Popover.toggle(event.currentTarget, popoverOf(config,
										document))}>{@html icons.info}</button>
							{/if}
						{/snippet}
						{#snippet panel(tab)}
							{@const document = documentOf(tab.id)}
							{#if document}
								<form class="tabs-demo__document" onsubmit={(event) => save(event, document)}>
									<textarea class="tabs-demo__editor" rows="6" aria-label={document.name}
										bind:value={document.text}></textarea>
									<div><Button type="submit" label={texts.save} variant="primary" size="small" /></div>
								</form>
							{:else}
								<p class="tabs-demo__text">{texts.welcomeText}</p>
							{/if}
						{/snippet}
					</Tabs>
					<p class="tabs-demo__status" role="status">{message}</p>
				</section>
			{:else if tab.id === 'lazyTitle'}
				<section>
					<Tabs class="tabs-demo__reports" label={texts.reports} texts={config.tabs} tabs={reports}
						bind:selected={report} onselect={(name) => created = afterSelecting(created, name)}>
						{#snippet panel(tab)}
							{#if tab.id === REPORT}
								{format(texts['report.text'], created.report)}
							{:else if tab.id === LIVE}
								{#if report === LIVE}{format(texts['live.text'], created.live)}{/if}
							{:else}
								{texts['summary.text']}
							{/if}
						{/snippet}
					</Tabs>
					<p class="tabs-demo__status tabs-demo__created" role="status">{format(texts.created,
						created.panels)}</p>
				</section>
			{:else if tab.id === 'monthsTitle'}
				<section>
					<Tabs class="tabs-demo__months" label={texts.months} texts={config.tabs} tabs={months}
						overflowMenu={config.overflowLabel}>
						{#snippet icon()}<span class="tabs-demo__icon">{@html icons.month}</span>{/snippet}
						{#snippet panel(tab)}{format(texts['month.text'], tab.title)}{/snippet}
					</Tabs>
				</section>
			{:else if tab.id === 'settingsTitle'}
				<section>
					<div class="tabs-demo__heading">
						<Button label={texts.newAlert} size="small" class="tabs-demo__alert"
							onclick={() => alerts += 1} />
						{#if !sectionNames.includes(config.addedSection)}
							<Button label={texts.addSection} size="small" class="tabs-demo__add" onclick={addSection} />
						{/if}
					</div>
					<div class="tabs-demo__controls">
						<label class="tabs-demo__control">
							<input type="checkbox" bind:checked={vertical} /> {texts.vertical}
						</label>
						<label class="tabs-demo__control">
							{texts.design}
							<Dropdown value={design} options={config.designs.map((each) =>
								({ value: each, label: texts['design.' + each.toUpperCase()] }))}
								onchange={(value) => design = value as TabsDesign}
								{...dropdownTexts(texts)} />
						</label>
					</div>
					<Tabs class="tabs-demo__settings" label={texts.settings} texts={config.tabs} tabs={sections}
						orientation={vertical ? 'vertical' : 'horizontal'} {design} bind:selected={section}
						onselect={selectSection}>
						{#snippet panel(tab)}{texts[tab.id + '.text']}{/snippet}
					</Tabs>
					<p class="tabs-demo__status">{format(texts.selected, texts[section ?? ''] ?? '')}</p>
				</section>
			{:else if tab.id === 'galleryTitle'}
				<section>
					{@render gallery(texts.alongTheTop, config.gallery.top, 'horizontal')}
					{@render gallery(texts.downTheSide, config.gallery.side, 'vertical')}
				</section>
			{/if}
		{/snippet}
	</Tabs>
</div>

{#snippet gallery(title: string, part: GalleryConfig, orientation: 'horizontal' | 'vertical')}
	<h4 class="tabs-demo__group">{title}</h4>
	<div class={'tabs-demo__gallery tabs-demo__gallery--' +
		(orientation === 'horizontal' ? 'top' : 'side')}>
		{#each part.designs as design (design)}
			<figure class="tabs-demo__example">
				<figcaption>{texts['design.' + design.toUpperCase()]}</figcaption>
				<Tabs {design} {orientation} label={texts['design.' + design.toUpperCase()]}
					texts={config.tabs} tabs={galleryTabs(part)} selected={part.selected}>
					{#snippet icon(tab)}
						<span class="tabs-demo__icon">{@html part.tabs.find((each) =>
							each.name === tab.id)?.icon ?? ''}</span>
					{/snippet}
					{#snippet panel(tab)}{texts[tab.id + '.text']}{/snippet}
				</Tabs>
			</figure>
		{/each}
	</div>
{/snippet}
