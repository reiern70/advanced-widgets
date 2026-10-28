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
  The CKEditor demo in Svelte: the editor of the Wicket page, in its markup, with the CKEditor the
  page loaded and its license key. The app keeps the article and the notes, their texts and the
  state the user left their editors in, bound. The HTML and the Markdown the editors give show below
  them, as text: a server keeping the HTML cleans it.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { Button, CKEditor, Tabs, type TabItem } from '@antilia/svelte';

	import { ckText, describe, type CKEditorConfig, type CKText } from '../ck-editor.js';

	let { config }: { config: CKEditorConfig } = $props();

	const texts = $derived(config.texts);
	// the texts and the options start as the page has them, then are the app's
	const initial = untrack(() => config);
	let article = $state<CKText>(ckText(initial.article));
	let notes = $state<CKText>(ckText(initial.notes));
	let readOnly = $state(false);
	let count = $state(true);
	let heard = $state(initial.texts.appStart);

	// the cases, one at a time
	const cases = $derived<TabItem[]>([{ id: 'articleTitle', title: texts.appArticleTitle, modified: false },
		{ id: 'notesTitle', title: texts.notesTitle, modified: false }]);

	function saveArticle() {
		article.saved = article.text;
		heard = texts.appSaved;
	}

	function saveNotes() {
		notes.saved = notes.text;
	}
</script>

<div class="rich-text-demo">
	<p class="ckeditor-demo__license">{texts.appLicense}</p>
	<Tabs design="pills" label={texts.cases} tabs={cases}>
		{#snippet panel(tab)}
			{#if tab.id === 'articleTitle'}
				<section>
					<div class="rich-text-demo__controls">
						<label class="rich-text-demo__control">
							<input type="checkbox" bind:checked={readOnly} /> {texts.readOnly}
						</label>
						<label class="rich-text-demo__control">
							<input type="checkbox" bind:checked={count} /> {texts.count}
						</label>
					</div>
					<form class="rich-text-demo__editor" onsubmit={(event) => {
						event.preventDefault();
						saveArticle();
					}}>
						<span class="rich-text-demo__name">{texts.articleLabel}</span>
						<CKEditor licenseKey={config.licenseKey} language={config.language}
							bind:value={article.text} bind:state={article.state} saved={article.saved}
							label={texts.articleLabel} placeholder={texts.placeholder} {readOnly} {count}
							texts={config.editor} onsave={saveArticle} />
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
						<CKEditor licenseKey={config.licenseKey} language={config.language}
							bind:value={notes.text} bind:state={notes.state} saved={notes.saved}
							format="markdown" label={texts.notesLabel} texts={config.editor}
							onsave={saveNotes} />
						<div class="rich-text-demo__actions">
							<Button type="submit" label={texts.save} variant="primary" size="small" />
							<span class="rich-text-demo__heard" role="status">
								{describe(texts, notes)}
							</span>
						</div>
					</form>
					<div class="rich-text-demo__saved">
						<h3 class="rich-text-demo__subtitle">{texts.markdownKept}</h3>
						<pre><code>{notes.text}</code></pre>
					</div>
				</section>
			{/if}
		{/snippet}
	</Tabs>
</div>
