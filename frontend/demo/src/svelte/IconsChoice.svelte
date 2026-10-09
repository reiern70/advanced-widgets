<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  The drop-down choosing the icons of the trees, as that of the Wicket page does.
-->
<script lang="ts">
	import { Dropdown } from '@antilia/svelte';

	import { iconSetOptions, type IconSet, type TreeDemoTexts } from '../tree.js';

	let { id, value, texts, onchange }: { id: string; value: IconSet; texts: TreeDemoTexts;
		onchange: (value: IconSet) => void } = $props();
</script>

<label for={id}>{texts.icons}</label>
<Dropdown {id} name="icons" {value} options={iconSetOptions(texts)}
	onchange={(chosen) => onchange(String(chosen) as IconSet)}
	filter={texts.filter} noMatch={texts.noMatch} more={texts.more} remove={texts.remove} />
