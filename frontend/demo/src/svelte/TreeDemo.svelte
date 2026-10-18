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
