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
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { registerRichTextEvent, RichTextEditor, setHost, standaloneHost,
	type Host } from '../src/index.js';
import { asParameters, attachClientEvents, plainValues, roundTrip,
	type AjaxAttributes } from '../src/wicket/client-events.js';

/** A request Wicket's Ajax was asked to send: its attributes, its extra parameters as sent. */
type Sent = AjaxAttributes & { ep: Array<{ name: string; value: string }> };

/** The attributes of the requests of an event going to the server, as the server renders them. */
const AJAX: AjaxAttributes = { u: 'page?1-1.0-editor', m: 'POST', c: 'editor' };

/**
 * @returns the requests Wicket's Ajax is asked to send from now on, each as the call gets its
 *          attributes, after their init handlers ran
 */
function requests(): Sent[] {
	const heard: Sent[] = [];
	(window.Wicket as Record<string, unknown>).Ajax = {
		Call: class {
			ajax(attributes: Sent) {
				heard.push(attributes);
			}

			_executeHandlers(handlers: unknown, attributes: AjaxAttributes) {
				(Array.isArray(handlers) ? handlers : []).forEach((handler) => handler(attributes));
			}
		}
	};
	return heard;
}

/** @returns the parameters a request carries, by their names: those the event adds among them */
function parameters(request: Sent): Record<string, string> {
	return Object.fromEntries(request.ep.map(({ name, value }) => [name, value]));
}

/** @returns the parameters the request carries for the event: its name, its values */
function event(request: Sent): [string, string] {
	const sent = parameters(request);
	return [sent['antilia-event'], sent['antilia-event-values']];
}

describe('client events', () => {
	let element: HTMLTextAreaElement;

	beforeEach(() => {
		element = document.createElement('textarea');
		element.id = 'editor';
		document.body.appendChild(element);
	});

	afterEach(() => {
		document.body.innerHTML = '';
		vi.useRealTimers();
		setHost();
		delete (window.Wicket as Record<string, unknown>).Ajax;
	});

	it('registers Wicket.ClientEvents', () => {
		const global = window.Wicket?.ClientEvents as { attach: unknown; detach: unknown;
			roundTrip: unknown };
		expect(global.attach).toBe(attachClientEvents);
		expect(global.roundTrip).toBe(roundTrip);
		expect(typeof global.detach).toBe('function');
	});

	it('sends the data of the detail of an event going to the server, with its attributes', () => {
		const heard = requests();
		attachClientEvents('editor', [{ eventName: 'antilia:resize', index: 2, ajax: AJAX,
			eventClientHandlerName: 'Wicket.ClientEvents.send' }]);

		element.dispatchEvent(new CustomEvent('antilia:resize', { detail: {
			editor: new (class Editor {})(), size: { width: 640, height: 320 } } }));
		expect(heard).toEqual([{ ...AJAX, ep: [{ name: 'antilia-event', value: 'antilia:resize' },
			{ name: 'antilia-event-values', value: '{"size":{"width":640,"height":320}}' }] }]);
	});

	it('sends the events of a type of the rich text editors as any other, without the editor', () => {
		registerRichTextEvent('test-client:asked', { fire: (editor, _state, cause) =>
			({ asked: cause.detail.what, length: editor.getText().length, node: document.body }) });
		element.defaultValue = '<p>Text</p>';
		const editor = new RichTextEditor(element);
		const heard = requests();
		attachClientEvents('editor', [{ eventName: 'test-client:asked', index: 0, ajax: AJAX,
			eventClientHandlerName: 'Wicket.ClientEvents.send' }],
			(found) => RichTextEditor.of(found.id));

		editor.trigger('test-client:asked', { what: 'size' });
		expect(heard.map(event)).toEqual([['test-client:asked',
			'{"asked":"size","length":4}']]);
	});

	it('runs the function, which sends what it chooses, by the attributes the event came with', () => {
		const heard = requests();
		const component = { name: 'the editor' };
		const seen: unknown[] = [];
		// those of a request submitting the form
		const submit = { u: 'page?1-1.1-editor', m: 'POST', c: 'editor', f: 'form1' };
		attachClientEvents('editor', [{ eventName: 'input', index: 0, ajax: submit,
			eventHandlerClientSideCode(event, editor, options, send) {
				seen.push(event.type, editor, options.eventName, this);
				send({ length: 3 });
			} }], () => component);

		element.dispatchEvent(new Event('input'));
		expect(seen).toEqual(['input', component, 'input', element]);
		expect(heard).toHaveLength(1);
		expect(heard[0]).toMatchObject(submit);
		expect(event(heard[0])).toEqual(['input', '{"length":3}']);
	});

	it('keeps an event without attributes in the browser', () => {
		const heard = requests();
		const send = vi.fn();
		attachClientEvents('editor', [{ eventName: 'antilia:resize', index: 0,
			eventHandlerClientSideCode(event, _component, _options, sender) {
				document.cookie = 'size=' + JSON.stringify((event as CustomEvent).detail.size);
				send();
				sender({ never: true });
			} }]);

		element.dispatchEvent(new CustomEvent('antilia:resize', { detail: { size: { width: 5 } } }));
		expect(document.cookie).toContain('size={"width":5}');
		expect(send).toHaveBeenCalledOnce();
		expect(heard).toEqual([]);
		// one handled by the built-in nothing does nothing
		attachClientEvents('editor', [{ eventName: 'focus', index: 0,
			eventClientHandlerName: 'Wicket.ClientEvents.nothing' }]);
		element.dispatchEvent(new Event('focus'));
		// and the built-in send sends nothing without attributes
		attachClientEvents('editor', [{ eventName: 'focus', index: 0,
			eventClientHandlerName: 'Wicket.ClientEvents.send' }]);
		element.dispatchEvent(new Event('focus'));
		expect(heard).toEqual([]);
	});

	it('gives the handler the JSON of the event, its properties configuring it', () => {
		const heard = requests();
		const options = { eventName: 'antilia:resize', index: 0,
			eventClientHandlerName: 'Sizes.keep', cookieName: 'size' };
		const seen: unknown[] = [];
		(window as unknown as Record<string, unknown>).Sizes = {
			// staying in the browser, it takes no send
			keep(event: Event, _component: unknown, given: typeof options) {
				seen.push(given);
				document.cookie = given.cookieName + '=' + (event as CustomEvent).detail.width;
			}
		};
		try {
			attachClientEvents('editor', [options]);

			element.dispatchEvent(new CustomEvent('antilia:resize', { detail: { width: 7 } }));
			expect(seen).toEqual([options]);
			expect(document.cookie).toContain('size=7');
			expect(heard).toEqual([]);
		} finally {
			delete (window as unknown as Record<string, unknown>).Sizes;
		}
	});

	it('runs each event of one name: one in the browser, one going to the server', () => {
		const heard = requests();
		attachClientEvents('editor', [
			{ eventName: 'antilia:resize', index: 0, eventHandlerClientSideCode(event) {
				document.cookie = 'kept=' + (event as CustomEvent).detail.size.height;
			} },
			{ eventName: 'antilia:resize', index: 1, ajax: AJAX,
				eventClientHandlerName: 'Wicket.ClientEvents.send' }]);

		element.dispatchEvent(new CustomEvent('antilia:resize', { detail: { size: { height: 300 } } }));
		expect(document.cookie).toContain('kept=300');
		expect(heard.map(event)).toEqual([['antilia:resize', '{"size":{"height":300}}']]);
	});

	it('waits for the events to pause, each postponing the one waiting', () => {
		vi.useFakeTimers();
		const heard = requests();
		attachClientEvents('editor', [{ eventName: 'input', index: 0, ajax: AJAX, delay: 500,
			eventHandlerClientSideCode: (event, _component, _options, send) => send((event as CustomEvent).detail) }]);

		element.dispatchEvent(new CustomEvent('input', { detail: 1 }));
		vi.advanceTimersByTime(400);
		element.dispatchEvent(new CustomEvent('input', { detail: 2 }));
		vi.advanceTimersByTime(400);
		expect(heard).toEqual([]);
		vi.advanceTimersByTime(100);
		// the last one, a value which is not an object wrapped
		expect(heard.map((request) => event(request)[1])).toEqual(['{"value":2}']);

		// gone from the page meanwhile: nothing
		element.dispatchEvent(new CustomEvent('input', { detail: 3 }));
		element.remove();
		vi.advanceTimersByTime(500);
		expect(heard).toHaveLength(1);
	});

	it('takes off the events it listened to when attached again or detached', () => {
		const heard = requests();
		attachClientEvents('editor', [{ eventName: 'focus', index: 0, ajax: AJAX,
			eventClientHandlerName: 'Wicket.ClientEvents.send' }]);
		attachClientEvents('editor', [{ eventName: 'blur', index: 0, ajax: AJAX,
			eventClientHandlerName: 'Wicket.ClientEvents.send' }]);

		element.dispatchEvent(new Event('focus'));
		element.dispatchEvent(new Event('blur'));
		expect(heard.map((request) => event(request)[0])).toEqual(['blur']);

		(window.Wicket?.ClientEvents as { detach(id: string): void }).detach('editor');
		element.dispatchEvent(new Event('blur'));
		expect(heard).toHaveLength(1);
		expect(attachClientEvents('missing', [])).toBeNull();
	});

	it('logs a handler failing or missing, or values that are not JSON, and goes on', () => {
		const errors: unknown[] = [];
		const host: Host = { ...standaloneHost, log: { error: (message) => errors.push(message) } };
		setHost(host);
		const heard = requests();
		const circular: Record<string, unknown> = {};
		circular.self = circular;
		attachClientEvents('editor', [
			{ eventName: 'focus', index: 0, ajax: AJAX, eventHandlerClientSideCode: () => { throw new Error('broken'); } },
			{ eventName: 'focus', index: 1, ajax: AJAX, eventHandlerClientSideCode: (_event, _component, _options, send) => send(circular) },
			{ eventName: 'focus', index: 2, ajax: { ...AJAX, u: 'page?1-1.2-editor' }, eventClientHandlerName: 'Wicket.ClientEvents.send' },
			{ eventName: 'focus', index: 3, ajax: AJAX, eventClientHandlerName: 'Nowhere.toBeFound' }]);

		element.dispatchEvent(new Event('focus'));
		expect(errors).toEqual(['The handler of focus failed', 'The values of focus are not JSON',
			'The handler of focus, Nowhere.toBeFound, is no function of the page']);
		// the request of the third alone, to the URL of its own behavior
		expect(heard.map((request) => [request.u, event(request)[1]])).toEqual([
			['page?1-1.2-editor', '{}']]);
	});

	it('runs the function of the page named, a path to it, found as the event comes', () => {
		const heard = requests();
		const component = { name: 'the editor' };
		attachClientEvents('editor', [{ eventName: 'input', index: 0, ajax: AJAX,
			eventClientHandlerName: 'Articles.onInput' }], () => component);
		const seen: unknown[] = [];
		(window as unknown as Record<string, unknown>).Articles = {
			onInput(event: Event, editor: unknown, _options: unknown, send: (values?: unknown) => void) {
				seen.push(event.type, editor);
				send({ typed: true });
			}
		};
		try {
			element.dispatchEvent(new Event('input'));
		} finally {
			delete (window as unknown as Record<string, unknown>).Articles;
		}
		expect(seen).toEqual(['input', component]);
		expect(heard.map((request) => event(request)[1])).toEqual(['{"typed":true}']);
	});

	it('runs the code of an event in place of the function named', () => {
		const heard = requests();
		attachClientEvents('editor', [{ eventName: 'input', index: 0, ajax: AJAX,
			eventClientHandlerName: 'Wicket.ClientEvents.send',
			eventHandlerClientSideCode: (_event, _component, _options, send) => send({ code: true }) }]);

		element.dispatchEvent(new CustomEvent('input', { detail: { code: false } }));
		expect(heard.map((request) => event(request)[1])).toEqual(['{"code":true}']);
	});

	it('sends with each event what the component adds, its state for example', () => {
		const heard = requests();
		const component = { state: 1 };
		attachClientEvents('editor', [{ eventName: 'input', index: 0, ajax: AJAX,
			eventClientHandlerName: 'Wicket.ClientEvents.send' }], () => component, {
			values: (editor) => ({ state: String((editor as typeof component).state++) }) });

		element.dispatchEvent(new Event('input'));
		element.dispatchEvent(new Event('input'));
		expect(heard.map((request) => parameters(request).state)).toEqual(['1', '2']);
	});

	it('sends the state only when the component has one not sent yet', () => {
		const heard = requests();
		let unsent = true;
		const component = { hasUnsentState: () => unsent };
		attachClientEvents('editor', [{ eventName: 'blur', index: 0, ajax: AJAX,
			eventClientHandlerName: 'Wicket.ClientEvents.sendState' }], () => component);

		element.dispatchEvent(new Event('blur'));
		unsent = false;
		element.dispatchEvent(new Event('blur'));
		expect(heard).toHaveLength(1);
		// a component without a state: always
		attachClientEvents('editor', [{ eventName: 'blur', index: 0, ajax: AJAX,
			eventClientHandlerName: 'Wicket.ClientEvents.sendState' }]);
		element.dispatchEvent(new Event('blur'));
		expect(heard).toHaveLength(2);
	});

	it('sends the extra parameters of an event, and its dynamic ones, with its own requests', () => {
		const heard = requests();
		attachClientEvents('editor', [
			// as Wicket renders them: an object, a name having several values in an array
			{ eventName: 'blur', index: 0, eventClientHandlerName: 'Wicket.ClientEvents.send',
				ajax: { ...AJAX, ep: { _nb: 'true', tag: ['a', 'b'] },
					dep: [(attrs: AjaxAttributes) => ({ to: attrs.u })] } },
			{ eventName: 'input', index: 1, ajax: AJAX,
				eventClientHandlerName: 'Wicket.ClientEvents.send' }]);

		element.dispatchEvent(new Event('blur'));
		element.dispatchEvent(new Event('input'));
		// the extra parameters first, in the ep of the request, which a page veil sees
		expect(heard[0].ep.slice(0, 3)).toEqual([{ name: '_nb', value: 'true' },
			{ name: 'tag', value: 'a' }, { name: 'tag', value: 'b' }]);
		// the dynamic ones Wicket's Ajax asks as it sends the request
		expect(heard[0].dep?.length).toBe(1);
		expect(parameters(heard[1])._nb).toBeUndefined();
	});

	it('adds the attributes a handler sends with the values to those of the event', () => {
		const heard = requests();
		const success = () => 'event';
		const added = () => 'handler';
		attachClientEvents('editor', [{ eventName: 'blur', index: 0,
			ajax: { ...AJAX, ep: [{ name: '_nb', value: 'true' }], sh: [success] },
			eventHandlerClientSideCode: (_event, _component, _options, send) => send({ left: true },
				{ m: 'GET', ep: { quick: '1' }, sh: [added], fh: [added] }) }]);

		element.dispatchEvent(new Event('blur'));
		expect(heard).toHaveLength(1);
		// in place of the event's, but the lists, which add to them
		expect(heard[0].m).toBe('GET');
		expect(heard[0].sh).toEqual([success, added]);
		expect(heard[0].fh).toEqual([added]);
		expect(heard[0].ep.map(({ name }) => name)).toEqual(['_nb', 'quick', 'antilia-event',
			'antilia-event-values']);
	});

	it('runs the init handlers of a round trip and tells Wicket of it, as Wicket does for an event', () => {
		const heard = requests();
		const published: Array<[string, unknown]> = [];
		const global = window.Wicket as Record<string, unknown>;
		global.Event = { publish: (topic: string, attrs: unknown) => published.push([topic, attrs]),
			subscribe() { /* none */ }, unsubscribe() { /* none */ } };
		const inits: unknown[] = [];
		try {
			expect(roundTrip({ u: 'page?1', ep: { a: '1' }, ih: [(attrs: unknown) => inits.push(attrs)] },
				[{ name: 'b', value: '2' }])).toBe(true);
		} finally {
			delete global.Event;
		}
		expect(heard).toHaveLength(1);
		expect(heard[0].ep).toEqual([{ name: 'a', value: '1' }, { name: 'b', value: '2' }]);
		expect(inits).toEqual([heard[0]]);
		expect(published).toEqual([['/ajax/call/init', heard[0]]]);

		// nothing without Wicket's Ajax, nor without a URL
		const errors: unknown[] = [];
		setHost({ ...standaloneHost, log: { error: (message) => errors.push(message) } });
		expect(roundTrip({})).toBe(false);
		delete (window.Wicket as Record<string, unknown>).Ajax;
		expect(roundTrip({ u: 'page?1' })).toBe(false);
		expect(errors).toHaveLength(2);
		expect(heard).toHaveLength(1);
	});

	it('sends an event telling the page goes in a beacon, as Wicket\'s Ajax would send it', () => {
		const heard = requests();
		const beacons: Array<[string, string]> = [];
		const beacon = vi.fn((url: string, body: URLSearchParams) => {
			beacons.push([url, body.toString()]);
			return true;
		});
		Object.defineProperty(navigator, 'sendBeacon', { value: beacon, configurable: true });
		element.name = 'text';
		element.value = 'Typed';
		try {
			attachClientEvents('editor', [{ eventName: 'leave', index: 3,
				eventClientHandlerName: 'Wicket.ClientEvents.sendState',
				ajax: { ...AJAX, ep: { _nb: 'true' }, dep: [() => ({ at: '9' })] } }], null,
			{ values: () => ({ state: '{}' }) });

			element.dispatchEvent(new CustomEvent('leave', { detail: { leaving: true } }));
			// without a URL, it goes via Ajax, which tells what is wrong
			attachClientEvents('editor', [{ eventName: 'leave', index: 0, ajax: { c: 'editor' },
				eventClientHandlerName: 'Wicket.ClientEvents.send' }]);
			setHost({ ...standaloneHost, log: { error: () => undefined } });
			element.dispatchEvent(new CustomEvent('leave', { detail: { leaving: true } }));
		} finally {
			delete (navigator as unknown as Record<string, unknown>).sendBeacon;
		}
		// the extra parameters, those of the event, the dynamic ones, then the field
		expect(beacons).toEqual([['page?1-1.0-editor&wicket-ajax=true', '_nb=true&state=%7B%7D&' +
			'antilia-event=leave&antilia-event-values=%7B%22leaving%22%3Atrue%7D&' +
			'at=9&text=Typed&wicket-ajax-baseurl=']]);
		expect(heard).toEqual([]);
	});

	it('takes the parameters as Wicket\'s Ajax takes them', () => {
		expect(asParameters(undefined)).toEqual([]);
		expect(asParameters('a=1')).toEqual([]);
		expect(asParameters({ a: 1, b: ['x', null], c: null })).toEqual([{ name: 'a', value: '1' },
			{ name: 'b', value: 'x' }]);
		expect(asParameters([{ name: 'a', value: true }, { value: 'no name' }, null]))
			.toEqual([{ name: 'a', value: 'true' }]);
	});

	it('carries what a component did not send with the page\'s other Ajax requests', () => {
		type Attributes = { c?: unknown; dep?: unknown[] };
		const subscribers: Array<(event: unknown, attrs: Attributes) => void> = [];
		const global = window.Wicket as Record<string, unknown>;
		global.Event = {
			subscribe(topic: string, subscriber: (event: unknown, attrs: Attributes) => void) {
				if (topic === '/ajax/call/before') {
					subscribers.push(subscriber);
				}
			},
			unsubscribe() {
				// never in this test
			},
			Topic: { AJAX_CALL_BEFORE: '/ajax/call/before' }
		};
		const before = (attrs: Attributes) => {
			subscribers.forEach((subscriber) => subscriber({}, attrs));
			return attrs.dep as Array<string | (() => Record<string, string>)>;
		};
		const component = { name: 'the editor' };
		let state = 0;
		let unsent = true;
		try {
			attachClientEvents('editor', [], () => component, {
				pending(given) {
					expect(given).toBe(component);
					if (!unsent) {
						return null;
					}
					unsent = false;
					return { 'antilia-state': String(++state) };
				} });

			// another component's request, asked as it goes, after its own dynamic parameters
			const dep = before({ c: 'link', dep: ['return {a: 1};'] });
			expect(dep[0]).toBe('return {a: 1};');
			expect(state).toBe(0);
			expect((dep[1] as () => unknown)()).toEqual({
				'antilia-pending:editor': '{"antilia-state":"1"}' });
			// taken as sent
			expect((dep[1] as () => unknown)()).toEqual({});
			// the component's own events send it themselves
			unsent = true;
			expect((before({ c: 'editor' })[0] as () => unknown)()).toEqual({});
			expect((before({ c: element })[0] as () => unknown)()).toEqual({});
			// a request of the page, not of a component
			expect((before({ c: window })[0] as () => unknown)()).toEqual({
				'antilia-pending:editor': '{"antilia-state":"2"}' });
			// detached, it gives nothing more: without components to ask, the request is left alone
			unsent = true;
			(window.Wicket?.ClientEvents as { detach(id: string): void }).detach('editor');
			expect(before({ c: 'link' })).toBeUndefined();
			// replaced by Wicket's Ajax, neither
			attachClientEvents('editor', [], () => component, { pending: () => ({ state: '3' }) });
			element.remove();
			expect((before({ c: 'link' })[0] as () => unknown)()).toEqual({});
			expect(before({ c: 'link' })).toBeUndefined();
		} finally {
			delete global.Event;
		}
	});

	it('tells scripts the names of the parameters', () => {
		const global = window.Wicket?.ClientEvents as Record<string, unknown>;
		expect(global.AJAX_EVENT).toBeUndefined();
		expect(global.parameters).toEqual({ event: 'antilia-event',
			values: 'antilia-event-values', state: 'antilia-state',
			textChanged: 'antilia-text-changed', pendingPrefix: 'antilia-pending:' });
	});

	it('keeps the data of a detail alone', () => {
		expect(plainValues(undefined)).toEqual({});
		expect(plainValues(null)).toEqual({});
		expect(plainValues(5)).toEqual({ value: 5 });
		expect(plainValues([1, 2])).toEqual({ value: [1, 2] });
		expect(plainValues(() => 1)).toEqual({});
		expect(plainValues(document.body)).toEqual({});
		expect(plainValues({ modified: true, editor: document.body, run: () => 1,
			nested: Object.create(null) })).toEqual({ modified: true, nested: {} });
	});
});
