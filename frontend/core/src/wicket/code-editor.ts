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
 * antilia-code-editor.js: registers Wicket.CodeEditor for the CodeEditor of ui-components, which
 * calls Wicket.CodeEditor.attach(id, options). The editor sends the server its state, and its text
 * when the user changed it, with each of its events going to the server, which the events of the
 * CodeEditor, Wicket.ClientEvents, send (Wicket.CodeEditor.stateParameters): by default a moment
 * after the user changed them, as the editor loses the focus and, in a beacon, as the page goes.
 * The core's own handlers sending the state are off.
 *
 * Wicket.CodeEditor.registerLanguage(name, function (kit) {...}) plugs in a grammar of the page's
 * own, built from the CodeMirror classes of the kit, a StreamLanguage for example.
 *
 * It registers Wicket.MarkdownPreview too, for the MarkdownPreview of ui-components, which calls
 * Wicket.MarkdownPreview.attach(id, options): the preview of the text of an editor, or of a text.
 */

import { CODE_EDITOR_KIT, CODE_EDITOR_LANGUAGES, CodeEditor, codeEditorLanguages, registerLanguage,
	type CodeEditorOptions } from '../code-editor.js';
import { NO_STATE_EVENTS } from '../editor-events.js';
import { MarkdownPreview } from '../markdown-preview.js';
import { useWicketHost, wicket } from './host.js';
import { stateParameters } from './send-state.js';

const WicketCodeEditor = {

	/**
	 * Makes an editor of a text area.
	 *
	 * @param id the markup id of the text area
	 * @param options the text, the language, what the user may do and the state
	 * @returns the editor, null when there is no such element
	 */
	attach(id: string, options: CodeEditorOptions = {}): CodeEditor | null {
		// the events of the server send the state
		return CodeEditor.attach(id, { ...options, events: { ...NO_STATE_EVENTS,
			...options.events } });
	},

	/** What the events of the server carry besides: see stateParameters(). */
	stateParameters,

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
