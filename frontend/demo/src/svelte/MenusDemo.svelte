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
  The Menus demo in Svelte: the menus of the Wicket page, in its markup. The app keeps the section
  of the navigation and the view of the side bar, whose items are current, and says what the user
  chose.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { Dropdown, Menu, Tabs, type MenuDesign, type MenuEntry, type MenuOrientation,
		type TabItem } from '@antilia/svelte';

	import { dropdownTexts, format } from '../data.js';
	import { applicationEntries, at, describe, galleryEntries, navigationEntries, pathOf,
		sidebarEntries, type DemoEntry, type MenusConfig } from '../menus.js';

	let { config }: { config: MenusConfig } = $props();

	const texts = $derived(config.texts);
	// the settings start as the page has them, then are the app's
	const initial = untrack(() => config);
	let applicationMessage = $state(initial.texts.applicationStart);
	let section = $state(initial.section);
	let view = $state(initial.view);
	let vertical = $state(initial.vertical);
	let design = $state<MenuDesign>(initial.design);
	let inline = $state(initial.inline);
	let hover = $state(initial.hover);
	let settingsMessage = $state(initial.texts.settingsAppStart);

	// the cases, one at a time
	const cases = $derived<TabItem[]>(['applicationTitle', 'navigationTitle', 'sidebarTitle',
		'settingsTitle', 'galleryTitle'].map((id) => ({ id, title: texts[id], modified: false })));

	/** @returns the items of a menu of the demo as Svelte's Menu takes them */
	function entries(items: DemoEntry[]): MenuEntry[] {
		return items.map((item): MenuEntry => item.separator ? { id: item.id, separator: true } : {
			id: item.id, label: item.label ?? '', icon: item.icon, tooltip: item.tooltip,
			disabled: item.disabled, href: item.href, current: item.current, onclick: item.action,
			items: item.items && entries(item.items)
		});
	}

	const application = $derived(entries(applicationEntries(config,
		(message) => applicationMessage = message)));
	const navigation = $derived(entries(navigationEntries(config, section,
		(next) => section = next)));
	const sidebar = $derived(entries(sidebarEntries(config, view, (next) => view = next)));
	const settings = $derived(entries(applicationEntries(config,
		(message) => settingsMessage = message)));
	const gallery = $derived(entries(galleryEntries(config)));
</script>

<div class="menus-demo">
	<Tabs design="pills" label={texts.cases} tabs={cases}>
		{#snippet panel(tab)}
			{#if tab.id === 'applicationTitle'}
				<section>
					<p class="menus-demo__intro">{texts.applicationAppIntro}</p>
					<div class="menus-demo__window">
						<nav class="menus-demo__application">
							<Menu label={texts.application} items={application} />
						</nav>
						<div class="menus-demo__document">{texts.documentText}</div>
					</div>
					<p class="menus-demo__status" role="status">{applicationMessage}</p>
				</section>
			{:else if tab.id === 'navigationTitle'}
				<section>
					<p class="menus-demo__intro">{texts.navigationAppIntro}</p>
					<nav class="menus-demo__navigation">
						<Menu label={texts.navigation} design="underline" openOnHover items={navigation} />
					</nav>
					<div class="menus-demo__page">
						<h4 class="menus-demo__where">{at(config, section)}</h4>
						<p class="menus-demo__text">{format(texts.routed, section)}</p>
					</div>
				</section>
			{:else if tab.id === 'sidebarTitle'}
				<section class="menus-demo__app">
					<nav class="menus-demo__sidebar">
						<Menu label={texts.sidebar} orientation="vertical" design="sidebar" inline
							items={sidebar} />
					</nav>
					<div class="menus-demo__content">
						<h4 class="menus-demo__where">{describe(config, pathOf(config.sidebar, view))}</h4>
						<p class="menus-demo__text">{texts.sidebarAppText}</p>
					</div>
				</section>
			{:else if tab.id === 'settingsTitle'}
				<section>
					<div class="menus-demo__controls">
						<label class="menus-demo__control">
							<input type="checkbox" bind:checked={vertical} /> {texts.vertical}
						</label>
						<label class="menus-demo__control">
							{texts.design}
							<Dropdown value={design} options={config.designs.map((each) =>
								({ value: each, label: texts['design.' + each.toUpperCase()] }))}
								onchange={(value) => design = value as MenuDesign}
								{...dropdownTexts(texts)} />
						</label>
						<label class="menus-demo__control">
							<input type="checkbox" bind:checked={inline} /> {texts.inline}
						</label>
						<label class="menus-demo__control">
							<input type="checkbox" bind:checked={hover} /> {texts.hover}
						</label>
					</div>
					<div class="menus-demo__preview">
						<nav>
							<Menu label={texts.application} orientation={vertical ? 'vertical' : 'horizontal'}
								{design} {inline} openOnHover={hover} items={settings} />
						</nav>
					</div>
					<p class="menus-demo__status" role="status">{settingsMessage}</p>
				</section>
			{:else if tab.id === 'galleryTitle'}
				<section>
					{@render designs(texts.alongTheTop, 'horizontal')}
					{@render designs(texts.downTheSide, 'vertical')}
				</section>
			{/if}
		{/snippet}
	</Tabs>
</div>

{#snippet designs(title: string, orientation: MenuOrientation)}
	<h4 class="menus-demo__group">{title}</h4>
	<div class={'menus-demo__gallery menus-demo__gallery--' +
		(orientation === 'horizontal' ? 'top' : 'side')}>
		{#each config.designs as shown (shown)}
			<figure class="menus-demo__example">
				<figcaption>{texts['design.' + shown.toUpperCase()]}</figcaption>
				<nav>
					<Menu label={texts['design.' + shown.toUpperCase()]} design={shown} {orientation}
						items={gallery} />
				</nav>
			</figure>
		{/each}
	</div>
{/snippet}
