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
 * The handlers a page plugs into the events of the editors, the rich text editors' and the code
 * editor's (the option events). On each event an editor fires, its handlers run, each at once or a
 * moment after the last of a row of such events, given the event, the editor and send: send gives
 * onStateChange the state of the editor, and tells whether the text changed, when the state or the
 * text changed since they were last sent.
 *
 * By default the editors send them a moment after the user changed the text, moved the cursor,
 * scrolled or resized (STATE_DELAY), and at once as they lose the focus and as the page goes
 * (DEFAULT_STATE_EVENTS). A page gives other handlers for an event, or none, by the key of the event
 * among the editor's events, or by the name of any other event the editor fires; the events it
 * leaves out keep their default:
 *
 *   new RichTextEditor(element, { onStateChange: keep, events: {
 *       change: [],                                            // nothing while the user types
 *       blur: [(event, editor, send) => send()],               // as the editor loses the focus
 *       resize: [{ delay: 800, handler: (event, editor, send) => send() }] } });
 */

import { getHost } from './host.js';

/** Sends the state, and tells whether the text changed, when they changed since last sent. */
export type EditorEventSend = () => void;

/** What runs on an event of an editor. */
export type EditorEventFunction<E> = (event: CustomEvent, editor: E, send: EditorEventSend) => void;

/** A handler running a moment after the last of a row of events. */
export interface DelayedEditorEventHandler<E> {
	/** What runs. */
	handler: EditorEventFunction<E>;
	/**
	 * How long it waits for the next of the events, in milliseconds, each postponing it until they
	 * pause; none, or 0, for not at all.
	 */
	delay?: number;
}

/** What runs on an event: at once, or a moment after the last of a row of them. */
export type EditorEventHandler<E> = EditorEventFunction<E> | DelayedEditorEventHandler<E>;

/**
 * The handlers of the events, by the keys of the editor's events (K), or by the names of any other
 * events it fires; an empty list for none.
 */
export type EditorEvents<E, K extends string> = {
	readonly [key in K]?: readonly EditorEventHandler<E>[];
} & {
	readonly [name: string]: readonly EditorEventHandler<E>[] | undefined;
};

/** How long the default handlers wait after the text changed, the cursor moved, a scroll and a resize. */
export const STATE_DELAY = 400;

/**
 * Sends the state: the handler of the events sending it by default.
 */
export function sendEditorState(_event: Event, _editor: unknown, send: EditorEventSend): void {
	send();
}

/** The default handlers, sending the state, by the keys of the editors' events. */
export const DEFAULT_STATE_EVENTS: Readonly<Record<string, readonly EditorEventHandler<unknown>[]>> =
	Object.freeze({
		change: Object.freeze([{ handler: sendEditorState, delay: STATE_DELAY }]),
		select: Object.freeze([{ handler: sendEditorState, delay: STATE_DELAY }]),
		scroll: Object.freeze([{ handler: sendEditorState, delay: STATE_DELAY }]),
		resize: Object.freeze([{ handler: sendEditorState, delay: STATE_DELAY }]),
		blur: Object.freeze([sendEditorState]),
		leave: Object.freeze([sendEditorState])
	});

/** No handlers in place of the default ones: for Wicket's editors, whose events send the state. */
export const NO_STATE_EVENTS: Readonly<Record<string, readonly EditorEventHandler<unknown>[]>> =
	Object.freeze(Object.fromEntries(Object.keys(DEFAULT_STATE_EVENTS)
		.map((key) => [key, Object.freeze([])])));

/** A handler, with the timer of its delay. */
interface Entry<E> {
	run: EditorEventFunction<E>;
	delay: number;
	timer: ReturnType<typeof setTimeout> | null;
}

/**
 * @param event an event
 * @returns whether it tells the page is going: leaving, in its detail
 */
function isLeaving(event: Event): boolean {
	const detail = (event as CustomEvent).detail as { leaving?: unknown } | null | undefined;
	return !!detail && typeof detail === 'object' && detail.leaving === true;
}

/**
 * Runs the handlers of the events of an editor, which the editor hands each event it fired, once
 * the event went through the page: the page's listeners hear of it first.
 *
 * @param <E> the editor
 */
export class EditorEventHandlers<E> {
	private readonly byType = new Map<string, Entry<E>[]>();

	/**
	 * @param editor the editor, which the handlers get
	 * @param names the names of the editor's events, by their keys
	 * @param events the handlers the page gives, by the keys of the events or their names; the
	 *        events left out keep the default handlers
	 * @param send sends the state, telling whether the page is going
	 */
	constructor(private readonly editor: E, names: Readonly<Record<string, string>>,
		events: Readonly<Record<string, readonly EditorEventHandler<E>[] | undefined>> | undefined,
		private readonly send: (leaving: boolean) => void) {
		// a key left out, or undefined, keeps its default handlers
		const all: Record<string, readonly EditorEventHandler<E>[] | undefined> =
			{ ...DEFAULT_STATE_EVENTS };
		Object.entries(events ?? {}).forEach(([key, handlers]) => {
			if (handlers !== undefined) {
				all[key] = handlers;
			}
		});
		Object.entries(all).forEach(([key, handlers]) => {
			if (!handlers?.length) {
				return;
			}
			const type = Object.prototype.hasOwnProperty.call(names, key) ? names[key]! : key;
			const entries = this.byType.get(type) ?? [];
			entries.push(...handlers.map((handler): Entry<E> => typeof handler === 'function' ?
				{ run: handler, delay: 0, timer: null } :
				{ run: handler.handler, delay: Math.max(handler.delay ?? 0, 0), timer: null }));
			this.byType.set(type, entries);
		});
	}

	/**
	 * Runs the handlers of an event the editor fired, at once or once the events of its name
	 * paused.
	 *
	 * @param event the event, a custom event telling the editor and more in its detail, or an
	 *        event of the text area, input or change
	 */
	handle(event: Event): void {
		this.byType.get(event.type)?.forEach((entry) => this.schedule(entry, event as CustomEvent));
	}

	private schedule(entry: Entry<E>, event: CustomEvent): void {
		if (entry.delay <= 0) {
			this.run(entry, event);
			return;
		}
		// each new event postpones the handler waiting, until they pause
		if (entry.timer !== null) {
			clearTimeout(entry.timer);
		}
		entry.timer = setTimeout(() => {
			entry.timer = null;
			this.run(entry, event);
		}, entry.delay);
	}

	private run(entry: Entry<E>, event: CustomEvent): void {
		const leaving = isLeaving(event);
		try {
			entry.run(event, this.editor, () => this.send(leaving));
		} catch (error) {
			getHost().log.error('The handler of ' + event.type + ' failed', error);
		}
	}

	/**
	 * Drops the handlers still waiting, and runs no more.
	 */
	destroy(): void {
		this.byType.forEach((entries) => entries.forEach((entry) => {
			if (entry.timer !== null) {
				clearTimeout(entry.timer);
				entry.timer = null;
			}
		}));
		this.byType.clear();
	}
}
