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
