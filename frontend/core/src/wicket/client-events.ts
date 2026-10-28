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
 * antilia-client-events.js: registers Wicket.ClientEvents for the ClientEventsBehavior of
 * ui-components, which calls Wicket.ClientEvents.attach(id, events, resolve) with the events the
 * component listens to.
 *
 * On each event the element fires, the script waits the delay of the event for the next of its
 * kind, then runs the function of the event, function(event, component, send): component is what
 * resolve(element) gives, the editor of a rich text editor for example, else the element. Calling
 * send(values) tells the server, for the Ajax events: the script fires a private event on the
 * element, which the Ajax behavior of their kind hears of, with the name of the event, its place
 * among the events of the component and the values, as JSON. Without a function, an Ajax event
 * sends the data of what the event told, its detail.
 */

import { getHost } from '../host.js';
import { useWicketHost, wicket } from './host.js';

/** What an event does: stays in the browser, or goes to the server via Ajax, alone or submitting the form. */
export type ClientEventKind = 'client' | 'ajax' | 'submit';

/** Sends the server the values, an object, for an Ajax event; does nothing for the others. */
export type ClientEventSend = (values?: unknown) => void;

/** What the browser runs on an event. */
export type ClientEventFunction = (event: Event, component: unknown, send: ClientEventSend) => void;

/** An event a component listens to. */
export interface ClientEventOptions {
	/** The name of the event in the browser. */
	id: string;
	/** Its place among the events of the component, which the server finds it by. */
	index: number;
	/** What the event does. */
	kind: ClientEventKind;
	/** How long the event waits for the next of its kind, in milliseconds; -1, or none, for not at all. */
	delay?: number;
	/** What runs on the event; without it an Ajax event sends its detail, the others do nothing. */
	fn?: ClientEventFunction;
}

/** The private events the Ajax behaviors of the component hear of, by the kind of the event. */
export const CLIENT_EVENTS_AJAX = 'antilia-client-events:ajax';
export const CLIENT_EVENTS_SUBMIT = 'antilia-client-events:submit';

/** What the private events tell the Ajax behaviors, which send it as the parameters of the request. */
export interface ClientEventRequest {
	event: string;
	index: number;
	values: string;
}

/** The events of an element, which attaching again takes off first. */
export interface AttachedClientEvents {
	/** Stops listening, and drops the events still waiting. */
	detach(): void;
}

const attached = new WeakMap<Element, AttachedClientEvents>();

/**
 * @param value anything
 * @returns whether it is data JSON writes as it is: a plain object or an array, not an editor or
 *          an element
 */
function isData(value: unknown): boolean {
	if (value === null || typeof value !== 'object') {
		return typeof value !== 'function' && typeof value !== 'symbol' &&
			typeof value !== 'bigint';
	}
	if (Array.isArray(value)) {
		return true;
	}
	const prototype = Object.getPrototypeOf(value);
	return prototype === Object.prototype || prototype === null;
}

/**
 * @param detail what an event told, the detail of a custom event
 * @returns its data, as an object: what is not data dropped, the editor of a rich text editor's
 *          events for example; an empty object for none
 */
export function plainValues(detail: unknown): Record<string, unknown> {
	if (detail === null || detail === undefined || !isData(detail)) {
		return {};
	}
	if (typeof detail !== 'object' || Array.isArray(detail)) {
		return { value: detail };
	}
	const values: Record<string, unknown> = {};
	Object.entries(detail as Record<string, unknown>).forEach(([name, value]) => {
		if (isData(value)) {
			values[name] = value;
		}
	});
	return values;
}

/**
 * @param values what a function sends
 * @returns them as an object, what the server takes: a value which is not one wrapped as value
 */
function asObject(values: unknown): Record<string, unknown> {
	if (values === null || values === undefined) {
		return {};
	}
	if (typeof values === 'object' && !Array.isArray(values)) {
		return values as Record<string, unknown>;
	}
	return { value: values };
}

/**
 * Listens to the events of an element, taking off those it listened to before.
 *
 * @param id the markup id of the element
 * @param events the events
 * @param resolve gives what the functions get as the component, from the element; the element
 *        when left out
 * @returns the events attached, null when there is no such element
 */
export function attachClientEvents(id: string, events: ClientEventOptions[],
	resolve?: (element: HTMLElement) => unknown): AttachedClientEvents | null {
	const element = document.getElementById(id);
	if (!element) {
		return null;
	}
	attached.get(element)?.detach();
	const timers = new Map<number, number>();
	const listeners: Array<[string, EventListener]> = [];

	const run = (options: ClientEventOptions, event: Event) => {
		const send: ClientEventSend = (values) => {
			if (options.kind === 'client' || !element.isConnected) {
				return;
			}
			let json: string;
			try {
				json = JSON.stringify(asObject(values));
			} catch (error) {
				getHost().log.error('The values of ' + options.id + ' are not JSON', error);
				return;
			}
			element.dispatchEvent(new CustomEvent<ClientEventRequest>(
				options.kind === 'ajax' ? CLIENT_EVENTS_AJAX : CLIENT_EVENTS_SUBMIT,
				{ bubbles: false, detail: { event: options.id, index: options.index, values: json } }));
		};
		try {
			if (options.fn) {
				options.fn.call(element, event, resolve ? resolve(element) : element, send);
			} else {
				send(plainValues((event as CustomEvent).detail));
			}
		} catch (error) {
			getHost().log.error('The function of ' + options.id + ' failed', error);
		}
	};

	events.forEach((options) => {
		const listener: EventListener = (event) => {
			const delay = options.delay ?? -1;
			if (delay <= 0) {
				run(options, event);
				return;
			}
			// each new event postpones the one waiting, until they pause
			window.clearTimeout(timers.get(options.index));
			timers.set(options.index, window.setTimeout(() => {
				timers.delete(options.index);
				if (element.isConnected) {
					run(options, event);
				}
			}, delay));
		};
		element.addEventListener(options.id, listener);
		listeners.push([options.id, listener]);
	});

	const handle: AttachedClientEvents = {
		detach() {
			listeners.forEach(([type, listener]) => element.removeEventListener(type, listener));
			listeners.length = 0;
			timers.forEach((timer) => window.clearTimeout(timer));
			timers.clear();
			if (attached.get(element) === handle) {
				attached.delete(element);
			}
		}
	};
	attached.set(element, handle);
	return handle;
}

const WicketClientEvents = {
	/** Listens to the events of a component: see attachClientEvents(). */
	attach: attachClientEvents,

	/**
	 * Stops listening to the events of a component.
	 *
	 * @param id the markup id of its element
	 */
	detach(id: string): void {
		const element = document.getElementById(id);
		if (element) {
			attached.get(element)?.detach();
		}
	},

	/** The data of what an event told: see plainValues(). */
	plainValues
};

const Wicket = wicket();
if (Wicket.ClientEvents === undefined) {
	useWicketHost();
	Wicket.ClientEvents = WicketClientEvents;
}
