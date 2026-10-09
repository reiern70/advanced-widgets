<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  The tree demo in Svelte: the files of advanced-widgets in a Tree, loaded from the files resource
  folder by folder; the file selected is shown next to it.
-->
<script lang="ts">
	import { Tree } from '@advanced-widgets/svelte';

	import { loadFiles, type FileData, type TreeDemoConfig } from '../tree.js';
	import FileDetails from './FileDetails.svelte';

	let { config }: { config: TreeDemoConfig } = $props();

	const load = $derived(loadFiles(config.filesUrl));
	let file = $state<FileData | null>(null);
</script>

<div class="tree-demo tree-demo--single">
	<Tree {load} template={config.template} label={config.texts.files}
		expanded={config.expanded} texts={config.texts.tree} height={config.height}
		class="tree-demo__box" onselect={(_key, data) => (file = data as FileData)} />
	<aside class="tree-demo__details">
		<FileDetails {file} texts={config.texts} />
	</aside>
</div>
