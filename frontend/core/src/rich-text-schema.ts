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
 * of two palettes apart: <span class="antilia-color-red"> colors the text, by a color of the text
 * (RichTextColor), of DEFAULT_RICH_TEXT_COLORS unless the page has its own, and
 * <mark class="antilia-background-yellow"> its background, by a color of the background
 * (RichTextBackgroundColor), of DEFAULT_RICH_TEXT_BACKGROUND_COLORS unless the page has its own.
 * rich-text-editor.css colors those of the default palettes from the theme, the page's style
 * sheets its own.
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

import type { RichTextEditorKit } from './rich-text-editor.js';
import { featureExtensions, richTextFeature } from './rich-text-features.js';
import { isSafeUrl, PASTED_STYLE_ATTRIBUTE, quietHtml, RICH_TEXT_ALIGN_CLASSES, RICH_TEXT_ALIGNMENTS,
	type RichTextAlignment } from './rich-text-html.js';

export { isSafeUrl, PASTED_STYLE_ATTRIBUTE, quietHtml, RICH_TEXT_ALIGN_CLASSES, RICH_TEXT_ALIGNMENTS,
	type RichTextAlignment };

/** The features built in the editor. */
export type RichTextFeatureName = 'heading' | 'bold' | 'italic' | 'underline' | 'strike' | 'code' |
	'textColor' | 'textBackground' | 'link' | 'bulletList' | 'orderedList' | 'taskList' |
	'blockquote' | 'codeBlock' | 'horizontalRule' | 'table' | 'image' | 'textAlign';

/**
 * What a text may hold: a feature built in the editor, or the name of one registered
 * (registerRichTextFeature).
 */
export type RichTextFeature = RichTextFeatureName | (string & {});

/** The features built in the editor, all of them: those of an editor of HTML when left out. */
export const RICH_TEXT_FEATURES: readonly RichTextFeatureName[] = Object.freeze(['heading', 'bold',
	'italic', 'underline', 'strike', 'code', 'textColor', 'textBackground', 'link', 'bulletList',
	'orderedList', 'taskList', 'blockquote', 'codeBlock', 'horizontalRule', 'table', 'image',
	'textAlign']);

/**
 * The features Markdown has no syntax for, which an editor of Markdown leaves out unless told
 * otherwise: underlined text, colors and alignment.
 */
export const RICH_TEXT_NOT_IN_MARKDOWN: readonly RichTextFeatureName[] = Object.freeze(['underline',
	'textColor', 'textBackground', 'textAlign']);

/**
 * A color of the text, of the palette of the colors of the text. The style sheets of the page color
 * its class; the cleaning of the HTML on the server keeps those of the palette alone.
 */
export interface RichTextColor {
	/** Its name, unique in the palette: what Tiptap's JSON keeps of it. */
	name: string;
	/** The class coloring the text, on a <span>. */
	colorClass: string;
	/**
	 * What the menu calls it; when left out, the text of the editor named color and its name
	 * capitalized, colorRed for red, else its name.
	 */
	label?: string;
}

/**
 * A color of the background of text, of a palette of its own, apart from the colors of the text:
 * a text takes one of each, or either. The style sheets of the page color its class; the cleaning
 * of the HTML on the server keeps those of the palette alone.
 */
export interface RichTextBackgroundColor {
	/** Its name, unique in the palette: what Tiptap's JSON keeps of it. */
	name: string;
	/** The class coloring the background of the text, on a <mark>. */
	backgroundClass: string;
	/**
	 * What the menu calls it; when left out, the text of the editor named color and its name
	 * capitalized, colorRed for red, else its name.
	 */
	label?: string;
}

/** The names of the colors of DEFAULT_RICH_TEXT_COLORS, those of RichTextColor in Wicket. */
export type RichTextColorName = 'gray' | 'brown' | 'red' | 'orange' | 'yellow' | 'green' | 'teal' |
	'cyan' | 'blue' | 'indigo' | 'purple' | 'pink';

/**
 * The names of the colors of DEFAULT_RICH_TEXT_BACKGROUND_COLORS, those of RichTextBackgroundColor
 * in Wicket.
 */
export type RichTextBackgroundColorName = 'gray' | 'brown' | 'red' | 'orange' | 'yellow' | 'lime' |
	'green' | 'teal' | 'cyan' | 'blue' | 'purple' | 'pink';

/** The start of the class coloring text, antilia-color-red for example. */
export const RICH_TEXT_COLOR_CLASS = 'antilia-color-';

/** The start of the class coloring the background of text, antilia-background-yellow. */
export const RICH_TEXT_BACKGROUND_CLASS = 'antilia-background-';

/** The colors of the text when left out, those rich-text-editor.css colors from the theme. */
export const DEFAULT_RICH_TEXT_COLORS: readonly RichTextColor[] = Object.freeze((['gray', 'brown',
	'red', 'orange', 'yellow', 'green', 'teal', 'cyan', 'blue', 'indigo', 'purple',
	'pink'] satisfies RichTextColorName[]).map((name) => Object.freeze({ name,
	colorClass: RICH_TEXT_COLOR_CLASS + name })));

/** The colors of the background when left out, those rich-text-editor.css colors from the theme. */
export const DEFAULT_RICH_TEXT_BACKGROUND_COLORS: readonly RichTextBackgroundColor[] =
	Object.freeze((['gray', 'brown', 'red', 'orange', 'yellow', 'lime', 'green', 'teal', 'cyan',
		'blue', 'purple', 'pink'] satisfies RichTextBackgroundColorName[]).map((name) =>
		Object.freeze({ name, backgroundClass: RICH_TEXT_BACKGROUND_CLASS + name })));

/** What a name of a color may be, as on the server: letters, digits, - and _, from a letter. */
const COLOR_NAME = /^[A-Za-z][A-Za-z0-9_-]{0,39}$/;

/** What a class of a color may be: one CSS class, without escapes. */
const COLOR_CLASS = /^-?[_A-Za-z][_A-Za-z0-9-]{0,99}$/;

/**
 * @returns the colors of a palette the editor takes, the defaults when left out, none for null:
 *          those with a name and a class it allows, each name and each class once, the first of
 *          them
 */
function palette<C extends { name: string }>(colors: readonly C[] | null | undefined,
	classOf: (color: C) => unknown, defaults: readonly C[]): readonly C[] {
	if (colors === undefined) {
		return defaults;
	}
	if (!colors) {
		return [];
	}
	const names = new Set<string>();
	const classes = new Set<string>();
	return colors.filter((color) => {
		const css = color ? classOf(color) : undefined;
		const fits = !!color && typeof color.name === 'string' && COLOR_NAME.test(color.name) &&
			!names.has(color.name) && typeof css === 'string' && COLOR_CLASS.test(css) &&
			!classes.has(css);
		if (fits) {
			names.add(color.name);
			classes.add(css);
		}
		return fits;
	});
}

/**
 * @returns the colors of the text the editor takes of a palette, DEFAULT_RICH_TEXT_COLORS when left
 *          out, none for null: those with a name and a class it allows, each name and each class
 *          once, the first of them
 */
export function richTextColors(colors: readonly RichTextColor[] | null | undefined):
	readonly RichTextColor[] {
	return palette(colors, (color) => color.colorClass, DEFAULT_RICH_TEXT_COLORS);
}

/**
 * @returns the colors of the background the editor takes of a palette,
 *          DEFAULT_RICH_TEXT_BACKGROUND_COLORS when left out, none for null, checked as
 *          richTextColors() checks those of the text
 */
export function richTextBackgroundColors(
	colors: readonly RichTextBackgroundColor[] | null | undefined):
	readonly RichTextBackgroundColor[] {
	return palette(colors, (color) => color.backgroundClass, DEFAULT_RICH_TEXT_BACKGROUND_COLORS);
}

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
			setTextColor: (color: string) => ReturnType;
			/** Gives the text selected its default color. */
			unsetTextColor: () => ReturnType;
		};
		textBackground: {
			/** Colors the background of the text selected, or of the text typed next. */
			setTextBackground: (color: string) => ReturnType;
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
 * @returns a mark coloring text by the classes of the colors of a palette, written in a tag: a
 *          span for the color of the text, a mark for its background. A tag of the mark without a
 *          class of the palette takes the color given, if any: a pasted <mark> is yellow, unless an
 *          extension of the page reads <mark> its own way.
 */
function colorMark<C extends { name: string }>(name: 'textColor' | 'textBackground',
	tag: string, classOf: (color: C) => string, colors: readonly C[], untagged: string | null) {
	const named = (value: unknown) => colors.find((color) => color.name === value);
	const colorOf = (element: HTMLElement) => colors.find((color) =>
		element.classList.contains(classOf(color)))?.name ?? null;
	return Mark.create({
		name,

		addAttributes() {
			return {
				color: {
					default: null,
					parseHTML: (element: HTMLElement) => colorOf(element) ?? untagged,
					renderHTML: (attributes: Record<string, unknown>) => {
						const color = named(attributes.color);
						return color ? { class: classOf(color) } : {};
					}
				}
			};
		},

		parseHTML() {
			const classed = { tag, getAttrs: (element: HTMLElement) =>
				colorOf(element) ? null : false };
			// below the default priority, 50, of the rules of other extensions
			return untagged ? [classed, { tag, priority: 40 }] : [classed];
		},

		renderHTML({ HTMLAttributes }) {
			return [tag, mergeAttributes(HTMLAttributes), 0];
		},

		addCommands() {
			const set = (color: string) => ({ commands }: CommandProps) =>
				!!named(color) && commands.setMark(name, { color });
			const unset = () => ({ commands }: CommandProps) => commands.unsetMark(name);
			return name === 'textColor' ? { setTextColor: set, unsetTextColor: unset } :
				{ setTextBackground: set, unsetTextBackground: unset };
		}
	});
}

/** The color of text, of a palette: <span class="antilia-color-red">. */
export function textColorMark(colors: readonly RichTextColor[]) {
	return colorMark('textColor', 'span', (color) => color.colorClass, colors, null);
}

/**
 * The background of text, of a palette: <mark class="antilia-background-yellow">. A pasted <mark>
 * without a class of the palette takes the color named yellow, if the palette has one.
 */
export function textBackgroundMark(colors: readonly RichTextBackgroundColor[]) {
	return colorMark('textBackground', 'mark', (color) => color.backgroundClass, colors,
		colors.some((color) => color.name === 'yellow') ? 'yellow' : null);
}

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
	/** The features: those built in, and those registered (registerRichTextFeature). */
	features: ReadonlySet<RichTextFeature>;
	format: RichTextFormat;
	headingLevels: readonly HeadingLevel[];
	/** The text shown while the editor is empty, read each time it shows. */
	placeholder: () => string;
	/** The most characters the text may have, none for null. */
	limit: number | null;
	/** The palette of the colors of the text, checked (richTextColors). */
	colors: readonly RichTextColor[];
	/** The palette of the colors of the background, checked (richTextBackgroundColors). */
	backgroundColors: readonly RichTextBackgroundColor[];
	/** The classes of Tiptap the features registered are made with. */
	kit: RichTextEditorKit;
	/** The options of the features registered, by their names. */
	featureOptions: Readonly<Record<string, Readonly<Record<string, unknown>>>>;
	/** Tells what failed. */
	log: (message: string, error: unknown) => void;
}

/**
 * @returns the extensions of an editor with the features asked for
 */
export function schemaExtensions(options: RichTextSchemaOptions): AnyExtension[] {
	// a feature registered under a built-in name replaces it
	const has = (feature: RichTextFeatureName) => options.features.has(feature) &&
		!richTextFeature(feature);
	const on = <T extends object>(feature: RichTextFeatureName, settings = {} as T): T | false =>
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
		extensions.push(textColorMark(options.colors));
	}
	if (has('textBackground')) {
		extensions.push(textBackgroundMark(options.backgroundColors));
	}
	if (has('textAlign')) {
		extensions.push(TextAlignClass);
	}
	for (const feature of options.features) {
		const definition = richTextFeature(feature);
		if (definition) {
			extensions.push(...featureExtensions(definition, { kit: options.kit,
				format: options.format, features: options.features,
				headingLevels: options.headingLevels,
				options: options.featureOptions[feature] ?? {} }, options.log));
		}
	}
	if (options.format === 'markdown') {
		extensions.push(Markdown);
	}
	return extensions;
}
