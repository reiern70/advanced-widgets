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
 * The events of the rich text editors a page or a library adds to those they fire of their own
 * (RICH_TEXT_EDITOR_EVENTS): event types registered by a name, shared by the Tiptap and the
 * CKEditor editors, which fire from the editors' own events, by hand (trigger()), or both.
 *
 *   registerRichTextEvent('antilia-text:headings', {
 *     from: [RICH_TEXT_EDITOR_EVENTS.text],   // none: only trigger()
 *     init: () => ({ headings: '' }),         // what the type keeps for each editor
 *     fire(editor, state, cause) {            // what the event tells; undefined: it does not fire
 *       const headings = ...;
 *       if (headings === state.headings) return undefined;
 *       state.headings = headings;
 *       return { headings };
 *     }
 *   });
 *   editor.trigger('antilia-text:headings');  // from the page, a tool or a key
 *
 * An editor fires the event as it fires its own: on its text area, or its element without one,
 * bubbling, its detail what fire() told and the editor (detail.editor), so that Wicket's
 * ClientEventsBehavior listens to it as to any other.
 *
 * The rules:
 * - a name is <owner>:<event>, in small letters, digits and dashes: antilia-text:headings; the
 *   owner antilia-rich-text-editor is the editors' own, for their built-in types;
 * - a name registers once: registering it again fails, rather than replacing the first;
 * - a type fires from the editors' own events alone (RICH_TEXT_EVENT_SOURCES), not from other
 *   types; and not while it is firing, on the same editor: no loops;
 * - the editor calls fire() for each event the type fires from, each time: when the user leaves
 *   the editor, blur and change both come, and the state of the type decides whether to fire
 *   twice.
 *
 * The types are kept in one registry for the page, shared by the scripts of the editors: Wicket
 * loads each editor in a script of its own, each with this module, and a type registers once for
 * both.
 */

import { getHost } from './host.js';
import { RICH_TEXT_EDITOR_EVENTS, type RichTextEditorState } from './rich-text-contract.js';

/** What the types of events are given of the editor firing them, Tiptap's or CKEditor's. */
export interface RichTextEventEditor {
	/** The element of the editor, holding the text area, if any. */
	readonly element: HTMLElement;
	/** The text area the editor keeps the text in, if it has one. */
	readonly field: HTMLTextAreaElement | null;
	/** @returns the text, in the editor's format */
	getValue(): string;
	/** @returns the text, as plain text: its blocks on lines of their own */
	getText(): string;
	/** @returns the text selected, as plain text; '' for none */
	getSelectedText(): string;
	/** @returns the selection, the scroll position and the size */
	getState(): RichTextEditorState;
	/** @returns whether the text has changes not saved */
	isModified(): boolean;
	/** Fires a type of events registered: see trigger() of the editors. */
	trigger(name: string, values?: Readonly<Record<string, unknown>>): boolean;
}

/** What a type fires from: one of the editor's own events, or trigger(). */
export interface RichTextEventCause {
	/** The name of the event it fires from, RICH_TEXT_EVENT_TRIGGER for trigger(). */
	type: string;
	/** What that event told, without the editor; the values of trigger(). */
	detail: Readonly<Record<string, unknown>>;
}

/** A type of events. */
export interface RichTextEventDefinition<S = any> {
	/**
	 * The events of the editors it fires from, among RICH_TEXT_EVENT_SOURCES; none, as when left
	 * out, for one that fires by trigger() alone.
	 */
	from?: readonly string[];
	/**
	 * What the type keeps for each editor, made as the editor starts (its first event, text), or,
	 * for a type registered later, at the editor's first event after; none when left out.
	 */
	init?: (editor: RichTextEventEditor) => S;
	/**
	 * What the event tells, its detail besides the editor, for an event it fires from; undefined,
	 * or null, for not firing. What the cause told, when left out.
	 */
	fire?: (editor: RichTextEventEditor, state: S, cause: RichTextEventCause) =>
		Record<string, unknown> | null | undefined;
}

/** The cause of an event fired by trigger(). */
export const RICH_TEXT_EVENT_TRIGGER = 'trigger';

/**
 * The events the types fire from: the editors' own, and those their text area fires, input as
 * the user changes the text and change once the editor lost the focus.
 */
export const RICH_TEXT_EVENT_SOURCES: readonly string[] = Object.freeze(['input', 'change',
	...Object.values(RICH_TEXT_EDITOR_EVENTS)]);

/** The owner of the editors' own events, and of their built-in types. */
const BUILT_IN_OWNER = 'antilia-rich-text-editor';

/** The owners of the events no page registers: the editors' and those of Wicket's events. */
const RESERVED_OWNERS: readonly string[] = [BUILT_IN_OWNER, 'antilia-client-events'];

/** <owner>:<event>, each in small letters, digits and dashes. */
const NAME = /^[a-z0-9]+(?:-[a-z0-9]+)*:[a-z0-9]+(?:-[a-z0-9]+)*$/;

interface Registry {
	types: Map<string, Readonly<RichTextEventDefinition>>;
}

/** The key of the registry among the globals, the same for each script holding this module. */
const KEY = Symbol.for('antilia.rich-text-events');

function registry(): Registry {
	const global = globalThis as unknown as Record<symbol, Registry | undefined>;
	let found = global[KEY];
	if (!found) {
		found = { types: new Map() };
		global[KEY] = found;
	}
	return found;
}

/**
 * @returns the definition checked and frozen, under a name checked
 * @throws Error for a name not of the form <owner>:<event>, or for an event to fire from that is
 *         not one of the editors'
 */
function checked(name: string, definition: RichTextEventDefinition):
	Readonly<RichTextEventDefinition> {
	if (!NAME.test(name)) {
		throw new Error('An event of the rich text editors is named <owner>:<event>, in small ' +
			'letters, digits and dashes, not ' + name);
	}
	const from = definition.from ?? [];
	from.forEach((source) => {
		if (!RICH_TEXT_EVENT_SOURCES.includes(source)) {
			throw new Error('The event ' + name + ' fires from the events of the editors alone, ' +
				'not from ' + source);
		}
	});
	return Object.freeze({ ...definition, from: Object.freeze([...new Set(from)]) });
}

/**
 * Makes a type of events known to the editors, Tiptap's and CKEditor's, under a name: those
 * there already fire it too.
 *
 * @param name the name of its events, <owner>:<event>, which Wicket's IClientEvent names
 * @param definition the events it fires from, what it keeps for each editor and what it tells
 * @throws Error for a name taken already, the editors' own or not of the form <owner>:<event>,
 *         and for an event to fire from that is not one of the editors'
 */
export function registerRichTextEvent<S>(name: string, definition: RichTextEventDefinition<S>):
	void {
	const owner = name.slice(0, name.indexOf(':'));
	if (RESERVED_OWNERS.includes(owner)) {
		throw new Error('The events of ' + owner + ' are the editors\' own: ' + name);
	}
	const types = registry().types;
	if (types.has(name)) {
		throw new Error('An event of the rich text editors is named ' + name + ' already');
	}
	types.set(name, checked(name, definition));
}

/**
 * Registers a built-in type, once whatever the number of scripts holding it.
 */
function registerBuiltIn<S>(name: string, definition: RichTextEventDefinition<S>): void {
	const types = registry().types;
	if (!types.has(name)) {
		types.set(name, checked(name, definition));
	}
}

/**
 * @returns the names of the types of events registered, the built-in ones too
 */
export function richTextEvents(): string[] {
	return Array.from(registry().types.keys());
}

/**
 * @returns the type of events registered under a name, if any
 */
export function richTextEvent(name: string): Readonly<RichTextEventDefinition> | undefined {
	return registry().types.get(name);
}

/** The built-in types of events, by their names. */
export const RICH_TEXT_BUILT_IN_EVENTS = Object.freeze({
	/**
	 * The text the user selected changed: detail.text has it, as plain text, '' once nothing is
	 * selected. Fires from select, once for each text: not while the cursor moves with nothing
	 * selected.
	 */
	selectedText: BUILT_IN_OWNER + ':selected-text',
	/**
	 * A paste lost something to the editor: detail.elements and detail.attributes have what, by
	 * name and count (see pasteCleaningReport). The editor fires it after the paste, by trigger(),
	 * and only for a paste that lost something.
	 */
	pasteCleaned: BUILT_IN_OWNER + ':paste-cleaned'
});

registerBuiltIn<{ text: string }>(RICH_TEXT_BUILT_IN_EVENTS.selectedText, {
	from: [RICH_TEXT_EDITOR_EVENTS.select],
	init: () => ({ text: '' }),
	fire(editor, state) {
		const text = editor.getSelectedText();
		if (text === state.text) {
			return undefined;
		}
		state.text = text;
		return { text };
	}
});

/**
 * @returns the counts of a report by their names, those that are counts above 0 alone
 */
function counts(given: unknown): Record<string, number> {
	const found: Record<string, number> = {};
	if (given && typeof given === 'object') {
		Object.entries(given as Record<string, unknown>).forEach(([name, count]) => {
			if (typeof count === 'number' && Number.isInteger(count) && count > 0) {
				found[name] = count;
			}
		});
	}
	return found;
}

registerBuiltIn(RICH_TEXT_BUILT_IN_EVENTS.pasteCleaned, {
	fire(_editor, _state, cause) {
		const elements = counts(cause.detail.elements);
		const attributes = counts(cause.detail.attributes);
		return Object.keys(elements).length || Object.keys(attributes).length ?
			{ elements, attributes } : undefined;
	}
});

/**
 * Fires the types of events of an editor: those of the events it fires from, and those
 * triggered. Each editor has one, which keeps the state of each type for it.
 */
export class RichTextEventTypes {
	private readonly states = new Map<string, unknown>();

	/** The types firing, which do not fire again until they are done. */
	private readonly firing = new Set<string>();

	/**
	 * @param editor the editor
	 * @param emit fires an event on the editor's text area, or its element, as the editor fires
	 *        its own, telling whether it fired: not once the editor is gone
	 */
	constructor(private readonly editor: RichTextEventEditor,
		private readonly emit: (name: string, detail: Record<string, unknown>) => boolean) {}

	/**
	 * Fires the types of events firing from an event of the editor, once it fired.
	 *
	 * @param source the name of the event, one of RICH_TEXT_EVENT_SOURCES
	 * @param detail what it told, without the editor
	 */
	from(source: string, detail: Readonly<Record<string, unknown>> = {}): void {
		const types = registry().types;
		// the state of the types registered since, as the editor is before the event
		types.forEach((type, name) => this.init(name, type));
		for (const [name, type] of types) {
			if (type.from?.includes(source)) {
				this.fire(name, type, { type: source, detail });
			}
		}
	}

	/**
	 * Fires a type of events by hand, from the page, a tool or a key.
	 *
	 * @param name the name of the type
	 * @param values what the type is told, as the detail of its cause
	 * @returns whether it fired: not for a type not registered, nor when the type told not to
	 */
	trigger(name: string, values: Readonly<Record<string, unknown>> = {}): boolean {
		const type = registry().types.get(name);
		if (!type) {
			getHost().log.error('No event of the rich text editors is named ' + name);
			return false;
		}
		return this.fire(name, type, { type: RICH_TEXT_EVENT_TRIGGER, detail: { ...values } });
	}

	private fire(name: string, type: Readonly<RichTextEventDefinition>,
		cause: RichTextEventCause): boolean {
		if (this.firing.has(name)) {
			getHost().log.error('The event ' + name + ' fires while it is firing: it does not');
			return false;
		}
		this.firing.add(name);
		try {
			this.init(name, type);
			const detail = type.fire ? type.fire(this.editor, this.states.get(name), cause) :
				{ ...cause.detail };
			return detail !== undefined && detail !== null && this.emit(name, { ...detail });
		} catch (error) {
			getHost().log.error('The event ' + name + ' of the rich text editors failed', error);
			return false;
		} finally {
			this.firing.delete(name);
		}
	}

	/**
	 * Makes what a type keeps for the editor, unless it did.
	 */
	private init(name: string, type: Readonly<RichTextEventDefinition>): void {
		if (this.states.has(name)) {
			return;
		}
		let state: unknown;
		try {
			state = type.init?.(this.editor);
		} catch (error) {
			getHost().log.error('The event ' + name + ' of the rich text editors failed', error);
		}
		this.states.set(name, state);
	}
}
