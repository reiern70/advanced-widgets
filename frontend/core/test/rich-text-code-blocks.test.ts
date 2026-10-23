/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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

import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';

import { TextSelection } from '@tiptap/pm/state';

import { CODE_BLOCK_LANGUAGE_ATTRIBUTE, codeLanguage, lezerHighlighter, Popover,
	registerRichTextHighlighter, RICH_TEXT_CODE_LANGUAGES, RichTextEditor, richTextHighlighter,
	setHost, standaloneHost, type RichTextHighlighter } from '../src/index.js';

/** A moment, for the grammars loading and the blocks drawn again. */
const tick = (ms = 20) => new Promise((resolve) => setTimeout(resolve, ms));

beforeAll(async () => {
	const rect = { left: 0, top: 0, right: 0, bottom: 0, width: 0, height: 0, x: 0, y: 0,
		toJSON: () => ({}) } as DOMRect;
	Range.prototype.getClientRects = () => [] as unknown as DOMRectList;
	Range.prototype.getBoundingClientRect = () => rect;
	document.elementFromPoint = () => null;
	await lezerHighlighter.load('java');
});

const JAVA = '<pre><code class="language-java">public class Bird {}</code></pre>';

function textArea(html: string): HTMLTextAreaElement {
	document.body.innerHTML = '<form><label for="notes">Notes</label><textarea id="notes">' +
		'</textarea></form>';
	const field = document.getElementById('notes') as HTMLTextAreaElement;
	field.defaultValue = html;
	return field;
}

function tool(editor: RichTextEditor, name: string): HTMLButtonElement {
	return editor.element.querySelector<HTMLButtonElement>(
		`.antilia-rich-text-toolbar [data-tool="${name}"]`)!;
}

function select(editor: RichTextEditor, anchor: number): void {
	const state = editor.editor.state;
	editor.editor.view.dispatch(state.tr.setSelection(TextSelection.create(state.doc, anchor)));
}

/**
 * @returns the classes of the spans of a code block, as the editor shows it, by their text
 */
function spans(editor: RichTextEditor): Record<string, string> {
	const found: Record<string, string> = {};
	editor.editor.view.dom.querySelectorAll('pre span[class]').forEach((span) => {
		found[span.textContent!] = span.className;
	});
	return found;
}

afterEach(() => {
	Popover.closeAll();
	registerRichTextHighlighter(null);
	document.body.innerHTML = '';
	setHost(standaloneHost);
	vi.restoreAllMocks();
});

describe('codeLanguage', () => {
	it('names a language in lower case, from its other names too', () => {
		expect(codeLanguage('Java')).toBe('java');
		expect(codeLanguage(' js ')).toBe('javascript');
		expect(codeLanguage('tsx')).toBe('typescript');
		expect(codeLanguage('md')).toBe('markdown');
		expect(codeLanguage('python')).toBe('python');
		expect(codeLanguage('constructor')).toBe('constructor');
		expect(codeLanguage('')).toBeNull();
		expect(codeLanguage(null)).toBeNull();
		expect(codeLanguage('no spaces')).toBeNull();
		expect(codeLanguage('a'.repeat(41))).toBeNull();
	});
});

describe('lezerHighlighter', () => {
	it('highlights the languages of the code editor once their grammars are loaded', async () => {
		expect(Object.keys(lezerHighlighter.languages)).toEqual(Object.keys(RICH_TEXT_CODE_LANGUAGES));
		const tokens = lezerHighlighter.highlight('public class Bird {}', 'java')!;
		expect(tokens).toContainEqual({ from: 0, to: 6, classes: 'tok-keyword' });
		expect(tokens).toContainEqual({ from: 13, to: 17,
			classes: 'tok-variableName tok-definition' });

		expect(await lezerHighlighter.load('ts')).toBe(true);
		expect(lezerHighlighter.highlight('let a: number = 1;', 'typescript'))
			.toContainEqual({ from: 7, to: 13, classes: 'tok-typeName' });
		expect(await lezerHighlighter.load('json')).toBe(true);
		expect(lezerHighlighter.highlight('{"a": 1}', 'json'))
			.toContainEqual({ from: 6, to: 7, classes: 'tok-number' });
		expect(await lezerHighlighter.load('md')).toBe(true);
		expect(lezerHighlighter.highlight('# Title', 'markdown')?.[0]?.classes)
			.toContain('tok-heading');
		expect(await lezerHighlighter.load('javascript')).toBe(true);
		expect(lezerHighlighter.highlight('const a = <b/>;', 'js')?.[0])
			.toEqual({ from: 0, to: 5, classes: 'tok-keyword' });

		expect(await lezerHighlighter.load('python')).toBe(false);
		expect(lezerHighlighter.highlight('def a(): pass', 'python')).toBeNull();
		expect(await lezerHighlighter.load('constructor')).toBe(false);
	});
});

describe('the code blocks', () => {
	it('show in the colors of their language, the HTML staying plain', async () => {
		const editor = new RichTextEditor(textArea(JAVA), { highlight: lezerHighlighter });
		expect(spans(editor)).toEqual({ public: 'tok-keyword', class: 'tok-keyword',
			Bird: 'tok-variableName tok-definition', '{': 'tok-punctuation',
			'}': 'tok-punctuation' });
		const block = editor.editor.view.dom.querySelector('pre')!;
		expect(block.getAttribute(CODE_BLOCK_LANGUAGE_ATTRIBUTE)).toBe('Java');
		expect(editor.getValue()).toBe(JAVA);

		// typed: drawn again
		select(editor, 8);
		editor.editor.view.dispatch(editor.editor.state.tr.insertText('final '));
		expect(spans(editor)['final']).toBe('tok-keyword');
		expect(editor.getValue()).toBe('<pre><code class="language-java">public final class Bird {}' +
			'</code></pre>');
	});

	it('are drawn again once the grammar of their language loaded', async () => {
		const load = vi.fn((_language: string) => Promise.resolve(true));
		let loaded = false;
		const highlighter: RichTextHighlighter = {
			languages: { sql: 'SQL' },
			highlight: (text) => loaded ? [{ from: 0, to: text.length, classes: 'tok-keyword' }] : null,
			load: (language) => load(language).then((known) => {
				loaded = true;
				return known;
			})
		};
		const editor = new RichTextEditor(textArea('<pre><code class="language-sql">SELECT</code>' +
			'</pre><pre><code class="language-sql">FROM</code></pre>'), { highlight: highlighter });
		expect(spans(editor)).toEqual({});
		expect(load).toHaveBeenCalledTimes(1);
		expect(load).toHaveBeenCalledWith('sql');
		await tick();
		expect(spans(editor)).toEqual({ SELECT: 'tok-keyword', FROM: 'tok-keyword' });
		expect(editor.editor.view.dom.querySelector('pre')!.getAttribute(
			CODE_BLOCK_LANGUAGE_ATTRIBUTE)).toBe('SQL');
	});

	it('take the highlighter registered, when told to highlight', () => {
		expect(richTextHighlighter()).toBeNull();
		const editor = new RichTextEditor(textArea(JAVA), { highlight: true });
		expect(spans(editor)).toEqual({});
		// the menu offers the languages already
		expect(tool(editor, 'codeBlock').getAttribute('aria-haspopup')).toBe('menu');

		registerRichTextHighlighter(lezerHighlighter);
		expect(richTextHighlighter()).toBe(lezerHighlighter);
		expect(spans(editor).public).toBe('tok-keyword');

		editor.configure({ highlight: false });
		expect(spans(editor)).toEqual({});
		expect(editor.editor.view.dom.querySelector('pre')!.hasAttribute(
			CODE_BLOCK_LANGUAGE_ATTRIBUTE)).toBe(false);
		expect(tool(editor, 'codeBlock').getAttribute('aria-pressed')).toBe('true');
		editor.configure({ highlight: true });
		expect(spans(editor).public).toBe('tok-keyword');
	});

	it('are plain without a highlighter, or in a language it does not know', () => {
		const plain = new RichTextEditor(textArea(JAVA));
		expect(spans(plain)).toEqual({});
		expect(plain.editor.view.dom.querySelector('pre')!.hasAttribute(
			CODE_BLOCK_LANGUAGE_ATTRIBUTE)).toBe(false);

		const other = new RichTextEditor(textArea('<pre><code class="language-cobol">MOVE A TO B' +
			'</code></pre><pre><code>int a;</code></pre>'), { highlight: lezerHighlighter });
		expect(spans(other)).toEqual({});
		const blocks = other.editor.view.dom.querySelectorAll('pre');
		expect(blocks[0]!.getAttribute(CODE_BLOCK_LANGUAGE_ATTRIBUTE)).toBe('cobol');
		expect(blocks[1]!.hasAttribute(CODE_BLOCK_LANGUAGE_ATTRIBUTE)).toBe(false);
	});
});

describe('the menu of the code block', () => {
	const items = () => Array.from(document.querySelectorAll<HTMLButtonElement>(
		'[role="menu"] [role="menuitemradio"]'));

	it('is a tool making a code block while the editor has no languages', () => {
		const editor = new RichTextEditor(textArea('<p>Code</p>'));
		const button = tool(editor, 'codeBlock');
		expect(button.getAttribute('aria-haspopup')).toBeNull();
		expect(button.getAttribute('aria-pressed')).toBe('false');
		button.click();
		expect(editor.getValue()).toBe('<pre><code>Code</code></pre>');
	});

	it('makes a code block in a language, changes it, and makes it a paragraph again', () => {
		const editor = new RichTextEditor(textArea('<p>let a = 1;</p>'),
			{ highlight: lezerHighlighter });
		select(editor, 3);
		const button = tool(editor, 'codeBlock');
		expect(button.getAttribute('aria-haspopup')).toBe('menu');
		expect(button.hasAttribute('aria-pressed')).toBe(false);
		expect(button.getAttribute('aria-disabled')).toBe('false');
		button.click();
		expect(items().map((item) => item.textContent)).toEqual(['Paragraph', 'Plain text', 'Java',
			'JavaScript', 'TypeScript', 'JSON', 'Markdown']);
		expect(items().map((item) => item.getAttribute('aria-checked'))).toEqual(['true', 'false',
			'false', 'false', 'false', 'false', 'false']);
		expect(document.querySelector('[role="menu"] [role="separator"]')).not.toBeNull();

		items()[4]!.click();
		expect(editor.getValue()).toBe('<pre><code class="language-typescript">let a = 1;</code></pre>');
		expect(button.classList.contains('antilia-rich-text-tool-active')).toBe(true);

		button.click();
		expect(items().map((item) => item.getAttribute('aria-checked'))).toEqual(['false', 'false',
			'false', 'false', 'true', 'false', 'false']);
		items()[1]!.click();
		expect(editor.getValue()).toBe('<pre><code>let a = 1;</code></pre>');

		button.click();
		expect(items()[1]!.getAttribute('aria-checked')).toBe('true');
		items()[3]!.click();
		expect(editor.getValue()).toBe('<pre><code class="language-javascript">let a = 1;</code>' +
			'</pre>');

		button.click();
		items()[0]!.click();
		expect(editor.getValue()).toBe('<p>let a = 1;</p>');
	});

	it('takes the other names of a language as the language', () => {
		const editor = new RichTextEditor(textArea('<pre><code class="language-js">a</code></pre>' +
			'<pre><code class="language-cobol">b</code></pre>'), { highlight: lezerHighlighter });
		select(editor, 1);
		tool(editor, 'codeBlock').click();
		expect(items().find((item) => item.getAttribute('aria-checked') === 'true')?.textContent)
			.toBe('JavaScript');
		Popover.closeAll();
		tool(editor, 'codeBlock').click();
		select(editor, 5);
		tool(editor, 'codeBlock').click();
		// a language the menu has not: none checked
		expect(items().some((item) => item.getAttribute('aria-checked') === 'true')).toBe(false);
	});

	it('offers the languages the page gives, or none', () => {
		const editor = new RichTextEditor(textArea('<p>x</p>'), { codeLanguages: ['sql', 'Java',
			'java', 'not a name'] });
		tool(editor, 'codeBlock').click();
		expect(items().map((item) => item.textContent)).toEqual(['Paragraph', 'Plain text', 'sql',
			'Java']);
		items()[2]!.click();
		expect(editor.getValue()).toBe('<pre><code class="language-sql">x</code></pre>');
		// named, though not highlighted
		expect(editor.editor.view.dom.querySelector('pre')!.getAttribute(
			CODE_BLOCK_LANGUAGE_ATTRIBUTE)).toBe('sql');

		editor.configure({ codeLanguages: false });
		expect(tool(editor, 'codeBlock').getAttribute('aria-pressed')).toBe('true');

		const highlighted = new RichTextEditor(textArea('<p>x</p>'), { highlight: lezerHighlighter,
			codeLanguages: false });
		expect(tool(highlighted, 'codeBlock').getAttribute('aria-haspopup')).toBeNull();

		const none = new RichTextEditor(textArea('<p>x</p>'), { highlight: lezerHighlighter,
			features: ['bold'] });
		expect(none.codeLanguages()).toEqual([]);
	});
});
