<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  The tree demo in Svelte: the files of antilia-widgets in a Tree, loaded from the files resource
  folder by folder; the file selected is shown next to it. The drop-down above it chooses its
  icons.
-->
<script lang="ts">
	import { untrack } from 'svelte';

	import { Tree } from '@antilia/svelte';

	import { loadFiles, type FileData, type IconSet, type TreeDemoConfig } from '../tree.js';
	import FileDetails from './FileDetails.svelte';
	import IconsChoice from './IconsChoice.svelte';

	let { config }: { config: TreeDemoConfig } = $props();

	const load = $derived(loadFiles(config.filesUrl));
	let file = $state<FileData | null>(null);
	let icons = $state<IconSet>(untrack(() => config.icons));
</script>

<div class="tree-demo tree-demo--single">
	<div class="tree-demo__stack">
		<div class="tree-demo__options">
			<IconsChoice id="svelte-tree-icons" value={icons} texts={config.texts}
				onchange={(value) => (icons = value)} />
		</div>
		<Tree {load} template={config.template} label={config.texts.files}
			expanded={config.expanded} texts={config.texts.tree} height={config.height}
			icons={config.iconSets[icons]} class="tree-demo__box"
			onselect={(_key, data) => (file = data as FileData)} />
	</div>
	<aside class="tree-demo__details">
		<FileDetails {file} texts={config.texts} />
	</aside>
</div>
