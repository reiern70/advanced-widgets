<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!-- A Dropdown whose value is bound, shown next to it. -->
<script lang="ts">
	import { Dropdown, type DropdownOption } from '../src/index.js';

	let { options, value = $bindable(), multiple = false }:
		{ options: DropdownOption[]; value?: string | string[]; multiple?: boolean } = $props();
</script>

<Dropdown {options} {multiple} bind:value />
<p>{JSON.stringify(value)}</p>
<button type="button" class="reset" onclick={() => (value = multiple ? [] : 'apple')}>reset</button>
