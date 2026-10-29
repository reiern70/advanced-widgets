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
 * The toolbar of the rich text editor: its tools in groups, as the page lists them, each a button
 * in the look of the page. Tiptap draws no toolbar of its own.
 *
 * A WAI-ARIA toolbar: it is one stop of Tab, the arrow keys move between its tools, Home and End
 * go to the first and the last one. A tool that formats shows whether its format is on with
 * aria-pressed; a tool that cannot act where the cursor is says so with aria-disabled and stays
 * reachable. Pressing a tool keeps the focus and the selection in the text.
 *
 * The text style (paragraph, headings) and the table open a menu of their own, a WAI-ARIA menu:
 * the arrow keys go through its items, Enter or a click chooses one, Escape closes it. The color
 * of the text and its background open a menu too, a color picker: the default (none for the
 * background) on a line of its own, then the colors of their palette as squares in a grid, then
 * the last colors chosen (recentRichTextColors); the color where the cursor is has a tick, and the
 * arrow keys move through the grid by columns and by rows. A bar under their icon shows the color
 * where the cursor is, by the class of the color: that of the text, or that of the background. So
 * does the code block open a menu when the editor has languages for it: the menu makes the block a
 * paragraph again, or a block in plain text or in one of the languages. Without them, the tool
 * makes a code block, or takes it away. The link and the image open a form in a popover: the
 * address, and whether the link opens in a new tab or the text in place of the image. An address
 * must be safe (see isSafeUrl): http, https, mailto or a relative one. While the page uploads
 * images, the form of the image has a button choosing a file to upload, whose address the image
 * then takes.
 *
 * The same class draws the bubble menu over a selection, with the tools of the inline formats.
 */

import type { Editor } from '@tiptap/core';

import { lookClasses, partClasses, toggleParts, type Look, type LookPart } from './look.js';
import { Popover } from './popover.js';
import { codeLanguage } from './rich-text-languages.js';
import { DEFAULT_RICH_TEXT_BACKGROUND_COLORS, DEFAULT_RICH_TEXT_COLORS, isSafeUrl,
	type HeadingLevel, type RichTextAlignment, type RichTextBackgroundColor, type RichTextColor,
	type RichTextFeature } from './rich-text-schema.js';
import { registeredTool, type RichTextMenuItem, type RichTextToolContext,
	type RichTextTrigger } from './rich-text-features.js';
import type { UploadHost } from './rich-text-upload.js';

/** The tools built in the toolbar. */
export type RichTextToolName = 'heading' | 'bold' | 'italic' | 'underline' | 'strike' | 'code' |
	'superscript' | 'subscript' | 'textColor' | 'textBackground' | 'link' | 'bulletList' |
	'orderedList' | 'taskList' | 'blockquote' | 'codeBlock' | 'horizontalRule' | 'table' | 'image' |
	'alignLeft' | 'alignCenter' | 'alignRight' | 'alignJustify' | 'undo' | 'redo' |
	'clearFormatting';

/**
 * A tool of the toolbar: a built-in one, or one of a feature registered
 * (registerRichTextFeature), by its name.
 */
export type RichTextTool = RichTextToolName | (string & {});

/** The built-in tools, in the order of the toolbar by default. */
export const RICH_TEXT_TOOLS: readonly RichTextToolName[] = Object.freeze(['heading', 'bold', 'italic',
	'underline', 'strike', 'code', 'superscript', 'subscript', 'textColor', 'textBackground', 'link',
	'bulletList', 'orderedList', 'taskList', 'blockquote', 'codeBlock', 'horizontalRule', 'table',
	'image', 'alignLeft', 'alignCenter', 'alignRight', 'alignJustify', 'clearFormatting', 'undo',
	'redo']);

/** The toolbar when the page gives none: the tools in groups. */
export const DEFAULT_RICH_TEXT_TOOLBAR: readonly (readonly RichTextToolName[])[] = Object.freeze([
	['heading'],
	['bold', 'italic', 'underline', 'strike', 'code', 'superscript', 'subscript'],
	['textColor', 'textBackground'],
	['link'],
	['bulletList', 'orderedList', 'taskList'],
	['blockquote', 'codeBlock', 'horizontalRule'],
	['table', 'image'],
	['alignLeft', 'alignCenter', 'alignRight', 'alignJustify'],
	['clearFormatting'],
	['undo', 'redo']
].map((group) => Object.freeze(group as RichTextToolName[])));

/** The tools of the bubble menu over a selection. */
export const RICH_TEXT_BUBBLE_TOOLS: readonly RichTextToolName[] = Object.freeze(['bold', 'italic',
	'underline', 'strike', 'code', 'superscript', 'subscript', 'link']);

/** What the editor says, in the language of the page. */
export interface RichTextEditorTexts {
	/** The accessible name of the toolbar and of the bubble menu. */
	toolbar: string;
	bubble: string;
	/** The tools, by their names. */
	heading: string;
	bold: string;
	italic: string;
	underline: string;
	strike: string;
	code: string;
	superscript: string;
	subscript: string;
	textColor: string;
	textBackground: string;
	link: string;
	bulletList: string;
	orderedList: string;
	taskList: string;
	blockquote: string;
	codeBlock: string;
	horizontalRule: string;
	table: string;
	image: string;
	alignLeft: string;
	alignCenter: string;
	alignRight: string;
	alignJustify: string;
	clearFormatting: string;
	undo: string;
	redo: string;
	/** The styles of the menu of the tool heading; {0} stands for the level. */
	paragraph: string;
	headingLevel: string;
	/** The items of the menu of the table. */
	insertTable: string;
	addRowBefore: string;
	addRowAfter: string;
	addColumnBefore: string;
	addColumnAfter: string;
	deleteRow: string;
	deleteColumn: string;
	toggleHeaderRow: string;
	deleteTable: string;
	/** The form of a link. */
	linkUrl: string;
	linkNewTab: string;
	removeLink: string;
	/** The form of an image. */
	imageUrl: string;
	imageAlt: string;
	apply: string;
	close: string;
	/** Said of an address the editor does not take. */
	invalidUrl: string;
	/** The item of the menu of the code block for a block without a language. */
	plainText: string;
	/** The first items of the menus of the colors: the default color, no background. */
	defaultColor: string;
	noBackground: string;
	/** The label of the last colors chosen, under the grid of the colors. */
	recentColors: string;
	/** The colors of the default palettes, unless their palette names them (RichTextColor). */
	colorGray: string;
	colorBrown: string;
	colorRed: string;
	colorOrange: string;
	colorYellow: string;
	colorLime: string;
	colorGreen: string;
	colorTeal: string;
	colorCyan: string;
	colorBlue: string;
	colorIndigo: string;
	colorPurple: string;
	colorPink: string;
	/** The button of the form of an image choosing a file to upload. */
	uploadImage: string;
	/** The mark of an image uploading; {0} the name of its file. */
	uploading: string;
	/** What stands for the name of a file that has none. */
	theImage: string;
	/** Why an image was not uploaded: {0} the name of its file, {1} the largest size, the types. */
	uploadFailed: string;
	uploadTooLarge: string;
	uploadType: string;
	/** The count: {0} the characters, {1} the limit, {0} the words; one of them alone. */
	characters: string;
	character: string;
	charactersOfLimit: string;
	words: string;
	word: string;
	/** The accessible name of the handle resizing the editor. */
	resize: string;
	/** The popover of an issue a proofreader found: its title by its kind, and what it offers. */
	proofreadSpelling: string;
	proofreadGrammar: string;
	proofreadStyle: string;
	proofreadReplacements: string;
	proofreadNoSuggestions: string;
	proofreadIgnore: string;
	/** The list of suggestions: its accessible name, while asking, and when nothing matches. */
	suggestions: string;
	suggestLoading: string;
	suggestNone: string;
}

export const DEFAULT_RICH_TEXT_EDITOR_TEXTS: Readonly<RichTextEditorTexts> = Object.freeze({
	toolbar: 'Formatting',
	bubble: 'Formatting of the selection',
	heading: 'Text style',
	bold: 'Bold',
	italic: 'Italic',
	underline: 'Underline',
	strike: 'Strikethrough',
	code: 'Code',
	superscript: 'Superscript',
	subscript: 'Subscript',
	textColor: 'Text color',
	textBackground: 'Background color',
	link: 'Link',
	bulletList: 'Bulleted list',
	orderedList: 'Numbered list',
	taskList: 'Task list',
	blockquote: 'Quote',
	codeBlock: 'Code block',
	horizontalRule: 'Horizontal line',
	table: 'Table',
	image: 'Image',
	alignLeft: 'Align left',
	alignCenter: 'Center',
	alignRight: 'Align right',
	alignJustify: 'Justify',
	clearFormatting: 'Clear formatting',
	undo: 'Undo',
	redo: 'Redo',
	paragraph: 'Paragraph',
	headingLevel: 'Heading {0}',
	insertTable: 'Insert a table',
	addRowBefore: 'Add a row above',
	addRowAfter: 'Add a row below',
	addColumnBefore: 'Add a column before',
	addColumnAfter: 'Add a column after',
	deleteRow: 'Delete the row',
	deleteColumn: 'Delete the column',
	toggleHeaderRow: 'Header row',
	deleteTable: 'Delete the table',
	linkUrl: 'Address',
	linkNewTab: 'Open in a new tab',
	removeLink: 'Remove the link',
	imageUrl: 'Address of the image',
	imageAlt: 'Text in place of the image',
	apply: 'Apply',
	close: 'Close',
	invalidUrl: 'Give an address starting with http://, https:// or mailto:, or a relative one.',
	plainText: 'Plain text',
	defaultColor: 'Default',
	noBackground: 'None',
	recentColors: 'Recent',
	colorGray: 'Gray',
	colorBrown: 'Brown',
	colorRed: 'Red',
	colorOrange: 'Orange',
	colorYellow: 'Yellow',
	colorLime: 'Lime',
	colorGreen: 'Green',
	colorTeal: 'Teal',
	colorCyan: 'Cyan',
	colorBlue: 'Blue',
	colorIndigo: 'Indigo',
	colorPurple: 'Purple',
	colorPink: 'Pink',
	uploadImage: 'Upload an image…',
	uploading: 'Uploading {0}…',
	theImage: 'The image',
	uploadFailed: '{0} could not be uploaded.',
	uploadTooLarge: '{0} is larger than {1}.',
	uploadType: '{0} is not an image of a type the editor takes: {1}.',
	characters: '{0} characters',
	character: '{0} character',
	charactersOfLimit: '{0} of {1} characters',
	words: '{0} words',
	word: '{0} word',
	resize: 'Drag, or use the arrow keys, to resize the editor',
	proofreadSpelling: 'Spelling',
	proofreadGrammar: 'Grammar',
	proofreadStyle: 'Style',
	proofreadReplacements: 'Replacements',
	proofreadNoSuggestions: 'No suggestions',
	proofreadIgnore: 'Ignore',
	suggestions: 'Suggestions',
	suggestLoading: 'Searching…',
	suggestNone: 'Nothing found'
});

/** The bar under the icon of a tool of colors, the color where the cursor is. */
const BAR_CLASS = 'antilia-rich-text-tool-bar';

/** The tools of colors, whose menus are color pickers, by the marks they set. */
type ColorMark = 'textColor' | 'textBackground';

/** The columns of the grid of a color picker, at most. */
const PALETTE_COLUMNS = 6;

/** The last colors chosen a color picker shows below its grid, at most. */
const RECENT_COLORS = 4;

/**
 * The last colors chosen kept, at most: more than a picker shows, so that one whose palette has
 * not all of them still has some to show.
 */
const RECENT_COLORS_KEPT = 12;

/** Where the browser keeps the last colors chosen, for the editors of the site. */
const RECENT_COLORS_KEY = 'antilia-rich-text-recent-colors';

/** The last colors chosen, while the browser keeps none: no localStorage, or none allowed. */
const recentInMemory: Record<ColorMark, readonly string[]> = { textColor: [], textBackground: [] };

/**
 * @returns the last colors chosen kept by the browser, by their marks; null when it keeps none
 */
function keptRecentColors(): Partial<Record<ColorMark, unknown>> | null {
	try {
		const kept = JSON.parse(localStorage.getItem(RECENT_COLORS_KEY) ?? '{}') as unknown;
		return kept && typeof kept === 'object' ? kept as Partial<Record<ColorMark, unknown>> : {};
	} catch {
		return null;
	}
}

/**
 * @param mark the color of the text, or of its background
 * @returns the names of the last colors chosen in the color pickers of the editors of the site,
 *          the last first: kept by the browser (localStorage), in memory while it keeps none
 */
export function recentRichTextColors(mark: ColorMark): readonly string[] {
	const kept = keptRecentColors();
	const names = kept ? kept[mark] : recentInMemory[mark];
	return Array.isArray(names) ? names.filter((name): name is string => typeof name === 'string')
		.slice(0, RECENT_COLORS_KEPT) : [];
}

/**
 * Keeps a color chosen in a color picker first among the last ones.
 */
function rememberRichTextColor(mark: ColorMark, name: string): void {
	const names = [name, ...recentRichTextColors(mark).filter((each) => each !== name)]
		.slice(0, RECENT_COLORS_KEPT);
	try {
		localStorage.setItem(RECENT_COLORS_KEY, JSON.stringify({ ...keptRecentColors(),
			[mark]: names }));
	} catch {
		recentInMemory[mark] = names;
	}
}

const svg = (paths: string) => '<svg viewBox="0 0 24 24" width="1em" height="1em" fill="none" ' +
	'stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" ' +
	'aria-hidden="true" focusable="false">' + paths + '</svg>';

/**
 * The icon of a tool of colors, over the bar of the color where the cursor is: an element of its
 * own, which the class of a color of the text or of the background colors.
 */
const colorIcon = (paths: string) => '<span class="antilia-rich-text-tool-icon" ' +
	'aria-hidden="true">' + svg(paths) + '<span class="' + BAR_CLASS + '"></span></span>';

/**
 * The tick on the color where the cursor is, in a color picker: a light mark on a dark halo, which
 * reads on light and dark colors alike.
 */
const TICK = '<svg class="antilia-rich-text-palette-tick" viewBox="0 0 24 24" width="1em" ' +
	'height="1em" fill="none" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" ' +
	'focusable="false"><path class="antilia-rich-text-palette-tick-halo" ' +
	'd="m5 12.5 4.5 4.5 9.5-10"/><path class="antilia-rich-text-palette-tick-mark" ' +
	'd="m5 12.5 4.5 4.5 9.5-10"/></svg>';

/** The icons of the tools, and the arrow of a menu. */
export const RICH_TEXT_ICONS: Readonly<Record<Exclude<RichTextToolName, 'heading'> | 'menu', string>> =
	Object.freeze({
		bold: svg('<path d="M7 5h6a3.5 3.5 0 0 1 0 7H7zM7 12h7a3.5 3.5 0 0 1 0 7H7z" ' +
			'stroke-width="2.5"/>'),
		italic: svg('<path d="M19 4h-9M14 20H5M15 4 9 20"/>'),
		underline: svg('<path d="M6 4v6a6 6 0 0 0 12 0V4M4 20h16"/>'),
		strike: svg('<path d="M16.5 7A4 4 0 0 0 13 5h-2a3.5 3.5 0 0 0-1.6 6.6M4 12h16M7.5 17a4 4 0 0 ' +
			'0 3.5 2h2a3.5 3.5 0 0 0 3-5.3"/>'),
		code: svg('<path d="m16 18 6-6-6-6M8 6l-6 6 6 6"/>'),
		// an x, and a small 2 raised or lowered beside it
		superscript: svg('<path d="m4 8 9 11M13 8l-9 11"/>' +
			'<path d="M16 4.7a1.5 1.5 0 0 1 2.9.4c0 1-2.9 1.9-2.9 3.4h3" stroke-width="1.5"/>'),
		subscript: svg('<path d="m4 5 9 11M13 5l-9 11"/>' +
			'<path d="M16 15.7a1.5 1.5 0 0 1 2.9.4c0 1-2.9 1.9-2.9 3.4h3" stroke-width="1.5"/>'),
		// a letter, and a marker, over the bar of the color where the cursor is
		textColor: colorIcon('<path d="m6 16 6-13 6 13M8.3 11h7.4"/>'),
		textBackground: colorIcon('<path d="m14.5 4.5 4 4-7 7H7.5v-4zM7.5 15.5 5 16l.5-2.5"/>'),
		link: svg('<path d="M10 13a5 5 0 0 0 7.5.5l3-3a5 5 0 0 0-7-7l-1.7 1.7"/>' +
			'<path d="M14 11a5 5 0 0 0-7.5-.5l-3 3a5 5 0 0 0 7 7l1.7-1.7"/>'),
		bulletList: svg('<path d="M9 6h11M9 12h11M9 18h11"/>' +
			'<path d="M4.5 6h.01M4.5 12h.01M4.5 18h.01" stroke-width="3"/>'),
		orderedList: svg('<path d="M10 6h10M10 12h10M10 18h10"/>' +
			'<path d="M4 4.5h1.5V9M4 9h3M4 14.2a1.5 1.5 0 0 1 2.9.4c0 1-2.9 1.9-2.9 3.4h3" ' +
			'stroke-width="1.5"/>'),
		taskList: svg('<rect x="3" y="4" width="6" height="6" rx="1"/>' +
			'<path d="m3.5 17 2 2 3.5-4M13 7h8M13 17h8"/>'),
		blockquote: svg('<path d="M5 5v14M10 7h10M10 12h10M10 17h6"/>'),
		codeBlock: svg('<rect x="3" y="4" width="18" height="16" rx="2"/>' +
			'<path d="m10 9.5-2.5 2.5 2.5 2.5M14 9.5l2.5 2.5-2.5 2.5"/>'),
		horizontalRule: svg('<path d="M3 12h18"/><path d="M7 6.5h10M7 17.5h10" opacity="0.45"/>'),
		table: svg('<rect x="3" y="4" width="18" height="16" rx="2"/>' +
			'<path d="M3 10h18M3 15h18M9 4v16M15 4v16"/>'),
		image: svg('<rect x="3" y="4" width="18" height="16" rx="2"/>' +
			'<circle cx="9" cy="9.5" r="1.8"/><path d="m21 16-5-5L5 20"/>'),
		alignLeft: svg('<path d="M4 6h16M4 10h10M4 14h16M4 18h10"/>'),
		alignCenter: svg('<path d="M4 6h16M7 10h10M4 14h16M7 18h10"/>'),
		alignRight: svg('<path d="M4 6h16M10 10h10M4 14h16M10 18h10"/>'),
		alignJustify: svg('<path d="M4 6h16M4 10h16M4 14h16M4 18h16"/>'),
		clearFormatting: svg('<path d="M4 7V5h11v2M9.5 5l-3 14M4.5 19h4M15 14l5 5M20 14l-5 5"/>'),
		undo: svg('<path d="M9 14 4 9l5-5"/><path d="M4 9h10.5a5.5 5.5 0 0 1 0 11H11"/>'),
		redo: svg('<path d="m15 14 5-5-5-5"/><path d="M20 9H9.5a5.5 5.5 0 0 0 0 11H13"/>'),
		menu: svg('<path d="m6 9 6 6 6-6"/>')
	});

/** What a tool does, and what it needs. */
interface ToolSpec {
	/** The feature the tool needs, none for null. */
	feature: RichTextFeature | null;
	kind: 'toggle' | 'action' | 'menu' | 'form';
	/** The keys doing it too, as Tiptap names them. */
	shortcut?: string | undefined;
	active?: ((editor: Editor) => boolean) | undefined;
	enabled?: ((editor: Editor) => boolean) | undefined;
	run?: ((editor: Editor, context: RichTextToolContext) => void) | undefined;
	/** For a tool of a feature registered: what the toolbar calls it, unless the texts do. */
	label?: string | undefined;
	/** For a tool of a feature registered: its icon, its label when none. */
	icon?: string | undefined;
	/** For a menu of a feature registered: its items. */
	items?: ((editor: Editor) => readonly RichTextMenuItem[]) | undefined;
	/** Whether a feature registered brings it. */
	registered?: boolean;
}

const ALIGNMENTS: Record<'alignLeft' | 'alignCenter' | 'alignRight' | 'alignJustify',
	RichTextAlignment> = { alignLeft: 'left', alignCenter: 'center', alignRight: 'right',
	alignJustify: 'justify' };

function aligned(editor: Editor, alignment: RichTextAlignment): boolean {
	if (alignment !== 'left') {
		return editor.isActive({ textAlign: alignment });
	}
	return !(['center', 'right', 'justify'] as const).some((other) =>
		editor.isActive({ textAlign: other }));
}

function align(alignment: RichTextAlignment): ToolSpec {
	return {
		feature: 'textAlign',
		kind: 'toggle',
		shortcut: { left: 'Mod-Shift-L', center: 'Mod-Shift-E', right: 'Mod-Shift-R',
			justify: 'Mod-Shift-J' }[alignment],
		active: (editor) => aligned(editor, alignment),
		enabled: (editor) => editor.can().setTextAlign(alignment),
		run: (editor) => editor.chain().focus().setTextAlign(alignment).run()
	};
}

const SPECS: Readonly<Record<RichTextToolName, ToolSpec>> = {
	heading: { feature: 'heading', kind: 'menu' },
	bold: { feature: 'bold', kind: 'toggle', shortcut: 'Mod-B',
		active: (editor) => editor.isActive('bold'),
		enabled: (editor) => editor.can().toggleBold(),
		run: (editor) => editor.chain().focus().toggleBold().run() },
	italic: { feature: 'italic', kind: 'toggle', shortcut: 'Mod-I',
		active: (editor) => editor.isActive('italic'),
		enabled: (editor) => editor.can().toggleItalic(),
		run: (editor) => editor.chain().focus().toggleItalic().run() },
	underline: { feature: 'underline', kind: 'toggle', shortcut: 'Mod-U',
		active: (editor) => editor.isActive('underline'),
		enabled: (editor) => editor.can().toggleUnderline(),
		run: (editor) => editor.chain().focus().toggleUnderline().run() },
	strike: { feature: 'strike', kind: 'toggle', shortcut: 'Mod-Shift-S',
		active: (editor) => editor.isActive('strike'),
		enabled: (editor) => editor.can().toggleStrike(),
		run: (editor) => editor.chain().focus().toggleStrike().run() },
	code: { feature: 'code', kind: 'toggle', shortcut: 'Mod-E',
		active: (editor) => editor.isActive('code'),
		enabled: (editor) => editor.can().toggleCode(),
		run: (editor) => editor.chain().focus().toggleCode().run() },
	superscript: { feature: 'superscript', kind: 'toggle', shortcut: 'Mod-Shift-.',
		active: (editor) => editor.isActive('superscript'),
		enabled: (editor) => editor.can().toggleSuperscript(),
		run: (editor) => editor.chain().focus().toggleSuperscript().run() },
	subscript: { feature: 'subscript', kind: 'toggle', shortcut: 'Mod-Shift-,',
		active: (editor) => editor.isActive('subscript'),
		enabled: (editor) => editor.can().toggleSubscript(),
		run: (editor) => editor.chain().focus().toggleSubscript().run() },
	textColor: { feature: 'textColor', kind: 'menu' },
	textBackground: { feature: 'textBackground', kind: 'menu' },
	link: { feature: 'link', kind: 'form', shortcut: 'Mod-K',
		active: (editor) => editor.isActive('link'),
		enabled: (editor) => editor.isActive('link') || editor.can().setLink({ href: '#' }) },
	bulletList: { feature: 'bulletList', kind: 'toggle', shortcut: 'Mod-Shift-8',
		active: (editor) => editor.isActive('bulletList'),
		enabled: (editor) => editor.can().toggleBulletList(),
		run: (editor) => editor.chain().focus().toggleBulletList().run() },
	orderedList: { feature: 'orderedList', kind: 'toggle', shortcut: 'Mod-Shift-7',
		active: (editor) => editor.isActive('orderedList'),
		enabled: (editor) => editor.can().toggleOrderedList(),
		run: (editor) => editor.chain().focus().toggleOrderedList().run() },
	taskList: { feature: 'taskList', kind: 'toggle', shortcut: 'Mod-Shift-9',
		active: (editor) => editor.isActive('taskList'),
		enabled: (editor) => editor.can().toggleTaskList(),
		run: (editor) => editor.chain().focus().toggleTaskList().run() },
	blockquote: { feature: 'blockquote', kind: 'toggle', shortcut: 'Mod-Shift-B',
		active: (editor) => editor.isActive('blockquote'),
		enabled: (editor) => editor.can().toggleBlockquote(),
		run: (editor) => editor.chain().focus().toggleBlockquote().run() },
	codeBlock: { feature: 'codeBlock', kind: 'toggle', shortcut: 'Mod-Alt-C',
		active: (editor) => editor.isActive('codeBlock'),
		enabled: (editor) => editor.can().toggleCodeBlock(),
		run: (editor) => editor.chain().focus().toggleCodeBlock().run() },
	horizontalRule: { feature: 'horizontalRule', kind: 'action',
		enabled: (editor) => editor.can().setHorizontalRule(),
		run: (editor) => editor.chain().focus().setHorizontalRule().run() },
	table: { feature: 'table', kind: 'menu' },
	image: { feature: 'image', kind: 'form',
		active: (editor) => editor.isActive('image'),
		enabled: (editor) => editor.isActive('image') || editor.can().setImage({ src: '#' }) },
	alignLeft: align(ALIGNMENTS.alignLeft),
	alignCenter: align(ALIGNMENTS.alignCenter),
	alignRight: align(ALIGNMENTS.alignRight),
	alignJustify: align(ALIGNMENTS.alignJustify),
	clearFormatting: { feature: null, kind: 'action',
		enabled: (editor) => editor.can().unsetAllMarks() || editor.can().clearNodes(),
		run: (editor) => editor.chain().focus().unsetAllMarks().clearNodes().run() },
	undo: { feature: null, kind: 'action', shortcut: 'Mod-Z',
		enabled: (editor) => editor.can().undo(),
		run: (editor) => editor.chain().focus().undo().run() },
	redo: { feature: null, kind: 'action', shortcut: 'Mod-Shift-Z',
		enabled: (editor) => editor.can().redo(),
		run: (editor) => editor.chain().focus().redo().run() }
};

/**
 * @returns what a tool does: that of a feature registered, which replaces a built-in one of its
 *          name, or the built-in one; none for a tool nobody has
 */
function toolSpec(tool: RichTextTool): ToolSpec | undefined {
	const registered = registeredTool(tool);
	if (registered) {
		const { definition } = registered;
		return { feature: registered.feature, kind: definition.kind ?? 'toggle',
			shortcut: definition.shortcut, active: definition.active, enabled: definition.enabled,
			run: definition.run, label: definition.label, icon: definition.icon,
			items: definition.items, registered: true };
	}
	return Object.prototype.hasOwnProperty.call(SPECS, tool) ?
		SPECS[tool as RichTextToolName] : undefined;
}

/**
 * @returns whether a tool works with the features of an editor
 */
export function toolAvailable(tool: RichTextTool, features: ReadonlySet<RichTextFeature>): boolean {
	const spec = toolSpec(tool);
	return !!spec && (spec.feature === null || features.has(spec.feature));
}

/**
 * @returns what the toolbar calls a tool: as the texts of the editor do, else as the feature
 *          registering it does, else by its name
 */
function toolLabel(tool: RichTextTool, texts: RichTextEditorTexts, spec?: ToolSpec): string {
	const text = (texts as unknown as Record<string, unknown>)[tool];
	return typeof text === 'string' && text ? text : spec?.label ?? tool;
}

/**
 * @returns the keys, as a page shows them: Ctrl+Shift+8, or ⌘⇧8 on a Mac
 */
export function shortcutText(shortcut: string, mac = isMac()): string {
	const keys = shortcut.split('-').map((key) => {
		if (key === 'Mod') {
			return mac ? '⌘' : 'Ctrl';
		}
		if (key === 'Shift') {
			return mac ? '⇧' : 'Shift';
		}
		if (key === 'Alt') {
			return mac ? '⌥' : 'Alt';
		}
		return key.toUpperCase();
	});
	return keys.join(mac ? '' : '+');
}

function isMac(): boolean {
	return typeof navigator !== 'undefined' && /Mac|iPhone|iPad/.test(navigator.platform ?? '');
}

/**
 * @returns what the menus call a color: its label, else the text color and its name capitalized,
 *          colorRed for red, else its name
 */
function colorLabel(color: { name: string; label?: string | undefined },
	texts: RichTextEditorTexts): string {
	if (color.label) {
		return color.label;
	}
	const key = 'color' + color.name[0]!.toUpperCase() + color.name.slice(1);
	const text = Object.prototype.hasOwnProperty.call(texts, key) ?
		(texts as unknown as Record<string, unknown>)[key] : undefined;
	return typeof text === 'string' && text ? text : color.name;
}

/** {0}, {1}... in a text, by what stands for them. */
export function format(text: string, ...values: (string | number)[]): string {
	return text.replace(/\{(\d)\}/g, (all, index: string) => {
		const value = values[Number(index)];
		return value === undefined ? all : String(value);
	});
}

/** An item of a menu, or a separator. */
interface MenuEntry {
	id: string;
	label: string;
	level?: number;
	/** The classes of a swatch before the label, for a menu of colors. */
	swatch?: string;
	/** Whether the item is the one in force, for a menu of choices. */
	checked?: (editor: Editor) => boolean;
	enabled: (editor: Editor) => boolean;
	run: (editor: Editor, context: RichTextToolContext) => void;
}

type MenuLine = MenuEntry | 'separator';

/** A color of the menu of a tool of colors, with its class: of the text, or of the background. */
interface PaletteColor {
	name: string;
	label?: string | undefined;
	css: string;
}

/** A language of code blocks: its name, as their class has it, and what the menu calls it. */
export interface RichTextCodeLanguage {
	name: string;
	label: string;
}

/** What a toolbar works with. */
export interface RichTextToolbarContext {
	/** The editor of Tiptap, which the rich text editor makes anew when its schema changes. */
	editor: () => Editor | undefined;
	look: Look;
	texts: () => RichTextEditorTexts;
	features: () => ReadonlySet<RichTextFeature>;
	headingLevels: () => readonly HeadingLevel[];
	/** The palette of the menu of the colors of the text, DEFAULT_RICH_TEXT_COLORS when left out. */
	colors?: () => readonly RichTextColor[];
	/**
	 * The palette of the menu of the colors of the background, DEFAULT_RICH_TEXT_BACKGROUND_COLORS
	 * when left out.
	 */
	backgroundColors?: () => readonly RichTextBackgroundColor[];
	/** The languages of the menu of the code block; none for a tool making a code block. */
	codeLanguages?: () => readonly RichTextCodeLanguage[];
	/** The uploads of the form of an image; none while the page uploads no images. */
	uploads?: () => UploadHost | null;
	/** Called as a form opens and once it closed. */
	onForm?: (open: boolean) => void;
	/**
	 * Fires a type of events registered on the editor, for the tools of the features registered;
	 * none fires when left out.
	 */
	trigger?: RichTextTrigger;
}

/** A tool drawn: its button, and its menu if it has one. */
interface DrawnTool {
	tool: RichTextTool;
	button: HTMLButtonElement;
	menu: HTMLElement | null;
	label: HTMLElement | null;
}

/** Count the menus and the fields of the forms, for their ids. */
let menus = 0;

let fields = 0;

/** The form open, in its popover, and the toolbar that opened it. */
let openForm: { popover: Popover; toolbar: RichTextToolbar } | null = null;

export class RichTextToolbar {
	/** The element of the toolbar. */
	readonly element: HTMLElement;

	private readonly context: RichTextToolbarContext;

	private tools: DrawnTool[] = [];

	/** The tool taking the focus when Tab comes to the toolbar. */
	private current = 0;

	/** The tool whose menu is open, if any. */
	private opened: DrawnTool | null = null;

	/** The items of the color picker open, by rows, for the arrow keys; none for another menu. */
	private paletteRows: HTMLButtonElement[][] = [];

	private listeners: Array<[EventTarget, string, EventListener, boolean]> = [];

	/**
	 * @param context the editor and what the toolbar shows
	 * @param groups the tools, in groups; those the features leave out do not show
	 * @param part the part the toolbar is: the toolbar, or the bubble menu
	 */
	constructor(context: RichTextToolbarContext, groups: readonly (readonly RichTextTool[])[],
		part: 'rich-text-toolbar' | 'rich-text-bubble' = 'rich-text-toolbar') {
		this.context = context;
		const element = document.createElement('div');
		element.className = this.css(part);
		element.setAttribute('role', 'toolbar');
		this.element = element;
		this.render(groups, part);
		this.listen(element, 'keydown', (event) => this.onKeyDown(event as KeyboardEvent));
		// pressing a tool keeps the focus, and the selection, in the text
		this.listen(element, 'mousedown', (event) => {
			if ((event.target as Element).closest('button')) {
				event.preventDefault();
			}
		});
		this.listen(document, 'pointerdown', (event) => {
			if (this.opened && !this.opened.button.parentElement!.contains(event.target as Node)) {
				this.closeMenu(false);
			}
		}, true);
		this.update();
	}

	/** Tiptap's editor, there once the toolbar acts. */
	private get editor(): Editor {
		return this.context.editor()!;
	}

	/** What the run of a tool, or of an item of its menu, has of the editor besides Tiptap's. */
	private get toolContext(): RichTextToolContext {
		return { trigger: (name, values) => this.context.trigger?.(name, values) ?? false };
	}

	private css(...parts: LookPart[]): string {
		return partClasses(this.context.look, ...parts);
	}

	/**
	 * @returns what a tool is: the code block opens a menu while the editor has languages for it
	 */
	private kindOf(tool: RichTextTool): ToolSpec['kind'] {
		const spec = toolSpec(tool);
		if (tool === 'codeBlock' && !spec?.registered && this.codeLanguages().length) {
			return 'menu';
		}
		return spec?.kind ?? 'action';
	}

	/**
	 * @returns whether a tool is one of colors built in, whose menu is a color picker
	 */
	private isPalette(tool: RichTextTool): tool is ColorMark {
		return (tool === 'textColor' || tool === 'textBackground') && !toolSpec(tool)?.registered;
	}

	private codeLanguages(): readonly RichTextCodeLanguage[] {
		return this.context.codeLanguages?.() ?? [];
	}

	private listen(target: EventTarget, type: string, listener: (event: Event) => void,
		capture = false): void {
		target.addEventListener(type, listener, capture);
		this.listeners.push([target, type, listener, capture]);
	}

	/**
	 * Draws the tools anew: after other features, tools or texts.
	 */
	render(groups: readonly (readonly RichTextTool[])[],
		part: 'rich-text-toolbar' | 'rich-text-bubble' = 'rich-text-toolbar'): void {
		this.closeMenu(false);
		const texts = this.context.texts();
		const features = this.context.features();
		this.element.setAttribute('aria-label', part === 'rich-text-bubble' ? texts.bubble :
			texts.toolbar);
		this.element.replaceChildren();
		this.tools = [];
		const seen = new Set<RichTextTool>();
		for (const group of groups) {
			const tools = group.filter((tool) => toolAvailable(tool, features) && !seen.has(tool));
			if (!tools.length) {
				continue;
			}
			const box = document.createElement('div');
			box.className = this.css('rich-text-group');
			box.setAttribute('role', 'group');
			for (const tool of tools) {
				seen.add(tool);
				box.append(this.draw(tool, texts));
			}
			this.element.append(box);
		}
		this.current = Math.min(this.current, Math.max(this.tools.length - 1, 0));
		this.element.hidden = !this.tools.length;
		this.update();
	}

	/**
	 * @returns the element of a tool: its button, in a box with its menu if it has one
	 */
	private draw(tool: RichTextTool, texts: RichTextEditorTexts): HTMLElement {
		const spec = toolSpec(tool)!;
		const kind = this.kindOf(tool);
		const button = document.createElement('button');
		button.type = 'button';
		button.className = this.css('rich-text-tool');
		button.dataset.tool = tool;
		button.tabIndex = -1;
		const name = toolLabel(tool, texts, spec);
		button.setAttribute('aria-label', name);
		button.title = spec.shortcut ? name + ' (' + shortcutText(spec.shortcut) + ')' : name;
		let label: HTMLElement | null = null;
		if (spec.registered) {
			if (spec.icon) {
				button.insertAdjacentHTML('beforeend', spec.icon);
			} else {
				// no icon: the label
				const text = document.createElement('span');
				text.className = 'antilia-rich-text-tool-label';
				text.textContent = name;
				button.append(text);
				button.classList.add('antilia-rich-text-tool-wide');
			}
		} else if (tool === 'heading') {
			label = document.createElement('span');
			label.className = 'antilia-rich-text-tool-label';
			button.append(label);
			button.classList.add('antilia-rich-text-tool-wide');
		} else {
			button.insertAdjacentHTML('beforeend',
				RICH_TEXT_ICONS[tool as Exclude<RichTextToolName, 'heading'>]);
		}
		if (kind === 'toggle') {
			button.setAttribute('aria-pressed', 'false');
		}
		const drawn: DrawnTool = { tool, button, menu: null, label };
		this.tools.push(drawn);
		const index = this.tools.length - 1;
		button.addEventListener('focus', () => this.setCurrent(index));
		if (kind !== 'menu') {
			button.addEventListener('click', () => this.press(drawn));
			return button;
		}
		button.insertAdjacentHTML('beforeend', RICH_TEXT_ICONS.menu);
		button.setAttribute('aria-haspopup', 'menu');
		button.setAttribute('aria-expanded', 'false');
		const box = document.createElement('div');
		box.className = 'antilia-rich-text-tool-box';
		const palette = this.isPalette(tool);
		const menu: HTMLElement = document.createElement(palette ? 'div' : 'ul');
		menu.className = this.css('rich-text-menu') + (palette ? ' antilia-rich-text-palette' : '');
		menu.id = 'antilia-rich-text-menu-' + (++menus);
		menu.setAttribute('role', 'menu');
		menu.setAttribute('aria-label', name);
		menu.hidden = true;
		button.setAttribute('aria-controls', menu.id);
		drawn.menu = menu;
		button.addEventListener('click', () => {
			if (this.opened === drawn) {
				this.closeMenu(true);
			} else if (button.getAttribute('aria-disabled') !== 'true') {
				this.openMenu(drawn, 'checked');
			}
		});
		menu.addEventListener('keydown', (event) => this.onMenuKeyDown(event));
		box.append(button, menu);
		return box;
	}

	/**
	 * Does what a tool of a button does.
	 */
	private press(drawn: DrawnTool): void {
		if (drawn.button.getAttribute('aria-disabled') === 'true') {
			return;
		}
		const spec = toolSpec(drawn.tool);
		if (!spec) {
			return;
		}
		if (spec.kind === 'form') {
			if (drawn.tool === 'link') {
				this.openLink(drawn.button);
			} else {
				this.openImage(drawn.button);
			}
			return;
		}
		spec.run?.(this.editor, this.toolContext);
	}

	/**
	 * Shows the state of the tools where the cursor is: those on, those that cannot act and the
	 * text style.
	 */
	update(): void {
		const editor = this.context.editor();
		if (!editor || editor.isDestroyed) {
			return;
		}
		const texts = this.context.texts();
		const features = this.context.features();
		this.tools.forEach((drawn, index) => {
			const spec = toolSpec(drawn.tool);
			if (!spec || !toolAvailable(drawn.tool, features)) {
				// drawn for other features: the toolbar draws its tools anew next
				return;
			}
			const enabled = this.isEnabled(drawn.tool);
			drawn.button.setAttribute('aria-disabled', String(!enabled));
			const active = !!spec.active?.(editor);
			if (drawn.button.hasAttribute('aria-pressed')) {
				drawn.button.setAttribute('aria-pressed', String(active));
			}
			toggleParts(drawn.button, this.context.look, active, 'rich-text-tool-active');
			drawn.button.tabIndex = index === this.current ? 0 : -1;
			if (drawn.label) {
				drawn.label.textContent = this.blockLabel(texts);
			}
			if (this.isPalette(drawn.tool)) {
				this.showColor(drawn.button, drawn.tool);
			}
		});
		if (this.opened) {
			this.refreshMenu(this.opened);
		}
	}

	/**
	 * Colors the bar under the icon of a tool of colors as the text is where the cursor is: by the
	 * class of the color, of the text or of the background.
	 */
	private showColor(button: HTMLButtonElement, mark: 'textColor' | 'textBackground'): void {
		const name = this.editor.getAttributes(mark).color as unknown;
		const color = this.palette(mark).find((each) => each.name === name);
		const bar = button.querySelector('.' + BAR_CLASS);
		bar?.setAttribute('class', color ? BAR_CLASS + ' ' + color.css : BAR_CLASS);
		if (color) {
			button.dataset.color = color.name;
		} else {
			delete button.dataset.color;
		}
	}

	/**
	 * @returns the colors of the menu of a tool of colors, each with its class: of the text, or of
	 *          the background
	 */
	private palette(mark: 'textColor' | 'textBackground'): readonly PaletteColor[] {
		return mark === 'textColor' ?
			(this.context.colors?.() ?? DEFAULT_RICH_TEXT_COLORS).map((color) => ({
				name: color.name, label: color.label, css: color.colorClass })) :
			(this.context.backgroundColors?.() ?? DEFAULT_RICH_TEXT_BACKGROUND_COLORS).map(
				(color) => ({ name: color.name, label: color.label, css: color.backgroundClass }));
	}

	private isEnabled(tool: RichTextTool): boolean {
		const editor = this.editor;
		if (!editor.isEditable) {
			return false;
		}
		if (tool === 'heading') {
			return this.blockEntries().some((entry) => entry.enabled(editor));
		}
		const spec = toolSpec(tool);
		if (this.kindOf(tool) === 'menu' && (spec?.registered || tool === 'table' ||
			tool === 'textColor' || tool === 'textBackground' || tool === 'codeBlock')) {
			return this.menuLines(this.tools.find((drawn) => drawn.tool === tool) ??
				{ tool }).some((entry) => entry !== 'separator' && entry.enabled(editor));
		}
		return spec?.enabled?.(editor) ?? true;
	}

	/**
	 * @returns the name of the text style where the cursor is
	 */
	private blockLabel(texts: RichTextEditorTexts): string {
		const editor = this.editor;
		const level = this.context.headingLevels().find((each) =>
			editor.isActive('heading', { level: each }));
		return level ? format(texts.headingLevel, level) : texts.paragraph;
	}

	private blockEntries(): MenuEntry[] {
		const texts = this.context.texts();
		return [
			{ id: 'paragraph', label: texts.paragraph,
				checked: (editor) => !editor.isActive('heading'),
				enabled: (editor) => editor.isActive('paragraph') || editor.can().setParagraph(),
				run: (editor) => editor.chain().focus().setParagraph().run() },
			...this.context.headingLevels().map((level): MenuEntry => ({
				id: 'heading' + level, label: format(texts.headingLevel, level), level,
				checked: (editor) => editor.isActive('heading', { level }),
				enabled: (editor) => editor.isActive('heading', { level }) ||
					editor.can().setHeading({ level }),
				run: (editor) => editor.chain().focus().setHeading({ level }).run() }))
		];
	}

	private tableEntries(): MenuLine[] {
		const texts = this.context.texts();
		const entry = (id: keyof RichTextEditorTexts, enabled: (editor: Editor) => boolean,
			run: (editor: Editor) => void): MenuEntry => ({ id, label: texts[id], enabled, run });
		return [
			entry('insertTable', (editor) => !editor.isActive('table') &&
				editor.can().insertTable({ rows: 3, cols: 3, withHeaderRow: true }),
			(editor) => editor.chain().focus().insertTable({ rows: 3, cols: 3,
				withHeaderRow: true }).run()),
			'separator',
			entry('addRowBefore', (editor) => editor.can().addRowBefore(),
				(editor) => editor.chain().focus().addRowBefore().run()),
			entry('addRowAfter', (editor) => editor.can().addRowAfter(),
				(editor) => editor.chain().focus().addRowAfter().run()),
			entry('addColumnBefore', (editor) => editor.can().addColumnBefore(),
				(editor) => editor.chain().focus().addColumnBefore().run()),
			entry('addColumnAfter', (editor) => editor.can().addColumnAfter(),
				(editor) => editor.chain().focus().addColumnAfter().run()),
			'separator',
			entry('deleteRow', (editor) => editor.can().deleteRow(),
				(editor) => editor.chain().focus().deleteRow().run()),
			entry('deleteColumn', (editor) => editor.can().deleteColumn(),
				(editor) => editor.chain().focus().deleteColumn().run()),
			entry('toggleHeaderRow', (editor) => editor.can().toggleHeaderRow(),
				(editor) => editor.chain().focus().toggleHeaderRow().run()),
			'separator',
			entry('deleteTable', (editor) => editor.can().deleteTable(),
				(editor) => editor.chain().focus().deleteTable().run())
		];
	}

	/**
	 * @returns the items of the menu of the code block: a paragraph again, or a code block in
	 *          plain text or in a language. A block in a language the menu has not is in none of
	 *          them.
	 */
	private codeEntries(): MenuLine[] {
		const texts = this.context.texts();
		const language = (editor: Editor) => editor.isActive('codeBlock') ?
			codeLanguage(editor.getAttributes('codeBlock').language as string | null) : undefined;
		const choose = (name: string | null) => (editor: Editor) => {
			const chain = editor.chain().focus();
			(editor.isActive('codeBlock') ? chain.updateAttributes('codeBlock', { language: name }) :
				chain.setCodeBlock({ language: name } as { language: string })).run();
		};
		const can = (editor: Editor) => editor.isActive('codeBlock') ||
			editor.can().setCodeBlock();
		return [
			{ id: 'paragraph', label: texts.paragraph,
				checked: (editor) => !editor.isActive('codeBlock'),
				enabled: (editor) => !editor.isActive('codeBlock') || editor.can().setParagraph(),
				run: (editor) => editor.chain().focus().setParagraph().run() },
			'separator',
			{ id: 'plainText', label: texts.plainText,
				checked: (editor) => language(editor) === null, enabled: can, run: choose(null) },
			...this.codeLanguages().map((each): MenuEntry => ({
				id: 'language-' + each.name, label: each.label,
				checked: (editor) => language(editor) === each.name, enabled: can,
				run: choose(each.name) }))
		];
	}

	/**
	 * @returns the items of the menu of the color of the text, or of its background: the default,
	 *          then the colors of the palette
	 */
	private colorEntries(mark: 'textColor' | 'textBackground'): MenuEntry[] {
		const texts = this.context.texts();
		const text = mark === 'textColor';
		const swatch = 'antilia-rich-text-swatch antilia-rich-text-swatch-' +
			(text ? 'text' : 'background');
		const colors = this.palette(mark);
		const can = (editor: Editor, color: string) => text ? editor.can().setTextColor(color) :
			editor.can().setTextBackground(color);
		const set = (editor: Editor, color: string) => text ?
			editor.chain().focus().setTextColor(color).run() :
			editor.chain().focus().setTextBackground(color).run();
		return [
			{ id: 'default', label: text ? texts.defaultColor : texts.noBackground,
				swatch: swatch + (text ? ' antilia-rich-text-swatch-default' :
					' antilia-rich-text-swatch-none'),
				checked: (editor) => !editor.isActive(mark),
				// where a color may be set at all: not in code
				enabled: (editor) => editor.isActive(mark) ||
					(!!colors[0] && can(editor, colors[0].name)),
				run: (editor) => text ? editor.chain().focus().unsetTextColor().run() :
					editor.chain().focus().unsetTextBackground().run() },
			...colors.map((color): MenuEntry => ({
				id: color.name, label: colorLabel(color, texts),
				swatch: swatch + ' ' + color.css,
				checked: (editor) => editor.isActive(mark, { color: color.name }),
				enabled: (editor) => editor.isActive(mark, { color: color.name }) ||
					can(editor, color.name),
				run: (editor) => {
					rememberRichTextColor(mark, color.name);
					set(editor, color.name);
				} }))
		];
	}

	private menuLines(drawn: Pick<DrawnTool, 'tool'>): MenuLine[] {
		const spec = toolSpec(drawn.tool);
		if (spec?.registered) {
			return (spec.items?.(this.editor) ?? []).map((item): MenuEntry => ({ id: item.id,
				label: item.label, checked: item.checked,
				enabled: (editor) => item.enabled?.(editor) ?? true, run: item.run }));
		}
		switch (drawn.tool) {
		case 'heading':
			return this.blockEntries();
		case 'codeBlock':
			return this.codeEntries();
		case 'textColor':
		case 'textBackground':
			return this.colorEntries(drawn.tool as 'textColor' | 'textBackground');
		default:
			return this.tableEntries();
		}
	}

	/**
	 * Shows the state of the items of an open menu as the cursor stands: in place, so that the
	 * item with the focus keeps it.
	 */
	private refreshMenu(drawn: DrawnTool): void {
		const editor = this.editor;
		const lines = this.menuLines(drawn).filter((line): line is MenuEntry =>
			line !== 'separator');
		for (const button of this.menuItems()) {
			const line = lines.find((each) => each.id === button.dataset.item);
			if (!line) {
				continue;
			}
			if (line.checked) {
				const checked = line.checked(editor);
				button.setAttribute('aria-checked', String(checked));
				toggleParts(button, this.context.look, checked, 'rich-text-menu-item-active');
			}
			button.setAttribute('aria-disabled', String(!line.enabled(editor)));
		}
	}

	/**
	 * Draws the items of a menu as the cursor stands.
	 */
	private renderMenu(drawn: DrawnTool): void {
		if (this.isPalette(drawn.tool)) {
			this.renderPalette(drawn, drawn.tool);
			return;
		}
		const menu = drawn.menu!;
		menu.replaceChildren();
		this.paletteRows = [];
		for (const line of this.menuLines(drawn)) {
			const item = document.createElement('li');
			if (line === 'separator') {
				item.className = this.css('rich-text-menu-separator');
				item.setAttribute('role', 'separator');
				menu.append(item);
				continue;
			}
			item.setAttribute('role', 'none');
			const button = this.menuButton(line, this.css('rich-text-menu-item'));
			button.textContent = line.label;
			if (line.swatch) {
				button.prepend(this.swatch(line.swatch));
			}
			if (line.level) {
				button.dataset.level = String(line.level);
			}
			item.append(button);
			menu.append(item);
		}
	}

	/**
	 * @returns the button of an item of a menu, as the cursor stands, choosing it when pressed
	 */
	private menuButton(line: MenuEntry, className: string): HTMLButtonElement {
		const editor = this.editor;
		const button = document.createElement('button');
		button.type = 'button';
		button.className = className;
		button.dataset.item = line.id;
		button.tabIndex = -1;
		if (line.checked) {
			const checked = line.checked(editor);
			button.setAttribute('role', 'menuitemradio');
			button.setAttribute('aria-checked', String(checked));
			toggleParts(button, this.context.look, checked, 'rich-text-menu-item-active');
		} else {
			button.setAttribute('role', 'menuitem');
		}
		button.setAttribute('aria-disabled', String(!line.enabled(editor)));
		button.addEventListener('click', () => {
			if (button.getAttribute('aria-disabled') !== 'true') {
				this.closeMenu(false);
				line.run(this.editor, this.toolContext);
			}
		});
		return button;
	}

	private swatch(className: string): HTMLElement {
		const swatch = document.createElement('span');
		swatch.className = className;
		swatch.setAttribute('aria-hidden', 'true');
		return swatch;
	}

	/**
	 * Draws the color picker of a tool of colors as the cursor stands: the default on a line of
	 * its own, the colors of the palette as squares in a grid, then the last colors chosen that the
	 * palette has, under a label. Each square is named by its color; the color where the cursor is
	 * has a tick. The squares of the last colors are items of the menu too, of the same colors.
	 */
	private renderPalette(drawn: DrawnTool, mark: ColorMark): void {
		const menu = drawn.menu!;
		const texts = this.context.texts();
		const [reset, ...colors] = this.colorEntries(mark);
		menu.replaceChildren();
		const first = this.menuButton(reset!, this.css('rich-text-menu-item') +
			' antilia-rich-text-palette-default');
		const name = document.createElement('span');
		name.className = 'antilia-rich-text-palette-name';
		name.textContent = reset!.label;
		first.append(this.swatch(reset!.swatch!), name);
		first.insertAdjacentHTML('beforeend', TICK);
		menu.append(first);
		const rows: HTMLButtonElement[][] = [[first]];
		const columns = Math.max(Math.min(PALETTE_COLUMNS, colors.length), 1);
		// the columns of the grid; a property set by the script, which a strict policy allows
		menu.style.setProperty('--antilia-rich-text-palette-columns', String(columns));
		if (colors.length) {
			menu.append(this.paletteGrid(colors, columns, rows));
		}
		const recent = recentRichTextColors(mark).map((each) => colors.find((color) =>
			color.id === each)).filter((color): color is MenuEntry => !!color)
			.slice(0, RECENT_COLORS);
		if (recent.length) {
			const separator = document.createElement('div');
			separator.className = this.css('rich-text-menu-separator');
			separator.setAttribute('role', 'separator');
			const label = document.createElement('div');
			label.className = 'antilia-rich-text-palette-label';
			label.id = menu.id + '-recent';
			label.textContent = texts.recentColors;
			const grid = this.paletteGrid(recent, columns, rows);
			grid.classList.add('antilia-rich-text-palette-recent');
			grid.setAttribute('aria-labelledby', label.id);
			menu.append(separator, label, grid);
		}
		this.paletteRows = rows;
	}

	/**
	 * @returns a grid of the squares of colors, whose rows it adds to those of the color picker
	 */
	private paletteGrid(colors: readonly MenuEntry[], columns: number,
		rows: HTMLButtonElement[][]): HTMLElement {
		const grid = document.createElement('div');
		grid.className = 'antilia-rich-text-palette-grid';
		grid.setAttribute('role', 'group');
		colors.forEach((color, index) => {
			const button = this.menuButton(color, 'antilia-rich-text-palette-color');
			button.setAttribute('aria-label', color.label);
			button.title = color.label;
			const swatch = this.swatch(color.swatch!);
			swatch.insertAdjacentHTML('beforeend', TICK);
			button.append(swatch);
			if (index % columns === 0) {
				rows.push([]);
			}
			rows[rows.length - 1]!.push(button);
			grid.append(button);
		});
		return grid;
	}

	private menuItems(): HTMLButtonElement[] {
		return this.opened ? Array.from(this.opened.menu!.querySelectorAll<HTMLButtonElement>(
			'button[data-item]')) : [];
	}

	/**
	 * Opens the menu of a tool, the focus on its item in force, or on its first or last item.
	 */
	private openMenu(drawn: DrawnTool, focus: 'checked' | 'first' | 'last'): void {
		this.closeMenu(false);
		this.closeForm();
		this.opened = drawn;
		this.renderMenu(drawn);
		drawn.menu!.hidden = false;
		drawn.button.setAttribute('aria-expanded', 'true');
		const items = this.menuItems();
		const target = focus === 'last' ? items[items.length - 1] : focus === 'checked' ?
			items.find((item) => item.getAttribute('aria-checked') === 'true') ?? items[0] :
			items[0];
		target?.focus();
	}

	/**
	 * Closes the menu open, if any.
	 *
	 * @param refocus whether the focus goes back to the tool of the menu
	 */
	private closeMenu(refocus: boolean): void {
		const drawn = this.opened;
		if (!drawn) {
			return;
		}
		this.opened = null;
		this.paletteRows = [];
		drawn.menu!.hidden = true;
		drawn.menu!.replaceChildren();
		drawn.button.setAttribute('aria-expanded', 'false');
		if (refocus) {
			drawn.button.focus();
		}
	}

	private onMenuKeyDown(event: KeyboardEvent): void {
		const items = this.menuItems();
		const index = items.indexOf(document.activeElement as HTMLButtonElement);
		const move = (to: number) => {
			event.preventDefault();
			items[(to + items.length) % items.length]?.focus();
		};
		switch (event.key) {
		case 'ArrowDown':
			if (!this.moveInPalette(event, 1)) {
				move(index + 1);
			}
			break;
		case 'ArrowUp':
			if (!this.moveInPalette(event, -1)) {
				move(index - 1);
			}
			break;
		case 'ArrowRight':
			// along the grid of a color picker
			if (this.paletteRows.length) {
				move(index + 1);
			}
			break;
		case 'ArrowLeft':
			if (this.paletteRows.length) {
				move(index - 1);
			}
			break;
		case 'Home':
			move(0);
			break;
		case 'End':
			move(items.length - 1);
			break;
		case 'Escape':
			event.preventDefault();
			event.stopPropagation();
			this.closeMenu(true);
			break;
		case 'Tab':
			this.closeMenu(false);
			break;
		default:
			if (event.key.length === 1 && /\S/.test(event.key)) {
				const letter = event.key.toLowerCase();
				// by the names of the items, those of the squares of a color picker too
				const next = [...items.slice(index + 1), ...items.slice(0, index + 1)].find(
					(item) => (item.getAttribute('aria-label') ?? item.textContent ?? '').trim()
						.toLowerCase().startsWith(letter));
				if (next) {
					event.preventDefault();
					next.focus();
				}
			}
		}
		event.stopPropagation();
	}

	/**
	 * Moves the focus to the row below or above in a color picker, to the same column or the
	 * last of a shorter row, from the last row to the first and back.
	 *
	 * @returns whether the menu open is a color picker
	 */
	private moveInPalette(event: KeyboardEvent, step: 1 | -1): boolean {
		const rows = this.paletteRows;
		const focused = document.activeElement as HTMLButtonElement;
		const row = rows.findIndex((each) => each.includes(focused));
		if (row < 0) {
			return false;
		}
		event.preventDefault();
		const column = rows[row]!.indexOf(focused);
		const next = rows[(row + step + rows.length) % rows.length]!;
		next[Math.min(column, next.length - 1)]!.focus();
		return true;
	}

	private setCurrent(index: number): void {
		if (index !== this.current) {
			this.current = index;
			this.tools.forEach((drawn, each) => {
				drawn.button.tabIndex = each === index ? 0 : -1;
			});
		}
	}

	private onKeyDown(event: KeyboardEvent): void {
		const index = this.tools.findIndex((drawn) => drawn.button === document.activeElement);
		if (index < 0) {
			return;
		}
		const count = this.tools.length;
		const go = (to: number) => {
			event.preventDefault();
			const target = (to + count) % count;
			this.setCurrent(target);
			this.tools[target]!.button.focus();
		};
		const drawn = this.tools[index]!;
		switch (event.key) {
		case 'ArrowRight':
			go(index + 1);
			break;
		case 'ArrowLeft':
			go(index - 1);
			break;
		case 'Home':
			go(0);
			break;
		case 'End':
			go(count - 1);
			break;
		case 'ArrowDown':
		case 'ArrowUp':
			if (drawn.menu && drawn.button.getAttribute('aria-disabled') !== 'true') {
				event.preventDefault();
				this.openMenu(drawn, event.key === 'ArrowUp' ? 'last' : 'first');
			}
			break;
		default:
		}
	}

	/**
	 * Opens the form of the link at the cursor, from a tool or the keys.
	 *
	 * @param trigger the tool the popover points at, the link tool when left out
	 * @returns whether the form opened
	 */
	openLink(trigger = this.toolButton('link')): boolean {
		const editor = this.editor;
		if (!trigger || !editor.isEditable || !this.isEnabled('link')) {
			return false;
		}
		const texts = this.context.texts();
		const linked = editor.isActive('link');
		const attributes = editor.getAttributes('link') as { href?: string; target?: string };
		const form = this.form();
		const url = this.field(form, texts.linkUrl, 'url', attributes.href ?? '');
		const newTab = this.check(form, texts.linkNewTab, attributes.target === '_blank');
		const error = this.error(form);
		const buttons = this.buttons(texts, linked ? texts.removeLink : null, () => {
			this.closeForm();
			editor.chain().focus().extendMarkRange('link').unsetLink().run();
		});
		form.append(buttons);
		form.addEventListener('submit', (event) => {
			event.preventDefault();
			const href = url.value.trim();
			if (!href) {
				this.closeForm();
				editor.chain().focus().extendMarkRange('link').unsetLink().run();
				return;
			}
			if (!isSafeUrl(href)) {
				this.showError(url, error, texts.invalidUrl);
				return;
			}
			const target = newTab.checked ? '_blank' : null;
			this.closeForm();
			const chain = editor.chain().focus();
			if (editor.state.selection.empty && !editor.isActive('link')) {
				chain.insertContent({ type: 'text', text: href, marks: [{ type: 'link',
					attrs: { href, target } }] }).run();
			} else {
				chain.extendMarkRange('link').setLink({ href, target }).run();
			}
		});
		this.showForm(trigger, texts.link, form, url);
		return true;
	}

	/**
	 * Opens the form of the image selected, or of one to insert.
	 */
	private openImage(trigger: HTMLElement): void {
		const editor = this.editor;
		const texts = this.context.texts();
		const selected = editor.isActive('image');
		const attributes = selected ? editor.getAttributes('image') as { src?: string;
			alt?: string } : {};
		const form = this.form();
		const src = this.field(form, texts.imageUrl, 'url', attributes.src ?? '');
		const uploads = this.context.uploads?.() ?? null;
		const upload = uploads?.enabled() ? this.uploadButton(form, uploads) : null;
		const alt = this.field(form, texts.imageAlt, 'text', attributes.alt ?? '');
		const error = this.error(form);
		const buttons = this.buttons(texts, null, () => undefined);
		form.append(buttons);
		const apply = (address: string) => {
			this.closeForm();
			const values = { src: address, alt: alt.value.trim() || null };
			if (selected) {
				editor.chain().focus().updateAttributes('image', values).run();
			} else {
				editor.chain().focus().setImage({ src: address, alt: values.alt ?? undefined }).run();
			}
		};
		form.addEventListener('submit', (event) => {
			event.preventDefault();
			const address = src.value.trim();
			if (!address || !isSafeUrl(address)) {
				this.showError(src, error, texts.invalidUrl);
				return;
			}
			apply(address);
		});
		upload?.picker.addEventListener('change', () => {
			const file = upload.picker.files?.[0];
			upload.picker.value = '';
			if (!file || !uploads) {
				return;
			}
			const refused = uploads.refusal(file);
			if (refused) {
				this.showError(upload.button, error, refused);
				return;
			}
			error.hidden = true;
			upload.status.textContent = uploads.uploading(file);
			upload.status.hidden = false;
			const busy = (on: boolean) => {
				upload.button.disabled = on;
				buttons.querySelectorAll('button').forEach((button) => {
					button.disabled = on;
				});
			};
			busy(true);
			uploads.upload(file).then((address) => {
				// the user closed the form meanwhile: the image is not wanted any more
				if (form.isConnected) {
					apply(address);
				}
			}, (failure: unknown) => {
				busy(false);
				upload.status.hidden = true;
				if (form.isConnected) {
					this.showError(upload.button, error, failure instanceof Error ? failure.message :
						String(failure));
				}
			});
		});
		this.showForm(trigger, texts.image, form, src);
	}

	/**
	 * Adds a button choosing a file to upload to the form of an image, with the file field it
	 * opens, hidden, and the line telling the upload.
	 */
	private uploadButton(form: HTMLFormElement, uploads: UploadHost): { button: HTMLButtonElement;
		picker: HTMLInputElement; status: HTMLElement } {
		const texts = this.context.texts();
		const box = document.createElement('div');
		box.className = 'antilia-rich-text-form-upload';
		const button = document.createElement('button');
		button.type = 'button';
		button.className = lookClasses(this.context.look, 'button', 'button-secondary',
			'button-small');
		button.textContent = texts.uploadImage;
		const picker = document.createElement('input');
		picker.type = 'file';
		picker.accept = uploads.types().join(',');
		picker.hidden = true;
		picker.tabIndex = -1;
		picker.className = 'antilia-rich-text-form-file';
		button.addEventListener('click', () => picker.click());
		const status = document.createElement('p');
		status.className = 'antilia-rich-text-form-status';
		status.setAttribute('role', 'status');
		status.hidden = true;
		box.append(button, picker, status);
		form.append(box);
		return { button, picker, status };
	}

	private toolButton(tool: RichTextTool): HTMLButtonElement | undefined {
		return this.tools.find((drawn) => drawn.tool === tool)?.button;
	}

	private form(): HTMLFormElement {
		const form = document.createElement('form');
		form.className = this.css('rich-text-form');
		form.noValidate = true;
		return form;
	}

	private field(form: HTMLFormElement, text: string, type: 'url' | 'text',
		value: string): HTMLInputElement {
		const id = 'antilia-rich-text-field-' + (++fields);
		const label = document.createElement('label');
		label.className = this.css('rich-text-form-label');
		label.htmlFor = id;
		label.textContent = text;
		const input = document.createElement('input');
		input.className = this.css('rich-text-form-input');
		input.id = id;
		input.type = type;
		input.value = value;
		input.autocomplete = 'off';
		input.spellcheck = false;
		form.append(label, input);
		return input;
	}

	private check(form: HTMLFormElement, text: string, checked: boolean): HTMLInputElement {
		const label = document.createElement('label');
		label.className = this.css('rich-text-form-check');
		const input = document.createElement('input');
		input.type = 'checkbox';
		input.className = this.css('rich-text-form-checkbox');
		input.checked = checked;
		label.append(input, document.createTextNode(' ' + text));
		form.append(label);
		return input;
	}

	private error(form: HTMLFormElement): HTMLElement {
		const error = document.createElement('p');
		error.className = this.css('rich-text-form-error');
		error.id = 'antilia-rich-text-error-' + (++fields);
		error.setAttribute('role', 'alert');
		error.hidden = true;
		form.append(error);
		return error;
	}

	private showError(input: HTMLElement, error: HTMLElement, text: string): void {
		error.textContent = text;
		error.hidden = false;
		if (input instanceof HTMLInputElement) {
			input.setAttribute('aria-invalid', 'true');
		}
		input.setAttribute('aria-describedby', error.id);
		input.focus();
	}

	private buttons(texts: RichTextEditorTexts, remove: string | null,
		onRemove: () => void): HTMLElement {
		const look = this.context.look;
		const box = document.createElement('div');
		box.className = this.css('rich-text-form-buttons');
		const apply = document.createElement('button');
		apply.type = 'submit';
		apply.className = lookClasses(look, 'button', 'button-primary', 'button-small');
		apply.textContent = texts.apply;
		box.append(apply);
		if (remove) {
			const button = document.createElement('button');
			button.type = 'button';
			button.className = lookClasses(look, 'button', 'button-ghost', 'button-small');
			button.textContent = remove;
			button.addEventListener('click', onRemove);
			box.append(button);
		}
		return box;
	}

	private showForm(trigger: HTMLElement, title: string, form: HTMLFormElement,
		focus: HTMLInputElement): void {
		this.closeMenu(false);
		this.closeForm();
		const texts = this.context.texts();
		this.context.onForm?.(true);
		const popover = Popover.show(trigger, { title, body: form, placement: 'bottom-left',
			look: this.context.look, closeLabel: texts.close, maxWidth: '22rem',
			className: 'antilia-rich-text-popover',
			onClose: () => {
				if (openForm?.popover === popover) {
					openForm = null;
				}
				this.context.onForm?.(false);
			} });
		if (popover) {
			openForm = { popover, toolbar: this };
			focus.focus();
			focus.select();
		}
	}

	/**
	 * @returns whether a form of this toolbar is open
	 */
	isFormOpen(): boolean {
		return openForm?.toolbar === this && openForm.popover.isOpen();
	}

	/**
	 * Closes the form of this toolbar, if one is open.
	 */
	closeForm(): void {
		if (openForm?.toolbar === this) {
			const { popover } = openForm;
			openForm = null;
			popover.close();
		}
	}

	/**
	 * @returns the element of the form open, if this toolbar opened it
	 */
	formElement(): HTMLElement | null {
		return this.isFormOpen() ? openForm!.popover.element : null;
	}

	/**
	 * Takes the toolbar away: its menus, its form and its listeners.
	 */
	destroy(): void {
		this.closeMenu(false);
		this.closeForm();
		this.listeners.forEach(([target, type, listener, capture]) =>
			target.removeEventListener(type, listener, capture));
		this.listeners = [];
		this.element.remove();
	}
}
