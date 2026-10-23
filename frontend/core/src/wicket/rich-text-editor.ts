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
 * antilia-rich-text-editor.js: registers Wicket.RichTextEditor for the RichTextEditor of
 * ui-components, which calls Wicket.RichTextEditor.attach(id, options). With stateUrl, the URL of
 * the editor's Ajax behavior, the editor sends the server its state, and its text with it when the
 * user changed it, under the name of the text area: a moment after the user changed them and as
 * the editor loses the focus, via Ajax; as the page goes, in a beacon the browser sends even then.
 *
 * Wicket.RichTextEditor.registerExtension(name, function (kit) {...}) plugs in an extension of the
 * page's own, built from the Tiptap classes of the kit, a Mark or a Node for example.
 *
 * With uploadUrl, the URL of the editor's upload behavior, the editor uploads the images the user
 * chooses, drops or pastes there, and the server answers their addresses. The grammars of the code
 * blocks are not here: antilia-rich-text-highlight.js, which a page loads only where an editor
 * highlights, registers them as Wicket.RichTextHighlighter, before this script or after it.
 */

import { registerRichTextHighlighter } from '../rich-text-code-blocks.js';
import { RICH_TEXT_EDITOR_KIT, RichTextEditor, registerRichTextExtension, richTextExtensions,
	type RichTextEditorOptions, type RichTextEditorState,
	type RichTextEditorStateChange } from '../rich-text-editor.js';
import type { RichTextHighlighter } from '../rich-text-languages.js';
import { uploadImageTo } from '../rich-text-upload.js';
import { useWicketHost, wicket } from './host.js';
import { sendState, type Parameter } from './send-state.js';

/** The options of Wicket's editor: those of the core, and where the server hears of the state. */
interface WicketRichTextEditorOptions extends RichTextEditorOptions {
	/** The URL of the Ajax behavior keeping the state on the server. */
	stateUrl?: string;
	/** The name the text area posts the text under, which the text goes to the server under too. */
	inputName?: string;
	/** The URL of the behavior taking the images the user uploads, and answering their addresses. */
	uploadUrl?: string;
}

const WicketRichTextEditor = {

	/**
	 * Makes an editor of a text area.
	 *
	 * @param id the markup id of the text area
	 * @param options the text, its format, the features, the toolbar and the state; where the
	 *        server hears of the state
	 * @returns the editor, null when there is no such element
	 */
	attach(id: string, options: WicketRichTextEditorOptions = {}): RichTextEditor | null {
		const { stateUrl, inputName, uploadUrl, ...rest } = options;
		let editor: RichTextEditor | null = null;
		const onStateChange = stateUrl ? (state: RichTextEditorState,
			change: RichTextEditorStateChange) => {
			const parameters: Parameter[] = [{ name: 'state', value: JSON.stringify(state) }];
			if (change.changed && inputName && editor) {
				parameters.push({ name: inputName, value: editor.getValue() });
			}
			sendState(id, stateUrl, parameters, change.leaving);
			rest.onStateChange?.(state, change);
		} : rest.onStateChange;
		const onUpload = uploadUrl ? uploadImageTo(uploadUrl) : rest.onUpload;
		editor = RichTextEditor.attach(id, { ...rest, onStateChange, onUpload });
		return editor;
	},

	/**
	 * @param id the markup id of the text area
	 * @returns the editor, if there is one
	 */
	of(id: string): RichTextEditor | null {
		return RichTextEditor.of(id);
	},

	/** Plugs in an extension: see registerRichTextExtension() of the core. */
	registerExtension: registerRichTextExtension,

	/** @returns the names of the extensions registered */
	extensions: richTextExtensions,

	/** The Tiptap classes of the editor, for the extensions of a page. */
	kit: RICH_TEXT_EDITOR_KIT,

	/** Highlights the code blocks of the editors told so: see registerRichTextHighlighter(). */
	registerHighlighter: registerRichTextHighlighter
};

const Wicket = wicket();
if (Wicket.RichTextEditor === undefined) {
	useWicketHost();
	Wicket.RichTextEditor = WicketRichTextEditor;
	// antilia-rich-text-highlight.js came first
	const highlighter = Wicket.RichTextHighlighter as RichTextHighlighter | undefined;
	if (highlighter) {
		registerRichTextHighlighter(highlighter);
	}
}
