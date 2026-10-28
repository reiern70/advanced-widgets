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
 * antilia-ckeditor.js: registers Wicket.CKEditor for the CKEditor of ui-components, which calls
 * Wicket.CKEditor.attach(id, options). It holds no code of CKEditor: the page loads CKEditor's
 * browser build before, which defines the global CKEDITOR, with its license key in the options.
 * With stateUrl, the URL of the editor's Ajax behavior, the editor sends the server its state, and
 * its text with it when the user changed it, under the name of the text area: a moment after the
 * user changed them and as the editor loses the focus, via Ajax; as the page goes, in a beacon the
 * browser sends even then.
 */

import { CKEditor, type CKEditorOptions } from '../ck-editor.js';
import type { RichTextEditorState, RichTextEditorStateChange } from '../rich-text-contract.js';
import { useWicketHost, wicket } from './host.js';
import { useRichTextEvents } from './rich-text-events.js';
import { sendState, type Parameter } from './send-state.js';

/** The options of Wicket's editor: those of the core, and where the server hears of the state. */
interface WicketCKEditorOptions extends CKEditorOptions {
	/** The URL of the Ajax behavior keeping the state on the server. */
	stateUrl?: string;
	/** The name the text area posts the text under, which the text goes to the server under too. */
	inputName?: string;
}

const WicketCKEditor = {

	/**
	 * Makes an editor of a text area.
	 *
	 * @param id the markup id of the text area
	 * @param options CKEditor's license key, the text, its format, the features and the state;
	 *        where the server hears of the state
	 * @returns the editor, null when there is no such element
	 */
	attach(id: string, options: WicketCKEditorOptions = {}): CKEditor | null {
		const { stateUrl, inputName, ...rest } = options;
		let editor: CKEditor | null = null;
		const onStateChange = stateUrl ? (state: RichTextEditorState,
			change: RichTextEditorStateChange) => {
			const parameters: Parameter[] = [{ name: 'state', value: JSON.stringify(state) }];
			if (change.changed && inputName && editor) {
				parameters.push({ name: inputName, value: editor.getValue() });
			}
			sendState(id, stateUrl, parameters, change.leaving);
			rest.onStateChange?.(state, change);
		} : rest.onStateChange;
		editor = CKEditor.attach(id, { ...rest, onStateChange });
		return editor;
	},

	/**
	 * @param id the markup id of the text area
	 * @returns the editor, if there is one
	 */
	of(id: string): CKEditor | null {
		return CKEditor.of(id);
	}
};

const Wicket = wicket();
if (Wicket.CKEditor === undefined) {
	useWicketHost();
	Wicket.CKEditor = WicketCKEditor;
	useRichTextEvents();
}
