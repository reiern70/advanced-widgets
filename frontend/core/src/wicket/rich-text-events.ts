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
 * Wicket.RichTextEvents, the types of events of the rich text editors (see rich-text-events.ts of
 * the core), which the scripts of both editors define, antilia-rich-text-editor.js and
 * antilia-ckeditor.js: whichever comes first, as they share the types. The script of a type, which
 * an IClientEvent of ui-components renders, registers it:
 *
 *   Wicket.RichTextEvents.register('antilia-text:headings', { from: [...], init: ..., fire: ... });
 *
 * and a page fires one by hand on an editor, of either kind:
 *
 *   Wicket.RichTextEvents.trigger('article', 'antilia-text:headings');
 */

import { getHost } from '../host.js';
import { RICH_TEXT_EDITOR_EVENTS } from '../rich-text-contract.js';
import { RICH_TEXT_BUILT_IN_EVENTS, RICH_TEXT_EVENT_SOURCES, registerRichTextEvent,
	richTextEvents, type RichTextEventEditor } from '../rich-text-events.js';
import { wicket } from './host.js';

/** The globals of the editors, which find an editor by the id of its text area. */
const EDITORS = ['RichTextEditor', 'CKEditor'];

const WicketRichTextEvents = {
	/** Makes a type of events known to both editors: see registerRichTextEvent() of the core. */
	register: registerRichTextEvent,

	/** @returns the names of the types of events registered */
	types: richTextEvents,

	/** The names of the editors' own events, which the types fire from, by their keys. */
	events: RICH_TEXT_EDITOR_EVENTS,

	/** The events the types fire from: the editors' own and those of their text area. */
	sources: RICH_TEXT_EVENT_SOURCES,

	/** The names of the built-in types, by their keys. */
	builtIn: RICH_TEXT_BUILT_IN_EVENTS,

	/**
	 * Fires a type of events on an editor, Tiptap's or CKEditor's: see trigger() of the editors.
	 *
	 * @param id the markup id of the editor's text area
	 * @param name the name of the type
	 * @param values what the type is told
	 * @returns whether the event fired: not without such an editor
	 */
	trigger(id: string, name: string, values?: Readonly<Record<string, unknown>>): boolean {
		const Wicket = wicket();
		for (const global of EDITORS) {
			const editors = Wicket[global] as { of?(id: string): RichTextEventEditor | null } |
				undefined;
			const editor = editors?.of?.(id);
			if (editor) {
				return editor.trigger(name, values);
			}
		}
		getHost().log.error('No rich text editor has the id ' + id);
		return false;
	}
};

/**
 * Defines Wicket.RichTextEvents, unless the script of the other editor did.
 */
export function useRichTextEvents(): void {
	const Wicket = wicket();
	if (Wicket.RichTextEvents === undefined) {
		Wicket.RichTextEvents = WicketRichTextEvents;
	}
}
