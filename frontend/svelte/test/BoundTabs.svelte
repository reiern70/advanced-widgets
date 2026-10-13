<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!-- Tabs whose tab selected is bound, shown next to them, closing their tabs as the user does. -->
<script lang="ts">
	import { untrack } from 'svelte';
	import { Tabs, type TabItem, type TabsDesign, type TabsOrientation } from '../src/index.js';

	let { selected = $bindable(), orientation = 'horizontal', design = 'tabs', onclose,
		confirmClose, items = [
			{ id: 'one', title: 'One', closable: true },
			{ id: 'two', title: 'Two', closable: true, modified: true },
			{ id: 'three', title: 'Three' }
		] }: { selected?: string | null; orientation?: TabsOrientation; design?: TabsDesign;
			onclose?: (tab: string) => void;
			confirmClose?: (tab: string, title: string) => boolean; items?: TabItem[] } = $props();

	let tabs = $state<TabItem[]>(untrack(() => items));
	let instance = $state<ReturnType<typeof Tabs>>();
	let infos = $state(0);
</script>

<Tabs bind:this={instance} id="t" {tabs} bind:selected {orientation} {design} {confirmClose}
	label="Documents" onclose={(tab) => {
		tabs = tabs.filter((each) => each.id !== tab);
		onclose?.(tab);
	}}>
	{#snippet panel(tab)}<p>Panel {tab.title}</p>{/snippet}
	{#snippet icon(tab)}<svg data-icon={tab.id}></svg>{/snippet}
	{#snippet actions(tab)}<button type="button" class="info" onclick={() => infos++}>
		{tab.id}</button>{/snippet}
</Tabs>
<output>{selected}</output>
<span class="infos">{infos}</span>
<button type="button" class="add" onclick={() => {
	tabs = [...tabs, { id: 'four', title: 'Four', closable: true }];
	selected = 'four';
}}>add</button>
<button type="button" class="pick" onclick={() => (selected = 'three')}>pick</button>
<button type="button" class="handle" onclick={() => instance?.close('three')}>close</button>
