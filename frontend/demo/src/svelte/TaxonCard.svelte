<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!-- The card of a group or a species, as the Wicket page shows it. -->
<script lang="ts">
	import { FACTS, type TaxonCard, type TreeDemoTexts } from '../tree.js';

	let { card, kind, texts }: { card: TaxonCard; kind: 'group' | 'species';
		texts: TreeDemoTexts } = $props();
</script>

<article class={'tree-demo-card tree-demo-card--' + kind}>
	<p class="tree-demo-card__rank">{card.rank}</p>
	<h3 class="tree-demo-card__name">{card.name}</h3>
	{#if card.scientific}
		<p class="tree-demo-card__scientific">{card.scientific}</p>
	{/if}
	<p class="tree-demo-card__description">{card.description}</p>
	<dl class="tree-demo-card__facts">
		{#each FACTS as fact (fact)}
			<dt>{texts[fact]}</dt>
			<dd>{card[fact]}</dd>
		{/each}
	</dl>
</article>
