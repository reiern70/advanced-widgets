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
 * The editor sends the server its state, and its text when the user changed it, with each of its
 * events going to the server, which the events of the CKEditor, Wicket.ClientEvents, send
 * (Wicket.CKEditor.stateParameters): by default a moment after the user changed them, as the
 * editor loses the focus and, in a beacon, as the page goes. The core's own handlers sending the
 * state are off.
 */

import { CKEditor, type CKEditorOptions } from '../ck-editor.js';
import { NO_STATE_EVENTS } from '../editor-events.js';
import { useWicketHost, wicket } from './host.js';
import { useRichTextEvents } from './rich-text-events.js';
import { stateParameters } from './send-state.js';

const WicketCKEditor = {

	/**
	 * Makes an editor of a text area.
	 *
	 * @param id the markup id of the text area
	 * @param options CKEditor's license key, the text, its format, the features and the state
	 * @returns the editor, null when there is no such element
	 */
	attach(id: string, options: CKEditorOptions = {}): CKEditor | null {
		// the events of the server send the state
		return CKEditor.attach(id, { ...options, events: { ...NO_STATE_EVENTS,
			...options.events } });
	},

	/** What the events of the server carry besides: see stateParameters(). */
	stateParameters,

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
