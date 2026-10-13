<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!-- A popover button with a counter as its content, for the tests. -->
<script lang="ts">
	import type { Popover, PopoverOptions } from '@antilia/core';

	import { PopoverButton } from '../src/index.js';

	let { popover = {}, rich = true, shown = true, onopen }: { popover?: PopoverOptions;
		rich?: boolean; shown?: boolean; onopen?: (popover: Popover) => void } = $props();

	let count = $state(0);
</script>

{#snippet counter()}
	<button type="button" class="count" onclick={() => count++}>{count}</button>
{/snippet}

<p id="before">before</p>
{#if shown}
	<PopoverButton class="mine" aria-label="More" options={popover} {onopen}
		content={rich ? counter : undefined}>i</PopoverButton>
{/if}
<p id="after">after</p>
