<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!-- A DatePicker, or DateTimePicker, whose value is bound, shown next to it. -->
<script lang="ts">
	import { DatePicker, DateTimePicker } from '../src/index.js';

	let { value = $bindable(''), time = false, locale = 'de-DE' }:
		{ value?: string; time?: boolean; locale?: string } = $props();
</script>

{#if time}
	<DateTimePicker {locale} bind:value />
{:else}
	<DatePicker {locale} bind:value />
{/if}
<p>{value}</p>
<button type="button" class="reset" onclick={() => (value = '2026-01-01')}>reset</button>
