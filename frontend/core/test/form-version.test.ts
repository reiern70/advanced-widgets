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

import { ATTRIBUTE, PARAMETER_PREFIX, set, stamp, stampSubmit, unwatch, watch,
	WicketFormVersion } from '../src/wicket/form-version.js';
import type { AjaxAttributes } from '../src/wicket/send-state.js';

type Subscriber = (event: unknown, attrs: AjaxAttributes) => void;

/** The subscribers of Wicket.Event, by topic, while a test fakes it. */
let topics: Map<string, Subscriber[]>;

/** Publishes a topic of Wicket.Event to the subscribers, as Wicket's Ajax does. */
function publish(topic: string, attrs: AjaxAttributes): void {
	(topics.get(topic) ?? []).forEach((subscriber) => subscriber({}, attrs));
}

/** @returns the parameters a request carries, by their names */
function parameters(attrs: AjaxAttributes): Record<string, string> {
	return Object.fromEntries((attrs.ep as Array<{ name: string; value: string }>)
		.map(({ name, value }) => [name, value]));
}

/** A submit event as the browser fires it, from a submitter, which a handler may prevent. */
function submit(form: HTMLFormElement, submitter: HTMLElement | null, prevent = false): Event {
	const event = new Event('submit', { bubbles: true, cancelable: true });
	Object.defineProperty(event, 'submitter', { value: submitter });
	if (prevent) {
		form.addEventListener('submit', (fired) => fired.preventDefault(), { once: true });
	}
	form.dispatchEvent(event);
	return event;
}

describe('form version', () => {
	beforeEach(() => {
		topics = new Map();
		(window.Wicket as Record<string, unknown>).Event = {
			subscribe(topic: string, subscriber: Subscriber) {
				topics.set(topic, [...(topics.get(topic) ?? []), subscriber]);
			},
			unsubscribe(topic: string, subscriber: Subscriber) {
				topics.set(topic, (topics.get(topic) ?? []).filter((each) => each !== subscriber));
			},
			Topic: { AJAX_CALL_INIT: '/ajax/call/init' }
		};
		document.body.innerHTML =
			'<form id="order" ' + ATTRIBUTE + '="3">' +
			'  <input name="amount" value="10">' +
			'  <button id="save" name="save">Save</button>' +
			'  <div id="line" ' + ATTRIBUTE + '="7">' +
			'    <button id="add" name="add">Add</button>' +
			'  </div>' +
			'</form>' +
			'<form id="search"><button id="find">Find</button></form>';
	});

	afterEach(() => {
		unwatch();
		delete (window.Wicket as Record<string, unknown>).Event;
		document.body.innerHTML = '';
	});

	it('registers Wicket.FormVersion', () => {
		expect(window.Wicket?.FormVersion).toBe(WicketFormVersion);
		expect(WicketFormVersion.attribute).toBe('data-antilia-form-version');
		expect(WicketFormVersion.parameterPrefix).toBe('antilia-form-version:');
	});

	it('makes an Ajax submit carry the version the form has at the click', () => {
		watch();
		const attrs: AjaxAttributes = { u: 'page?1-1.0-order-save', f: 'order', c: 'save',
			ep: [{ name: 'a', value: '1' }] };
		publish('/ajax/call/init', attrs);
		// the answer of an earlier submit raises the version before this request leaves
		set('order', 4);

		expect(parameters(attrs)).toEqual({ a: '1', [PARAMETER_PREFIX + 'order']: '3' });
		expect(document.getElementById('order')?.getAttribute(ATTRIBUTE)).toBe('4');
	});

	it('leaves the extra parameters of the other requests alone', () => {
		// Wicket's Ajax copies the attributes for each request, but not their extra parameters
		const shared = [{ name: 'a', value: '1' }];
		const attrs: AjaxAttributes = { f: 'order', c: 'save', ep: shared };

		expect(stamp(attrs)).toBe(true);

		expect(shared).toEqual([{ name: 'a', value: '1' }]);
		expect(attrs.ep).not.toBe(shared);
	});

	it('takes the extra parameters given as values by their names', () => {
		const attrs: AjaxAttributes = { f: 'order', c: 'save', ep: { a: '1' } };

		stamp(attrs);

		expect(parameters(attrs)).toEqual({ a: '1', [PARAMETER_PREFIX + 'order']: '3' });
	});

	it('sends the version of the nearest form, a nested one', () => {
		const attrs: AjaxAttributes = { f: 'order', c: 'add' };

		stamp(attrs);

		expect(parameters(attrs)).toEqual({ [PARAMETER_PREFIX + 'line']: '7' });
	});

	it('sends the version of the submitted form when the component is not in it', () => {
		const attrs: AjaxAttributes = { f: 'order', c: 'elsewhere' };

		stamp(attrs);

		expect(parameters(attrs)).toEqual({ [PARAMETER_PREFIX + 'order']: '3' });
	});

	it('leaves alone the requests submitting no form, and the forms without a version', () => {
		const update: AjaxAttributes = { c: 'save', ep: [] };
		const search: AjaxAttributes = { f: 'search', c: 'find' };

		expect(stamp(update)).toBe(false);
		expect(stamp(search)).toBe(false);
		expect(stamp(null)).toBe(false);

		expect(update.ep).toEqual([]);
		expect(search.ep).toBeUndefined();
	});

	it('makes a normal submit carry the version, in a hidden input of the form', () => {
		watch();
		const form = document.getElementById('order') as HTMLFormElement;

		submit(form, document.getElementById('save'));
		submit(form, document.getElementById('save'));

		const inputs = form.querySelectorAll<HTMLInputElement>('input[type=hidden]');
		expect(inputs).toHaveLength(1);
		expect(inputs[0].name).toBe(PARAMETER_PREFIX + 'order');
		expect(inputs[0].value).toBe('3');
	});

	it('writes the version of a nested form its submitter is in', () => {
		const form = document.getElementById('order') as HTMLFormElement;

		expect(stampSubmit(submit(form, document.getElementById('add')))).toBe(true);

		expect(form.querySelector<HTMLInputElement>('input[type=hidden]')?.name)
			.toBe(PARAMETER_PREFIX + 'line');
	});

	it('leaves alone a submit an Ajax handler took', () => {
		watch();
		const form = document.getElementById('order') as HTMLFormElement;

		submit(form, document.getElementById('save'), true);

		expect(form.querySelector('input[type=hidden]')).toBeNull();
	});

	it('drops what a normal submit left when an Ajax submit goes', () => {
		const form = document.getElementById('order') as HTMLFormElement;
		submit(form, null);
		set('order', 5);
		const attrs: AjaxAttributes = { f: 'order', c: 'save' };

		stamp(attrs);

		expect(form.querySelector('input[type=hidden]')).toBeNull();
		expect(parameters(attrs)).toEqual({ [PARAMETER_PREFIX + 'order']: '5' });
	});

	it('watches once, and stops', () => {
		watch();
		watch();
		expect(topics.get('/ajax/call/init')).toHaveLength(1);

		unwatch();
		expect(topics.get('/ajax/call/init')).toHaveLength(0);
		const form = document.getElementById('order') as HTMLFormElement;
		submit(form, document.getElementById('save'));
		expect(form.querySelector('input[type=hidden]')).toBeNull();
	});
});
