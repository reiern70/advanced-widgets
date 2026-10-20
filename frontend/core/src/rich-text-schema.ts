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
 * What the rich text editor lets a text hold, its features, as the Tiptap extensions making them:
 * paragraphs, headings, the marks of the text, links, lists, quotes, code blocks, rules, tables,
 * images and alignment. A feature left out is not in the editor's schema: pasting it brings its
 * text alone.
 *
 * The HTML the editor writes has no style attribute, which a Content Security Policy without
 * 'unsafe-inline' would block: text aligns by the classes of RICH_TEXT_ALIGN_CLASSES, on
 * paragraphs, headings and the cells of tables, and tables have no widths. The editor reads the
 * alignment of pasted HTML from its style or its align attribute too.
 *
 * Links go only to http, https and mailto addresses, or relative ones (isSafeUrl), as the cleaning
 * of the HTML on the server allows: the editor drops the others, javascript: for example.
 */

import { Extension, mergeAttributes, type AnyExtension } from '@tiptap/core';
import { Image } from '@tiptap/extension-image';
import { TaskItem, TaskList } from '@tiptap/extension-list';
import { Table, TableCell, TableHeader, TableRow } from '@tiptap/extension-table';
import { CharacterCount, Placeholder } from '@tiptap/extensions';
import { Markdown } from '@tiptap/markdown';
import { StarterKit } from '@tiptap/starter-kit';

/** What a text may hold. */
export type RichTextFeature = 'heading' | 'bold' | 'italic' | 'underline' | 'strike' | 'code' |
	'link' | 'bulletList' | 'orderedList' | 'taskList' | 'blockquote' | 'codeBlock' |
	'horizontalRule' | 'table' | 'image' | 'textAlign';

/** The features, all of them: those of an editor of HTML when left out. */
export const RICH_TEXT_FEATURES: readonly RichTextFeature[] = Object.freeze(['heading', 'bold',
	'italic', 'underline', 'strike', 'code', 'link', 'bulletList', 'orderedList', 'taskList',
	'blockquote', 'codeBlock', 'horizontalRule', 'table', 'image', 'textAlign']);

/**
 * The features Markdown has no syntax for, which an editor of Markdown leaves out unless told
 * otherwise: underlined text and alignment.
 */
export const RICH_TEXT_NOT_IN_MARKDOWN: readonly RichTextFeature[] = Object.freeze(['underline',
	'textAlign']);

/** How the editor keeps the text: as HTML, as Markdown or as Tiptap's JSON. */
export type RichTextFormat = 'html' | 'markdown' | 'json';

export const RICH_TEXT_FORMATS: readonly RichTextFormat[] = Object.freeze(['html', 'markdown',
	'json']);

/** How text aligns; left, the start, is the default. */
export type RichTextAlignment = 'left' | 'center' | 'right' | 'justify';

export const RICH_TEXT_ALIGNMENTS: readonly RichTextAlignment[] = Object.freeze(['left', 'center',
	'right', 'justify']);

/** The classes aligning text in the HTML of the editor: none for left, the default. */
export const RICH_TEXT_ALIGN_CLASSES: Readonly<Record<Exclude<RichTextAlignment, 'left'>, string>> =
	Object.freeze({
		center: 'antilia-text-center',
		right: 'antilia-text-right',
		justify: 'antilia-text-justify'
	});

/** The levels of the headings, 1 to 4 when left out. */
export const DEFAULT_HEADING_LEVELS: readonly HeadingLevel[] = Object.freeze([1, 2, 3, 4]);

export type HeadingLevel = 1 | 2 | 3 | 4 | 5 | 6;

/**
 * @returns whether a link or an image may go to an address: http, https, mailto or a relative
 *          one; not javascript:, data: and so on. Spaces and control characters the browser
 *          ignores do not hide a scheme.
 */
export function isSafeUrl(url: string | null | undefined): boolean {
	if (url == null) {
		return false;
	}
	const bare = url.replace(/[\u0000-\u0020\u007f-\u00a0\u1680\u180e\u2000-\u2029\u205f\u3000\ufeff]/g,
		'');
	if (!bare) {
		return false;
	}
	const scheme = /^([a-z][a-z0-9+.-]*):/i.exec(bare);
	return !scheme || ['http', 'https', 'mailto'].includes(scheme[1]!.toLowerCase());
}

/**
 * The attribute the style of pasted HTML moves to (see quietHtml), which the editor reads the
 * alignment of a paragraph from.
 */
export const PASTED_STYLE_ATTRIBUTE = 'data-antilia-style';

/** A style attribute in a tag, not yet moved: after the whole attributes before it, quoted or not. */
const STYLE_IN_TAG = /(<[a-zA-Z][^\s>/]*(?:\s+[^\s"'>/=]+(?:\s*=\s*(?:"[^"]*"|'[^']*'|[^\s"'>]+))?)*?\s+)style(?=\s*=)/;

/**
 * @returns HTML the editor reads without a style attribute: each moves to PASTED_STYLE_ATTRIBUTE.
 *          Under a Content Security Policy without 'unsafe-inline', the browser reports every
 *          style attribute it parses, even out of the page, and reads none: pasted from another
 *          page, the HTML would fill the console. What the styles say the editor has a mark or an
 *          alignment for comes along: bold, italic, underlined and struck text, as the spans of
 *          Google Docs have it, and alignment; the bold wrapper of all Google Docs pastes, a b of a
 *          normal weight, goes.
 */
export function quietHtml(html: string): string {
	if (!STYLE_IN_TAG.test(html) || typeof document === 'undefined') {
		return html;
	}
	let quiet = html;
	while (STYLE_IN_TAG.test(quiet)) {
		quiet = quiet.replace(STYLE_IN_TAG, '$1' + PASTED_STYLE_ATTRIBUTE);
	}
	const template = document.createElement('template');
	template.innerHTML = quiet;
	template.content.querySelectorAll<HTMLElement>(`[${PASTED_STYLE_ATTRIBUTE}]`).forEach(
		(element) => {
			const style = element.getAttribute(PASTED_STYLE_ATTRIBUTE) ?? '';
			const weight = /font-weight\s*:\s*([a-z0-9]+)/i.exec(style)?.[1]?.toLowerCase();
			if (element.tagName === 'B' && (weight === 'normal' || weight === '400')) {
				element.replaceWith(...Array.from(element.childNodes));
				return;
			}
			const wrap = (tag: string) => {
				const inner = document.createElement(tag);
				inner.append(...Array.from(element.childNodes));
				element.append(inner);
			};
			const inline = !/^(P|H[1-6]|LI|UL|OL|TD|TH|TABLE|TR|DIV|BLOCKQUOTE|PRE)$/
				.test(element.tagName);
			if (inline && weight && /^(bold|bolder|[6-9]00)$/.test(weight)) {
				wrap('strong');
			}
			if (inline && /font-style\s*:\s*italic/i.test(style)) {
				wrap('em');
			}
			if (inline && /text-decoration[a-z-]*\s*:[^;]*underline/i.test(style)) {
				wrap('u');
			}
			if (inline && /text-decoration[a-z-]*\s*:[^;]*line-through/i.test(style)) {
				wrap('s');
			}
		});
	return template.innerHTML;
}

/**
 * @returns the alignment of an element: its class, else its style, moved or not, else its align
 *          attribute; null for the default. The style is read as written: the browser may not
 *          parse it, under a Content Security Policy.
 */
function alignmentOf(element: HTMLElement): RichTextAlignment | null {
	for (const [alignment, css] of Object.entries(RICH_TEXT_ALIGN_CLASSES)) {
		if (element.classList.contains(css)) {
			return alignment as RichTextAlignment;
		}
	}
	const style = (element.getAttribute(PASTED_STYLE_ATTRIBUTE) ?? '') + ';' +
		(element.getAttribute('style') ?? '');
	const styled = /(?:^|;)\s*text-align\s*:\s*([a-z]+)/i.exec(style)?.[1];
	const given = (styled || element.getAttribute('align') || '').trim().toLowerCase();
	const alignment = given === 'start' ? 'left' : given === 'end' ? 'right' : given;
	return RICH_TEXT_ALIGNMENTS.includes(alignment as RichTextAlignment) && alignment !== 'left' ?
		alignment as RichTextAlignment : null;
}

/** The attribute of an alignment, written as a class. */
function alignAttribute(name: string) {
	return {
		default: null,
		parseHTML: (element: HTMLElement) => alignmentOf(element),
		renderHTML: (attributes: Record<string, unknown>) => {
			const css = RICH_TEXT_ALIGN_CLASSES[attributes[name] as keyof typeof RICH_TEXT_ALIGN_CLASSES];
			return css ? { class: css } : {};
		}
	};
}

declare module '@tiptap/core' {
	interface Commands<ReturnType> {
		antiliaTextAlign: {
			/** Aligns the paragraphs and headings of the selection. */
			setTextAlign: (alignment: RichTextAlignment) => ReturnType;
			/** Gives the paragraphs and headings of the selection the default alignment. */
			unsetTextAlign: () => ReturnType;
		};
	}
}

/** The node types that align. */
const ALIGNED = ['paragraph', 'heading'];

/**
 * Aligns paragraphs and headings by classes, not by style, with Ctrl+Shift+L, E, R and J.
 */
export const TextAlignClass = Extension.create({
	name: 'antiliaTextAlign',

	addGlobalAttributes() {
		return [{ types: ALIGNED, attributes: { textAlign: alignAttribute('textAlign') } }];
	},

	addCommands() {
		return {
			setTextAlign: (alignment: RichTextAlignment) => ({ commands }) => {
				if (!RICH_TEXT_ALIGNMENTS.includes(alignment)) {
					return false;
				}
				const value = alignment === 'left' ? null : alignment;
				return ALIGNED.map((type) => commands.updateAttributes(type, { textAlign: value }))
					.some(Boolean);
			},
			unsetTextAlign: () => ({ commands }) => ALIGNED.map((type) =>
				commands.resetAttributes(type, 'textAlign')).some(Boolean)
		};
	},

	addKeyboardShortcuts() {
		return {
			'Mod-Shift-l': () => this.editor.commands.setTextAlign('left'),
			'Mod-Shift-e': () => this.editor.commands.setTextAlign('center'),
			'Mod-Shift-r': () => this.editor.commands.setTextAlign('right'),
			'Mod-Shift-j': () => this.editor.commands.setTextAlign('justify')
		};
	}
});

/** A table without widths: no style on the table, no columns. */
const PlainTable = Table.extend({
	renderHTML({ HTMLAttributes }) {
		const { style: _style, ...attributes } = HTMLAttributes as Record<string, unknown>;
		return ['table', mergeAttributes(this.options.HTMLAttributes, attributes), ['tbody', 0]];
	}
});

/** Images of safe addresses only: no data:, no javascript:. */
const SafeImage = Image.extend({
	parseHTML() {
		return [{ tag: 'img[src]', getAttrs: (element: HTMLElement) =>
			isSafeUrl(element.getAttribute('src')) ? null : false }];
	}
});

/** Cells aligned by classes. */
const PlainCell = TableCell.extend({
	addAttributes() {
		return { ...this.parent?.(), align: alignAttribute('align') };
	}
});

const PlainHeader = TableHeader.extend({
	addAttributes() {
		return { ...this.parent?.(), align: alignAttribute('align') };
	}
});

/** What makes the schema of an editor. */
export interface RichTextSchemaOptions {
	features: ReadonlySet<RichTextFeature>;
	format: RichTextFormat;
	headingLevels: readonly HeadingLevel[];
	/** The text shown while the editor is empty, read each time it shows. */
	placeholder: () => string;
	/** The most characters the text may have, none for null. */
	limit: number | null;
}

/**
 * @returns the extensions of an editor with the features asked for
 */
export function schemaExtensions(options: RichTextSchemaOptions): AnyExtension[] {
	const has = (feature: RichTextFeature) => options.features.has(feature);
	const on = <T extends object>(feature: RichTextFeature, settings = {} as T): T | false =>
		has(feature) ? settings : false;
	const lists = has('bulletList') || has('orderedList');
	const extensions: AnyExtension[] = [
		StarterKit.configure({
			heading: has('heading') && options.headingLevels.length ?
				{ levels: [...options.headingLevels] } : false,
			bold: on('bold'),
			italic: on('italic'),
			underline: on('underline'),
			strike: on('strike'),
			code: on('code'),
			link: on('link', {
				openOnClick: false,
				autolink: true,
				linkOnPaste: true,
				defaultProtocol: 'https',
				HTMLAttributes: { target: null, rel: 'noopener noreferrer nofollow', class: null },
				isAllowedUri: (url: string) => isSafeUrl(url)
			}),
			bulletList: on('bulletList'),
			orderedList: on('orderedList'),
			listItem: lists ? {} : false,
			listKeymap: lists ? {} : false,
			blockquote: on('blockquote'),
			codeBlock: on('codeBlock', { languageClassPrefix: 'language-', defaultLanguage: null }),
			horizontalRule: on('horizontalRule'),
			dropcursor: { class: 'antilia-rich-text-drop-cursor', color: false, width: 2 },
			trailingNode: false
		}),
		Placeholder.configure({ placeholder: () => options.placeholder() }),
		CharacterCount.configure({ limit: options.limit })
	];
	if (has('taskList')) {
		extensions.push(TaskList, TaskItem.configure({ nested: true }));
	}
	if (has('table')) {
		extensions.push(PlainTable.configure({ resizable: false }), TableRow, PlainHeader,
			PlainCell);
	}
	if (has('image')) {
		extensions.push(SafeImage.configure({ inline: false, allowBase64: false }));
	}
	if (has('textAlign')) {
		extensions.push(TextAlignClass);
	}
	if (options.format === 'markdown') {
		extensions.push(Markdown);
	}
	return extensions;
}
