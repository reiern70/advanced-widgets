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
 * How the editors of Wicket send the server their state, and their text with it: via Ajax, or in
 * a beacon as the page goes, which the browser sends even then.
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
 * Sends the server the state and maybe the text: via Ajax, or in a beacon as the page goes, which
 * Wicket takes as Ajax by its parameters.
 *
 * @param id the markup id of the component, the channel of the request
 * @param url the URL of the component's Ajax behavior
 * @param parameters what the request carries
 * @param leaving whether the page is going
 */
export function sendState(id: string, url: string, parameters: Parameter[], leaving: boolean): void {
	const ajax = window.Wicket?.Ajax as WicketAjax | undefined;
	if (leaving && typeof navigator.sendBeacon === 'function') {
		const body = new URLSearchParams();
		parameters.forEach((parameter) => body.append(parameter.name, parameter.value));
		body.append('wicket-ajax-baseurl', ajax?.baseUrl ?? '');
		navigator.sendBeacon(url + (url.includes('?') ? '&' : '?') + 'wicket-ajax=true', body);
		return;
	}
	// in the channel of the page's other requests, after them: the state and the text the editor
	// had as the user went on, to a check box changing the options for example
	ajax?.post({ u: url, c: id, ep: parameters });
}
