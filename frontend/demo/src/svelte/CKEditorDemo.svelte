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
  them, as text: a server keeping the HTML cleans it. The images and the files of the article and
  of the notes go to the resource of the uploads of the application, as those of the Wicket editor,
  with the same limits: those taken out are deleted once the app saved the text.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { Button, CKEditor, Tabs, type TabItem } from '@antilia/svelte';

	import { ckText, describe, type CKEditorConfig, type CKText } from '../ck-editor.js';
	import { attachments, filesTakenOut, imagesTakenOut, storedImages,
		uploader } from '../rich-text-editor.js';

	let { config }: { config: CKEditorConfig } = $props();

	const texts = $derived(config.texts);
	// the texts and the options start as the page has them, then are the app's
	const initial = untrack(() => config);
	let article = $state<CKText>(ckText(initial.article));
	let notes = $state<CKText>(ckText(initial.notes));
	let readOnly = $state(false);
	let count = $state(true);
	let uploadDialog = $state(false);
	let heard = $state(initial.texts.appStart);
	// the images and the files go to the resource of the uploads of the application
	const onupload = uploader(initial);
	const stored = storedImages(initial);
	const attached = attachments(initial);
	const articleImages = imagesTakenOut(initial);
	const articleFiles = filesTakenOut(initial);
	const notesImages = imagesTakenOut(initial);
	const notesFiles = filesTakenOut(initial);

	// the cases, one at a time
	const cases = $derived<TabItem[]>([{ id: 'articleTitle', title: texts.appArticleTitle, modified: false },
		{ id: 'notesTitle', title: texts.notesTitle, modified: false }]);

	function saveArticle() {
		articleImages.saved(article.text);
		articleFiles.saved(article.text);
		article.saved = article.text;
		heard = texts.appSaved;
	}

	function saveNotes() {
		notesImages.saved(notes.text);
		notesFiles.saved(notes.text);
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
						<label class="rich-text-demo__control">
							<input type="checkbox" bind:checked={uploadDialog} /> {texts.uploadDialog}
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
							texts={config.editor} onsave={saveArticle} {onupload}
							uploadTypes={config.uploadTypes} uploadMaxSize={config.uploadMaxSize}
							uploadMaxWidth={config.uploadMaxWidth} uploadMaxHeight={config.uploadMaxHeight}
							uploadMaxCount={config.uploadMaxCount} uploadScaling {uploadDialog}
							onstorepermanently={stored.keep} onfindimages={stored.find}
							onattach={attached.onAttach} attachTypes={attached.attachTypes}
							attachMaxSize={attached.attachMaxSize} attachMaxCount={attached.attachMaxCount}
							onimagesremoved={(urls) => articleImages.add(urls)}
							onattachmentsremoved={(urls) => articleFiles.add(urls)} />
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
							onsave={saveNotes} {onupload} uploadTypes={config.uploadTypes}
							uploadMaxSize={config.uploadMaxSize} uploadMaxWidth={config.uploadMaxWidth}
							uploadMaxHeight={config.uploadMaxHeight} onattach={attached.onAttach}
							attachTypes={attached.attachTypes} attachMaxSize={attached.attachMaxSize}
							attachMaxCount={attached.attachMaxCount}
							onimagesremoved={(urls) => notesImages.add(urls)}
							onattachmentsremoved={(urls) => notesFiles.add(urls)} />
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
