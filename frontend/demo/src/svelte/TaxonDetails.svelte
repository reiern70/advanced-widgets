<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  The group or the species selected in a tree of the demos, as the Wicket page shows it: its
  lineage, the card of its group and, for a species, its own card below; or an invitation to
  select one.
-->
<script lang="ts">
	import { groupCard, type TaxonData, type TreeDemoTexts } from '../tree.js';
	import TaxonCard from './TaxonCard.svelte';

	let { taxon, texts }: { taxon: TaxonData | null; texts: TreeDemoTexts } = $props();

	const group = $derived(taxon && groupCard(taxon));
</script>

<section class="tree-demo-taxon" aria-live="polite">
	<h2 class="tree-demo-taxon__title">{texts.selected}</h2>
	{#if taxon}
		<ol class="tree-demo-taxon__lineage" aria-label={texts.lineage}>
			{#each taxon.lineage as name, index (index)}
				<li aria-current={index === taxon.lineage.length - 1 ? 'true' : undefined}>{name}</li>
			{/each}
		</ol>
		{#if group}
			<TaxonCard card={group} kind="group" {texts} />
		{/if}
		{#if !taxon.group}
			<TaxonCard card={taxon} kind="species" {texts} />
		{/if}
	{:else}
		<p class="tree-demo-taxon__none">{texts.noneSelected}</p>
	{/if}
</section>
