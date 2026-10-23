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
 * The highlighter of the code blocks of the rich text editor: the grammars of the code editor,
 * Lezer's parsers of Java, JavaScript, TypeScript, JSON and Markdown, and the classes of Lezer's
 * classHighlighter, tok-keyword and so on, which code-editor.css colors from the theme.
 *
 * The grammars weigh about a third of the editor, so the editor does not carry them: a page that
 * highlights gives it lezerHighlighter (the option highlight), whose grammars load the first time
 * a block shows their language; a bundler of the page's own makes each a file of its own. Wicket's
 * pages load them all at once, in antilia-rich-text-highlight.js, only where an editor highlights.
 */

import type { Parser } from '@lezer/common';
import { classHighlighter, highlightTree } from '@lezer/highlight';

import { codeLanguage, RICH_TEXT_CODE_LANGUAGES, type RichTextHighlighter,
	type RichTextToken } from './rich-text-languages.js';

/** The grammars, by the names of their languages, loaded the first time they are asked for. */
const GRAMMARS: Readonly<Record<string, () => Promise<Parser>>> = Object.freeze({
	java: () => import('@lezer/java').then((module) => module.parser),
	javascript: () => import('@lezer/javascript').then((module) =>
		module.parser.configure({ dialect: 'jsx' })),
	typescript: () => import('@lezer/javascript').then((module) =>
		module.parser.configure({ dialect: 'ts' })),
	json: () => import('@lezer/json').then((module) => module.parser),
	markdown: () => import('@lezer/markdown').then((module) => module.parser.configure(module.GFM))
});

/** The grammars loaded. */
const parsers = new Map<string, Parser>();

/** The grammars loading, or tried. */
const loading = new Map<string, Promise<boolean>>();

/**
 * Highlights the code blocks in Java, JavaScript, TypeScript, JSON and Markdown, as the code editor
 * does; js, ts and md name them too.
 */
export const lezerHighlighter: RichTextHighlighter = Object.freeze({
	languages: RICH_TEXT_CODE_LANGUAGES,

	highlight(text: string, language: string): readonly RichTextToken[] | null {
		const parser = parsers.get(codeLanguage(language) ?? '');
		if (!parser) {
			return null;
		}
		const tokens: RichTextToken[] = [];
		highlightTree(parser.parse(text), classHighlighter, (from, to, classes) => {
			tokens.push({ from, to, classes });
		});
		return tokens;
	},

	load(language: string): Promise<boolean> {
		const key = codeLanguage(language) ?? '';
		const grammar = Object.prototype.hasOwnProperty.call(GRAMMARS, key) ? GRAMMARS[key] :
			undefined;
		if (!grammar) {
			return Promise.resolve(false);
		}
		let done = loading.get(key);
		if (!done) {
			done = grammar().then((parser) => {
				parsers.set(key, parser);
				return true;
			}, () => {
				// the next block of the language tries again
				loading.delete(key);
				return false;
			});
			loading.set(key, done);
		}
		return done;
	}
});
