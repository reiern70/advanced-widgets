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

import { TextSelection } from '@tiptap/pm/state';

import { issuesOf, languageToolKind, languageToolProofreader, Popover, proofreadAt, proofreadBlocks,
	proofreadIssues, RICH_TEXT_ISSUE_CLASSES, RichTextEditor, setHost, standaloneHost,
	type Host, type RichTextIssue, type RichTextProofreader } from '../src/index.js';

beforeAll(() => {
	// jsdom lays nothing out: ProseMirror measures nothing
	const rect = { left: 0, top: 0, right: 0, bottom: 0, width: 0, height: 0, x: 0, y: 0,
		toJSON: () => ({}) } as DOMRect;
	Range.prototype.getClientRects = () => [] as unknown as DOMRectList;
	Range.prototype.getBoundingClientRect = () => rect;
	document.elementFromPoint = () => null;
});

/**
 * A proofreader of the tests: each word of the list is spelled wrong, its replacement the word
 * fixed; "they is" is a mistake of grammar. It keeps the texts it was asked about.
 */
class Words implements RichTextProofreader {
	readonly asked: { texts: readonly string[]; language: string | null }[] = [];

	readonly signals: AbortSignal[] = [];

	constructor(private readonly words: Readonly<Record<string, string>> = { wrold: 'world',
		teh: 'the' }) {
	}

	async check(texts: readonly string[], language: string | null,
		signal: AbortSignal): Promise<readonly (readonly RichTextIssue[])[]> {
		this.asked.push({ texts, language });
		this.signals.push(signal);
		return texts.map((text) => {
			const issues: RichTextIssue[] = [];
			for (const match of text.matchAll(/\p{L}+/gu)) {
				const fixed = this.words[match[0]];
				if (fixed !== undefined) {
					issues.push({ from: match.index, to: match.index + match[0].length,
						kind: 'spelling', replacements: [fixed, fixed + 's'], rule: 'SPELLING',
						message: 'Possible spelling mistake' });
				}
			}
			const grammar = text.indexOf('they is');
			if (grammar >= 0) {
				issues.push({ from: grammar, to: grammar + 7, kind: 'grammar', rule: 'AGREEMENT',
					message: 'The verb does not agree with its subject', replacements: ['they are'] });
			}
			return issues;
		});
	}
}

function textArea(html: string): HTMLTextAreaElement {
	document.body.innerHTML = '<form><label for="notes">Notes</label><textarea id="notes" ' +
		'name="notes"></textarea><input id="other"></form>';
	const field = document.getElementById('notes') as HTMLTextAreaElement;
	field.defaultValue = html;
	return field;
}

/** The words underlined, with their classes. */
function underlined(editor: RichTextEditor): string[] {
	return Array.from(editor.editor.view.dom.querySelectorAll('.' + RICH_TEXT_ISSUE_CLASSES.issue))
		.map((element) => element.textContent + ' ' + element.className.replace(
			RICH_TEXT_ISSUE_CLASSES.issue + ' ', ''));
}

/** Lets the delay go by, and the proofreader answer. */
const checked = (ms = 700) => vi.advanceTimersByTimeAsync(ms);

function type(editor: RichTextEditor, text: string, at?: number): void {
	const state = editor.editor.state;
	const tr = at === undefined ? state.tr.insertText(text) : state.tr.insertText(text, at);
	editor.editor.view.dispatch(tr);
}

/** Presses Mod-., as the user does. */
function next(editor: RichTextEditor): void {
	editor.editor.view.dom.dispatchEvent(new KeyboardEvent('keydown', { key: '.', ctrlKey: true,
		bubbles: true, cancelable: true }));
}

beforeEach(() => {
	vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] });
});

afterEach(() => {
	Popover.closeAll();
	document.body.innerHTML = '';
	setHost(standaloneHost);
	vi.useRealTimers();
	vi.unstubAllGlobals();
	vi.restoreAllMocks();
});

describe('proofreadBlocks', () => {
	it('takes the text of each block, but code blocks, places of the editor kept', () => {
		const editor = new RichTextEditor(textArea('<h2>Title</h2><p>One <code>x</code> two<br>' +
			'three</p><ul><li><p>Item</p></li></ul><pre><code>code teh</code></pre>' +
			'<table><tbody><tr><td><p>Cell</p></td></tr></tbody></table>'));
		const doc = editor.editor.state.doc;
		const blocks = proofreadBlocks(doc);

		expect(blocks.map((block) => block.text)).toEqual(['Title', 'One x two\nthree', 'Item',
			'Cell']);
		blocks.forEach((block) => expect(doc.textBetween(block.start,
			block.start + block.text.length, undefined, '\n')).toBe(block.text));
		// the inline code and the line break
		expect(blocks[1]!.skipped).toEqual([[4, 5], [9, 10]]);
	});
});

describe('proofreadIssues', () => {
	it('takes the issues within the text, of a kind known, in their order', () => {
		expect(proofreadIssues([
			{ from: 4, to: 6, kind: 'style', message: ' Long ', replacements: ['a', 7, 'a', 'b'],
				rule: 'R' },
			{ from: 0, to: 2, kind: 'other' },
			{ from: 3, to: 3, kind: 'spelling' },
			{ from: -1, to: 2, kind: 'spelling' },
			{ from: 5, to: 99, kind: 'spelling' },
			{ from: 1.5, to: 2, kind: 'spelling' },
			null, 'no'
		], 10)).toEqual([
			{ from: 0, to: 2, kind: 'grammar' },
			{ from: 4, to: 6, kind: 'style', message: 'Long', replacements: ['a', 'b'], rule: 'R' }
		]);
		expect(proofreadIssues(undefined, 10)).toEqual([]);
	});
});

describe('Proofreading', () => {
	it('underlines what the proofreader finds once the user stopped typing, in the language of ' +
		'the text', async () => {
		const words = new Words();
		const editor = new RichTextEditor(textArea('<p>Hello wrold</p><p>Fine</p><p>they is</p>'),
			{ proofreader: words, textLanguage: 'en-GB' });
		expect(underlined(editor)).toEqual([]);

		await checked();
		expect(words.asked).toEqual([{ texts: ['Hello wrold', 'Fine', 'they is'],
			language: 'en-GB' }]);
		expect(underlined(editor)).toEqual(['wrold antilia-rich-text-issue-spelling',
			'they is antilia-rich-text-issue-grammar']);
		const word = editor.editor.view.dom.querySelector('.antilia-rich-text-issue-spelling')!;
		expect(word.getAttribute('aria-invalid')).toBe('spelling');
		// never in the text
		expect(editor.getValue()).toBe('<p>Hello wrold</p><p>Fine</p><p>they is</p>');
		// two underlines else
		expect(editor.editor.view.dom.getAttribute('spellcheck')).toBe('false');
	});

	it('checks only the blocks changed, keeping the issues whose words stayed', async () => {
		const words = new Words();
		const editor = new RichTextEditor(textArea('<p>Hello wrold teh</p><p>Fine</p>'),
			{ proofreader: words });
		await checked();
		expect(underlined(editor)).toHaveLength(2);

		// in the second word: until checked again, the first keeps its line
		type(editor, 'x', 15);
		expect(editor.editor.state.doc.firstChild!.textContent).toBe('Hello wrold texh');
		expect(underlined(editor)).toEqual(['wrold antilia-rich-text-issue-spelling']);
		await checked(100);
		expect(words.asked).toHaveLength(1);

		await checked();
		expect(words.asked[1]!.texts).toEqual(['Hello wrold texh']);
		expect(underlined(editor)).toEqual(['wrold antilia-rich-text-issue-spelling']);

		// a text checked already is not asked again
		editor.editor.commands.undo();
		await checked();
		expect(words.asked).toHaveLength(2);
		expect(underlined(editor)).toHaveLength(2);
	});

	it('asks in the language of the page without one of its own, and anew in another', async () => {
		document.documentElement.lang = 'de';
		try {
			const words = new Words();
			const editor = new RichTextEditor(textArea('<p>teh</p>'), { proofreader: words });
			await checked();
			expect(words.asked[0]!.language).toBe('de');

			editor.configure({ textLanguage: 'es' });
			await checked(0);
			expect(words.asked[1]).toEqual({ texts: ['teh'], language: 'es' });
		} finally {
			document.documentElement.removeAttribute('lang');
		}
	});

	it('replaces an issue by a replacement chosen in its popover', async () => {
		const editor = new RichTextEditor(textArea('<p>Hello <strong>wrold</strong></p>'),
			{ proofreader: new Words() });
		await checked();
		const word = editor.editor.view.dom.querySelector<HTMLElement>(
			'.' + RICH_TEXT_ISSUE_CLASSES.issue)!;
		editor.editor.view.someProp('handleClick', (handle) => handle(editor.editor.view, 8,
			new MouseEvent('click', { bubbles: true })));
		// ProseMirror passes the event of the click, on the word
		await checked(0);
		expect(Popover.current()).toBeNull();

		const click = new MouseEvent('click', { bubbles: true });
		Object.defineProperty(click, 'target', { value: word });
		editor.editor.view.someProp('handleClick', (handle) => handle(editor.editor.view, 8, click));
		await checked(0);
		const popover = Popover.current()!;
		expect(popover.element.classList.contains(RICH_TEXT_ISSUE_CLASSES.popover)).toBe(true);
		expect(popover.element.querySelector('[id$="-title"]')?.textContent).toBe('Spelling');
		expect(popover.element.querySelector('.antilia-rich-text-issue-message')?.textContent)
			.toBe('Possible spelling mistake');
		const buttons = Array.from(popover.element.querySelectorAll<HTMLButtonElement>(
			'.antilia-rich-text-issue-replacements button'));
		expect(buttons.map((button) => button.textContent)).toEqual(['world', 'worlds']);
		expect(document.activeElement).toBe(buttons[0]);
		expect(popover.trigger.classList.contains(RICH_TEXT_ISSUE_CLASSES.anchor)).toBe(true);
		expect(editor.editor.view.dom.contains(popover.trigger)).toBe(false);

		buttons[0]!.click();
		expect(popover.isOpen()).toBe(false);
		expect(editor.getValue()).toBe('<p>Hello <strong>world</strong></p>');
		await checked();
		expect(underlined(editor)).toEqual([]);
	});

	it('ignores an issue, and those of its rule and its words', async () => {
		const words = new Words();
		const editor = new RichTextEditor(textArea('<p>teh one</p><p>teh two</p><p>wrold</p>'),
			{ proofreader: words });
		await checked();
		expect(underlined(editor)).toHaveLength(3);

		editor.editor.commands.setTextSelection(1);
		next(editor);
		const popover = Popover.current()!;
		const ignore = Array.from(popover.element.querySelectorAll('button'))
			.find((button) => button.textContent === 'Ignore')!;
		ignore.click();
		expect(underlined(editor)).toEqual(['wrold antilia-rich-text-issue-spelling']);
		await checked();
		expect(underlined(editor)).toEqual(['wrold antilia-rich-text-issue-spelling']);
	});

	it('goes to the next issue from the cursor by Mod-., selecting it', async () => {
		const editor = new RichTextEditor(textArea('<p>teh and wrold</p><p>they is</p>'),
			{ proofreader: new Words() });
		next(editor);
		expect(Popover.current()).toBeNull();
		await checked();

		editor.editor.commands.setTextSelection(5);
		next(editor);
		const { from, to } = editor.editor.state.selection;
		expect(editor.editor.state.doc.textBetween(from, to)).toBe('wrold');
		expect(Popover.current()?.element.textContent).toContain('world');

		editor.editor.commands.setTextSelection(20);
		next(editor);
		expect(Popover.current()?.element.querySelector('[id$="-title"]')?.textContent)
			.toBe('Grammar');
		expect(Popover.current()?.element.textContent).toContain('The verb does not agree');

		// after the last, the first
		const end = editor.editor.state.doc.content.size - 1;
		editor.editor.view.dispatch(editor.editor.state.tr.setSelection(
			TextSelection.create(editor.editor.state.doc, end)));
		next(editor);
		expect(issuesOf(editor.editor.state).map((found) => found.text)).toEqual(['teh', 'wrold',
			'they is']);
		const selected = editor.editor.state.selection;
		expect(editor.editor.state.doc.textBetween(selected.from, selected.to)).toBe('teh');
	});

	it('says there is no replacement, and the issue stays the editor\'s while its popover is open',
		async () => {
			const proofreader: RichTextProofreader = {
				check: async (texts) => texts.map(() => [{ from: 0, to: 3, kind: 'style' as const }])
			};
			const editor = new RichTextEditor(textArea('<p>Very long</p>'), { proofreader });
			await checked();
			const blur = vi.fn();
			editor.element.addEventListener('antilia-rich-text-editor:blur', blur);
			editor.focus();
			next(editor);
			const popover = Popover.current()!;
			expect(popover.element.querySelector('.antilia-rich-text-issue-none')?.textContent)
				.toBe('No suggestions');
			expect(underlined(editor)).toEqual(['Ver antilia-rich-text-issue-style']);
			expect(editor.editor.view.dom.querySelector('.antilia-rich-text-issue')
				?.hasAttribute('aria-invalid')).toBe(false);
			await checked(0);
			expect(blur).not.toHaveBeenCalled();
		});

	it('checks nothing read only, nor without a proofreader, and anew with another', async () => {
		const words = new Words();
		const editor = new RichTextEditor(textArea('<p>teh</p>'), { proofreader: words,
			readOnly: true });
		await checked();
		expect(words.asked).toEqual([]);

		editor.configure({ readOnly: false });
		await checked(0);
		expect(underlined(editor)).toHaveLength(1);

		editor.configure({ proofreader: null });
		expect(underlined(editor)).toEqual([]);
		expect(editor.editor.view.dom.hasAttribute('spellcheck')).toBe(false);

		const other = new Words({ teh: 'tea' });
		editor.configure({ proofreader: other, spellcheck: true });
		expect(editor.editor.view.dom.getAttribute('spellcheck')).toBe('true');
		await checked(0);
		expect(other.asked).toHaveLength(1);
		expect(underlined(editor)).toHaveLength(1);
	});

	it('asks again for what it no longer has, its request cancelled', async () => {
		let answer: ((issues: RichTextIssue[][]) => void) | null = null;
		const signals: AbortSignal[] = [];
		const proofreader: RichTextProofreader = {
			check: (_texts, _language, signal) => {
				signals.push(signal);
				return new Promise((resolve) => {
					answer = resolve;
				});
			}
		};
		const editor = new RichTextEditor(textArea('<p>one</p>'), { proofreader });
		await checked();
		expect(signals).toHaveLength(1);

		editor.setValue('<p>two</p>');
		await checked();
		expect(signals[0]!.aborted).toBe(true);
		expect(signals).toHaveLength(2);
		answer!([[]]);
		await checked(0);
		expect(underlined(editor)).toEqual([]);
	});

	it('tells of a proofreader failing, and asks again once the text changes', async () => {
		const log = vi.fn();
		const host: Host = { ...standaloneHost, log: { ...standaloneHost.log, error: log } };
		setHost(host);
		const check = vi.fn<RichTextProofreader['check']>()
			.mockRejectedValueOnce(new Error('down'))
			.mockImplementation(() => {
				throw new Error('again');
			});
		const editor = new RichTextEditor(textArea('<p>one</p>'), { proofreader: { check } });
		await checked();
		expect(log).toHaveBeenCalledWith('The proofreader of the rich text editor failed',
			new Error('down'));
		await checked(5000);
		expect(check).toHaveBeenCalledTimes(1);

		type(editor, 'x');
		await checked();
		expect(check).toHaveBeenCalledTimes(2);
		expect(log).toHaveBeenLastCalledWith('The proofreader of the rich text editor failed',
			new Error('again'));
	});

	it('stops once the editor goes', async () => {
		const words = new Words();
		const editor = new RichTextEditor(textArea('<p>teh</p>'), { proofreader: words,
			proofreadDelay: 100 });
		editor.destroy();
		await checked();
		expect(words.asked).toEqual([]);
	});
});

describe('languageToolProofreader', () => {
	it('asks LanguageTool about the texts at once, apart by blank lines, and splits its answer',
		async () => {
			const fetch = vi.fn(async (_url: string | URL | Request, _init?: RequestInit) =>
				new Response(JSON.stringify({ matches: [
					{ offset: 6, length: 5, message: 'Possible spelling mistake found.',
						replacements: [{ value: 'world' }, { value: 'word' }],
						rule: { id: 'MORFOLOGIK_RULE_EN_GB', issueType: 'misspelling' } },
					{ offset: 13, length: 7, message: 'Agreement', replacements: [{ value: 'they are' }],
						rule: { id: 'AGREEMENT', issueType: 'grammar' } },
					{ offset: 23, length: 2, replacements: [], rule: { issueType: 'whitespace' } },
					{ offset: 'x', length: 2 }
				] }), { status: 200 }));
			const proofreader = languageToolProofreader({ url: 'https://lt.example/v2/check',
				motherTongue: 'de', level: 'picky', parameters: { enabledOnly: 'false' },
				fetch: fetch as typeof globalThis.fetch });
			const signal = new AbortController().signal;

			const found = await proofreader.check(['Hello wrold', 'they is', 'a  b'], 'en-GB',
				signal);
			expect(found).toEqual([
				[{ from: 6, to: 11, kind: 'spelling', message: 'Possible spelling mistake found.',
					replacements: ['world', 'word'], rule: 'MORFOLOGIK_RULE_EN_GB' }],
				[{ from: 0, to: 7, kind: 'grammar', message: 'Agreement', replacements: ['they are'],
					rule: 'AGREEMENT' }],
				[{ from: 1, to: 3, kind: 'style' }]
			]);
			const [url, init] = fetch.mock.calls[0]!;
			expect(url).toBe('https://lt.example/v2/check');
			expect(init?.method).toBe('POST');
			expect(init?.signal).toBe(signal);
			const body = init?.body as URLSearchParams;
			expect(body.get('text')).toBe('Hello wrold\n\nthey is\n\na  b');
			expect(body.get('language')).toBe('en-GB');
			expect(body.get('motherTongue')).toBe('de');
			expect(body.get('level')).toBe('picky');
			expect(body.get('enabledOnly')).toBe('false');
		});

	it('lets LanguageTool find the language without one, and fails as the server does', async () => {
		const fetch = vi.fn(async () => new Response('Too many requests', { status: 429 }));
		const proofreader = languageToolProofreader({ url: '/check',
			fetch: fetch as unknown as typeof globalThis.fetch });

		await expect(proofreader.check(['a'], null, new AbortController().signal)).rejects
			.toThrow('LanguageTool answered 429');
		const init = (fetch.mock.calls[0] as unknown as [string, RequestInit])[1];
		expect((init.body as URLSearchParams).get('language')).toBe('auto');
	});

	it('names the kinds of LanguageTool\'s types of issues', () => {
		expect(languageToolKind('misspelling')).toBe('spelling');
		expect(languageToolKind('style')).toBe('style');
		expect(languageToolKind('typographical')).toBe('style');
		expect(languageToolKind('grammar')).toBe('grammar');
		expect(languageToolKind('duplication')).toBe('grammar');
		expect(languageToolKind(undefined)).toBe('grammar');
	});
});

describe('proofreadAt', () => {
	it('posts the texts and their language to the server, and takes the issues within them',
		async () => {
			const fetch = vi.fn(async (_url: string | URL | Request, _init?: RequestInit) =>
				new Response(JSON.stringify({ issues: [[{ from: 0, to: 3, kind: 'spelling',
					replacements: ['the'] }, { from: 2, to: 9, kind: 'spelling' }], []] }),
				{ status: 200 }));
			vi.stubGlobal('fetch', fetch);
			const signal = new AbortController().signal;

			expect(await proofreadAt('/check').check(['teh', 'ok'], 'en-GB', signal)).toEqual([
				[{ from: 0, to: 3, kind: 'spelling', replacements: ['the'] }], []]);
			const [url, init] = fetch.mock.calls[0]!;
			expect(url).toBe('/check');
			expect(init?.signal).toBe(signal);
			const body = init?.body as URLSearchParams;
			expect(body.get('texts')).toBe('["teh","ok"]');
			expect(body.get('language')).toBe('en-GB');

			await proofreadAt('/check').check(['a'], null, signal);
			expect((fetch.mock.calls[1]![1]?.body as URLSearchParams).has('language')).toBe(false);
		});

	it('fails as the server does', async () => {
		vi.stubGlobal('fetch', vi.fn(async () => new Response(JSON.stringify({
			error: 'The texts could not be checked' }), { status: 200 })));
		const signal = new AbortController().signal;
		await expect(proofreadAt('/check').check(['a'], 'en', signal)).rejects
			.toThrow('The texts could not be checked');

		vi.stubGlobal('fetch', vi.fn(async () => new Response('<html>', { status: 502 })));
		await expect(proofreadAt('/check').check(['a'], 'en', signal)).rejects
			.toThrow('The server answered 502');
	});
});
