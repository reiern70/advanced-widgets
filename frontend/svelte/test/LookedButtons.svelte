<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!-- Buttons in the look given by setLook. -->
<script lang="ts">
	import { Button, setLook } from '../src/index.js';

	let { look }: { look: string } = $props();

	// svelte-ignore state_referenced_locally
	setLook(look);
</script>

<Button label="Save" variant="primary" size="small" />
<Button label="Undo" variant="ghost" iconOnly />
