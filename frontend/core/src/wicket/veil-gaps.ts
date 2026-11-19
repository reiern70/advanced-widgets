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
 * antilia-veil-gaps.js: registers Wicket.VeilGaps, an add-on to Wicket's veil, Wicket.Veil of
 * wicket-veil.js, which Wicket 11 brings with PageVeilBehavior and LocalVeilBehavior. VeilGaps of
 * the library renders it after wicket-veil.js, wherever a page renders that, and it installs
 * itself as it loads. Wicket's veil keeps a second click from reaching a component the first one's
 * answer took away, or from running an action twice; this closes three ways round it.
 *
 * The keyboard. Wicket's veil blocks only the mouse, so Enter or Space pressed twice on a focused
 * button sends two requests. While a veil covers the element a key goes to, Enter and Space are
 * dropped, but where they type: Space in a text field, Enter in a text area.
 *
 * The queue. Wicket's veil goes up when a request is sent, but a request waits in the queue of its
 * channel while another one runs, and a click meanwhile is queued too: a double click behind a
 * request the veil leaves alone, a filter's or a timer's, runs twice. This veils the page, or the
 * element of a local veil, from /ajax/call/init on, which Wicket's Ajax publishes within the
 * event's handler, before it throttles or queues the request, until the request is done, or until
 * it is plain that it will never run: a throttled request replaced by a later one, one its channel
 * drops or ignores, one whose precondition stops it. Wicket's Ajax publishes no topic as a request
 * waits in the queue of its channel, nor for one dropped, throttled away or ignored by its channel
 * (apache/wicket#1942, https://github.com/apache/wicket/issues/1942): this reads Wicket's channels
 * and throttler to tell.
 *
 * The click the veil catches. The mouse goes down on a button while a field's change goes to the
 * server: the veil comes up between the press and the release, and the browser has no click for
 * the button. The click is given back to it once the veil is lowered, if it is still in the page.
 */

import { onWicketTopic, wicket } from './host.js';
import type { AjaxAttributes } from './send-state.js';

/** The extra parameter that leaves a request unveiled: PageVeilBehavior.NO_VEIL_PARAMETER. */
export const NO_VEIL_PARAMETER = 'wicket_nb';

/** The class of Wicket's veils, the page's and the local ones. */
const VEIL_CLASS = 'wicket-veil';

/** The class Wicket's local veil gives the element it covers. */
const HOST_CLASS = 'wicket-veil-host';

/** The class of the veil this puts over the page while a request waits its turn. */
export const GUARD_CLASS = 'antilia-veil-guard';

/** How often a click caught by a veil looks whether the veil is gone, in milliseconds. */
export const REPLAY_POLL = 50;

/** Wicket.Veil, as far as this uses it. */
interface WicketVeil {
	page(options: unknown): void;
	local(id: string, options: unknown): void;
	show(id: string): void;
	hide(id: string): void;
}

/** A channel of Wicket's Ajax, as Wicket.channelManager keeps them. */
interface WicketChannel {
	busy?: boolean;
}

/** The parts of the Wicket global this reads. */
interface WicketInternals {
	Veil?: WicketVeil;
	channelManager?: { channels?: Record<string, WicketChannel | undefined> };
	Throttler?: { entries?: Record<string, unknown> };
}

/** Where a request is in Wicket's Ajax. */
type State = 'waiting' | 'started' | 'sent';

/** The veil a request holds up, from /ajax/call/init on. */
interface Hold {
	/** The attributes of the request, the same object from /ajax/call/init to /ajax/call/done. */
	attrs: object;
	/** The markup id of the local veil's element, null for the page. */
	local: string | null;
	/** Whether Wicket's local veil took the hold. */
	shown: boolean;
	/** The name of the request's channel. */
	channel: string;
	/** The id the request is throttled by, null when not throttled. */
	throttle: string | null;
	/** Waiting for its turn, started (Wicket's Ajax runs it), or sent. */
	state: State;
}

/** A press of the mouse, or a touch, that a veil may catch the release of. */
interface Press {
	target: Element;
	/** The markup id of the target, or of the nearest element around it with one. */
	id: string | null;
}

let installed = false;

let stops: Array<() => void> = [];

/** Whether the page has a page veil: Wicket.Veil.page() was called. */
let pageVeil = false;

/** The markup ids of the elements with a local veil: Wicket.Veil.local() was called for them. */
const localVeils = new Set<string>();

/** The requests holding a veil up, by their attributes. */
const holds = new Map<object, Hold>();

/** The request each throttle id holds back in Wicket's throttler, as far as this knows. */
const throttled = new Map<string, Hold>();

/** The veil over the page while requests wait their turn, and how many hold it up. */
let guard: HTMLElement | null = null;
let guardHolds = 0;

let reconcileTimer: ReturnType<typeof setTimeout> | null = null;

/** The press going on, and the click a veil caught, to give back. */
let press: Press | null = null;
let replay: Press | null = null;
let replayTimer: ReturnType<typeof setTimeout> | null = null;

/** The keys whose keydown was dropped, so that their keyup is too. */
const droppedKeys = new Set<string>();

function internals(): WicketInternals {
	return (window.Wicket ?? {}) as WicketInternals;
}

/**
 * @param attrs the attributes of a request
 * @returns whether the request carries the parameter leaving it unveiled, as Wicket's veil reads it
 */
export function isOptedOut(attrs: AjaxAttributes): boolean {
	const ep = attrs.ep;
	if (Array.isArray(ep)) {
		return ep.some((parameter) => !!parameter &&
			(parameter as { name?: unknown }).name === NO_VEIL_PARAMETER);
	}
	return !!ep && typeof ep === 'object' &&
		Object.prototype.hasOwnProperty.call(ep, NO_VEIL_PARAMETER);
}

/**
 * @param attrs the attributes of a request
 * @returns the veil Wicket's veil takes for it: the markup id of a local veil's element, null for
 *          the page, undefined for none
 */
function veilOf(attrs: AjaxAttributes): string | null | undefined {
	const event = attrs.event as { target?: unknown } | undefined;
	let node: Node | null = event?.target instanceof Node && event.target.isConnected ?
		event.target : null;
	if (!node) {
		node = typeof attrs.c === 'string' ? document.getElementById(attrs.c) : null;
	}
	for (; node && node !== document; node = node.parentNode) {
		if (node instanceof Element && node.id && localVeils.has(node.id)) {
			return node.id;
		}
	}
	return pageVeil ? null : undefined;
}

/**
 * @param channel the channel of a request, as its attribute ch has it: name|type
 * @returns its name and its type, as Wicket.Channel reads them
 */
export function parseChannel(channel: unknown): { name: string; type: string } {
	const parsed = typeof channel === 'string' ? /^([^|]+)\|(d|s|a)$/.exec(channel) : null;
	return parsed ? { name: parsed[1], type: parsed[2] } : { name: '0', type: 's' };
}

/**
 * @param attrs the attributes of a request
 * @returns the id Wicket's throttler keeps it by, null when it is not throttled
 */
function throttleOf(attrs: AjaxAttributes): string | null {
	const settings = attrs.tr as { id?: unknown } | undefined;
	return settings && settings.id !== undefined && settings.id !== null ?
		String(settings.id) : null;
}

/**
 * @param name the name of a channel
 * @returns whether a request of it runs, other requests then waiting their turn
 */
function isBusy(name: string): boolean {
	return internals().channelManager?.channels?.[name]?.busy === true;
}

/**
 * @param id a throttle id
 * @returns whether Wicket's throttler holds a request back under it
 */
function isThrottling(id: string): boolean {
	const entries = internals().Throttler?.entries;
	return !!entries && entries[id] !== undefined;
}

function inThrottler(hold: Hold): boolean {
	return hold.throttle !== null && throttled.get(hold.throttle) === hold &&
		isThrottling(hold.throttle);
}

function raise(hold: Hold): void {
	if (hold.local === null) {
		guardHolds++;
		if (!guard) {
			guard = document.createElement('div');
			guard.className = GUARD_CLASS;
			document.body.appendChild(guard);
		}
		return;
	}
	const veil = internals().Veil;
	if (veil && document.getElementById(hold.local)) {
		veil.show(hold.local);
		hold.shown = true;
	}
}

function lower(hold: Hold): void {
	if (hold.local === null) {
		guardHolds = Math.max(0, guardHolds - 1);
		if (guardHolds === 0 && guard) {
			guard.remove();
			guard = null;
		}
	} else if (hold.shown) {
		hold.shown = false;
		internals().Veil?.hide(hold.local);
	}
}

/**
 * Lets a request's veil down: it is done, or will never run.
 */
function end(hold: Hold): void {
	if (holds.get(hold.attrs) !== hold) {
		return;
	}
	holds.delete(hold.attrs);
	if (hold.throttle !== null && throttled.get(hold.throttle) === hold) {
		throttled.delete(hold.throttle);
	}
	lower(hold);
}

/**
 * Lets down the veils of the requests that will never run: those no channel runs, nor queues,
 * nor the throttler holds back, and those Wicket's Ajax started but neither sent nor finished,
 * which it does at once, so that an error stopped them. Wicket's Ajax tells of none of them by a
 * topic, apache/wicket#1942: Wicket.channelManager and Wicket.Throttler tell instead.
 */
export function reconcile(): void {
	reconcileTimer = null;
	for (const hold of Array.from(holds.values())) {
		if (hold.state === 'sent' || inThrottler(hold) ||
			hold.state === 'waiting' && isBusy(hold.channel)) {
			continue;
		}
		end(hold);
	}
}

function reconcileLater(): void {
	if (reconcileTimer === null) {
		reconcileTimer = setTimeout(reconcile, 0);
	}
}

/**
 * Raises the veil of a request as the event fires it: called with its attributes at
 * /ajax/call/init.
 *
 * @param attrs the attributes of the request, the same object Wicket's Ajax then runs it with
 */
export function onInit(attrs: AjaxAttributes | null | undefined): void {
	if (!attrs || typeof attrs !== 'object') {
		return;
	}
	const throttle = throttleOf(attrs);
	if (throttle !== null) {
		// the throttler keeps one request per id: the one it holds back is replaced by this one
		const previous = throttled.get(throttle);
		if (previous && isThrottling(throttle)) {
			end(previous);
		}
		throttled.delete(throttle);
	}
	if (isOptedOut(attrs)) {
		return;
	}
	const local = veilOf(attrs);
	if (local === undefined) {
		return;
	}
	const channel = parseChannel(attrs.ch);
	if (throttle === null && isBusy(channel.name)) {
		if (channel.type === 'a') {
			// an active channel ignores a request while another one runs
			return;
		}
		if (channel.type === 'd') {
			// a drop channel forgets the requests waiting their turn
			for (const hold of Array.from(holds.values())) {
				if (hold.channel === channel.name && hold.state === 'waiting' &&
					!inThrottler(hold)) {
					end(hold);
				}
			}
		}
	}
	const hold: Hold = { attrs, local, shown: false, channel: channel.name, throttle,
		state: 'waiting' };
	holds.set(attrs, hold);
	if (throttle !== null) {
		throttled.set(throttle, hold);
	}
	raise(hold);
	reconcileLater();
}

function advance(attrs: unknown, state: State): void {
	const hold = attrs && typeof attrs === 'object' ? holds.get(attrs) : undefined;
	if (hold) {
		hold.state = state;
		reconcileLater();
	}
}

function onDone(attrs: unknown): void {
	const hold = attrs && typeof attrs === 'object' ? holds.get(attrs) : undefined;
	if (hold) {
		end(hold);
	}
	reconcileLater();
}

/**
 * @param element an element of the page
 * @returns whether it is a veil: Wicket's, or the one over the page while requests wait their turn
 */
export function isVeil(element: Element | null): boolean {
	return !!element && (element.classList.contains(VEIL_CLASS) ||
		element.classList.contains(GUARD_CLASS));
}

/**
 * @param element an element of the page, null for the page
 * @returns whether a veil covers it: one over the page, or a local one over an element around it
 */
export function isCovered(element: Element | null): boolean {
	if (guard?.isConnected) {
		return true;
	}
	const body = document.body;
	if (body && Array.from(body.children).some(isVeil)) {
		return true;
	}
	for (let node = element; node; node = node.parentElement) {
		if (node.classList.contains(HOST_CLASS) && Array.from(node.children).some(isVeil)) {
			return true;
		}
	}
	return false;
}

/** The types of input taking typed text, Space among it. */
const BUTTON_INPUTS = new Set(['button', 'submit', 'reset', 'image', 'checkbox', 'radio', 'file',
	'color', 'range']);

/**
 * @param element where a key goes
 * @param key Enter, or Space
 * @returns whether the key types there, rather than acts: Space in a text field, a list or a
 *          text area, Enter in a text area, in editable content either
 */
function types(element: Element, key: string): boolean {
	if (element instanceof HTMLTextAreaElement ||
		element instanceof HTMLElement && element.isContentEditable) {
		return true;
	}
	if (key !== ' ') {
		return false;
	}
	return element instanceof HTMLSelectElement ||
		element instanceof HTMLInputElement && !BUTTON_INPUTS.has(element.type);
}

/**
 * Drops Enter and Space going to an element under a veil, but where they type, and the keyup of a
 * key whose keydown it dropped.
 *
 * @param event a keydown or keyup, as it starts down from the window
 * @returns whether it dropped the key
 */
export function onKey(event: KeyboardEvent): boolean {
	const key = event.key === 'Spacebar' ? ' ' : event.key;
	// Enter ending the composition of a character, as an input method has it, types
	if (key !== 'Enter' && key !== ' ' || event.isComposing) {
		return false;
	}
	if (event.type === 'keyup') {
		if (!droppedKeys.delete(key)) {
			return false;
		}
	} else {
		const target = event.target instanceof Element ? event.target : document.activeElement;
		if (!target || !isCovered(target) || types(target, key)) {
			return false;
		}
		droppedKeys.add(key);
	}
	event.preventDefault();
	event.stopImmediatePropagation();
	return true;
}

/**
 * @returns the markup id of the element, or of the nearest element around it with one
 */
function idOf(element: Element): string | null {
	return element.closest('[id]')?.id || null;
}

function isPrimary(event: PointerEvent): boolean {
	return event.isPrimary !== false && event.button === 0;
}

/**
 * Notes a press that no veil covers, which a veil rising may catch the release of.
 *
 * @param event a pointerdown, as it starts down from the window
 */
export function onPointerDown(event: PointerEvent): void {
	const target = event.target instanceof Element ? event.target : null;
	if (!target || isVeil(target) || isCovered(target)) {
		// a press on a veil gives nothing back, and leaves the click it caught before alone
		press = null;
		return;
	}
	press = isPrimary(event) ? { target, id: idOf(target) } : null;
	// a press elsewhere: the click caught before is no longer what the user wants
	cancelReplay();
}

/**
 * Keeps the click of a press whose release a veil caught, to give it back once the veil is down:
 * to what the press and the release are on together, as the browser would have clicked it.
 *
 * @param event a pointerup, as it starts down from the window
 * @returns whether a click is kept
 */
export function onPointerUp(event: PointerEvent): boolean {
	const down = press;
	press = null;
	if (!down || !isPrimary(event)) {
		return false;
	}
	// a touch keeps its pointerup on the element touched: what is under it tells of a veil
	if (!isVeil(document.elementFromPoint(event.clientX, event.clientY))) {
		return false;
	}
	const released = document.elementsFromPoint(event.clientX, event.clientY)
		.find((element) => !isVeil(element));
	let target: Element | null = down.target;
	while (target && released && !target.contains(released)) {
		target = target.parentElement;
	}
	if (!target || target === document.body || target === document.documentElement) {
		return false;
	}
	cancelReplay();
	replay = { target, id: idOf(target) };
	replayTimer = setTimeout(giveBack, REPLAY_POLL);
	return true;
}

function cancelReplay(): void {
	if (replayTimer !== null) {
		clearTimeout(replayTimer);
	}
	replayTimer = null;
	replay = null;
}

/**
 * Gives the click a veil caught back to its element once no veil covers it: the element itself
 * if it is still in the page, else the one that took its markup id, as an Ajax answer replacing
 * it leaves; none if it is no longer shown, or disabled.
 */
export function giveBack(): void {
	replayTimer = null;
	const caught = replay;
	if (!caught) {
		return;
	}
	const target = caught.target.isConnected ? caught.target :
		caught.id ? document.getElementById(caught.id) : null;
	if (!target) {
		replay = null;
		return;
	}
	if (isCovered(target)) {
		replayTimer = setTimeout(giveBack, REPLAY_POLL);
		return;
	}
	replay = null;
	if (target.getClientRects().length === 0 || target.matches(':disabled')) {
		return;
	}
	if (target instanceof HTMLElement) {
		target.click();
	} else {
		target.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true,
			composed: true, view: window, detail: 1 }));
	}
}

function onPointerCancel(): void {
	press = null;
}

/**
 * @param listener a listener of a topic of Wicket.Event
 * @returns the listener, telling Wicket's log of an error rather than throwing it: Wicket.Event
 *          tells the topic to its listeners one after the other, and Wicket's Ajax sends a request
 *          after it told /ajax/call/init, so an error would stop both
 */
function safely<T>(listener: (payload: T) => void): (payload: T) => void {
	return (payload) => {
		try {
			listener(payload);
		} catch (error) {
			const log = window.Wicket?.Log;
			if (log?.error) {
				log.error('Wicket.VeilGaps:', error);
			} else {
				console.error('Wicket.VeilGaps:', error);
			}
		}
	};
}

/**
 * Learns the veils of the page from the calls registering them with Wicket.Veil: the page veil
 * and the elements with a local veil.
 */
function watchVeil(veil: WicketVeil): () => void {
	const { page, local } = veil;
	veil.page = function (this: unknown, options: unknown) {
		pageVeil = true;
		return page.call(this, options);
	};
	veil.local = function (this: unknown, id: string, options: unknown) {
		localVeils.add(id);
		return local.call(this, id, options);
	};
	return () => {
		veil.page = page;
		veil.local = local;
	};
}

/**
 * Closes the gaps of Wicket's veil in this page: learns its veils from Wicket.Veil, and listens to
 * the requests of Wicket's Ajax, to the keys and to the presses. Once per page; the script calls it
 * as it loads.
 */
export function install(): void {
	if (installed) {
		return;
	}
	const veil = internals().Veil;
	if (!veil) {
		return;
	}
	installed = true;
	stops = [
		watchVeil(veil),
		onWicketTopic<AjaxAttributes>('AJAX_CALL_INIT', '/ajax/call/init', safely(onInit)),
		onWicketTopic<AjaxAttributes>('AJAX_CALL_BEFORE', '/ajax/call/before',
			safely((attrs) => advance(attrs, 'started'))),
		onWicketTopic<AjaxAttributes>('AJAX_CALL_BEFORE_SEND', '/ajax/call/beforeSend',
			safely((attrs) => advance(attrs, 'sent'))),
		onWicketTopic<AjaxAttributes>('AJAX_CALL_DONE', '/ajax/call/done', safely(onDone))
	];
	const listeners: Array<[string, (event: never) => unknown]> = [['keydown', onKey],
		['keyup', onKey], ['pointerdown', onPointerDown], ['pointerup', onPointerUp],
		['pointercancel', onPointerCancel]];
	listeners.forEach(([type, listener]) => window.addEventListener(type,
		listener as EventListener, true));
	stops.push(() => listeners.forEach(([type, listener]) => window.removeEventListener(type,
		listener as EventListener, true)));
}

/**
 * Forgets the page and lowers what this raised, for the tests.
 */
export function uninstall(): void {
	stops.forEach((stop) => stop());
	stops = [];
	installed = false;
	pageVeil = false;
	localVeils.clear();
	for (const hold of Array.from(holds.values())) {
		end(hold);
	}
	throttled.clear();
	guard?.remove();
	guard = null;
	guardHolds = 0;
	if (reconcileTimer !== null) {
		clearTimeout(reconcileTimer);
		reconcileTimer = null;
	}
	cancelReplay();
	press = null;
	droppedKeys.clear();
}

/** Wicket.VeilGaps. */
export const WicketVeilGaps = {
	install,
	/** Whether a veil covers an element: the page's, a local one around it. */
	isCovered
};

const Wicket = wicket();
if (Wicket.VeilGaps === undefined) {
	Wicket.VeilGaps = WicketVeilGaps;
	install();
}
