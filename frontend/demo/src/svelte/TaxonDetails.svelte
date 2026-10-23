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
