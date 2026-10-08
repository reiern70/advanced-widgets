<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  The Scrollbar demo in Svelte: the boxes of the Wicket page, those with slim bars a Scrollable,
  the last one growing in the app.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { Scrollable } from '@advanced-widgets/svelte';

	import { lines, MORE_LINES, TILES, type ScrollbarConfig } from '../scrollbar.js';

	let { config }: { config: ScrollbarConfig } = $props();

	const texts = $derived(config.texts);
	// the lines start as the page shows them, then are the app's
	let count = $state(untrack(() => config.lines));

	function add(event: MouseEvent) {
		event.preventDefault();
		count += MORE_LINES;
	}
</script>

{#snippet numbered(pattern: string, count: number)}
	{#each lines(pattern, count) as line, index (index)}
		<p>{line}</p>
	{/each}
{/snippet}

<div class="scrollbar-demo">
	<section>
		<h3>{texts.native}</h3>
		<div class="scrollbar-demo__box">{@render numbered(texts['line.long'], 40)}</div>
	</section>
	<section>
		<h3>{texts.vertical}</h3>
		<Scrollable class="scrollbar-demo__box">{@render numbered(texts['line.long'], 40)}</Scrollable>
	</section>
	<section>
		<h3>{texts.horizontal}</h3>
		<Scrollable class="scrollbar-demo__box scrollbar-demo__box--x" axis="x">
			<div class="scrollbar-demo__strip">
				{#each TILES as tile (tile)}
					<span>{tile}</span>
				{/each}
			</div>
		</Scrollable>
	</section>
	<section>
		<h3>{texts.both}</h3>
		<Scrollable class="scrollbar-demo__box scrollbar-demo__box--both">
			{@render numbered(texts['line.long'], 60)}
		</Scrollable>
	</section>
	<section>
		<h3>{texts.growing}</h3>
		<Scrollable class="scrollbar-demo__box scrollbar-demo__box--short">
			{@render numbered(texts.line, count)}
		</Scrollable>
		<p><a href="#top" onclick={add}>{texts.addLines}</a></p>
	</section>
</div>
