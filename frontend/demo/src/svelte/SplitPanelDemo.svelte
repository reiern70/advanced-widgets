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
  The SplitPanel demo in Svelte: the split panels of the Wicket page, in its markup, the sentence
  below the layout following its dividers and its collapsed panes in the browser. Only the body of
  a pane scrolls, below its title, with the scroll bars of Scrollable.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { Scrollable, SplitPanel, type SplitPane, type SplitSize } from '@antilia/svelte';

	import { describe, type SplitPanelConfig } from '../split-panel.js';

	let { config }: { config: SplitPanelConfig } = $props();

	const texts = $derived(config.texts);
	const layout = $derived(config.layout);
	// the dividers and the check boxes start as the page has them, then are the app's
	const initial = untrack(() => config);
	let files = $state<SplitSize>(initial.layout.files.position);
	let filesCollapsed = $state<SplitPane | null>(initial.layout.files.collapsed ?? null);
	let editor = $state<SplitSize>(initial.layout.editor.position);
	let editorCollapsed = $state<SplitPane | null>(initial.layout.editor.collapsed ?? null);
	let vertical = $state(initial.vertical);
	let resizable = $state(initial.resizable);
	let collapsible = $state(initial.collapsible);
</script>

<div class="split-demo">
	<section>
		<h3>{texts.layoutTitle}</h3>
		<div class="split-demo__layout">
			<SplitPanel bind:position={files} bind:collapsed={filesCollapsed} min={layout.files.min}
				max={layout.files.max} collapsible label={texts.divider}
				collapseFirstLabel={texts.collapseLeft} collapseSecondLabel={texts.collapseRight}
				restoreLabel={texts.restore}>
				{#snippet first()}
					<div class="split-demo__pane">
						<div class="split-demo__title">{texts.files}</div>
						<Scrollable class="split-demo__body">
							<ul class="split-demo__files">
								{#each config.files as file (file)}<li>{file}</li>{/each}
							</ul>
						</Scrollable>
					</div>
				{/snippet}
				{#snippet second()}
					<SplitPanel orientation="vertical" bind:position={editor}
						bind:collapsed={editorCollapsed} min={layout.editor.min}
						minSecond={layout.editor.minSecond} collapsible label={texts.divider}
						collapseFirstLabel={texts.collapseTop}
						collapseSecondLabel={texts.collapseBottom} restoreLabel={texts.restore}>
						{#snippet first()}
							<div class="split-demo__pane">
								<div class="split-demo__title">{texts.editor}</div>
								<Scrollable class="split-demo__body">
									<pre class="split-demo__code">{config.code}</pre>
								</Scrollable>
							</div>
						{/snippet}
						{#snippet second()}
							<div class="split-demo__pane">
								<div class="split-demo__title">{texts.console}</div>
								<Scrollable class="split-demo__body">
									<pre class="split-demo__console">{config.console}</pre>
								</Scrollable>
							</div>
						{/snippet}
					</SplitPanel>
				{/snippet}
			</SplitPanel>
		</div>
		<p class="split-demo__sizes">{describe(config, { position: files, collapsed: filesCollapsed },
			{ position: editor, collapsed: editorCollapsed })}</p>
	</section>
	<section>
		<h3>{texts.settingsTitle}</h3>
		<div class="split-demo__checks">
			<label class="split-demo__check">
				<input type="checkbox" bind:checked={vertical} />
				{texts.vertical}
			</label>
			<label class="split-demo__check">
				<input type="checkbox" bind:checked={resizable} />
				{texts.resizable}
			</label>
			<label class="split-demo__check">
				<input type="checkbox" bind:checked={collapsible} />
				{texts.collapsible}
			</label>
		</div>
		<div class="split-demo__settings">
			<SplitPanel orientation={vertical ? 'vertical' : 'horizontal'} {resizable} {collapsible}
				label={texts.divider}
				collapseFirstLabel={vertical ? texts.collapseTop : texts.collapseLeft}
				collapseSecondLabel={vertical ? texts.collapseBottom : texts.collapseRight}
				restoreLabel={texts.restore}>
				{#snippet first()}
					<div class="split-demo__pane">
						<Scrollable class="split-demo__body">
							<p class="split-demo__text">{texts.first}</p>
						</Scrollable>
					</div>
				{/snippet}
				{#snippet second()}
					<div class="split-demo__pane">
						<Scrollable class="split-demo__body">
							<p class="split-demo__text">{texts.second}</p>
						</Scrollable>
					</div>
				{/snippet}
			</SplitPanel>
		</div>
	</section>
</div>
