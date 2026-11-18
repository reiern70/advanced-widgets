/// <reference types="vite/client" />
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

import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { CSS_CLASS, install, lift,
	WicketPageLoadingVeil } from '../src/wicket/page-loading-veil.js';
import veil from '../styles/page-loading-veil.css?raw';
import noscript from '../styles/page-loading-veil-noscript.css?raw';

type Listener = () => void;

/** The handlers given to the fake Wicket.Event.add for domready, in their order. */
let domready: Listener[];

/** What the handlers saw, in the order they ran. */
let seen: string[];

/** Stops the waiting of the install() of a test. */
let stop: (() => void) | undefined;

/** Makes the document tell the given readyState. */
function readyState(state: DocumentReadyState): void {
	Object.defineProperty(document, 'readyState', { configurable: true, get: () => state });
}

/** @returns whether the page is veiled */
function veiled(): boolean {
	return document.body.classList.contains(CSS_CLASS) ||
		document.documentElement.classList.contains(CSS_CLASS);
}

/**
 * Fakes Wicket.Event.add for domready, as Wicket's Ajax in plain JavaScript does: at once once the
 * document is no longer loading, on DOMContentLoaded of the document before.
 */
function plainWicketEvent(): void {
	(window.Wicket as Record<string, unknown>).Event = {
		add(_element: Window, type: string, listener: Listener) {
			expect(type).toBe('domready');
			domready.push(listener);
			if (document.readyState !== 'loading') {
				listener();
			} else {
				document.addEventListener('DOMContentLoaded', () => listener(), { once: true });
			}
		}
	};
}

/**
 * Fakes Wicket.Event.add for domready as Wicket's Ajax on jQuery does: the handlers run later, in
 * their order, when flushed.
 */
function jQueryWicketEvent(): () => void {
	const queued: Listener[] = [];
	(window.Wicket as Record<string, unknown>).Event = {
		add(_element: Window, _type: string, listener: Listener) {
			domready.push(listener);
			queued.push(listener);
		}
	};
	return () => {
		while (queued.length > 0) {
			queued.shift()?.();
		}
	};
}

/** Adds a domready handler, as a component's OnDomReadyHeaderItem does. */
function onDomReady(listener: Listener): void {
	(window.Wicket?.Event as unknown as { add(w: Window, t: string, l: Listener): void })
		.add(window, 'domready', listener);
}

/** The handler a component's initialization runs in: it notes whether the page is veiled. */
function component(name: string): Listener {
	return () => seen.push(name + (veiled() ? ' veiled' : ' unveiled'));
}

describe('page loading veil', () => {
	beforeEach(() => {
		domready = [];
		seen = [];
		document.body.className = CSS_CLASS;
	});

	afterEach(() => {
		stop?.();
		stop = undefined;
		delete (window.Wicket as Record<string, unknown>).Event;
		document.body.className = '';
		document.documentElement.className = '';
		readyState('complete');
	});

	it('registers Wicket.PageLoadingVeil', () => {
		expect(window.Wicket?.PageLoadingVeil).toBe(WicketPageLoadingVeil);
		expect(WicketPageLoadingVeil.cssClass).toBe('antilia-page-loading');
	});

	it('lifts the veil of the body and of the html element', () => {
		document.documentElement.classList.add(CSS_CLASS);

		expect(lift()).toBe(2);

		expect(veiled()).toBe(false);
		expect(WicketPageLoadingVeil.lift()).toBe(0);
	});

	it('lifts the veil once the domready handlers of the page have run, in plain JavaScript',
		() => {
			readyState('loading');
			plainWicketEvent();
			// the page's script, before the domready handlers of the page's components
			stop = install();
			onDomReady(component('dropdown'));
			expect(veiled()).toBe(true);

			readyState('interactive');
			document.dispatchEvent(new Event('DOMContentLoaded', { bubbles: true }));

			expect(seen).toEqual(['dropdown veiled']);
			expect(veiled()).toBe(false);
		});

	it('lifts the veil after the ready handlers jQuery queued', () => {
		readyState('loading');
		const flush = jQueryWicketEvent();
		stop = install();
		onDomReady(component('palette'));

		readyState('interactive');
		document.dispatchEvent(new Event('DOMContentLoaded', { bubbles: true }));
		// jQuery runs its ready handlers later
		expect(veiled()).toBe(true);
		flush();

		expect(seen).toEqual(['palette veiled']);
		expect(veiled()).toBe(false);
	});

	it('lifts the veil at DOMContentLoaded on a page without Wicket.Event', () => {
		readyState('loading');
		stop = install();
		document.addEventListener('DOMContentLoaded', component('page'), { once: true });

		readyState('interactive');
		document.dispatchEvent(new Event('DOMContentLoaded', { bubbles: true }));

		expect(seen).toEqual(['page veiled']);
		expect(veiled()).toBe(false);
	});

	it('lifts the veil when the engine of Wicket\'s Ajax fails', () => {
		readyState('loading');
		(window.Wicket as Record<string, unknown>).Event = {
			add() {
				throw new Error('jQuery is not defined');
			}
		};
		stop = install();

		readyState('interactive');
		document.dispatchEvent(new Event('DOMContentLoaded', { bubbles: true }));

		expect(veiled()).toBe(false);
	});

	it('lifts the veil on load when it came after DOMContentLoaded', () => {
		readyState('interactive');
		plainWicketEvent();
		stop = install();
		expect(veiled()).toBe(true);

		window.dispatchEvent(new Event('load'));

		expect(veiled()).toBe(false);
		expect(domready).toHaveLength(1);
	});

	it('lifts the veil once only', () => {
		readyState('loading');
		plainWicketEvent();
		stop = install();

		readyState('interactive');
		document.dispatchEvent(new Event('DOMContentLoaded', { bubbles: true }));
		document.body.classList.add(CSS_CLASS);
		readyState('complete');
		window.dispatchEvent(new Event('load'));

		expect(domready).toHaveLength(1);
		expect(veiled()).toBe(true);
	});

	it('lifts the veil at once when the page has loaded already', () => {
		plainWicketEvent();

		stop = install();

		expect(veiled()).toBe(false);
	});

	it('stops waiting when told to', () => {
		readyState('loading');
		install()();

		readyState('interactive');
		document.dispatchEvent(new Event('DOMContentLoaded', { bubbles: true }));

		expect(veiled()).toBe(true);
	});
});

describe('page loading veil style sheets', () => {
	/** @returns the declarations of the rule of the selector, without white space */
	function rule(css: string, selector: string): string {
		const start = css.indexOf(selector + ' {');
		expect(start, selector).toBeGreaterThanOrEqual(0);
		return css.substring(css.indexOf('{', start) + 1, css.indexOf('}', start))
			.replace(/\s+/g, ' ').trim();
	}

	it('veils the element with the class by its ::before and ::after, transparent', () => {
		expect(veil).toContain('.antilia-page-loading::before,\n.antilia-page-loading::after {');
		const both = rule(veil, '.antilia-page-loading::after');
		expect(both).toContain('position: fixed;');
		expect(both).toContain('cursor: wait;');
		const before = rule(veil, '.antilia-page-loading::before');
		expect(before).toContain('background: transparent;');
	});

	it('shows the spinner after the delay and lifts the veil after the timeout, by CSS alone',
		() => {
			const before = veil.substring(veil.lastIndexOf('.antilia-page-loading::before {'));
			expect(rule(before, '.antilia-page-loading::before')).toContain(
				'antilia-page-loading-busy 0s var(--antilia-page-loading-delay, 300ms) both, ' +
				'antilia-page-loading-timeout 0s var(--antilia-page-loading-timeout, 10s) forwards');
			const after = veil.substring(veil.lastIndexOf('.antilia-page-loading::after {'));
			expect(rule(after, '.antilia-page-loading::after')).toContain('opacity: 0;');
			expect(rule(after, '.antilia-page-loading::after')).toContain(
				'antilia-page-loading-spinner 0s var(--antilia-page-loading-delay, 300ms) forwards');
			expect(veil.substring(veil.indexOf('@keyframes antilia-page-loading-timeout')))
				.toMatch(/to\s*{\s*visibility: hidden;/);
		});

	it('takes the veil away in a browser without scripts', () => {
		expect(rule(noscript, '.antilia-page-loading.antilia-page-loading::after'))
			.toBe('content: none;');
		expect(noscript).toContain('.antilia-page-loading.antilia-page-loading::before,');
	});
});
