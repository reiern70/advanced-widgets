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
 * A rich text editor: Tiptap, on ProseMirror, with a toolbar of the components' own, in the
 * colors of the theme it is in and the look of the page. The text has headings, bold, italic,
 * underlined, struck and code text, links, lists, task lists, quotes, code blocks, rules, tables,
 * images and alignment: the features (see rich-text-schema.ts), all of them unless the page
 * leaves some out. The usual keys format (Ctrl+B, Ctrl+I...), Markdown's marks do as the user
 * types (# a heading, - a list, **bold**), Ctrl+K edits the link and Ctrl+S tells the page to save.
 *
 * The text is HTML by default, Markdown or Tiptap's JSON if the page says so (format). The HTML
 * has no style attribute: alignment is a class. The browser's HTML is never to be trusted by the
 * server, which cleans it: Wicket's RichTextEditor does.
 *
 * new RichTextEditor(element, options) works on a text area, which it hides and wraps in the
 * element of the editor, keeping the text in it: a form posts it, and the text area tells the
 * page of the user's changes with the events input, and change once the editor loses the focus,
 * as a text area does. It works on any other element too, an empty one React or Svelte renders,
 * which becomes the element of the editor; a text area in it, if any, is then the one the editor
 * keeps the text in. A text area a server renders may have the class
 * antilia-rich-text-editor-loading: while scripts run, the style sheet hides it in the room of the
 * editor until the editor is made, so that a page arriving slowly does not show its HTML.
 *
 * What is selected, how far the text is scrolled and the size the user gave the editor are its
 * state (RichTextEditorState). The editor keeps it while hidden, in a tab that is not selected for
 * example, and shows it again when shown; onStateChange reports it, a moment after the user
 * changed it, as the editor loses the focus and as the page goes, for a page that keeps it, on the
 * server for example, to give it back with the option state, after the user came back to the
 * page. Rendered anew in the same place with the same text, as Wicket does, the editor goes on as
 * it was.
 *
 * The editor tells whether the text has changes not saved, against the text last saved (saved,
 * markSaved()): as data-modified on its element, which tabs and windows of the desktop show, and
 * with onModifiedChange. Closing a tab or a window anyway puts the saved text back.
 *
 * Besides its callbacks, the editor fires events on the text area, or its element without one,
 * which bubble: RICH_TEXT_EDITOR_EVENTS, for listeners of the page's own, and Wicket's Ajax
 * behaviors.
 *
 * Tiptap's own style sheet is not written into the page: rich-text-editor.css has its rules, so
 * that the editor needs no style element under a Content Security Policy. Its sizes are custom
 * properties set through the CSSOM, as the resizable's, which makes the editor resizable by its
 * bottom edge by default.
 *
 * The extensions are pluggable: any Tiptap extension the page gives (extensions), or one a page
 * without Tiptap's modules of its own, a Wicket page, registers by a name, built from the classes
 * of Tiptap it is given (RICH_TEXT_EDITOR_KIT).
 *
 * Code blocks are highlighted in the colors of their language with a highlighter the page gives
 * (highlight, see rich-text-code-blocks.ts), whose grammars this script does not carry; the menu of
 * the tool code block chooses the language (codeLanguages). Images are uploaded, chosen in the form
 * of the image, dropped into the text or pasted, while the page uploads them (onUpload, see
 * rich-text-upload.ts).
 */

import { createDocument, Editor, Extension, getHTMLFromFragment, Mark, markInputRule, markPasteRule,
	mergeAttributes, Node as TiptapNode, nodeInputRule, textblockTypeInputRule, wrappingInputRule,
	type AnyExtension } from '@tiptap/core';
import { BubbleMenu } from '@tiptap/extension-bubble-menu';
import type { Node as ProseMirrorNode } from '@tiptap/pm/model';
import { Plugin, PluginKey, TextSelection, type Transaction } from '@tiptap/pm/state';

import { getHost, onRemoving, type Unsubscribe } from './host.js';
import { lookOf, partClasses, type Look, type LookPart } from './look.js';
import { Resizable, type ResizableSize, type ResizeAxis } from './resizable.js';
import { CodeBlocks, redrawCodeBlocks, richTextHighlighter } from './rich-text-code-blocks.js';
import { codeLanguage, RICH_TEXT_CODE_LANGUAGES,
	type RichTextHighlighter } from './rich-text-languages.js';
import { DEFAULT_HEADING_LEVELS, isSafeUrl, quietHtml, RICH_TEXT_FEATURES, RICH_TEXT_FORMATS,
	RICH_TEXT_NOT_IN_MARKDOWN, schemaExtensions, type HeadingLevel, type RichTextFeature,
	type RichTextFormat } from './rich-text-schema.js';
import { DEFAULT_RICH_TEXT_EDITOR_TEXTS, DEFAULT_RICH_TEXT_TOOLBAR, format, RICH_TEXT_BUBBLE_TOOLS,
	RichTextToolbar, type RichTextCodeLanguage, type RichTextEditorTexts, type RichTextTool,
	type RichTextToolbarContext } from './rich-text-toolbar.js';
import { DEFAULT_RICH_TEXT_UPLOAD_TYPES, sizeText, typesText, uploadExtension,
	type RichTextUploader, type UploadHost } from './rich-text-upload.js';

/**
 * The classes of Tiptap and ProseMirror a function registering an extension is given, for a page
 * that has no Tiptap modules of its own, such as a Wicket page: the editor's, so that the
 * extension works with them.
 */
export interface RichTextEditorKit {
	Extension: typeof Extension;
	Node: typeof TiptapNode;
	Mark: typeof Mark;
	mergeAttributes: typeof mergeAttributes;
	markInputRule: typeof markInputRule;
	markPasteRule: typeof markPasteRule;
	nodeInputRule: typeof nodeInputRule;
	textblockTypeInputRule: typeof textblockTypeInputRule;
	wrappingInputRule: typeof wrappingInputRule;
	Plugin: typeof Plugin;
	PluginKey: typeof PluginKey;
}

/** The classes of Tiptap and ProseMirror of the editor. */
export const RICH_TEXT_EDITOR_KIT: RichTextEditorKit = Object.freeze({
	Extension, Node: TiptapNode, Mark, mergeAttributes, markInputRule, markPasteRule,
	nodeInputRule, textblockTypeInputRule, wrappingInputRule, Plugin, PluginKey
});

/** Makes an extension, or several, from the classes of the kit. */
export type RichTextExtensionLoader = (kit: RichTextEditorKit) => AnyExtension | AnyExtension[];

/** An extension of an editor: one of Tiptap, or the name of one registered. */
export type RichTextExtension = AnyExtension | string;

/** The extensions registered, by their names. */
const registered = new Map<string, RichTextExtensionLoader | AnyExtension | AnyExtension[]>();

/** Those made already from a loader. */
const made = new Map<string, AnyExtension[]>();

/**
 * Makes an extension known to the editors, under a name; it replaces one of that name. A function
 * is called the first time an editor takes the extension, with the classes of the kit.
 *
 * @param name the name, which the option extensions and Wicket's RichTextExtension name; the case
 *        does not matter
 * @param extension the extension, several, or a function making them
 */
export function registerRichTextExtension(name: string,
	extension: RichTextExtensionLoader | AnyExtension | AnyExtension[]): void {
	const key = name.trim().toLowerCase();
	registered.set(key, extension);
	made.delete(key);
}

/**
 * @returns the names of the extensions registered
 */
export function richTextExtensions(): string[] {
	return Array.from(registered.keys());
}

/**
 * @returns the extensions registered under a name, made once; none for a name not known
 */
function extensionsNamed(name: string): AnyExtension[] {
	const key = name.trim().toLowerCase();
	const done = made.get(key);
	if (done) {
		return done;
	}
	const entry = registered.get(key);
	if (entry === undefined) {
		getHost().log.error('No extension of the rich text editor is named ' + name);
		return [];
	}
	try {
		const result = typeof entry === 'function' ? entry(RICH_TEXT_EDITOR_KIT) : entry;
		const extensions = Array.isArray(result) ? result : [result];
		made.set(key, extensions);
		return extensions;
	} catch (error) {
		getHost().log.error('The extension ' + name + ' of the rich text editor failed', error);
		return [];
	}
}

/** What is selected: from where the selection starts to where its head, the cursor, is. */
export interface RichTextEditorSelection {
	anchor: number;
	head: number;
}

/**
 * The state of an editor besides its text: what is selected, how far it is scrolled, in pixels,
 * and the size the user gave it, null for the size the page's style sheets give it. The positions
 * of the selection are those of ProseMirror, which count the places between the nodes too.
 */
export interface RichTextEditorState {
	selection?: RichTextEditorSelection;
	scrollTop?: number;
	scrollLeft?: number;
	width?: number | null;
	height?: number | null;
}

/** What onStateChange is told besides the state. */
export interface RichTextEditorStateChange {
	/** Whether the page is going: the last chance to keep the state. */
	leaving: boolean;
	/** Whether the text changed since the state was last reported. */
	changed: boolean;
}

/**
 * What an editor holds and shows, and what it lets the user do.
 */
export interface RichTextEditorOptions {
	/** The text; that of the text area, or none, when left out. */
	value?: string;
	/** The text last saved, against which the editor tells of changes; the text when left out. */
	saved?: string;
	/** How the editor keeps the text: html when left out, markdown or json. */
	format?: RichTextFormat;
	/**
	 * What the text may hold: all the features when left out, but for Markdown underlined text
	 * and alignment, which Markdown has no syntax for.
	 */
	features?: readonly RichTextFeature[];
	/** The levels of the headings, 1 to 4 when left out. */
	headingLevels?: readonly HeadingLevel[];
	/**
	 * The tools of the toolbar, in groups; DEFAULT_RICH_TEXT_TOOLBAR when left out, false for no
	 * toolbar. The tools of features left out do not show.
	 */
	toolbar?: readonly (readonly RichTextTool[])[] | false;
	/** Whether a menu of the inline formats shows over the text selected, false when left out. */
	bubbleMenu?: boolean;
	/** Whether the user cannot change the text, false when left out; read only text areas are so. */
	readOnly?: boolean;
	/** The text shown while the editor is empty. */
	placeholder?: string;
	/** The accessible name of the text; that of the labels of the text area when left out. */
	label?: string;
	/** The most characters the text may have, none when left out or null. */
	limit?: number | null;
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
	/** More extensions: Tiptap's, or the names of those registered. */
	extensions?: readonly RichTextExtension[];
	/**
	 * Whether the code blocks show in the colors of their language: true with the highlighter
	 * registered (registerRichTextHighlighter, which Wicket's antilia-rich-text-highlight.js calls),
	 * or a highlighter of the page's own, lezerHighlighter for example; false when left out.
	 */
	highlight?: boolean | RichTextHighlighter;
	/**
	 * The languages of code blocks the menu of the tool code block offers, by their names: those of
	 * the highlighter when left out while highlighting, none otherwise; false for none. Without
	 * languages, the tool makes a code block, or takes it away.
	 */
	codeLanguages?: readonly string[] | false;
	/**
	 * Uploads an image the user chose in the form of the image, dropped into the text or pasted,
	 * answering the address it is served at, which must be safe (isSafeUrl): see uploadImageTo().
	 * None when left out or null: an image is then an address the user gives.
	 */
	onUpload?: RichTextUploader | null;
	/** The types of the images the user may upload: PNG, JPEG, GIF and WebP when left out. */
	uploadTypes?: readonly string[];
	/** The largest image the user may upload, in bytes; none when left out or null. */
	uploadMaxSize?: number | null;
	/** What the editor says, in English when left out. */
	texts?: Partial<RichTextEditorTexts>;
	/** The look of the editor; that of the element it is in when left out (see lookOf). */
	look?: Look;
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

/** The events the editor fires, on its text area or its element, which bubble. */
export const RICH_TEXT_EDITOR_EVENTS = Object.freeze({
	/** The user changed the text. */
	change: 'antilia-rich-text-editor:change',
	/** The editor got the focus. */
	focus: 'antilia-rich-text-editor:focus',
	/** The editor lost the focus. */
	blur: 'antilia-rich-text-editor:blur',
	/** The user moved the cursor or selected text. */
	select: 'antilia-rich-text-editor:select',
	/** The user pressed Ctrl+S, or Cmd+S. */
	save: 'antilia-rich-text-editor:save',
	/** The user resized the editor. */
	resize: 'antilia-rich-text-editor:resize',
	/** The text came to have changes not saved, or no longer has. */
	modified: 'antilia-rich-text-editor:modified',
	/** The state was reported: what onStateChange is given. */
	state: 'antilia-rich-text-editor:state',
	/** The editor has a text: it started, or its text changed, by the user or by the page. */
	text: 'antilia-rich-text-editor:text'
});

/** What the events of the editor carry, besides what each tells. */
export interface RichTextEditorEventDetail {
	editor: RichTextEditor;
	selection?: RichTextEditorSelection;
	state?: RichTextEditorState;
	modified?: boolean;
	size?: ResizableSize;
}

/** The classes of the editor and its states. */
export const RICH_TEXT_EDITOR_CLASSES = Object.freeze({
	editor: 'antilia-rich-text-editor',
	field: 'antilia-rich-text-editor-field',
	/** Of a text area until the editor is made of it, which the style sheet hides meanwhile. */
	loading: 'antilia-rich-text-editor-loading',
	readOnly: 'antilia-rich-text-editor-readonly',
	disabled: 'antilia-rich-text-editor-disabled',
	focused: 'antilia-rich-text-editor-focused'
});

const DEFAULTS = { stateDelay: 400, resizable: 'vertical' as ResizeAxis, minWidth: 200,
	minHeight: 96 };

/** Marks the transactions of the page, not the user's, which fire no events. */
const EXTERNAL = 'antilia-rich-text-editor:external';

/** The options that make the schema: changing one makes the editor anew. */
const SCHEMA_OPTIONS: readonly (keyof RichTextEditorOptions)[] = ['format', 'features',
	'headingLevels', 'limit', 'extensions', 'bubbleMenu'];

function resolve(target: HTMLElement | string): HTMLElement | null {
	return typeof target === 'string' ? document.getElementById(target) : target;
}

/**
 * @returns a state in one shape whatever its source: no selection is a cursor at the start, no
 *          scrolling none and no size the page's
 */
function canonicalState(state: RichTextEditorState | null | undefined): string {
	if (!state) {
		return 'null';
	}
	return JSON.stringify([state.selection?.anchor ?? 0, state.selection?.head ?? 0,
		Math.round(state.scrollTop ?? 0), Math.round(state.scrollLeft ?? 0), state.width ?? null,
		state.height ?? null]);
}

function sameState(one: RichTextEditorState | null | undefined,
	other: RichTextEditorState | null | undefined): boolean {
	return canonicalState(one) === canonicalState(other);
}

/**
 * @returns whether a document holds nothing: one paragraph, empty and plain
 */
function isEmptyDocument(doc: ProseMirrorNode): boolean {
	const first = doc.firstChild;
	return doc.childCount === 1 && !!first && first.type.name === 'paragraph' &&
		first.content.size === 0 && !first.attrs.textAlign;
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
const instances = new WeakMap<Element, RichTextEditor>();

/** What editors the page took away go on with, by the id of their text area or element. */
const continuations = new Map<string, Continuation>();

export class RichTextEditor {
	/** The element of the editor, holding the text area, if any, the toolbar and the text. */
	readonly element: HTMLElement;

	/** The text area the editor keeps the text in, if it has one. */
	readonly field: HTMLTextAreaElement | null;

	/** The element the text scrolls in, holding Tiptap's. */
	readonly content: HTMLElement;

	/** Tiptap's editor, for what the options do not offer; made anew when the schema changes. */
	editor: Editor;

	options: RichTextEditorOptions;

	/** The text last saved, as the editor writes it. */
	private saved: string;

	private modified = false;

	private readonly look: Look;

	/** Whether the element was made by the editor, and is taken away again by destroy(). */
	private readonly ownElement: boolean;

	/** Whether the text area was hidden before the editor hid it. */
	private readonly fieldHidden: HTMLElement['hidden'] = false;

	private toolbar: RichTextToolbar | null = null;

	private bubble: RichTextToolbar | null = null;

	private status: HTMLElement | null = null;

	/** The message telling the user an upload went wrong, while it shows. */
	private message: HTMLElement | null = null;

	private messageTimer: ReturnType<typeof setTimeout> | null = null;

	/** What the uploads ask of the editor, made once. */
	private uploads: UploadHost | null = null;

	private resizable: Resizable | null = null;

	/** The size the editor has while not resizable, as given. */
	private size: ResizableSize = { width: null, height: null };

	/** How far the text is scrolled, while the editor is shown. */
	private scroll = { top: 0, left: 0 };

	/** Whether the editor is shown, not in a hidden tab for example. */
	private shown = false;

	/** The state last reported, null before. */
	private reported: RichTextEditorState | null = null;

	/** Whether the text was reported, with the state, since the user last changed it. */
	private textReported = true;

	/** Whether the focus is in the editor, its toolbar or a form of it. */
	private focused = false;

	/** The text when the editor got the focus, for the event change. */
	private focusValue: string | null = null;

	/** The text the editor has, as it writes it. */
	private value = '';

	/** The selection last told of. */
	private selected: RichTextEditorSelection = { anchor: 0, head: 0 };

	/** Whether Tiptap's editor is being made anew, its toolbars not yet. */
	private rebuilding = false;

	/** Whether Escape was the last key: Tab then leaves the editor, even in a list. */
	private escaped = false;

	private timer: ReturnType<typeof setTimeout> | null = null;

	private listeners: Array<[EventTarget, string, EventListener, boolean]> = [];

	private observer: ResizeObserver | null = null;

	private stopWatchingRemoval: Unsubscribe | null = null;

	/** Whether the editor is firing an event on its text area, which it then does not hear. */
	private dispatching = false;

	private destroyed = false;

	/**
	 * @param target a text area, or an element: the editor's, with the text area to keep the text
	 *        in, if any
	 * @param options the text, its format, the features and the toolbar; the state it starts in
	 *        and the callbacks
	 */
	constructor(target: HTMLElement, options: RichTextEditorOptions = {}) {
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
			} else if (parent && parent.classList.contains(RICH_TEXT_EDITOR_CLASSES.editor)) {
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

		const given = options.value ?? field?.value ?? '';
		const content = document.createElement('div');
		content.className = this.css('rich-text-content');
		// Firefox puts a box that scrolls in the order of Tab: the text in it is the stop
		content.tabIndex = -1;
		this.content = content;
		this.render();
		element.append(content);
		this.editor = this.create(given);
		this.value = this.serialize(this.editor.state.doc);
		this.saved = options.saved !== undefined ? this.canonical(options.saved) :
			field ? this.canonical(field.defaultValue) : this.value;
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
		this.applySelection(state?.selection);
		this.selected = this.selection();
		this.scroll = { top: Math.max(state?.scrollTop ?? 0, 0),
			left: Math.max(state?.scrollLeft ?? 0, 0) };
		this.size = { width: state?.width ?? null, height: state?.height ?? null };
		this.renderToolbar();
		this.renderStatus();
		this.renderResizable();
		this.syncField(this.value);
		this.renderModified(this.value, false);
		this.bind();
		this.restoreScroll();
		this.stopWatchingRemoval = onRemoving(field ?? element, () => this.remove());
		this.dispatch('text');
	}

	/**
	 * Makes an editor of a text area or an element, as Wicket does.
	 *
	 * @param target the text area or the element, or its id
	 * @param options the text, its format, the features and the toolbar; the state and the
	 *        callbacks
	 * @returns the editor, null when there is no such element
	 */
	static attach(target: HTMLElement | string,
		options?: RichTextEditorOptions): RichTextEditor | null {
		const resolved = resolve(target);
		return resolved ? new RichTextEditor(resolved, options) : null;
	}

	/**
	 * @param target the text area or the element of an editor, or its id
	 * @returns the editor, if there is one
	 */
	static of(target: HTMLElement | string | null | undefined): RichTextEditor | null {
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
	format(): RichTextFormat {
		const given = this.options.format;
		return given && RICH_TEXT_FORMATS.includes(given) ? given : 'html';
	}

	/**
	 * @returns what the text may hold
	 */
	features(): ReadonlySet<RichTextFeature> {
		const given = this.options.features;
		if (given) {
			return new Set(given.filter((feature) => RICH_TEXT_FEATURES.includes(feature)));
		}
		return new Set(this.format() === 'markdown' ? RICH_TEXT_FEATURES.filter((feature) =>
			!RICH_TEXT_NOT_IN_MARKDOWN.includes(feature)) : RICH_TEXT_FEATURES);
	}

	private headingLevels(): readonly HeadingLevel[] {
		const given = this.options.headingLevels;
		return given ? [...new Set(given.filter((level) => Number.isInteger(level) && level >= 1 &&
			level <= 6))].sort() : DEFAULT_HEADING_LEVELS;
	}

	private texts(): RichTextEditorTexts {
		return { ...DEFAULT_RICH_TEXT_EDITOR_TEXTS, ...this.options.texts };
	}

	/**
	 * @returns the dimensions the user resizes the editor in, null for none
	 */
	private axis(): ResizeAxis | null {
		const axis = this.options.resizable;
		return axis === false ? null : axis ?? DEFAULTS.resizable;
	}

	private render(): void {
		const element = this.element;
		element.classList.add(...this.css('rich-text-editor').split(' '));
		element.classList.toggle(RICH_TEXT_EDITOR_CLASSES.readOnly, this.isReadOnly());
		element.classList.toggle(RICH_TEXT_EDITOR_CLASSES.disabled, this.isDisabled());
		const field = this.field;
		if (field) {
			field.classList.remove(RICH_TEXT_EDITOR_CLASSES.loading);
			field.classList.add(...this.css('rich-text-editor-field').split(' '));
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
	 * @returns the attributes of the text, ProseMirror's element
	 */
	private contentAttributes(): Record<string, string> {
		const attributes: Record<string, string> = {
			role: 'textbox',
			'aria-multiline': 'true',
			class: 'antilia-rich-text-text'
		};
		const label = this.label();
		if (label) {
			attributes['aria-label'] = label;
		}
		if (this.isReadOnly()) {
			attributes['aria-readonly'] = 'true';
		}
		return attributes;
	}

	/**
	 * @returns Tiptap's editor, with the extensions of the options and the text
	 */
	private create(value: string): Editor {
		const textFormat = this.format();
		let content: string | Record<string, unknown> = value;
		if (textFormat === 'json') {
			content = this.parseJson(value);
		} else if (textFormat === 'html') {
			content = quietHtml(value);
		}
		const behavior = Extension.create({
			name: 'antiliaRichTextEditor',
			addKeyboardShortcuts: () => ({
				'Mod-s': () => {
					this.save();
					return true;
				},
				'Mod-k': () => this.openLink()
			})
		});
		const extensions: AnyExtension[] = [
			...schemaExtensions({
				features: this.features(),
				format: textFormat,
				headingLevels: this.headingLevels(),
				placeholder: () => this.options.placeholder ?? '',
				limit: this.options.limit ?? null
			}),
			behavior,
			...(this.options.extensions ?? []).flatMap((extension) =>
				typeof extension === 'string' ? extensionsNamed(extension) : [extension])
		];
		const features = this.features();
		if (features.has('codeBlock')) {
			extensions.push(CodeBlocks.configure({ highlighter: () => this.highlighter(),
				label: (language) => this.languageLabel(language) }));
		}
		if (features.has('image')) {
			extensions.push(uploadExtension(this.uploadHost()));
		}
		if (this.options.bubbleMenu) {
			const bubble = new RichTextToolbar(this.toolbarContext(), [RICH_TEXT_BUBBLE_TOOLS],
				'rich-text-bubble');
			this.bubble = bubble;
			extensions.push(BubbleMenu.configure({
				element: bubble.element,
				appendTo: () => this.element,
				shouldShow: ({ editor, view, state, from, to }) => {
					if (bubble.isFormOpen()) {
						return true;
					}
					const inMenu = bubble.element.contains(document.activeElement);
					return (view.hasFocus() || inMenu) && editor.isEditable && !state.selection.empty &&
						state.selection instanceof TextSelection &&
						state.doc.textBetween(from, to).length > 0 && !editor.isActive('codeBlock');
				},
				options: { placement: 'top', offset: 8, flip: true, shift: { padding: 8 } }
			}));
		}
		const editor = new Editor({
			element: this.content,
			extensions,
			content,
			contentType: textFormat === 'markdown' ? 'markdown' : undefined,
			editable: !this.isReadOnly(),
			injectCSS: false,
			autofocus: false,
			editorProps: { attributes: this.contentAttributes(),
				transformPastedHTML: (html: string) => quietHtml(html) }
		});
		editor.on('transaction', ({ transaction }) => this.onTransaction(editor, transaction));
		return editor;
	}

	private parseJson(value: string): Record<string, unknown> | string {
		if (!value.trim()) {
			return '';
		}
		try {
			return JSON.parse(value) as Record<string, unknown>;
		} catch (error) {
			getHost().log.error('The text of the rich text editor is no JSON', error);
			return '';
		}
	}

	/**
	 * @returns a document of the editor's schema holding a text in the editor's format
	 */
	private parse(value: string): ProseMirrorNode {
		const schema = this.editor.schema;
		const textFormat = this.format();
		try {
			if (textFormat === 'markdown' && value.trim() && this.editor.markdown) {
				const json = this.editor.markdown.parse(value);
				if (json.content?.length) {
					return schema.nodeFromJSON(json);
				}
			} else if (textFormat === 'json') {
				const json = this.parseJson(value);
				return createDocument(json === '' ? '' : json, schema);
			} else if (textFormat === 'html') {
				return createDocument(quietHtml(value), schema);
			}
		} catch (error) {
			getHost().log.error('The rich text editor could not read the text', error);
		}
		return createDocument('', schema);
	}

	/**
	 * @returns the text of a document in the editor's format: '' for an empty one
	 */
	private serialize(doc: ProseMirrorNode): string {
		const textFormat = this.format();
		if (textFormat === 'markdown') {
			return isEmptyDocument(doc) ? '' : this.editor.markdown?.serialize(doc.toJSON()) ?? '';
		}
		if (isEmptyDocument(doc)) {
			return '';
		}
		return textFormat === 'json' ? JSON.stringify(doc.toJSON()) :
			getHTMLFromFragment(doc.content, this.editor.schema);
	}

	/**
	 * @returns a text as the editor writes it: its HTML of the same content, for one
	 */
	private canonical(value: string): string {
		return this.serialize(this.parse(value));
	}

	private toolbarContext(): RichTextToolbarContext {
		return {
			editor: () => this.editor,
			look: this.look,
			texts: () => this.texts(),
			features: () => this.features(),
			headingLevels: () => this.headingLevels(),
			codeLanguages: () => this.codeLanguages(),
			uploads: () => this.uploadHost(),
			onForm: (open) => {
				if (!open) {
					setTimeout(() => {
						this.checkFocus();
						// the bubble menu goes, unless the focus came back to the text
						if (!this.destroyed && !this.editor.isDestroyed) {
							this.editor.view.dispatch(this.editor.state.tr.setMeta(EXTERNAL, true));
						}
					});
				}
			}
		};
	}

	/**
	 * @returns the highlighter of the code blocks, null for none
	 */
	private highlighter(): RichTextHighlighter | null {
		const highlight = this.options.highlight;
		if (typeof highlight === 'object' && highlight !== null) {
			return highlight;
		}
		return highlight ? richTextHighlighter() : null;
	}

	/**
	 * @returns the languages of the menu of the code block: those given, or those of the
	 *          highlighter while highlighting; each once, by its name
	 */
	codeLanguages(): RichTextCodeLanguage[] {
		const given = this.options.codeLanguages;
		if (given === false || !this.features().has('codeBlock')) {
			return [];
		}
		const highlighter = this.highlighter();
		const names = given ?? (this.options.highlight ?
			Object.keys(highlighter?.languages ?? RICH_TEXT_CODE_LANGUAGES) : []);
		const seen = new Set<string>();
		return names.map((name) => codeLanguage(name)).filter((name): name is string => {
			if (name === null || seen.has(name)) {
				return false;
			}
			seen.add(name);
			return true;
		}).map((name) => ({ name, label: this.labelOf(name) }));
	}

	/**
	 * @returns what the editor calls a language: as the highlighter does, or the editor; its name
	 *          for one neither knows
	 */
	private labelOf(name: string): string {
		const own = (labels: Readonly<Record<string, string>> | undefined) =>
			labels && Object.prototype.hasOwnProperty.call(labels, name) ? labels[name] : undefined;
		return own(this.highlighter()?.languages) ?? own(RICH_TEXT_CODE_LANGUAGES) ?? name;
	}

	/**
	 * @returns what a code block shows of its language: what the editor calls it while the menu
	 *          offers languages, nothing otherwise
	 */
	private languageLabel(language: string): string | null {
		return this.codeLanguages().length ? this.labelOf(language) : null;
	}

	/**
	 * @returns what the uploads of images ask of the editor
	 */
	private uploadHost(): UploadHost {
		if (!this.uploads) {
			this.uploads = {
				editor: () => this.editor,
				enabled: () => !!this.options.onUpload && !this.isReadOnly() &&
					this.features().has('image'),
				types: () => this.uploadTypes(),
				refusal: (file) => this.refusal(file),
				uploading: (file) => format(this.texts().uploading, this.fileName(file)),
				upload: (file) => this.upload(file),
				alert: (message) => this.alert(message)
			};
		}
		return this.uploads;
	}

	/**
	 * @returns the name of a file as the messages say it, which a file pasted may not have
	 */
	private fileName(file: File): string {
		return file.name || this.texts().theImage;
	}

	private uploadTypes(): string[] {
		return (this.options.uploadTypes ?? DEFAULT_RICH_TEXT_UPLOAD_TYPES).map((type) =>
			type.trim().toLowerCase()).filter(Boolean);
	}

	/**
	 * @returns why a file cannot be uploaded: not of the types taken, or too large; null when it
	 *          can
	 */
	private refusal(file: File): string | null {
		const texts = this.texts();
		const types = this.uploadTypes();
		if (!types.includes(file.type.toLowerCase())) {
			return format(texts.uploadType, this.fileName(file), typesText(types));
		}
		const max = this.options.uploadMaxSize;
		if (max != null && file.size > max) {
			return format(texts.uploadTooLarge, this.fileName(file), sizeText(max));
		}
		return null;
	}

	/**
	 * Uploads an image, by the page.
	 *
	 * @returns the address of the image, a safe one; rejected with the message saying why not
	 */
	private async upload(file: File): Promise<string> {
		const texts = this.texts();
		const failed = format(texts.uploadFailed, this.fileName(file));
		const uploader = this.options.onUpload;
		const refused = this.refusal(file);
		if (!uploader || refused) {
			throw new Error(refused ?? failed);
		}
		let address: string;
		try {
			address = String(await uploader(file)).trim();
		} catch (error) {
			throw new Error(error instanceof Error && error.message ? error.message : failed);
		}
		if (!isSafeUrl(address)) {
			getHost().log.error('The address of an image uploaded is not one the editor takes: ' +
				address);
			throw new Error(failed);
		}
		return address;
	}

	/**
	 * Tells the user something went wrong, above the text, for a while.
	 */
	private alert(text: string): void {
		if (this.destroyed) {
			return;
		}
		if (!this.message) {
			this.message = document.createElement('div');
			this.message.className = 'antilia-rich-text-message';
			this.message.setAttribute('role', 'alert');
			this.content.before(this.message);
		}
		this.message.textContent = text;
		if (this.messageTimer !== null) {
			clearTimeout(this.messageTimer);
		}
		this.messageTimer = setTimeout(() => this.clearMessage(), 8000);
	}

	private clearMessage(): void {
		if (this.messageTimer !== null) {
			clearTimeout(this.messageTimer);
			this.messageTimer = null;
		}
		this.message?.remove();
		this.message = null;
	}

	/**
	 * Draws the toolbar, or takes it away, as the options say.
	 */
	private renderToolbar(): void {
		const groups = this.options.toolbar === false ? [] :
			this.options.toolbar ?? DEFAULT_RICH_TEXT_TOOLBAR;
		if (!groups.length) {
			this.toolbar?.destroy();
			this.toolbar = null;
		} else if (this.toolbar) {
			this.toolbar.render(groups);
		} else {
			this.toolbar = new RichTextToolbar(this.toolbarContext(), groups);
			this.element.insertBefore(this.toolbar.element, this.content);
		}
		if (this.toolbar) {
			this.toolbar.element.setAttribute('aria-controls', this.editor.view.dom.id ||
				this.textId());
			this.toolbar.element.hidden = this.toolbar.element.hidden || this.isReadOnly();
		}
		this.bubble?.render([RICH_TEXT_BUBBLE_TOOLS], 'rich-text-bubble');
	}

	/**
	 * @returns the id of the text, given to it when it has none
	 */
	private textId(): string {
		const dom = this.editor.view.dom;
		if (!dom.id) {
			dom.id = (this.field?.id || this.element.id || 'antilia-rich-text-' + (++texts)) +
				'-text';
		}
		return dom.id;
	}

	/**
	 * Shows the count of the characters and the words below the text, or takes it away.
	 */
	private renderStatus(): void {
		const shown = !!this.options.count || this.options.limit != null;
		if (!shown) {
			this.status?.remove();
			this.status = null;
			return;
		}
		if (!this.status) {
			this.status = document.createElement('div');
			this.status.className = this.css('rich-text-status');
			this.content.after(this.status);
		}
		const storage = this.editor.storage.characterCount;
		const characters = storage.characters();
		const words = storage.words();
		const texts = this.texts();
		const limit = this.options.limit;
		const counted = limit != null ? format(texts.charactersOfLimit, characters, limit) :
			format(characters === 1 ? texts.character : texts.characters, characters);
		this.status.textContent = counted + ' · ' + format(words === 1 ? texts.word : texts.words,
			words);
		this.status.classList.toggle('antilia-rich-text-status-full', limit != null &&
			characters >= limit);
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

	private listen<E extends Event>(target: EventTarget, type: string,
		listener: (event: E) => void, options: AddEventListenerOptions | boolean = false): void {
		target.addEventListener(type, listener as EventListener, options);
		this.listeners.push([target, type, listener as EventListener,
			typeof options === 'boolean' ? options : !!options.capture]);
	}

	private bind(): void {
		const scroller = this.content;
		this.listen(scroller, 'scroll', () => {
			// hidden, a scroller is at its start: the editor keeps where it was
			if (scroller.clientHeight > 0) {
				this.scroll = { top: scroller.scrollTop, left: scroller.scrollLeft };
				this.stateChanged();
			}
		}, { passive: true });
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
		// a click beside the text, below its last line, puts the cursor at its end
		this.listen<MouseEvent>(scroller, 'mousedown', (event) => {
			if (event.target === scroller && !this.editor.isDestroyed && this.editor.isEditable) {
				event.preventDefault();
				this.editor.commands.focus('end');
			}
		});
		this.listen(this.element, 'focusin', () => this.onFocusIn());
		this.listen(this.element, 'focusout', () => setTimeout(() => this.checkFocus()));
		// Escape, then Tab, leaves the editor, even where Tab indents a list or goes to a cell
		this.listen<KeyboardEvent>(this.element, 'keydown', (event) => {
			if (!this.editor.view.dom.contains(event.target as Node)) {
				return;
			}
			if (event.key === 'Tab' && this.escaped) {
				event.stopPropagation();
			}
			if (event.key !== 'Shift') {
				this.escaped = event.key === 'Escape';
			}
		}, true);
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
	 * @returns whether the focus is in the editor: its text, its toolbar, its bubble menu or a form
	 *          of theirs
	 */
	private hasFocus(): boolean {
		const active = document.activeElement;
		if (!active) {
			return false;
		}
		return this.element.contains(active) || !!this.toolbar?.formElement()?.contains(active) ||
			!!this.bubble?.formElement()?.contains(active) || !!this.toolbar?.isFormOpen() ||
			!!this.bubble?.isFormOpen();
	}

	private onFocusIn(): void {
		if (this.focused || this.destroyed) {
			return;
		}
		this.focused = true;
		this.element.classList.add(RICH_TEXT_EDITOR_CLASSES.focused);
		this.focusValue = this.getValue();
		this.options.onFocus?.();
		this.dispatch('focus');
	}

	/**
	 * Tells the editor lost the focus, once it is out of the editor and its forms.
	 */
	private checkFocus(): void {
		if (!this.focused || this.destroyed || this.hasFocus()) {
			return;
		}
		this.focused = false;
		this.element.classList.remove(RICH_TEXT_EDITOR_CLASSES.focused);
		const value = this.getValue();
		if (this.focusValue !== null && this.focusValue !== value) {
			this.fieldEvent('change');
		}
		this.focusValue = null;
		this.options.onBlur?.(value);
		this.dispatch('blur');
		this.report(false);
	}

	private onTransaction(editor: Editor, transaction: Transaction): void {
		if (editor !== this.editor || this.destroyed) {
			return;
		}
		const rebuilding = this.rebuilding;
		const user = !transaction.getMeta(EXTERNAL);
		if (transaction.docChanged) {
			const value = this.serialize(editor.state.doc);
			if (value !== this.value) {
				this.value = value;
				this.syncField(value);
				this.renderModified(value, true);
				this.dispatch('text');
				if (user) {
					this.textReported = false;
					this.options.onChange?.(value);
					this.fieldEvent('input');
					this.dispatch('change');
					this.stateChanged();
				}
			}
			this.renderStatus();
		}
		const selection = this.selection();
		if (selection.anchor !== this.selected.anchor || selection.head !== this.selected.head) {
			this.selected = selection;
			if (user) {
				this.options.onSelectionChange?.(selection);
				this.dispatch('select', { selection });
				this.stateChanged();
			}
		}
		if (!rebuilding) {
			this.toolbar?.update();
			this.bubble?.update();
		}
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
	 * Fires an event of the editor on its text area, or its element without one.
	 */
	private dispatch(name: keyof typeof RICH_TEXT_EDITOR_EVENTS,
		detail: Omit<RichTextEditorEventDetail, 'editor'> = {}): void {
		const target = this.field ?? this.element;
		this.dispatching = true;
		try {
			target.dispatchEvent(new CustomEvent<RichTextEditorEventDetail>(
				RICH_TEXT_EDITOR_EVENTS[name], { bubbles: true, detail: { editor: this, ...detail } }));
		} finally {
			this.dispatching = false;
		}
	}

	/**
	 * Fires input or change on the text area, as a text area does when the user changes it.
	 */
	private fieldEvent(type: 'input' | 'change'): void {
		if (this.field) {
			this.dispatching = true;
			try {
				this.field.dispatchEvent(new Event(type, { bubbles: true }));
			} finally {
				this.dispatching = false;
			}
		}
	}

	/**
	 * Selects what a state says, within the text; the start of the text for none.
	 */
	private applySelection(selection: RichTextEditorSelection | undefined): boolean {
		if (!selection || !Number.isFinite(selection.anchor) || !Number.isFinite(selection.head)) {
			return false;
		}
		const doc = this.editor.state.doc;
		const clamp = (position: number) => Math.min(Math.max(Math.round(position), 0),
			doc.content.size);
		const chosen = TextSelection.between(doc.resolve(clamp(selection.anchor)),
			doc.resolve(clamp(selection.head)));
		this.editor.view.dispatch(this.editor.state.tr.setSelection(chosen).setMeta(EXTERNAL, true)
			.setMeta('addToHistory', false));
		return true;
	}

	private selection(): RichTextEditorSelection {
		const selection = this.editor.state.selection;
		return { anchor: selection.anchor, head: selection.head };
	}

	/**
	 * Scrolls the text where it was, once the editor is shown and laid out.
	 */
	private restoreScroll(): void {
		const { top, left } = this.scroll;
		if (top <= 0 && left <= 0) {
			return;
		}
		const scroller = this.content;
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
	 * @returns the text, as plain text: what a reader sees, without the formats
	 */
	getText(): string {
		return this.editor.getText();
	}

	/**
	 * Replaces the text, without calling back: the page decides. The selection stays, within the
	 * text. The user cannot undo it.
	 *
	 * @param value the text, in the editor's format
	 * @param saved whether it is the text last saved too
	 */
	setValue(value: string, saved = false): void {
		const doc = this.parse(value);
		const canonical = this.serialize(doc);
		if (saved) {
			this.saved = canonical;
		}
		if (canonical !== this.value) {
			const state = this.editor.state;
			const selection = this.selection();
			const tr = state.tr.replaceWith(0, state.doc.content.size, doc.content)
				.setMeta(EXTERNAL, true).setMeta('addToHistory', false);
			const size = tr.doc.content.size;
			const clamp = (position: number) => Math.min(Math.max(position, 0), size);
			tr.setSelection(TextSelection.between(tr.doc.resolve(clamp(selection.anchor)),
				tr.doc.resolve(clamp(selection.head))));
			this.editor.view.dispatch(tr);
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
		this.saved = this.canonical(value);
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
	 * Puts the focus in the text, where the cursor is.
	 */
	focus(): void {
		this.editor.commands.focus();
	}

	/**
	 * Opens the form of the link at the cursor, as Ctrl+K does.
	 *
	 * @returns whether it opened: not without the feature link, nor in a read only editor
	 */
	openLink(): boolean {
		return !!this.toolbar?.openLink() || !!this.bubble?.openLink();
	}

	/**
	 * Takes other options; those left out stay as they were. The text, the text last saved and
	 * the state stay too: setValue(), markSaved() and setState() change them. Other features, a
	 * format, the extensions, a limit or the bubble menu make Tiptap's editor anew.
	 */
	configure(options: RichTextEditorOptions = {}): void {
		const defined = Object.fromEntries(Object.entries(options)
			.filter(([, value]) => value !== undefined)) as RichTextEditorOptions;
		const before = this.options;
		this.options = { ...before, ...defined };
		const changed = (...names: (keyof RichTextEditorOptions)[]) => names.some((name) =>
			JSON.stringify(before[name] ?? null) !== JSON.stringify(this.options[name] ?? null));
		this.render();
		const readOnly = this.isReadOnly();
		// read only now, or no longer: the options, or the text area
		const editableChanged = this.editor.isEditable === readOnly;
		const rebuilt = changed(...SCHEMA_OPTIONS) || before.extensions !== this.options.extensions;
		if (rebuilt) {
			this.rebuild(before.format !== this.options.format);
		} else if (changed('placeholder', 'label') || editableChanged) {
			this.editor.setEditable(!readOnly, false);
			this.editor.setOptions({ editorProps: { attributes: this.contentAttributes(),
				transformPastedHTML: (html: string) => quietHtml(html) } });
			// the placeholder shows anew
			this.editor.view.dispatch(this.editor.state.tr.setMeta(EXTERNAL, true));
		}
		const codeChanged = before.highlight !== this.options.highlight ||
			changed('codeLanguages');
		if (codeChanged && !rebuilt) {
			redrawCodeBlocks(this.editor.view);
		}
		if (rebuilt || editableChanged || codeChanged || changed('toolbar', 'texts')) {
			this.renderToolbar();
		} else {
			this.toolbar?.update();
		}
		if (rebuilt || changed('count', 'texts')) {
			this.renderStatus();
		}
		this.renderResizable();
	}

	/**
	 * Makes Tiptap's editor anew, for another schema, with the text and the selection it had.
	 *
	 * @param reformat whether the format changed: the text then reads in the new one
	 */
	private rebuild(reformat: boolean): void {
		const selection = this.selection();
		// through HTML, which the new schema reads leaving out what it has not: the editor's,
		// which has no style
		const html = this.editor.getHTML();
		this.bubble?.destroy();
		this.bubble = null;
		this.editor.destroy();
		this.content.replaceChildren();
		this.rebuilding = true;
		try {
			this.editor = this.create('');
			const state = this.editor.state;
			this.editor.view.dispatch(state.tr.replaceWith(0, state.doc.content.size,
				createDocument(html, this.editor.schema).content).setMeta(EXTERNAL, true)
				.setMeta('addToHistory', false));
		} finally {
			this.rebuilding = false;
		}
		this.value = this.serialize(this.editor.state.doc);
		if (reformat) {
			this.saved = this.value;
		}
		this.applySelection(selection);
		this.selected = this.selection();
		this.syncField(this.value);
		this.renderModified(this.value, true);
		this.dispatch('text');
	}

	/**
	 * Takes the editor away: Tiptap's, the toolbar, the handle resizing it and what it gave the
	 * elements; the text area is shown again, with the text, and a wrapper the editor made goes.
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
			field.classList.remove(...this.css('rich-text-editor-field').split(' '));
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
			element.classList.remove(...this.css('rich-text-editor').split(' '),
				...Object.values(RICH_TEXT_EDITOR_CLASSES));
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
	 * Stops the editor, leaving its element and its text area: Tiptap's editor, the toolbar and
	 * the handle resizing it go.
	 */
	private release(): void {
		if (this.destroyed) {
			return;
		}
		this.destroyed = true;
		if (this.timer !== null) {
			clearTimeout(this.timer);
			this.timer = null;
		}
		this.listeners.forEach(([target, type, listener, capture]) => {
			target.removeEventListener(type, listener, capture);
		});
		this.listeners = [];
		this.observer?.disconnect();
		this.observer = null;
		this.stopWatchingRemoval?.();
		this.stopWatchingRemoval = null;
		this.toolbar?.destroy();
		this.toolbar = null;
		this.bubble?.destroy();
		this.bubble = null;
		this.status?.remove();
		this.status = null;
		this.clearMessage();
		this.resizable?.destroy();
		this.resizable = null;
		this.editor.destroy();
		this.content.remove();
		if (instances.get(this.element) === this) {
			instances.delete(this.element);
		}
		if (this.field && instances.get(this.field) === this) {
			instances.delete(this.field);
		}
	}
}

/** Counts the texts given an id. */
let texts = 0;

/**
 * Makes an editor of an element, in the shape of a Svelte action: use:richTextEditor={options}.
 *
 * @param element the element of the editor, or a text area
 * @param options the text, its format, the features and the toolbar; the state and the callbacks
 * @returns update() taking other options, destroy() taking the editor away
 */
export function richTextEditor(element: HTMLElement, options?: RichTextEditorOptions): {
	update(options?: RichTextEditorOptions): void;
	destroy(): void;
} {
	const instance = new RichTextEditor(element, options);
	return {
		update: (next) => instance.configure(next),
		destroy: () => instance.destroy()
	};
}
