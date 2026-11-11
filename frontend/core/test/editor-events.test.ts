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
import { afterEach, describe, expect, it, vi } from 'vitest';

import { DEFAULT_STATE_EVENTS, EditorEventHandlers, NO_STATE_EVENTS, sendEditorState, setHost,
	standaloneHost, STATE_DELAY } from '../src/index.js';

/** The names of the events of an editor, by their keys. */
const NAMES = { change: 'test:change', select: 'test:select', blur: 'test:blur',
	leave: 'test:leave', save: 'test:save' };

/** An event of an editor, as the editors fire them. */
function event(type: string, detail: Record<string, unknown> = {}): CustomEvent {
	return new CustomEvent(type, { detail });
}

describe('the handlers of the events of the editors', () => {
	afterEach(() => {
		vi.useRealTimers();
		setHost();
	});

	it('send the state by default: a moment after the changes, at once on blur and leave', () => {
		vi.useFakeTimers();
		const sent: boolean[] = [];
		const handlers = new EditorEventHandlers({}, NAMES, undefined,
			(leaving) => sent.push(leaving));

		handlers.handle(event('test:change'));
		handlers.handle(event('test:select'));
		vi.advanceTimersByTime(STATE_DELAY - 1);
		expect(sent).toEqual([]);
		vi.advanceTimersByTime(1);
		// each its own timer
		expect(sent).toEqual([false, false]);

		handlers.handle(event('test:blur'));
		handlers.handle(event('test:leave', { leaving: true }));
		expect(sent).toEqual([false, false, false, true]);
		// none on the events without handlers
		handlers.handle(event('test:save'));
		expect(sent).toHaveLength(4);
		expect(Object.keys(DEFAULT_STATE_EVENTS)).toEqual(Object.keys(NO_STATE_EVENTS));
	});

	it('take the handlers of the page, keeping the defaults of the events left out or undefined', () => {
		vi.useFakeTimers();
		const sent: boolean[] = [];
		const seen: string[] = [];
		const editor = { name: 'the editor' };
		const handlers = new EditorEventHandlers(editor, NAMES, {
			change: undefined,
			select: [],
			save: [(fired, given, send) => {
				seen.push(fired.type + ' ' + (given === editor));
				send();
			}],
			'other:event': [{ delay: 50, handler: (fired) => seen.push(fired.type) }]
		}, (leaving) => sent.push(leaving));

		handlers.handle(event('test:select'));
		handlers.handle(event('test:save'));
		expect(seen).toEqual(['test:save true']);
		expect(sent).toEqual([false]);
		handlers.handle(event('test:change'));
		vi.advanceTimersByTime(STATE_DELAY);
		expect(sent).toEqual([false, false]);

		// by any other name, each event postponing the handler waiting
		handlers.handle(event('other:event'));
		vi.advanceTimersByTime(40);
		handlers.handle(event('other:event'));
		vi.advanceTimersByTime(40);
		expect(seen).toEqual(['test:save true']);
		vi.advanceTimersByTime(10);
		expect(seen).toEqual(['test:save true', 'other:event']);
	});

	it('drop the handlers waiting once destroyed, and log one failing', () => {
		vi.useFakeTimers();
		const errors: unknown[] = [];
		setHost({ ...standaloneHost, log: { error: (message) => errors.push(message) } });
		const sent = vi.fn();
		const handlers = new EditorEventHandlers({}, NAMES, { blur: [() => {
			throw new Error('broken');
		}, sendEditorState] }, sent);

		handlers.handle(event('test:blur'));
		expect(errors).toEqual(['The handler of test:blur failed']);
		expect(sent).toHaveBeenCalledOnce();

		handlers.handle(event('test:change'));
		handlers.destroy();
		vi.advanceTimersByTime(STATE_DELAY);
		handlers.handle(event('test:blur'));
		expect(sent).toHaveBeenCalledOnce();
	});
});
