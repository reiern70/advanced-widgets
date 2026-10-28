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
 * The CKEditor demo: an article in HTML, kept by the app with the state the user left its editor
 * in, with check boxes switching reading only and the count of the words, and the HTML the editor
 * gives; notes in Markdown, and the Markdown the editor gives. CKEditor is the global CKEDITOR of
 * its browser build, which the Wicket page loads, with the license key the page gives.
 */

import type { CKEditorTexts, RichTextEditorState } from '@antilia/core';

import { format } from './data.js';

/** The texts of the demo, by the keys of the page's bundle. */
export interface CKEditorDemoTexts {
	/** The accessible name of the pills of the cases. */
	cases: string;
	notesTitle: string;
	articleLabel: string;
	notesLabel: string;
	placeholder: string;
	readOnly: string;
	count: string;
	save: string;
	unsaved: string;
	allSaved: string;
	/** The title of the article in the apps, which keep it unclean. */
	appArticleTitle: string;
	appHtml: string;
	markdownKept: string;
	/** What the app keeps: {0} the characters of the text, {1} whether all is saved. */
	appKnown: string;
	appStart: string;
	appSaved: string;
	/** Why the app shows the HTML as text: a server cleans it. */
	appNotClean: string;
	/** Which CKEditor the app gives the component, and its license key. */
	appLicense: string;
	[key: string]: string;
}

/** What the Wicket page gives the apps. */
export interface CKEditorConfig {
	article: string;
	notes: string;
	texts: CKEditorDemoTexts;
	/** The texts of the editor of its own, in the language of the page. */
	editor: Partial<CKEditorTexts>;
	/** CKEditor's license key: GPL, or a commercial key. */
	licenseKey: string;
	/** The language of CKEditor's texts, whose translations the page loaded; none for English. */
	language?: string;
}

/** A text the app keeps: the text, the text last saved and the state of its editor. */
export interface CKText {
	text: string;
	saved: string;
	state?: RichTextEditorState;
}

/**
 * @returns a text the app starts with: saved as it is
 */
export function ckText(text: string): CKText {
	return { text, saved: text };
}

/**
 * @returns what the app keeps of a text, in words
 */
export function describe(texts: CKEditorDemoTexts, text: CKText): string {
	return format(texts.appKnown, text.text.length, text.text === text.saved ? texts.allSaved :
		texts.unsaved);
}
