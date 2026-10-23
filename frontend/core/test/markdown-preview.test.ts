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

import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';

import { BOOTSTRAP_LOOK, CodeEditor, loadLanguage, MarkdownPreview, markdownPreview,
	registerLanguage, renderMarkdown, setHost, standaloneHost } from '../src/index.js';
import { blockAt, matchingHeight, renderMarkdownBlocks, safeUrl, stepScrollTop,
	type MarkdownBlock, type StepGeometry } from '../src/markdown-preview.js';

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

/**
 * @returns the geometry of an editor whose lines are 20 pixels high, below a padding of 4, and
 *          of a preview whose blocks are at the tops given
 */
function geometry(lines: number, tops: Map<HTMLElement, number>,
	previewHeight: number): StepGeometry {
	return {
		lineAt: (height) => Math.min(Math.max(Math.floor((height - 4) / 20) + 1, 1), lines),
		lineTop: (line) => 4 + (line - 1) * 20,
		blockTop: (element) => tops.get(element)!,
		editorHeight: 8 + lines * 20,
		previewHeight
	};
}

/**
 * @returns a block of each line of a text of a hundred, each 40 pixels high in the preview,
 *          below a padding of 10, and their geometry
 */
function evenBlocks(): [MarkdownBlock[], StepGeometry] {
	const blocks = Array.from({ length: 100 }, (_, index) => ({ line: index + 1,
		element: document.createElement('p') }));
	const tops = new Map(blocks.map((block) => [block.element, 10 + (block.line - 1) * 40]));
	return [blocks, geometry(100, tops, 10 + 100 * 40 + 10)];
}

describe('keeping in step with the editor', () => {
	it('knows the line of the text each block starts at', () => {
		const blocks = renderMarkdownBlocks('# Title\n\nA paragraph\nover two lines\n\n- one\n- two\n\n' +
			'> quoted\n>\n> again\n\n```js\nlet x = 1;\n```\n\n| a | b |\n|---|---|\n| 1 | 2 |\n\n' +
			'---\n\n1. first\n\n2. second\n   with more\n\n- [x] done', document.createElement('div'));

		expect(blocks.map((block) => block.element.tagName.toLowerCase() + ' ' + block.line))
			.toEqual(['h1 1', 'p 3', 'ul 6', 'li 6', 'li 7', 'blockquote 9', 'p 9', 'p 11', 'pre 13',
				'table 17', 'tr 17', 'tr 19', 'hr 21', 'ol 23', 'li 23', 'p 23', 'li 25', 'p 25',
				'ul 28', 'li 28']);
		// and the HTML is as it was, without marks of the lines
		expect(blocks[0].element.outerHTML).toBe('<h1>Title</h1>');
		expect(renderMarkdownBlocks('', document.createElement('div'))).toEqual([]);
	});

	it('finds the last block starting at a line or before it', () => {
		const blocks = [1, 3, 3, 6].map((line) => ({ line, element: document.createElement('p') }));

		expect([0, 1, 2, 3, 5, 6, 40].map((line) => blockAt(blocks, line))).toEqual(
			[-1, 0, 0, 2, 2, 3, 3]);
		expect(blockAt([], 4)).toBe(-1);
	});

	it('matches a height of the editor to one of the preview, between the blocks around it', () => {
		const [title, paragraph, heading] = ['h1', 'p', 'h2'].map((name) =>
			document.createElement(name));
		const blocks = [{ line: 1, element: title }, { line: 3, element: paragraph },
			{ line: 6, element: heading }];
		const sizes = geometry(6, new Map([[title, 16], [paragraph, 70], [heading, 200]]), 300);
		const at = (height: number) => matchingHeight(blocks, sizes, height);

		// the tops of the lines are those of their blocks
		expect([at(4), at(44), at(104)]).toEqual([16, 70, 200]);
		// the starts and the ends of the two match, the paddings above the first lines too
		expect([at(0), at(2), at(128)]).toEqual([0, 8, 300]);
		// in a block of two lines, and the empty line after it, a sixth of the way to the next
		expect(at(54)).toBeCloseTo(70 + 130 / 6);
		expect(at(116)).toBe(250);
		expect(matchingHeight([], sizes, 64)).toBe(150);
	});

	it('scrolls the preview to show the part matching the cursor as high as the cursor', () => {
		const [blocks, sizes] = evenBlocks();
		// lines 50 to 60 in sight, the cursor on line 56, half way down
		const top = stepScrollTop(blocks, sizes, { scrollTop: 1000, height: 200,
			cursor: 4 + 55 * 20 }, 300);

		expect(top).toBeCloseTo(10 + 55 * 40 - 0.52 * 300);
		// scrolling the editor, the cursor in sight, scrolls the preview as far, in proportion to
		// their heights
		expect(stepScrollTop(blocks, sizes, { scrollTop: 1010, height: 200, cursor: 4 + 55 * 20 },
			300)).toBeCloseTo(top + 10 * 300 / 200);
	});

	it('takes the edges of the editor with the cursor out of sight, without a jump', () => {
		const [blocks, sizes] = evenBlocks();
		const scrolled = (scrollTop: number, cursor: number) => stepScrollTop(blocks, sizes,
			{ scrollTop, height: 200, cursor }, 300);

		// the cursor just above the editor, and on its top line: about the same
		expect(Math.abs(scrolled(1000, 999) - scrolled(1000, 1001))).toBeLessThan(1);
		expect(Math.abs(scrolled(1000, 1199) - scrolled(1000, 1201))).toBeLessThan(1);
		// a viewport away and more, the start of the text shows the start of the preview, and its
		// end the end
		expect(scrolled(0, 1900)).toBe(0);
		expect(scrolled(1808, 4)).toBe(4020 - 300);
		// a preview shorter than it is high does not scroll
		expect(stepScrollTop(blocks, { ...sizes, previewHeight: 200 }, { scrollTop: 1000,
			height: 200, cursor: 1100 }, 300)).toBe(0);
	});

	it('scrolls with the editor it follows while asked to, and while both show', async () => {
		const text = Array.from({ length: 30 }, (_, index) => '## Part ' + (index + 1))
			.join('\n\n');
		document.body.innerHTML = '<textarea id="code"></textarea><div id="preview"></div>';
		const editor = new CodeEditor(document.getElementById('code')!, { value: text });
		const element = document.getElementById('preview')!;
		const preview = new MarkdownPreview(element, { editor: 'code' });

		// the layout: the editor's lines 20 pixels high, the preview's headings 100, both 200 high
		const view = editor.view;
		const scroller = view.scrollDOM;
		const doc = view.state.doc;
		let editorScroll = 0;
		let previewScroll = 0;
		const box = (target: HTMLElement, sizes: Record<string, number>) => Object.entries(sizes)
			.forEach(([name, value]) => Object.defineProperty(target, name, { configurable: true,
				get: () => value }));
		box(scroller, { clientHeight: 200, clientWidth: 300, scrollHeight: doc.lines * 20 });
		box(element, { clientHeight: 200, clientWidth: 300, scrollHeight: 30 * 100 });
		Object.defineProperty(scroller, 'scrollTop', { configurable: true, get: () => editorScroll,
			set: () => undefined });
		Object.defineProperty(element, 'scrollTop', { configurable: true,
			get: () => previewScroll, set: (value: number) => {
				previewScroll = value;
			} });
		const rect = (top: number) => ({ top, left: 0, right: 0, bottom: top, width: 0, height: 0,
			x: 0, y: top, toJSON: () => ({}) }) as DOMRect;
		vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockImplementation(function (
			this: HTMLElement) {
			return rect(this.parentElement === element ?
				Array.from(element.children).indexOf(this) * 100 - previewScroll : 0);
		});
		const lineBlock = (number: number) => {
			const line = doc.line(Math.min(Math.max(number, 1), doc.lines));
			return { from: line.from, to: line.to, length: line.length, top: (line.number - 1) * 20,
				height: 20, bottom: line.number * 20, type: 0 } as unknown as ReturnType<
				typeof view.lineBlockAt>;
		};
		Object.defineProperty(view, 'documentTop', { configurable: true,
			get: () => -editorScroll });
		vi.spyOn(view, 'lineBlockAt').mockImplementation((position) =>
			lineBlock(doc.lineAt(position).number));
		vi.spyOn(view, 'lineBlockAtHeight').mockImplementation((height) =>
			lineBlock(Math.floor(height / 20) + 1));
		vi.spyOn(view, 'coordsAtPos').mockImplementation((position) => {
			const top = (doc.lineAt(position).number - 1) * 20 - editorScroll;
			return { top, bottom: top + 20, left: 0, right: 0 };
		});
		// the cursor on Part 21, on line 41, half way down the editor
		const moveTo = async (line: number) => {
			view.dispatch({ selection: { anchor: doc.line(line).from }, userEvent: 'select' });
			await tick();
		};
		editorScroll = 700;

		await moveTo(41);
		expect(previewScroll).toBe(0);

		preview.configure({ scrollWithEditor: true });
		await tick();
		expect(previewScroll).toBe(2000 - 100);

		// scrolled to the top, the cursor far below: the preview shows its start
		editorScroll = 0;
		scroller.dispatchEvent(new Event('scroll'));
		await tick();
		expect(previewScroll).toBe(0);

		// collapsed in a split panel, the preview does not scroll; shown again, it does
		element.style.visibility = 'hidden';
		editorScroll = 700;
		await moveTo(41);
		expect(previewScroll).toBe(0);
		element.style.visibility = '';
		await moveTo(41);
		expect(previewScroll).toBe(1900);

		// another editor is not the one followed
		document.body.insertAdjacentHTML('beforeend', '<textarea id="other"></textarea>');
		const other = new CodeEditor(document.getElementById('other')!, { value: 'a\nb' });
		editorScroll = 0;
		other.view.dispatch({ selection: { anchor: 2 }, userEvent: 'select' });
		await tick();
		expect(previewScroll).toBe(1900);

		preview.configure({ scrollWithEditor: false });
		await moveTo(1);
		expect(previewScroll).toBe(1900);
		preview.destroy();
	});
});
