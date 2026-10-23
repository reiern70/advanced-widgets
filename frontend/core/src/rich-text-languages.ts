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
 * The languages of the code blocks of the rich text editor, and what highlights them: what the
 * editor and its highlighter (rich-text-highlight.ts) share. Nothing here needs Tiptap nor Lezer,
 * so that the script of the highlighter does not carry the editor, nor the editor the grammars.
 */

/** A range of the text of a code block, and the classes highlighting it: tok-keyword... */
export interface RichTextToken {
	from: number;
	to: number;
	classes: string;
}

/**
 * Highlights the text of code blocks, by its language: the ranges and their classes, which the
 * editor gives the text as decorations, leaving its HTML plain.
 */
export interface RichTextHighlighter {
	/** The languages it highlights, by their names, and what the menu of the code block calls them. */
	readonly languages: Readonly<Record<string, string>>;
	/**
	 * @param text the text of a code block
	 * @param language the name of its language, or one of its other names (see codeLanguage)
	 * @returns the ranges of the text and their classes; null while the grammar of the language is
	 *          not loaded (see load), or for a language the highlighter does not know
	 */
	highlight(text: string, language: string): readonly RichTextToken[] | null;
	/**
	 * Loads the grammar of a language, once.
	 *
	 * @returns whether the highlighter knows the language
	 */
	load(language: string): Promise<boolean>;
}

/**
 * The languages of code blocks the editor knows, those of the code editor, by their names, and
 * what the menu of the tool code block calls them.
 */
export const RICH_TEXT_CODE_LANGUAGES: Readonly<Record<string, string>> = Object.freeze({
	java: 'Java',
	javascript: 'JavaScript',
	typescript: 'TypeScript',
	json: 'JSON',
	markdown: 'Markdown'
});

/** The other names of the languages, such as the info strings of code blocks of Markdown. */
const OTHER_NAMES: ReadonlyMap<string, string> = new Map([['js', 'javascript'],
	['jsx', 'javascript'], ['mjs', 'javascript'], ['ts', 'typescript'], ['tsx', 'typescript'],
	['md', 'markdown']]);

/** What the name of a language may be, as the class language-* of a code block holds it. */
const LANGUAGE_NAME = /^[A-Za-z0-9_+#.-]{1,40}$/;

/**
 * @returns the name of a language, in lower case, from one of its other names too (js for
 *          javascript); null for none, or for a name a class could not hold
 */
export function codeLanguage(name: string | null | undefined): string | null {
	const key = (name ?? '').trim().toLowerCase();
	if (!LANGUAGE_NAME.test(key)) {
		return null;
	}
	return OTHER_NAMES.get(key) ?? key;
}
