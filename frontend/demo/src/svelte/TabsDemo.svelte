<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  The Tabs demo in Svelte: the tabs of the Wicket page, in its markup. The app keeps the documents:
  a tab is marked while its text differs from the one saved, and closing it takes the document
  away. Its action shows what the app knows of the document in a popover pointing at it.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { Button, Popover, Tabs, type TabItem, type TabsDesign } from '@antilia/svelte';

	import { format } from '../data.js';
	import { documentsOf, newDocument, popoverOf, type Document, type GalleryConfig,
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

	const tabs = $derived<TabItem[]>([{ id: 'welcome', title: texts.welcome },
		...documents.map((document) => ({ id: document.id, title: document.name, closable: true,
			modified: document.text !== document.saved }))]);
	// the cases, one at a time
	const cases = $derived<TabItem[]>(['documentsTitle', 'settingsTitle', 'galleryTitle']
		.map((id) => ({ id, title: texts[id], modified: false })));
	const sections = $derived<TabItem[]>(config.sections.map((name) => ({ id: name,
		title: texts[name] })));

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
			{:else if tab.id === 'settingsTitle'}
				<section>
					<div class="tabs-demo__controls">
						<label class="tabs-demo__control">
							<input type="checkbox" bind:checked={vertical} /> {texts.vertical}
						</label>
						<label class="tabs-demo__control">
							{texts.design}
							<select bind:value={design}>
								{#each config.designs as each (each)}
									<option value={each}>{texts['design.' + each.toUpperCase()]}</option>
								{/each}
							</select>
						</label>
					</div>
					<Tabs class="tabs-demo__settings" label={texts.settings} texts={config.tabs} tabs={sections}
						orientation={vertical ? 'vertical' : 'horizontal'} {design} bind:selected={section}>
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
