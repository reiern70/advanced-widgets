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
 * A code editor: CodeMirror 6, in the colors of the theme it is in and the look of the page, with
 * the grammar of a language highlighting the text, line numbers, folding, search (Ctrl+F), undo,
 * brackets closing and matching, several cursors, and Ctrl+S (Cmd+S) telling the page to save.
 *
 * The grammars are pluggable: a language is a name the editor knows (CODE_EDITOR_LANGUAGES: java,
 * javascript, typescript, json and markdown, whose fenced code blocks take the grammars of the
 * others), one a page registers with registerLanguage(), or a CodeMirror extension of its own.
 * A grammar registered with a function is loaded the first time an editor shows it, and a page
 * without CodeMirror's modules of its own, a Wicket page, builds one from the CodeMirror classes
 * the function is given (CODE_EDITOR_KIT), a StreamLanguage for example.
 *
 * new CodeEditor(element, options) works on a text area, which it hides and wraps in the element of
 * the editor, keeping the text in it: a form posts it, and the text area tells the page of the
 * user's changes with the events input, and change once the editor loses the focus, as a text area
 * does. It works on any other element too, an empty one React or Svelte renders, which becomes the
 * element of the editor; a text area in it, if any, is then the one the editor keeps the text in.
 *
 * Where the cursor is, what is selected, how far the text is scrolled and the size the user gave
 * the editor are its state (CodeEditorState). The editor keeps it while hidden, in a tab that is
 * not selected for example, and shows it again when shown; onStateChange reports it, a moment
 * after the user changed it, as the editor loses the focus and as the page goes, for a page that
 * keeps it, on the server for example, to give it back with the option state, after the user came
 * back to the page. Rendered anew in the same place with the same text, as Wicket does, the editor
 * goes on as it was.
 *
 * The editor tells whether the text has changes not saved, against the text last saved (saved,
 * markSaved()): as data-modified on its element, which tabs and windows of the desktop show, and
 * with onModifiedChange. Closing a tab or a window anyway puts the saved text back.
 *
 * Besides its callbacks, the editor fires events on the text area, or its element without one,
 * which bubble: CODE_EDITOR_EVENTS, for listeners of the page's own, and Wicket's Ajax behaviors.
 *
 * CodeMirror writes its style sheet into the page in a style element: under a Content Security
 * Policy without 'unsafe-inline', the element needs the nonce of the page, which the editor takes
 * from the scripts of the page unless given one. Its own sizes are custom properties set through
 * the CSSOM, as the resizable's, which makes the editor resizable by its bottom edge by default.
 */

import { closeBrackets, closeBracketsKeymap } from '@codemirror/autocomplete';
import { defaultKeymap, history, historyKeymap, indentWithTab } from '@codemirror/commands';
import { bracketMatching, foldGutter, foldKeymap, HighlightStyle, indentOnInput, indentUnit,
	Language, LanguageDescription, LanguageSupport, LRLanguage, StreamLanguage,
	syntaxHighlighting } from '@codemirror/language';
import { highlightSelectionMatches, searchKeymap } from '@codemirror/search';
import { Annotation, Compartment, EditorSelection, EditorState,
	type Extension } from '@codemirror/state';
import { crosshairCursor, drawSelection, dropCursor, EditorView, highlightActiveLine,
	highlightActiveLineGutter, highlightSpecialChars, keymap, lineNumbers,
	placeholder as placeholderText, rectangularSelection, type ViewUpdate } from '@codemirror/view';
import { classHighlighter, styleTags, tags } from '@lezer/highlight';

import { getHost, onRemoving, type Unsubscribe } from './host.js';
import { lookOf, partClasses, type Look, type LookPart } from './look.js';
import { Resizable, type ResizableSize, type ResizeAxis } from './resizable.js';

/**
 * The CodeMirror classes a function registering a grammar is given, for a page that has no
 * CodeMirror modules of its own, such as a Wicket page: the editor's, so that the grammar works
 * with them.
 */
export interface CodeEditorKit {
	EditorState: typeof EditorState;
	EditorView: typeof EditorView;
	EditorSelection: typeof EditorSelection;
	Language: typeof Language;
	LanguageSupport: typeof LanguageSupport;
	LanguageDescription: typeof LanguageDescription;
	LRLanguage: typeof LRLanguage;
	StreamLanguage: typeof StreamLanguage;
	HighlightStyle: typeof HighlightStyle;
	syntaxHighlighting: typeof syntaxHighlighting;
	styleTags: typeof styleTags;
	tags: typeof tags;
	keymap: typeof keymap;
}

/** The CodeMirror classes of the editor. */
export const CODE_EDITOR_KIT: CodeEditorKit = Object.freeze({
	EditorState, EditorView, EditorSelection, Language, LanguageSupport, LanguageDescription,
	LRLanguage, StreamLanguage, HighlightStyle, syntaxHighlighting, styleTags, tags, keymap
});

/**
 * Makes the grammar of a language, maybe loading it first: a CodeMirror extension, a
 * LanguageSupport usually.
 */
export type CodeEditorLanguageLoader = (kit: CodeEditorKit) => Extension | Promise<Extension>;

/**
 * The language of an editor: the name of one the editor knows or a page registered, or a
 * CodeMirror extension of the page's own.
 */
export type CodeEditorLanguage = string | Extension;

/** A range of the text, from where the selection starts to where its head, the cursor, is. */
export interface CodeEditorRange {
	anchor: number;
	head: number;
}

/** What is selected: the ranges, a cursor where a range is empty, and the index of the main one. */
export interface CodeEditorSelection {
	ranges: CodeEditorRange[];
	main: number;
}

/**
 * The state of an editor besides its text: what is selected, how far it is scrolled, in pixels,
 * and the size the user gave it, null for the size the page's style sheets give it.
 */
export interface CodeEditorState {
	selection?: CodeEditorSelection;
	scrollTop?: number;
	scrollLeft?: number;
	width?: number | null;
	height?: number | null;
}

/** What onStateChange is told besides the state. */
export interface CodeEditorStateChange {
	/** Whether the page is going: the last chance to keep the state. */
	leaving: boolean;
	/** Whether the text changed since the state was last reported. */
	changed: boolean;
}

/** What the editor says, in the language of the page. */
export interface CodeEditorTexts {
	/** The accessible name of the handle resizing the editor. */
	resize: string;
	/**
	 * CodeMirror's phrases, those of its search panel for example, by their English text (see
	 * EditorState.phrases): "Find", "Replace", "next" and so on.
	 */
	phrases: Record<string, string>;
}

/**
 * How an editor shows the text and what it lets the user do.
 */
export interface CodeEditorOptions {
	/** The text; that of the text area, or none, when left out. */
	value?: string;
	/** The text last saved, against which the editor tells of changes; the text when left out. */
	saved?: string;
	/** The grammar highlighting the text, none (plain text) when left out or null. */
	language?: CodeEditorLanguage | null;
	/** Whether the lines show their numbers, true when left out. */
	lineNumbers?: boolean;
	/** Whether blocks fold, with arrows in the gutter and Ctrl+Shift+[ and ], true when left out. */
	folding?: boolean;
	/** Whether the user cannot change the text, false when left out; read only text areas are so. */
	readOnly?: boolean;
	/**
	 * Whether the user can put the cursor in the text and select it, true when left out; false
	 * shows the text as a picture, read only.
	 */
	selectable?: boolean;
	/** Whether long lines wrap, false when left out. */
	lineWrapping?: boolean;
	/** The width of a tab, and of a level of indentation, in spaces: 4 when left out. */
	tabSize?: number;
	/**
	 * Whether Tab indents, true when left out: then Escape, followed by Tab, takes the focus out
	 * of the editor.
	 */
	indentWithTab?: boolean;
	/** The text shown while the editor is empty. */
	placeholder?: string;
	/** The accessible name of the text; that of the labels of the text area when left out. */
	label?: string;
	/** The dimensions the user resizes the editor in, vertical when left out; false for none. */
	resizable?: ResizeAxis | false;
	/** The limits of the size the user gives the editor, in pixels. */
	minWidth?: number;
	minHeight?: number;
	maxWidth?: number | null;
	maxHeight?: number | null;
	/** The state the editor starts in: the selection, the scroll position and the size. */
	state?: CodeEditorState;
	/** How long after the user changed the state onStateChange reports it, 400 ms when left out. */
	stateDelay?: number;
	/** More CodeMirror extensions: key bindings, listeners and so on. */
	extensions?: Extension[];
	/** What the editor says, in English when left out. */
	texts?: Partial<CodeEditorTexts>;
	/** The look of the editor; that of the element it is in when left out (see lookOf). */
	look?: Look;
	/**
	 * The nonce of the Content Security Policy for CodeMirror's style element; that of the
	 * scripts of the page when left out.
	 */
	nonce?: string;
	/** Called with the text each time the user changed it. */
	onChange?: (value: string) => void;
	/** Called when the editor got the focus. */
	onFocus?: () => void;
	/** Called with the text when the editor lost the focus. */
	onBlur?: (value: string) => void;
	/** Called with the selection each time the user moved the cursor or selected text. */
	onSelectionChange?: (selection: CodeEditorSelection) => void;
	/**
	 * Called with the state a moment after the user changed it, as the editor loses the focus and
	 * as the page goes, when it changed since it was last reported.
	 */
	onStateChange?: (state: CodeEditorState, change: CodeEditorStateChange) => void;
	/** Called when the text came to have changes not saved, or no longer has. */
	onModifiedChange?: (modified: boolean) => void;
	/** Called with the text when the user pressed Ctrl+S, or Cmd+S. */
	onSave?: (value: string) => void;
	/** Called with the size once the user has resized the editor. */
	onResize?: (size: ResizableSize) => void;
}

/** The events the editor fires, on its text area or its element, which bubble. */
export const CODE_EDITOR_EVENTS = Object.freeze({
	/** The user changed the text. */
	change: 'antilia-code-editor:change',
	/** The editor got the focus. */
	focus: 'antilia-code-editor:focus',
	/** The editor lost the focus. */
	blur: 'antilia-code-editor:blur',
	/** The user moved the cursor or selected text. */
	select: 'antilia-code-editor:select',
	/** The user pressed Ctrl+S, or Cmd+S. */
	save: 'antilia-code-editor:save',
	/** The user resized the editor. */
	resize: 'antilia-code-editor:resize',
	/** The text came to have changes not saved, or no longer has. */
	modified: 'antilia-code-editor:modified',
	/** The state was reported: what onStateChange is given. */
	state: 'antilia-code-editor:state',
	/**
	 * The editor has a text: it started, or its text changed, by the user or by the page, for
	 * what shows the text elsewhere, a preview of Markdown for example.
	 */
	text: 'antilia-code-editor:text'
});

/** What the events of the editor carry, besides what each tells. */
export interface CodeEditorEventDetail {
	editor: CodeEditor;
	selection?: CodeEditorSelection;
	state?: CodeEditorState;
	modified?: boolean;
	size?: ResizableSize;
}

/** The classes of the editor and its states. */
export const CODE_EDITOR_CLASSES = Object.freeze({
	editor: 'antilia-code-editor',
	field: 'antilia-code-editor-field',
	readOnly: 'antilia-code-editor-readonly',
	disabled: 'antilia-code-editor-disabled',
	unselectable: 'antilia-code-editor-unselectable'
});

/** The English phrases of CodeMirror, which the texts of a page translate. */
export const CODE_EDITOR_PHRASES: readonly string[] = Object.freeze(['Find', 'Replace', 'next',
	'previous', 'all', 'match case', 'regexp', 'by word', 'replace', 'replace all', 'close',
	'current match', 'on line', 'replaced match on line $', 'replaced $ matches', 'Go to line', 'go',
	'Folded lines', 'Unfolded lines', 'to', 'folded code', 'unfold', 'Fold line', 'Unfold line',
	'Selection deleted', 'Control character']);

export const DEFAULT_CODE_EDITOR_TEXTS: Readonly<CodeEditorTexts> = Object.freeze({
	resize: 'Drag, or use the arrow keys, to resize the editor',
	phrases: Object.freeze({})
});

/** The theme custom property telling the color scheme, light or dark, of a theme. */
const COLOR_SCHEME_PROPERTY = '--antilia-theme-color-scheme';

const DEFAULTS = { tabSize: 4, stateDelay: 400, resizable: 'vertical' as ResizeAxis,
	minWidth: 160, minHeight: 64 };

/** Marks the transactions of the page, not the user's, which fire no events. */
const external = Annotation.define<boolean>();

/** The look of CodeMirror in the colors of the theme, those of code-editor.css. */
const THEME = EditorView.theme({
	'&': {
		height: '100%',
		color: 'var(--antilia-code-editor-text, inherit)',
		backgroundColor: 'var(--antilia-code-editor-background, transparent)',
		fontSize: 'var(--antilia-code-editor-font-size, 0.875em)'
	},
	'&.cm-focused': { outline: 'none' },
	'.cm-scroller': {
		fontFamily: 'var(--antilia-code-editor-font, ui-monospace, SFMono-Regular, Menlo, ' +
			'Consolas, monospace)',
		lineHeight: '1.5'
	},
	'.cm-content': { caretColor: 'var(--antilia-code-editor-cursor, currentColor)' },
	'.cm-cursor, .cm-dropCursor': {
		borderLeftColor: 'var(--antilia-code-editor-cursor, currentColor)'
	},
	['&.cm-focused > .cm-scroller > .cm-selectionLayer .cm-selectionBackground, ' +
		'.cm-content ::selection']: {
		backgroundColor: 'var(--antilia-code-editor-selection, #b4d5fe)'
	},
	'.cm-selectionBackground': {
		backgroundColor: 'var(--antilia-code-editor-selection-blurred, #d9d9d9)'
	},
	'.cm-activeLine': { backgroundColor: 'var(--antilia-code-editor-active-line, transparent)' },
	'.cm-gutters': {
		backgroundColor: 'var(--antilia-code-editor-gutter-background, transparent)',
		color: 'var(--antilia-code-editor-gutter-text, inherit)',
		borderRight: '1px solid var(--antilia-code-editor-border, #ddd)'
	},
	'.cm-activeLineGutter': {
		backgroundColor: 'var(--antilia-code-editor-active-line, transparent)',
		color: 'var(--antilia-code-editor-text, inherit)'
	},
	'.cm-foldPlaceholder': {
		backgroundColor: 'var(--antilia-code-editor-gutter-background, transparent)',
		borderColor: 'var(--antilia-code-editor-border, #ddd)',
		color: 'var(--antilia-code-editor-gutter-text, inherit)'
	},
	'.cm-searchMatch': {
		backgroundColor: 'var(--antilia-code-editor-match, #ffe58a)',
		outline: '1px solid var(--antilia-code-editor-border, #ddd)'
	},
	'.cm-searchMatch.cm-searchMatch-selected': {
		backgroundColor: 'var(--antilia-code-editor-match-selected, #ffc947)'
	},
	'.cm-selectionMatch': { backgroundColor: 'var(--antilia-code-editor-match, #ffe58a)' },
	'&.cm-focused .cm-matchingBracket': {
		backgroundColor: 'var(--antilia-code-editor-bracket, #bad0f8)',
		outline: '1px solid var(--antilia-code-editor-border, #ddd)'
	},
	'&.cm-focused .cm-nonmatchingBracket': {
		backgroundColor: 'transparent',
		outline: '1px solid var(--antilia-code-editor-invalid, #c53030)'
	},
	'.cm-placeholder': { color: 'var(--antilia-code-editor-gutter-text, #888)' },
	'.cm-panels': {
		backgroundColor: 'var(--antilia-code-editor-panel-background, #f5f5f5)',
		color: 'var(--antilia-code-editor-text, inherit)'
	},
	'.cm-panels.cm-panels-top': { borderBottom: '1px solid var(--antilia-code-editor-border, #ddd)' },
	'.cm-panels.cm-panels-bottom': { borderTop: '1px solid var(--antilia-code-editor-border, #ddd)' },
	'.cm-textfield': {
		backgroundColor: 'var(--antilia-code-editor-background, #fff)',
		color: 'var(--antilia-code-editor-text, inherit)',
		border: '1px solid var(--antilia-code-editor-border, #ddd)',
		borderRadius: '0.25em'
	},
	'.cm-button': {
		backgroundImage: 'none',
		backgroundColor: 'var(--antilia-code-editor-panel-button, #eee)',
		color: 'var(--antilia-code-editor-text, inherit)',
		border: '1px solid var(--antilia-code-editor-border, #ddd)',
		borderRadius: '0.25em'
	},
	'.cm-tooltip': {
		backgroundColor: 'var(--antilia-code-editor-background, #fff)',
		color: 'var(--antilia-code-editor-text, inherit)',
		border: '1px solid var(--antilia-code-editor-border, #ddd)'
	}
});

type LanguageEntry = CodeEditorLanguageLoader | Extension;

/** The grammars by the names of their languages, and their other names. */
const languages = new Map<string, LanguageEntry>([
	['java', () => import('@codemirror/lang-java').then((module) => module.java())],
	['javascript', () => import('@codemirror/lang-javascript').then((module) =>
		module.javascript())],
	['typescript', () => import('@codemirror/lang-javascript').then((module) =>
		module.javascript({ typescript: true }))],
	['json', () => import('@codemirror/lang-json').then((module) => module.json())],
	['markdown', () => import('@codemirror/lang-markdown').then((module) => module.markdown({
		base: module.markdownLanguage,
		// the fenced code blocks take the grammars of the editor
		codeLanguages: (info) => describe(info)
	}))]
]);

const aliases = new Map<string, string>([['js', 'javascript'], ['ts', 'typescript'],
	['md', 'markdown']]);

/** The grammars loaded, and those loading. */
const loaded = new Map<string, Extension>();
const loading = new Map<string, Promise<Extension | null>>();

/** The descriptions of the grammars for the code blocks of Markdown. */
const descriptions = new Map<string, LanguageDescription>();

/** The names of the languages the editor knows from the start. */
export const CODE_EDITOR_LANGUAGES: readonly string[] = Object.freeze(Array.from(languages.keys()));

/**
 * @returns the name a language is registered under, null when it is none
 */
function resolveLanguage(name: string): string | null {
	const key = name.trim().toLowerCase();
	const known = aliases.get(key) ?? key;
	return languages.has(known) ? known : null;
}

/**
 * Makes a grammar known to the editors, under the name of its language; it replaces one of that
 * name. A function is called the first time an editor shows the language.
 *
 * @param name the name of the language, which the option language and Wicket's
 *        CodeEditorLanguage name; the case does not matter
 * @param language the grammar, or a function making it, maybe loading it first
 * @param otherNames other names of the language, such as the info string of a code block of
 *        Markdown
 */
export function registerLanguage(name: string, language: CodeEditorLanguageLoader | Extension,
	otherNames: string[] = []): void {
	const key = name.trim().toLowerCase();
	languages.set(key, language);
	otherNames.forEach((other) => aliases.set(other.trim().toLowerCase(), key));
	loaded.delete(key);
	loading.delete(key);
	descriptions.delete(key);
}

/**
 * @returns the names of the languages the editors know: those they know from the start and
 *          those registered
 */
export function codeEditorLanguages(): string[] {
	return Array.from(languages.keys());
}

/**
 * @param name the name of a language, or one of its other names
 * @returns the grammar of the language, loaded once; null for a language not known
 */
export function loadLanguage(name: string): Promise<Extension | null> {
	const key = resolveLanguage(name);
	if (key === null) {
		return Promise.resolve(null);
	}
	const done = loaded.get(key);
	if (done) {
		return Promise.resolve(done);
	}
	let pending = loading.get(key);
	if (!pending) {
		const entry = languages.get(key)!;
		pending = Promise.resolve(typeof entry === 'function' ? entry(CODE_EDITOR_KIT) : entry)
			.then((extension) => {
				if (loading.get(key) === pending) {
					loaded.set(key, extension);
				}
				return extension;
			}, (error: unknown) => {
				loading.delete(key);
				getHost().log.error('The grammar of ' + key + ' did not load', error);
				return null;
			});
		loading.set(key, pending);
	}
	return pending;
}

/**
 * @param name the name of a language, or one of its other names
 * @returns the grammar of the language when loaded, null for a language not known, undefined
 *          for one not loaded yet
 */
export function loadedLanguage(name: string): Extension | null | undefined {
	const key = resolveLanguage(name);
	return key === null ? null : loaded.get(key);
}

/**
 * @returns the grammar of the language of a code block of Markdown, by the info string of the
 *          block, null for none known
 */
function describe(info: string): LanguageDescription | null {
	const key = resolveLanguage(info.split(/\s/)[0] ?? '');
	if (key === null) {
		return null;
	}
	let description = descriptions.get(key);
	if (!description) {
		description = LanguageDescription.of({
			name: key,
			load: () => loadLanguage(key).then((extension) => {
				const support = extension instanceof LanguageSupport ? extension :
					extension instanceof Language ? new LanguageSupport(extension) : null;
				if (!support) {
					throw new Error('The grammar of ' + key + ' is no language');
				}
				return support;
			})
		});
		descriptions.set(key, description);
	}
	return description;
}

/**
 * @returns the nonce of the scripts or style sheets of the page, if they have one
 */
function pageNonce(): string | undefined {
	const element = document.querySelector<HTMLElement>('script[nonce], style[nonce], link[nonce]');
	return element?.nonce || element?.getAttribute('nonce') || undefined;
}

function resolve(target: HTMLElement | string): HTMLElement | null {
	return typeof target === 'string' ? document.getElementById(target) : target;
}

/**
 * @returns a state in one shape whatever its source: no selection is a cursor at the start, no
 *          scrolling none and no size the page's
 */
function canonical(state: CodeEditorState | null | undefined): string {
	if (!state) {
		return 'null';
	}
	const ranges = state.selection?.ranges?.length ? state.selection.ranges : [{ anchor: 0, head: 0 }];
	return JSON.stringify([ranges.map((range) => [range.anchor, range.head]),
		state.selection?.ranges?.length ? state.selection.main ?? 0 : 0,
		Math.round(state.scrollTop ?? 0), Math.round(state.scrollLeft ?? 0), state.width ?? null,
		state.height ?? null]);
}

function sameState(one: CodeEditorState | null | undefined,
	other: CodeEditorState | null | undefined): boolean {
	return canonical(one) === canonical(other);
}

/**
 * What an editor rendered anew in the same place goes on with: its text, the text last saved,
 * its state, and the state it last reported.
 */
interface Continuation {
	value: string;
	saved: string;
	state: CodeEditorState;
	reported: CodeEditorState | null;
}

/** The editors, by their element and their text area. */
const instances = new WeakMap<Element, CodeEditor>();

/** What editors the page took away go on with, by the id of their text area or element. */
const continuations = new Map<string, Continuation>();

export class CodeEditor {
	/** The element of the editor, holding the text area, if any, and CodeMirror's. */
	readonly element: HTMLElement;

	/** The text area the editor keeps the text in, if it has one. */
	readonly field: HTMLTextAreaElement | null;

	/** CodeMirror's view, for what the options do not offer. */
	readonly view: EditorView;

	options: CodeEditorOptions;

	/** The text last saved. */
	private saved: string;

	private modified = false;

	private readonly look: Look;

	/** Whether the element was made by the editor, and is taken away again by destroy(). */
	private readonly ownElement: boolean;

	/** Whether the text area was hidden before the editor hid it. */
	private readonly fieldHidden: HTMLElement['hidden'] = false;

	private resizable: Resizable | null = null;

	/** The size the editor has while not resizable, as given. */
	private size: ResizableSize = { width: null, height: null };

	/** How far the text is scrolled, while the editor is shown. */
	private scroll = { top: 0, left: 0 };

	/** Whether the editor is shown, not in a hidden tab for example. */
	private shown = false;

	/** The state last reported, null before. */
	private reported: CodeEditorState | null = null;

	/** Whether the text was reported, with the state, since the user last changed it. */
	private textReported = true;

	/** The text when the editor got the focus, for the event change. */
	private focusValue: string | null = null;

	private timer: ReturnType<typeof setTimeout> | null = null;

	/** Counts the grammars asked for, so that one loading late does not replace a later one. */
	private languageRequest = 0;

	private readonly compartments = {
		language: new Compartment(),
		gutters: new Compartment(),
		editing: new Compartment(),
		wrapping: new Compartment(),
		indentation: new Compartment(),
		placeholder: new Compartment(),
		label: new Compartment(),
		phrases: new Compartment(),
		theme: new Compartment(),
		extensions: new Compartment()
	};

	private listeners: Array<[EventTarget, string, EventListener]> = [];

	private observer: ResizeObserver | null = null;

	private stopWatchingRemoval: Unsubscribe | null = null;

	/** Whether the editor is firing an event on its text area, which it then does not hear. */
	private dispatching = false;

	private destroyed = false;

	/**
	 * @param target a text area, or an element: the editor's, with the text area to keep the text
	 *        in, if any
	 * @param options the text, the language and what the user may do; the state it starts in and
	 *        the callbacks
	 */
	constructor(target: HTMLElement, options: CodeEditorOptions = {}) {
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
			} else if (parent && parent.classList.contains(CODE_EDITOR_CLASSES.editor)) {
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

		const value = options.value ?? field?.value ?? '';
		this.saved = options.saved ?? (field ? field.defaultValue : value);
		let state = options.state;
		// the page knows the state it gives
		this.reported = options.state ?? null;
		if (before && before.value === value) {
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

		this.render();
		this.view = new EditorView({
			parent: element,
			state: EditorState.create({
				doc: value,
				selection: this.selectionOf(state?.selection, value.length),
				extensions: this.extensions()
			})
		});
		this.scroll = { top: Math.max(state?.scrollTop ?? 0, 0),
			left: Math.max(state?.scrollLeft ?? 0, 0) };
		this.size = { width: state?.width ?? null, height: state?.height ?? null };
		this.renderResizable();
		this.syncField(value);
		this.renderModified(value, false);
		this.applyLanguage();
		this.bind();
		this.restoreScroll();
		this.stopWatchingRemoval = onRemoving(field ?? element, () => this.remove());
		this.dispatch('text');
	}

	/**
	 * Makes an editor of a text area or an element, as Wicket does.
	 *
	 * @param target the text area or the element, or its id
	 * @param options the text, the language and what the user may do; the state and the callbacks
	 * @returns the editor, null when there is no such element
	 */
	static attach(target: HTMLElement | string, options?: CodeEditorOptions): CodeEditor | null {
		const resolved = resolve(target);
		return resolved ? new CodeEditor(resolved, options) : null;
	}

	/**
	 * @param target the text area or the element of an editor, or its id
	 * @returns the editor, if there is one
	 */
	static of(target: HTMLElement | string | null | undefined): CodeEditor | null {
		const element = typeof target === 'string' ? document.getElementById(target) : target;
		return (element && instances.get(element)) || null;
	}

	/**
	 * @returns the classes of parts in the look of the editor
	 */
	private css(...parts: LookPart[]): string {
		return partClasses(this.look, ...parts);
	}

	private isReadOnly(): boolean {
		return !!this.options.readOnly || !!this.field?.readOnly || this.isDisabled();
	}

	private isDisabled(): boolean {
		return !!this.field?.disabled;
	}

	private isSelectable(): boolean {
		return (this.options.selectable ?? true) && !this.isDisabled();
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
		element.classList.add(...this.css('code-editor').split(' '));
		element.classList.toggle(CODE_EDITOR_CLASSES.readOnly, this.isReadOnly());
		element.classList.toggle(CODE_EDITOR_CLASSES.disabled, this.isDisabled());
		element.classList.toggle(CODE_EDITOR_CLASSES.unselectable, !this.isSelectable());
		const field = this.field;
		if (field) {
			field.classList.add(...this.css('code-editor-field').split(' '));
			field.hidden = true;
			field.tabIndex = -1;
			field.setAttribute('aria-hidden', 'true');
		}
	}

	/**
	 * @returns the extensions of CodeMirror: those of the options in compartments, which
	 *          configure() changes
	 */
	private extensions(): Extension[] {
		const compartments = this.compartments;
		const name = typeof this.options.language === 'string' ?
			resolveLanguage(this.options.language) : null;
		const language = name !== null ? loaded.get(name) ?? [] :
			typeof this.options.language === 'object' && this.options.language !== null ?
				this.options.language : [];
		return [
			keymap.of([{ key: 'Mod-s', preventDefault: true, run: () => {
				this.save();
				return true;
			} }]),
			highlightSpecialChars(),
			history(),
			drawSelection(),
			dropCursor(),
			EditorState.allowMultipleSelections.of(true),
			indentOnInput(),
			syntaxHighlighting(classHighlighter),
			bracketMatching(),
			closeBrackets(),
			rectangularSelection(),
			crosshairCursor(),
			highlightSelectionMatches(),
			keymap.of([...closeBracketsKeymap, ...defaultKeymap, ...searchKeymap, ...historyKeymap]),
			THEME,
			compartments.language.of(language),
			compartments.gutters.of(this.gutters()),
			compartments.editing.of(this.editing()),
			compartments.wrapping.of(this.options.lineWrapping ? EditorView.lineWrapping : []),
			compartments.indentation.of(this.indentation()),
			compartments.placeholder.of(this.options.placeholder ?
				placeholderText(this.options.placeholder) : []),
			compartments.label.of(EditorView.contentAttributes.of(this.labelAttributes())),
			compartments.phrases.of(EditorState.phrases.of(this.texts().phrases)),
			compartments.theme.of(this.theme()),
			compartments.extensions.of(this.options.extensions ?? []),
			EditorView.updateListener.of((update) => this.onUpdate(update))
		];
	}

	private texts(): CodeEditorTexts {
		const texts = this.options.texts;
		return {
			resize: texts?.resize ?? DEFAULT_CODE_EDITOR_TEXTS.resize,
			phrases: { ...DEFAULT_CODE_EDITOR_TEXTS.phrases, ...texts?.phrases }
		};
	}

	private gutters(): Extension {
		const extensions: Extension[] = [];
		if (this.options.lineNumbers ?? true) {
			extensions.push(lineNumbers(), highlightActiveLineGutter());
		}
		if (this.options.folding ?? true) {
			extensions.push(foldGutter(), keymap.of(foldKeymap));
		}
		return extensions;
	}

	private editing(): Extension {
		const selectable = this.isSelectable();
		return [
			EditorState.readOnly.of(this.isReadOnly()),
			EditorView.editable.of(selectable),
			selectable ? highlightActiveLine() : [],
			selectable && !this.isReadOnly() && (this.options.indentWithTab ?? true) ?
				keymap.of([indentWithTab]) : []
		];
	}

	private indentation(): Extension {
		const size = Math.min(Math.max(Math.round(this.options.tabSize ?? DEFAULTS.tabSize), 1), 16);
		return [EditorState.tabSize.of(size), indentUnit.of(' '.repeat(size))];
	}

	/**
	 * @returns the accessible name of the text: the label of the options, or the text of the
	 *          labels of the text area, its aria-label or its title
	 */
	private labelAttributes(): Record<string, string> {
		if (this.options.label) {
			return { 'aria-label': this.options.label };
		}
		const field = this.field;
		if (!field) {
			return {};
		}
		const texts = Array.from(field.labels ?? []).map((label) => (label.textContent ?? '')
			.replace(/\s+/g, ' ').trim()).filter(Boolean);
		const label = texts.join(' ') || field.getAttribute('aria-label') || field.title;
		return label ? { 'aria-label': label } : {};
	}

	/**
	 * @returns CodeMirror's look in the colors of the theme, dark in a dark theme, and the nonce
	 *          of its style element
	 */
	private theme(): Extension {
		const scheme = getComputedStyle(this.element).getPropertyValue(COLOR_SCHEME_PROPERTY).trim();
		const nonce = this.options.nonce ?? pageNonce();
		return [
			EditorView.darkTheme.of(scheme === 'dark'),
			nonce ? EditorView.cspNonce.of(nonce) : []
		];
	}

	/**
	 * Shows the grammar of the language, loading it first when it is not loaded yet.
	 */
	private applyLanguage(): void {
		const request = ++this.languageRequest;
		const language = this.options.language;
		if (typeof language !== 'string') {
			this.reconfigure(this.compartments.language, language ?? []);
			return;
		}
		const name = resolveLanguage(language);
		const done = name !== null ? loaded.get(name) : undefined;
		if (done || name === null) {
			this.reconfigure(this.compartments.language, done ?? []);
			return;
		}
		void loadLanguage(name).then((extension) => {
			if (!this.destroyed && request === this.languageRequest) {
				this.reconfigure(this.compartments.language, extension ?? []);
			}
		});
	}

	private reconfigure(compartment: Compartment, extension: Extension): void {
		if (compartment.get(this.view.state) !== extension) {
			this.view.dispatch({ effects: compartment.reconfigure(extension),
				annotations: external.of(true) });
		}
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
		listener: (event: E) => void, options?: AddEventListenerOptions): void {
		target.addEventListener(type, listener as EventListener, options);
		this.listeners.push([target, type, listener as EventListener]);
	}

	private bind(): void {
		const scroller = this.view.scrollDOM;
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

	private onUpdate(update: ViewUpdate): void {
		const user = update.transactions.some((transaction) => !transaction.annotation(external));
		if (update.docChanged) {
			const value = update.state.doc.toString();
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
		if (update.selectionSet && user) {
			const selection = this.selection();
			this.options.onSelectionChange?.(selection);
			this.dispatch('select', { selection });
			this.stateChanged();
		}
		if (update.focusChanged) {
			if (update.view.hasFocus) {
				this.focusValue = this.getValue();
				this.options.onFocus?.();
				this.dispatch('focus');
			} else {
				const value = this.getValue();
				if (this.focusValue !== null && this.focusValue !== value) {
					this.fieldEvent('change');
				}
				this.focusValue = null;
				this.options.onBlur?.(value);
				this.dispatch('blur');
				this.report(false);
			}
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
	private dispatch(name: keyof typeof CODE_EDITOR_EVENTS,
		detail: Omit<CodeEditorEventDetail, 'editor'> = {}): void {
		const target = this.field ?? this.element;
		this.dispatching = true;
		try {
			target.dispatchEvent(new CustomEvent<CodeEditorEventDetail>(CODE_EDITOR_EVENTS[name], {
				bubbles: true, detail: { editor: this, ...detail } }));
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
	 * @returns a selection of CodeMirror within the text: that of a state, the start of the text
	 *          for none
	 */
	private selectionOf(selection: CodeEditorSelection | undefined,
		length: number): EditorSelection | undefined {
		const ranges = Array.isArray(selection?.ranges) ? selection.ranges.filter((range) =>
			range && Number.isFinite(range.anchor) && Number.isFinite(range.head)) : [];
		if (!ranges.length) {
			return undefined;
		}
		const clamp = (position: number) => Math.min(Math.max(Math.round(position), 0), length);
		const main = Number.isInteger(selection!.main) && selection!.main >= 0 &&
			selection!.main < ranges.length ? selection!.main : 0;
		return EditorSelection.create(ranges.map((range) => EditorSelection.range(
			clamp(range.anchor), clamp(range.head))), main);
	}

	private selection(): CodeEditorSelection {
		const selection = this.view.state.selection;
		return {
			ranges: selection.ranges.map((range) => ({ anchor: range.anchor, head: range.head })),
			main: selection.mainIndex
		};
	}

	/**
	 * Scrolls the text where it was, once the editor is shown and laid out.
	 */
	private restoreScroll(): void {
		const { top, left } = this.scroll;
		if (top <= 0 && left <= 0) {
			return;
		}
		const scroller = this.view.scrollDOM;
		const apply = () => {
			if (scroller.clientHeight > 0) {
				scroller.scrollTop = top;
				scroller.scrollLeft = left;
			}
		};
		apply();
		// and again once CodeMirror has measured the lines it shows
		this.view.requestMeasure({ key: this.scroll, read: () => null, write: apply });
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
	 * @returns the text
	 */
	getValue(): string {
		return this.view.state.doc.toString();
	}

	/**
	 * Replaces the text, without calling back: the page decides. The selection stays, within the
	 * text.
	 *
	 * @param value the text
	 * @param saved whether it is the text last saved too
	 */
	setValue(value: string, saved = false): void {
		if (saved) {
			this.saved = value;
		}
		const state = this.view.state;
		if (value !== state.doc.toString()) {
			const selection = this.selectionOf(this.selection(), value.length);
			this.view.dispatch({ changes: { from: 0, to: state.doc.length, insert: value },
				selection, annotations: external.of(true) });
			this.textReported = false;
		} else {
			this.syncField(value);
		}
		this.renderModified(value, true);
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
		this.saved = value;
		this.renderSaved();
		this.renderModified(this.getValue(), true);
	}

	/**
	 * @returns the selection, the scroll position and the size
	 */
	getState(): CodeEditorState {
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
	setState(state: CodeEditorState): void {
		const selection = this.selectionOf(state.selection, this.view.state.doc.length);
		if (selection) {
			this.view.dispatch({ selection, annotations: external.of(true) });
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
		const value = this.getValue();
		this.options.onSave?.(value);
		this.dispatch('save');
	}

	/**
	 * Puts the focus in the text, where the cursor is.
	 */
	focus(): void {
		this.view.focus();
	}

	/**
	 * Takes other options; those left out stay as they were. The text, the text last saved and
	 * the state stay too: setValue(), markSaved() and setState() change them.
	 */
	configure(options: CodeEditorOptions = {}): void {
		const defined = Object.fromEntries(Object.entries(options)
			.filter(([, value]) => value !== undefined)) as CodeEditorOptions;
		const before = this.options;
		this.options = { ...before, ...defined };
		this.render();
		const compartments = this.compartments;
		const effects = [
			compartments.gutters.reconfigure(this.gutters()),
			compartments.editing.reconfigure(this.editing()),
			compartments.wrapping.reconfigure(this.options.lineWrapping ? EditorView.lineWrapping :
				[]),
			compartments.indentation.reconfigure(this.indentation()),
			compartments.placeholder.reconfigure(this.options.placeholder ?
				placeholderText(this.options.placeholder) : []),
			compartments.label.reconfigure(EditorView.contentAttributes.of(this.labelAttributes())),
			compartments.phrases.reconfigure(EditorState.phrases.of(this.texts().phrases)),
			compartments.theme.reconfigure(this.theme())
		];
		if (defined.extensions !== undefined) {
			effects.push(compartments.extensions.reconfigure(this.options.extensions ?? []));
		}
		this.view.dispatch({ effects, annotations: external.of(true) });
		if (defined.language !== undefined && defined.language !== before.language) {
			this.applyLanguage();
		}
		this.renderResizable();
	}

	/**
	 * Takes the editor away: CodeMirror's view, the handle resizing it and what it gave the
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
			field.classList.remove(...this.css('code-editor-field').split(' '));
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
			element.classList.remove(...this.css('code-editor').split(' '),
				...Object.values(CODE_EDITOR_CLASSES));
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
	 * Stops the editor, leaving its element and its text area: CodeMirror's view and the handle
	 * resizing it go.
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
		this.listeners.forEach(([target, type, listener]) => {
			target.removeEventListener(type, listener);
		});
		this.listeners = [];
		this.observer?.disconnect();
		this.observer = null;
		this.stopWatchingRemoval?.();
		this.stopWatchingRemoval = null;
		this.resizable?.destroy();
		this.resizable = null;
		this.view.destroy();
		if (instances.get(this.element) === this) {
			instances.delete(this.element);
		}
		if (this.field && instances.get(this.field) === this) {
			instances.delete(this.field);
		}
	}
}

/**
 * Makes an editor of an element, in the shape of a Svelte action: use:codeEditor={options}.
 *
 * @param element the element of the editor, or a text area
 * @param options the text, the language and what the user may do; the state and the callbacks
 * @returns update() taking other options, destroy() taking the editor away
 */
export function codeEditor(element: HTMLElement, options?: CodeEditorOptions): {
	update(options?: CodeEditorOptions): void;
	destroy(): void;
} {
	const instance = new CodeEditor(element, options);
	return {
		update: (next) => instance.configure(next),
		destroy: () => instance.destroy()
	};
}
