<!--
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<!--
  The Scrollbar demo in Svelte: the boxes of the Wicket page, those with slim bars a Scrollable,
  the last one growing in the app and scrolled through the functions of its Scrollable.
-->
<script lang="ts">
	import { tick, untrack } from 'svelte';
	import { Scrollable } from '@antilia/svelte';

	import { line, LINE, lines, MORE_LINES, POSITION, TILES,
		type ScrollbarConfig } from '../scrollbar.js';

	let { config }: { config: ScrollbarConfig } = $props();

	const texts = $derived(config.texts);
	// the lines start as the page shows them, then are the app's
	let count = $state(untrack(() => config.lines));
	let growing = $state<ReturnType<typeof Scrollable>>();

	async function add() {
		const first = count + 1;
		count += MORE_LINES;
		await tick();
		growing?.scrollToElement(line(first), true);
	}
</script>

{#snippet numbered(pattern: string, count: number)}
	{#each lines(pattern, count) as text, index (index)}
		<p data-line={index + 1}>{text}</p>
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
		<Scrollable bind:this={growing} class="scrollbar-demo__box scrollbar-demo__box--short">
			{@render numbered(texts.line, count)}
		</Scrollable>
		<div class="scrollbar-demo__actions">
			<button type="button" class="scrollbar-demo__add" title={texts.addLines}
				aria-label={texts.addLines} onclick={add}>+</button>
			<button type="button" class="scrollbar-demo__to"
				onclick={() => growing?.scrollToTop(true)}>{texts.toTop}</button>
			<button type="button" class="scrollbar-demo__to"
				onclick={() => growing?.scrollToBottom(true)}>{texts.toBottom}</button>
			<button type="button" class="scrollbar-demo__to"
				onclick={() => growing?.scrollTo({ top: POSITION }, true)}>{texts.toPosition}</button>
			<button type="button" class="scrollbar-demo__to"
				onclick={() => growing?.scrollToElement(line(LINE), true)}>{texts.toLine}</button>
		</div>
	</section>
</div>
