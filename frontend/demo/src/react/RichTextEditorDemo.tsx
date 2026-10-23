/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { useState } from 'react';

import { Button, Dropdown, RichTextEditor, Tab, Tabs } from '@antilia/react';

import { dropdownTexts } from '../data.js';
import { describe, richText, toolbarOf, uploader, type RichText,
	type RichTextEditorConfig } from '../rich-text-editor.js';

/**
 * The RichTextEditor demo in React: the editor of the Wicket page, in its markup. The app keeps the
 * article and the notes, their texts and the state the user left their editors in. The HTML and
 * the Markdown the editors give show below them, as text: a server keeping the HTML cleans it.
 */
export function RichTextEditorDemo({ config }: { config: RichTextEditorConfig }) {
	const { texts } = config;
	const [article, setArticle] = useState<RichText>(() => richText(config.article));
	const [notes, setNotes] = useState<RichText>(() => richText(config.notes));
	const [toolbar, setToolbar] = useState(config.toolbars[0]);
	const [bubbleMenu, setBubbleMenu] = useState(true);
	const [readOnly, setReadOnly] = useState(false);
	const [count, setCount] = useState(true);
	const [highlight, setHighlight] = useState(true);
	const [onUpload] = useState(() => uploader(config));
	const [heard, setHeard] = useState(texts.appStart);
	const check = (label: string, checked: boolean, change: (checked: boolean) => void) => (
		<label className="rich-text-demo__control">
			<input type="checkbox" checked={checked}
				onChange={(event) => change(event.currentTarget.checked)} />
			{' '}{label}
		</label>
	);
	const saveArticle = () => {
		setArticle((all) => ({ ...all, saved: all.text }));
		setHeard(texts.appSaved);
	};
	const saveNotes = () => setNotes((all) => ({ ...all, saved: all.text }));

	return (
		<div className="rich-text-demo">
			<Tabs design="pills" label={texts.cases}>
				<Tab id="articleTitle" title={texts.appArticleTitle} modified={false}>
					<section>
						<div className="rich-text-demo__controls">
							<label className="rich-text-demo__control">
								{texts.toolbar}{' '}
								<Dropdown value={toolbar} options={config.toolbars.map((each) =>
									({ value: each, label: texts['toolbar.' + each] }))}
									onChange={(value) => setToolbar(value as string)} {...dropdownTexts(texts)} />
							</label>
							{check(texts.bubbleMenu, bubbleMenu, setBubbleMenu)}
							{check(texts.readOnly, readOnly, setReadOnly)}
							{check(texts.count, count, setCount)}
							{check(texts.highlight, highlight, setHighlight)}
						</div>
						<p className="rich-text-demo__status">{texts.extras}</p>
						<form className="rich-text-demo__editor" onSubmit={(event) => {
							event.preventDefault();
							saveArticle();
						}}>
							<span className="rich-text-demo__name">{texts.articleLabel}</span>
							<RichTextEditor value={article.text} saved={article.saved} state={article.state}
								label={texts.articleLabel} placeholder={texts.placeholder}
								toolbar={toolbarOf(toolbar)} bubbleMenu={bubbleMenu} readOnly={readOnly}
								count={count} texts={config.editor} highlight={highlight} onUpload={onUpload}
								uploadTypes={config.uploadTypes} uploadMaxSize={config.uploadMaxSize}
								onChange={(text) => setArticle((all) => ({ ...all, text }))}
								onStateChange={(state) => setArticle((all) => ({ ...all, state }))}
								onSave={saveArticle} />
							<div className="rich-text-demo__actions">
								<Button type="submit" label={texts.save} variant="primary" size="small" />
								<span className="rich-text-demo__heard" role="status">
									{heard}
								</span>
							</div>
						</form>
						<p className="rich-text-demo__status">{describe(texts, article)}</p>
						<div className="rich-text-demo__saved">
							<h3 className="rich-text-demo__subtitle">{texts.appHtml}</h3>
							<p className="rich-text-demo__status">{texts.appNotClean}</p>
							<pre><code>{article.text}</code></pre>
						</div>
					</section>
				</Tab>
				<Tab id="notesTitle" title={texts.notesTitle} modified={false}>
					<section>
						<form className="rich-text-demo__editor" onSubmit={(event) => {
							event.preventDefault();
							saveNotes();
						}}>
							<span className="rich-text-demo__name">{texts.notesLabel}</span>
							<RichTextEditor value={notes.text} saved={notes.saved} state={notes.state}
								format="markdown" label={texts.notesLabel} bubbleMenu texts={config.editor}
								highlight onUpload={onUpload} uploadTypes={config.uploadTypes}
								uploadMaxSize={config.uploadMaxSize}
								onChange={(text) => setNotes((all) => ({ ...all, text }))}
								onStateChange={(state) => setNotes((all) => ({ ...all, state }))}
								onSave={saveNotes} />
							<div className="rich-text-demo__actions">
								<Button type="submit" label={texts.save} variant="primary" size="small" />
								<span className="rich-text-demo__heard" role="status">
									{describe(texts, notes)}
								</span>
							</div>
						</form>
						<div className="rich-text-demo__saved">
							<h3 className="rich-text-demo__subtitle">{texts.appMarkdown}</h3>
							<pre><code>{notes.text}</code></pre>
						</div>
					</section>
				</Tab>
			</Tabs>
		</div>
	);
}
