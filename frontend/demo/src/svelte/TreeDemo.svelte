<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  The tree demo in Svelte: the tree of life in a Tree, loaded from the tree of life resource group
  by group, groups selected as well as species; the cards of what is selected are shown next to
  it. The drop-down above it chooses its icons.
-->
<script lang="ts">
	import { untrack } from 'svelte';

	import { Tree } from '@antilia/svelte';

	import { loadTaxa, type IconSet, type TaxonData, type TreeDemoConfig } from '../tree.js';
	import TaxonDetails from './TaxonDetails.svelte';
	import IconsChoice from './IconsChoice.svelte';

	let { config }: { config: TreeDemoConfig } = $props();

	const load = $derived(loadTaxa(config.dataUrl));
	let taxon = $state<TaxonData | null>(null);
	let icons = $state<IconSet>(untrack(() => config.icons));
</script>

<div class="tree-demo tree-demo--single">
	<div class="tree-demo__stack">
		<div class="tree-demo__options">
			<IconsChoice id="svelte-tree-icons" value={icons} texts={config.texts}
				onchange={(value) => (icons = value)} />
		</div>
		<Tree {load} template={config.template} label={config.texts.treeOfLife} selectBranches
			expanded={config.expanded} texts={config.texts.tree} height={config.height}
			icons={config.iconSets[icons]} class="tree-demo__box"
			onselect={(_key, data) => (taxon = data as TaxonData)} />
	</div>
	<aside class="tree-demo__details">
		<TaxonDetails {taxon} texts={config.texts} />
	</aside>
</div>
