/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';

import { BOOTSTRAP_LOOK, CodeEditor, loadLanguage, MarkdownPreview, markdownPreview,
	registerLanguage, renderMarkdown, setHost, standaloneHost } from '../src/index.js';
import { safeUrl } from '../src/markdown-preview.js';

/** A moment, for the grammars loading and the frame the preview waits for. */
const tick = () => new Promise((resolve) => setTimeout(resolve, 40));

beforeAll(() => {
	// jsdom lays nothing out: CodeMirror measures nothing
	const rect = { left: 0, top: 0, right: 0, bottom: 0, width: 0, height: 0, x: 0, y: 0,
		toJSON: () => ({}) } as DOMRect;
	Range.prototype.getClientRects = () => [] as unknown as DOMRectList;
	Range.prototype.getBoundingClientRect = () => rect;
});

afterEach(() => {
	document.body.innerHTML = '';
	setHost(standaloneHost);
	vi.restoreAllMocks();
});

/**
 * @returns the HTML of a text rendered into an element
 */
function html(text: string): string {
	const element = document.createElement('div');
	renderMarkdown(text, element);
	return element.innerHTML;
}

describe('renderMarkdown', () => {
	it('renders headings and paragraphs, with emphasis, code and line breaks', () => {
		expect(html('# Shop *front* #\n\nSetext\n---\n\nA **small** shop, `npm  run`, ~~old~~,' +
			' H~2~O, x^2^\nnext  \nbroken')).toBe('<h1>Shop <em>front</em></h1><h2>Setext</h2>' +
			'<p>A <strong>small</strong> shop, <code>npm  run</code>, <del>old</del>, H<sub>2</sub>O, ' +
			'x<sup>2</sup>\nnext<br>broken</p>');
		expect(html('###### Six\n\n`` a ` b ``')).toBe('<h6>Six</h6><p><code>a ` b</code></p>');
	});

	it('shows escapes, entities and HTML as text, never as HTML', () => {
		expect(html('a\\*b &amp; &#35; &#x41; &unknown; <b onclick="x()">bold</b> <!-- no -->'))
			.toBe('<p>a*b &amp; # A &amp;unknown; &lt;b onclick="x()"&gt;bold&lt;/b&gt; </p>');
		expect(html('<div onclick="x()">\nhtml\n</div>')).toBe(
			'<pre class="antilia-markdown-preview-html">&lt;div onclick="x()"&gt;\nhtml\n&lt;/div&gt;' +
			'</pre>');
		expect(html('<!-- a comment -->')).toBe('');
	});

	it('links to web addresses, the page\'s own and mail, in a new tab but for the page\'s places', () => {
		const element = document.createElement('div');
		renderMarkdown('[shop](https://example.org/shop "The shop") [here](#top) [docs](docs/a.md) ' +
			'<https://auto.org> www.bare.org [mail](mailto:ada@example.org)', element);
		const links = Array.from(element.querySelectorAll('a'));

		expect(links.map((link) => link.href)).toEqual(['https://example.org/shop',
			document.baseURI.replace(/#.*$/, '') + '#top', new URL('docs/a.md', document.baseURI).href,
			'https://auto.org/', 'http://www.bare.org/', 'mailto:ada@example.org']);
		expect(links[0].title).toBe('The shop');
		expect(links[0].target).toBe('_blank');
		expect(links[0].rel).toBe('noopener noreferrer');
		expect(links[1].hasAttribute('target')).toBe(false);
		expect(links.map((link) => link.textContent)).toEqual(['shop', 'here', 'docs',
			'https://auto.org', 'www.bare.org', 'mail']);
	});

	it('does not link to scripts or other addresses, showing their text', () => {
		const element = document.createElement('div');
		renderMarkdown('[a](javascript:alert(1)) [b](<java\tscript:alert(1)>) ' +
			'[c](\u0001javascript:x) [d](data:text/html,x) [e](vbscript:x) [f]()', element);

		expect(element.querySelectorAll('a')).toHaveLength(0);
		expect(Array.from(element.querySelectorAll('span')).map((span) => span.textContent))
			.toEqual(['a', 'b', 'c', 'd', 'e', 'f']);
		// as the browser reads an address: without its tabs and line breaks, and its controls
		expect(safeUrl('java\tscript:alert(1)')).toBeNull();
		expect(safeUrl('java\nscript:alert(1)')).toBeNull();
		expect(safeUrl(' \u0001javascript:alert(1)')).toBeNull();
		expect(safeUrl('  ')).toBeNull();
		expect(safeUrl('http://[bad')).toBeNull();
		expect(safeUrl('JAVASCRIPT:alert(1)')).toBeNull();
		expect(safeUrl('https://example.org')).toBe('https://example.org/');
	});

	it('shows pictures from the web and data: addresses, others as their text', () => {
		const element = document.createElement('div');
		renderMarkdown('![a shop](images/shop.png "The shop") ![tiny](data:image/png;base64,AAAA) ' +
			'![script](javascript:x) ![page](data:text/html,x) ![mail](mailto:x@y.z)', element);
		const images = Array.from(element.querySelectorAll('img'));

		expect(images.map((image) => image.getAttribute('src'))).toEqual([
			new URL('images/shop.png', document.baseURI).href, 'data:image/png;base64,AAAA']);
		expect(images[0].alt).toBe('a shop');
		expect(images[0].title).toBe('The shop');
		expect(images[0].getAttribute('loading')).toBe('lazy');
		expect(element.textContent).toBe('  script page mail');
	});

	it('takes the pictures it showed again, so that they do not load anew', () => {
		const element = document.createElement('div');
		renderMarkdown('![one](a.png "A") ![two](b.png)', element);
		const [first, second] = Array.from(element.querySelectorAll('img'));

		renderMarkdown('# Pictures\n\n![two again](b.png) ![one again](a.png)', element);
		const images = Array.from(element.querySelectorAll('img'));
		expect(images).toEqual([second, first]);
		expect(images.map((image) => image.alt)).toEqual(['two again', 'one again']);
		expect(first.hasAttribute('title')).toBe(false);
	});

	it('follows references to links and pictures, and shows those not defined as written', () => {
		expect(html('[the shop][Shop], [shop][], [shop], ![logo][pic] and [*none*]\n\n' +
			'[shop]: https://example.org  "Shop"\n[pic]: logo.png\n> [quoted]: https://q.org\n\n' +
			'[quoted]')).toBe('<p><a href="https://example.org/" target="_blank" ' +
			'rel="noopener noreferrer" title="Shop">the shop</a>, <a href="https://example.org/" ' +
			'target="_blank" rel="noopener noreferrer" title="Shop">shop</a>, ' +
			'<a href="https://example.org/" target="_blank" rel="noopener noreferrer" title="Shop">' +
			'shop</a>, <img src="' + new URL('logo.png', document.baseURI).href + '" ' +
			'loading="lazy" alt="logo"> and [<em>none</em>]</p><blockquote></blockquote>' +
			'<p><a href="https://q.org/" target="_blank" rel="noopener noreferrer">quoted</a></p>');
	});

	it('renders tight lists without paragraphs and loose ones with them', () => {
		expect(html('- one\n- two\n  - nested\n\n3. three\n4. four')).toBe(
			'<ul><li>one</li><li>two<ul><li>nested</li></ul></li></ul>' +
			'<ol start="3"><li>three</li><li>four</li></ol>');
		expect(html('1. one\n\n2. two')).toBe('<ol><li><p>one</p></li><li><p>two</p></li></ol>');
		expect(html('- one\n\n  more')).toBe('<ul><li><p>one</p><p>more</p></li></ul>');
	});

	it('renders tasks with check boxes the user cannot change', () => {
		const element = document.createElement('div');
		renderMarkdown('- [ ] to do\n- [x] **done**', element);
		const items = Array.from(element.querySelectorAll('li'));
		const boxes = Array.from(element.querySelectorAll('input'));

		expect(items.map((item) => item.className)).toEqual(['antilia-markdown-preview-task',
			'antilia-markdown-preview-task']);
		expect(items.map((item) => item.textContent)).toEqual([' to do', ' done']);
		expect(boxes.map((box) => [box.type, box.disabled, box.checked])).toEqual([
			['checkbox', true, false], ['checkbox', true, true]]);
		expect(items[1].querySelector('strong')?.textContent).toBe('done');

		renderMarkdown('- [x] done\n\n- [ ] loose', element);
		expect(element.querySelectorAll('li > p > input')).toHaveLength(2);
	});

	it('renders quotes, rules and code blocks, without the marks of the quotes', () => {
		expect(html('> quoted\ncontinued\n> ```\n> let a;\n>\n> let b;\n> ```\n\n***\n\n' +
			'    indented\n\n    code')).toBe('<blockquote><p>quoted\ncontinued</p>' +
			'<pre class="antilia-markdown-preview-code"><code>let a;\n\nlet b;</code></pre>' +
			'</blockquote><hr><pre class="antilia-markdown-preview-code"><code>indented\n\ncode' +
			'</code></pre>');
		expect(html('```\n```')).toBe('<pre class="antilia-markdown-preview-code"><code></code></pre>');
		expect(html('```unknown-language\nplain\n```')).toBe('<pre class="antilia-markdown-preview-' +
			'code"><code class="language-unknown-language">plain</code></pre>');
	});

	it('renders tables, their columns aligned, rows short of cells filled', () => {
		const element = document.createElement('div');
		renderMarkdown('| Item | Price | Count | Note |\n|:--|--:|:-:|---|\n| *tea* | 2.50 | 3 |' +
			' x |\n| cake |', element);
		const table = element.querySelector('table')!;

		expect(table.className).toBe('antilia-markdown-preview-table');
		expect(Array.from(table.querySelectorAll('th')).map((cell) => [cell.textContent,
			cell.className])).toEqual([['Item', 'antilia-markdown-preview-left'],
			['Price', 'antilia-markdown-preview-right'], ['Count', 'antilia-markdown-preview-center'],
			['Note', '']]);
		expect(Array.from(table.querySelectorAll('tbody tr')).map((row) => Array.from(
			row.children).map((cell) => cell.textContent))).toEqual([['tea', '2.50', '3', 'x'],
			['cake', '', '', '']]);
		expect(table.querySelector('td em')?.textContent).toBe('tea');

		renderMarkdown('| a |\n|---|\n| b |', element, BOOTSTRAP_LOOK);
		expect(element.querySelector('table')!.className).toBe(
			'antilia-markdown-preview-table table table-sm');
	});

	it('highlights the code blocks by the grammars of the editor, once loaded', async () => {
		const element = document.createElement('div');
		const text = '```java\nint total = 0;\n```';
		await loadLanguage('java');

		expect(renderMarkdown(text, element)).toEqual([]);
		const code = element.querySelector('code')!;
		expect(code.className).toBe('language-java');
		expect(code.textContent).toBe('int total = 0;');
		expect(code.querySelector('.tok-typeName')?.textContent).toBe('int');
		expect(code.querySelector('.tok-number')?.textContent).toBe('0');
	});

	it('tells which grammars it waits for, and shows their code plain meanwhile', () => {
		registerLanguage('later', () => new Promise(() => undefined));
		const element = document.createElement('div');

		expect(renderMarkdown('```later\nsome code\n```', element)).toEqual(['later']);
		expect(element.querySelector('code')!.innerHTML).toBe('some code');
	});
});

describe('MarkdownPreview', () => {
	it('renders the text into its element, a region by its label, that takes the focus', () => {
		document.body.innerHTML = '<div id="preview" class="mine"></div>';
		const onRender = vi.fn();
		const preview = MarkdownPreview.attach('preview', { value: '# Notes', label: 'Preview',
			onRender })!;
		const element = preview.element;

		expect(MarkdownPreview.of('preview')).toBe(preview);
		expect(MarkdownPreview.of(element)).toBe(preview);
		expect(element.className).toBe('mine antilia-markdown-preview');
		expect(element.getAttribute('role')).toBe('region');
		expect(element.getAttribute('aria-label')).toBe('Preview');
		expect(element.tabIndex).toBe(0);
		expect(element.innerHTML).toBe('<h1>Notes</h1>');
		expect(onRender).toHaveBeenCalledWith(element);
		expect(MarkdownPreview.attach('none')).toBeNull();

		preview.setValue('*more*');
		expect(preview.getValue()).toBe('*more*');
		expect(element.innerHTML).toBe('<p><em>more</em></p>');
		preview.setValue('*more*');
		expect(onRender).toHaveBeenCalledTimes(2);

		preview.configure({ label: '' });
		expect(element.hasAttribute('role')).toBe(false);
		expect(element.hasAttribute('aria-label')).toBe(false);

		preview.destroy();
		expect(element.outerHTML).toBe('<div id="preview" class="mine"></div>');
		expect(MarkdownPreview.of(element)).toBeNull();
	});

	it('says what its placeholder says while the text is empty', () => {
		const element = document.createElement('div');
		const preview = new MarkdownPreview(element, { value: '  ', placeholder: 'Nothing yet' });

		expect(element.innerHTML).toBe('<p class="antilia-markdown-preview-empty">Nothing yet</p>');
		preview.configure({ placeholder: '' });
		expect(element.innerHTML).toBe('');
		preview.setValue('text');
		expect(element.innerHTML).toBe('<p>text</p>');
	});

	it('follows the text of a code editor, rendered anew by the page too', async () => {
		document.body.innerHTML = '<textarea id="code"># One</textarea><div id="preview"></div>';
		const editor = new CodeEditor(document.getElementById('code')!);
		const preview = MarkdownPreview.attach('preview', { editor: 'code' })!;
		const element = preview.element;
		expect(element.innerHTML).toBe('<h1>One</h1>');

		editor.view.dispatch({ changes: { from: 5, insert: ' and two' }, userEvent: 'input.type' });
		editor.view.dispatch({ changes: { from: 0, insert: '#' }, userEvent: 'input.type' });
		expect(element.innerHTML).toBe('<h1>One</h1>');
		await tick();
		expect(element.innerHTML).toBe('<h2>One and two</h2>');

		// the page's own change, and an editor rendered anew in the same place
		editor.setValue('page');
		await tick();
		expect(element.innerHTML).toBe('<p>page</p>');
		const field = document.getElementById('code') as HTMLTextAreaElement;
		field.value = 'anew';
		new CodeEditor(field);
		await tick();
		expect(element.innerHTML).toBe('<p>anew</p>');

		// another editor is not the one followed
		const other = document.createElement('textarea');
		document.body.append(other);
		new CodeEditor(other, { value: 'other' });
		await tick();
		expect(element.innerHTML).toBe('<p>anew</p>');
	});

	it('takes the text of an editor not made yet from its text area, and the editor\'s later', async () => {
		document.body.innerHTML = '<div id="preview"></div><textarea id="code">*field*</textarea>';
		const preview = new MarkdownPreview(document.getElementById('preview')!, {
			editor: document.getElementById('code'), value: 'ignored' });
		expect(preview.getValue()).toBe('*field*');

		new CodeEditor(document.getElementById('code')!, { value: '**editor**' });
		await tick();
		expect(preview.element.innerHTML).toBe('<p><strong>editor</strong></p>');

		preview.configure({ editor: null, value: 'own' });
		expect(preview.element.innerHTML).toBe('<p>own</p>');
		new MarkdownPreview(document.createElement('div'), { editor: 'none', value: 'given' });
	});

	it('renders the code blocks again once their grammars are loaded', async () => {
		let resolve: (language: unknown) => void = () => undefined;
		registerLanguage('slow', (kit) => new Promise((done) => {
			resolve = done;
		}).then(() => kit.StreamLanguage.define({
			token: (stream) => {
				if (stream.match(/^\d+/)) {
					return 'number';
				}
				stream.next();
				return null;
			}
		})));
		const element = document.createElement('div');
		const preview = new MarkdownPreview(element, { value: '```slow\nanswer 42\n```' });
		expect(element.querySelector('.tok-number')).toBeNull();

		resolve(null);
		await tick();
		expect(element.querySelector('.tok-number')?.textContent).toBe('42');
		expect(preview.getValue()).toBe('```slow\nanswer 42\n```');
	});

	it('does not render the text again when a grammar did not load', async () => {
		vi.spyOn(console, 'error').mockImplementation(() => undefined);
		registerLanguage('broken', () => Promise.reject(new Error('no grammar')));
		const onRender = vi.fn();
		new MarkdownPreview(document.createElement('div'), { value: '```broken\nx\n```', onRender });

		await tick();
		expect(onRender).toHaveBeenCalledTimes(1);
	});

	it('stops following the editor once the page takes its element away', async () => {
		let removing: ((element: Element) => void) | null = null;
		setHost({ ...standaloneHost, onNodeRemoving: (listener) => {
			removing = listener;
			return () => {
				removing = null;
			};
		} });
		document.body.innerHTML = '<textarea id="code">one</textarea><div id="preview"></div>';
		const editor = new CodeEditor(document.getElementById('code')!);
		const element = document.getElementById('preview')!;
		const preview = new MarkdownPreview(element, { editor: 'code' });

		editor.view.dispatch({ changes: { from: 0, insert: 'a ' }, userEvent: 'input.type' });
		removing!(document.body);
		element.remove();
		await tick();
		expect(preview.getValue()).toBe('one');
		expect(removing).toBeNull();
	});

	it('is a Svelte action too', () => {
		const element = document.createElement('div');
		const action = markdownPreview(element, { value: '# One' });
		expect(element.innerHTML).toBe('<h1>One</h1>');

		action.update({ value: '# Two' });
		expect(element.innerHTML).toBe('<h1>Two</h1>');
		action.destroy();
		expect(element.innerHTML).toBe('');
	});

	it('takes the look of the page for its tables', () => {
		document.body.innerHTML = '<div data-antilia-look="bootstrap"><div id="preview"></div></div>';
		const preview = MarkdownPreview.attach('preview', { value: '| a |\n|---|\n| b |' })!;

		expect(preview.element.querySelector('table')!.className).toBe(
			'antilia-markdown-preview-table table table-sm');
	});
});
