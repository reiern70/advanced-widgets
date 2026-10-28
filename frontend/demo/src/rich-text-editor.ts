/*
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
 * editor in, with a drop-down and check boxes switching the toolbar, the bubble menu, reading only,
 * the count of the words and the colors of the code blocks, and the HTML the editor gives; notes
 * in Markdown, and the Markdown the editor gives. The images the user uploads go to the Wicket
 * page, which keeps them and answers their addresses; the page checks the article too, with the
 * proofreader of its writing case, and offers its people after @, its topics after # and its terms
 * on Ctrl+Space.
 */

import { proofreadAt, suggestAt, uploadImageTo, type RichTextEditorState,
	type RichTextEditorTexts, type RichTextProofreader, type RichTextSuggester,
	type RichTextTool } from '@antilia/core';

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
	highlight: string;
	/** What the code blocks and the uploads do, as the page says it. */
	extras: string;
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
	/** Where the apps upload images: the page's own resource, answering their addresses. */
	uploadUrl: string;
	uploadTypes: string[];
	uploadMaxSize: number;
	/** Where the apps have their texts checked: the page's own behavior. */
	proofreadUrl: string;
	/** Where the apps ask for suggestions, and the suggesters of the page. */
	suggestUrl: string;
	suggesters: Omit<RichTextSuggester, 'items'>[];
}

/**
 * @returns what checks the texts of the apps: the page
 */
export function proofreader(config: RichTextEditorConfig): RichTextProofreader {
	return proofreadAt(config.proofreadUrl);
}

/**
 * @returns what offers the apps the people, the topics and the terms: the page
 */
export function suggesters(config: RichTextEditorConfig): RichTextSuggester[] {
	return (config.suggesters ?? []).map((suggester) => suggestAt(config.suggestUrl, suggester));
}

/**
 * @returns what the editors of the apps upload images with: to the page
 */
export function uploader(config: RichTextEditorConfig): (file: File) => Promise<string> {
	return uploadImageTo(config.uploadUrl);
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
