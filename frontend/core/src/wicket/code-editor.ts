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
 * antilia-code-editor.js: registers Wicket.CodeEditor for the CodeEditor of ui-components, which
 * calls Wicket.CodeEditor.attach(id, options). With stateUrl, the URL of the editor's Ajax
 * behavior, the editor sends the server its state, and its text with it when the user changed it,
 * under the name of the text area: a moment after the user changed them and as the editor loses the
 * focus, via Ajax; as the page goes, in a beacon the browser sends even then.
 *
 * Wicket.CodeEditor.registerLanguage(name, function (kit) {...}) plugs in a grammar of the page's
 * own, built from the CodeMirror classes of the kit, a StreamLanguage for example.
 *
 * It registers Wicket.MarkdownPreview too, for the MarkdownPreview of ui-components, which calls
 * Wicket.MarkdownPreview.attach(id, options): the preview of the text of an editor, or of a text.
 */

import { CODE_EDITOR_KIT, CODE_EDITOR_LANGUAGES, CodeEditor, codeEditorLanguages, registerLanguage,
	type CodeEditorOptions, type CodeEditorState, type CodeEditorStateChange } from '../code-editor.js';
import { MarkdownPreview } from '../markdown-preview.js';
import { useWicketHost, wicket } from './host.js';
import { sendState, type Parameter } from './send-state.js';

/** The options of Wicket's editor: those of the core, and where the server hears of the state. */
interface WicketCodeEditorOptions extends CodeEditorOptions {
	/** The URL of the Ajax behavior keeping the state on the server. */
	stateUrl?: string;
	/** The name the text area posts the text under, which the text goes to the server under too. */
	inputName?: string;
}

const WicketCodeEditor = {

	/**
	 * Makes an editor of a text area.
	 *
	 * @param id the markup id of the text area
	 * @param options the text, the language, what the user may do and the state; where the server
	 *        hears of the state
	 * @returns the editor, null when there is no such element
	 */
	attach(id: string, options: WicketCodeEditorOptions = {}): CodeEditor | null {
		const { stateUrl, inputName, ...rest } = options;
		let editor: CodeEditor | null = null;
		const onStateChange = stateUrl ? (state: CodeEditorState, change: CodeEditorStateChange) => {
			const parameters: Parameter[] = [{ name: 'state', value: JSON.stringify(state) }];
			if (change.changed && inputName && editor) {
				parameters.push({ name: inputName, value: editor.getValue() });
			}
			sendState(id, stateUrl, parameters, change.leaving);
			rest.onStateChange?.(state, change);
		} : rest.onStateChange;
		editor = CodeEditor.attach(id, { ...rest, onStateChange });
		return editor;
	},

	/**
	 * @param id the markup id of the text area
	 * @returns the editor, if there is one
	 */
	of(id: string): CodeEditor | null {
		return CodeEditor.of(id);
	},

	/** Plugs in a grammar: see registerLanguage() of the core. */
	registerLanguage,

	/** @returns the names of the languages the editors know */
	languages: codeEditorLanguages,

	/** The languages the editor knows from the start. */
	LANGUAGES: CODE_EDITOR_LANGUAGES,

	/** The CodeMirror classes of the editor, for the grammars of a page. */
	kit: CODE_EDITOR_KIT
};

const Wicket = wicket();
if (Wicket.CodeEditor === undefined) {
	useWicketHost();
	Wicket.CodeEditor = WicketCodeEditor;
}
if (Wicket.MarkdownPreview === undefined) {
	Wicket.MarkdownPreview = MarkdownPreview;
}
