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
  The tree grid demo in Svelte: the tree of life in a TreeGrid with the columns of the Wicket one,
  groups selected as well as species; the cards of what is selected are shown next to it. The
  drop-downs above it choose how its columns are resized and its icons, as those of the Wicket
  page do.
-->
<script lang="ts">
	import { untrack } from 'svelte';

	import { Dropdown, TreeGrid } from '@antilia/svelte';

	import { loadTaxa, resizeMode, resizingOptions, type IconSet, type Resizing,
		type TaxonData, type TreeGridDemoConfig } from '../tree.js';
	import TaxonDetails from './TaxonDetails.svelte';
	import IconsChoice from './IconsChoice.svelte';

	let { config }: { config: TreeGridDemoConfig } = $props();

	const load = $derived(loadTaxa(config.dataUrl));
	let taxon = $state<TaxonData | null>(null);
	let resizing = $state<Resizing>(untrack(() => config.resizableColumns));
	let icons = $state<IconSet>(untrack(() => config.icons));
	const texts = $derived(config.texts);
</script>

<div class="tree-demo tree-demo--single">
	<div class="tree-demo__stack">
		<div class="tree-demo__options">
			<label for="svelte-tree-grid-resize">{texts.resize}</label>
			<Dropdown id="svelte-tree-grid-resize" name="resize" value={resizing}
				options={resizingOptions(texts)}
				onchange={(value) => (resizing = String(value) as Resizing)}
				filter={texts.filter} noMatch={texts.noMatch} more={texts.more} remove={texts.remove} />
			<IconsChoice id="svelte-tree-grid-icons" value={icons} {texts}
				onchange={(value) => (icons = value)} />
		</div>
		<TreeGrid {load} template={config.template} label={texts.treeOfLife} selectBranches
			header={config.header} columns={config.columns} expanded={config.expanded}
			texts={texts.tree} height={config.height} icons={config.iconSets[icons]}
			resizableColumns={resizeMode(resizing)}
			class="tree-demo__box tree-demo__box--table"
			onselect={(_key, data) => (taxon = data as TaxonData)} />
	</div>
	<aside class="tree-demo__details">
		<TaxonDetails {taxon} texts={config.texts} />
	</aside>
</div>
