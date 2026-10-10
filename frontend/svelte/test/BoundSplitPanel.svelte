<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!-- A SplitPanel whose position and collapsed pane are bound, shown next to it. -->
<script lang="ts">
	import { SplitPanel, type SplitOrientation, type SplitPane, type SplitSize }
		from '../src/index.js';

	let { position = $bindable(40), collapsed = $bindable(null), orientation = 'horizontal',
		resizable = true, collapsible = false, onresized }: { position?: SplitSize;
			collapsed?: SplitPane | null; orientation?: SplitOrientation; resizable?: boolean;
			collapsible?: boolean; onresized?: (position: SplitSize) => void } = $props();
</script>

<SplitPanel id="s" bind:position bind:collapsed {orientation} {resizable} {collapsible}
	{onresized} label="Resize">
	{#snippet first()}<p>One</p>{/snippet}
	{#snippet second()}<p>Two</p>{/snippet}
</SplitPanel>
<output>{position}</output>
<span class="collapsed">{collapsed}</span>
<button type="button" class="reset" onclick={() => (position = 20)}>reset</button>
<button type="button" class="restore" onclick={() => (collapsed = null)}>restore</button>
