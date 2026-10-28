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

/*
 * What the rich text editor lets a text hold, its features, as the Tiptap extensions making them:
 * paragraphs, headings, the marks of the text, its colors, links, lists, quotes, code blocks,
 * rules, tables, images and alignment. A feature left out is not in the editor's schema: pasting
 * it brings its text alone.
 *
 * The HTML the editor writes has no style attribute, which a Content Security Policy without
 * 'unsafe-inline' would block: text aligns by the classes of RICH_TEXT_ALIGN_CLASSES, on
 * paragraphs, headings and the cells of tables, and tables have no widths. The editor reads the
 * alignment of pasted HTML from its style or its align attribute too. Colors are classes as well,
 * of a palette (RICH_TEXT_COLORS): <span class="antilia-color-red"> colors the text,
 * <mark class="antilia-background-yellow"> its background; rich-text-editor.css colors them from
 * the theme.
 *
 * Links go only to http, https and mailto addresses, or relative ones (isSafeUrl), as the cleaning
 * of the HTML on the server allows: the editor drops the others, javascript: for example.
 */

import { Extension, Mark, mergeAttributes, type AnyExtension,
	type CommandProps } from '@tiptap/core';
import { Image } from '@tiptap/extension-image';
import { TaskItem, TaskList } from '@tiptap/extension-list';
import { Table, TableCell, TableHeader, TableRow } from '@tiptap/extension-table';
import { CharacterCount, Placeholder } from '@tiptap/extensions';
import { Markdown } from '@tiptap/markdown';
import { StarterKit } from '@tiptap/starter-kit';

import { isSafeUrl, PASTED_STYLE_ATTRIBUTE, quietHtml, RICH_TEXT_ALIGN_CLASSES, RICH_TEXT_ALIGNMENTS,
	type RichTextAlignment } from './rich-text-html.js';

export { isSafeUrl, PASTED_STYLE_ATTRIBUTE, quietHtml, RICH_TEXT_ALIGN_CLASSES, RICH_TEXT_ALIGNMENTS,
	type RichTextAlignment };

/** What a text may hold. */
export type RichTextFeature = 'heading' | 'bold' | 'italic' | 'underline' | 'strike' | 'code' |
	'textColor' | 'textBackground' | 'link' | 'bulletList' | 'orderedList' | 'taskList' |
	'blockquote' | 'codeBlock' | 'horizontalRule' | 'table' | 'image' | 'textAlign';

/** The features, all of them: those of an editor of HTML when left out. */
export const RICH_TEXT_FEATURES: readonly RichTextFeature[] = Object.freeze(['heading', 'bold',
	'italic', 'underline', 'strike', 'code', 'textColor', 'textBackground', 'link', 'bulletList',
	'orderedList', 'taskList', 'blockquote', 'codeBlock', 'horizontalRule', 'table', 'image',
	'textAlign']);

/**
 * The features Markdown has no syntax for, which an editor of Markdown leaves out unless told
 * otherwise: underlined text, colors and alignment.
 */
export const RICH_TEXT_NOT_IN_MARKDOWN: readonly RichTextFeature[] = Object.freeze(['underline',
	'textColor', 'textBackground', 'textAlign']);

/** The colors of the text and of its background, a palette the theme colors. */
export type RichTextColor = 'gray' | 'red' | 'orange' | 'yellow' | 'green' | 'blue' | 'purple' |
	'pink';

export const RICH_TEXT_COLORS: readonly RichTextColor[] = Object.freeze(['gray', 'red', 'orange',
	'yellow', 'green', 'blue', 'purple', 'pink']);

/** The start of the class coloring text, antilia-color-red for example. */
export const RICH_TEXT_COLOR_CLASS = 'antilia-color-';

/** The start of the class coloring the background of text, antilia-background-yellow. */
export const RICH_TEXT_BACKGROUND_CLASS = 'antilia-background-';

/** How the editor keeps the text: as HTML, as Markdown or as Tiptap's JSON. */
export type RichTextFormat = 'html' | 'markdown' | 'json';

export const RICH_TEXT_FORMATS: readonly RichTextFormat[] = Object.freeze(['html', 'markdown',
	'json']);

/** The levels of the headings, 1 to 4 when left out. */
export const DEFAULT_HEADING_LEVELS: readonly HeadingLevel[] = Object.freeze([1, 2, 3, 4]);

export type HeadingLevel = 1 | 2 | 3 | 4 | 5 | 6;

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
		textColor: {
			/** Colors the text selected, or the text typed next, in a color of the palette. */
			setTextColor: (color: RichTextColor) => ReturnType;
			/** Gives the text selected its default color. */
			unsetTextColor: () => ReturnType;
		};
		textBackground: {
			/** Colors the background of the text selected, or of the text typed next. */
			setTextBackground: (color: RichTextColor) => ReturnType;
			/** Takes away the background of the text selected. */
			unsetTextBackground: () => ReturnType;
		};
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

/**
 * @returns the color of the palette a class of an element names, null for none
 */
function colorOf(element: HTMLElement, prefix: string): RichTextColor | null {
	return RICH_TEXT_COLORS.find((color) => element.classList.contains(prefix + color)) ?? null;
}

/**
 * @returns a mark coloring text by the classes of a prefix, written in a tag: a span for the
 *          color of the text, a mark for its background. A tag of the mark without a class of the
 *          palette takes the color given, if any: a pasted <mark> is yellow, unless an extension
 *          of the page reads <mark> its own way.
 */
function colorMark(name: 'textColor' | 'textBackground', tag: string, prefix: string,
	untagged: RichTextColor | null) {
	return Mark.create({
		name,

		addAttributes() {
			return {
				color: {
					default: null,
					parseHTML: (element: HTMLElement) => colorOf(element, prefix) ?? untagged,
					renderHTML: (attributes: Record<string, unknown>) =>
						RICH_TEXT_COLORS.includes(attributes.color as RichTextColor) ?
							{ class: prefix + attributes.color } : {}
				}
			};
		},

		parseHTML() {
			const classed = { tag, getAttrs: (element: HTMLElement) =>
				colorOf(element, prefix) ? null : false };
			// below the default priority, 50, of the rules of other extensions
			return untagged ? [classed, { tag, priority: 40 }] : [classed];
		},

		renderHTML({ HTMLAttributes }) {
			return [tag, mergeAttributes(HTMLAttributes), 0];
		},

		addCommands() {
			const set = (color: RichTextColor) => ({ commands }: CommandProps) =>
				RICH_TEXT_COLORS.includes(color) && commands.setMark(name, { color });
			const unset = () => ({ commands }: CommandProps) => commands.unsetMark(name);
			return name === 'textColor' ? { setTextColor: set, unsetTextColor: unset } :
				{ setTextBackground: set, unsetTextBackground: unset };
		}
	});
}

/** The color of text: <span class="antilia-color-red">. */
export const TextColorClass = colorMark('textColor', 'span', RICH_TEXT_COLOR_CLASS, null);

/** The background of text: <mark class="antilia-background-yellow">. */
export const TextBackgroundClass = colorMark('textBackground', 'mark', RICH_TEXT_BACKGROUND_CLASS,
	'yellow');

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
	if (has('textColor')) {
		extensions.push(TextColorClass);
	}
	if (has('textBackground')) {
		extensions.push(TextBackgroundClass);
	}
	if (has('textAlign')) {
		extensions.push(TextAlignClass);
	}
	if (options.format === 'markdown') {
		extensions.push(Markdown);
	}
	return extensions;
}
