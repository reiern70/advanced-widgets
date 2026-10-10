<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!-- A Palette whose value is bound, shown next to it. -->
<script lang="ts">
	import { Palette, type PaletteChoice } from '../src/index.js';

	let { options, value = $bindable([]), ordered = false }:
		{ options: PaletteChoice[]; value?: string[]; ordered?: boolean } = $props();
</script>

<Palette {options} {ordered} name="fruits" bind:value />
<p>{JSON.stringify(value)}</p>
<button type="button" class="reset" onclick={() => (value = ['cherry'])}>reset</button>
