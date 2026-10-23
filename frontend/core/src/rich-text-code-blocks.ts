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
 * The code blocks of the rich text editor, as the editor shows them: in the colors of their
 * language, and named by it. A code block keeps its language as HTML has it,
 * <pre><code class="language-java">, which the menu of the tool code block chooses.
 *
 * A ProseMirror plugin gives the text of each block the classes a highlighter finds for it
 * (RichTextHighlighter, see rich-text-languages.ts), as decorations: the HTML the editor writes
 * stays plain. The grammar of a language loads the first time a block shows it, and the blocks are
 * drawn again once it has. The block itself gets data-antilia-language, the name of its language,
 * which the style sheet shows in its corner.
 *
 * The highlighter is the page's: given to the editor (the option highlight), or registered for the
 * editors told highlight: true by a script loaded only where they are, as Wicket's
 * antilia-rich-text-highlight.js does (registerRichTextHighlighter). The editor's script carries
 * no grammar.
 */

import { Extension } from '@tiptap/core';
import type { Node as ProseMirrorNode } from '@tiptap/pm/model';
import { Plugin, PluginKey } from '@tiptap/pm/state';
import { Decoration, DecorationSet, type EditorView } from '@tiptap/pm/view';

import { codeLanguage, type RichTextHighlighter,
	type RichTextToken } from './rich-text-languages.js';

/** The highlighter registered for the editors told highlight: true, none before. */
let registered: RichTextHighlighter | null = null;

/** Draws the blocks of an editor again, for each editor shown. */
const editors = new Set<() => void>();

/**
 * Makes a highlighter the one of the editors told highlight: true, those made already too, which
 * then draw their code blocks again.
 *
 * @param highlighter the highlighter, lezerHighlighter for example; null for none
 */
export function registerRichTextHighlighter(highlighter: RichTextHighlighter | null): void {
	registered = highlighter;
	editors.forEach((redraw) => redraw());
}

/**
 * @returns the highlighter registered, null for none
 */
export function richTextHighlighter(): RichTextHighlighter | null {
	return registered;
}

/** The attribute naming the language of a code block, which the style sheet shows. */
export const CODE_BLOCK_LANGUAGE_ATTRIBUTE = 'data-antilia-language';

/** What the plugin of the code blocks asks the editor, each time it draws them. */
export interface CodeBlockOptions {
	/** The highlighter, null for none. */
	highlighter: () => RichTextHighlighter | null;
	/** What a block calls its language, null for a block that does not name it. */
	label: (language: string) => string | null;
}

const KEY = new PluginKey<DecorationSet>('antiliaCodeBlocks');

/** The meta of a transaction drawing the blocks again. */
const REDRAW = 'antilia-rich-text-code:redraw';

/** The tokens of the texts last highlighted, by their language and text, as many as this. */
const CACHED = 200;

/**
 * Draws the code blocks of an editor again: after its highlighter, or the languages it names,
 * changed.
 */
export function redrawCodeBlocks(view: EditorView): void {
	if (!view.isDestroyed) {
		view.dispatch(view.state.tr.setMeta(REDRAW, true).setMeta('addToHistory', false));
	}
}

/**
 * Highlights the code blocks and names their languages, with the highlighter and the names the
 * editor gives.
 */
export const CodeBlocks = Extension.create<CodeBlockOptions>({
	name: 'antiliaCodeBlocks',

	addOptions() {
		return { highlighter: () => null, label: () => null };
	},

	addProseMirrorPlugins() {
		const options = this.options;
		let view: EditorView | null = null;
		const cache = new Map<string, readonly RichTextToken[]>();
		let cachedBy: RichTextHighlighter | null = null;
		const asked = new Set<string>();
		const redraw = () => {
			if (view) {
				redrawCodeBlocks(view);
			}
		};

		const tokens = (highlighter: RichTextHighlighter, text: string,
			language: string): readonly RichTextToken[] | null => {
			if (highlighter !== cachedBy) {
				cache.clear();
				asked.clear();
				cachedBy = highlighter;
			}
			const key = language + '\n' + text;
			const known = cache.get(key);
			if (known) {
				return known;
			}
			const found = highlighter.highlight(text, language);
			if (found) {
				if (cache.size >= CACHED) {
					cache.clear();
				}
				cache.set(key, found);
			} else if (!asked.has(language)) {
				// drawn again once the grammar is there
				asked.add(language);
				highlighter.load(language).then((known) => {
					if (known && highlighter === options.highlighter()) {
						redraw();
					}
				}, () => undefined);
			}
			return found;
		};

		const draw = (doc: ProseMirrorNode): DecorationSet => {
			const highlighter = options.highlighter();
			const decorations: Decoration[] = [];
			doc.descendants((node, position) => {
				if (node.type.name !== 'codeBlock') {
					return true;
				}
				const language = codeLanguage(node.attrs.language as string | null);
				if (language) {
					const label = options.label(language);
					if (label) {
						decorations.push(Decoration.node(position, position + node.nodeSize,
							{ [CODE_BLOCK_LANGUAGE_ATTRIBUTE]: label }));
					}
					const start = position + 1;
					const found = highlighter ? tokens(highlighter, node.textContent, language) : null;
					found?.forEach((token) => {
						if (token.to > token.from) {
							decorations.push(Decoration.inline(start + token.from, start + token.to,
								{ class: token.classes }));
						}
					});
				}
				return false;
			});
			return DecorationSet.create(doc, decorations);
		};

		return [new Plugin<DecorationSet>({
			key: KEY,
			state: {
				init: (_, state) => draw(state.doc),
				apply: (transaction, decorations, _, state) =>
					transaction.docChanged || transaction.getMeta(REDRAW) ? draw(state.doc) :
						decorations
			},
			props: {
				decorations: (state) => KEY.getState(state)
			},
			view: (editorView) => {
				view = editorView;
				editors.add(redraw);
				return {
					destroy: () => {
						editors.delete(redraw);
						view = null;
					}
				};
			}
		})];
	}
});
