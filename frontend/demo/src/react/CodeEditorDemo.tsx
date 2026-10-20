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

import { Button, CodeEditor, Dropdown, MarkdownPreview, Resizable, SplitPanel, Tab, Tabs,
	type SplitPane, type SplitSize } from '@antilia/react';

import { axisOf, collapsedText, describe, filesOf, notesOf, registerLogGrammar, samplesOf,
	type CodeEditorConfig, type Sample } from '../code-editor.js';
import { dropdownTexts, format } from '../data.js';
import { Icon } from './DialogOptionsDemo.js';

registerLogGrammar();

/**
 * The CodeEditor demo in React: the editor of the Wicket page, in its markup. The app keeps each
 * sample, its text and the state the user left its editor in, and gives the editor of a language
 * the state again when the language is shown anew. The notes in Markdown show beside their
 * preview in a split panel, in a box the user makes taller; a button hides the preview and shows it
 * again, and a check box keeps it in step with the editor. The files open in tabs, which their
 * editors tell of their changes not saved.
 */
export function CodeEditorDemo({ config }: { config: CodeEditorConfig }) {
	const { texts } = config;
	const [samples, setSamples] = useState<Record<string, Sample>>(() => samplesOf(config));
	const [language, setLanguage] = useState(config.languages[0].name);
	const [resize, setResize] = useState(config.resizes[0]);
	const [lineNumbers, setLineNumbers] = useState(true);
	const [readOnly, setReadOnly] = useState(false);
	const [selectable, setSelectable] = useState(true);
	const [lineWrapping, setLineWrapping] = useState(false);
	const [heard, setHeard] = useState(texts.start);
	const [notes, setNotes] = useState<Sample>(() => notesOf(config));
	const [notesMessage, setNotesMessage] = useState(texts.notesStart);
	const [position, setPosition] = useState<SplitSize>(50);
	const [collapsed, setCollapsed] = useState<SplitPane | null>(null);
	const [notesHeight, setNotesHeight] = useState<number | null>(null);
	const [inStep, setInStep] = useState(false);
	const [files, setFiles] = useState<Sample[]>(() => filesOf(config));
	const [filesMessage, setFilesMessage] = useState(texts.filesStart);
	const sample = samples[language];

	const update = (change: Partial<Sample>) => setSamples((all) => ({ ...all,
		[language]: { ...all[language], ...change } }));
	const save = () => {
		setSamples((all) => ({ ...all, [language]: { ...all[language], saved: all[language].text } }));
		setHeard(format(texts.heardSave, sample.file));
	};
	const updateNotes = (change: Partial<Sample>) => setNotes((all) => ({ ...all, ...change }));
	const saveNotes = () => {
		setNotes((all) => ({ ...all, saved: all.text }));
		setNotesMessage(format(texts.saved, notes.file));
	};
	// hides the preview, giving the editor all the room, or shows it again
	const togglePreview = () => {
		const next = collapsed === 'second' ? null : 'second';
		setCollapsed(next);
		setNotesMessage(collapsedText(texts, next));
	};
	const updateFile = (id: string, change: Partial<Sample>) => setFiles((all) =>
		all.map((file) => file.id === id ? { ...file, ...change } : file));
	const saveFile = (file: Sample) => {
		setFiles((all) => all.map((each) => each.id === file.id ? { ...each, saved: each.text } :
			each));
		setFilesMessage(format(texts.saved, file.file));
	};
	const check = (label: string, checked: boolean, change: (checked: boolean) => void) => (
		<label className="code-editor-demo__control">
			<input type="checkbox" checked={checked}
				onChange={(event) => change(event.currentTarget.checked)} />
			{' '}{label}
		</label>
	);

	return (
		<div className="code-editor-demo">
			<Tabs design="pills" label={texts.cases}>
				<Tab id="editorTitle" title={texts.editorTitle} modified={false}>
					<section>
						<div className="code-editor-demo__controls">
							<label className="code-editor-demo__control">
								{texts.language}{' '}
								<Dropdown value={language} options={config.languages.map((each) =>
									({ value: each.name, label: texts['language.' + each.name] }))}
									onChange={(value) => setLanguage(value as string)} {...dropdownTexts(texts)} />
							</label>
							<label className="code-editor-demo__control">
								{texts.resize}{' '}
								<Dropdown value={resize} options={config.resizes.map((each) =>
									({ value: each, label: texts['resize.' + each] }))}
									onChange={(value) => setResize(value as string)} {...dropdownTexts(texts)} />
							</label>
							{check(texts.lineNumbers, lineNumbers, setLineNumbers)}
							{check(texts.readOnly, readOnly, setReadOnly)}
							{check(texts.selectable, selectable, setSelectable)}
							{check(texts.lineWrapping, lineWrapping, setLineWrapping)}
						</div>
						<form className="code-editor-demo__editor" onSubmit={(event) => {
							event.preventDefault();
							save();
						}}>
							<span className="code-editor-demo__name">{sample.file}</span>
							{/* a language shown anew starts where the user left it */}
							<CodeEditor key={language} value={sample.text} saved={sample.saved}
								language={language} state={sample.state} label={sample.file}
								lineNumbers={lineNumbers} readOnly={readOnly} selectable={selectable}
								lineWrapping={lineWrapping} resizable={axisOf(resize)} texts={config.editor}
								onChange={(text) => update({ text })}
								onStateChange={(state) => update({ state })}
								onFocus={() => setHeard(format(texts.heardFocus, sample.file))}
								onBlur={() => setHeard(format(texts.heardBlur, sample.file))}
								onSave={save} />
							<div className="code-editor-demo__actions">
								<Button type="submit" label={texts.save} variant="primary" size="small" />
								<span className="code-editor-demo__heard" role="status">{heard}</span>
							</div>
						</form>
						<p className="code-editor-demo__status">{describe(config, sample)}</p>
					</section>
				</Tab>
				<Tab id="notesTitle" title={texts.notesTitle} modified={false}>
					<section>
						<form className="code-editor-demo__notes" onSubmit={(event) => {
							event.preventDefault();
							saveNotes();
						}}>
							<div className="code-editor-demo__actions">
								<span className="code-editor-demo__name">{notes.file}</span>
								<Button type="submit" label={texts.save} variant="primary" size="small" />
								<Button label={collapsed === 'second' ? texts.showPreview : texts.hidePreview}
									variant="secondary" size="small" aria-controls="react-notes-preview"
									onClick={togglePreview} />
								{check(texts.inStep, inStep, setInStep)}
							</div>
							<Resizable className="code-editor-demo__split" axis="vertical" minHeight={160}
								label={config.handle} height={notesHeight}
								onResized={(size) => setNotesHeight(size.height)}>
								<SplitPanel collapsible collapsed={collapsed} position={position} min={20}
									minSecond={20} label={config.split.divider}
									collapseFirstLabel={config.split.collapseLeft}
									collapseSecondLabel={config.split.collapseRight}
									restoreLabel={config.split.restore} onResized={setPosition}
									onCollapse={(next) => {
										setCollapsed(next);
										setNotesMessage(collapsedText(texts, next));
									}}>
									<CodeEditor id="react-notes-editor" value={notes.text} saved={notes.saved}
										state={notes.state} language="markdown" label={notes.file} lineWrapping
										resizable={false}
										texts={config.editor} onChange={(text) => updateNotes({ text })}
										onStateChange={(state) => updateNotes({ state })} onSave={saveNotes} />
									<MarkdownPreview id="react-notes-preview" value={notes.text}
										editor="react-notes-editor" scrollWithEditor={inStep}
										label={config.preview.label} placeholder={config.preview.placeholder} />
								</SplitPanel>
							</Resizable>
						</form>
						<p className="code-editor-demo__status" role="status">{notesMessage}</p>
					</section>
				</Tab>
				<Tab id="filesTitle" title={texts.filesTitle} modified={false}>
					<section>
						<Tabs className="code-editor-demo__files" label={texts.filesLabel} texts={config.tabs}
							onClose={(id) => {
								const closed = files.find((file) => file.id === id);
								setFiles(files.filter((file) => file.id !== id));
								if (closed) {
									setFilesMessage(format(texts.closed, closed.file));
								}
							}}>
							{files.map((file) => (
								<Tab key={file.id} id={file.id} title={file.file} closable
									icon={<Icon markup={config.icons.file} />}>
									<form className="code-editor-demo__file" onSubmit={(event) => {
										event.preventDefault();
										saveFile(file);
									}}>
										<CodeEditor value={file.text} saved={file.saved} language={file.language}
											label={file.file} texts={config.editor}
											onChange={(text) => updateFile(file.id, { text })}
											onSave={() => saveFile(file)} />
										<div><Button type="submit" label={texts.save} variant="primary"
											size="small" /></div>
									</form>
								</Tab>
							))}
						</Tabs>
						<p className="code-editor-demo__status" role="status">{filesMessage}</p>
					</section>
				</Tab>
			</Tabs>
		</div>
	);
}
