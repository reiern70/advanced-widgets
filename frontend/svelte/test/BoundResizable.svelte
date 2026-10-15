<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!-- A Resizable whose size is bound, shown next to it. -->
<script lang="ts">
	import { Resizable, type ResizeAxis } from '../src/index.js';

	let { width = $bindable(null), height = $bindable(120), axis = 'both', enabled = true,
		className = 'notes', onresized }: { width?: number | null; height?: number | null;
			axis?: ResizeAxis; enabled?: boolean; className?: string;
			onresized?: (size: unknown) => void } = $props();
</script>

<Resizable id="r" class={className} bind:width bind:height {axis} {enabled} {onresized}
	label="Resize the notes">
	<p>Text</p>
</Resizable>
<output>{width} × {height}</output>
<button type="button" class="reset" onclick={() => (height = 300)}>reset</button>
