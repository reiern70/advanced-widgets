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
 * antilia-page-loading-veil.js: lifts the veil PageLoadingVeilBehavior draws over a page while it
 * loads (antilia-page-loading-veil.css), by taking the class antilia-page-loading away, once the
 * page's scripts have run: Wicket's domready handlers, in which the components of the page are
 * initialized (each component's OnDomReadyHeaderItem, all of them in one handler).
 *
 * The behavior loads it deferred, so that it holds up neither the parsing nor the first paint. It
 * waits for DOMContentLoaded on the window, which the event reaches after the listeners of the
 * document: Wicket's domready handlers, in Wicket's Ajax in plain JavaScript, listen there and
 * have run by then. It then hands the lifting to Wicket.Event.add(window, 'domready'), which runs
 * it at once in plain JavaScript, and with jQuery after the ready handlers jQuery has queued. A
 * page without Wicket.Event has no domready handlers to wait for.
 *
 * Registers Wicket.PageLoadingVeil with lift(), for a page that wants the veil away sooner.
 */

import { wicket } from './host.js';

/** The class veiling the element carrying it: PageLoadingVeilBehavior.CSS_CLASS. */
export const CSS_CLASS = 'antilia-page-loading';

interface WicketEvents {
	add?(element: Window, type: string, listener: () => void): unknown;
}

/**
 * Lifts the veil: takes the class away from every element carrying it, the page's <body> or
 * <html>.
 *
 * @param root the document
 * @returns how many elements were veiled
 */
export function lift(root: Document = document): number {
	const veiled = Array.from(root.querySelectorAll('.' + CSS_CLASS));
	veiled.forEach((element) => element.classList.remove(CSS_CLASS));
	return veiled.length;
}

/**
 * Calls back once Wicket's domready handlers have run, while or after the document's own
 * DOMContentLoaded listeners ran.
 *
 * @param callback called once
 */
function afterDomReady(callback: () => void): void {
	const events = window.Wicket?.Event as WicketEvents | undefined;
	if (typeof events?.add !== 'function') {
		callback();
		return;
	}
	try {
		events.add(window, 'domready', callback);
	} catch {
		// the engine of Wicket's Ajax failed, jQuery missing for example: no handler to wait for
		callback();
	}
}

/**
 * Lifts the veil once the page's scripts have run: after DOMContentLoaded and Wicket's domready
 * handlers, or at the latest on load, should the script run only after DOMContentLoaded; at once
 * when the page has loaded already.
 *
 * @returns stops waiting, for the tests
 */
export function install(): () => void {
	let done = false;
	const ready = () => {
		if (!done) {
			done = true;
			stop();
			afterDomReady(() => lift());
		}
	};
	const stop = () => {
		window.removeEventListener('DOMContentLoaded', ready);
		window.removeEventListener('load', ready);
	};
	if (document.readyState === 'complete') {
		ready();
		return stop;
	}
	// on the window, reached after the listeners of the document, Wicket's domready handlers
	window.addEventListener('DOMContentLoaded', ready);
	window.addEventListener('load', ready);
	return () => {
		done = true;
		stop();
	};
}

/** Wicket.PageLoadingVeil. */
export const WicketPageLoadingVeil = {
	lift: () => lift(),
	/** The class veiling the element carrying it. */
	cssClass: CSS_CLASS
};

const Wicket = wicket();
if (Wicket.PageLoadingVeil === undefined) {
	Wicket.PageLoadingVeil = WicketPageLoadingVeil;
	install();
}
