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
  The RichTextEditor demo in Svelte: the editor of the Wicket page, in its markup. The app keeps the
  article and the notes, their texts and the state the user left their editors in, bound. The HTML
  and the Markdown the editors give show below them, as text: a server keeping the HTML cleans it.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { Button, Dropdown, RichTextEditor, Tabs, type TabItem } from '@antilia/svelte';

	import { dropdownTexts } from '../data.js';
	import { describe, imagesTakenOut, proofreader as proofreaderOf, richText,
		suggesters as suggestersOf, toolbarOf, uploader, type RichText,
		type RichTextEditorConfig } from '../rich-text-editor.js';

	let { config }: { config: RichTextEditorConfig } = $props();

	const texts = $derived(config.texts);
	// the texts and the options start as the page has them, then are the app's
	const initial = untrack(() => config);
	let article = $state<RichText>(richText(initial.article));
	let notes = $state<RichText>(richText(initial.notes));
	let toolbar = $state(initial.toolbars[0]);
	let bubbleMenu = $state(true);
	let readOnly = $state(false);
	let count = $state(true);
	let highlight = $state(true);
	const onupload = uploader(initial);
	const articleImages = imagesTakenOut(initial);
	const notesImages = imagesTakenOut(initial);
	const limits = { uploadTypes: initial.uploadTypes, uploadMaxSize: initial.uploadMaxSize,
		uploadMaxWidth: initial.uploadMaxWidth, uploadMaxHeight: initial.uploadMaxHeight,
		uploadMaxCount: initial.uploadMaxCount, uploadScaling: true };
	const proofreader = proofreaderOf(initial);
	const suggesters = suggestersOf(initial);
	let heard = $state(initial.texts.appStart);

	// the cases, one at a time
	const cases = $derived<TabItem[]>([{ id: 'articleTitle', title: texts.appArticleTitle, modified: false },
		{ id: 'notesTitle', title: texts.notesTitle, modified: false }]);

	function saveArticle() {
		articleImages.saved(article.text);
		article.saved = article.text;
		heard = texts.appSaved;
	}

	function saveNotes() {
		notesImages.saved(notes.text);
		notes.saved = notes.text;
	}
</script>

<div class="rich-text-demo">
	<Tabs design="pills" label={texts.cases} tabs={cases}>
		{#snippet panel(tab)}
			{#if tab.id === 'articleTitle'}
				<section>
					<div class="rich-text-demo__controls">
						<label class="rich-text-demo__control">
							{texts.toolbar}
							<Dropdown value={toolbar} options={config.toolbars.map((each) =>
								({ value: each, label: texts['toolbar.' + each] }))}
								onchange={(value) => toolbar = value as string} {...dropdownTexts(texts)} />
						</label>
						<label class="rich-text-demo__control">
							<input type="checkbox" bind:checked={bubbleMenu} /> {texts.bubbleMenu}
						</label>
						<label class="rich-text-demo__control">
							<input type="checkbox" bind:checked={readOnly} /> {texts.readOnly}
						</label>
						<label class="rich-text-demo__control">
							<input type="checkbox" bind:checked={count} /> {texts.count}
						</label>
						<label class="rich-text-demo__control">
							<input type="checkbox" bind:checked={highlight} /> {texts.highlight}
						</label>
					</div>
					<p class="rich-text-demo__status">{texts.extras}</p>
					<form class="rich-text-demo__editor" onsubmit={(event) => {
						event.preventDefault();
						saveArticle();
					}}>
						<span class="rich-text-demo__name">{texts.articleLabel}</span>
						<RichTextEditor bind:value={article.text} bind:state={article.state}
							saved={article.saved} label={texts.articleLabel} placeholder={texts.placeholder}
							toolbar={toolbarOf(toolbar)} {bubbleMenu} {readOnly} {count} texts={config.editor}
							{highlight} {onupload} {...limits}
							onimagesremoved={(urls) => articleImages.add(urls)} {proofreader} {suggesters}
							onsave={saveArticle} />
						<div class="rich-text-demo__actions">
							<Button type="submit" label={texts.save} variant="primary" size="small" />
							<span class="rich-text-demo__heard" role="status">
								{heard}
							</span>
						</div>
					</form>
					<p class="rich-text-demo__status">{describe(texts, article)}</p>
					<div class="rich-text-demo__saved">
						<h3 class="rich-text-demo__subtitle">{texts.appHtml}</h3>
						<p class="rich-text-demo__status">{texts.appNotClean}</p>
						<pre><code>{article.text}</code></pre>
					</div>
				</section>
			{:else if tab.id === 'notesTitle'}
				<section>
					<form class="rich-text-demo__editor" onsubmit={(event) => {
						event.preventDefault();
						saveNotes();
					}}>
						<span class="rich-text-demo__name">{texts.notesLabel}</span>
						<RichTextEditor bind:value={notes.text} bind:state={notes.state} saved={notes.saved}
							format="markdown" label={texts.notesLabel} bubbleMenu texts={config.editor}
							highlight {onupload} {...limits}
							onimagesremoved={(urls) => notesImages.add(urls)} onsave={saveNotes} />
						<div class="rich-text-demo__actions">
							<Button type="submit" label={texts.save} variant="primary" size="small" />
							<span class="rich-text-demo__heard" role="status">
								{describe(texts, notes)}
							</span>
						</div>
					</form>
					<div class="rich-text-demo__saved">
						<h3 class="rich-text-demo__subtitle">{texts.appMarkdown}</h3>
						<pre><code>{notes.text}</code></pre>
					</div>
				</section>
			{/if}
		{/snippet}
	</Tabs>
</div>
