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
import { attachClientEvents, CLIENT_EVENTS_AJAX, CLIENT_EVENTS_SUBMIT, plainValues,
	type ClientEventRequest } from '../src/wicket/client-events.js';

/** The requests the element asked of the server, by the private events it fired. */
function requests(element: HTMLElement): Array<{ type: string } & ClientEventRequest> {
	const heard: Array<{ type: string } & ClientEventRequest> = [];
	[CLIENT_EVENTS_AJAX, CLIENT_EVENTS_SUBMIT].forEach((type) => element.addEventListener(type,
		(event) => heard.push({ type, ...(event as CustomEvent<ClientEventRequest>).detail })));
	return heard;
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
	});

	it('registers Wicket.ClientEvents', () => {
		const global = window.Wicket?.ClientEvents as { attach: unknown; detach: unknown };
		expect(global.attach).toBe(attachClientEvents);
		expect(typeof global.detach).toBe('function');
	});

	it('sends the data of the detail of an Ajax event by the built-in handler', () => {
		const heard = requests(element);
		attachClientEvents('editor', [{ eventName: 'antilia:resize', index: 2, kind: 'ajax', eventClientHandlerName: 'Wicket.ClientEvents.send' }]);

		element.dispatchEvent(new CustomEvent('antilia:resize', { detail: {
			editor: new (class Editor {})(), size: { width: 640, height: 320 } } }));
		expect(heard).toEqual([{ type: CLIENT_EVENTS_AJAX, event: 'antilia:resize', index: 2,
			values: '{"size":{"width":640,"height":320}}', extra: {} }]);
	});

	it('sends the events of a type of the rich text editors as any other, without the editor', () => {
		registerRichTextEvent('test-client:asked', { fire: (editor, _state, cause) =>
			({ asked: cause.detail.what, length: editor.getText().length, node: document.body }) });
		element.defaultValue = '<p>Text</p>';
		const editor = new RichTextEditor(element);
		const heard = requests(element);
		attachClientEvents('editor', [{ eventName: 'test-client:asked', index: 0, kind: 'ajax', eventClientHandlerName: 'Wicket.ClientEvents.send' }],
			(found) => RichTextEditor.of(found.id));

		editor.trigger('test-client:asked', { what: 'size' });
		expect(heard).toEqual([{ type: CLIENT_EVENTS_AJAX, event: 'test-client:asked', index: 0,
			values: '{"asked":"size","length":4}', extra: {} }]);
	});

	it('runs the function, which sends what it chooses, submitting the form', () => {
		const heard = requests(element);
		const component = { name: 'the editor' };
		const seen: unknown[] = [];
		attachClientEvents('editor', [{ eventName: 'input', index: 0, kind: 'submit',
			eventHandlerClientSideCode(event, editor, send) {
				seen.push(event.type, editor, this);
				send({ length: 3 });
			} }], () => component);

		element.dispatchEvent(new Event('input'));
		expect(seen).toEqual(['input', component, element]);
		expect(heard).toEqual([{ type: CLIENT_EVENTS_SUBMIT, event: 'input', index: 0,
			values: '{"length":3}', extra: {} }]);
	});

	it('keeps a client event in the browser', () => {
		const heard = requests(element);
		const send = vi.fn();
		attachClientEvents('editor', [{ eventName: 'antilia:resize', index: 0, kind: 'client',
			eventHandlerClientSideCode(event, _component, sender) {
				document.cookie = 'size=' + JSON.stringify((event as CustomEvent).detail.size);
				send();
				sender({ never: true });
			} }]);

		element.dispatchEvent(new CustomEvent('antilia:resize', { detail: { size: { width: 5 } } }));
		expect(document.cookie).toContain('size={"width":5}');
		expect(send).toHaveBeenCalledOnce();
		expect(heard).toEqual([]);
		// a client event handled by the built-in nothing does nothing
		attachClientEvents('editor', [{ eventName: 'focus', index: 0, kind: 'client',
			eventClientHandlerName: 'Wicket.ClientEvents.nothing' }]);
		element.dispatchEvent(new Event('focus'));
		expect(heard).toEqual([]);
	});

	it('gives the handler the JSON of the event, its properties configuring it', () => {
		const heard = requests(element);
		const options = { eventName: 'antilia:resize', index: 0, kind: 'client' as const,
			eventClientHandlerName: 'Sizes.keep', cookieName: 'size' };
		const seen: unknown[] = [];
		(window as unknown as Record<string, unknown>).Sizes = {
			keep(event: Event, _component: unknown, _send: unknown, given: typeof options) {
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
		const heard = requests(element);
		attachClientEvents('editor', [
			{ eventName: 'antilia:resize', index: 0, kind: 'client', eventHandlerClientSideCode(event) {
				document.cookie = 'kept=' + (event as CustomEvent).detail.size.height;
			} },
			{ eventName: 'antilia:resize', index: 1, kind: 'ajax', eventClientHandlerName: 'Wicket.ClientEvents.send' }]);

		element.dispatchEvent(new CustomEvent('antilia:resize', { detail: { size: { height: 300 } } }));
		expect(document.cookie).toContain('kept=300');
		expect(heard).toEqual([{ type: CLIENT_EVENTS_AJAX, event: 'antilia:resize', index: 1,
			values: '{"size":{"height":300}}', extra: {} }]);
	});

	it('waits for the events to pause, each postponing the one waiting', () => {
		vi.useFakeTimers();
		const heard = requests(element);
		attachClientEvents('editor', [{ eventName: 'input', index: 0, kind: 'ajax', delay: 500,
			eventHandlerClientSideCode: (event, _component, send) => send((event as CustomEvent).detail) }]);

		element.dispatchEvent(new CustomEvent('input', { detail: 1 }));
		vi.advanceTimersByTime(400);
		element.dispatchEvent(new CustomEvent('input', { detail: 2 }));
		vi.advanceTimersByTime(400);
		expect(heard).toEqual([]);
		vi.advanceTimersByTime(100);
		// the last one, a value which is not an object wrapped
		expect(heard.map((request) => request.values)).toEqual(['{"value":2}']);

		// gone from the page meanwhile: nothing
		element.dispatchEvent(new CustomEvent('input', { detail: 3 }));
		element.remove();
		vi.advanceTimersByTime(500);
		expect(heard).toHaveLength(1);
	});

	it('takes off the events it listened to when attached again or detached', () => {
		const heard = requests(element);
		attachClientEvents('editor', [{ eventName: 'focus', index: 0, kind: 'ajax', eventClientHandlerName: 'Wicket.ClientEvents.send' }]);
		attachClientEvents('editor', [{ eventName: 'blur', index: 0, kind: 'ajax', eventClientHandlerName: 'Wicket.ClientEvents.send' }]);

		element.dispatchEvent(new Event('focus'));
		element.dispatchEvent(new Event('blur'));
		expect(heard.map((request) => request.event)).toEqual(['blur']);

		(window.Wicket?.ClientEvents as { detach(id: string): void }).detach('editor');
		element.dispatchEvent(new Event('blur'));
		expect(heard).toHaveLength(1);
		expect(attachClientEvents('missing', [])).toBeNull();
	});

	it('logs a handler failing or missing, or values that are not JSON, and goes on', () => {
		const errors: unknown[] = [];
		const host: Host = { ...standaloneHost, log: { error: (message) => errors.push(message) } };
		setHost(host);
		const heard = requests(element);
		const circular: Record<string, unknown> = {};
		circular.self = circular;
		attachClientEvents('editor', [
			{ eventName: 'focus', index: 0, kind: 'ajax', eventHandlerClientSideCode: () => { throw new Error('broken'); } },
			{ eventName: 'focus', index: 1, kind: 'ajax', eventHandlerClientSideCode: (_event, _component, send) => send(circular) },
			{ eventName: 'focus', index: 2, kind: 'ajax', eventClientHandlerName: 'Wicket.ClientEvents.send' },
			{ eventName: 'focus', index: 3, kind: 'ajax', eventClientHandlerName: 'Nowhere.toBeFound' }]);

		element.dispatchEvent(new Event('focus'));
		expect(errors).toEqual(['The handler of focus failed', 'The values of focus are not JSON',
			'The handler of focus, Nowhere.toBeFound, is no function of the page']);
		expect(heard.map((request) => [request.index, request.values])).toEqual([[2, '{}']]);
	});

	it('runs the function of the page named, a path to it, found as the event comes', () => {
		const heard = requests(element);
		const component = { name: 'the editor' };
		attachClientEvents('editor', [{ eventName: 'input', index: 0, kind: 'ajax',
			eventClientHandlerName: 'Articles.onInput' }], () => component);
		const seen: unknown[] = [];
		(window as unknown as Record<string, unknown>).Articles = {
			onInput(event: Event, editor: unknown, send: (values?: unknown) => void) {
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
		expect(heard.map((request) => request.values)).toEqual(['{"typed":true}']);
	});

	it('runs the code of an event in place of the function named', () => {
		const heard = requests(element);
		attachClientEvents('editor', [{ eventName: 'input', index: 0, kind: 'ajax',
			eventClientHandlerName: 'Wicket.ClientEvents.send',
			eventHandlerClientSideCode: (_event, _component, send) => send({ code: true }) }]);

		element.dispatchEvent(new CustomEvent('input', { detail: { code: false } }));
		expect(heard.map((request) => request.values)).toEqual(['{"code":true}']);
	});

	it('sends with each event what the component adds, its state for example', () => {
		const heard = requests(element);
		const component = { state: 1 };
		attachClientEvents('editor', [{ eventName: 'input', index: 0, kind: 'ajax',
			eventClientHandlerName: 'Wicket.ClientEvents.send' }], () => component, {
			values: (editor) => ({ state: String((editor as typeof component).state++) }) });

		element.dispatchEvent(new Event('input'));
		element.dispatchEvent(new Event('input'));
		expect(heard.map((request) => request.extra)).toEqual([{ state: '1' }, { state: '2' }]);
	});

	it('sends the state only when the component has one not sent yet', () => {
		const heard = requests(element);
		let unsent = true;
		const component = { hasUnsentState: () => unsent };
		attachClientEvents('editor', [{ eventName: 'blur', index: 0, kind: 'ajax',
			eventClientHandlerName: 'Wicket.ClientEvents.sendState' }], () => component);

		element.dispatchEvent(new Event('blur'));
		unsent = false;
		element.dispatchEvent(new Event('blur'));
		expect(heard).toHaveLength(1);
		// a component without a state: always
		attachClientEvents('editor', [{ eventName: 'blur', index: 0, kind: 'ajax',
			eventClientHandlerName: 'Wicket.ClientEvents.sendState' }]);
		element.dispatchEvent(new Event('blur'));
		expect(heard).toHaveLength(2);
	});

	it('sends an event telling the page goes in a beacon, with the field and what the component adds', () => {
		const heard = requests(element);
		const beacons: Array<[string, string]> = [];
		const beacon = vi.fn((url: string, body: URLSearchParams) => {
			beacons.push([url, body.toString()]);
			return true;
		});
		Object.defineProperty(navigator, 'sendBeacon', { value: beacon, configurable: true });
		element.name = 'text';
		element.value = 'Typed';
		try {
			attachClientEvents('editor', [{ eventName: 'leave', index: 3, kind: 'ajax',
				eventClientHandlerName: 'Wicket.ClientEvents.sendState' }], null, { ajaxUrl: 'page?1-1.0-editor',
				values: () => ({ state: '{}' }) });

			element.dispatchEvent(new CustomEvent('leave', { detail: { leaving: true } }));
			// without the URL of its kind, it goes via Ajax
			attachClientEvents('editor', [{ eventName: 'leave', index: 0, kind: 'submit',
				eventClientHandlerName: 'Wicket.ClientEvents.send' }], null, { ajaxUrl: 'page?1-1.0-editor' });
			element.dispatchEvent(new CustomEvent('leave', { detail: { leaving: true } }));
		} finally {
			delete (navigator as unknown as Record<string, unknown>).sendBeacon;
		}
		expect(beacons).toEqual([['page?1-1.0-editor&wicket-ajax=true', 'state=%7B%7D&' +
			'antilia-event=leave&antilia-event-index=3&antilia-event-values=%7B%22leaving%22%3Atrue%7D&' +
			'text=Typed&wicket-ajax-baseurl=']]);
		expect(heard.map((request) => [request.type, request.event])).toEqual([
			[CLIENT_EVENTS_SUBMIT, 'leave']]);
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
