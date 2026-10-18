<!--
  Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)

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
  The Resizable demo in Svelte: the resizable boxes of the Wicket page, in its markup. Their sizes
  are bound and kept by the app, which gives the card the size it had when the button puts it back.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { Button, Resizable } from '@antilia/svelte';

	import { describe, type ResizableConfig } from '../resizable.js';

	let { config }: { config: ResizableConfig } = $props();

	const texts = $derived(config.texts);
	const initial = untrack(() => config);
	let note = $state<number | null>(null);
	let side = $state<number | null>(null);
	let cardWidth = $state<number | null>(initial.card.width ?? null);
	let cardHeight = $state<number | null>(initial.card.height ?? null);
	let cardShown = $state(true);
</script>

<div class="resizable-demo">
	<section>
		<h3>{texts.noteTitle}</h3>
		<Resizable class="resizable-demo__note" axis="vertical" bind:height={note} minHeight={80}
			maxHeight={480} label={config.handle}>
			<p>{texts.noteText}</p>
		</Resizable>
	</section>
	<section>
		<h3>{texts.sideTitle}</h3>
		<div class="resizable-demo__layout">
			<Resizable class="resizable-demo__side" axis="horizontal" bind:width={side} minWidth={140}
				maxWidth={520} label={config.handle}>
				<ul>
					{#each config.items as item (item)}<li>{texts[item]}</li>{/each}
				</ul>
			</Resizable>
			<div class="resizable-demo__main"></div>
		</div>
	</section>
	<section>
		<h3>{texts.cardTitle}</h3>
		<div class="resizable-demo__heading">
			<Button label={cardShown ? texts.takeCardAway : texts.putCardBack} variant="secondary"
				size="small" onclick={() => cardShown = !cardShown} />
		</div>
		<div class="resizable-demo__place">
			{#if cardShown}
				<Resizable class="resizable-demo__card" bind:width={cardWidth} bind:height={cardHeight}
					minWidth={180} minHeight={100} label={config.handle}>
					<p>{texts.cardText}</p>
				</Resizable>
			{/if}
		</div>
	</section>
	<p class="resizable-demo__sizes" role="status">{describe(config, note, side,
		cardShown ? { width: cardWidth, height: cardHeight } : null)}</p>
</div>
