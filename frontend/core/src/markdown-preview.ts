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

/*
 * A preview of Markdown: the text as a page shows it, with GitHub's tables, task lists and
 * strikethrough, its fenced code blocks highlighted by the grammars of the code editor in the
 * colors of the theme.
 *
 * The text is read by the grammar of the code editor, Lezer's Markdown parser, and the preview is
 * built from its syntax tree an element at a time, never parsed from HTML: HTML in the text shows
 * as text, and links and images take only web addresses, those of the page's own site, http,
 * https and mailto, and pictures in data: addresses. So the preview shows safely what anyone wrote,
 * and works under a strict Content Security Policy.
 *
 * new MarkdownPreview(element, options) renders the text into an element, and setValue() another
 * one. With the option editor, the preview follows a code editor: it shows its text, and renders
 * it again, a frame later, each time it changes, by the user or the page; the editor may be
 * rendered anew, as Wicket does, and the preview follows the new one. renderMarkdown() renders a
 * text into an element once.
 *
 * With scrollWithEditor too, the preview keeps in step with the editor: as the user moves the
 * cursor, types or scrolls the editor, the preview scrolls to the part matching the place in the
 * editor. The blocks it renders know the line of the text they start at, and the preview matches
 * the editor's lines to them, between the tops of the blocks around a line (stepScrollTop()). While
 * the preview or the editor is hidden, in a collapsed pane of a split panel for example, nothing
 * scrolls; shown again, the preview comes in step.
 */

import { Language, LanguageSupport } from '@codemirror/language';
import { markdownLanguage } from '@codemirror/lang-markdown';
import type { SyntaxNode } from '@lezer/common';
import { classHighlighter, highlightCode } from '@lezer/highlight';

import { CODE_EDITOR_EVENTS, CodeEditor, loadedLanguage, loadLanguage } from './code-editor.js';
import { onRemoving, type Unsubscribe } from './host.js';
import { lookOf, partClasses, type Look, type LookPart } from './look.js';

/**
 * How a preview shows the text.
 */
export interface MarkdownPreviewOptions {
	/** The text, in Markdown; none when left out. */
	value?: string;
	/**
	 * A code editor whose text the preview shows: its text area or its element, or the id of
	 * either. The preview follows it as its text changes.
	 */
	editor?: HTMLElement | string | null;
	/**
	 * Whether the preview keeps in step with the editor it follows: as the user moves the cursor,
	 * types or scrolls the editor, the preview scrolls to the part matching the place in the
	 * editor. False when left out: each scrolls on its own.
	 */
	scrollWithEditor?: boolean;
	/** The look of the preview; that of the element it is in when left out (see lookOf). */
	look?: Look;
	/** The accessible name of the preview, a region of the page. */
	label?: string;
	/** What the preview says while the text is empty, nothing when left out. */
	placeholder?: string;
	/** Called each time the preview rendered the text. */
	onRender?: (element: HTMLElement) => void;
}

/** The classes of the preview and its parts. */
export const MARKDOWN_PREVIEW_CLASSES = Object.freeze({
	preview: 'antilia-markdown-preview',
	empty: 'antilia-markdown-preview-empty',
	code: 'antilia-markdown-preview-code',
	task: 'antilia-markdown-preview-task',
	html: 'antilia-markdown-preview-html',
	alignLeft: 'antilia-markdown-preview-left',
	alignCenter: 'antilia-markdown-preview-center',
	alignRight: 'antilia-markdown-preview-right'
});

/** The addresses links may go to, besides those of the page's own site. */
const LINK_PROTOCOLS = new Set(['http:', 'https:', 'mailto:']);

/** The addresses pictures may come from, besides those of the page's own site. */
const IMAGE_PROTOCOLS = new Set(['http:', 'https:']);

/** The entities of HTML a text uses most, the others are left as written. */
const ENTITIES: Record<string, string> = { amp: '&', lt: '<', gt: '>', quot: '"', apos: '\'',
	nbsp: ' ', copy: '©', reg: '®', trade: '™', hellip: '…',
	mdash: '—', ndash: '–', laquo: '«', raquo: '»', euro: '€',
	middot: '·', times: '×', larr: '←', rarr: '→' };

/** The markers of the syntax, which the preview does not show. */
const MARKS = new Set(['HeaderMark', 'QuoteMark', 'ListMark', 'LinkMark', 'EmphasisMark',
	'CodeMark', 'CodeInfo', 'LinkTitle', 'LinkLabel', 'URL', 'StrikethroughMark', 'SubscriptMark',
	'SuperscriptMark', 'TaskMarker', 'TableDelimiter']);

/** The inline elements, by the nodes of the syntax they show. */
const INLINE_ELEMENTS: Record<string, string> = { Emphasis: 'em', StrongEmphasis: 'strong',
	Strikethrough: 'del', Subscript: 'sub', Superscript: 'sup' };

/** The previews, by their element. */
const instances = new WeakMap<Element, MarkdownPreview>();

/** An address a link of a reference goes to, and its title. */
interface Reference {
	url: string;
	title: string | null;
}

/** The pictures of a preview, by their address, which the preview rendered anew takes again. */
type Pictures = Map<string, HTMLImageElement[]>;

/**
 * A block the preview rendered, a heading, a paragraph, an item of a list or a row of a table for
 * example: its element, and the line of the text it starts at, 1 for the first.
 */
export interface MarkdownBlock {
	line: number;
	element: HTMLElement;
}

/**
 * What the preview measures to keep in step with an editor: heights from the top of the content
 * of each, in pixels, the editor's taking its padding, as its scroll position does.
 */
export interface StepGeometry {
	/** The line of the text at a height of the editor's content, 1 for the first. */
	lineAt(height: number): number;
	/** The height of the top of a line in the editor's content. */
	lineTop(line: number): number;
	/** The height of the top of an element of the preview in its content. */
	blockTop(element: HTMLElement): number;
	/** The height of the editor's content. */
	editorHeight: number;
	/** The height of the preview's content. */
	previewHeight: number;
}

/** Where the editor is: how far it is scrolled, and where its cursor is, in its content. */
export interface StepEditor {
	/** How far its content is scrolled, in pixels. */
	scrollTop: number;
	/** The height of what it shows of its content. */
	height: number;
	/** The height of the cursor in its content: the top of the cursor. */
	cursor: number;
}

/**
 * @returns the label of a reference as references are matched: without its brackets, its case
 *          and its runs of white space
 */
function normalizeLabel(label: string): string {
	return label.replace(/^\[|\]$/g, '').trim().replace(/\s+/g, ' ').toLowerCase();
}

/**
 * @returns the text of an entity, or the entity as written when it is none the preview knows
 */
function decodeEntity(entity: string): string {
	const numeric = /^&#(x[0-9a-f]+|\d+);$/i.exec(entity);
	if (numeric) {
		const code = numeric[1][0].toLowerCase() === 'x' ? parseInt(numeric[1].slice(1), 16) :
			parseInt(numeric[1], 10);
		return code > 0 && code <= 0x10ffff ? String.fromCodePoint(code) : '�';
	}
	return ENTITIES[entity.slice(1, -1)] ?? entity;
}

/**
 * @returns the text with its escapes and its entities as what they stand for
 */
function unescape(text: string): string {
	return text.replace(/\\([!-/:-@[-`{-~])/g, '$1')
		.replace(/&(#x[0-9a-f]+|#\d+|\w+);/gi, (entity) => decodeEntity(entity));
}

/**
 * @returns the address of a link or a picture, made absolute against the page; null when it may
 *          not be followed: an address of a script, for example, or one the browser cannot read
 */
export function safeUrl(url: string, image = false): string | null {
	if (!url.trim()) {
		return null;
	}
	let parsed: URL;
	try {
		// the browser's own reading of the address, as it would follow it
		parsed = new URL(url.trim(), document.baseURI);
	} catch {
		return null;
	}
	if (image && parsed.protocol === 'data:') {
		return /^data:image\/[\w.+-]+[;,]/i.test(parsed.href) ? parsed.href : null;
	}
	return (image ? IMAGE_PROTOCOLS : LINK_PROTOCOLS).has(parsed.protocol) ? parsed.href : null;
}

/**
 * @returns the language of a grammar of the editor, null for an extension that is none
 */
function languageOf(extension: unknown): Language | null {
	return extension instanceof LanguageSupport ? extension.language :
		extension instanceof Language ? extension : null;
}

/**
 * Builds the elements of a text, from its syntax tree.
 */
class Renderer {
	/** The languages of the code blocks whose grammars are not loaded yet. */
	readonly pending = new Set<string>();

	/** The blocks rendered, in the order of the text, as they are in the page. */
	readonly rendered: MarkdownBlock[] = [];

	private readonly references = new Map<string, Reference>();

	/** Where the lines of the text start. */
	private readonly lineStarts: number[] = [0];

	/**
	 * @param tableClasses the classes of the tables, in the look of the preview
	 */
	constructor(private readonly text: string, private readonly document: Document,
		private readonly pictures: Pictures, private readonly tableClasses: string) {
		for (let index = text.indexOf('\n'); index >= 0; index = text.indexOf('\n', index + 1)) {
			this.lineStarts.push(index + 1);
		}
	}

	render(target: Node): void {
		const tree = markdownLanguage.parser.parse(this.text);
		this.collectReferences(tree.topNode);
		this.blocks(tree.topNode, target, false);
	}

	private slice(from: number, to: number): string {
		return this.text.slice(from, to);
	}

	/**
	 * @returns the line of a position of the text, 1 for the first
	 */
	private lineOf(position: number): number {
		const starts = this.lineStarts;
		let low = 0;
		let high = starts.length - 1;
		while (low < high) {
			const middle = (low + high + 1) >> 1;
			if (starts[middle] <= position) {
				low = middle;
			} else {
				high = middle - 1;
			}
		}
		return low + 1;
	}

	private element<K extends keyof HTMLElementTagNameMap>(name: K, parent: Node,
		className?: string): HTMLElementTagNameMap[K] {
		const element = this.document.createElement(name);
		if (className) {
			element.className = className;
		}
		parent.appendChild(element);
		return element;
	}

	/**
	 * Adds the element of a block of the text, which knows the line the block starts at.
	 */
	private blockElement<K extends keyof HTMLElementTagNameMap>(name: K, parent: Node,
		node: SyntaxNode, className?: string): HTMLElementTagNameMap[K] {
		const element = this.element(name, parent, className);
		this.rendered.push({ line: this.lineOf(node.from), element });
		return element;
	}

	private textNode(text: string, parent: Node): void {
		if (text) {
			parent.appendChild(this.document.createTextNode(text));
		}
	}

	private collectReferences(node: SyntaxNode): void {
		for (let child = node.firstChild; child; child = child.nextSibling) {
			if (child.name === 'LinkReference') {
				const label = child.getChild('LinkLabel');
				const url = child.getChild('URL');
				if (label && url) {
					const key = normalizeLabel(this.slice(label.from, label.to));
					if (key && !this.references.has(key)) {
						this.references.set(key, { url: this.url(url), title: this.title(child) });
					}
				}
			} else if (child.firstChild && !child.name.startsWith('ATXHeading') &&
				child.name !== 'Paragraph' && child.name !== 'Table') {
				this.collectReferences(child);
			}
		}
	}

	/**
	 * @returns the address of a URL node, without its angle brackets and escapes
	 */
	private url(node: SyntaxNode): string {
		return unescape(this.slice(node.from, node.to).replace(/^<(.*)>$/s, '$1'));
	}

	/**
	 * @returns the title of a link, a picture or a reference, null for none
	 */
	private title(node: SyntaxNode): string | null {
		const title = node.getChild('LinkTitle');
		return title ? unescape(this.slice(title.from + 1, title.to - 1)) : null;
	}

	/**
	 * Renders the blocks in a node: those of the text, of a quote or of an item of a list.
	 *
	 * @param tight whether the paragraphs show without their margins, in a tight list
	 */
	private blocks(node: SyntaxNode, parent: Node, tight: boolean): void {
		for (let child = node.firstChild; child; child = child.nextSibling) {
			this.block(child, parent, tight);
		}
	}

	private block(node: SyntaxNode, parent: Node, tight: boolean): void {
		const name = node.name;
		const heading = /^(?:ATX|Setext)Heading(\d)$/.exec(name);
		if (heading) {
			const element = this.blockElement(('h' + heading[1]) as 'h1', parent, node);
			this.inline(node, element, ...this.headingRange(node));
			return;
		}
		switch (name) {
			case 'Paragraph':
				// in a tight list, the item knows the line
				this.inline(node, tight ? parent : this.blockElement('p', parent, node), node.from,
					node.to);
				return;
			case 'Blockquote':
				this.blocks(node, this.blockElement('blockquote', parent, node), false);
				return;
			case 'BulletList':
			case 'OrderedList':
				this.list(node, parent);
				return;
			case 'FencedCode':
			case 'CodeBlock':
				this.code(node, parent);
				return;
			case 'HorizontalRule':
				this.blockElement('hr', parent, node);
				return;
			case 'Table':
				this.table(node, parent);
				return;
			case 'HTMLBlock':
				// shown as written, never as HTML
				this.textNode(this.slice(node.from, node.to),
					this.blockElement('pre', parent, node, MARKDOWN_PREVIEW_CLASSES.html));
				return;
			case 'Task':
				this.task(node, tight ? parent : this.blockElement('p', parent, node));
				return;
			case 'LinkReference':
			case 'CommentBlock':
			case 'ProcessingInstructionBlock':
				return;
			default:
				if (!MARKS.has(name)) {
					this.blocks(node, parent, tight);
				}
		}
	}

	/**
	 * @returns where the text of a heading is, without its marks and the spaces around it
	 */
	private headingRange(node: SyntaxNode): [number, number] {
		let from = node.from;
		let to = node.to;
		const marks = node.getChildren('HeaderMark');
		if (node.name.startsWith('Setext')) {
			to = marks[marks.length - 1]?.from ?? to;
		} else {
			from = marks[0]?.to ?? from;
			const closing = marks.length > 1 ? marks[marks.length - 1] : null;
			if (closing && closing.to === node.to) {
				to = closing.from;
			}
		}
		const text = this.slice(from, to);
		return [from + (text.length - text.trimStart().length), to - (text.length -
			text.trimEnd().length)];
	}

	/**
	 * @returns whether a list is loose: an empty line between its items, or between the blocks of
	 *          one of them
	 */
	private loose(node: SyntaxNode): boolean {
		const blank = /\n[ \t>]*\n/;
		let previous: SyntaxNode | null = null;
		for (let item = node.firstChild; item; item = item.nextSibling) {
			if (previous && blank.test(this.slice(previous.to, item.from))) {
				return true;
			}
			let last: SyntaxNode | null = null;
			for (let child = item.firstChild; child; child = child.nextSibling) {
				if (child.name !== 'ListMark' && last &&
					blank.test(this.slice(last.to, child.from))) {
					return true;
				}
				last = child.name === 'ListMark' ? null : child;
			}
			previous = item;
		}
		return false;
	}

	private list(node: SyntaxNode, parent: Node): void {
		const ordered = node.name === 'OrderedList';
		const list = this.blockElement(ordered ? 'ol' : 'ul', parent, node);
		const tight = !this.loose(node);
		let first = true;
		for (let item = node.firstChild; item; item = item.nextSibling) {
			if (item.name !== 'ListItem') {
				continue;
			}
			const element = this.blockElement('li', list, item);
			const mark = item.getChild('ListMark');
			// the number of the first item is where the list starts
			if (ordered && mark && first) {
				const start = parseInt(this.slice(mark.from, mark.to), 10);
				if (Number.isFinite(start) && start !== 1) {
					(list as HTMLOListElement).start = start;
				}
			}
			first = false;
			if (item.getChild('Task')) {
				element.className = MARKDOWN_PREVIEW_CLASSES.task;
			}
			this.blocks(item, element, tight);
		}
	}

	/**
	 * Renders a task of a list: its check box, checked when done, which the user cannot change,
	 * and its text.
	 */
	private task(node: SyntaxNode, parent: Node): void {
		const marker = node.getChild('TaskMarker');
		const box = this.element('input', parent);
		box.type = 'checkbox';
		box.disabled = true;
		box.checked = marker ? /x/i.test(this.slice(marker.from, marker.to)) : false;
		this.textNode(' ', parent);
		const from = marker ? marker.to : node.from;
		const text = this.slice(from, node.to);
		this.inline(node, parent, from + (text.length - text.trimStart().length), node.to);
	}

	/**
	 * Renders a code block, highlighted by the grammar of its language when the editor knows it.
	 */
	private code(node: SyntaxNode, parent: Node): void {
		const pre = this.blockElement('pre', parent, node, MARKDOWN_PREVIEW_CLASSES.code);
		const code = this.element('code', pre);
		const info = node.getChild('CodeInfo');
		const language = info ? this.slice(info.from, info.to).trim().split(/\s/)[0] : '';
		// the lines of the code, without the marks of the quote or the list they are in
		let text = '';
		let last = -1;
		for (let child = node.firstChild; child; child = child.nextSibling) {
			if (child.name === 'CodeText') {
				if (last >= 0) {
					text += '\n'.repeat((this.slice(last, child.from).match(/\n/g) ?? []).length);
				}
				text += this.slice(child.from, child.to);
				last = child.to;
			}
		}
		text = text.replace(/\n$/, '');
		if (language) {
			code.className = 'language-' + language.replace(/[^\w+#.-]/g, '');
		}
		const grammar = language ? loadedLanguage(language) : null;
		const highlighted = grammar ? languageOf(grammar) : null;
		if (!highlighted) {
			if (grammar === undefined) {
				this.pending.add(language);
			}
			this.textNode(text, code);
			return;
		}
		const tree = highlighted.parser.parse(text);
		highlightCode(text, tree, classHighlighter, (part, classes) => {
			if (classes) {
				this.textNode(part, this.element('span', code, classes));
			} else {
				this.textNode(part, code);
			}
		}, () => this.textNode('\n', code));
	}

	private table(node: SyntaxNode, parent: Node): void {
		const table = this.blockElement('table', parent, node, this.tableClasses);
		const alignments: string[] = [];
		const delimiter = node.getChildren('TableDelimiter')[0];
		if (delimiter) {
			this.slice(delimiter.from, delimiter.to).replace(/^\s*\|/, '').replace(/\|\s*$/, '')
				.split('|').forEach((cell) => {
					const trimmed = cell.trim();
					alignments.push(trimmed.startsWith(':') && trimmed.endsWith(':') ? 'center' :
						trimmed.endsWith(':') ? 'right' : trimmed.startsWith(':') ? 'left' : '');
				});
		}
		const header = node.getChild('TableHeader');
		const columns = Math.max(header ? header.getChildren('TableCell').length : 0, 1);
		// each row a line of the text
		const row = (source: SyntaxNode, section: HTMLTableSectionElement,
			cellName: 'th' | 'td') => {
			const tr = this.blockElement('tr', section, source);
			const cells = source.getChildren('TableCell');
			for (let index = 0; index < columns; index++) {
				const cell = this.element(cellName, tr);
				const alignment = alignments[index];
				if (alignment) {
					cell.className = alignment === 'center' ? MARKDOWN_PREVIEW_CLASSES.alignCenter :
						alignment === 'right' ? MARKDOWN_PREVIEW_CLASSES.alignRight :
							MARKDOWN_PREVIEW_CLASSES.alignLeft;
				}
				const content = cells[index];
				if (content) {
					this.inline(content, cell, content.from, content.to);
				}
			}
		};
		if (header) {
			row(header, this.element('thead', table), 'th');
		}
		const rows = node.getChildren('TableRow');
		if (rows.length) {
			const body = this.element('tbody', table);
			rows.forEach((each) => row(each, body, 'td'));
		}
	}

	/**
	 * Renders the inline content of a node between two positions: its text, and its children as
	 * elements, without the marks of the syntax.
	 */
	private inline(node: SyntaxNode, parent: Node, from: number, to: number): void {
		let position = from;
		for (let child = node.firstChild; child; child = child.nextSibling) {
			if (child.to <= from || child.from >= to) {
				continue;
			}
			if (child.from > position) {
				this.textNode(this.slice(position, child.from), parent);
			}
			this.inlineNode(child, parent);
			position = Math.max(position, child.to);
		}
		if (position < to) {
			this.textNode(this.slice(position, to), parent);
		}
	}

	private inlineNode(node: SyntaxNode, parent: Node): void {
		const name = node.name;
		const element = INLINE_ELEMENTS[name];
		if (element) {
			this.inline(node, this.element(element as 'em', parent), node.from, node.to);
			return;
		}
		switch (name) {
			case 'InlineCode': {
				const marks = node.getChildren('CodeMark');
				let code = this.slice(marks[0]?.to ?? node.from, marks[1]?.from ?? node.to)
					.replace(/\r?\n/g, ' ');
				if (/^ .*[^ ].* $/.test(code)) {
					code = code.slice(1, -1);
				}
				this.textNode(code, this.element('code', parent));
				return;
			}
			case 'Link':
				this.link(node, parent);
				return;
			case 'Image':
				this.image(node, parent);
				return;
			case 'Autolink': {
				const url = node.getChild('URL');
				this.anchor(url ? this.url(url) : '', url ? this.url(url) : '', parent, null);
				return;
			}
			case 'URL': {
				// an address in the text, as GitHub links it
				const url = this.slice(node.from, node.to);
				this.anchor(/^www\./i.test(url) ? 'http://' + url : url, url, parent, null);
				return;
			}
			case 'Escape':
				this.textNode(this.slice(node.from + 1, node.to), parent);
				return;
			case 'Entity':
				this.textNode(decodeEntity(this.slice(node.from, node.to)), parent);
				return;
			case 'HardBreak':
				this.element('br', parent);
				return;
			case 'Comment':
			case 'ProcessingInstruction':
				return;
			default:
				if (MARKS.has(name)) {
					return;
				}
				// HTML, shown as written, and what the preview does not know, as text
				this.inline(node, parent, node.from, node.to);
		}
	}

	/**
	 * @returns where the text of a link or a picture is: between its brackets
	 */
	private linkText(node: SyntaxNode): [number, number] {
		const marks = node.getChildren('LinkMark');
		return [marks[0]?.to ?? node.from, marks[1]?.from ?? node.to];
	}

	/**
	 * @returns the address and the title of a link or a picture, null for a reference that the text
	 *          does not define
	 */
	private target(node: SyntaxNode): Reference | null {
		const url = node.getChild('URL');
		if (url) {
			return { url: this.url(url), title: this.title(node) };
		}
		const label = node.getChild('LinkLabel');
		const [from, to] = this.linkText(node);
		const key = normalizeLabel(label && label.to - label.from > 2 ?
			this.slice(label.from, label.to) : this.slice(from, to));
		return this.references.get(key) ?? (node.getChildren('LinkMark').length > 2 ?
			{ url: '', title: null } : null);
	}

	private link(node: SyntaxNode, parent: Node): void {
		const target = this.target(node);
		const [from, to] = this.linkText(node);
		if (!target) {
			// a reference the text does not define: as written
			this.textNode(this.slice(node.from, from), parent);
			this.inline(node, parent, from, to);
			this.textNode(this.slice(to, node.to), parent);
			return;
		}
		const anchor = this.anchor(target.url, null, parent, target.title);
		this.inline(node, anchor, from, to);
	}

	/**
	 * Adds a link: an a element going to the address when it may be followed, or a span.
	 *
	 * @param text the text of the link, none to leave it empty
	 */
	private anchor(url: string, text: string | null, parent: Node,
		title: string | null): HTMLElement {
		const href = safeUrl(url);
		let element: HTMLElement;
		if (href) {
			const anchor = this.element('a', parent);
			anchor.href = href;
			// the page the preview is in stays, unless the link is to a place in it
			if (!url.trim().startsWith('#')) {
				anchor.target = '_blank';
				anchor.rel = 'noopener noreferrer';
			}
			element = anchor;
		} else {
			element = this.element('span', parent);
		}
		if (title) {
			element.title = title;
		}
		if (text !== null) {
			this.textNode(text, element);
		}
		return element;
	}

	private image(node: SyntaxNode, parent: Node): void {
		const target = this.target(node);
		const [from, to] = this.linkText(node);
		const alt = unescape(this.slice(from, to));
		const src = target ? safeUrl(target.url, true) : null;
		if (!src) {
			this.textNode(alt, parent);
			return;
		}
		// the picture the preview showed before, so that it does not load again
		const shown = this.pictures.get(src)?.shift();
		const image = shown ?? this.document.createElement('img');
		if (!shown) {
			image.src = src;
			image.setAttribute('loading', 'lazy');
		}
		image.alt = alt;
		if (target?.title) {
			image.title = target.title;
		} else {
			image.removeAttribute('title');
		}
		parent.appendChild(image);
	}
}

/**
 * @returns the pictures shown in an element, by their address
 */
function picturesOf(element: Element): Pictures {
	const pictures: Pictures = new Map();
	element.querySelectorAll('img').forEach((image) => {
		const src = image.getAttribute('src') ?? '';
		pictures.set(src, [...pictures.get(src) ?? [], image]);
	});
	return pictures;
}

/**
 * Renders a text into an element, replacing what it has.
 *
 * @returns the renderer: the blocks rendered and the languages whose grammars are not loaded yet
 */
function renderInto(text: string, element: HTMLElement, look: Look): Renderer {
	const renderer = new Renderer(text, element.ownerDocument, picturesOf(element),
		partClasses(look, 'markdown-preview-table'));
	const fragment = element.ownerDocument.createDocumentFragment();
	renderer.render(fragment);
	element.replaceChildren(fragment);
	return renderer;
}

/**
 * Renders a text in Markdown into an element, replacing what it has; the code blocks whose grammar
 * is not loaded yet show plain.
 *
 * @param text the text
 * @param element the element
 * @param look the look of the tables; that of the element when left out (see lookOf)
 * @returns the languages of the code blocks whose grammars are not loaded yet
 */
export function renderMarkdown(text: string, element: HTMLElement,
	look: Look = lookOf(element)): string[] {
	return Array.from(renderInto(text, element, look).pending);
}

/**
 * Renders a text in Markdown into an element, as renderMarkdown() does.
 *
 * @returns the blocks rendered, in the order of the text, with the lines they start at
 */
export function renderMarkdownBlocks(text: string, element: HTMLElement,
	look: Look = lookOf(element)): MarkdownBlock[] {
	return renderInto(text, element, look).rendered;
}

/**
 * @param blocks blocks in the order of the text
 * @returns the index of the last block starting at a line or before it, -1 for none
 */
export function blockAt(blocks: readonly MarkdownBlock[], line: number): number {
	let low = -1;
	let high = blocks.length - 1;
	while (low < high) {
		const middle = (low + high + 1) >> 1;
		if (blocks[middle].line <= line) {
			low = middle;
		} else {
			high = middle - 1;
		}
	}
	return low;
}

/**
 * @param blocks the blocks of the preview, in the order of the text
 * @param height a height in the editor's content
 * @returns the height in the preview's content matching it: as far between the tops of the blocks
 *          around its line as it is between the tops of their lines in the editor; before the
 *          first block, between the starts of the two contents, and after the last, between it
 *          and their ends
 */
export function matchingHeight(blocks: readonly MarkdownBlock[], geometry: StepGeometry,
	height: number): number {
	// a height in the editor and the one matching it in the preview
	const point = (index: number): [number, number] => index < 0 ? [0, 0] :
		index >= blocks.length ? [geometry.editorHeight, geometry.previewHeight] :
			[geometry.lineTop(blocks[index].line), geometry.blockTop(blocks[index].element)];
	const index = blockAt(blocks, geometry.lineAt(height));
	let lower = point(index);
	let upper = point(index + 1);
	if (height < lower[0]) {
		// above the first line, in the padding of the editor
		upper = lower;
		lower = point(-1);
	}
	const span = upper[0] - lower[0];
	const fraction = span > 0 ? Math.min(Math.max((height - lower[0]) / span, 0), 1) : 0;
	return lower[1] + fraction * Math.max(upper[1] - lower[1], 0);
}

/**
 * @returns where in the editor's viewport the preview is anchored, from 0, its top, to 1, its
 *          bottom: at the cursor; with the cursor out of sight, at the edge it went past, turning
 *          to how far the editor is scrolled as the cursor gets a viewport away, so that the start
 *          of the text shows the start of the preview, and its end the end
 */
function anchorOf(editor: StepEditor, contentHeight: number): number {
	const { scrollTop, height, cursor } = editor;
	if (height <= 0) {
		return 0;
	}
	const at = (cursor - scrollTop) / height;
	const scrolled = contentHeight > height ?
		Math.min(Math.max(scrollTop / (contentHeight - height), 0), 1) : 0;
	if (at < 0) {
		return scrolled * Math.min(-at, 1);
	}
	if (at > 1) {
		return 1 - (1 - scrolled) * Math.min(at - 1, 1);
	}
	return at;
}

/**
 * Where the preview scrolls to keep in step with an editor. The part of the preview matching the
 * cursor shows as high in the preview as the cursor does in the editor; with the cursor out of
 * sight, the part matching the top or the bottom line of the editor shows at the top or the bottom
 * of the preview. While the cursor is in sight, the preview scrolls as the cursor moves up or down
 * the editor, in proportion to their heights, and there is no jump as the cursor goes out of sight
 * or comes back.
 *
 * @param blocks the blocks of the preview, in the order of the text
 * @param geometry the sizes of the editor and the preview, and where their lines and blocks are
 * @param editor how far the editor is scrolled, and where its cursor is
 * @param height the height of what the preview shows of its content
 * @returns how far the preview scrolls, in pixels
 */
export function stepScrollTop(blocks: readonly MarkdownBlock[], geometry: StepGeometry,
	editor: StepEditor, height: number): number {
	const anchor = anchorOf(editor, geometry.editorHeight);
	// the preview showing the part matching the editor's top line at its top, or its bottom line
	// at its bottom
	const top = matchingHeight(blocks, geometry, editor.scrollTop);
	const bottom = matchingHeight(blocks, geometry, editor.scrollTop + editor.height) - height;
	const scrollTop = top + anchor * (bottom - top);
	return Math.min(Math.max(scrollTop, 0), Math.max(geometry.previewHeight - height, 0));
}

/**
 * @returns whether an element shows: laid out, not hidden, in a collapsed pane of a split panel
 *          for example
 */
function shown(element: HTMLElement): boolean {
	if (!element.isConnected || element.clientHeight <= 0 || element.clientWidth <= 0) {
		return false;
	}
	const visibility = getComputedStyle(element).visibility;
	return visibility !== 'hidden' && visibility !== 'collapse';
}

/**
 * Calls back a frame later, or in a moment without frames.
 */
function nextFrame(callback: () => void): number {
	return typeof requestAnimationFrame === 'function' ? requestAnimationFrame(callback) :
		window.setTimeout(callback, 16);
}

function cancelNextFrame(frame: number): void {
	(typeof cancelAnimationFrame === 'function' ? cancelAnimationFrame : clearTimeout)(frame);
}

function resolve(target: HTMLElement | string): HTMLElement | null {
	return typeof target === 'string' ? document.getElementById(target) : target;
}

export class MarkdownPreview {
	/** The element the preview renders the text into. */
	readonly element: HTMLElement;

	options: MarkdownPreviewOptions;

	private value: string;

	private readonly look: Look;

	/** How many times the preview rendered, so that a grammar loaded late renders the last text. */
	private renders = 0;

	/** The blocks rendered, in the order of the text, with the lines they start at. */
	private blocks: readonly MarkdownBlock[] = [];

	private frame: number | null = null;

	/** The frame the preview comes in step with the editor in, if it is to. */
	private stepFrame: number | null = null;

	private listeners: Array<[EventTarget, string, EventListener, AddEventListenerOptions?]> = [];

	/** Watches the size of the preview: shown again, or resized, it comes in step. */
	private observer: ResizeObserver | null = null;

	private stopWatchingRemoval: Unsubscribe | null = null;

	/**
	 * @param element the element to render the text into
	 * @param options the text, or the editor whose text the preview shows
	 */
	constructor(element: HTMLElement, options: MarkdownPreviewOptions = {}) {
		this.element = element;
		this.options = { ...options };
		this.look = options.look ?? lookOf(element);
		instances.get(element)?.destroy();
		instances.set(element, this);
		this.value = this.editorValue() ?? options.value ?? '';
		this.decorate();
		this.render();
		this.listen(document, CODE_EDITOR_EVENTS.text, (event) => this.onEditorText(event));
		// the user moved the cursor, typed or scrolled the editor
		this.listen(document, CODE_EDITOR_EVENTS.select, (event) => {
			if (this.follows((event as CustomEvent<{ editor?: CodeEditor }>).detail?.editor)) {
				this.stepLater();
			}
		});
		this.listen(document, 'scroll', (event) => {
			if (this.options.scrollWithEditor &&
				event.target === this.followedEditor()?.view.scrollDOM) {
				this.stepLater();
			}
		}, { capture: true, passive: true });
		if (typeof ResizeObserver === 'function') {
			this.observer = new ResizeObserver(() => this.stepLater());
			this.observer.observe(element);
		}
		this.stopWatchingRemoval = onRemoving(element, () => this.detach());
	}

	/**
	 * Makes a preview of an element, as Wicket does.
	 *
	 * @param element the element or its id
	 * @param options the text, or the editor whose text the preview shows
	 * @returns the preview, null when there is no such element
	 */
	static attach(element: HTMLElement | string,
		options?: MarkdownPreviewOptions): MarkdownPreview | null {
		const resolved = resolve(element);
		return resolved ? new MarkdownPreview(resolved, options) : null;
	}

	/**
	 * @param target the element of a preview or its id
	 * @returns the preview, if there is one
	 */
	static of(target: HTMLElement | string | null | undefined): MarkdownPreview | null {
		const element = typeof target === 'string' ? document.getElementById(target) : target;
		return (element && instances.get(element)) || null;
	}

	/**
	 * @returns the text shown
	 */
	getValue(): string {
		return this.value;
	}

	/**
	 * Shows another text.
	 *
	 * @param value the text, in Markdown
	 */
	setValue(value: string): void {
		this.cancelFrame();
		if (value !== this.value) {
			this.value = value;
			this.render();
		}
	}

	/**
	 * Takes other options; those left out stay as they were.
	 */
	configure(options: MarkdownPreviewOptions = {}): void {
		const defined = Object.fromEntries(Object.entries(options)
			.filter(([, value]) => value !== undefined)) as MarkdownPreviewOptions;
		this.options = { ...this.options, ...defined };
		this.cancelFrame();
		if (!this.options.scrollWithEditor) {
			this.cancelStep();
		}
		this.value = this.editorValue() ?? defined.value ?? this.value;
		this.decorate();
		// and in step at once, when it is to be
		this.render();
	}

	/**
	 * Takes the preview away, leaving the element empty.
	 */
	destroy(): void {
		this.detach();
		const element = this.element;
		element.replaceChildren();
		element.classList.remove(...this.css('markdown-preview').split(' '));
		['role', 'aria-label', 'tabindex'].forEach((name) => element.removeAttribute(name));
		if (!element.classList.length) {
			element.removeAttribute('class');
		}
		if (instances.get(element) === this) {
			instances.delete(element);
		}
	}

	private css(...parts: LookPart[]): string {
		return partClasses(this.look, ...parts);
	}

	private decorate(): void {
		const element = this.element;
		element.classList.add(...this.css('markdown-preview').split(' '));
		const label = this.options.label;
		if (label) {
			element.setAttribute('role', 'region');
			element.setAttribute('aria-label', label);
		} else {
			element.removeAttribute('role');
			element.removeAttribute('aria-label');
		}
		// a long preview scrolls, by the keyboard too
		element.tabIndex = 0;
	}

	/**
	 * @returns the element of the editor the preview follows, null for none
	 */
	private editorElement(): HTMLElement | null {
		const editor = this.options.editor;
		return editor ? resolve(editor) : null;
	}

	/**
	 * @returns the text of the editor the preview follows, undefined without one
	 */
	private editorValue(): string | undefined {
		const element = this.editorElement();
		if (!element) {
			return undefined;
		}
		const editor = CodeEditor.of(element);
		if (editor) {
			return editor.getValue();
		}
		return element instanceof HTMLTextAreaElement ? element.value : undefined;
	}

	/**
	 * @returns the editor the preview follows, null for none or one not made yet
	 */
	private followedEditor(): CodeEditor | null {
		const element = this.editorElement();
		return element ? CodeEditor.of(element) : null;
	}

	/**
	 * @returns whether an editor is the one the preview follows
	 */
	private follows(editor: CodeEditor | undefined): editor is CodeEditor {
		const followed = this.editorElement();
		return !!followed && !!editor && (followed === editor.element || followed === editor.field);
	}

	/**
	 * The text of an editor changed: the preview shows it a frame later, when it is the editor's
	 * it follows.
	 */
	private onEditorText(event: Event): void {
		const editor = (event as CustomEvent<{ editor?: CodeEditor }>).detail?.editor;
		if (!this.follows(editor)) {
			return;
		}
		if (this.frame === null) {
			this.frame = nextFrame(() => {
				this.frame = null;
				this.setValue(editor.getValue());
			});
		}
	}

	private cancelFrame(): void {
		if (this.frame !== null) {
			cancelNextFrame(this.frame);
			this.frame = null;
		}
	}

	/**
	 * Comes in step with the editor a frame later, once the editor has scrolled to its cursor and
	 * the text is rendered; when the preview is to keep in step with an editor.
	 */
	private stepLater(): void {
		if (this.stepFrame === null && this.options.scrollWithEditor && this.options.editor) {
			this.stepFrame = nextFrame(() => {
				this.stepFrame = null;
				this.comeInStep();
			});
		}
	}

	private cancelStep(): void {
		if (this.stepFrame !== null) {
			cancelNextFrame(this.stepFrame);
			this.stepFrame = null;
		}
	}

	/**
	 * Scrolls the preview to the part matching the place in the editor, unless either is hidden.
	 */
	private comeInStep(): void {
		const editor = this.followedEditor();
		const element = this.element;
		if (!editor || !this.options.scrollWithEditor || !shown(element) ||
			!shown(editor.view.scrollDOM)) {
			return;
		}
		const view = editor.view;
		const scroller = view.scrollDOM;
		const doc = view.state.doc;
		// where the contents start on the screen; that of the editor holds its padding above the
		// first line
		const editorTop = scroller.getBoundingClientRect().top + scroller.clientTop -
			scroller.scrollTop;
		const documentTop = view.documentTop - editorTop;
		const previewTop = element.getBoundingClientRect().top + element.clientTop -
			element.scrollTop;
		const geometry: StepGeometry = {
			lineAt: (height) => doc.lineAt(view.lineBlockAtHeight(height - documentTop).from).number,
			// the text rendered may be ahead of the editor's, by a frame
			lineTop: (line) => documentTop + view.lineBlockAt(doc.line(Math.min(Math.max(line, 1),
				doc.lines)).from).top,
			blockTop: (block) => block.getBoundingClientRect().top - previewTop,
			editorHeight: scroller.scrollHeight,
			previewHeight: element.scrollHeight
		};
		const head = view.state.selection.main.head;
		// where the cursor shows, or its line where the editor has not drawn it
		const coords = view.coordsAtPos(head);
		const cursor = coords ? coords.top - editorTop : documentTop + view.lineBlockAt(head).top;
		const scrollTop = stepScrollTop(this.blocks, geometry, { scrollTop: scroller.scrollTop,
			height: scroller.clientHeight, cursor }, element.clientHeight);
		if (Math.abs(scrollTop - element.scrollTop) >= 1) {
			element.scrollTop = scrollTop;
		}
	}

	private render(): void {
		const element = this.element;
		const render = ++this.renders;
		let pending: string[] = [];
		if (this.value.trim()) {
			const renderer = renderInto(this.value, element, this.look);
			pending = Array.from(renderer.pending);
			this.blocks = renderer.rendered;
		} else {
			this.blocks = [];
			element.replaceChildren();
			if (this.options.placeholder) {
				const empty = element.ownerDocument.createElement('p');
				empty.className = MARKDOWN_PREVIEW_CLASSES.empty;
				empty.textContent = this.options.placeholder;
				element.append(empty);
			}
		}
		this.stepLater();
		this.options.onRender?.(element);
		if (pending.length) {
			// the code blocks are highlighted once their grammars are loaded, unless the text
			// changed meanwhile
			Promise.all(pending.map((language) => loadLanguage(language))).then((grammars) => {
				if (grammars.some(Boolean) && render === this.renders &&
					instances.get(element) === this) {
					this.render();
				}
			});
		}
	}

	private listen(target: EventTarget, type: string, listener: (event: Event) => void,
		options?: AddEventListenerOptions): void {
		target.addEventListener(type, listener, options);
		this.listeners.push([target, type, listener, options]);
	}

	/**
	 * Stops following the editor, leaving the elements in place.
	 */
	private detach(): void {
		this.cancelFrame();
		this.cancelStep();
		this.listeners.forEach(([target, type, listener, options]) => {
			target.removeEventListener(type, listener, options);
		});
		this.listeners = [];
		this.observer?.disconnect();
		this.observer = null;
		this.stopWatchingRemoval?.();
		this.stopWatchingRemoval = null;
	}
}

/**
 * Makes a preview of an element, in the shape of a Svelte action: use:markdownPreview={options}.
 *
 * @param element the element to render the text into
 * @param options the text, or the editor whose text the preview shows
 * @returns update() taking other options, destroy() taking the preview away
 */
export function markdownPreview(element: HTMLElement, options?: MarkdownPreviewOptions): {
	update(options?: MarkdownPreviewOptions): void;
	destroy(): void;
} {
	const instance = new MarkdownPreview(element, options);
	return {
		update: (next) => instance.configure(next),
		destroy: () => instance.destroy()
	};
}
