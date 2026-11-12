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
 * How the components of Wicket send the server what they keep in the browser: via Ajax, or in a
 * beacon as the page goes, which the browser sends even then. The editors send their state with
 * each of their events going to the server (stateParameters()), by Wicket.ClientEvents.
 *
 * A request a script sends by itself goes with the attributes the server rendered for it, those of
 * Wicket's Ajax (roundTrip()): the extra parameters a page adds, the one a page veil leaves the
 * request alone by among them, go with it.
 */

import { getHost } from '../host.js';
import { wicket } from './host.js';

interface WicketAjax {
	baseUrl?: string;
}

/** A parameter of a request, as Wicket.Ajax takes them. */
export interface Parameter {
	name: string;
	value: string;
}

/**
 * Sends the server what a request of Wicket's Ajax carries, in a beacon, which the browser sends
 * even as the page goes: Wicket takes it as Ajax by its parameters.
 *
 * @param url the URL of the component's Ajax behavior
 * @param parameters what the request carries
 * @returns whether the browser took the beacon: not without beacons
 */
export function sendBeacon(url: string, parameters: Parameter[]): boolean {
	if (typeof navigator.sendBeacon !== 'function') {
		return false;
	}
	const ajax = window.Wicket?.Ajax as WicketAjax | undefined;
	const body = new URLSearchParams();
	parameters.forEach((parameter) => body.append(parameter.name, parameter.value));
	body.append('wicket-ajax-baseurl', ajax?.baseUrl ?? '');
	return navigator.sendBeacon(url + (url.includes('?') ? '&' : '?') + 'wicket-ajax=true', body);
}

/**
 * The attributes of a request of Wicket's Ajax, as the server renders them, by
 * AbstractDefaultAjaxBehavior#renderAjaxAttributes, and as a script adds to them.
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
 * Makes a round trip of Wicket's Ajax: a request with the attributes, carrying the parameters
 * besides their extra parameters, as Wicket's Ajax sends the request of an event it listens to,
 * its init handlers first.
 *
 * @param attributes the attributes of the request, as the server rendered them: its URL, its
 *        method, its extra parameters, its dynamic extra parameters and the listeners of the call
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
 * The parameter of the requests of an editor's events holding its state, as JSON: SendStateEvent's
 * STATE_PARAMETER on the server. Before 10.11.0-RC7 the editors sent it as state, by a request of
 * their own.
 */
export const STATE_PARAMETER = 'antilia-state';

/**
 * The parameter of the requests of an editor's events telling whether the text changed since it
 * was last sent: SendStateEvent's TEXT_CHANGED_PARAMETER on the server.
 */
export const TEXT_CHANGED_PARAMETER = 'antilia-text-changed';

/** An editor of Wicket, which takes its state to send. */
interface StateSource {
	takeState(): { state: unknown; changed: boolean };
	takeUnsentState?(): unknown;
}

/**
 * @param editor an editor, null before its script made it
 * @returns the parameters its events carry to the server besides their own: the state, as JSON,
 *          and whether the text changed since it was last sent, which the server then takes from
 *          the text area; none without an editor
 */
export function stateParameters(editor: StateSource | null | undefined): Record<string, string> {
	if (!editor || typeof editor.takeState !== 'function') {
		return {};
	}
	const { state, changed } = editor.takeState();
	return { [STATE_PARAMETER]: JSON.stringify(state), [TEXT_CHANGED_PARAMETER]: String(changed) };
}

/**
 * @param editor an editor, null before its script made it
 * @returns the parameters the page's other Ajax requests carry for it: the state, as JSON, when
 *          the editor did not send it yet, without the text, which those requests do not send;
 *          null for none
 */
export function pendingParameters(editor: StateSource | null | undefined):
	Record<string, string> | null {
	if (!editor || typeof editor.takeUnsentState !== 'function') {
		return null;
	}
	const state = editor.takeUnsentState();
	return state === null || state === undefined ? null :
		{ [STATE_PARAMETER]: JSON.stringify(state) };
}
