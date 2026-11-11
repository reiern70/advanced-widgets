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
 * ui-components, which calls Wicket.ClientEvents.attach(id, events, resolve, options) with the
 * events the component listens to.
 *
 * Each event comes as the JSON of its properties, which IClientEvent.toJson() writes: eventName,
 * index, kind, and what the event tells besides, the delay of the events having one for example.
 * On each event the element fires, the script waits the delay of the event, if any, for the next
 * of its kind, then runs the handler of the event, function(event, component, send, options): its
 * code, eventHandlerClientSideCode, or else the function of the page named
 * eventClientHandlerName, a global or a path to one, which the script finds as the event comes.
 * component is what resolve(element) gives, the editor of a rich text editor for example, else
 * the element; options, the JSON of the event, by which its properties configure the handler.
 * Calling send(values) tells the server, for the Ajax events: the
 * script fires a private event on the element, which the Ajax behavior of their kind hears of,
 * with the name of the event, its place among the events of the component, the values, as JSON,
 * and what options.values(component) gives besides, the state of an editor for example. An event
 * telling leaving: true in its detail goes in a beacon instead, which the browser sends even as
 * the page goes.
 *
 * Three handlers are built in, by their names: Wicket.ClientEvents.send, sending the data of what
 * the event told, its detail; Wicket.ClientEvents.sendState, doing so only when the component has
 * a state not sent yet (hasUnsentState()), an editor's; and Wicket.ClientEvents.nothing.
 */

import { getHost } from '../host.js';
import { useWicketHost, wicket } from './host.js';
import { sendBeacon, type Parameter } from './send-state.js';

/** What an event does: stays in the browser, or goes to the server via Ajax, alone or submitting the form. */
export type ClientEventKind = 'client' | 'ajax' | 'submit';

/** Sends the server the values, an object, for an Ajax event; does nothing for the others. */
export type ClientEventSend = (values?: unknown) => void;

/** What the browser runs on an event: options is the JSON of the event, its properties. */
export type ClientEventFunction = (event: Event, component: unknown, send: ClientEventSend,
	options: ClientEventOptions) => void;

/** An event a component listens to: the JSON of its properties. */
export interface ClientEventOptions {
	/** The name of the event in the browser. */
	eventName: string;
	/** Its place among the events of the component, which the server finds it by. */
	index: number;
	/** What the event does. */
	kind: ClientEventKind;
	/** How long the event waits for the next of its kind, in milliseconds; -1, or none, for not at all. */
	delay?: number;
	/** What runs on the event, in place of the function named by eventClientHandlerName. */
	eventHandlerClientSideCode?: ClientEventFunction;
	/** The name of the function of the page that runs on the event: a global, or a path to one. */
	eventClientHandlerName?: string;
	/** What else the event tells its handler. */
	[property: string]: unknown;
}

/** What the component gives the events besides. */
export interface ClientEventsOptions {
	/** The URL of the Ajax behavior of the Ajax events, which a beacon goes to as the page goes. */
	ajaxUrl?: string;
	/** The URL of the Ajax behavior of the events submitting the form, for a beacon too. */
	submitUrl?: string;
	/**
	 * Gives the parameters, strings by their names, each request of an event carries besides its
	 * own: the state of an editor, for example.
	 */
	values?: (component: unknown) => Record<string, string> | null | undefined;
}

/** The private events the Ajax behaviors of the component hear of, by the kind of the event. */
export const CLIENT_EVENTS_AJAX = 'antilia-client-events:ajax';
export const CLIENT_EVENTS_SUBMIT = 'antilia-client-events:submit';

/** The parameters of the requests of the events, as the server reads them. */
const EVENT_PARAMETER = 'antilia-event';
const INDEX_PARAMETER = 'antilia-event-index';
const VALUES_PARAMETER = 'antilia-event-values';

/** What the private events tell the Ajax behaviors, which send it as the parameters of the request. */
export interface ClientEventRequest {
	event: string;
	index: number;
	values: string;
	/** The parameters the component adds. */
	extra?: Record<string, string>;
}

/** The events of an element, which attaching again takes off first. */
export interface AttachedClientEvents {
	/** Stops listening, and drops the events still waiting. */
	detach(): void;
}

/** What Wicket's Ajax serializes forms with. */
interface WicketForm {
	serializeElement?(element: Element, recursive?: boolean): Parameter[];
	serializeForm?(form: Element): Parameter[];
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
 * @param values what a handler sends
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
 * @param name the name of a function: a global, or a path to one, Articles.showModified for example
 * @returns the function, null when there is none of the name
 */
export function functionNamed(name: string): ClientEventFunction | null {
	let value: unknown = window;
	for (const part of name.split('.')) {
		if (value === null || value === undefined) {
			return null;
		}
		value = (value as Record<string, unknown>)[part];
	}
	return typeof value === 'function' ? value as ClientEventFunction : null;
}

/**
 * @param event an event
 * @returns whether it tells the page is going: leaving, in its detail
 */
function isLeaving(event: Event): boolean {
	const detail = (event as CustomEvent).detail as { leaving?: unknown } | null | undefined;
	return !!detail && typeof detail === 'object' && detail.leaving === true;
}

/** Sends the data of what the event told, its detail. */
function sendDetail(event: Event, _component: unknown, send: ClientEventSend): void {
	send(plainValues((event as CustomEvent).detail));
}

/**
 * Sends the data of what the event told when the component has a state not sent yet, as it tells
 * by hasUnsentState(); always for a component without.
 */
function sendState(event: Event, component: unknown, send: ClientEventSend): void {
	const unsent = (component as { hasUnsentState?: () => boolean } | null)?.hasUnsentState;
	if (typeof unsent === 'function' && !unsent.call(component)) {
		return;
	}
	sendDetail(event, component, send);
}

/** Does nothing. */
function nothing(): void {
	// an event of a name made to do nothing
}

/**
 * @param element an element in a form, or not
 * @returns its outermost form, which Wicket submits, null for none
 */
function rootForm(element: Element): HTMLFormElement | null {
	let form = element.closest('form');
	while (form?.parentElement?.closest('form')) {
		form = form.parentElement.closest('form');
	}
	return form;
}

/**
 * @param element the element of the component
 * @param kind the kind of the event
 * @returns what Wicket's Ajax would send of the page: the value of the element, a field, and the
 *          form for an event submitting it
 */
function serialized(element: HTMLElement, kind: ClientEventKind): Parameter[] {
	const form = wicket().Form as WicketForm | undefined;
	if (kind === 'submit') {
		const root = rootForm(element);
		return root && form?.serializeForm ? form.serializeForm(root) : [];
	}
	if (form?.serializeElement) {
		return form.serializeElement(element, false);
	}
	const field = element as HTMLElement & { name?: string; value?: unknown };
	return field.name && typeof field.value === 'string' ?
		[{ name: field.name, value: field.value }] : [];
}

/**
 * Listens to the events of an element, taking off those it listened to before.
 *
 * @param id the markup id of the element
 * @param events the events
 * @param resolve gives what the handlers get as the component, from the element; the element
 *        when left out or null
 * @param options the URLs the beacons go to and the parameters the component adds to the requests
 * @returns the events attached, null when there is no such element
 */
export function attachClientEvents(id: string, events: ClientEventOptions[],
	resolve?: ((element: HTMLElement) => unknown) | null,
	options: ClientEventsOptions = {}): AttachedClientEvents | null {
	const element = document.getElementById(id);
	if (!element) {
		return null;
	}
	attached.get(element)?.detach();
	const timers = new Map<number, number>();
	const listeners: Array<[string, EventListener]> = [];

	const run = (event: ClientEventOptions, fired: Event) => {
		const component = resolve ? resolve(element) : element;
		const send: ClientEventSend = (values) => {
			if (event.kind === 'client' || !element.isConnected) {
				return;
			}
			let json: string;
			let extra: Record<string, string> = {};
			try {
				json = JSON.stringify(asObject(values));
				extra = options.values?.(component) ?? {};
			} catch (error) {
				getHost().log.error('The values of ' + event.eventName + ' are not JSON', error);
				return;
			}
			const url = event.kind === 'ajax' ? options.ajaxUrl : options.submitUrl;
			if (isLeaving(fired) && url) {
				const parameters: Parameter[] = Object.entries(extra)
					.map(([name, value]) => ({ name, value: String(value) }));
				parameters.push({ name: EVENT_PARAMETER, value: event.eventName },
					{ name: INDEX_PARAMETER, value: String(event.index) },
					{ name: VALUES_PARAMETER, value: json },
					...serialized(element, event.kind));
				if (sendBeacon(url, parameters)) {
					return;
				}
			}
			element.dispatchEvent(new CustomEvent<ClientEventRequest>(
				event.kind === 'ajax' ? CLIENT_EVENTS_AJAX : CLIENT_EVENTS_SUBMIT,
				{ bubbles: false, detail: { event: event.eventName, index: event.index,
					values: json, extra } }));
		};
		try {
			const name = event.eventClientHandlerName;
			const handler = typeof event.eventHandlerClientSideCode === 'function' ?
				event.eventHandlerClientSideCode : (name ? functionNamed(name) : null);
			if (!handler) {
				getHost().log.error('The handler of ' + event.eventName + ', ' + name +
					', is no function of the page');
				return;
			}
			handler.call(element, fired, component, send, event);
		} catch (error) {
			getHost().log.error('The handler of ' + event.eventName + ' failed', error);
		}
	};

	events.forEach((event) => {
		const listener: EventListener = (fired) => {
			const delay = event.delay ?? -1;
			if (delay <= 0) {
				run(event, fired);
				return;
			}
			// each new event postpones the one waiting, until they pause
			window.clearTimeout(timers.get(event.index));
			timers.set(event.index, window.setTimeout(() => {
				timers.delete(event.index);
				if (element.isConnected) {
					run(event, fired);
				}
			}, delay));
		};
		element.addEventListener(event.eventName, listener);
		listeners.push([event.eventName, listener]);
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
	plainValues,

	/** The built-in handler sending the data of what the event told. */
	send: sendDetail,

	/** The built-in handler sending it when the component has a state not sent yet. */
	sendState,

	/** The built-in handler doing nothing. */
	nothing
};

const Wicket = wicket();
if (Wicket.ClientEvents === undefined) {
	useWicketHost();
	Wicket.ClientEvents = WicketClientEvents;
}
