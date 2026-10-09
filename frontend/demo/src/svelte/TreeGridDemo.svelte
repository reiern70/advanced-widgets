<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  The tree grid demo in Svelte: the files of antilia-widgets in a TreeGrid with the columns of
  the Wicket one; the file selected is shown next to it. The drop-downs above it choose how its
  columns are resized and its icons, as those of the Wicket page do.
-->
<script lang="ts">
	import { untrack } from 'svelte';

	import { Dropdown, TreeGrid } from '@antilia/svelte';

	import { loadFiles, resizeMode, resizingOptions, type FileData, type IconSet, type Resizing,
		type TreeGridDemoConfig } from '../tree.js';
	import FileDetails from './FileDetails.svelte';
	import IconsChoice from './IconsChoice.svelte';

	let { config }: { config: TreeGridDemoConfig } = $props();

	const load = $derived(loadFiles(config.filesUrl));
	let file = $state<FileData | null>(null);
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
		<TreeGrid {load} template={config.template} label={texts.files}
			header={config.header} columns={config.columns} expanded={config.expanded}
			texts={texts.tree} height={config.height} icons={config.iconSets[icons]}
			resizableColumns={resizeMode(resizing)}
			class="tree-demo__box tree-demo__box--table"
			onselect={(_key, data) => (file = data as FileData)} />
	</div>
	<aside class="tree-demo__details">
		<FileDetails {file} texts={config.texts} />
	</aside>
</div>
