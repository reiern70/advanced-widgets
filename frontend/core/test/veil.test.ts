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
/// <reference types="vite/client" />
import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';

// the veil of ui-components, a copy of Wicket 11's with its gaps closed, as the page loads it
import source from '../../../ui-components/src/main/java/com/antiliacode/widgets/veil/antilia-veil.js?raw';

type Subscriber = (event: unknown, attrs: unknown) => void;

/** The parts of Wicket.Veil the tests call. */
interface Veil {
	page(options: { delay: number; minimum: number }): void;
	local(id: string, options: { delay: number; minimum: number }): void;
	show(id: string): void;
	hide(id: string): void;
	isCovered(element: Element | null): boolean;
	_reset(): void;
}

/** A request's attributes, as Wicket's Ajax gives them to the topics. */
type Attrs = Record<string, unknown>;

const TOPIC = {
	AJAX_CALL_INIT: '/ajax/call/init',
	AJAX_CALL_BEFORE: '/ajax/call/before',
	AJAX_CALL_BEFORE_SEND: '/ajax/call/beforeSend',
	AJAX_CALL_DONE: '/ajax/call/done',
	DOM_NODE_ADDED: '/dom/node/added'
};

const OPTIONS = { delay: 300, minimum: 500 };

/** The subscribers of Wicket.Event, by topic: the script subscribes once. */
const topics = new Map<string, Subscriber[]>();

/** Wicket's channels and throttler entries, as a test sets them. */
const channels: Record<string, { busy: boolean } | undefined> = {};
const entries: Record<string, unknown> = {};

const log = { error: vi.fn() };

function veil(): Veil {
	return (window.Wicket as Record<string, unknown>).Veil as Veil;
}

function publish(topic: string, attrs: unknown): void {
	(topics.get(topic) ?? []).forEach((subscriber) => subscriber({}, attrs));
}

function click(element: Element, more: Attrs = {}): Attrs {
	return { u: './x', c: element.id, event: { target: element }, ...more };
}

/** Its turn comes: Wicket's Ajax runs, sends and finishes a request. */
function run(attrs: Attrs): void {
	publish(TOPIC.AJAX_CALL_BEFORE, attrs);
	publish(TOPIC.AJAX_CALL_BEFORE_SEND, attrs);
	publish(TOPIC.AJAX_CALL_DONE, attrs);
}

/** Fires a request whose channel is free: Wicket's Ajax sends it at once. */
function send(attrs: Attrs): void {
	publish(TOPIC.AJAX_CALL_INIT, attrs);
	publish(TOPIC.AJAX_CALL_BEFORE, attrs);
	publish(TOPIC.AJAX_CALL_BEFORE_SEND, attrs);
}

function done(attrs: Attrs): void {
	publish(TOPIC.AJAX_CALL_DONE, attrs);
}

function pageVeil(): Element | null {
	return document.querySelector('body > .wicket-veil');
}

function key(target: Element, type: string, value: string, isComposing = false): KeyboardEvent {
	const event = new KeyboardEvent(type, { key: value, bubbles: true, cancelable: true,
		isComposing });
	target.dispatchEvent(event);
	return event;
}

/** A pointer event, which jsdom lacks, as a mouse fires it with a button, the main one first. */
function pointer(target: Element, type: string, button = 0): void {
	const event = new MouseEvent(type, { bubbles: true, cancelable: true, button,
		clientX: 10, clientY: 20 });
	Object.defineProperty(event, 'isPrimary', { value: true });
	target.dispatchEvent(event);
}

/** Makes what is under the pointer, which jsdom does not lay out: from the top down. */
function under(...elements: Element[]): void {
	document.elementFromPoint = () => elements[0] ?? null;
	document.elementsFromPoint = () => elements;
}

/** Makes an element shown, which jsdom does not lay out. */
function shown(element: Element): void {
	element.getClientRects = () => [{}] as unknown as DOMRectList;
}

const byId = (id: string): HTMLElement => document.getElementById(id)!;

describe('the veil', () => {
	beforeAll(() => {
		const wicket = (window.Wicket ?? (window.Wicket = {})) as Record<string, unknown>;
		Object.assign(wicket, {
			Event: {
				subscribe(topic: string, subscriber: Subscriber) {
					topics.set(topic, [...(topics.get(topic) ?? []), subscriber]);
				},
				unsubscribe() {
					// the veil never unsubscribes
				},
				Topic: TOPIC
			},
			Log: log,
			channelManager: { channels },
			Throttler: { entries }
		});
		delete wicket.Veil;
		new Function(source)();
	});

	beforeEach(() => {
		vi.useFakeTimers();
		document.body.innerHTML =
			'<form id="form">' +
			'  <input id="amount" name="amount" type="text">' +
			'  <textarea id="notes" name="notes"></textarea>' +
			'  <button id="save" name="save" type="button"><span id="label">Save</span></button>' +
			'</form>' +
			'<div id="table"><button id="sort" type="button">Sort</button></div>' +
			'<a id="slow" href="#">Slow</a>';
	});

	afterEach(() => {
		veil()._reset();
		vi.useRealTimers();
		Object.keys(channels).forEach((name) => delete channels[name]);
		Object.keys(entries).forEach((name) => delete entries[name]);
		log.error.mockReset();
		document.body.innerHTML = '';
	});

	describe('of the page', () => {
		beforeEach(() => {
			veil().page(OPTIONS);
		});

		it('goes up at /ajax/call/init and down when the request is done', () => {
			const attrs = click(byId('save'));
			publish(TOPIC.AJAX_CALL_INIT, attrs);
			expect(pageVeil()).not.toBeNull();

			run(attrs);
			expect(pageVeil()).toBeNull();
			expect(document.querySelectorAll('.wicket-veil')).toHaveLength(0);
		});

		it('shows the spinner after its delay from the click, and keeps it its minimum time', () => {
			const attrs = click(byId('save'));
			send(attrs);
			vi.advanceTimersByTime(OPTIONS.delay);
			expect(pageVeil()!.classList.contains('wicket-veil-busy')).toBe(true);

			done(attrs);
			expect(pageVeil()).not.toBeNull();
			vi.advanceTimersByTime(OPTIONS.minimum);
			expect(pageVeil()).toBeNull();
		});

		it('veils a request Wicket\'s Ajax runs without /ajax/call/init as it is sent', () => {
			const attrs = click(byId('save'));
			publish(TOPIC.AJAX_CALL_BEFORE_SEND, attrs);
			expect(pageVeil()).not.toBeNull();
			done(attrs);
			expect(pageVeil()).toBeNull();
		});

		it('leaves a request carrying the parameter of no veil alone', () => {
			send(click(byId('save'), { ep: [{ name: 'wicket_nb', value: 'true' }] }));
			expect(pageVeil()).toBeNull();
			send(click(byId('save'), { ep: { wicket_nb: 'true' } }));
			expect(pageVeil()).toBeNull();
		});

		it('stays up while a request waits its turn behind another one', () => {
			channels['0'] = { busy: true };
			const attrs = click(byId('save'));
			publish(TOPIC.AJAX_CALL_INIT, attrs);
			vi.advanceTimersByTime(5000);
			expect(pageVeil()).not.toBeNull();

			run(attrs);
			vi.advanceTimersByTime(OPTIONS.minimum);
			expect(pageVeil()).toBeNull();
		});

		it('comes down when its precondition stops the request', () => {
			const attrs = click(byId('save'));
			publish(TOPIC.AJAX_CALL_INIT, attrs);
			publish(TOPIC.AJAX_CALL_BEFORE, attrs);
			publish(TOPIC.AJAX_CALL_DONE, attrs);
			expect(pageVeil()).toBeNull();
		});

		it('comes down for a request no channel runs nor queues', () => {
			publish(TOPIC.AJAX_CALL_INIT, click(byId('save')));
			expect(pageVeil()).not.toBeNull();
			vi.advanceTimersByTime(0);
			expect(pageVeil()).toBeNull();
		});

		it('comes down for a request started but neither sent nor done: an error', () => {
			const attrs = click(byId('save'));
			channels['0'] = { busy: true };
			publish(TOPIC.AJAX_CALL_INIT, attrs);
			publish(TOPIC.AJAX_CALL_BEFORE, attrs);
			vi.advanceTimersByTime(0);
			expect(pageVeil()).toBeNull();
		});

		it('ignores a request an active channel drops as another one runs', () => {
			channels.busy = { busy: true };
			publish(TOPIC.AJAX_CALL_INIT, click(byId('save'), { ch: 'busy|a' }));
			expect(pageVeil()).toBeNull();
		});

		it('comes down for the requests a drop channel forgets for a later one', () => {
			channels.filter = { busy: true };
			const first = click(byId('save'), { ch: 'filter|d' });
			const second = click(byId('save'), { ch: 'filter|d' });
			publish(TOPIC.AJAX_CALL_INIT, first);
			publish(TOPIC.AJAX_CALL_INIT, second);
			done(second);
			expect(pageVeil()).toBeNull();
		});

		it('comes down for a throttled request a later one replaces', () => {
			const first = click(byId('save'), { tr: { id: 'save', d: 500 } });
			publish(TOPIC.AJAX_CALL_INIT, first);
			entries.save = {};
			vi.advanceTimersByTime(100);
			expect(pageVeil()).not.toBeNull();

			const second = click(byId('save'), { tr: { id: 'save', d: 500 } });
			publish(TOPIC.AJAX_CALL_INIT, second);
			entries.save = undefined;
			run(second);
			expect(pageVeil()).toBeNull();
		});

		it('stays up for a throttled request that left the throttler for a busy channel', () => {
			channels['0'] = { busy: true };
			const first = click(byId('save'), { tr: { id: 'save', d: 500 } });
			publish(TOPIC.AJAX_CALL_INIT, first);
			const second = click(byId('save'), { tr: { id: 'save', d: 500 } });
			publish(TOPIC.AJAX_CALL_INIT, second);
			vi.advanceTimersByTime(1000);
			run(second);
			expect(pageVeil()).not.toBeNull();
			run(first);
			vi.advanceTimersByTime(OPTIONS.minimum);
			expect(pageVeil()).toBeNull();
		});

		it('drops Enter and Space going to a button under it, and their keyup', () => {
			send(click(byId('save')));
			const listener = vi.fn();
			byId('save').addEventListener('keydown', listener);

			expect(key(byId('save'), 'keydown', 'Enter').defaultPrevented).toBe(true);
			expect(key(byId('save'), 'keyup', 'Enter').defaultPrevented).toBe(true);
			expect(key(byId('save'), 'keydown', ' ').defaultPrevented).toBe(true);
			expect(key(byId('amount'), 'keydown', 'Enter').defaultPrevented).toBe(true);
			expect(listener).not.toHaveBeenCalled();
			expect(veil().isCovered(byId('save'))).toBe(true);
		});

		it('lets the keys type: Space in a text field, Enter in a text area, a composition', () => {
			send(click(byId('save')));
			expect(key(byId('amount'), 'keydown', ' ').defaultPrevented).toBe(false);
			expect(key(byId('notes'), 'keydown', 'Enter').defaultPrevented).toBe(false);
			expect(key(byId('amount'), 'keydown', 'Enter', true).defaultPrevented).toBe(false);
			// a keyup whose keydown it did not drop, another key
			expect(key(byId('save'), 'keyup', ' ').defaultPrevented).toBe(false);
			expect(key(byId('save'), 'keydown', 'Tab').defaultPrevented).toBe(false);
		});

		it('leaves the keys alone once it is down', () => {
			const attrs = click(byId('save'));
			send(attrs);
			done(attrs);
			expect(key(byId('save'), 'keydown', 'Enter').defaultPrevented).toBe(false);
			expect(veil().isCovered(byId('save'))).toBe(false);
		});

		it('gives back the click whose release it caught once it is down', () => {
			const clicks = vi.fn();
			byId('save').addEventListener('click', clicks);
			shown(byId('label'));
			pointer(byId('label'), 'pointerdown');
			// the field's change, as the mouse goes down
			const change = click(byId('amount'));
			send(change);
			under(pageVeil()!, byId('label'), byId('save'));
			pointer(pageVeil()!, 'pointerup');

			vi.advanceTimersByTime(OPTIONS.delay);
			expect(clicks).not.toHaveBeenCalled();
			done(change);
			// the spinner stays its minimum time
			vi.advanceTimersByTime(OPTIONS.minimum - 50);
			expect(clicks).not.toHaveBeenCalled();
			vi.advanceTimersByTime(100);
			expect(clicks).toHaveBeenCalledTimes(1);
			vi.advanceTimersByTime(500);
			expect(clicks).toHaveBeenCalledTimes(1);
		});

		it('gives the click to the element that took the markup id of the one caught', () => {
			pointer(byId('save'), 'pointerdown');
			const change = click(byId('amount'));
			send(change);
			under(pageVeil()!, byId('save'));
			pointer(pageVeil()!, 'pointerup');

			const replaced = document.createElement('button');
			replaced.id = 'save';
			replaced.type = 'button';
			byId('save').replaceWith(replaced);
			shown(replaced);
			const clicks = vi.fn();
			replaced.addEventListener('click', clicks);
			done(change);
			vi.advanceTimersByTime(100);
			expect(clicks).toHaveBeenCalledTimes(1);
		});

		it('gives no click to an element gone, hidden or disabled', () => {
			for (const answer of ['remove', 'hide', 'disable']) {
				const clicks = vi.fn();
				const button = byId('save');
				button.addEventListener('click', clicks);
				shown(button);
				pointer(button, 'pointerdown');
				const change = click(byId('amount'));
				send(change);
				under(pageVeil()!, button);
				pointer(pageVeil()!, 'pointerup');
				if (answer === 'remove') {
					button.remove();
				} else if (answer === 'hide') {
					button.getClientRects = () => [] as unknown as DOMRectList;
				} else {
					(button as HTMLButtonElement).disabled = true;
				}
				done(change);
				vi.advanceTimersByTime(100);
				expect(clicks, answer).not.toHaveBeenCalled();
				document.body.innerHTML =
					'<button id="save" type="button">Save</button><input id="amount">';
			}
		});

		it('keeps no click when the release is not on it, or the press was', () => {
			const clicks = vi.fn();
			byId('save').addEventListener('click', clicks);
			shown(byId('save'));
			pointer(byId('save'), 'pointerdown');
			under(byId('save'));
			pointer(byId('save'), 'pointerup');

			const attrs = click(byId('save'));
			send(attrs);
			pointer(pageVeil()!, 'pointerdown');
			under(pageVeil()!, byId('save'));
			pointer(pageVeil()!, 'pointerup');
			done(attrs);
			vi.advanceTimersByTime(100);
			expect(clicks).not.toHaveBeenCalled();
		});

		it('forgets the click caught when the user presses elsewhere, or with another button', () => {
			const clicks = vi.fn();
			byId('save').addEventListener('click', clicks);
			shown(byId('save'));
			pointer(byId('save'), 'pointerdown');
			const change = click(byId('amount'));
			send(change);
			under(pageVeil()!, byId('save'));
			pointer(pageVeil()!, 'pointerup');
			done(change);
			pointer(byId('slow'), 'pointerdown');
			vi.advanceTimersByTime(100);
			expect(clicks).not.toHaveBeenCalled();

			pointer(byId('save'), 'pointerdown', 2);
			const other = click(byId('amount'));
			send(other);
			under(pageVeil()!, byId('save'));
			pointer(pageVeil()!, 'pointerup', 2);
			done(other);
			vi.advanceTimersByTime(100);
			expect(clicks).not.toHaveBeenCalled();
		});

		it('tells Wicket\'s log of an error rather than stopping the request', () => {
			const attrs = click(byId('save'));
			Object.defineProperty(attrs, 'tr', { get() {
				throw new Error('broken');
			} });
			expect(() => publish(TOPIC.AJAX_CALL_INIT, attrs)).not.toThrow();
			expect(log.error).toHaveBeenCalledWith('Wicket.Veil:', expect.any(Error));
		});
	});

	describe('of an element', () => {
		beforeEach(() => {
			veil().page(OPTIONS);
			veil().local('table', OPTIONS);
		});

		it('goes up from the click inside it, the page staying free', () => {
			const attrs = click(byId('sort'));
			publish(TOPIC.AJAX_CALL_INIT, attrs);
			expect(byId('table').querySelector(':scope > .wicket-veil')).not.toBeNull();
			expect(pageVeil()).toBeNull();
			expect(veil().isCovered(byId('sort'))).toBe(true);
			expect(veil().isCovered(byId('save'))).toBe(false);

			run(attrs);
			expect(byId('table').querySelector('.wicket-veil')).toBeNull();
		});

		it('drops the keys inside the element veiled only', () => {
			send(click(byId('sort')));
			expect(key(byId('sort'), 'keydown', 'Enter').defaultPrevented).toBe(true);
			expect(key(byId('save'), 'keydown', 'Enter').defaultPrevented).toBe(false);
		});

		it('still goes up and down by show and hide, as the server pushes an update', () => {
			veil().show('table');
			expect(veil().isCovered(byId('sort'))).toBe(true);
			veil().hide('table');
			expect(veil().isCovered(byId('sort'))).toBe(false);
		});
	});

	it('veils nothing in a page without a veil', () => {
		send(click(byId('save')));
		expect(document.querySelector('.wicket-veil')).toBeNull();
		expect(key(byId('save'), 'keydown', 'Enter').defaultPrevented).toBe(false);
	});
});
