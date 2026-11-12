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
 * index, and what the event tells besides, the delay of the events having one for example. On
 * each event the element fires, the script waits the delay of the event, if any, for the next of
 * its kind, then runs the handler of the event, function(event, component, options, send): its
 * code, eventHandlerClientSideCode, or else the function of the page named
 * eventClientHandlerName, a global or a path to one, which the script finds as the event comes.
 * component is what resolve(element) gives, the editor of a rich text editor for example, else
 * the element; options, the JSON of the event, by which its properties configure the handler;
 * send, last, which a handler staying in the browser leaves out.
 *
 * That is all the script knows of an event: one going to the server comes with ajax, the
 * attributes of its requests, those of Wicket's Ajax, which the server rendered: the URL, the
 * method, GET or POST, the form submitted, if any, the extra parameters, the dynamic extra
 * parameters and the listeners of the call, on success and on failure among them. Calling
 * send(values) makes a round trip of Wicket's Ajax with them (roundTrip()), carrying the name of
 * the event, its place among the events of the component, the values, as JSON, and what
 * options.values(component) gives besides, the state of an editor for example; send(values,
 * attributes) adds attributes of the handler's own, listeners of the call for example. An event
 * without ajax stays in the browser: send does nothing. An event telling leaving: true in its
 * detail goes in a beacon instead, which the browser sends even as the page goes.
 *
 * Each of the page's other Ajax requests carries what options.pending(component) gives, what the
 * component did not send yet, the state of an editor the user scrolled without focusing it for
 * example, as the parameter antilia-pending:<markup id>, JSON.
 *
 * Three handlers are built in, by their names: Wicket.ClientEvents.send, sending the data of what
 * the event told, its detail; Wicket.ClientEvents.sendState, doing so only when the component has
 * a state not sent yet (hasUnsentState()), an editor's; and Wicket.ClientEvents.nothing.
 */

import { getHost } from '../host.js';
import { onWicketTopic, useWicketHost, wicket } from './host.js';
import { sendBeacon, STATE_PARAMETER, TEXT_CHANGED_PARAMETER, type Parameter } from './send-state.js';

/**
 * The attributes of a request of Wicket's Ajax, as the server renders them for an event going to
 * it, and as a handler adds to them.
 */
export interface AjaxAttributes {
	/** The URL of the Ajax behavior. */
	u?: string;
	/** The method: GET, or POST. */
	m?: string;
	/** The markup id of the component, whose field Wicket sends with the request. */
	c?: unknown;
	/** The markup id of the form the request submits. */
	f?: string;
	/** The name of the component submitting the form. */
	sc?: string;
	/** Whether Wicket sends the fields within the component too. */
	sr?: boolean;
	/** The extra parameters: values by their names, or an array of name and value. */
	ep?: unknown;
	/** The dynamic extra parameters: functions of the attributes giving parameters. */
	dep?: unknown[];
	/** The rest of Wicket's attributes: the listeners of the call, sh and fh among them. */
	[attribute: string]: unknown;
}

/**
 * Sends the server the values, an object, for an event going to the server, by a round trip of
 * Wicket's Ajax; the attributes, if any, add to those of the event: listeners of the call, extra
 * parameters, or another method. Does nothing for an event staying in the browser.
 */
export type ClientEventSend = (values?: unknown, attributes?: AjaxAttributes) => void;

/** What the browser runs on an event: options is the JSON of the event, its properties. */
export type ClientEventFunction = (event: Event, component: unknown, options: ClientEventOptions,
	send: ClientEventSend) => void;

/** An event a component listens to: the JSON of its properties. */
export interface ClientEventOptions {
	/** The name of the event in the browser. */
	eventName: string;
	/** Its place among the events of the component, which the server finds it by. */
	index: number;
	/** How long the event waits for the next of its kind, in milliseconds; -1, or none, for not at all. */
	delay?: number;
	/** What runs on the event, in place of the function named by eventClientHandlerName. */
	eventHandlerClientSideCode?: ClientEventFunction;
	/** The name of the function of the page that runs on the event: a global, or a path to one. */
	eventClientHandlerName?: string;
	/** The attributes of the requests of an event going to the server; none for one staying in the browser. */
	ajax?: AjaxAttributes;
	/** What else the event tells its handler. */
	[property: string]: unknown;
}

/** What the component gives the events besides. */
export interface ClientEventsOptions {
	/**
	 * Gives the parameters, strings by their names, each request of an event carries besides its
	 * own: the state of an editor, for example.
	 */
	values?: (component: unknown) => Record<string, string> | null | undefined;
	/**
	 * Gives the parameters, strings by their names, the component did not send yet, which the
	 * page's other Ajax requests carry, taking them as sent: the state of an editor the user
	 * scrolled without focusing it, for example. Null, or nothing, for none.
	 */
	pending?: (component: unknown) => Record<string, string> | null | undefined;
}

/** The parameters of the requests of the events, as the server reads them. */
const EVENT_PARAMETER = 'antilia-event';
const INDEX_PARAMETER = 'antilia-event-index';
const VALUES_PARAMETER = 'antilia-event-values';

/**
 * The prefix of the parameter of the page's Ajax requests holding what a component did not send
 * yet, followed by its markup id.
 */
export const PENDING_PARAMETER_PREFIX = 'antilia-pending:';

/**
 * The attributes of Wicket's Ajax holding lists, which those a handler adds add to: the dynamic
 * extra parameters, the preconditions and the listeners of the call.
 */
const LIST_ATTRIBUTES = ['dep', 'pre', 'ih', 'bh', 'bsh', 'ah', 'sh', 'fh', 'coh', 'dh'];

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

/** A call of Wicket's Ajax. */
interface WicketCall {
	ajax(attributes: AjaxAttributes): unknown;
	_executeHandlers?(handlers: unknown, attributes: AjaxAttributes): void;
}

/** Wicket's Ajax, making calls. */
interface WicketAjaxCalls {
	Call?: new () => WicketCall;
}

/** Wicket's events, which tell the listeners of a topic. */
interface WicketEvents {
	publish?(topic: string, ...payload: unknown[]): void;
	Topic?: Record<string, string>;
}

const attached = new WeakMap<Element, AttachedClientEvents>();

/** A component giving what it did not send yet to the page's other Ajax requests. */
interface PendingSource {
	element: HTMLElement;
	resolve?: ((element: HTMLElement) => unknown) | null;
	pending: (component: unknown) => Record<string, string> | null | undefined;
}

const pendingSources = new Set<PendingSource>();

let carryingPending = false;

/**
 * @param own the markup id of the component the request is of, whose events send their own
 * @returns what the components of the page did not send yet, each as the parameter
 *          antilia-pending:<markup id>, JSON
 */
function pendingValues(own: string | undefined): Record<string, string> {
	const values: Record<string, string> = {};
	pendingSources.forEach((source) => {
		const element = source.element;
		if (!element.isConnected) {
			// replaced by Wicket's Ajax without being detached
			pendingSources.delete(source);
			return;
		}
		if (element.id === own) {
			return;
		}
		try {
			const pending = source.pending(source.resolve ? source.resolve(element) : element);
			if (pending && Object.keys(pending).length > 0) {
				values[PENDING_PARAMETER_PREFIX + element.id] = JSON.stringify(pending);
			}
		} catch (error) {
			getHost().log.error('What ' + element.id + ' did not send is not JSON', error);
		}
	});
	return values;
}

/**
 * Makes each Ajax request of the page carry what the components did not send yet: a dynamic
 * parameter, asked as Wicket's Ajax sends the request, after its preconditions.
 */
function carryPending(): void {
	if (carryingPending || !wicket().Event?.subscribe) {
		return;
	}
	carryingPending = true;
	onWicketTopic<AjaxAttributes>('AJAX_CALL_BEFORE', '/ajax/call/before', (attrs) => {
		if (!attrs || typeof attrs !== 'object' || pendingSources.size === 0) {
			return;
		}
		const component = attrs.c as { id?: unknown } | string | undefined;
		const own = typeof component === 'string' ? component :
			typeof component?.id === 'string' ? component.id : undefined;
		// the attributes of this request alone: Wicket's Ajax copies them for each
		attrs.dep = [...(Array.isArray(attrs.dep) ? attrs.dep : []), () => pendingValues(own)];
	});
}

/**
 * @param parameters parameters as Wicket's Ajax takes them: values by their names, an array of
 *        values for a name having several, or an array of name and value
 * @returns them as an array of name and value, the values as strings, those that are null or
 *          undefined left out
 */
export function asParameters(parameters: unknown): Parameter[] {
	const list: Parameter[] = [];
	if (Array.isArray(parameters)) {
		parameters.forEach((parameter) => {
			const { name, value } = (parameter ?? {}) as { name?: unknown; value?: unknown };
			if (typeof name === 'string' && value !== null && value !== undefined) {
				list.push({ name, value: String(value) });
			}
		});
	} else if (parameters !== null && typeof parameters === 'object') {
		Object.entries(parameters as Record<string, unknown>).forEach(([name, value]) => {
			(Array.isArray(value) ? value : [value]).forEach((each) => {
				if (each !== null && each !== undefined) {
					list.push({ name, value: String(each) });
				}
			});
		});
	}
	return list;
}

/**
 * @param attributes the attributes of an event's requests
 * @param added those a handler adds
 * @returns the attributes of the request: those added in place of the event's, but the extra
 *          parameters, the dynamic ones and the listeners of the call, which add to the event's
 */
function merged(attributes: AjaxAttributes, added?: AjaxAttributes): AjaxAttributes {
	const all: AjaxAttributes = { ...attributes };
	if (!added) {
		return all;
	}
	Object.entries(added).forEach(([name, value]) => {
		if (name === 'ep') {
			all.ep = [...asParameters(attributes.ep), ...asParameters(value)];
		} else if (LIST_ATTRIBUTES.includes(name)) {
			const before = attributes[name];
			all[name] = [...(Array.isArray(before) ? before : before === undefined ? [] : [before]),
				...(Array.isArray(value) ? value : [value])];
		} else {
			all[name] = value;
		}
	});
	return all;
}

/**
 * @param attributes the attributes of a request
 * @returns the parameters its dynamic extra parameters give, functions of the attributes, as
 *          Wicket's Ajax asks them
 */
function dynamicParameters(attributes: AjaxAttributes): Parameter[] {
	const list: Parameter[] = [];
	(attributes.dep ?? []).forEach((parameter) => {
		if (typeof parameter === 'function') {
			list.push(...asParameters((parameter as (attrs: AjaxAttributes) => unknown)(attributes)));
		}
	});
	return list;
}

/**
 * @param attributes the attributes of a request
 * @returns what Wicket's Ajax would send of the page with it: the form it submits, with the
 *          component submitting it, or else the field of the component
 */
function serialized(attributes: AjaxAttributes): Parameter[] {
	const form = wicket().Form as WicketForm | undefined;
	if (typeof attributes.f === 'string') {
		const element = document.getElementById(attributes.f);
		const fields = element && form?.serializeForm ? form.serializeForm(element) : [];
		return typeof attributes.sc === 'string' ?
			[...fields, { name: attributes.sc, value: '1' }] : fields;
	}
	const element = typeof attributes.c === 'string' ? document.getElementById(attributes.c) : null;
	if (!element) {
		return [];
	}
	if (form?.serializeElement) {
		return form.serializeElement(element, attributes.sr === true);
	}
	const field = element as HTMLElement & { name?: string; value?: unknown };
	return field.name && typeof field.value === 'string' ?
		[{ name: field.name, value: field.value }] : [];
}

/**
 * Sends a request as Wicket's Ajax would, in a beacon, which the browser sends even as the page
 * goes: its extra parameters, the parameters, its dynamic extra parameters and what Wicket would
 * send of the page.
 *
 * @param attributes the attributes of the request
 * @param parameters the parameters it carries besides
 * @returns whether the browser took the beacon
 */
function beacon(attributes: AjaxAttributes, parameters: Parameter[]): boolean {
	if (typeof attributes.u !== 'string') {
		return false;
	}
	return sendBeacon(attributes.u, [...asParameters(attributes.ep), ...parameters,
		...dynamicParameters(attributes), ...serialized(attributes)]);
}

/**
 * Makes a round trip of Wicket's Ajax: a request with the attributes, carrying the parameters
 * besides their extra parameters, as Wicket's Ajax sends the request of an event it listens to,
 * its init handlers first.
 *
 * @param attributes the attributes of the request, those of an event going to the server: its URL,
 *        its method, its extra parameters, its dynamic extra parameters and the listeners of the call
 * @param parameters what the request carries besides
 * @returns whether the request went: not without Wicket's Ajax, nor without a URL
 */
export function roundTrip(attributes: AjaxAttributes, parameters: Parameter[] = []): boolean {
	const ajax = wicket().Ajax as WicketAjaxCalls | undefined;
	if (!ajax?.Call || typeof attributes.u !== 'string') {
		getHost().log.error('No Ajax of Wicket to send a request to ' + attributes.u);
		return false;
	}
	const attrs: AjaxAttributes = { ...attributes,
		ep: [...asParameters(attributes.ep), ...parameters] };
	const call = new ajax.Call();
	call._executeHandlers?.(attrs.ih, attrs);
	const events = wicket().Event as WicketEvents | undefined;
	events?.publish?.(events.Topic?.AJAX_CALL_INIT ?? '/ajax/call/init', attrs);
	call.ajax(attrs);
	return true;
}

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
function sendDetail(event: Event, _component: unknown, _options: ClientEventOptions,
	send: ClientEventSend): void {
	send(plainValues((event as CustomEvent).detail));
}

/**
 * Sends the data of what the event told when the component has a state not sent yet, as it tells
 * by hasUnsentState(); always for a component without.
 */
function sendState(event: Event, component: unknown, options: ClientEventOptions,
	send: ClientEventSend): void {
	const unsent = (component as { hasUnsentState?: () => boolean } | null)?.hasUnsentState;
	if (typeof unsent === 'function' && !unsent.call(component)) {
		return;
	}
	sendDetail(event, component, options, send);
}

/** Does nothing. */
function nothing(): void {
	// an event of a name made to do nothing
}

/**
 * Listens to the events of an element, taking off those it listened to before.
 *
 * @param id the markup id of the element
 * @param events the events
 * @param resolve gives what the handlers get as the component, from the element; the element
 *        when left out or null
 * @param options the parameters the component adds to the requests
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
		const send: ClientEventSend = (values, added) => {
			if (!event.ajax || !element.isConnected) {
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
			const parameters: Parameter[] = Object.entries(extra)
				.map(([name, value]) => ({ name, value: String(value) }));
			parameters.push({ name: EVENT_PARAMETER, value: event.eventName },
				{ name: INDEX_PARAMETER, value: String(event.index) },
				{ name: VALUES_PARAMETER, value: json });
			const attributes = merged(event.ajax, added);
			if (isLeaving(fired) && beacon(attributes, parameters)) {
				return;
			}
			roundTrip(attributes, parameters);
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
			handler.call(element, fired, component, event, send);
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

	const pending: PendingSource | null = options.pending ?
		{ element, resolve, pending: options.pending } : null;
	if (pending) {
		pendingSources.add(pending);
		carryPending();
	}

	const handle: AttachedClientEvents = {
		detach() {
			listeners.forEach(([type, listener]) => element.removeEventListener(type, listener));
			listeners.length = 0;
			timers.forEach((timer) => window.clearTimeout(timer));
			timers.clear();
			if (pending) {
				pendingSources.delete(pending);
			}
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

	/** A round trip of Wicket's Ajax with the attributes of a request: see roundTrip(). */
	roundTrip,

	/** The built-in handler sending the data of what the event told. */
	send: sendDetail,

	/** The built-in handler sending it when the component has a state not sent yet. */
	sendState,

	/** The built-in handler doing nothing. */
	nothing,

	/** The names of the parameters of the requests, as the server reads them. */
	parameters: Object.freeze({
		/** The name of the event: ClientEventsBehavior.EVENT_PARAMETER. */
		event: EVENT_PARAMETER,
		/** Its place among the events of the component: INDEX_PARAMETER. */
		index: INDEX_PARAMETER,
		/** The values the handler sent, JSON: VALUES_PARAMETER. */
		values: VALUES_PARAMETER,
		/** The state of an editor, JSON: SendStateEvent.STATE_PARAMETER. */
		state: STATE_PARAMETER,
		/** Whether its text changed: SendStateEvent.TEXT_CHANGED_PARAMETER. */
		textChanged: TEXT_CHANGED_PARAMETER,
		/** Followed by a markup id, what the component did not send: PENDING_PARAMETER_PREFIX. */
		pendingPrefix: PENDING_PARAMETER_PREFIX
	})
};

const Wicket = wicket();
if (Wicket.ClientEvents === undefined) {
	useWicketHost();
	Wicket.ClientEvents = WicketClientEvents;
}
