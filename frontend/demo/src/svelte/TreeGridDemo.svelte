<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  The tree grid demo in Svelte: the files of advanced-widgets in a TreeGrid with the columns of
  the Wicket one; the file selected is shown next to it.
-->
<script lang="ts">
	import { TreeGrid } from '@advanced-widgets/svelte';

	import { loadFiles, type FileData, type TreeGridDemoConfig } from '../tree.js';
	import FileDetails from './FileDetails.svelte';

	let { config }: { config: TreeGridDemoConfig } = $props();

	const load = $derived(loadFiles(config.filesUrl));
	let file = $state<FileData | null>(null);
</script>

<div class="tree-demo tree-demo--single">
	<div class="tree-demo__box tree-demo__box--table">
		<TreeGrid {load} template={config.template} label={config.texts.files}
			header={config.header} columns={config.columns} expanded={config.expanded}
			texts={config.texts.tree}
			onselect={(_key, data) => (file = data as FileData)} />
	</div>
	<aside class="tree-demo__details">
		<FileDetails {file} texts={config.texts} />
	</aside>
</div>
