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
 * the arrow keys go through its items, Enter or a click chooses one, Escape closes it. So does the
 * code block when the editor has languages for it: the menu makes the block a paragraph again, or
 * a block in plain text or in one of the languages. Without them, the tool makes a code block, or
 * takes it away. The link and the image open a form in a popover: the address, and whether the link
 * opens in a new tab or the text in place of the image. An address must be safe (see isSafeUrl):
 * http, https, mailto or a relative one. While the page uploads images, the form of the image has
 * a button choosing a file to upload, whose address the image then takes.
 *
 * The same class draws the bubble menu over a selection, with the tools of the inline formats.
 */

import type { Editor } from '@tiptap/core';

import { lookClasses, partClasses, toggleParts, type Look, type LookPart } from './look.js';
import { Popover } from './popover.js';
import { codeLanguage } from './rich-text-languages.js';
import { isSafeUrl, type HeadingLevel, type RichTextAlignment,
	type RichTextFeature } from './rich-text-schema.js';
import type { UploadHost } from './rich-text-upload.js';

/** A tool of the toolbar. */
export type RichTextTool = 'heading' | 'bold' | 'italic' | 'underline' | 'strike' | 'code' |
	'link' | 'bulletList' | 'orderedList' | 'taskList' | 'blockquote' | 'codeBlock' |
	'horizontalRule' | 'table' | 'image' | 'alignLeft' | 'alignCenter' | 'alignRight' |
	'alignJustify' | 'undo' | 'redo' | 'clearFormatting';

/** The tools, in the order of the toolbar by default. */
export const RICH_TEXT_TOOLS: readonly RichTextTool[] = Object.freeze(['heading', 'bold', 'italic',
	'underline', 'strike', 'code', 'link', 'bulletList', 'orderedList', 'taskList', 'blockquote',
	'codeBlock', 'horizontalRule', 'table', 'image', 'alignLeft', 'alignCenter', 'alignRight',
	'alignJustify', 'clearFormatting', 'undo', 'redo']);

/** The toolbar when the page gives none: the tools in groups. */
export const DEFAULT_RICH_TEXT_TOOLBAR: readonly (readonly RichTextTool[])[] = Object.freeze([
	['heading'],
	['bold', 'italic', 'underline', 'strike', 'code'],
	['link'],
	['bulletList', 'orderedList', 'taskList'],
	['blockquote', 'codeBlock', 'horizontalRule'],
	['table', 'image'],
	['alignLeft', 'alignCenter', 'alignRight', 'alignJustify'],
	['clearFormatting'],
	['undo', 'redo']
].map((group) => Object.freeze(group as RichTextTool[])));

/** The tools of the bubble menu over a selection. */
export const RICH_TEXT_BUBBLE_TOOLS: readonly RichTextTool[] = Object.freeze(['bold', 'italic',
	'underline', 'strike', 'code', 'link']);

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
	resize: 'Drag, or use the arrow keys, to resize the editor'
});

const svg = (paths: string) => '<svg viewBox="0 0 24 24" width="1em" height="1em" fill="none" ' +
	'stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" ' +
	'aria-hidden="true" focusable="false">' + paths + '</svg>';

/** The icons of the tools, and the arrow of a menu. */
export const RICH_TEXT_ICONS: Readonly<Record<Exclude<RichTextTool, 'heading'> | 'menu', string>> =
	Object.freeze({
		bold: svg('<path d="M7 5h6a3.5 3.5 0 0 1 0 7H7zM7 12h7a3.5 3.5 0 0 1 0 7H7z" ' +
			'stroke-width="2.5"/>'),
		italic: svg('<path d="M19 4h-9M14 20H5M15 4 9 20"/>'),
		underline: svg('<path d="M6 4v6a6 6 0 0 0 12 0V4M4 20h16"/>'),
		strike: svg('<path d="M16.5 7A4 4 0 0 0 13 5h-2a3.5 3.5 0 0 0-1.6 6.6M4 12h16M7.5 17a4 4 0 0 ' +
			'0 3.5 2h2a3.5 3.5 0 0 0 3-5.3"/>'),
		code: svg('<path d="m16 18 6-6-6-6M8 6l-6 6 6 6"/>'),
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
	shortcut?: string;
	active?: (editor: Editor) => boolean;
	enabled?: (editor: Editor) => boolean;
	run?: (editor: Editor) => void;
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

const SPECS: Readonly<Record<RichTextTool, ToolSpec>> = {
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
 * @returns whether a tool works with the features of an editor
 */
export function toolAvailable(tool: RichTextTool, features: ReadonlySet<RichTextFeature>): boolean {
	const spec = SPECS[tool];
	return !!spec && (spec.feature === null || features.has(spec.feature));
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
	/** Whether the item is the one in force, for a menu of choices. */
	checked?: (editor: Editor) => boolean;
	enabled: (editor: Editor) => boolean;
	run: (editor: Editor) => void;
}

type MenuLine = MenuEntry | 'separator';

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
	/** The languages of the menu of the code block; none for a tool making a code block. */
	codeLanguages?: () => readonly RichTextCodeLanguage[];
	/** The uploads of the form of an image; none while the page uploads no images. */
	uploads?: () => UploadHost | null;
	/** Called as a form opens and once it closed. */
	onForm?: (open: boolean) => void;
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

	private css(...parts: LookPart[]): string {
		return partClasses(this.context.look, ...parts);
	}

	/**
	 * @returns what a tool is: the code block opens a menu while the editor has languages for it
	 */
	private kindOf(tool: RichTextTool): ToolSpec['kind'] {
		if (tool === 'codeBlock' && this.codeLanguages().length) {
			return 'menu';
		}
		return SPECS[tool].kind;
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
		const spec = SPECS[tool];
		const kind = this.kindOf(tool);
		const button = document.createElement('button');
		button.type = 'button';
		button.className = this.css('rich-text-tool');
		button.dataset.tool = tool;
		button.tabIndex = -1;
		const name = texts[tool];
		button.setAttribute('aria-label', name);
		button.title = spec.shortcut ? name + ' (' + shortcutText(spec.shortcut) + ')' : name;
		let label: HTMLElement | null = null;
		if (tool === 'heading') {
			label = document.createElement('span');
			label.className = 'antilia-rich-text-tool-label';
			button.append(label);
			button.classList.add('antilia-rich-text-tool-wide');
		} else {
			button.insertAdjacentHTML('beforeend', RICH_TEXT_ICONS[tool]);
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
		const menu = document.createElement('ul');
		menu.className = this.css('rich-text-menu');
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
		const spec = SPECS[drawn.tool];
		if (spec.kind === 'form') {
			if (drawn.tool === 'link') {
				this.openLink(drawn.button);
			} else {
				this.openImage(drawn.button);
			}
			return;
		}
		spec.run?.(this.editor);
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
			const spec = SPECS[drawn.tool];
			if (!toolAvailable(drawn.tool, features)) {
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
		});
		if (this.opened) {
			this.refreshMenu(this.opened);
		}
	}

	private isEnabled(tool: RichTextTool): boolean {
		const editor = this.editor;
		if (!editor.isEditable) {
			return false;
		}
		if (tool === 'heading') {
			return this.blockEntries().some((entry) => entry.enabled(editor));
		}
		if (tool === 'table' || (tool === 'codeBlock' && this.kindOf(tool) === 'menu')) {
			return this.menuLines(this.tools.find((drawn) => drawn.tool === tool) ??
				{ tool }).some((entry) => entry !== 'separator' && entry.enabled(editor));
		}
		return SPECS[tool].enabled?.(editor) ?? true;
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

	private menuLines(drawn: Pick<DrawnTool, 'tool'>): MenuLine[] {
		switch (drawn.tool) {
		case 'heading':
			return this.blockEntries();
		case 'codeBlock':
			return this.codeEntries();
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
		const menu = drawn.menu!;
		const editor = this.editor;
		menu.replaceChildren();
		for (const line of this.menuLines(drawn)) {
			const item = document.createElement('li');
			if (line === 'separator') {
				item.className = this.css('rich-text-menu-separator');
				item.setAttribute('role', 'separator');
				menu.append(item);
				continue;
			}
			item.setAttribute('role', 'none');
			const button = document.createElement('button');
			button.type = 'button';
			button.className = this.css('rich-text-menu-item');
			button.dataset.item = line.id;
			button.tabIndex = -1;
			button.textContent = line.label;
			if (line.level) {
				button.dataset.level = String(line.level);
			}
			if (line.checked) {
				const checked = line.checked(editor);
				button.setAttribute('role', 'menuitemradio');
				button.setAttribute('aria-checked', String(checked));
				toggleParts(button, this.context.look, checked, 'rich-text-menu-item-active');
			} else {
				button.setAttribute('role', 'menuitem');
			}
			const enabled = line.enabled(editor);
			button.setAttribute('aria-disabled', String(!enabled));
			button.addEventListener('click', () => {
				if (button.getAttribute('aria-disabled') !== 'true') {
					this.closeMenu(false);
					line.run(this.editor);
				}
			});
			item.append(button);
			menu.append(item);
		}
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
			move(index + 1);
			break;
		case 'ArrowUp':
			move(index - 1);
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
				const next = [...items.slice(index + 1), ...items.slice(0, index + 1)].find(
					(item) => item.textContent?.trim().toLowerCase().startsWith(letter));
				if (next) {
					event.preventDefault();
					next.focus();
				}
			}
		}
		event.stopPropagation();
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
