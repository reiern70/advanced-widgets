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

import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';

import { RICH_TEXT_SUGGESTION_CLASSES, RichTextEditor, setHost, standaloneHost, suggestAt,
	suggestionItems, type Host, type RichTextSuggester,
	type RichTextSuggestion } from '../src/index.js';

beforeAll(() => {
	// jsdom lays nothing out: ProseMirror measures nothing
	const rect = { left: 0, top: 0, right: 0, bottom: 0, width: 0, height: 0, x: 0, y: 0,
		toJSON: () => ({}) } as DOMRect;
	Range.prototype.getClientRects = () => [] as unknown as DOMRectList;
	Range.prototype.getBoundingClientRect = () => rect;
	document.elementFromPoint = () => null;
});

const USERS: readonly RichTextSuggestion[] = [
	{ id: '1', label: 'Ann', detail: 'ann@example.org' },
	{ id: '2', label: 'Andrew' },
	{ id: '3', label: 'Bob' }
];

/** A suggester of users, keeping what it was asked. */
function users(asked: string[] = [], signals: AbortSignal[] = []): RichTextSuggester {
	return {
		name: 'users',
		char: '@',
		items: async (query, signal) => {
			asked.push(query);
			signals.push(signal);
			return USERS.filter((user) => user.label.toLowerCase().startsWith(query.toLowerCase()));
		}
	};
}

const WORDS = ['example', 'excellent', 'editor'];

/** A suggester completing words, without a character. */
const words: RichTextSuggester = {
	name: 'words',
	items: async (query) => WORDS.filter((word) => word.startsWith(query))
		.map((word) => ({ id: word, label: word }))
};

function textArea(html: string): HTMLTextAreaElement {
	document.body.innerHTML = '<form><label for="notes">Notes</label><textarea id="notes" ' +
		'name="notes"></textarea><input id="other"></form>';
	const field = document.getElementById('notes') as HTMLTextAreaElement;
	field.defaultValue = html;
	return field;
}

/** Types at the end of the text, as the user does. */
function type(editor: RichTextEditor, text: string): void {
	const tiptap = editor.editor;
	tiptap.commands.setTextSelection(tiptap.state.doc.content.size - 1);
	for (const char of text) {
		tiptap.view.dispatch(tiptap.state.tr.insertText(char));
	}
}

function key(editor: RichTextEditor, key: string, init: KeyboardEventInit = {}): KeyboardEvent {
	const event = new KeyboardEvent('keydown', { key, bubbles: true, cancelable: true, ...init });
	editor.editor.view.dom.dispatchEvent(event);
	return event;
}

function list(editor: RichTextEditor): HTMLElement | null {
	return editor.element.querySelector<HTMLElement>('.' + RICH_TEXT_SUGGESTION_CLASSES.list);
}

function options(editor: RichTextEditor): string[] {
	return Array.from(list(editor)?.querySelectorAll('[role="option"]') ?? [])
		.map((option) => option.textContent ?? '');
}

/** Lets the delay go by, and the suggesters answer. */
const asked = (ms = 200) => vi.advanceTimersByTimeAsync(ms);

beforeEach(() => {
	vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] });
});

afterEach(() => {
	document.body.innerHTML = '';
	setHost(standaloneHost);
	vi.useRealTimers();
	vi.unstubAllGlobals();
	vi.restoreAllMocks();
});

describe('Suggestions', () => {
	it('lists the users after @, as the user types, and mentions the one chosen', async () => {
		const queries: string[] = [];
		const editor = new RichTextEditor(textArea('<p>Hi</p>'), { suggesters: [users(queries)] });
		type(editor, ' @');
		const text = editor.editor.view.dom;
		expect(list(editor)?.textContent).toBe('Searching…');
		await asked();
		expect(queries).toEqual(['']);
		expect(options(editor)).toEqual(['Ann ann@example.org', 'Andrew', 'Bob']);
		expect(list(editor)?.getAttribute('role')).toBe('listbox');
		expect(list(editor)?.getAttribute('aria-label')).toBe('Suggestions');
		expect(text.getAttribute('aria-expanded')).toBe('true');
		expect(text.getAttribute('aria-controls')).toBe(list(editor)!.id);
		expect(text.getAttribute('aria-autocomplete')).toBe('list');
		expect(text.getAttribute('aria-activedescendant')).toBe(list(editor)!.id + '-0');

		type(editor, 'An');
		await asked();
		expect(queries).toEqual(['', 'An']);
		expect(options(editor)).toEqual(['Ann ann@example.org', 'Andrew']);

		key(editor, 'ArrowDown');
		await asked(0);
		expect(text.getAttribute('aria-activedescendant')).toBe(list(editor)!.id + '-1');
		expect(list(editor)!.querySelector('[aria-selected="true"]')?.textContent).toBe('Andrew');
		// round and back
		key(editor, 'ArrowDown');
		key(editor, 'ArrowUp');
		expect(key(editor, 'Enter').defaultPrevented).toBe(true);
		expect(editor.getValue()).toBe('<p>Hi <span class="antilia-mention" data-mention="users" ' +
			'data-id="2">@Andrew</span> </p>');
		expect(list(editor)).toBeNull();
		await asked(0);
		expect(text.hasAttribute('aria-expanded')).toBe(false);
		expect(text.hasAttribute('aria-activedescendant')).toBe(false);

		// a mention goes at once
		editor.editor.commands.setTextSelection(editor.editor.state.doc.content.size - 2);
		key(editor, 'Backspace');
		editor.editor.commands.deleteRange({ from: 4, to: 5 });
		expect(editor.getValue()).toBe('<p>Hi  </p>');
	});

	it('reads the mentions of the text, and only those of a kind and an id', () => {
		const editor = new RichTextEditor(textArea('<p><span class="antilia-mention" ' +
			'data-mention="users" data-id="1">@Ann</span> and <span data-mention="bad kind" ' +
			'data-id="2">@Bob</span></p>'));
		expect(editor.getValue()).toBe('<p><span class="antilia-mention" data-mention="users" ' +
			'data-id="1">@Ann</span> and @Bob</p>');
		expect(editor.getText()).toBe('@Ann and @Bob');
	});

	it('opens on a character starting a word only, and not again once closed', async () => {
		const queries: string[] = [];
		const editor = new RichTextEditor(textArea('<p>mail</p>'), { suggesters: [users(queries)] });
		type(editor, '@ex');
		await asked();
		expect(list(editor)).toBeNull();
		expect(queries).toEqual([]);

		type(editor, ' (@');
		await asked();
		expect(options(editor)).toHaveLength(3);
		key(editor, 'Escape');
		expect(list(editor)).toBeNull();
		type(editor, 'A');
		await asked();
		expect(list(editor)).toBeNull();

		// a blank ends the query; a new one opens again
		type(editor, ' @');
		await asked();
		expect(options(editor)).toHaveLength(3);
		type(editor, ' ');
		expect(list(editor)).toBeNull();
	});

	it('completes the word before the cursor on Ctrl+Space, as the user goes on typing', async () => {
		const editor = new RichTextEditor(textArea('<p>An e</p>'), { suggesters: [users(), words] });
		type(editor, '');
		expect(key(editor, ' ', { ctrlKey: true }).defaultPrevented).toBe(true);
		await asked();
		expect(options(editor)).toEqual(['example', 'excellent', 'editor']);

		type(editor, 'x');
		await asked();
		expect(options(editor)).toEqual(['example', 'excellent']);
		key(editor, 'ArrowDown');
		key(editor, 'Tab');
		expect(editor.getValue()).toBe('<p>An excellent </p>');
		expect(list(editor)).toBeNull();

		// another word, with the next letter, does not open it
		type(editor, 'e');
		await asked();
		expect(list(editor)).toBeNull();
	});

	it('inserts the text of a mention where the text has none', async () => {
		const editor = new RichTextEditor(textArea('Hi'), { format: 'markdown',
			suggesters: [users()] });
		type(editor, ' @B');
		await asked();
		list(editor)!.querySelector<HTMLElement>('[role="option"]')!.click();
		expect(editor.getValue()).toBe('Hi @Bob ');
		expect(editor.editor.state.doc.firstChild!.childCount).toBe(1);
	});

	it('says when nothing is found, and tells of a suggester failing', async () => {
		const log = vi.fn();
		const host: Host = { ...standaloneHost, log: { ...standaloneHost.log, error: log } };
		setHost(host);
		const failing: RichTextSuggester = { name: 'tags', char: '@', items: () => {
			throw new Error('down');
		} };
		const editor = new RichTextEditor(textArea('<p>Hi</p>'), { suggesters: [failing,
			users()] });
		type(editor, ' @Z');
		await asked();
		expect(list(editor)?.textContent).toBe('Nothing found');
		expect(log).toHaveBeenCalledWith('A suggester of the rich text editor failed',
			new Error('down'));
		// Enter is the text's again
		key(editor, 'Enter');
		expect(editor.editor.state.doc.childCount).toBe(2);
		expect(list(editor)).toBeNull();
	});

	it('cancels a request once the query changes, and offers nothing read only', async () => {
		const queries: string[] = [];
		const signals: AbortSignal[] = [];
		const slow: RichTextSuggester = { name: 'users', char: '@', items: (query, signal) => {
			queries.push(query);
			signals.push(signal);
			return new Promise((resolve) => setTimeout(() => resolve(USERS), 1000));
		} };
		const editor = new RichTextEditor(textArea('<p>Hi</p>'), { suggesters: [slow] });
		type(editor, ' @');
		await asked();
		type(editor, 'A');
		expect(signals[0]?.aborted).toBe(true);
		await asked(1200);
		expect(queries).toEqual(['', 'A']);
		expect(options(editor)).toHaveLength(3);
		key(editor, 'Escape');

		editor.configure({ readOnly: true });
		type(editor, ' @');
		await asked();
		expect(list(editor)).toBeNull();
		expect(key(editor, ' ', { ctrlKey: true }).defaultPrevented).toBe(false);
	});

	it('closes as the text loses the focus, or the editor goes', async () => {
		const editor = new RichTextEditor(textArea('<p>Hi</p>'), { suggesters: [users()] });
		type(editor, ' @');
		await asked();
		const shown = list(editor)!;
		const down = new MouseEvent('mousedown', { bubbles: true, cancelable: true });
		shown.dispatchEvent(down);
		expect(down.defaultPrevented).toBe(true);
		editor.editor.view.dom.dispatchEvent(new FocusEvent('blur'));
		expect(list(editor)).toBeNull();

		type(editor, ' @');
		await asked();
		expect(list(editor)).not.toBeNull();
		editor.destroy();
		expect(document.querySelector('.' + RICH_TEXT_SUGGESTION_CLASSES.list)).toBeNull();
	});
});

describe('suggestionItems', () => {
	it('takes items with an id and a label, ten at most', () => {
		expect(suggestionItems([{ id: 1, label: ' Ann ', detail: ' x ', text: 'ann' },
			{ id: 'b' }, { label: 'no id' }, null, { id: {}, label: 'c' }])).toEqual([
			{ id: '1', label: 'Ann', detail: 'x', text: 'ann' }]);
		expect(suggestionItems(Array.from({ length: 12 }, (_, index) => ({ id: index,
			label: 'n' + index })))).toHaveLength(10);
		expect(suggestionItems({})).toEqual([]);
	});
});

describe('suggestAt', () => {
	it('asks the server for the items of a suggester by its name', async () => {
		const fetch = vi.fn(async (_url: string | URL | Request, _init?: RequestInit) =>
			new Response(JSON.stringify({ items: [{ id: '1', label: 'Ann' }] }), { status: 200 }));
		vi.stubGlobal('fetch', fetch);
		const suggester = suggestAt('/suggest', { name: 'users', char: '@' });
		expect(suggester.char).toBe('@');

		expect(await suggester.items('A', new AbortController().signal)).toEqual([
			{ id: '1', label: 'Ann' }]);
		const [url, init] = fetch.mock.calls[0]!;
		expect(url).toBe('/suggest');
		const body = init?.body as URLSearchParams;
		expect(body.get('name')).toBe('users');
		expect(body.get('query')).toBe('A');
	});

	it('fails as the server does', async () => {
		vi.stubGlobal('fetch', vi.fn(async () => new Response(JSON.stringify({ error: 'No' }),
			{ status: 200 })));
		const suggester = suggestAt('/suggest', { name: 'users' });
		await expect(suggester.items('', new AbortController().signal)).rejects.toThrow('No');

		vi.stubGlobal('fetch', vi.fn(async () => new Response('down', { status: 500 })));
		await expect(suggester.items('', new AbortController().signal)).rejects
			.toThrow('The server answered 500');
	});
});
