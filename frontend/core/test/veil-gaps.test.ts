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
import { afterEach, beforeEach, describe, expect, it, type Mock, vi } from 'vitest';

import { GUARD_CLASS, giveBack, install, isCovered, isOptedOut, parseChannel,
	REPLAY_POLL, uninstall, WicketVeilGaps } from '../src/wicket/veil-gaps.js';
import type { AjaxAttributes } from '../src/wicket/send-state.js';

type Subscriber = (event: unknown, attrs: unknown) => void;

/** The subscribers of Wicket.Event, by topic, while a test fakes it. */
let topics: Map<string, Subscriber[]>;

/** Wicket.Veil, as a test fakes it: a local veil puts a veil element into its element. */
let veil: {
	page: Mock<(options: unknown) => void>;
	local: Mock<(id: string, options: unknown) => void>;
	show: Mock<(id: string) => void>;
	hide: Mock<(id: string) => void>;
};

/** Wicket's channels and throttler entries, as a test sets them. */
let channels: Record<string, { busy: boolean }>;
let entries: Record<string, unknown>;

const TOPIC = {
	AJAX_CALL_INIT: '/ajax/call/init',
	AJAX_CALL_BEFORE: '/ajax/call/before',
	AJAX_CALL_BEFORE_SEND: '/ajax/call/beforeSend',
	AJAX_CALL_DONE: '/ajax/call/done'
};

function publish(topic: string, attrs: unknown): void {
	(topics.get(topic) ?? []).forEach((subscriber) => subscriber({}, attrs));
}

/** A click on an element, as Wicket's Ajax tells of it: the attributes of its request. */
function click(element: Element, more: Partial<AjaxAttributes> = {}): AjaxAttributes {
	return { u: './x', c: element.id, event: { target: element }, ...more };
}

/** Runs a request through Wicket's Ajax after its /ajax/call/init: sent, answered, done. */
function run(attrs: AjaxAttributes): void {
	publish(TOPIC.AJAX_CALL_BEFORE, attrs);
	publish(TOPIC.AJAX_CALL_BEFORE_SEND, attrs);
	publish(TOPIC.AJAX_CALL_DONE, attrs);
}

/** Fires a request whose channel is free, as Wicket's Ajax does: it is sent at once. */
function send(attrs: AjaxAttributes): void {
	publish(TOPIC.AJAX_CALL_INIT, attrs);
	publish(TOPIC.AJAX_CALL_BEFORE, attrs);
	publish(TOPIC.AJAX_CALL_BEFORE_SEND, attrs);
}

/** The answer of a request sent is in. */
function done(attrs: AjaxAttributes): void {
	publish(TOPIC.AJAX_CALL_DONE, attrs);
}

function guard(): Element | null {
	return document.querySelector('.' + GUARD_CLASS);
}

function key(target: Element, type: string, value: string): KeyboardEvent {
	const event = new KeyboardEvent(type, { key: value, bubbles: true, cancelable: true });
	target.dispatchEvent(event);
	return event;
}

/** A pointer event, which jsdom lacks, as a mouse fires it with its main button. */
function pointer(target: Element, type: string, button = 0): Event {
	const event = new MouseEvent(type, { bubbles: true, cancelable: true, button,
		clientX: 10, clientY: 20 });
	Object.defineProperty(event, 'isPrimary', { value: true });
	target.dispatchEvent(event);
	return event;
}

/** Makes what is under the pointer, which jsdom does not lay out: from the top down. */
function under(...elements: Element[]): void {
	document.elementFromPoint = () => elements[0] ?? null;
	document.elementsFromPoint = () => elements;
}

/** Wicket.Veil as the page has it, the add-on watching it. */
function wicketVeil(): typeof veil {
	return (window.Wicket as Record<string, unknown>).Veil as typeof veil;
}

/** Makes an element shown, which jsdom does not lay out. */
function shown(element: Element): void {
	element.getClientRects = () => [{}] as unknown as DOMRectList;
}

describe('veil gaps', () => {
	beforeEach(() => {
		vi.useFakeTimers();
		topics = new Map();
		channels = {};
		entries = {};
		veil = {
			page: vi.fn<(options: unknown) => void>(),
			local: vi.fn<(id: string, options: unknown) => void>(),
			show: vi.fn<(id: string) => void>((id) => {
				const host = document.getElementById(id)!;
				host.classList.add('wicket-veil-host');
				const element = document.createElement('div');
				element.className = 'wicket-veil';
				host.appendChild(element);
			}),
			hide: vi.fn<(id: string) => void>((id) => {
				document.getElementById(id)?.querySelector(':scope > .wicket-veil')?.remove();
			})
		};
		Object.assign(window.Wicket as Record<string, unknown>, {
			Event: {
				subscribe(topic: string, subscriber: Subscriber) {
					topics.set(topic, [...(topics.get(topic) ?? []), subscriber]);
				},
				unsubscribe(topic: string, subscriber: Subscriber) {
					topics.set(topic, (topics.get(topic) ?? [])
						.filter((each) => each !== subscriber));
				},
				Topic: TOPIC
			},
			Veil: { ...veil },
			channelManager: { channels },
			Throttler: { entries }
		});
		document.body.innerHTML =
			'<form id="form">' +
			'  <input id="amount" name="amount" type="text">' +
			'  <textarea id="notes" name="notes"></textarea>' +
			'  <button id="save" name="save" type="button"><span id="label">Save</span></button>' +
			'</form>' +
			'<div id="table"><button id="sort">Sort</button></div>' +
			'<a id="slow" href="#">Slow</a>';
		install();
	});

	afterEach(() => {
		uninstall();
		vi.useRealTimers();
		const wicket = window.Wicket as Record<string, unknown>;
		['Event', 'Veil', 'channelManager', 'Throttler'].forEach((name) => delete wicket[name]);
		document.body.innerHTML = '';
	});

	const save = (): HTMLElement => document.getElementById('save')!;

	it('registers Wicket.VeilGaps', () => {
		expect(window.Wicket?.VeilGaps).toBe(WicketVeilGaps);
	});

	it('learns the veils from Wicket.Veil, which still registers them', () => {
		wicketVeil().page({ delay: 300 });
		wicketVeil().local('table', { delay: 300 });

		expect(veil.page).toHaveBeenCalledWith({ delay: 300 });
		expect(veil.local).toHaveBeenCalledWith('table', { delay: 300 });
	});

	it('reads the parameter leaving a request unveiled, as Wicket\'s veil does', () => {
		expect(isOptedOut({ ep: [{ name: 'wicket_nb', value: 'true' }] })).toBe(true);
		expect(isOptedOut({ ep: { wicket_nb: 'true' } })).toBe(true);
		expect(isOptedOut({ ep: [{ name: 'other', value: 'x' }] })).toBe(false);
		expect(isOptedOut({})).toBe(false);
	});

	it('reads the channel of a request, as Wicket.Channel does', () => {
		expect(parseChannel('filter|d')).toEqual({ name: 'filter', type: 'd' });
		expect(parseChannel(undefined)).toEqual({ name: '0', type: 's' });
		expect(parseChannel('odd')).toEqual({ name: '0', type: 's' });
	});

	describe('with a page veil', () => {
		beforeEach(() => {
			wicketVeil().page({ delay: 300, minimum: 500 });
		});

		it('veils the page from /ajax/call/init until the request is done', () => {
			const attrs = click(save());
			publish(TOPIC.AJAX_CALL_INIT, attrs);
			expect(guard()).not.toBeNull();
			expect(isCovered(save())).toBe(true);

			run(attrs);
			expect(guard()).toBeNull();
			expect(isCovered(save())).toBe(false);
		});

		it('keeps the page veiled while any request holds it', () => {
			const first = click(save());
			const second = click(document.getElementById('slow')!);
			publish(TOPIC.AJAX_CALL_INIT, first);
			publish(TOPIC.AJAX_CALL_INIT, second);
			run(first);
			expect(guard()).not.toBeNull();
			run(second);
			expect(guard()).toBeNull();
		});

		it('leaves a request carrying the parameter of no veil alone', () => {
			publish(TOPIC.AJAX_CALL_INIT,
				click(save(), { ep: [{ name: 'wicket_nb', value: 'true' }] }));
			expect(guard()).toBeNull();
		});

		it('lets the veil of a request down when its precondition stops it', () => {
			const attrs = click(save());
			publish(TOPIC.AJAX_CALL_INIT, attrs);
			publish(TOPIC.AJAX_CALL_BEFORE, attrs);
			// Wicket's Ajax calls done() when a precondition fails, never sends it
			publish(TOPIC.AJAX_CALL_DONE, attrs);
			expect(guard()).toBeNull();
		});

		it('keeps a request waiting its turn veiled while its channel is busy', () => {
			channels['0'] = { busy: true };
			const attrs = click(save());
			publish(TOPIC.AJAX_CALL_INIT, attrs);
			vi.advanceTimersByTime(1000);
			expect(guard()).not.toBeNull();

			// its turn comes
			run(attrs);
			expect(guard()).toBeNull();
		});

		it('lets down the veil of a request no channel runs nor queues', () => {
			// Wicket's Ajax never scheduled it: an error before, in a listener of the topic
			publish(TOPIC.AJAX_CALL_INIT, click(save()));
			expect(guard()).not.toBeNull();
			vi.advanceTimersByTime(0);
			expect(guard()).toBeNull();
		});

		it('lets down the veil of a request started but neither sent nor done: an error', () => {
			const attrs = click(save());
			channels['0'] = { busy: true };
			publish(TOPIC.AJAX_CALL_INIT, attrs);
			publish(TOPIC.AJAX_CALL_BEFORE, attrs);
			vi.advanceTimersByTime(0);
			expect(guard()).toBeNull();
		});

		it('keeps a sent request veiled until it is done, however long it takes', () => {
			const attrs = click(save());
			publish(TOPIC.AJAX_CALL_INIT, attrs);
			publish(TOPIC.AJAX_CALL_BEFORE, attrs);
			publish(TOPIC.AJAX_CALL_BEFORE_SEND, attrs);
			vi.advanceTimersByTime(60000);
			expect(guard()).not.toBeNull();
			publish(TOPIC.AJAX_CALL_DONE, attrs);
			expect(guard()).toBeNull();
		});

		it('ignores a request an active channel drops as another one runs', () => {
			channels.busy = { busy: true };
			publish(TOPIC.AJAX_CALL_INIT, click(save(), { ch: 'busy|a' }));
			expect(guard()).toBeNull();
		});

		it('lets down the veils of the requests a drop channel forgets for a later one', () => {
			channels.filter = { busy: true };
			const first = click(save(), { ch: 'filter|d' });
			const second = click(save(), { ch: 'filter|d' });
			publish(TOPIC.AJAX_CALL_INIT, first);
			publish(TOPIC.AJAX_CALL_INIT, second);
			publish(TOPIC.AJAX_CALL_DONE, second);
			// the first one never runs: only the second held the veil
			expect(guard()).toBeNull();
		});

		it('veils a throttled request, and lets it down when a later one replaces it', () => {
			const first = click(save(), { tr: { id: 'save', d: 500 } });
			publish(TOPIC.AJAX_CALL_INIT, first);
			entries.save = {};
			vi.advanceTimersByTime(100);
			expect(guard()).not.toBeNull();

			const second = click(save(), { tr: { id: 'save', d: 500 } });
			publish(TOPIC.AJAX_CALL_INIT, second);
			// the throttler runs the second one alone
			entries.save = undefined;
			run(second);
			expect(guard()).toBeNull();
		});

		it('keeps a throttled request that left the throttler for a busy channel', () => {
			channels['0'] = { busy: true };
			const first = click(save(), { tr: { id: 'save', d: 500 } });
			publish(TOPIC.AJAX_CALL_INIT, first);
			// it left the throttler for the queue of its channel; another one comes
			const second = click(save(), { tr: { id: 'save', d: 500 } });
			publish(TOPIC.AJAX_CALL_INIT, second);
			vi.advanceTimersByTime(1000);
			run(second);
			expect(guard()).not.toBeNull();
			run(first);
			expect(guard()).toBeNull();
		});

		it('drops Enter and Space going to a button under the veil, and their keyup', () => {
			publish(TOPIC.AJAX_CALL_INIT, click(save()));
			const keydown = key(save(), 'keydown', 'Enter');
			const keyup = key(save(), 'keyup', 'Enter');
			expect(keydown.defaultPrevented).toBe(true);
			expect(keyup.defaultPrevented).toBe(true);
			expect(key(save(), 'keydown', ' ').defaultPrevented).toBe(true);

			const listener = vi.fn();
			save().addEventListener('keydown', listener);
			key(save(), 'keydown', 'Enter');
			expect(listener).not.toHaveBeenCalled();
		});

		it('drops Enter in a text field, which would send its form', () => {
			publish(TOPIC.AJAX_CALL_INIT, click(save()));
			expect(key(document.getElementById('amount')!, 'keydown', 'Enter').defaultPrevented)
				.toBe(true);
		});

		it('lets the keys type: Space in a text field, Enter and Space in a text area', () => {
			publish(TOPIC.AJAX_CALL_INIT, click(save()));
			expect(key(document.getElementById('amount')!, 'keydown', ' ').defaultPrevented)
				.toBe(false);
			expect(key(document.getElementById('notes')!, 'keydown', 'Enter').defaultPrevented)
				.toBe(false);
			expect(key(document.getElementById('notes')!, 'keydown', ' ').defaultPrevented)
				.toBe(false);
		});

		it('leaves the keys alone without a veil, and the other keys with one', () => {
			expect(key(save(), 'keydown', 'Enter').defaultPrevented).toBe(false);
			// a keyup whose keydown it did not drop
			publish(TOPIC.AJAX_CALL_INIT, click(save()));
			expect(key(save(), 'keyup', ' ').defaultPrevented).toBe(false);
			expect(key(save(), 'keydown', 'Tab').defaultPrevented).toBe(false);
		});

		it('lets Enter end the composition of a character', () => {
			publish(TOPIC.AJAX_CALL_INIT, click(save()));
			const event = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true,
				cancelable: true, isComposing: true });
			document.getElementById('amount')!.dispatchEvent(event);
			expect(event.defaultPrevented).toBe(false);
		});

		it('tells Wicket\'s log of an error rather than stopping the request', () => {
			const error = vi.fn();
			(window.Wicket as Record<string, unknown>).Log = { error };
			const attrs = click(save());
			Object.defineProperty(attrs, 'ep', { get() {
				throw new Error('broken');
			} });
			expect(() => publish(TOPIC.AJAX_CALL_INIT, attrs)).not.toThrow();
			expect(error).toHaveBeenCalledWith('Wicket.VeilGaps:', expect.any(Error));
			delete (window.Wicket as Record<string, unknown>).Log;
		});

		it('drops the keys under Wicket\'s own page veil too', () => {
			const element = document.createElement('div');
			element.className = 'wicket-veil';
			document.body.appendChild(element);
			expect(key(save(), 'keydown', 'Enter').defaultPrevented).toBe(true);
		});

		it('gives back the click a veil caught once the veil is down', () => {
			const clicks = vi.fn();
			save().addEventListener('click', clicks);
			shown(document.getElementById('label')!);
			pointer(document.getElementById('label')!, 'pointerdown');
			// the field's change, as the mouse goes down
			const change = click(document.getElementById('amount')!);
			send(change);
			under(guard()!, document.getElementById('label')!, save());
			pointer(guard()!, 'pointerup');

			vi.advanceTimersByTime(REPLAY_POLL * 3);
			expect(clicks).not.toHaveBeenCalled();
			done(change);
			vi.advanceTimersByTime(REPLAY_POLL);
			expect(clicks).toHaveBeenCalledTimes(1);
			vi.advanceTimersByTime(REPLAY_POLL * 3);
			expect(clicks).toHaveBeenCalledTimes(1);
		});

		it('waits for Wicket\'s veil to go too, which stays a moment for its spinner', () => {
			const clicks = vi.fn();
			save().addEventListener('click', clicks);
			shown(save());
			pointer(save(), 'pointerdown');
			const change = click(document.getElementById('amount')!);
			send(change);
			const wicketVeil = document.createElement('div');
			wicketVeil.className = 'wicket-veil';
			document.body.appendChild(wicketVeil);
			under(wicketVeil, save());
			pointer(wicketVeil, 'pointerup');

			done(change);
			vi.advanceTimersByTime(REPLAY_POLL * 4);
			expect(clicks).not.toHaveBeenCalled();
			wicketVeil.remove();
			vi.advanceTimersByTime(REPLAY_POLL);
			expect(clicks).toHaveBeenCalledTimes(1);
		});

		it('gives the click to the element that took the markup id of the one caught', () => {
			pointer(save(), 'pointerdown');
			const change = click(document.getElementById('amount')!);
			send(change);
			under(guard()!, save());
			pointer(guard()!, 'pointerup');

			// the answer replaces the button
			const replaced = document.createElement('button');
			replaced.id = 'save';
			replaced.type = 'button';
			save().replaceWith(replaced);
			shown(replaced);
			const clicks = vi.fn();
			replaced.addEventListener('click', clicks);
			done(change);
			vi.advanceTimersByTime(REPLAY_POLL);
			expect(clicks).toHaveBeenCalledTimes(1);
		});

		it('gives no click to an element gone, hidden or disabled', () => {
			for (const answer of ['remove', 'hide', 'disable']) {
				const clicks = vi.fn();
				const button = save();
				button.addEventListener('click', clicks);
				shown(button);
				pointer(button, 'pointerdown');
				const change = click(document.getElementById('amount')!);
				send(change);
				under(guard()!, button);
				pointer(guard()!, 'pointerup');
				if (answer === 'remove') {
					button.remove();
				} else if (answer === 'hide') {
					button.getClientRects = () => [] as unknown as DOMRectList;
				} else {
					(button as HTMLButtonElement).disabled = true;
				}
				done(change);
				vi.advanceTimersByTime(REPLAY_POLL);
				expect(clicks, answer).not.toHaveBeenCalled();
				document.body.innerHTML =
					'<button id="save" type="button">Save</button><input id="amount">';
			}
		});

		it('keeps no click when the release is not on a veil, or the press was', () => {
			const clicks = vi.fn();
			save().addEventListener('click', clicks);
			shown(save());
			pointer(save(), 'pointerdown');
			under(save());
			pointer(save(), 'pointerup');

			const attrs = click(save());
			send(attrs);
			pointer(guard()!, 'pointerdown');
			under(guard()!, save());
			pointer(guard()!, 'pointerup');
			done(attrs);
			vi.advanceTimersByTime(REPLAY_POLL * 2);
			expect(clicks).not.toHaveBeenCalled();
		});

		it('forgets the click caught when the user presses elsewhere', () => {
			const clicks = vi.fn();
			save().addEventListener('click', clicks);
			shown(save());
			pointer(save(), 'pointerdown');
			const change = click(document.getElementById('amount')!);
			send(change);
			under(guard()!, save());
			pointer(guard()!, 'pointerup');
			done(change);
			pointer(document.getElementById('slow')!, 'pointerdown');
			vi.advanceTimersByTime(REPLAY_POLL * 2);
			expect(clicks).not.toHaveBeenCalled();
		});

		it('keeps no click of a button but the main one', () => {
			pointer(save(), 'pointerdown', 2);
			send(click(document.getElementById('amount')!));
			under(guard()!, save());
			pointer(guard()!, 'pointerup', 2);
			const clicks = vi.fn();
			save().addEventListener('click', clicks);
			giveBack();
			expect(clicks).not.toHaveBeenCalled();
		});
	});

	describe('with a local veil', () => {
		beforeEach(() => {
			wicketVeil().page({ delay: 300, minimum: 500 });
			wicketVeil().local('table', { delay: 300, minimum: 500 });
		});

		it('raises the local veil from /ajax/call/init, the page staying free', () => {
			const attrs = click(document.getElementById('sort')!);
			publish(TOPIC.AJAX_CALL_INIT, attrs);
			expect(veil.show).toHaveBeenCalledWith('table');
			expect(guard()).toBeNull();
			expect(isCovered(document.getElementById('sort'))).toBe(true);
			expect(isCovered(save())).toBe(false);

			run(attrs);
			expect(veil.hide).toHaveBeenCalledWith('table');
			expect(isCovered(document.getElementById('sort'))).toBe(false);
		});

		it('finds the local veil from the component when the event\'s element is gone', () => {
			const attrs: AjaxAttributes = { u: './x', c: 'sort',
				event: { target: document.createElement('span') } };
			publish(TOPIC.AJAX_CALL_INIT, attrs);
			expect(veil.show).toHaveBeenCalledWith('table');
		});

		it('drops the keys inside the element veiled only', () => {
			publish(TOPIC.AJAX_CALL_INIT, click(document.getElementById('sort')!));
			expect(key(document.getElementById('sort')!, 'keydown', 'Enter').defaultPrevented)
				.toBe(true);
			expect(key(save(), 'keydown', 'Enter').defaultPrevented).toBe(false);
		});
	});

	it('veils nothing in a page without a veil', () => {
		publish(TOPIC.AJAX_CALL_INIT, click(save()));
		expect(guard()).toBeNull();
		expect(key(save(), 'keydown', 'Enter').defaultPrevented).toBe(false);
	});
});
