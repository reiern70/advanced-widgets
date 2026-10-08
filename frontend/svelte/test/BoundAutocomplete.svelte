<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!-- An Autocomplete whose values are bound, shown next to it. -->
<script lang="ts">
	import { Autocomplete, type AutocompleteSuggestion, type AutocompleteValue } from '../src/index.js';

	let { search, value = $bindable([]), multiple = false }: {
		search: (term: string) => Promise<AutocompleteSuggestion[]>;
		value?: AutocompleteValue[];
		multiple?: boolean;
	} = $props();
</script>

<Autocomplete {search} {multiple} bind:value template={'<b>{{name}}</b>'} name="fruits" />
<p>{value.map((chosen) => chosen.id).join(',')}</p>
<button type="button" class="reset" onclick={() => (value = [{ id: 'x', label: 'X' }])}>reset</button>
