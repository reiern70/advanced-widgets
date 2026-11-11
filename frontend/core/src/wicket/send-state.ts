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
 */

interface WicketAjax {
	post(attributes: Record<string, unknown>): void;
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
 * Sends the server the state and maybe the text: via Ajax, or in a beacon as the page goes, which
 * Wicket takes as Ajax by its parameters.
 *
 * @param id the markup id of the component, the channel of the request
 * @param url the URL of the component's Ajax behavior
 * @param parameters what the request carries
 * @param leaving whether the page is going
 */
export function sendState(id: string, url: string, parameters: Parameter[], leaving: boolean): void {
	if (leaving && sendBeacon(url, parameters)) {
		return;
	}
	// in the channel of the page's other requests, after them: the state and the text the editor
	// had as the user went on, to a check box changing the options for example
	const ajax = window.Wicket?.Ajax as WicketAjax | undefined;
	ajax?.post({ u: url, c: id, ep: parameters });
}

/** An editor of Wicket, which takes its state to send. */
interface StateSource {
	takeState(): { state: unknown; changed: boolean };
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
	return { 'antilia-state': JSON.stringify(state), 'antilia-text-changed': String(changed) };
}
