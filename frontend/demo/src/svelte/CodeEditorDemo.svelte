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
  The CodeEditor demo in Svelte: the editor of the Wicket page, in its markup. The app keeps each
  sample, its text and the state the user left its editor in, bound, and gives the editor of a
  language the state again when the language is shown anew. The notes in Markdown show beside their
  preview in a split panel, in a box the user makes taller; a button hides the preview and shows it
  again, and a check box keeps it in step with the editor. The files open in tabs, which their
  editors tell of their changes not saved.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { Button, CodeEditor, Dropdown, MarkdownPreview, Resizable, SplitPanel, Tabs,
		type SplitPane, type SplitSize, type TabItem } from '@antilia/svelte';

	import { axisOf, collapsedText, describe, filesOf, notesOf, registerLogGrammar, samplesOf,
		type CodeEditorConfig, type Sample } from '../code-editor.js';
	import { dropdownTexts, format } from '../data.js';

	registerLogGrammar();

	let { config }: { config: CodeEditorConfig } = $props();

	const texts = $derived(config.texts);
	// the samples, the files and the options start as the page has them, then are the app's
	const initial = untrack(() => config);
	let samples = $state<Record<string, Sample>>(samplesOf(initial));
	let language = $state(initial.languages[0].name);
	let resize = $state(initial.resizes[0]);
	let lineNumbers = $state(true);
	let readOnly = $state(false);
	let selectable = $state(true);
	let lineWrapping = $state(false);
	let heard = $state(initial.texts.start);
	let notes = $state<Sample>(notesOf(initial));
	let notesMessage = $state(initial.texts.notesStart);
	let position = $state<SplitSize>(50);
	let collapsed = $state<SplitPane | null>(null);
	let notesHeight = $state<number | null>(null);
	let inStep = $state(false);
	let files = $state<Sample[]>(filesOf(initial));
	let filesMessage = $state(initial.texts.filesStart);

	const sample = $derived(samples[language]);
	// the cases, one at a time
	const cases = $derived<TabItem[]>(['editorTitle', 'notesTitle', 'filesTitle']
		.map((id) => ({ id, title: texts[id], modified: false })));
	const fileTabs = $derived<TabItem[]>(files.map((file) => ({ id: file.id, title: file.file,
		closable: true })));

	function save() {
		sample.saved = sample.text;
		heard = format(texts.heardSave, sample.file);
	}

	function saveNotes() {
		notes.saved = notes.text;
		notesMessage = format(texts.saved, notes.file);
	}

	/** Hides the preview, giving the editor all the room, or shows it again. */
	function togglePreview() {
		collapsed = collapsed === 'second' ? null : 'second';
		notesMessage = collapsedText(texts, collapsed);
	}

	function saveFile(file: Sample) {
		file.saved = file.text;
		filesMessage = format(texts.saved, file.file);
	}

	function close(id: string) {
		const closed = files.find((file) => file.id === id);
		files = files.filter((file) => file.id !== id);
		if (closed) {
			filesMessage = format(texts.closed, closed.file);
		}
	}
</script>

<div class="code-editor-demo">
	<Tabs design="pills" label={texts.cases} tabs={cases}>
		{#snippet panel(tab)}
			{#if tab.id === 'editorTitle'}
				<section>
					<div class="code-editor-demo__controls">
						<label class="code-editor-demo__control">
							{texts.language}
							<Dropdown value={language} options={config.languages.map((each) =>
								({ value: each.name, label: texts['language.' + each.name] }))}
								onchange={(value) => language = value as string} {...dropdownTexts(texts)} />
						</label>
						<label class="code-editor-demo__control">
							{texts.resize}
							<Dropdown value={resize} options={config.resizes.map((each) =>
								({ value: each, label: texts['resize.' + each] }))}
								onchange={(value) => resize = value as string} {...dropdownTexts(texts)} />
						</label>
						<label class="code-editor-demo__control">
							<input type="checkbox" bind:checked={lineNumbers} /> {texts.lineNumbers}
						</label>
						<label class="code-editor-demo__control">
							<input type="checkbox" bind:checked={readOnly} /> {texts.readOnly}
						</label>
						<label class="code-editor-demo__control">
							<input type="checkbox" bind:checked={selectable} /> {texts.selectable}
						</label>
						<label class="code-editor-demo__control">
							<input type="checkbox" bind:checked={lineWrapping} /> {texts.lineWrapping}
						</label>
					</div>
					<form class="code-editor-demo__editor" onsubmit={(event) => {
						event.preventDefault();
						save();
					}}>
						<span class="code-editor-demo__name">{sample.file}</span>
						<!-- a language shown anew starts where the user left it -->
						{#key language}
							<CodeEditor bind:value={sample.text} bind:state={sample.state} saved={sample.saved}
								{language} label={sample.file} {lineNumbers} {readOnly} {selectable}
								{lineWrapping} resizable={axisOf(resize)} texts={config.editor}
								onfocus={() => heard = format(texts.heardFocus, sample.file)}
								onblur={() => heard = format(texts.heardBlur, sample.file)}
								onsave={save} />
						{/key}
						<div class="code-editor-demo__actions">
							<Button type="submit" label={texts.save} variant="primary" size="small" />
							<span class="code-editor-demo__heard" role="status">{heard}</span>
						</div>
					</form>
					<p class="code-editor-demo__status">{describe(config, sample)}</p>
				</section>
			{:else if tab.id === 'notesTitle'}
				<section>
					<form class="code-editor-demo__notes" onsubmit={(event) => {
						event.preventDefault();
						saveNotes();
					}}>
						<div class="code-editor-demo__actions">
							<span class="code-editor-demo__name">{notes.file}</span>
							<Button type="submit" label={texts.save} variant="primary" size="small" />
							<Button label={collapsed === 'second' ? texts.showPreview : texts.hidePreview}
								variant="secondary" size="small" aria-controls="svelte-notes-preview"
								onclick={togglePreview} />
							<label class="code-editor-demo__control">
								<input type="checkbox" bind:checked={inStep} /> {texts.inStep}
							</label>
						</div>
						<Resizable class="code-editor-demo__split" axis="vertical" minHeight={160}
							label={config.handle} bind:height={notesHeight}>
							<SplitPanel collapsible bind:collapsed bind:position min={20} minSecond={20}
								label={config.split.divider} collapseFirstLabel={config.split.collapseLeft}
								collapseSecondLabel={config.split.collapseRight}
								restoreLabel={config.split.restore}
								oncollapse={(next) => notesMessage = collapsedText(texts, next)}>
								{#snippet first()}
									<CodeEditor id="svelte-notes-editor" bind:value={notes.text}
										bind:state={notes.state} saved={notes.saved} language="markdown"
										label={notes.file} lineWrapping resizable={false}
										texts={config.editor} onsave={saveNotes} />
								{/snippet}
								{#snippet second()}
									<MarkdownPreview id="svelte-notes-preview" value={notes.text}
										editor="svelte-notes-editor" scrollWithEditor={inStep}
										label={config.preview.label} placeholder={config.preview.placeholder} />
								{/snippet}
							</SplitPanel>
						</Resizable>
					</form>
					<p class="code-editor-demo__status" role="status">{notesMessage}</p>
				</section>
			{:else if tab.id === 'filesTitle'}
				<section>
					<Tabs class="code-editor-demo__files" label={texts.filesLabel} texts={config.tabs}
						tabs={fileTabs} onclose={close}>
						{#snippet icon()}
							<span class="code-editor-demo__icon">{@html config.icons.file}</span>
						{/snippet}
						{#snippet panel(fileTab)}
							{@const file = files.find((each) => each.id === fileTab.id)}
							{#if file}
								<form class="code-editor-demo__file" onsubmit={(event) => {
									event.preventDefault();
									saveFile(file);
								}}>
									<CodeEditor bind:value={file.text} saved={file.saved} language={file.language}
										label={file.file} texts={config.editor} onsave={() => saveFile(file)} />
									<div><Button type="submit" label={texts.save} variant="primary" size="small" /></div>
								</form>
							{/if}
						{/snippet}
					</Tabs>
					<p class="code-editor-demo__status" role="status">{filesMessage}</p>
				</section>
			{/if}
		{/snippet}
	</Tabs>
</div>
