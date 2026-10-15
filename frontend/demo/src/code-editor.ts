/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The CodeEditor demo: a sample in each language the editor knows and in that of a log, whose
 * grammar the app plugs in, each kept by the app with the state the user left its editor in; check
 * boxes and drop-downs switching the options of the editor; notes in Markdown beside their preview,
 * in a split panel, a button hiding the preview and showing it again; and the files of a project in
 * closable tabs, an editor in each, marking its tab while it has changes not saved.
 */

import { codeEditorLanguages, registerLanguage, type CodeEditorState,
	type CodeEditorTexts, type ResizeAxis, type TabsTexts } from '@antilia/core';

import { format, type DropdownTexts } from './data.js';

/** The texts of the demo, by the keys of the page's bundle. */
export interface CodeEditorDemoTexts extends DropdownTexts {
	/** The accessible name of the pills of the cases. */
	cases: string;
	editorTitle: string;
	notesTitle: string;
	filesTitle: string;
	language: string;
	resize: string;
	lineNumbers: string;
	readOnly: string;
	selectable: string;
	lineWrapping: string;
	save: string;
	start: string;
	/** What the page heard: {0} the name of the file. */
	heardFocus: string;
	heardBlur: string;
	heardSave: string;
	/**
	 * What is known of a sample: {0} its file, {1} its lines, {2} the line and {3} the column of
	 * the cursor, {4} the characters selected, {5} the size, {6} whether all is saved.
	 */
	known: string;
	sizeOfPage: string;
	/** The size given to the editor: {0} its width, {1} its height. */
	sizeGiven: string;
	unsaved: string;
	allSaved: string;
	hidePreview: string;
	showPreview: string;
	notesStart: string;
	previewHidden: string;
	editorHidden: string;
	bothShown: string;
	filesLabel: string;
	filesStart: string;
	closed: string;
	saved: string;
	[key: string]: string;
}

/** A sample as the page gives it: the name of its language, its file and its text. */
export interface SampleConfig {
	name: string;
	file: string;
	text: string;
}

/** A file of the project as the page gives it. */
export interface FileConfig {
	name: string;
	language: string;
	text: string;
}

/** What the page gives the demo. */
export interface CodeEditorConfig {
	texts: CodeEditorDemoTexts;
	/** What the editor says. */
	editor: Partial<CodeEditorTexts>;
	/** What the tabs say. */
	tabs: TabsTexts;
	/** The samples, in the order of the drop-down of the languages. */
	languages: SampleConfig[];
	/** How the editor resizes, in the order of the drop-down: the names of the axes, or none. */
	resizes: string[];
	files: FileConfig[];
	/** The notes in Markdown shown beside their preview. */
	notes: { file: string; text: string };
	/** What the preview says. */
	preview: { label: string; placeholder: string };
	/** What the divider of the split panel says. */
	split: { divider: string; collapseLeft: string; collapseRight: string; restore: string };
	/** The markup of the icons, as Wicket renders them. */
	icons: { file: string };
}

/** A sample or a file, as the app keeps it: its text, the text saved and the editor's state. */
export interface Sample {
	/** The name of its language, or of its tab. */
	id: string;
	file: string;
	language: string;
	text: string;
	saved: string;
	state?: CodeEditorState;
}

/**
 * Plugs in the grammar of a log, as the Wicket page does with its script, once: the times are
 * numbers, the levels keywords, errors invalid, warnings meta, the threads comments, the quoted
 * texts strings and the exceptions types.
 */
export function registerLogGrammar(): void {
	if (codeEditorLanguages().includes('log')) {
		return;
	}
	registerLanguage('log', (kit) => kit.StreamLanguage.define({
		name: 'log',
		token(stream) {
			if (stream.eatSpace()) {
				return null;
			}
			const tokens: Array<[RegExp, string]> = [
				[/^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}:\d{2}(\.\d+)?/, 'number'],
				[/^(ERROR|FATAL)\b/, 'invalid'],
				[/^WARN(ING)?\b/, 'meta'],
				[/^(INFO|DEBUG|TRACE)\b/, 'keyword'],
				[/^\[[^\]]*\]/, 'comment'],
				[/^"[^"]*"/, 'string'],
				[/^[A-Z]\w*(Exception|Error)\b/, 'typeName'],
				[/^\d+/, 'number']
			];
			for (const [pattern, token] of tokens) {
				if (stream.match(pattern)) {
					return token;
				}
			}
			stream.next();
			return null;
		}
	}), ['logs']);
}

/**
 * @returns the samples the page gives, by the names of their languages, as the app keeps them
 */
export function samplesOf(config: CodeEditorConfig): Record<string, Sample> {
	return Object.fromEntries(config.languages.map((sample) => [sample.name, { id: sample.name,
		file: sample.file, language: sample.name, text: sample.text, saved: sample.text }]));
}

/**
 * @returns the notes the page gives, as the app keeps them
 */
export function notesOf(config: CodeEditorConfig): Sample {
	return { id: 'notes', file: config.notes.file, language: 'markdown', text: config.notes.text,
		saved: config.notes.text };
}

/**
 * @returns what is said once the user collapsed a pane of the notes, or restored them
 */
export function collapsedText(texts: CodeEditorDemoTexts, collapsed: string | null): string {
	return collapsed === 'second' ? texts.previewHidden : collapsed === 'first' ?
		texts.editorHidden : texts.bothShown;
}

/**
 * @returns the files the page gives, as the app keeps them
 */
export function filesOf(config: CodeEditorConfig): Sample[] {
	return config.files.map((file, index) => ({ id: 'file-' + index, file: file.name,
		language: file.language, text: file.text, saved: file.text }));
}

/**
 * @returns how the editor resizes, by the name in the drop-down
 */
export function axisOf(resize: string): ResizeAxis | false {
	return resize === 'vertical' || resize === 'horizontal' || resize === 'both' ? resize : false;
}

/**
 * @returns what the app knows of a sample: its lines, where the cursor is, what is selected, the
 *          size of the editor and whether all is saved, in words, as the Wicket page says it
 */
export function describe(config: CodeEditorConfig, sample: Sample): string {
	const { texts } = config;
	const text = sample.text;
	const state = sample.state ?? {};
	const ranges = state.selection?.ranges ?? [];
	const main = ranges[state.selection?.main ?? 0] ?? { anchor: 0, head: 0 };
	const cursor = Math.min(main.head, text.length);
	const before = text.slice(0, cursor);
	const line = before.split('\n').length;
	const column = cursor - before.lastIndexOf('\n');
	const selected = ranges.reduce((sum, range) => sum + Math.abs(range.head - range.anchor), 0);
	const width = state.width ?? null;
	const height = state.height ?? null;
	const size = width === null && height === null ? texts.sizeOfPage :
		format(texts.sizeGiven, width ?? 'auto', height ?? 'auto');
	return format(texts.known, sample.file, text === '' ? 0 : text.split('\n').length, line,
		column, selected, size, text !== sample.saved ? texts.unsaved : texts.allSaved);
}
