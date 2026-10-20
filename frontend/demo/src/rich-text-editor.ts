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

/*
 * The RichTextEditor demo: an article in HTML, kept by the app with the state the user left its
 * editor in, with a drop-down and check boxes switching the toolbar, the bubble menu, reading only
 * and the count of the words, and the HTML the editor gives; notes in Markdown, and the Markdown
 * the editor gives.
 */

import type { RichTextEditorState, RichTextEditorTexts, RichTextTool } from '@antilia/core';

import { format, type DropdownTexts } from './data.js';

/** The texts of the demo, by the keys of the page's bundle. */
export interface RichTextEditorDemoTexts extends DropdownTexts {
	/** The accessible name of the pills of the cases. */
	cases: string;
	articleTitle: string;
	notesTitle: string;
	articleLabel: string;
	notesLabel: string;
	placeholder: string;
	toolbar: string;
	bubbleMenu: string;
	readOnly: string;
	count: string;
	save: string;
	unsaved: string;
	allSaved: string;
	/** The title of the article in the apps, which keep it unclean. */
	appArticleTitle: string;
	appHtml: string;
	appMarkdown: string;
	/** What the app keeps: {0} the characters of the text, {1} whether all is saved. */
	appKnown: string;
	appStart: string;
	appSaved: string;
	/** Why the app shows the HTML as text: a server cleans it. */
	appNotClean: string;
	[key: string]: string;
}

/** What the Wicket page gives the apps. */
export interface RichTextEditorConfig {
	article: string;
	notes: string;
	texts: RichTextEditorDemoTexts;
	editor: Partial<RichTextEditorTexts>;
	toolbars: string[];
}

/** A text the app keeps: the text, the text last saved and the state of its editor. */
export interface RichText {
	text: string;
	saved: string;
	state?: RichTextEditorState;
}

/** The compact toolbar, as the Wicket page has it. */
const COMPACT: RichTextTool[][] = [['heading'], ['bold', 'italic', 'link'],
	['bulletList', 'orderedList'], ['undo', 'redo']];

/**
 * @returns the tools of a toolbar of the drop-down: the default toolbar for full, false for none
 */
export function toolbarOf(name: string): RichTextTool[][] | false | undefined {
	return name === 'compact' ? COMPACT : name === 'none' ? false : undefined;
}

/**
 * @returns a text the app starts with: saved as it is
 */
export function richText(text: string): RichText {
	return { text, saved: text };
}

/**
 * @returns what the app keeps of a text, in words
 */
export function describe(texts: RichTextEditorDemoTexts, text: RichText): string {
	return format(texts.appKnown, text.text.length, text.text === text.saved ? texts.allSaved :
		texts.unsaved);
}
