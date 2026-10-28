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

import { setHost, standaloneHost, type Host } from '../src/index.js';
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

	it('sends the data of the detail of an Ajax event without a function', () => {
		const heard = requests(element);
		attachClientEvents('editor', [{ id: 'antilia:resize', index: 2, kind: 'ajax' }]);

		element.dispatchEvent(new CustomEvent('antilia:resize', { detail: {
			editor: new (class Editor {})(), size: { width: 640, height: 320 } } }));
		expect(heard).toEqual([{ type: CLIENT_EVENTS_AJAX, event: 'antilia:resize', index: 2,
			values: '{"size":{"width":640,"height":320}}' }]);
	});

	it('runs the function, which sends what it chooses, submitting the form', () => {
		const heard = requests(element);
		const component = { name: 'the editor' };
		const seen: unknown[] = [];
		attachClientEvents('editor', [{ id: 'input', index: 0, kind: 'submit',
			fn(event, editor, send) {
				seen.push(event.type, editor, this);
				send({ length: 3 });
			} }], () => component);

		element.dispatchEvent(new Event('input'));
		expect(seen).toEqual(['input', component, element]);
		expect(heard).toEqual([{ type: CLIENT_EVENTS_SUBMIT, event: 'input', index: 0,
			values: '{"length":3}' }]);
	});

	it('keeps a client event in the browser', () => {
		const heard = requests(element);
		const send = vi.fn();
		attachClientEvents('editor', [{ id: 'antilia:resize', index: 0, kind: 'client',
			fn(event, _component, sender) {
				document.cookie = 'size=' + JSON.stringify((event as CustomEvent).detail.size);
				send();
				sender({ never: true });
			} }]);

		element.dispatchEvent(new CustomEvent('antilia:resize', { detail: { size: { width: 5 } } }));
		expect(document.cookie).toContain('size={"width":5}');
		expect(send).toHaveBeenCalledOnce();
		expect(heard).toEqual([]);
		// a client event without a function does nothing
		attachClientEvents('editor', [{ id: 'focus', index: 0, kind: 'client' }]);
		element.dispatchEvent(new Event('focus'));
		expect(heard).toEqual([]);
	});

	it('runs each event of one name: one in the browser, one going to the server', () => {
		const heard = requests(element);
		attachClientEvents('editor', [
			{ id: 'antilia:resize', index: 0, kind: 'client', fn(event) {
				document.cookie = 'kept=' + (event as CustomEvent).detail.size.height;
			} },
			{ id: 'antilia:resize', index: 1, kind: 'ajax' }]);

		element.dispatchEvent(new CustomEvent('antilia:resize', { detail: { size: { height: 300 } } }));
		expect(document.cookie).toContain('kept=300');
		expect(heard).toEqual([{ type: CLIENT_EVENTS_AJAX, event: 'antilia:resize', index: 1,
			values: '{"size":{"height":300}}' }]);
	});

	it('waits for the events to pause, each postponing the one waiting', () => {
		vi.useFakeTimers();
		const heard = requests(element);
		attachClientEvents('editor', [{ id: 'input', index: 0, kind: 'ajax', delay: 500,
			fn: (event, _component, send) => send((event as CustomEvent).detail) }]);

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
		attachClientEvents('editor', [{ id: 'focus', index: 0, kind: 'ajax' }]);
		attachClientEvents('editor', [{ id: 'blur', index: 0, kind: 'ajax' }]);

		element.dispatchEvent(new Event('focus'));
		element.dispatchEvent(new Event('blur'));
		expect(heard.map((request) => request.event)).toEqual(['blur']);

		(window.Wicket?.ClientEvents as { detach(id: string): void }).detach('editor');
		element.dispatchEvent(new Event('blur'));
		expect(heard).toHaveLength(1);
		expect(attachClientEvents('missing', [])).toBeNull();
	});

	it('logs a function failing, or values that are not JSON, and goes on', () => {
		const errors: unknown[] = [];
		const host: Host = { ...standaloneHost, log: { error: (message) => errors.push(message) } };
		setHost(host);
		const heard = requests(element);
		const circular: Record<string, unknown> = {};
		circular.self = circular;
		attachClientEvents('editor', [
			{ id: 'focus', index: 0, kind: 'ajax', fn: () => { throw new Error('broken'); } },
			{ id: 'focus', index: 1, kind: 'ajax', fn: (_event, _component, send) => send(circular) },
			{ id: 'focus', index: 2, kind: 'ajax' }]);

		element.dispatchEvent(new Event('focus'));
		expect(errors).toEqual(['The function of focus failed', 'The values of focus are not JSON']);
		expect(heard.map((request) => [request.index, request.values])).toEqual([[2, '{}']]);
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
