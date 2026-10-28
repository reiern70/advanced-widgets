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
 * CKEditor 5 as a rich text editor of the components, beside Tiptap's (rich-text-editor.ts) and
 * with its contract, so that a page can switch between the two: CKEditor's ClassicEditor, with its
 * own toolbar, balloons and dialogs, in the colors of the theme it is in (ck-editor.css).
 *
 * This module holds no code of CKEditor, which is licensed under the GPL or under the commercial
 * terms of CKSource: the page brings it, and its license key. The classes are those of the global
 * CKEDITOR, which the browser build of the ckeditor5 package defines (ckeditor5.umd.js), or those
 * of the page's own build (build); the license key is 'GPL' for the open-source distribution, or
 * the key CKSource gave (licenseKey). The page brings CKEditor's style sheet too, ckeditor5.css,
 * and, for its texts in another language, its translations (language, translations).
 *
 * new CKEditor(element, options) works on a text area, which it hides and wraps in the element of
 * the editor, keeping the text in it: a form posts it, and the text area tells the page of the
 * user's changes with the events input, and change once the editor loses the focus, as a text
 * area does. It works on any other element too, an empty one React or Svelte renders, which
 * becomes the element of the editor; a text area in it, if any, is then the one the editor keeps
 * the text in. A text area a server renders may have the class antilia-ck-editor-loading: while
 * scripts run, the style sheet hides it in the room of the editor.
 *
 * CKEditor starts asynchronously: ready resolves once it has, onReady is called then; until then
 * getValue() is the text given. Should it not start, without CKEditor in the page or with a
 * license key it refuses, the text area shows again, to type the text in, and onError tells why.
 *
 * The text is HTML, or Markdown with CKEditor's Markdown plugin (format). What it may hold are the
 * features of the Tiptap editor (rich-text-schema.ts), each made by CKEditor's plugins
 * (CK_EDITOR_FEATURE_PLUGINS), all of them unless the page leaves some out; the toolbar has their
 * tools unless the page gives CKEditor's own (toolbar). More plugins (plugins) and any other
 * configuration of CKEditor (config) are the page's.
 *
 * The HTML has no style attribute, which a Content Security Policy without 'unsafe-inline' would
 * block: text aligns by classes, those of the Tiptap editor, and images have no width or height,
 * for which CKEditor writes a style. The HTML a page gives and the user pastes is made quiet
 * before CKEditor reads it (quietHtml), its style attributes moved aside: the browser would
 * report each.
 *
 * As the Tiptap editor, it keeps its state, what is selected, how far the text is scrolled and
 * the size the user gave it, by the bar along its bottom; it tells whether the text has changes
 * not saved, as data-modified on its element, which tabs and windows of the desktop show; Ctrl+S
 * tells the page to save; and it fires the events of RICH_TEXT_EDITOR_EVENTS, which the events
 * of Wicket's editor listen to, and those of the types of events registered, shared with the
 * Tiptap editor (registerRichTextEvent, see rich-text-events.ts), from its own or by trigger().
 * The positions of the selection count the places between the nodes too, as ProseMirror's do.
 */

import { getHost, onRemoving, type Unsubscribe } from './host.js';
import { lookOf, partClasses, type Look, type LookPart } from './look.js';
import { Resizable, type ResizableSize, type ResizeAxis } from './resizable.js';
import { RICH_TEXT_EDITOR_EVENTS, sameState, type RichTextEditorSelection,
	type RichTextEditorState, type RichTextEditorStateChange } from './rich-text-contract.js';
import { RichTextEventTypes } from './rich-text-events.js';
import { quietHtml, RICH_TEXT_ALIGN_CLASSES } from './rich-text-html.js';
import type { HeadingLevel, RichTextFeature } from './rich-text-schema.js';

/* What the glue uses of CKEditor, as its classes have it: no more, so that any build fits. */

/** Something that fires events: CKEditor's emitters and observables. */
export interface CKEditorEmitter {
	on(event: string, callback: (...args: any[]) => void,
		options?: { priority?: string | number }): void;
}

/** A node of CKEditor's model: an element, or text. */
export interface CKEditorModelNode {
	is(type: string): boolean;
	readonly startOffset: number | null;
	readonly offsetSize: number;
	/** The characters of text. */
	readonly data?: string;
}

/** An element of CKEditor's model, its root among them. */
export interface CKEditorModelElement extends CKEditorModelNode {
	readonly maxOffset: number;
	getChildren(): Iterable<CKEditorModelNode>;
	getChild(index: number): CKEditorModelNode | null;
	offsetToIndex(offset: number): number;
}

/** A place in CKEditor's model. */
export interface CKEditorModelPosition {
	readonly path: readonly number[];
	isBefore(other: CKEditorModelPosition): boolean;
}

/** What is between two places in CKEditor's model. */
export interface CKEditorModelRange {
	readonly start: CKEditorModelPosition;
	readonly end: CKEditorModelPosition;
}

/** What CKEditor's model writes with. */
export interface CKEditorModelWriter {
	setSelection(range: CKEditorModelRange, options?: { backward?: boolean }): void;
}

/** CKEditor's model. */
export interface CKEditorModel {
	readonly document: CKEditorEmitter & {
		readonly selection: CKEditorEmitter & {
			readonly anchor: CKEditorModelPosition | null;
			readonly focus: CKEditorModelPosition | null;
		};
		getRoot(): CKEditorModelElement | null;
	};
	readonly schema: {
		addAttributeCheck(check: (context: { endsWith(query: string): boolean },
			attribute: string) => boolean | undefined): void;
		getNearestSelectionRange(position: CKEditorModelPosition,
			direction?: string): CKEditorModelRange | null;
	};
	change(callback: (writer: CKEditorModelWriter) => void): void;
	createPositionFromPath(root: CKEditorModelElement,
		path: readonly number[]): CKEditorModelPosition;
	createRange(start: CKEditorModelPosition, end?: CKEditorModelPosition): CKEditorModelRange;
}

/** An editor of CKEditor, as ClassicEditor.create() makes it. */
export interface CKEditorInstance {
	readonly model: CKEditorModel;
	readonly data: {
		parse(data: string): unknown;
		stringify(fragment: unknown): string;
	};
	readonly editing: { readonly view: { focus(): void; readonly document: CKEditorEmitter } };
	readonly conversion: {
		for(group: string): { add(helper: (dispatcher: CKEditorEmitter) => void): unknown };
	};
	readonly ui: {
		readonly element: HTMLElement | null;
		readonly view: { readonly editable: { readonly element: HTMLElement | null } };
		readonly focusTracker: CKEditorEmitter & { readonly isFocused: boolean };
	};
	readonly keystrokes: {
		set(keystroke: string, callback: (data: unknown, cancel: () => void) => void): void;
	};
	getData(): string;
	setData(data: string): void;
	enableReadOnlyMode(lockId: string): void;
	disableReadOnlyMode(lockId: string): void;
	destroy(): Promise<unknown>;
}

/** CKEditor's ClassicEditor: an editor with a toolbar above the text. */
export interface CKEditorClass {
	create(element: HTMLElement, config: Record<string, unknown>): Promise<CKEditorInstance>;
}

/**
 * The classes of CKEditor: the global CKEDITOR of its browser build, or an object of the page's
 * own with ClassicEditor and the plugins, by their names (Bold, Table...).
 */
export interface CKEditorBuild {
	readonly ClassicEditor: CKEditorClass;
	readonly [name: string]: unknown;
}

/** A plugin of CKEditor: its class, or its name in the build. */
export type CKEditorPlugin = string | object;

/** The features CKEditor makes: those of the Tiptap editor it has plugins for. */
export type CKEditorFeature = Extract<RichTextFeature, 'heading' | 'bold' | 'italic' |
	'underline' | 'strike' | 'code' | 'link' | 'bulletList' | 'orderedList' | 'taskList' |
	'blockquote' | 'codeBlock' | 'horizontalRule' | 'table' | 'image' | 'textAlign'>;

/** The plugins of CKEditor making each feature, by their names in the build. */
export const CK_EDITOR_FEATURE_PLUGINS: Readonly<Record<CKEditorFeature, readonly string[]>> =
	Object.freeze({
		heading: ['Heading'],
		bold: ['Bold'],
		italic: ['Italic'],
		underline: ['Underline'],
		strike: ['Strikethrough'],
		code: ['Code'],
		link: ['Link', 'AutoLink'],
		bulletList: ['List'],
		orderedList: ['List'],
		taskList: ['List', 'TodoList'],
		blockquote: ['BlockQuote'],
		codeBlock: ['CodeBlock'],
		horizontalRule: ['HorizontalLine'],
		table: ['Table', 'TableToolbar'],
		image: ['Image', 'ImageToolbar', 'ImageCaption', 'ImageTextAlternative', 'ImageInsertViaUrl',
			'AutoImage'],
		textAlign: ['Alignment']
	});

/** The features CKEditor makes, all of them: those of an editor of HTML when left out. */
export const CK_EDITOR_FEATURES = Object.freeze(Object.keys(CK_EDITOR_FEATURE_PLUGINS)) as
	readonly CKEditorFeature[];

/** The features Markdown has no syntax for, which an editor of Markdown leaves out. */
const NOT_IN_MARKDOWN: readonly CKEditorFeature[] = ['underline', 'textAlign'];

/** The plugins of every editor: typing, the clipboard, undo, paragraphs, Markdown's marks. */
export const CK_EDITOR_BASE_PLUGINS: readonly string[] = Object.freeze(['Essentials', 'Paragraph',
	'Autoformat', 'PasteFromOffice']);

/** The tools of the default toolbar, in groups, as CKEditor names them, by their features. */
const TOOLBAR: readonly (readonly (readonly [CKEditorFeature | null, string])[])[] = [
	[['heading', 'heading']],
	[['bold', 'bold'], ['italic', 'italic'], ['underline', 'underline'],
		['strike', 'strikethrough'], ['code', 'code']],
	[['link', 'link']],
	[['bulletList', 'bulletedList'], ['orderedList', 'numberedList'], ['taskList', 'todoList']],
	[['blockquote', 'blockQuote'], ['codeBlock', 'codeBlock'], ['horizontalRule', 'horizontalLine'],
		['table', 'insertTable'], ['image', 'insertImage']],
	[['textAlign', 'alignment']],
	[[null, 'undo'], [null, 'redo']]
];

/** What the editor says, its own texts: CKEditor's are translated by CKEditor. */
export interface CKEditorTexts {
	characters: string;
	character: string;
	words: string;
	word: string;
	resize: string;
	/** The switch of the form of a link opening it in a new tab. */
	linkNewTab: string;
}

export const DEFAULT_CK_EDITOR_TEXTS: Readonly<CKEditorTexts> = Object.freeze({
	characters: '{0} characters',
	character: '{0} character',
	words: '{0} words',
	word: '{0} word',
	resize: 'Drag, or use the arrow keys, to resize the editor',
	linkNewTab: 'Open in a new tab'
});

/**
 * What an editor holds and shows, and what it lets the user do.
 */
export interface CKEditorOptions {
	/** The classes of CKEditor: the global CKEDITOR of its browser build when left out. */
	build?: CKEditorBuild;
	/** The license key: 'GPL' for the open-source distribution, or the key CKSource gave. */
	licenseKey?: string;
	/** The language of CKEditor's texts, en when left out; its translations must be there. */
	language?: string;
	/** CKEditor's translations, imported from ckeditor5/translations; the browser's are global. */
	translations?: readonly unknown[];
	/** The text; that of the text area, or none, when left out. */
	value?: string;
	/** The text last saved, against which the editor tells of changes; the text when left out. */
	saved?: string;
	/** How the editor keeps the text: html when left out, or markdown. */
	format?: 'html' | 'markdown';
	/**
	 * What the text may hold: all the features when left out, but for Markdown underlined text
	 * and alignment, which Markdown has no syntax for. Those CKEditor does not make are left out.
	 */
	features?: readonly RichTextFeature[];
	/** The levels of the headings, 1 to 4 when left out. */
	headingLevels?: readonly HeadingLevel[];
	/**
	 * The tools of the toolbar, as CKEditor names them, '|' between groups: those of the features
	 * when left out; false for no toolbar.
	 */
	toolbar?: readonly string[] | false;
	/** More plugins: their classes, or their names in the build. */
	plugins?: readonly CKEditorPlugin[];
	/** More of CKEditor's configuration, which takes the place of the editor's own. */
	config?: Readonly<Record<string, unknown>>;
	/** Whether the user cannot change the text, false when left out; read only text areas are so. */
	readOnly?: boolean;
	/** The text shown while the editor is empty. */
	placeholder?: string;
	/** The accessible name of the text; that of the labels of the text area when left out. */
	label?: string;
	/** Whether the editor counts the characters and the words below the text, false when left out. */
	count?: boolean;
	/** The dimensions the user resizes the editor in, vertical when left out; false for none. */
	resizable?: ResizeAxis | false;
	/** The limits of the size the user gives the editor, in pixels. */
	minWidth?: number;
	minHeight?: number;
	maxWidth?: number | null;
	maxHeight?: number | null;
	/** The state the editor starts in: the selection, the scroll position and the size. */
	state?: RichTextEditorState;
	/** How long after the user changed the state onStateChange reports it, 400 ms when left out. */
	stateDelay?: number;
	/** What the editor says of its own, in English when left out. */
	texts?: Partial<CKEditorTexts>;
	/** The look of the editor; that of the element it is in when left out (see lookOf). */
	look?: Look;
	/** Called with CKEditor's editor once it started. */
	onReady?: (editor: CKEditorInstance) => void;
	/** Called with what went wrong when CKEditor could not start. */
	onError?: (error: unknown) => void;
	/** Called with the text each time the user changed it. */
	onChange?: (value: string) => void;
	/** Called when the editor got the focus. */
	onFocus?: () => void;
	/** Called with the text when the editor lost the focus. */
	onBlur?: (value: string) => void;
	/** Called with the selection each time the user moved the cursor or selected text. */
	onSelectionChange?: (selection: RichTextEditorSelection) => void;
	/**
	 * Called with the state a moment after the user changed it, as the editor loses the focus and
	 * as the page goes, when it changed since it was last reported.
	 */
	onStateChange?: (state: RichTextEditorState, change: RichTextEditorStateChange) => void;
	/** Called when the text came to have changes not saved, or no longer has. */
	onModifiedChange?: (modified: boolean) => void;
	/** Called with the text when the user pressed Ctrl+S, or Cmd+S. */
	onSave?: (value: string) => void;
	/** Called with the size once the user has resized the editor. */
	onResize?: (size: ResizableSize) => void;
}

/** What the events of the editor carry, besides what each tells. */
export interface CKEditorEventDetail {
	editor: CKEditor;
	selection?: RichTextEditorSelection;
	state?: RichTextEditorState;
	modified?: boolean;
	size?: ResizableSize;
}

/** The classes of the editor and its states. */
export const CK_EDITOR_CLASSES = Object.freeze({
	editor: 'antilia-ck-editor',
	field: 'antilia-ck-editor-field',
	/** The element CKEditor is made on, which it hides, its UI after it. */
	source: 'antilia-ck-editor-source',
	/** Of a text area until the editor is made of it, which the style sheet hides meanwhile. */
	loading: 'antilia-ck-editor-loading',
	readOnly: 'antilia-ck-editor-readonly',
	disabled: 'antilia-ck-editor-disabled',
	focused: 'antilia-ck-editor-focused'
});

const DEFAULTS = { stateDelay: 400, resizable: 'vertical' as ResizeAxis, minWidth: 200,
	minHeight: 96, headingLevels: [1, 2, 3, 4] as readonly HeadingLevel[] };

/** The lock of CKEditor's read only mode the editor holds. */
const READ_ONLY_LOCK = 'antilia-ck-editor';

/** The options that make CKEditor's configuration: changing one makes it anew. */
const CONFIG_OPTIONS: readonly (keyof CKEditorOptions)[] = ['format', 'features',
	'headingLevels', 'toolbar', 'licenseKey', 'language', 'placeholder', 'label'];

/** The options compared by what they are, not by their value. */
const OBJECT_OPTIONS: readonly (keyof CKEditorOptions)[] = ['build', 'plugins', 'config',
	'translations'];

/** CKEditor's ids of the items of lists, new each time it reads them: no change of the text. */
const LIST_ITEM_ID = / data-list-item-id="[^"]*"/g;

/** The images of CKEditor's model, and their sizes, for which CKEditor writes a style. */
const IMAGES = ['imageBlock', 'imageInline'];
const SIZES = ['width', 'height'];

function resolve(target: HTMLElement | string): HTMLElement | null {
	return typeof target === 'string' ? document.getElementById(target) : target;
}

/**
 * @returns a text with its placeholders, {0} and {1}, replaced
 */
function format(text: string, ...values: (string | number)[]): string {
	return text.replace(/\{(\d)\}/g, (all, index: string) => {
		const value = values[Number(index)];
		return value === undefined ? all : String(value);
	});
}

/**
 * @returns how many places a node of the model takes: its characters for text, its content and
 *          the places before and after it for an element, as ProseMirror counts
 */
function sizeOf(node: CKEditorModelNode): number {
	if (node.is('$text')) {
		return node.offsetSize;
	}
	let size = 2;
	for (const child of (node as CKEditorModelElement).getChildren()) {
		size += sizeOf(child);
	}
	return size;
}

/**
 * @returns the place a path of the model is at, counted as ProseMirror counts
 */
export function offsetOfPath(root: CKEditorModelElement, path: readonly number[]): number {
	let offset = 0;
	let parent = root;
	for (let level = 0; level < path.length; level++) {
		const at = path[level]!;
		for (const child of parent.getChildren()) {
			const start = child.startOffset ?? 0;
			if (start >= at) {
				break;
			}
			offset += child.is('$text') ? Math.min(child.offsetSize, at - start) : sizeOf(child);
		}
		if (level < path.length - 1) {
			const child = parent.getChild(parent.offsetToIndex(at));
			if (!child || child.is('$text')) {
				break;
			}
			parent = child as CKEditorModelElement;
			offset += 1;
		}
	}
	return offset;
}

/**
 * @returns the path of the model at a place counted as ProseMirror counts; the end of the text
 *          for one past it
 */
export function pathOfOffset(root: CKEditorModelElement, offset: number): number[] {
	const path: number[] = [];
	let parent = root;
	let rest = Math.max(0, Math.round(offset));
	for (;;) {
		let inside: CKEditorModelElement | null = null;
		for (const child of parent.getChildren()) {
			const start = child.startOffset ?? 0;
			if (child.is('$text')) {
				if (rest <= child.offsetSize) {
					path.push(start + rest);
					return path;
				}
				rest -= child.offsetSize;
				continue;
			}
			if (rest === 0) {
				path.push(start);
				return path;
			}
			const size = sizeOf(child);
			if (rest < size) {
				path.push(start);
				inside = child as CKEditorModelElement;
				rest -= 1;
				break;
			}
			rest -= size;
		}
		if (!inside) {
			path.push(parent.maxOffset);
			return path;
		}
		parent = inside;
	}
}

/**
 * @returns the characters of an element of the model, its blocks apart
 */
function textOf(element: CKEditorModelElement, parts: string[] = []): string[] {
	for (const child of element.getChildren()) {
		if (child.is('$text')) {
			parts.push(child.data ?? '');
		} else {
			textOf(child as CKEditorModelElement, parts);
			parts.push('\n');
		}
	}
	return parts;
}

/**
 * @returns the characters of an element of the model between two places, counted as ProseMirror
 *          counts, a separator between the texts of two blocks
 */
function textBetween(element: CKEditorModelElement, from: number, to: number,
	separator: string): string {
	let text = '';
	// whether a block started or ended since the last characters
	let apart = false;
	const walk = (parent: CKEditorModelElement, start: number) => {
		let offset = start;
		for (const child of parent.getChildren()) {
			if (offset >= to) {
				return;
			}
			if (child.is('$text')) {
				const data = child.data ?? '';
				const first = Math.max(from - offset, 0);
				const last = Math.min(to - offset, data.length);
				if (first < last) {
					text += (apart && text ? separator : '') + data.slice(first, last);
					apart = false;
				}
				offset += child.offsetSize;
				continue;
			}
			const size = sizeOf(child);
			if (offset + size > from) {
				apart = true;
				walk(child as CKEditorModelElement, offset + 1);
				apart = true;
			}
			offset += size;
		}
	};
	walk(element, 0);
	return text;
}

/**
 * What an editor rendered anew in the same place goes on with: its text, the text last saved,
 * its state, and the state it last reported.
 */
interface Continuation {
	value: string;
	saved: string;
	state: RichTextEditorState;
	reported: RichTextEditorState | null;
}

/** The editors, by their element and their text area. */
const instances = new WeakMap<Element, CKEditor>();

/** What editors the page took away go on with, by the id of their text area or element. */
const continuations = new Map<string, Continuation>();

export class CKEditor {
	/** The element of the editor, holding the text area, if any, and CKEditor's. */
	readonly element: HTMLElement;

	/** The text area the editor keeps the text in, if it has one. */
	readonly field: HTMLTextAreaElement | null;

	/** Resolves with the editor once CKEditor started; rejects with why it could not. */
	readonly ready: Promise<CKEditor>;

	/** CKEditor's editor, null until it started; made anew when its configuration changes. */
	editor: CKEditorInstance | null = null;

	options: CKEditorOptions;

	/** The text last saved, as the editor writes it. */
	private saved: string;

	/** Whether the text last saved is as the editor writes it, which needs CKEditor. */
	private savedCanonical = false;

	private modified = false;

	private readonly look: Look;

	/** Whether the element was made by the editor, and is taken away again by destroy(). */
	private readonly ownElement: boolean;

	/** Whether the text area was hidden before the editor hid it. */
	private readonly fieldHidden: HTMLElement['hidden'] = false;

	/** The element CKEditor is made on, which it hides, its UI after it. */
	private source: HTMLElement | null = null;

	private status: HTMLElement | null = null;

	private resizable: Resizable | null = null;

	/** The size the editor has, as the user gave it. */
	private size: ResizableSize = { width: null, height: null };

	/** How far the text is scrolled, while the editor is shown. */
	private scroll = { top: 0, left: 0 };

	/** The selection CKEditor takes once it started, or again. */
	private pendingSelection: RichTextEditorSelection | null = null;

	/** Whether the editor is shown, not in a hidden tab for example. */
	private shown = false;

	/** The state last reported, null before. */
	private reported: RichTextEditorState | null = null;

	/** Whether the text was reported, with the state, since the user last changed it. */
	private textReported = true;

	private focused = false;

	/** The text when the editor got the focus, for the event change. */
	private focusValue: string | null = null;

	/** The text the editor has, as it writes it. */
	private value: string;

	/** The selection last told of. */
	private selected: RichTextEditorSelection = { anchor: 0, head: 0 };

	/** Whether the page, not the user, changes CKEditor's text or selection: no events. */
	private external = false;

	/** Counts the CKEditors made, so that one made for options since changed goes. */
	private generation = 0;

	private timer: ReturnType<typeof setTimeout> | null = null;

	private listeners: Array<[EventTarget, string, EventListener, boolean]> = [];

	/** The listeners of the element CKEditor scrolls its text in, made anew with CKEditor. */
	private scrollerListeners: Array<[EventTarget, string, EventListener]> = [];

	private observer: ResizeObserver | null = null;

	private stopWatchingRemoval: Unsubscribe | null = null;

	/** Whether the editor is firing an event on its text area, which it then does not hear. */
	private dispatching = false;

	private destroyed = false;

	/** The types of events registered, which fire on the editor, and what each keeps for it. */
	private readonly eventTypes = new RichTextEventTypes(this, (name, detail) =>
		!this.destroyed && this.emit(name, detail));

	/**
	 * @param target a text area, or an element: the editor's, with the text area to keep the text
	 *        in, if any
	 * @param options CKEditor and its license key, the text, its format and the features; the
	 *        state it starts in and the callbacks
	 */
	constructor(target: HTMLElement, options: CKEditorOptions = {}) {
		this.options = { ...options };
		this.look = options.look ?? lookOf(target);
		const field = target instanceof HTMLTextAreaElement ? target :
			target.querySelector<HTMLTextAreaElement>(':scope > textarea');
		this.field = field;
		// rendered anew in the same place: the editor there gives way, telling what it was at
		let previous = instances.get(target) ?? (field ? instances.get(field) : undefined);
		let element = target;
		this.ownElement = false;
		if (field && target === field) {
			const parent = field.parentElement;
			if (parent && previous?.element === parent) {
				element = parent;
				this.ownElement = previous.ownElement;
			} else if (parent && parent.classList.contains(CK_EDITOR_CLASSES.editor)) {
				element = parent;
			} else {
				element = document.createElement('div');
				field.parentNode?.insertBefore(element, field);
				element.append(field);
				this.ownElement = true;
			}
		}
		this.element = element;
		previous = previous ?? instances.get(element);
		const id = field?.id || element.id;
		const before = previous?.continuation() ?? (id ? continuations.get(id) : undefined);
		if (id) {
			continuations.delete(id);
		}
		previous?.release();
		Array.from(element.children).forEach((child) => {
			if (child !== field) {
				child.remove();
			}
		});
		instances.set(element, this);
		if (field) {
			instances.set(field, this);
			this.fieldHidden = field.hidden;
		}
		this.value = options.value ?? field?.value ?? '';
		this.saved = options.saved !== undefined ? options.saved :
			field ? field.defaultValue : this.value;
		let state = options.state;
		// the page knows the state it gives
		this.reported = options.state ?? null;
		if (before && before.value === this.value) {
			// the same text again: the editor goes on as it was, unless the page has another state
			// than the one it was told
			if (options.saved === undefined) {
				this.saved = before.saved;
			}
			if (!state || !before.reported || sameState(state, before.reported)) {
				state = before.state;
				this.reported = before.reported;
			}
		}
		this.pendingSelection = state?.selection ?? null;
		this.selected = state?.selection ?? { anchor: 0, head: 0 };
		this.scroll = { top: Math.max(state?.scrollTop ?? 0, 0),
			left: Math.max(state?.scrollLeft ?? 0, 0) };
		this.size = { width: state?.width ?? null, height: state?.height ?? null };
		this.render();
		this.renderStatus();
		this.renderResizable();
		this.syncField(this.value);
		this.renderModified(this.value, false);
		this.bind();
		this.stopWatchingRemoval = onRemoving(field ?? element, () => this.remove());
		this.ready = this.start();
		// a page not waiting for it is told by onError and the log
		this.ready.catch(() => undefined);
	}

	/**
	 * Makes an editor of a text area or an element, as Wicket does.
	 *
	 * @param target the text area or the element, or its id
	 * @param options CKEditor and its license key, the text, its format and the features; the
	 *        state and the callbacks
	 * @returns the editor, null when there is no such element
	 */
	static attach(target: HTMLElement | string, options?: CKEditorOptions): CKEditor | null {
		const resolved = resolve(target);
		return resolved ? new CKEditor(resolved, options) : null;
	}

	/**
	 * @param target the text area or the element of an editor, or its id
	 * @returns the editor, if there is one
	 */
	static of(target: HTMLElement | string | null | undefined): CKEditor | null {
		const element = typeof target === 'string' ? document.getElementById(target) : target;
		return (element && instances.get(element)) || null;
	}

	private css(...parts: LookPart[]): string {
		return partClasses(this.look, ...parts);
	}

	private isReadOnly(): boolean {
		return !!this.options.readOnly || !!this.field?.readOnly || this.isDisabled();
	}

	private isDisabled(): boolean {
		return !!this.field?.disabled;
	}

	/**
	 * @returns how the editor keeps the text
	 */
	format(): 'html' | 'markdown' {
		return this.options.format === 'markdown' ? 'markdown' : 'html';
	}

	/**
	 * @returns what the text may hold, among the features CKEditor makes
	 */
	features(): ReadonlySet<CKEditorFeature> {
		const given = this.options.features;
		if (given) {
			return new Set(CK_EDITOR_FEATURES.filter((feature) => given.includes(feature)));
		}
		return new Set(this.format() === 'markdown' ? CK_EDITOR_FEATURES.filter((feature) =>
			!NOT_IN_MARKDOWN.includes(feature)) : CK_EDITOR_FEATURES);
	}

	private texts(): CKEditorTexts {
		return { ...DEFAULT_CK_EDITOR_TEXTS, ...this.options.texts };
	}

	/**
	 * @returns the dimensions the user resizes the editor in, null for none
	 */
	private axis(): ResizeAxis | null {
		const axis = this.options.resizable;
		return axis === false ? null : axis ?? DEFAULTS.resizable;
	}

	/**
	 * @returns the classes of CKEditor: those given, or the global CKEDITOR
	 */
	private build(): CKEditorBuild | null {
		const build = this.options.build ??
			(globalThis as { CKEDITOR?: CKEditorBuild }).CKEDITOR;
		return build && typeof build.ClassicEditor?.create === 'function' ? build : null;
	}

	private render(): void {
		const element = this.element;
		element.classList.add(...this.css('ck-editor').split(' '));
		element.classList.toggle(CK_EDITOR_CLASSES.readOnly, this.isReadOnly());
		element.classList.toggle(CK_EDITOR_CLASSES.disabled, this.isDisabled());
		const field = this.field;
		if (field) {
			field.classList.remove(CK_EDITOR_CLASSES.loading);
			field.classList.add(...this.css('ck-editor-field').split(' '));
			field.hidden = true;
			field.tabIndex = -1;
			field.setAttribute('aria-hidden', 'true');
		}
	}

	/**
	 * @returns the accessible name of the text: the label of the options, or the text of the
	 *          labels of the text area, its aria-label or its title
	 */
	private label(): string | null {
		if (this.options.label) {
			return this.options.label;
		}
		const field = this.field;
		if (!field) {
			return null;
		}
		const texts = Array.from(field.labels ?? []).map((label) => (label.textContent ?? '')
			.replace(/\s+/g, ' ').trim()).filter(Boolean);
		return texts.join(' ') || field.getAttribute('aria-label') || field.title || null;
	}

	/**
	 * @returns the plugins of CKEditor: those of every editor, of the features, of the format and
	 *          those given; a name the build has no plugin of is told of and left out
	 */
	private plugins(build: CKEditorBuild): object[] {
		const names = [...CK_EDITOR_BASE_PLUGINS];
		this.features().forEach((feature) => names.push(...CK_EDITOR_FEATURE_PLUGINS[feature]));
		if (this.format() === 'markdown') {
			names.push('Markdown');
		}
		const plugins: object[] = [];
		const add = (plugin: CKEditorPlugin) => {
			const found = typeof plugin === 'string' ? build[plugin] : plugin;
			if (typeof found === 'function' || (typeof found === 'object' && found !== null)) {
				if (!plugins.includes(found)) {
					plugins.push(found);
				}
			} else {
				getHost().log.error('CKEditor has no plugin ' + String(plugin));
			}
		};
		names.forEach(add);
		(this.options.plugins ?? []).forEach(add);
		return plugins;
	}

	/**
	 * @returns the tools of the toolbar: those given, or those of the features, '|' between groups
	 */
	toolbarItems(): string[] {
		const given = this.options.toolbar;
		if (given === false) {
			return [];
		}
		if (given) {
			return [...given];
		}
		const features = this.features();
		const items: string[] = [];
		TOOLBAR.forEach((group) => {
			const tools = group.filter(([feature]) => feature === null || features.has(feature))
				.map(([, tool]) => tool);
			if (tools.length) {
				if (items.length) {
					items.push('|');
				}
				items.push(...tools);
			}
		});
		return items;
	}

	/**
	 * @returns CKEditor's configuration: the plugins, the toolbar, the license key, the language,
	 *          the headings, alignment by classes, links opening in a new tab; and the page's own
	 */
	private config(build: CKEditorBuild): Record<string, unknown> {
		const options = this.options;
		const features = this.features();
		const levels = [...new Set((options.headingLevels ?? DEFAULTS.headingLevels)
			.filter((level) => Number.isInteger(level) && level >= 1 && level <= 6))].sort();
		const config: Record<string, unknown> = {
			plugins: this.plugins(build),
			toolbar: { items: this.toolbarItems(), shouldNotGroupWhenFull: false },
			heading: {
				options: [{ model: 'paragraph', title: 'Paragraph', class: 'ck-heading_paragraph' },
					...levels.map((level) => ({ model: 'heading' + level, view: 'h' + level,
						title: 'Heading ' + level, class: 'ck-heading_heading' + level }))]
			},
			alignment: {
				options: [{ name: 'left', className: 'antilia-text-left' },
					...Object.entries(RICH_TEXT_ALIGN_CLASSES).map(([name, className]) =>
						({ name, className }))]
			},
			link: {
				defaultProtocol: 'https://',
				decorators: {
					openInNewTab: {
						mode: 'manual',
						label: this.texts().linkNewTab,
						attributes: { target: '_blank', rel: 'noopener noreferrer nofollow' }
					}
				}
			},
			table: { contentToolbar: ['tableColumn', 'tableRow', 'mergeTableCells'] },
			image: { toolbar: ['imageTextAlternative', 'toggleImageCaption'] }
		};
		if (!features.has('heading')) {
			delete config.heading;
		}
		if (options.licenseKey !== undefined) {
			config.licenseKey = options.licenseKey;
		}
		if (options.language) {
			config.language = options.language;
		}
		if (options.translations) {
			config.translations = [...options.translations];
		}
		if (options.placeholder) {
			config.placeholder = options.placeholder;
		}
		const label = this.label();
		if (label) {
			config.label = label;
		}
		return { ...config, ...options.config };
	}

	/**
	 * Starts CKEditor, on an element of its own in the editor's.
	 *
	 * @returns resolves with the editor once CKEditor started
	 */
	private async start(): Promise<CKEditor> {
		const generation = ++this.generation;
		const build = this.build();
		if (!build) {
			return this.fail(new Error('There is no CKEditor in the page: give the build, or load ' +
				'the browser build of ckeditor5, which defines CKEDITOR'));
		}
		const source = document.createElement('div');
		source.className = CK_EDITOR_CLASSES.source;
		this.source?.remove();
		this.source = source;
		// after the text area, before the count and the handle resizing the editor
		if (this.field) {
			this.field.after(source);
		} else {
			this.element.prepend(source);
		}
		let editor: CKEditorInstance;
		try {
			editor = await build.ClassicEditor.create(source, this.config(build));
		} catch (error) {
			if (generation !== this.generation || this.destroyed) {
				throw error;
			}
			return this.fail(error);
		}
		if (generation !== this.generation || this.destroyed) {
			// options changed meanwhile, or the editor went
			await editor.destroy();
			if (this.destroyed) {
				throw new Error('The editor was destroyed before CKEditor started');
			}
			return this.ready;
		}
		this.editor = editor;
		this.setUp(editor);
		this.options.onReady?.(editor);
		return this;
	}

	/**
	 * CKEditor could not start: the text area shows again, to type the text in.
	 */
	private fail(error: unknown): never {
		getHost().log.error('CKEditor could not start', error);
		this.options.onError?.(error);
		this.destroy();
		throw error;
	}

	/**
	 * Makes CKEditor the editor's: its text, what it lets the text hold, its events, its state.
	 */
	private setUp(editor: CKEditorInstance): void {
		const model = editor.model;
		// no width or height on images, for which CKEditor writes a style: not read from the HTML,
		// and not given by CKEditor once an image loaded, its natural size
		model.schema.addAttributeCheck((context, attribute) => SIZES.includes(attribute) &&
			IMAGES.some((image) => context.endsWith(image)) ? false : undefined);
		// CKEditor gives an image its natural size once it loaded: the size stays in the model,
		// neither the text nor the HTML gets it, before CKEditor's converters
		editor.conversion.for('downcast').add((dispatcher) => IMAGES.forEach((image) =>
			SIZES.forEach((size) => dispatcher.on(`attribute:${size}:${image}`, (event, data: {
				item: unknown }, api: { consumable: { consume(item: unknown, type: string): boolean } }) =>
				api.consumable.consume(data.item, (event as { name: string }).name),
			{ priority: 'highest' }))));
		// pasted HTML made quiet, as the HTML the page gives, for every plugin reading it: the
		// clipboard's pipeline reads the content, Paste from Office the HTML of the clipboard again
		editor.editing.view.document.on('clipboardInput', (_event, data: {
			content?: unknown; dataTransfer?: { getData(type: string): string } }) => {
			if (typeof data.content === 'string') {
				data.content = quietHtml(data.content);
			}
			const transfer = data.dataTransfer;
			if (transfer) {
				data.dataTransfer = Object.assign(Object.create(transfer) as typeof transfer, {
					getData: (type: string) => {
						const value = transfer.getData(type);
						return type === 'text/html' && value ? quietHtml(value) : value;
					}
				});
			}
		}, { priority: 'highest' });
		editor.keystrokes.set('Ctrl+S', (_data, cancel) => {
			this.save();
			cancel();
		});
		this.external = true;
		try {
			editor.setData(this.prepare(this.value));
		} finally {
			this.external = false;
		}
		this.value = this.read(editor);
		if (!this.savedCanonical) {
			this.saved = this.canonical(this.saved);
			this.savedCanonical = true;
		}
		this.renderReadOnly();
		this.applySelection(this.pendingSelection ?? this.selected);
		this.pendingSelection = null;
		this.selected = this.selection();
		model.document.on('change:data', () => this.onDataChange());
		model.document.selection.on('change:range', () => this.onSelectionChange());
		editor.ui.focusTracker.on('change:isFocused', (_event, _name, focused: boolean) => {
			if (focused) {
				this.onFocus();
			} else {
				this.onBlur();
			}
		});
		this.bindScroller();
		this.syncField(this.value);
		this.renderStatus();
		this.renderModified(this.value, false);
		this.restoreScroll();
		this.dispatch('text');
	}

	/**
	 * @returns a text as CKEditor reads it: HTML made quiet, Markdown as it is
	 */
	private prepare(value: string): string {
		return this.format() === 'html' ? quietHtml(value) : value;
	}

	/**
	 * @returns CKEditor's text as the editor keeps it: '' while empty, the HTML without the ids of
	 *          the items of lists
	 */
	private read(editor: CKEditorInstance): string {
		const data = editor.getData();
		return this.format() === 'html' ? data.replace(LIST_ITEM_ID, '') : data;
	}

	/**
	 * @returns a text as the editor writes it: CKEditor's of the same content, for one; as it is
	 *          until CKEditor started
	 */
	private canonical(value: string): string {
		const editor = this.editor;
		if (!editor) {
			return value;
		}
		try {
			const data = editor.data.stringify(editor.data.parse(this.prepare(value)));
			return this.format() === 'html' ? data.replace(LIST_ITEM_ID, '') : data;
		} catch (error) {
			getHost().log.error('CKEditor could not read the text', error);
			return value;
		}
	}

	private onDataChange(): void {
		const editor = this.editor;
		if (!editor || this.destroyed) {
			return;
		}
		const value = this.read(editor);
		if (value === this.value) {
			return;
		}
		this.value = value;
		this.syncField(value);
		this.renderModified(value, true);
		this.renderStatus();
		this.dispatch('text');
		if (!this.external) {
			this.textReported = false;
			this.options.onChange?.(value);
			this.fieldEvent('input');
			this.dispatch('change');
			this.stateChanged();
		}
	}

	private onSelectionChange(): void {
		if (!this.editor || this.destroyed) {
			return;
		}
		const selection = this.selection();
		if (selection.anchor === this.selected.anchor && selection.head === this.selected.head) {
			return;
		}
		this.selected = selection;
		if (!this.external) {
			this.options.onSelectionChange?.(selection);
			this.dispatch('select', { selection });
			this.stateChanged();
		}
	}

	private onFocus(): void {
		if (this.focused || this.destroyed) {
			return;
		}
		this.focused = true;
		this.element.classList.add(CK_EDITOR_CLASSES.focused);
		this.focusValue = this.getValue();
		this.options.onFocus?.();
		this.dispatch('focus');
	}

	private onBlur(): void {
		if (!this.focused || this.destroyed) {
			return;
		}
		this.focused = false;
		this.element.classList.remove(CK_EDITOR_CLASSES.focused);
		const value = this.getValue();
		if (this.focusValue !== null && this.focusValue !== value) {
			this.fieldEvent('change');
		}
		this.focusValue = null;
		this.options.onBlur?.(value);
		this.dispatch('blur');
		this.report(false);
	}

	/**
	 * Shows the count of the characters and the words below the text, or takes it away.
	 */
	private renderStatus(): void {
		if (!this.options.count) {
			this.status?.remove();
			this.status = null;
			return;
		}
		if (!this.status) {
			this.status = document.createElement('div');
			this.status.className = this.css('ck-editor-status');
			this.status.setAttribute('aria-live', 'polite');
		}
		// after CKEditor's, before the handle resizing the editor
		const handle = this.element.querySelector(':scope > .antilia-resizable-handle');
		if (handle) {
			handle.before(this.status);
		} else {
			this.element.append(this.status);
		}
		// counted once CKEditor started, in its model
		const root = this.editor?.model.document.getRoot();
		if (!root) {
			this.status.textContent = '';
			return;
		}
		const parts = textOf(root);
		const characters = parts.filter((part) => part !== '\n').join('').length;
		const words = (parts.join(' ').match(/\S+/g) ?? []).length;
		const texts = this.texts();
		this.status.textContent = format(characters === 1 ? texts.character : texts.characters,
			characters) + ' · ' + format(words === 1 ? texts.word : texts.words, words);
	}

	/**
	 * Makes the editor resizable, or not, as the options say, with the size it has.
	 */
	private renderResizable(): void {
		const axis = this.axis();
		const options = this.options;
		if (!axis) {
			this.resizable?.destroy();
			this.resizable = null;
			return;
		}
		const resizable = {
			axis, width: this.size.width, height: this.size.height,
			minWidth: options.minWidth ?? DEFAULTS.minWidth,
			minHeight: options.minHeight ?? DEFAULTS.minHeight,
			maxWidth: options.maxWidth ?? null, maxHeight: options.maxHeight ?? null,
			label: this.texts().resize, look: this.look,
			onResized: (size: ResizableSize) => this.onResized(size)
		};
		if (this.resizable) {
			this.resizable.configure(resizable);
		} else {
			this.resizable = new Resizable(this.element, resizable);
		}
		this.size = this.resizable.getSize();
	}

	private onResized(size: ResizableSize): void {
		this.size = size;
		this.options.onResize?.({ ...size });
		this.dispatch('resize', { size: { ...size } });
		this.stateChanged();
	}

	/**
	 * Gives CKEditor the editor's read only mode, and its toolbar goes or comes back.
	 */
	private renderReadOnly(): void {
		const editor = this.editor;
		if (!editor) {
			return;
		}
		if (this.isReadOnly()) {
			editor.enableReadOnlyMode(READ_ONLY_LOCK);
		} else {
			editor.disableReadOnlyMode(READ_ONLY_LOCK);
		}
	}

	private listen<E extends Event>(target: EventTarget, type: string,
		listener: (event: E) => void, options: AddEventListenerOptions | boolean = false): void {
		target.addEventListener(type, listener as EventListener, options);
		this.listeners.push([target, type, listener as EventListener,
			typeof options === 'boolean' ? options : !!options.capture]);
	}

	private bind(): void {
		const field = this.field;
		if (field) {
			// the page put the text area back as it was, closing a dialog anyway for example
			this.listen<Event>(field, 'input', (event) => {
				if (event.target === field && !(event instanceof CustomEvent) &&
					field.value !== this.getValue() && !this.dispatching) {
					this.setValue(field.value);
				}
			});
			Array.from(field.labels ?? []).forEach((label) => this.listen<MouseEvent>(label, 'click',
				(event) => {
					event.preventDefault();
					this.focus();
				}));
		}
		// a tab or a window of the desktop closed anyway: the saved text comes back
		const discard = (event: Event) => {
			const target = event.target as Node | null;
			if (target && target.contains(this.element) && this.isModified()) {
				this.setValue(this.saved);
			}
		};
		this.listen(document, 'antilia-tabs:discard', discard);
		this.listen(document, 'antilia-desktop:discard', discard);
		this.listen(window, 'pagehide', () => {
			if (this.timer !== null || !this.textReported) {
				this.report(true);
			}
		});
	}

	/**
	 * Listens to the element CKEditor scrolls its text in, made anew with CKEditor: how far it is
	 * scrolled, and whether it is shown.
	 */
	private bindScroller(): void {
		this.unbindScroller();
		const scroller = this.scroller();
		if (!scroller) {
			return;
		}
		const onScroll = () => {
			// hidden, a scroller is at its start: the editor keeps where it was
			if (scroller.clientHeight > 0) {
				this.scroll = { top: scroller.scrollTop, left: scroller.scrollLeft };
				this.stateChanged();
			}
		};
		scroller.addEventListener('scroll', onScroll, { passive: true });
		this.scrollerListeners.push([scroller, 'scroll', onScroll]);
		this.shown = scroller.clientHeight > 0;
		if (typeof ResizeObserver === 'function') {
			this.observer = new ResizeObserver(() => {
				const shown = scroller.clientHeight > 0;
				if (shown && !this.shown) {
					this.restoreScroll();
				}
				this.shown = shown;
			});
			this.observer.observe(scroller);
		}
	}

	private unbindScroller(): void {
		this.scrollerListeners.forEach(([target, type, listener]) =>
			target.removeEventListener(type, listener));
		this.scrollerListeners = [];
		this.observer?.disconnect();
		this.observer = null;
	}

	/**
	 * @returns the element CKEditor scrolls its text in, its editable
	 */
	private scroller(): HTMLElement | null {
		return this.editor?.ui.view.editable.element ?? null;
	}

	/**
	 * Keeps the text in the text area, if any, which a form posts.
	 */
	private syncField(value: string): void {
		if (this.field && this.field.value !== value) {
			this.field.value = value;
		}
		this.renderSaved();
	}

	/**
	 * Gives the text area the text last saved as the text it was rendered with, which tells the
	 * page's dialogs whether it has changes; after its value, which it then keeps.
	 */
	private renderSaved(): void {
		const field = this.field;
		if (field && field.defaultValue !== this.saved) {
			const value = field.value;
			field.defaultValue = this.saved;
			if (field.value !== value) {
				field.value = value;
			}
		}
	}

	/**
	 * Tells whether the text has changes not saved: data-modified on the element, which tabs and
	 * the desktop show, and onModifiedChange when it changed.
	 */
	private renderModified(value: string, callBack: boolean): void {
		const modified = value !== this.saved;
		this.element.setAttribute('data-modified', String(modified));
		if (modified !== this.modified) {
			this.modified = modified;
			if (callBack) {
				this.options.onModifiedChange?.(modified);
				this.dispatch('modified', { modified });
			}
		}
	}

	/**
	 * Fires an event of the editor's own, then the types of events firing from it.
	 */
	private dispatch(name: keyof typeof RICH_TEXT_EDITOR_EVENTS,
		detail: Omit<CKEditorEventDetail, 'editor'> = {}): void {
		const type = RICH_TEXT_EDITOR_EVENTS[name];
		this.emit(type, detail);
		this.eventTypes.from(type, detail);
	}

	/**
	 * Fires an event on the text area, or the element without one, which bubbles: its detail
	 * what it tells and the editor.
	 *
	 * @returns true, the event fired
	 */
	private emit(type: string, detail: Readonly<Record<string, unknown>>): boolean {
		const target = this.field ?? this.element;
		// a listener may fire another event meanwhile: the editor still does not hear it
		const dispatching = this.dispatching;
		this.dispatching = true;
		try {
			target.dispatchEvent(new CustomEvent(type, { bubbles: true,
				detail: { ...detail, editor: this } }));
		} finally {
			this.dispatching = dispatching;
		}
		return true;
	}

	/**
	 * Fires input or change on the text area, as a text area does when the user changes it, then
	 * the types of events firing from it, also without a text area.
	 */
	private fieldEvent(type: 'input' | 'change'): void {
		if (this.field) {
			const dispatching = this.dispatching;
			this.dispatching = true;
			try {
				this.field.dispatchEvent(new Event(type, { bubbles: true }));
			} finally {
				this.dispatching = dispatching;
			}
		}
		this.eventTypes.from(type);
	}

	/**
	 * Fires a type of events registered (registerRichTextEvent), from the page, a plugin or a key,
	 * as the Tiptap editor does.
	 *
	 * @param name the name of the type, <owner>:<event>
	 * @param values what the type is told, as the detail of its cause; what the event tells for a
	 *        type without fire()
	 * @returns whether the event fired: not for a type not registered, nor while it is firing
	 */
	trigger(name: string, values?: Readonly<Record<string, unknown>>): boolean {
		return !this.destroyed && this.eventTypes.trigger(name, values);
	}

	/**
	 * Selects what a selection says, within the text, in CKEditor; once it started, until then.
	 *
	 * @returns whether CKEditor took it
	 */
	private applySelection(selection: RichTextEditorSelection | null | undefined): boolean {
		if (!selection || !Number.isFinite(selection.anchor) || !Number.isFinite(selection.head)) {
			return false;
		}
		const editor = this.editor;
		const root = editor?.model.document.getRoot();
		if (!editor || !root) {
			this.pendingSelection = selection;
			return false;
		}
		const model = editor.model;
		const at = (offset: number) => {
			const position = model.createPositionFromPath(root, pathOfOffset(root, offset));
			return model.schema.getNearestSelectionRange(position, 'both')?.start ?? position;
		};
		try {
			const anchor = at(selection.anchor);
			const head = at(selection.head);
			const backward = head.isBefore(anchor);
			this.external = true;
			model.change((writer) => writer.setSelection(backward ? model.createRange(head, anchor) :
				model.createRange(anchor, head), { backward }));
		} catch (error) {
			getHost().log.error('CKEditor could not select what the state says', error);
			return false;
		} finally {
			this.external = false;
		}
		return true;
	}

	private selection(): RichTextEditorSelection {
		const editor = this.editor;
		const root = editor?.model.document.getRoot();
		if (!editor || !root) {
			return { ...(this.pendingSelection ?? this.selected) };
		}
		const selection = editor.model.document.selection;
		const anchor = selection.anchor ? offsetOfPath(root, selection.anchor.path) : 0;
		const head = selection.focus ? offsetOfPath(root, selection.focus.path) : anchor;
		return { anchor, head };
	}

	/**
	 * Scrolls the text where it was, once the editor is shown and laid out.
	 */
	private restoreScroll(): void {
		const { top, left } = this.scroll;
		const scroller = this.scroller();
		if (!scroller || (top <= 0 && left <= 0)) {
			return;
		}
		const apply = () => {
			if (!this.destroyed && scroller.clientHeight > 0) {
				scroller.scrollTop = top;
				scroller.scrollLeft = left;
			}
		};
		apply();
		// and again once the images and the rest of the text are laid out
		if (typeof requestAnimationFrame === 'function') {
			requestAnimationFrame(apply);
		}
	}

	/**
	 * Reports the state a moment after the user last changed it.
	 */
	private stateChanged(): void {
		if (this.timer !== null) {
			clearTimeout(this.timer);
		}
		this.timer = setTimeout(() => this.report(false),
			Math.max(this.options.stateDelay ?? DEFAULTS.stateDelay, 0));
	}

	/**
	 * Reports the state, when it or the text changed since it was last reported.
	 *
	 * @param leaving whether the page is going
	 */
	private report(leaving: boolean): void {
		if (this.timer !== null) {
			clearTimeout(this.timer);
			this.timer = null;
		}
		if (this.destroyed) {
			return;
		}
		const state = this.getState();
		const changed = !this.textReported;
		if (!changed && sameState(state, this.reported)) {
			return;
		}
		this.textReported = true;
		if (this.options.onStateChange) {
			this.reported = state;
			this.options.onStateChange(state, { leaving, changed });
		}
		this.dispatch('state', { state });
	}

	/**
	 * @returns the text, in the editor's format; '' while the editor is empty
	 */
	getValue(): string {
		return this.value;
	}

	/**
	 * @returns the text, as plain text, a blank line between its blocks, as the Tiptap editor
	 *          writes it; '' until CKEditor started
	 */
	getText(): string {
		const root = this.editor?.model.document.getRoot();
		return root ? textBetween(root, 0, Infinity, '\n\n') : '';
	}

	/**
	 * @returns the text selected, as plain text, a line for each block; '' for none, and until
	 *          CKEditor started
	 */
	getSelectedText(): string {
		const root = this.editor?.model.document.getRoot();
		if (!root) {
			return '';
		}
		const { anchor, head } = this.selection();
		return textBetween(root, Math.min(anchor, head), Math.max(anchor, head), '\n');
	}

	/**
	 * Replaces the text, without calling back: the page decides. The selection stays, within the
	 * text. The user cannot undo it.
	 *
	 * @param value the text, in the editor's format
	 * @param saved whether it is the text last saved too
	 */
	setValue(value: string, saved = false): void {
		if (this.destroyed) {
			return;
		}
		const editor = this.editor;
		if (!editor) {
			// CKEditor takes it once it started
			this.value = value;
			if (saved) {
				this.saved = value;
				this.savedCanonical = false;
			}
			this.syncField(value);
			this.textReported = false;
			this.renderModified(value, true);
			return;
		}
		const canonical = this.canonical(value);
		if (saved) {
			this.saved = canonical;
		}
		if (canonical !== this.value) {
			const selection = this.selection();
			this.external = true;
			try {
				editor.setData(this.prepare(value));
			} finally {
				this.external = false;
			}
			this.applySelection(selection);
			this.selected = this.selection();
			this.textReported = false;
		} else {
			this.syncField(canonical);
		}
		this.renderModified(this.value, true);
	}

	/**
	 * @returns whether the text has changes not saved
	 */
	isModified(): boolean {
		return this.modified;
	}

	/**
	 * Takes the text, or another, as the text last saved: the editor no longer has changes.
	 *
	 * @param value the text saved, the editor's when left out
	 */
	markSaved(value: string = this.getValue()): void {
		if (this.destroyed) {
			return;
		}
		this.saved = this.canonical(value);
		this.savedCanonical = !!this.editor;
		this.renderSaved();
		this.renderModified(this.getValue(), true);
	}

	/**
	 * @returns the selection, the scroll position and the size
	 */
	getState(): RichTextEditorState {
		return {
			selection: this.selection(),
			scrollTop: Math.round(this.scroll.top),
			scrollLeft: Math.round(this.scroll.left),
			width: this.size.width,
			height: this.size.height
		};
	}

	/**
	 * Puts the editor in a state, without calling back: the page decides. What the state leaves
	 * out stays.
	 */
	setState(state: RichTextEditorState): void {
		if (this.applySelection(state.selection)) {
			this.selected = this.selection();
		}
		if (state.scrollTop !== undefined || state.scrollLeft !== undefined) {
			this.scroll = { top: Math.max(state.scrollTop ?? this.scroll.top, 0),
				left: Math.max(state.scrollLeft ?? this.scroll.left, 0) };
			this.restoreScroll();
		}
		if (state.width !== undefined || state.height !== undefined) {
			this.size = { width: state.width === undefined ? this.size.width : state.width,
				height: state.height === undefined ? this.size.height : state.height };
			this.resizable?.setSize(this.size);
			if (this.resizable) {
				this.size = this.resizable.getSize();
			}
		}
	}

	/**
	 * Tells the page to save the text: onSave and the event save, as Ctrl+S does.
	 */
	save(): void {
		this.options.onSave?.(this.getValue());
		this.dispatch('save');
	}

	/**
	 * Puts the focus in the text, where the cursor is, once CKEditor started.
	 */
	focus(): void {
		if (this.editor) {
			this.editor.editing.view.focus();
		} else {
			this.ready.then((editor) => editor.focus(), () => undefined);
		}
	}

	/**
	 * Takes other options; those left out stay as they were. The text, the text last saved and
	 * the state stay too: setValue(), markSaved() and setState() change them. Other features, a
	 * format, a toolbar, plugins or CKEditor's configuration make CKEditor anew.
	 */
	configure(options: CKEditorOptions = {}): void {
		if (this.destroyed) {
			return;
		}
		const defined = Object.fromEntries(Object.entries(options)
			.filter(([, value]) => value !== undefined)) as CKEditorOptions;
		const before = this.options;
		this.options = { ...before, ...defined };
		const changed = (...names: (keyof CKEditorOptions)[]) => names.some((name) =>
			JSON.stringify(before[name] ?? null) !== JSON.stringify(this.options[name] ?? null));
		this.render();
		const restart = changed(...CONFIG_OPTIONS) ||
			OBJECT_OPTIONS.some((name) => before[name] !== this.options[name]) ||
			before.texts?.linkNewTab !== this.options.texts?.linkNewTab;
		if (restart && this.editor) {
			this.restart(before.format !== this.options.format);
		} else if (restart) {
			// CKEditor is starting with the options before: another starts, that one goes
			this.restartWith(this.value);
		} else {
			this.renderReadOnly();
		}
		this.renderStatus();
		this.renderResizable();
	}

	/**
	 * Makes CKEditor anew, for another configuration, with the text and the selection it had.
	 *
	 * @param reformat whether the format changed: the text then is read anew, and saved as it is
	 */
	private restart(reformat: boolean): void {
		const editor = this.editor;
		if (!editor) {
			return;
		}
		this.pendingSelection = this.selection();
		const value = reformat ? editor.getData() : this.value;
		this.unbindScroller();
		this.editor = null;
		this.stop(editor);
		if (reformat) {
			this.savedCanonical = false;
			this.saved = value;
		}
		this.restartWith(value);
	}

	/**
	 * Starts CKEditor anew with a text: ready is then the new start.
	 */
	private restartWith(value: string): void {
		this.value = value;
		(this as { ready: Promise<CKEditor> }).ready = this.start();
		this.ready.catch(() => undefined);
	}

	/**
	 * Takes the editor away: CKEditor, the handle resizing it and what it gave the elements; the
	 * text area is shown again, with the text, and a wrapper the editor made goes.
	 */
	destroy(): void {
		if (this.destroyed) {
			return;
		}
		this.release();
		const element = this.element;
		const field = this.field;
		if (field) {
			field.hidden = this.fieldHidden;
			field.removeAttribute('tabindex');
			field.removeAttribute('aria-hidden');
			field.classList.remove(...this.css('ck-editor-field').split(' '));
			if (!field.classList.length) {
				field.removeAttribute('class');
			}
		}
		if (this.ownElement && element.parentNode) {
			if (field) {
				element.parentNode.insertBefore(field, element);
			}
			element.remove();
		} else {
			element.classList.remove(...this.css('ck-editor').split(' '),
				...Object.values(CK_EDITOR_CLASSES));
			element.removeAttribute('data-modified');
			if (!element.classList.length) {
				element.removeAttribute('class');
			}
		}
	}

	/**
	 * @returns what an editor rendered anew in the same place goes on with
	 */
	private continuation(): Continuation {
		return { value: this.getValue(), saved: this.saved, state: this.getState(),
			reported: this.reported };
	}

	/**
	 * The page is taking the editor away: what it was at is kept for an editor rendered anew with
	 * the same id, and the editor goes.
	 */
	private remove(): void {
		const id = this.field?.id || this.element.id;
		if (id && !this.destroyed) {
			continuations.set(id, this.continuation());
		}
		this.destroy();
	}

	/**
	 * Stops the editor, leaving its element and its text area: CKEditor and the handle resizing
	 * it go.
	 */
	private release(): void {
		if (this.destroyed) {
			return;
		}
		this.destroyed = true;
		this.generation++;
		if (this.timer !== null) {
			clearTimeout(this.timer);
			this.timer = null;
		}
		this.listeners.forEach(([target, type, listener, capture]) => {
			target.removeEventListener(type, listener, capture);
		});
		this.listeners = [];
		this.unbindScroller();
		this.stopWatchingRemoval?.();
		this.stopWatchingRemoval = null;
		this.status?.remove();
		this.status = null;
		this.resizable?.destroy();
		this.resizable = null;
		const editor = this.editor;
		this.editor = null;
		if (editor) {
			this.stop(editor);
		}
		this.source?.remove();
		this.source = null;
		if (instances.get(this.element) === this) {
			instances.delete(this.element);
		}
		if (this.field && instances.get(this.field) === this) {
			instances.delete(this.field);
		}
	}

	/**
	 * Stops a CKEditor: its UI goes at once, CKEditor in a moment.
	 */
	private stop(editor: CKEditorInstance): void {
		const ui = editor.ui.element;
		editor.destroy().catch((error: unknown) =>
			getHost().log.error('CKEditor could not stop', error));
		ui?.remove();
	}
}

/**
 * Makes an editor of an element, in the shape of a Svelte action: use:ckEditor={options}.
 *
 * @param element the element of the editor, or a text area
 * @param options CKEditor and its license key, the text, its format and the features; the state
 *        and the callbacks
 * @returns update() taking other options, destroy() taking the editor away
 */
export function ckEditor(element: HTMLElement, options?: CKEditorOptions): {
	update(options?: CKEditorOptions): void;
	destroy(): void;
} {
	const instance = new CKEditor(element, options);
	return {
		update: (next) => instance.configure(next),
		destroy: () => instance.destroy()
	};
}
