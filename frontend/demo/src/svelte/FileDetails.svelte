<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!-- The file selected in a tree of the demos, as the Wicket page shows it, or an invitation to select one. -->
<script lang="ts">
	import { FACTS, type FileData, type TreeDemoTexts } from '../tree.js';

	let { file, texts }: { file: FileData | null; texts: TreeDemoTexts } = $props();
</script>

<section class="tree-demo-file" aria-live="polite">
	<h2 class="tree-demo-file__title">{texts.selectedFile}</h2>
	{#if file}
		<dl class="tree-demo-file__facts">
			{#each FACTS as fact (fact)}
				<dt>{texts[fact]}</dt>
				<dd class={fact === 'name' || fact === 'path' ? 'tree-demo-file__' + fact : undefined}>
					{file[fact]}</dd>
			{/each}
		</dl>
	{:else}
		<p class="tree-demo-file__none">{texts.noneSelected}</p>
	{/if}
</section>
