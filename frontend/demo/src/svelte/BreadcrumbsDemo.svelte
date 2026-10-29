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
  The Breadcrumbs demo in Svelte: the breadcrumbs of the Wicket page, in its markup. The app keeps
  the page of the site and the folder open, and says which crumb of the settings the user chose.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { Breadcrumb, Dropdown, Tabs, type BreadcrumbItem, type BreadcrumbSeparator,
		type TabItem } from '@antilia/svelte';

	import { dropdownTexts } from '../data.js';
	import { below, folderCrumbs, label, maxItemsLabel, placeAt, settingsCrumbs, siteCrumbs,
		siteHref, type BreadcrumbsConfig, type DemoCrumb, type Place } from '../breadcrumbs.js';

	let { config }: { config: BreadcrumbsConfig } = $props();

	const texts = $derived(config.texts);
	// the settings start as the page has them, then are the app's
	const initial = untrack(() => config);
	let sitePath = $state<string[]>([]);
	let folder = $state<string[]>(initial.folder);
	let separator = $state<BreadcrumbSeparator>(initial.separator);
	let maxItems = $state(initial.maxItems);
	let message = $state(initial.texts.settingsAppStart);

	// the cases, one at a time
	const cases = $derived<TabItem[]>(['siteTitle', 'foldersTitle', 'settingsTitle']
		.map((id) => ({ id, title: texts[id], modified: false })));

	/** @returns the crumbs of the demo as Svelte's Breadcrumb takes them */
	function items(crumbs: DemoCrumb[]): BreadcrumbItem[] {
		return crumbs.map((crumb) => ({ id: crumb.id, label: crumb.label, iconMarkup: crumb.icon,
			href: crumb.href, onclick: crumb.action }));
	}

	const page = $derived(placeAt(config.site, sitePath));
	const open = $derived(placeAt(config.files, folder));
	const site = $derived(items(siteCrumbs(config, sitePath, (next) => sitePath = next)));
	const folders = $derived(items(folderCrumbs(config, folder, (next) => folder = next)));
	const settings = $derived(items(settingsCrumbs(config, (next) => message = next)));
</script>

{#snippet place(each: Place, onclick: (event: MouseEvent) => void, href?: string)}
	<li>
		{#if href !== undefined}
			<a class="breadcrumbs-demo__place" {href} {onclick}>{@render content(each)}</a>
		{:else}
			<button type="button" class="breadcrumbs-demo__place" {onclick}>{@render content(each)}</button>
		{/if}
	</li>
{/snippet}

{#snippet content(each: Place)}
	{#if each.icon}<span class="breadcrumbs-demo__icon">{@html each.icon}</span>{/if}
	<span>{label(config, each)}</span>
{/snippet}

<div class="breadcrumbs-demo">
	<Tabs design="pills" label={texts.cases} tabs={cases}>
		{#snippet panel(tab)}
			{#if tab.id === 'siteTitle'}
				<section>
					<p class="breadcrumbs-demo__intro">{texts.siteAppIntro}</p>
					<Breadcrumb texts={config.breadcrumbTexts} items={site} />
					<div class="breadcrumbs-demo__page">
						<h4 class="breadcrumbs-demo__heading">{label(config, page)}</h4>
						<p class="breadcrumbs-demo__text">{below(config, page)}</p>
						<ul class="breadcrumbs-demo__places">
							{#each page.children ?? [] as child (child.name)}
								{@render place(child, (event) => {
									event.preventDefault();
									sitePath = [...sitePath, child.name];
								}, siteHref([...sitePath, child.name]))}
							{/each}
						</ul>
					</div>
				</section>
			{:else if tab.id === 'foldersTitle'}
				<section>
					<p class="breadcrumbs-demo__intro">{texts.foldersAppIntro}</p>
					<Breadcrumb texts={{ ...config.breadcrumbTexts, label: texts.folders }}
						maxItems={config.foldersMaxItems} items={folders} />
					<div class="breadcrumbs-demo__page">
						<h4 class="breadcrumbs-demo__heading">{label(config, open)}</h4>
						<ul class="breadcrumbs-demo__places">
							{#each open.children ?? [] as child (child.name)}
								{@render place(child, () => folder = [...folder, child.name])}
							{/each}
						</ul>
						{#if !open.children?.length}
							<p class="breadcrumbs-demo__text">{texts.empty}</p>
						{/if}
					</div>
				</section>
			{:else if tab.id === 'settingsTitle'}
				<section>
					<p class="breadcrumbs-demo__intro">{texts.settingsAppIntro}</p>
					<div class="breadcrumbs-demo__controls">
						<label class="breadcrumbs-demo__control">
							{texts.separator}
							<Dropdown value={separator} options={config.separators.map((each) =>
								({ value: each, label: texts['separator.' + each.toUpperCase()] }))}
								onchange={(value) => separator = value as BreadcrumbSeparator}
								{...dropdownTexts(texts)} />
						</label>
						<label class="breadcrumbs-demo__control">
							{texts.maxItems}
							<Dropdown value={String(maxItems)} options={config.maxItemsChoices.map((each) =>
								({ value: String(each), label: maxItemsLabel(config, each) }))}
								onchange={(value) => maxItems = Number(value)}
								{...dropdownTexts(texts)} />
						</label>
					</div>
					<div class="breadcrumbs-demo__preview">
						<Breadcrumb texts={config.breadcrumbTexts} {separator} {maxItems} items={settings} />
					</div>
					<p class="breadcrumbs-demo__status" role="status">{message}</p>
				</section>
			{/if}
		{/snippet}
	</Tabs>
</div>
