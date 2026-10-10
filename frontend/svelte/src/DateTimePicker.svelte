<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  @component
  A DatePicker taking a time too: its value, which binds both ways, is ISO text such as
  2026-10-09T14:30.
-->
<script lang="ts">
	import type { ComponentProps } from 'svelte';
	import DatePicker from './DatePicker.svelte';

	let { value = $bindable(''), ...rest }: Omit<ComponentProps<typeof DatePicker>, 'time'> =
		$props();
</script>

<DatePicker {...rest} time bind:value />
