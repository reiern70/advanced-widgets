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
 * Since CKEditor 49 has no Watchdog, CKEditor keeps running after an error that escaped it, but
 * may be left in a state of its own. The editor hears of those of its CKEditor by CKEditor's
 * onEditorError, when the build has it, and tells them to onError, with the event error of
 * CK_EDITOR_EVENTS and in the log. Then it starts CKEditor again, as the Watchdog did, with the
 * text it last had, which the text area holds, and the selection, and says so in a line below the
 * text (recovery): after each of its first crashes, then as long as they come further apart than
 * an interval on average; crashes coming more often, it leaves CKEditor running, saying so in that
 * line.
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
 * Given onUpload, the editor uploads the images the user chooses, pastes or drops, as the Tiptap
 * editor does: CKEditor's ImageUpload, for the feature image, with an upload adapter of the
 * editor's own calling onUpload, which shows the progress on the image and stops the upload as
 * the user takes the image out. The editor checks each image first, its type, its size and its
 * pixels (uploadTypes, uploadMaxSize, uploadMaxWidth and uploadMaxHeight), and tells why it does
 * not upload one, or why it failed, in a line above the text, as the Tiptap editor does, not in
 * CKEditor's alert. The bar of the progress has no style attribute either: it grows by classes.
 *
 * The rest of the uploads of the Tiptap editor are CKEditor's too, under the same options (see
 * ck-editor-uploads.ts): the images CKEditor is given are checked before it uploads them, how many
 * (uploadMaxCount) and their pixels, those too large scaled down when the user ticks them
 * (uploadScaling), and a file of another type chosen in CKEditor's dialog of files is told of; the
 * dialog of the uploads (uploadDialog) takes the place of CKEditor's own button; the images kept
 * permanently (onStorePermanently) are ticked in its lines and inserted by the tool storedImage
 * (onFindImages); the tool images lists the images of the text; the files attached (onAttach) are
 * an inline widget of the editor's own, written as the Tiptap editor writes them; and the images
 * and the files the user takes out are told of a moment after (onImagesRemoved,
 * onAttachmentsRemoved), as the Tiptap editor tells them. The lists, the dialogs and the files
 * take the look of rich-text-editor.css, which the page loads too.
 *
 * As the Tiptap editor, it keeps its state, what is selected, how far the text is scrolled and
 * the size the user gave it, by the bar along its bottom; it tells whether the text has changes
 * not saved, as data-modified on its element, which tabs and windows of the desktop show; Ctrl+S
 * tells the page to save; and it fires the events of RICH_TEXT_EDITOR_EVENTS, whose handlers
 * (events) send the state to onStateChange as the Tiptap editor's do and which the events of
 * Wicket's editor listen to, and those of the types of events registered, shared with the
 * Tiptap editor (registerRichTextEvent, see rich-text-events.ts), from its own or by trigger().
 * The positions of the selection count the places between the nodes too, as ProseMirror's do.
 *
 * A paste loses what CKEditor's plugins do not read, scripts and handlers of events, links and
 * images to addresses it does not take: the editor compares the HTML pasted with what CKEditor
 * inserts, and tells what was lost as the Tiptap editor does, to onPasteCleaned, with the event
 * paste-cleaned and, while the page wants it (pasteNotice), in a polite line under the toolbar
 * until the text changes again.
 */

import { getHost, onRemoving, type Unsubscribe } from './host.js';
import { lookOf, partClasses, type Look, type LookPart } from './look.js';
import { Resizable, type ResizableSize, type ResizeAxis } from './resizable.js';
import { EditorEventHandlers, type EditorEvents } from './editor-events.js';
import { RICH_TEXT_EDITOR_EVENTS, sameState, textLanguage, type RichTextEditorEventKey,
	type RichTextEditorSelection, type RichTextEditorState,
	type RichTextEditorStateChange } from './rich-text-contract.js';
import { RICH_TEXT_BUILT_IN_EVENTS, RichTextEventTypes } from './rich-text-events.js';
import { DEFAULT_RICH_TEXT_PASTE_TEXTS, isEmptyCleaningReport, isSafeUrl, pasteCleaningReport,
	pasteNoticeText, quietHtml, RICH_TEXT_ALIGN_CLASSES, RichTextPasteNotice,
	type RichTextCleaningReport, type RichTextPasteTexts } from './rich-text-html.js';
import { dimensionsText, fits, readImageDimensions,
	type RichTextImageDimensions } from './rich-text-image-headers.js';
import { checkFiles, DEFAULT_RICH_TEXT_UPLOAD_TEXTS, DEFAULT_RICH_TEXT_UPLOAD_TYPES, fileNameOf,
	isAbort, refusalOf, sizeText, type RichTextUploader, type RichTextUploadOptions,
	type RichTextUploadTexts, type UploadDialogHost, type UploadLimits } from './rich-text-upload.js';
import { askToScale, openUploadDialog } from './rich-text-upload-dialog.js';
import { attachmentKind, attachmentRefusal, attachmentTypes, attachmentUrlTest, checkAttachments,
	type AttachmentLimits, type RichTextAttachmentType,
	type RichTextAttachmentUrls } from './rich-text-attachments.js';
import { imagePicker, type RichTextImageFinder,
	type RichTextImageKeeper } from './rich-text-stored-images.js';
import { ckAttachmentConversion, ckAttachmentsIn, ckAttachmentsOf, ckFillList,
	ckImagesIn, ckImagesOf, ckInsertAttachments, ckRegisterTools, ckRemoveAttachments,
	ckRemoveImages, ckSelect, type CKButtonTool, type CKPanelTool, type CKUploadsBuild,
	type CKUploadsEditor } from './ck-editor-uploads.js';
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

/**
 * A command of CKEditor, bold or superscript for example: whether it is on where the selection is,
 * and its execute(), which fires the event execute first.
 */
export interface CKEditorCommand extends CKEditorEmitter {
	readonly value: unknown;
	execute(...args: unknown[]): unknown;
}

/** An editor of CKEditor, as ClassicEditor.create() makes it. */
export interface CKEditorInstance {
	readonly model: CKEditorModel;
	/** CKEditor's commands, by their names. */
	readonly commands?: { get(name: string): CKEditorCommand | undefined };
	readonly data: {
		parse(data: string): unknown;
		stringify(fragment: unknown): string;
	};
	readonly editing: { readonly view: CKEditorEditingView };
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
	/** CKEditor's plugins, by their names: the clipboard's pipeline tells what a paste inserts. */
	readonly plugins?: {
		has(name: string): boolean;
		get(name: string): unknown;
	};
	getData(): string;
	setData(data: string): void;
	enableReadOnlyMode(lockId: string): void;
	disableReadOnlyMode(lockId: string): void;
	destroy(): Promise<unknown>;
}

/** The view of CKEditor's editing: the text as the user sees it. */
export interface CKEditorEditingView {
	focus(): void;
	readonly document: CKEditorEmitter & {
		/** The element of the text, as the view has it. */
		getRoot(): unknown;
	};
	change(callback: (writer: CKEditorViewWriter) => void): void;
}

/** What changes the view of CKEditor's editing. */
export interface CKEditorViewWriter {
	setAttribute(key: string, value: string, element: unknown): void;
	removeAttribute(key: string, element: unknown): void;
}

/** What CKEditor's onEditorError tells of an error that escaped an editor. */
export interface CKEditorErrorData {
	/** The error, a CKEditorError. */
	readonly error: unknown;
	/** The editor, or the context, the error came from; null when CKEditor cannot tell. */
	readonly source: unknown;
}

/**
 * CKEditor's onEditorError (CKEditor 49 and later): registers a callback for the errors escaping
 * the editors of the page, and returns a function unregistering it.
 */
export type CKEditorOnEditorError = (callback: (data: CKEditorErrorData) => void) => () => void;

/** CKEditor's ClassicEditor: an editor with a toolbar above the text. */
export interface CKEditorClass {
	create(element: HTMLElement, config: Record<string, unknown>): Promise<CKEditorInstance>;
	/** CKEditor's onEditorError, reachable from the class, in CKEditor 49 and later. */
	readonly onEditorError?: CKEditorOnEditorError;
}

/**
 * The classes of CKEditor: the global CKEDITOR of its browser build, or an object of the page's
 * own with ClassicEditor and the plugins, by their names (Bold, Table...).
 */
export interface CKEditorBuild {
	readonly ClassicEditor: CKEditorClass;
	/** CKEditor's onEditorError, as the browser build of CKEditor 49 and later has it. */
	readonly onEditorError?: CKEditorOnEditorError;
	readonly [name: string]: unknown;
}

/** A plugin of CKEditor: its class, or its name in the build. */
export type CKEditorPlugin = string | object;

/** The features CKEditor makes: those of the Tiptap editor it has plugins for. */
export type CKEditorFeature = Extract<RichTextFeature, 'heading' | 'bold' | 'italic' |
	'underline' | 'strike' | 'code' | 'superscript' | 'subscript' | 'link' | 'bulletList' |
	'orderedList' | 'taskList' | 'blockquote' | 'codeBlock' | 'horizontalRule' | 'table' | 'image' |
	'attachment' | 'textAlign'>;

/** The plugins of CKEditor making each feature, by their names in the build. */
export const CK_EDITOR_FEATURE_PLUGINS: Readonly<Record<CKEditorFeature, readonly string[]>> =
	Object.freeze({
		heading: ['Heading'],
		bold: ['Bold'],
		italic: ['Italic'],
		underline: ['Underline'],
		strike: ['Strikethrough'],
		code: ['Code'],
		superscript: ['Superscript'],
		subscript: ['Subscript'],
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
		// an inline widget of the editor's own, the files attached (ck-editor-uploads.ts)
		attachment: ['Widget'],
		textAlign: ['Alignment']
	});

/** The features CKEditor makes, all of them: those of an editor of HTML when left out. */
export const CK_EDITOR_FEATURES = Object.freeze(Object.keys(CK_EDITOR_FEATURE_PLUGINS)) as
	readonly CKEditorFeature[];

/** The features Markdown has no syntax for, which an editor of Markdown leaves out. */
const NOT_IN_MARKDOWN: readonly CKEditorFeature[] = ['underline', 'superscript', 'subscript',
	'textAlign'];

/**
 * Makes a command of CKEditor setting a mark of text take another one away first, in the same step
 * of undo: superscript takes subscript away, as in the Tiptap editor, where CKEditor lets text be
 * both. Nothing without either command.
 */
function takesAway(editor: CKEditorInstance, name: string, other: string): void {
	const command = editor.commands?.get(name);
	const opposite = editor.commands?.get(other);
	if (!command || !opposite) {
		return;
	}
	let inside = false;
	// before the command, which runs at the normal priority, and after its check of being enabled
	command.on('execute', (event: { stop(): void }, args: unknown[] = []) => {
		const forced = (args[0] as { forceValue?: unknown } | undefined)?.forceValue;
		if (inside || !(forced === undefined ? !command.value : forced)) {
			return;
		}
		event.stop();
		inside = true;
		try {
			editor.model.change(() => {
				opposite.execute({ forceValue: false });
				command.execute(...args);
			});
		} finally {
			inside = false;
		}
	}, { priority: 'high' });
}

/** The plugins of every editor: typing, the clipboard, undo, paragraphs, Markdown's marks. */
export const CK_EDITOR_BASE_PLUGINS: readonly string[] = Object.freeze(['Essentials', 'Paragraph',
	'Autoformat', 'PasteFromOffice']);

/**
 * When a tool of the uploads shows in the default toolbar: while the editor uploads images, while
 * it opens the dialog of the uploads, while it finds the images the page stores, while it attaches
 * files.
 */
type ToolCondition = 'upload' | 'dialog' | 'find' | 'attach';

/**
 * The tools of the default toolbar, in groups, as CKEditor names them, by their features: those of
 * the uploads besides CKEditor's own, the editor's (ck-editor-uploads.ts), on their condition.
 */
const TOOLBAR: readonly (readonly (readonly [CKEditorFeature | null, string, ToolCondition?])[])[] =
	[
		[['heading', 'heading']],
		[['bold', 'bold'], ['italic', 'italic'], ['underline', 'underline'],
			['strike', 'strikethrough'], ['code', 'code'], ['superscript', 'superscript'],
			['subscript', 'subscript']],
		[['link', 'link']],
		[['bulletList', 'bulletedList'], ['orderedList', 'numberedList'], ['taskList', 'todoList']],
		[['blockquote', 'blockQuote'], ['codeBlock', 'codeBlock'],
			['horizontalRule', 'horizontalLine'], ['table', 'insertTable'], ['image', 'insertImage'],
			['image', 'uploadImages', 'dialog'], ['image', 'storedImage', 'find'],
			['image', 'images', 'upload'], ['attachment', 'attachment', 'attach'],
			['attachment', 'attachments', 'attach']],
		[['textAlign', 'alignment']],
		[[null, 'undo'], [null, 'redo']]
	];

/**
 * What the editor says, its own texts, with the notice telling what a paste lost
 * (RichTextPasteTexts) and what its uploads say, as the Tiptap editor's (RichTextUploadTexts):
 * CKEditor's are translated by CKEditor.
 */
export interface CKEditorTexts extends RichTextPasteTexts, RichTextUploadTexts {
	characters: string;
	character: string;
	words: string;
	word: string;
	resize: string;
	/** The switch of the form of a link opening it in a new tab. */
	linkNewTab: string;
	/** The line below the text once CKEditor ran into an error and started again (recovery). */
	errorRestarted: string;
	/** The line below the text once CKEditor ran into errors too often to start again. */
	errorGaveUp: string;
}

export const DEFAULT_CK_EDITOR_TEXTS: Readonly<CKEditorTexts> = Object.freeze({
	characters: '{0} characters',
	character: '{0} character',
	words: '{0} words',
	word: '{0} word',
	resize: 'Drag, or use the arrow keys, to resize the editor',
	linkNewTab: 'Open in a new tab',
	errorRestarted: 'Something went wrong in the editor. It started again, with your text.',
	errorGaveUp: 'Something keeps going wrong in the editor. Save your text, then reload the ' +
		'page.',
	...DEFAULT_RICH_TEXT_UPLOAD_TEXTS,
	...DEFAULT_RICH_TEXT_PASTE_TEXTS
});

/**
 * How the editor starts CKEditor again after an error escaped it, as CKEditor's Watchdog did: after
 * each of the first crashes, then as long as the crashes come further apart than an interval on
 * average.
 */
export interface CKEditorRecovery {
	/**
	 * How many crashes in a row CKEditor starts again after, however close they come: 3 when left
	 * out, the Watchdog's crashNumberLimit.
	 */
	crashes?: number;
	/**
	 * After more crashes, CKEditor starts again only while the last of them came further apart than
	 * this on average, in milliseconds: 5000 when left out, the Watchdog's
	 * minimumNonErrorTimePeriod.
	 */
	interval?: number;
}

/** What happened as CKEditor failed, which onError and the event error tell. */
export interface CKEditorFailure {
	/** Whether CKEditor had started: false when it could not start, the text area shown again. */
	started: boolean;
	/** Whether the editor starts CKEditor again, with the text it had. */
	restarting: boolean;
	/** Whether the editor gave up starting CKEditor again, the errors coming too often. */
	gaveUp: boolean;
	/** How many times CKEditor failed after it started, this time among them. */
	crashes: number;
}

/** What the event error of the editor carries, besides the editor. */
export interface CKEditorErrorDetail extends CKEditorFailure {
	/** The error. */
	error: unknown;
	/** The name of the error, CKEditorError for one of CKEditor. */
	name: string;
	/** What the error says. */
	message: string;
}

/** The events the editor fires besides those it shares with the Tiptap editor. */
export const CK_EDITOR_EVENTS = Object.freeze({
	/**
	 * CKEditor ran into an error after it started, or could not start: its detail is a
	 * CKEditorErrorDetail.
	 */
	error: 'antilia-ck-editor:error'
});

/** How the editor starts CKEditor again, unless the options say otherwise. */
export const DEFAULT_CK_EDITOR_RECOVERY: Readonly<Required<CKEditorRecovery>> = Object.freeze({
	crashes: 3,
	interval: 5000
});

/** The longest message of an error the event error carries, in characters. */
const MESSAGE_LENGTH = 1000;

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
	 * What the text may hold: all the features when left out, but the files attached without
	 * onAttach, and for Markdown underlined text, superscript, subscript and alignment, which
	 * Markdown has no syntax for. Those CKEditor does not make are left out. Superscript and subscript take each other away, as in the Tiptap
	 * editor.
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
	/**
	 * Whether the browser checks the spelling of the text, underlining the words its dictionaries
	 * do not know: as the browser does by default when left out or null.
	 */
	spellcheck?: boolean | null;
	/**
	 * The language of the text, a tag of BCP 47 (en-GB, de), which the text gets as its lang: the
	 * browser checks its spelling in it; that of the page when left out, null or no such tag.
	 * CKEditor's texts stay in their language (language).
	 */
	textLanguage?: string | null;
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
	/**
	 * The handlers of the editor's events, by their keys (RICH_TEXT_EDITOR_EVENTS) or the names of
	 * other events it fires: each runs on its event, at once or a moment after the last of a row of
	 * them, and may send the state to onStateChange (see editor-events.ts). Those left out keep
	 * theirs: change, select, scroll and resize send the state 400 ms after the user paused, blur
	 * and leave at once; an empty list for none.
	 */
	events?: EditorEvents<CKEditor, RichTextEditorEventKey>;
	/**
	 * Whether a line under the toolbar tells the user what a paste lost, until the text changes
	 * again: "Part of the paste was left out: scripts, images."; false when left out.
	 */
	pasteNotice?: boolean;
	/** What the editor says of its own, in English when left out. */
	texts?: Partial<CKEditorTexts>;
	/** The look of the editor; that of the element it is in when left out (see lookOf). */
	look?: Look;
	/**
	 * Uploads an image the user chose, pasted or dropped, resolving to its address, or rejecting
	 * with the message telling why not: CKEditor's ImageUpload then, for the feature image, the
	 * progress shown on the image as told (onProgress) and the upload canceled (signal) as the user
	 * takes the image out. uploadImageTo(url) posts it to a URL, as Wicket's
	 * RichTextImageUploadBehavior takes it. None when left out or null: an image is an address
	 * the user types. One coming or going makes CKEditor anew.
	 */
	onUpload?: RichTextUploader | null;
	/** The media types of the images uploaded: those of DEFAULT_RICH_TEXT_UPLOAD_TYPES when left out. */
	uploadTypes?: readonly string[];
	/** The largest image uploaded, in bytes: none when left out or null. */
	uploadMaxSize?: number | null;
	/** The widest picture uploaded, in pixels, read from its file: none when left out or null. */
	uploadMaxWidth?: number | null;
	/** The highest picture uploaded, in pixels, read from its file: none when left out or null. */
	uploadMaxHeight?: number | null;
	/**
	 * Whether a dialog offers the user to scale down the pictures larger than that, each by a box
	 * to tick, rather than refusing them; false when left out.
	 */
	uploadScaling?: boolean;
	/**
	 * How many images one drop, choice or paste takes at most, the first, the others left out with
	 * a message; none when left out or null.
	 */
	uploadMaxCount?: number | null;
	/**
	 * Whether the images are uploaded in the dialog of the uploads, rather than by CKEditor's own
	 * button: the tool uploadImages opens it, a zone to drop images on or choose them and the list
	 * of the files, each with the share of it sent, which the user takes out before the images go
	 * into the text; false when left out. Pasted and dropped into the text, they upload as before.
	 */
	uploadDialog?: boolean;
	/**
	 * Called a moment after the user took images out of the text, with their addresses: by a key,
	 * a cut, the tool images or the page's own code, or uploaded and never put into it; at once as
	 * the user saves or leaves the editor. The page deletes them, at once or once the text is
	 * saved: deleteImagesAt() asks Wicket's RichTextImageUploadBehavior to.
	 */
	onImagesRemoved?: (urls: string[]) => void;
	/**
	 * Stores images the user uploaded permanently, those ticked in the dialog of the uploads, once
	 * they are in the text: see storePermanentlyAt(). Resolves to the addresses of those stored.
	 * With it, the tool uploadImages opens the dialog, whose lines have the box to tick. None when
	 * left out or null.
	 */
	onStorePermanently?: RichTextImageKeeper | null;
	/**
	 * Finds the images the page stores permanently for what the user typed, which the tool
	 * storedImage, "Insert image from store", lists and inserts: see findImagesAt(). None when
	 * left out or null: the tool does not show.
	 */
	onFindImages?: RichTextImageFinder | null;
	/**
	 * Uploads a file the user attaches, a PDF or a spreadsheet, chosen by the tool attachment,
	 * dropped into the text or pasted, answering the address it is served at, which must be safe:
	 * see uploadFileTo(). The file goes into the text as a link with its name and its size, as the
	 * Tiptap editor writes it, for the feature attachment. None when left out or null: the tools of
	 * the files do not show, and a file that is no image dropped into the text is refused.
	 */
	onAttach?: RichTextUploader | null;
	/**
	 * The types of the files the user may attach, each a media type, an extension and a label:
	 * DEFAULT_RICH_TEXT_ATTACH_TYPES, those of the server, when left out.
	 */
	attachTypes?: readonly RichTextAttachmentType[];
	/** The largest file the user may attach, in bytes; none when left out or null. */
	attachMaxSize?: number | null;
	/** How many files one drop, choice or paste takes at most, the first; none when left out. */
	attachMaxCount?: number | null;
	/**
	 * Whether the tool attachment opens the dialog of the uploads, rather than choosing the files
	 * at once; false when left out.
	 */
	attachDialog?: boolean;
	/**
	 * The addresses of the files attached to the text, or a test of an address, for Markdown, where
	 * a file is a plain link: a link to one of them, a text of its own, comes back as a file
	 * attached. None when left out or null: every link comes back as a link.
	 */
	attachmentUrls?: RichTextAttachmentUrls | null;
	/**
	 * Called a moment after the user took files attached out of the text, with their addresses, as
	 * onImagesRemoved is for the images: the page deletes them, at once or once the text is saved.
	 */
	onAttachmentsRemoved?: (urls: string[]) => void;
	/** Called with CKEditor's editor once it started. */
	onReady?: (editor: CKEditorInstance) => void;
	/**
	 * How the editor starts CKEditor again after an error escaped it, as CKEditor's Watchdog did:
	 * DEFAULT_CK_EDITOR_RECOVERY when left out; false to leave CKEditor running as it is, as
	 * CKEditor 49 does, telling of the errors alone.
	 */
	recovery?: CKEditorRecovery | false;
	/**
	 * Called with what went wrong when CKEditor could not start, or ran into an error after it
	 * started, which CKEditor tells by its onEditorError, and what the editor does about it.
	 */
	onError?: (error: unknown, failure: CKEditorFailure) => void;
	/** Called with the text each time the user changed it. */
	onChange?: (value: string) => void;
	/** Called when the editor got the focus. */
	onFocus?: () => void;
	/** Called with the text when the editor lost the focus. */
	onBlur?: (value: string) => void;
	/** Called with the selection each time the user moved the cursor or selected text. */
	onSelectionChange?: (selection: RichTextEditorSelection) => void;
	/**
	 * Called with the state when a handler of the events sends it (events), and it, or the text,
	 * changed since it was last sent: by default a moment after the user changed it, as the editor
	 * loses the focus and as the page goes.
	 */
	onStateChange?: (state: RichTextEditorState, change: RichTextEditorStateChange) => void;
	/** Called when the text came to have changes not saved, or no longer has. */
	onModifiedChange?: (modified: boolean) => void;
	/** Called with the text when the user pressed Ctrl+S, or Cmd+S. */
	onSave?: (value: string) => void;
	/** Called with the size once the user has resized the editor. */
	onResize?: (size: ResizableSize) => void;
	/**
	 * Called with what a paste lost, once it is in the text, before the event paste-cleaned: a
	 * paste losing nothing calls nothing.
	 */
	onPasteCleaned?: (report: RichTextCleaningReport) => void;
}

/** What the events of the editor carry, besides what each tells. */
export interface CKEditorEventDetail {
	editor: CKEditor;
	selection?: RichTextEditorSelection;
	state?: RichTextEditorState;
	modified?: boolean;
	size?: ResizableSize;
	/** How far the text is scrolled, for scroll. */
	scroll?: { top: number; left: number };
	/** True as the page goes, for leave. */
	leaving?: boolean;
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
	focused: 'antilia-ck-editor-focused',
	/** The line below the text telling the user CKEditor ran into an error. */
	error: 'antilia-ck-editor-error',
	/** The line above the text telling the user why an image was not uploaded, for a while. */
	message: 'antilia-ck-editor-message',
	/** The field of the tool attachment choosing the files, hidden. */
	attachPicker: 'antilia-ck-editor-attach-picker'
});

/** How long the line telling why an image was not uploaded stays, in milliseconds. */
const MESSAGE_TIME = 8000;

/** The steps the bar of the progress of an upload grows by, in percent, each a class of its own. */
const PROGRESS_STEP = 5;

/** The class of the bar of the progress of an upload at a share, in steps of PROGRESS_STEP. */
export function uploadProgressClass(percent: number): string {
	const share = Math.min(Math.max(Number.isFinite(percent) ? percent : 0, 0), 100);
	return 'antilia-ck-upload-' + Math.round(share / PROGRESS_STEP) * PROGRESS_STEP;
}

/** What CKEditor's FileRepository gives an upload adapter: the file, and the progress to set. */
export interface CKEditorFileLoader extends CKEditorEmitter {
	/** The file to upload, null when CKEditor could not read it. */
	readonly file: Promise<File | null>;
	/** The bytes sent, which CKEditor shows the progress by. */
	uploaded: number;
	/** The bytes of the whole request, null until known. */
	uploadTotal: number | null;
}

/** An upload adapter of CKEditor: it uploads a file, and stops as CKEditor aborts it. */
export interface CKEditorUploadAdapter {
	upload(): Promise<{ default: string }>;
	abort(): void;
}

const DEFAULTS = { resizable: 'vertical' as ResizeAxis, minWidth: 200,
	minHeight: 96, headingLevels: [1, 2, 3, 4] as readonly HeadingLevel[] };

/** The lock of CKEditor's read only mode the editor holds. */
const READ_ONLY_LOCK = 'antilia-ck-editor';

/** The options that make CKEditor's configuration: changing one makes it anew. */
const CONFIG_OPTIONS: readonly (keyof CKEditorOptions)[] = ['format', 'features',
	'headingLevels', 'toolbar', 'licenseKey', 'language', 'textLanguage', 'placeholder', 'label',
	'uploadTypes'];

/**
 * The options of the uploads whose coming or going makes CKEditor anew, the callbacks and the
 * dialog: its plugins and its tools change.
 */
const UPLOAD_SWITCHES: readonly (keyof CKEditorOptions)[] = ['onUpload', 'onStorePermanently',
	'onFindImages', 'onAttach', 'uploadDialog'];

/** How long after the user took images out of the text the editor tells of them, in ms. */
const REMOVED_DELAY = 300;

/** The options compared by what they are, not by their value. */
const OBJECT_OPTIONS: readonly (keyof CKEditorOptions)[] = ['build', 'plugins', 'config',
	'translations'];

/** CKEditor's ids of the items of lists, new each time it reads them: no change of the text. */
const LIST_ITEM_ID = / data-list-item-id="[^"]*"/g;

/** The images of CKEditor's model, and their sizes, for which CKEditor writes a style. */
const IMAGES = ['imageBlock', 'imageInline'];
const SIZES = ['width', 'height'];

/**
 * The icons of the tools of the uploads, when the build has no icon of CKEditor for them: each
 * line its own fill of none, for CKEditor fills those without one.
 */
const ICONS = Object.freeze({
	// two pictures, one behind the other
	images: icon('<rect x="7" y="3" width="14" height="14" rx="2" fill="none"/>' +
		'<path d="M3 7v12a2 2 0 0 0 2 2h12" fill="none"/><circle cx="11.5" cy="7.5" r="1.4" ' +
		'fill="none"/><path d="m21 13-4-4-6 6" fill="none"/>'),
	// a picture going up
	uploadImages: icon('<rect x="3" y="4" width="18" height="16" rx="2" fill="none"/>' +
		'<path d="M12 16V9M9 12l3-3 3 3" fill="none"/>'),
	// a picture coming out of a box
	storedImage: icon('<path d="M3 14v5a1 1 0 0 0 1 1h16a1 1 0 0 0 1-1v-5" fill="none"/>' +
		'<path d="M3 14h5l1 2h6l1-2h5" fill="none"/><rect x="7" y="3" width="10" height="8" ' +
		'rx="1.5" fill="none"/>'),
	// a paper clip
	attachment: icon('<path d="m20 11.5-8.2 8.2a5 5 0 0 1-7.1-7.1l8.5-8.5a3.3 3.3 0 0 1 4.7 4.7' +
		'l-8.5 8.5a1.7 1.7 0 0 1-2.4-2.4l7.8-7.8" fill="none"/>'),
	// a sheet with a folded corner, its lines
	attachments: icon('<path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z" ' +
		'fill="none"/><path d="M14 3v5h5M9 13h6M9 17h6" fill="none"/>')
});

/**
 * @returns an icon of CKEditor's size, its lines drawn in the color of the text
 */
function icon(content: string): string {
	return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" stroke="currentColor" ' +
		'stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">' + content + '</svg>';
}

/**
 * @returns whether a number of the options is a limit: finite, above 0
 */
function positive(value: number | null | undefined): value is number {
	return typeof value === 'number' && Number.isFinite(value) && value > 0;
}

/**
 * @returns what the list of the images calls an image: its text, else the name of its file, else
 *          what stands for one without
 */
function imageLabel(src: string, alt: string | null, texts: Pick<CKEditorTexts, 'theImage'>):
	string {
	if (alt && alt.trim()) {
		return alt.trim();
	}
	const file = src.split(/[?#]/)[0]!.split('/').pop() ?? '';
	try {
		return decodeURIComponent(file) || texts.theImage;
	} catch {
		return file || texts.theImage;
	}
}

function resolve(target: HTMLElement | string): HTMLElement | null {
	return typeof target === 'string' ? document.getElementById(target) : target;
}

/**
 * @returns the name and the message of an error, as the event error carries them: what it says
 *          cut to MESSAGE_LENGTH characters
 */
function describeError(error: unknown): { name: string; message: string } {
	const named = error as { name?: unknown; message?: unknown } | null | undefined;
	const name = typeof named?.name === 'string' && named.name ? named.name : 'Error';
	const message = typeof named?.message === 'string' ? named.message :
		error === undefined || error === null ? '' : String(error);
	return { name: name.slice(0, 100), message: message.slice(0, MESSAGE_LENGTH) };
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

	/** The line telling what a paste lost, while the options want it. */
	private notice: RichTextPasteNotice | null = null;

	/** The HTML of a paste, made quiet, until CKEditor inserts what it read of it. */
	private pasted: string | null = null;

	/** The line above the text telling why an image was not uploaded, while it tells. */
	private message: HTMLElement | null = null;

	private messageTimer: ReturnType<typeof setTimeout> | null = null;

	/** The images of the text, by their addresses, each with how many times it is in it. */
	private images = new Map<string, number>();

	/** The files attached to the text, by their addresses, as the images. */
	private attachments = new Map<string, number>();

	/** The images the user took out of the text, to tell of in a moment. */
	private removed = new Set<string>();

	/** The files the user took out of the text, to tell of in a moment. */
	private removedAttachments = new Set<string>();

	private removedTimer: ReturnType<typeof setTimeout> | null = null;

	/** Whether the editor gives CKEditor's command uploading images the files it checked. */
	private passing = false;

	/** Why files chosen in CKEditor's dialog of files are of no type taken, to tell. */
	private typeRefusals: readonly string[] = [];

	/** The field choosing the files to attach, once the tool attachment made it. */
	private attachPicker: HTMLInputElement | null = null;

	/** The line below the text telling the user CKEditor ran into an error, while it tells. */
	private errorLine: HTMLElement | null = null;

	/** What the line telling of an error says: that CKEditor started again, or gave up; none. */
	private errorShown: 'restarted' | 'gaveUp' | null = null;

	/** How many times CKEditor failed after it started. */
	private crashCount = 0;

	/** When CKEditor failed last, as many times as the recovery looks back at. */
	private crashTimes: number[] = [];

	/** Whether the editor gave up starting CKEditor again, the errors coming too often. */
	private gaveUp = false;

	/** The CKEditor whose errors the editor hears of, and what stops hearing of them. */
	private hearing: { editor: CKEditorInstance; stop: () => void } | null = null;

	private resizable: Resizable | null = null;

	/** The size the editor has, as the user gave it. */
	private size: ResizableSize = { width: null, height: null };

	/** How far the text is scrolled, while the editor is shown. */
	private scroll = { top: 0, left: 0 };

	/** The selection CKEditor takes once it started, or again. */
	private pendingSelection: RichTextEditorSelection | null = null;

	/** Whether the editor is shown, not in a hidden tab for example. */
	private shown = false;

	/** The state last sent, or the one the page gave, null before. */
	private reported: RichTextEditorState | null = null;

	/** The state the editor started in: nothing to send while it has it, before any was sent. */
	private started: RichTextEditorState | null = null;

	/** Whether the text was sent, with the state, since the user last changed it. */
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

	/** Runs the handlers of the events. */
	private handlers: EditorEventHandlers<CKEditor> | null = null;

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
		this.renderEventHandlers();
		this.started = this.getState();
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
	 * @returns what the text may hold, among the features CKEditor makes: those given, or all of
	 *          the format, the files attached with onAttach alone
	 */
	features(): ReadonlySet<CKEditorFeature> {
		const given = this.options.features;
		if (given) {
			return new Set(CK_EDITOR_FEATURES.filter((feature) => given.includes(feature)));
		}
		return new Set(CK_EDITOR_FEATURES.filter((feature) =>
			(feature !== 'attachment' || !!this.options.onAttach) &&
			(this.format() !== 'markdown' || !NOT_IN_MARKDOWN.includes(feature))));
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
		if (this.uploading()) {
			// pasting and dropping images, and their progress; the upload button of CKEditor, unless
			// the dialog of the uploads takes its place
			names.push(...(this.options.uploadDialog ? ['ImageUploadEditing', 'ImageUploadProgress'] :
				['ImageUpload']));
		}
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
		const features = this.features();
		if (features.has('image') || features.has('attachment')) {
			// the files attached and the tools of the uploads, the editor's own
			plugins.push(this.ownPlugin(build));
		}
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
			const tools = group.filter(([feature, , condition]) => (feature === null ||
				features.has(feature)) && (!condition || this.hasTools(condition)))
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
		if (this.uploading()) {
			// CKEditor's names of the types of images: png for image/png
			(config.image as Record<string, unknown>).upload = { types: this.uploadTypes()
				.filter((type) => type.startsWith('image/')).map((type) => type.slice(6)) };
		}
		if (!features.has('heading')) {
			delete config.heading;
		}
		if (options.licenseKey !== undefined) {
			config.licenseKey = options.licenseKey;
		}
		const content = textLanguage(options.textLanguage);
		if (content) {
			// CKEditor gives the text the language of its content as its lang
			config.language = { ui: options.language || 'en', content };
		} else if (options.language) {
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
		this.hearErrors(editor, build);
		this.setUp(editor);
		// CKEditor has the selection now
		if (this.reported === null && this.textReported) {
			this.started = this.getState();
		}
		this.options.onReady?.(editor);
		return this;
	}

	/**
	 * CKEditor could not start: the text area shows again, to type the text in.
	 */
	private fail(error: unknown): never {
		getHost().log.error('CKEditor could not start', error);
		this.tellError(error, { started: false, restarting: false, gaveUp: this.gaveUp,
			crashes: this.crashCount });
		this.destroy();
		throw error;
	}

	/**
	 * Hears of the errors escaping CKEditor, by CKEditor's onEditorError when the build has it
	 * (CKEditor 49 and later): those of this CKEditor alone, until it stops.
	 */
	private hearErrors(editor: CKEditorInstance, build: CKEditorBuild): void {
		this.stopHearingErrors();
		const register = typeof build.onEditorError === 'function' ? build.onEditorError :
			build.ClassicEditor.onEditorError;
		if (typeof register !== 'function') {
			return;
		}
		try {
			const stop = register(({ error, source }) => {
				if (source === editor && editor === this.editor && !this.destroyed) {
					this.crashed(editor, error);
				}
			});
			this.hearing = { editor, stop: typeof stop === 'function' ? stop : () => undefined };
		} catch (error) {
			getHost().log.error('The editor cannot hear of the errors of CKEditor', error);
		}
	}

	/**
	 * No longer hears of the errors of a CKEditor, or of the one heard of.
	 */
	private stopHearingErrors(editor?: CKEditorInstance): void {
		const hearing = this.hearing;
		if (!hearing || (editor && hearing.editor !== editor)) {
			return;
		}
		this.hearing = null;
		try {
			hearing.stop();
		} catch (error) {
			getHost().log.error('The editor could not stop hearing of the errors of CKEditor', error);
		}
	}

	/**
	 * @returns how the editor starts CKEditor again after an error, null for not at all: the
	 *          option recovery, DEFAULT_CK_EDITOR_RECOVERY for what it leaves out
	 */
	private recovery(): Required<CKEditorRecovery> | null {
		const given = this.options.recovery;
		if (given === false) {
			return null;
		}
		const crashes = given?.crashes;
		const interval = given?.interval;
		return {
			crashes: typeof crashes === 'number' && Number.isFinite(crashes) && crashes >= 0 ?
				Math.floor(crashes) : DEFAULT_CK_EDITOR_RECOVERY.crashes,
			interval: typeof interval === 'number' && Number.isFinite(interval) && interval >= 0 ?
				interval : DEFAULT_CK_EDITOR_RECOVERY.interval
		};
	}

	/**
	 * @returns whether CKEditor starts again after the crashes it had, as CKEditor's Watchdog
	 *          decided: after each of the first ones, then while the last of them came further
	 *          apart than the interval on average
	 */
	private mayRestart(recovery: Required<CKEditorRecovery>): boolean {
		const times = this.crashTimes;
		if (times.length <= recovery.crashes) {
			return true;
		}
		const last = times[times.length - 1]!;
		const first = times[times.length - 1 - recovery.crashes]!;
		return (last - first) / recovery.crashes > recovery.interval;
	}

	/**
	 * CKEditor ran into an error after it started: CKEditor starts again with the text it had and
	 * the selection, unless the recovery is off, or the errors come too often, CKEditor then
	 * running on as it is. Then the error is told: in the log, to onError and with the event
	 * error; and to the user, in a line below the text, unless the recovery is off.
	 */
	private crashed(editor: CKEditorInstance, error: unknown): void {
		const recovery = this.recovery();
		this.crashCount++;
		if (recovery) {
			this.crashTimes = [...this.crashTimes, Date.now()].slice(-(recovery.crashes + 1));
		}
		const restarting = !!recovery && !this.gaveUp && this.mayRestart(recovery);
		if (recovery && !restarting) {
			this.gaveUp = true;
		}
		getHost().log.error(restarting ? 'CKEditor ran into an error, and starts again' :
			this.gaveUp ? 'CKEditor ran into an error again, and runs on as it is' :
				'CKEditor ran into an error', error);
		if (restarting) {
			this.recover(editor);
		}
		this.tellError(error, { started: true, restarting, gaveUp: this.gaveUp,
			crashes: this.crashCount });
		if (recovery && !this.destroyed) {
			this.errorShown = restarting ? 'restarted' : 'gaveUp';
			this.renderErrorLine();
		}
	}

	/**
	 * Starts CKEditor again after it ran into an error, as the Watchdog did: with the text it last
	 * had, which the text area holds, and the selection, the focus back in it if it was there.
	 */
	private recover(editor: CKEditorInstance): void {
		let selection = this.selected;
		try {
			selection = this.selection();
		} catch (error) {
			getHost().log.error('CKEditor could not tell what is selected', error);
		}
		const focused = this.focused;
		this.pendingSelection = selection;
		this.unbindScroller();
		this.editor = null;
		this.stop(editor);
		this.restartWith(this.value);
		if (focused) {
			this.ready.then((restarted) => restarted.focus(), () => undefined);
		}
	}

	/**
	 * Tells what went wrong: to onError, then with the event error.
	 */
	private tellError(error: unknown, failure: CKEditorFailure): void {
		try {
			this.options.onError?.(error, { ...failure });
		} catch (thrown) {
			getHost().log.error('onError of the editor failed', thrown);
		}
		this.emit(CK_EDITOR_EVENTS.error, { ...failure, error, ...describeError(error) });
	}

	/**
	 * Shows the line telling the user CKEditor ran into an error, below the text, before the count
	 * and the handle resizing the editor; or takes it away, once it tells nothing.
	 */
	private renderErrorLine(): void {
		const shown = this.errorShown;
		if (!shown || this.destroyed) {
			this.errorLine?.remove();
			this.errorLine = null;
			return;
		}
		if (!this.errorLine) {
			this.errorLine = document.createElement('div');
			this.errorLine.className = CK_EDITOR_CLASSES.error;
		}
		const line = this.errorLine;
		// polite as CKEditor started again; the user has to act once it gave up
		line.setAttribute('role', shown === 'gaveUp' ? 'alert' : 'status');
		line.toggleAttribute('data-gave-up', shown === 'gaveUp');
		const texts = this.texts();
		const text = shown === 'gaveUp' ? texts.errorGaveUp : texts.errorRestarted;
		if (line.textContent !== text) {
			line.textContent = text;
		}
		const next = this.status ?? this.element.querySelector(':scope > .antilia-resizable-handle');
		if (next) {
			if (line.nextElementSibling !== next) {
				next.before(line);
			}
		} else if (line.parentElement !== this.element || line.nextElementSibling) {
			this.element.append(line);
		}
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
			// HTML, not text, which CKEditor makes HTML of: what CKEditor inserts is compared with it;
			// a paste of the browser has no content yet, which CKEditor reads from the clipboard
			const html = transfer?.getData('text/html');
			this.pasted = !html ? null : typeof data.content === 'string' ? data.content :
				quietHtml(html);
			if (transfer) {
				data.dataTransfer = Object.assign(Object.create(transfer) as typeof transfer, {
					getData: (type: string) => {
						const value = transfer.getData(type);
						return type === 'text/html' && value ? quietHtml(value) : value;
					}
				});
			}
		}, { priority: 'highest' });
		this.hearPastes(editor);
		this.receiveUploads(editor);
		this.receiveAttachments(editor);
		this.renderAttachPicker();
		takesAway(editor, 'superscript', 'subscript');
		takesAway(editor, 'subscript', 'superscript');
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
		// the images and the files the text has, which the user takes out from now on
		const root = editor.model.document.getRoot();
		this.images = ckImagesIn(root);
		this.attachments = ckAttachmentsIn(root);
		if (!this.savedCanonical) {
			this.saved = this.canonical(this.saved);
			this.savedCanonical = true;
		}
		this.renderReadOnly();
		this.renderSpellcheck();
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
		this.renderNotice();
		this.renderStatus();
		this.renderModified(this.value, false);
		this.restoreScroll();
		this.dispatch('text');
	}

	/**
	 * Compares what CKEditor inserts of a paste with the HTML pasted, before it inserts it, after
	 * the plugins changing it (Paste from Office): what the paste lost is told once it is in the
	 * text. Nothing without the clipboard's pipeline.
	 */
	private hearPastes(editor: CKEditorInstance): void {
		const pipeline = editor.plugins?.has('ClipboardPipeline') ?
			editor.plugins.get('ClipboardPipeline') as CKEditorEmitter : null;
		pipeline?.on('contentInsertion', (_event, data: { content?: unknown }) => {
			const pasted = this.pasted;
			this.pasted = null;
			if (pasted === null || editor !== this.editor || this.destroyed || !data.content) {
				return;
			}
			try {
				this.pasteCleaned(pasteCleaningReport(pasted, editor.data.stringify(data.content)));
			} catch (error) {
				getHost().log.error('The editor could not tell what the paste lost', error);
			}
		}, { priority: 'normal' });
	}

	/**
	 * Tells what a paste lost, once the paste is in the text: onPasteCleaned, the event
	 * paste-cleaned and the notice; a paste losing nothing tells nothing.
	 */
	private pasteCleaned(report: RichTextCleaningReport): void {
		if (isEmptyCleaningReport(report)) {
			return;
		}
		// after the paste, which CKEditor inserts right after this
		queueMicrotask(() => {
			if (this.destroyed) {
				return;
			}
			this.notice?.show(pasteNoticeText(report, this.texts()));
			this.options.onPasteCleaned?.({ elements: { ...report.elements },
				attributes: { ...report.attributes } });
			this.trigger(RICH_TEXT_BUILT_IN_EVENTS.pasteCleaned, { elements: report.elements,
				attributes: report.attributes });
		});
	}

	/**
	 * @returns whether the editor uploads images: it has the feature image and onUpload
	 */
	private uploading(): boolean {
		return this.features().has('image') && typeof this.options.onUpload === 'function';
	}

	/**
	 * @returns the media types of the images uploaded, lower case
	 */
	private uploadTypes(): string[] {
		return (this.options.uploadTypes ?? DEFAULT_RICH_TEXT_UPLOAD_TYPES).map((type) =>
			type.trim().toLowerCase()).filter(Boolean);
	}

	/**
	 * @returns the widest and the highest picture uploaded, null for no limit
	 */
	private uploadMaxDimensions(): RichTextImageDimensions | null {
		const { uploadMaxWidth: width, uploadMaxHeight: height } = this.options;
		const positive = (value: number | null | undefined): value is number =>
			typeof value === 'number' && Number.isFinite(value) && value > 0;
		return positive(width) && positive(height) ?
			{ width: Math.floor(width), height: Math.floor(height) } : null;
	}

	/**
	 * Lets CKEditor upload the images the user chooses, pastes or drops, by an upload adapter of
	 * the editor's own calling onUpload, once checked: how many, their types, sizes and pixels,
	 * those too large scaled down when the user ticks them; tells why it did not upload one in the
	 * line above the text, not in CKEditor's alert, and of files of a type not taken that CKEditor
	 * leaves alone; and shows the progress of each by classes. Nothing without onUpload or
	 * CKEditor's FileRepository.
	 */
	private receiveUploads(editor: CKEditorInstance): void {
		const plugins = editor.plugins;
		if (!this.uploading() || !plugins?.has('FileRepository')) {
			return;
		}
		const repository = plugins.get('FileRepository') as {
			createUploadAdapter?: (loader: CKEditorFileLoader) => CKEditorUploadAdapter;
			readonly loaders?: { get(id: unknown): CKEditorFileLoader | null | undefined };
		};
		repository.createUploadAdapter = (loader) => this.uploadAdapter(loader);
		if (plugins.has('Notification')) {
			// CKEditor's warning that an upload failed, its message the adapter's
			(plugins.get('Notification') as CKEditorEmitter).on('show:warning:upload',
				(event: { stop(): void }, data: { message?: unknown } | undefined) => {
					event.stop();
					const message = data?.message;
					if (editor === this.editor && typeof message === 'string' && message) {
						this.alert(message);
					}
				}, { priority: 'high' });
		}
		// the files CKEditor's ImageUpload is given, chosen, pasted or dropped, checked before it
		// uploads them: how many, their types, sizes and pixels, those too large scaled down when
		// the user wants it; told why not, the files chosen of another type among them
		const command = editor.commands?.get('uploadImage');
		command?.on('execute', (event: { stop(): void }, args: unknown[] = []) => {
			if (this.passing) {
				return;
			}
			const given = (args[0] as { file?: File | readonly File[] } | undefined)?.file;
			const files = Array.isArray(given) ? [...given as readonly File[]] :
				given ? [given as File] : [];
			const refused = this.typeRefusals;
			this.typeRefusals = [];
			if (!files.length) {
				return;
			}
			event.stop();
			void this.checkUploads(editor, files, refused);
		}, { priority: 'high' });
		this.quietUploadProgress(editor, repository);
	}

	/**
	 * Checks the images CKEditor's ImageUpload was given before it uploads them, as the Tiptap
	 * editor checks them: how many one choice or drop takes, their types, sizes and pixels, asking
	 * the user which pictures too large to scale down while the page allows it. Then CKEditor
	 * uploads those left, where the selection is; the line above the text tells why not the others.
	 *
	 * @param refused why files chosen were of no type taken, which CKEditor left out
	 */
	private async checkUploads(editor: CKEditorInstance, files: readonly File[],
		refused: readonly string[]): Promise<void> {
		const texts = this.texts();
		const limits = this.uploadLimits();
		let checked: File[] = [];
		const messages = [...refused];
		try {
			const check = await checkFiles(files, limits, texts, (oversize) => askToScale(oversize,
				limits.maxDimensions!, texts, this.look));
			checked = check.files;
			messages.push(...check.refused);
		} catch (error) {
			getHost().log.error('The editor could not check the images', error);
			messages.push(...files.map((file) => format(texts.uploadFailed, fileNameOf(file, texts))));
		}
		if (messages.length) {
			this.alert(messages.join(' '));
		}
		const command = editor.commands?.get('uploadImage');
		if (!checked.length || !command || editor !== this.editor || this.destroyed) {
			return;
		}
		this.passing = true;
		try {
			command.execute({ file: checked });
		} finally {
			this.passing = false;
		}
	}

	/**
	 * Hears of the files pasted or dropped into the text CKEditor leaves alone, those of no type of
	 * image taken: attached when the editor attaches files, else told why not.
	 */
	private receiveAttachments(editor: CKEditorInstance): void {
		if (!this.uploading() && !this.attaching()) {
			return;
		}
		editor.editing.view.document.on('clipboardInput', (event: { stop(): void }, data: {
			dataTransfer?: { files?: ArrayLike<File>; getData?(type: string): string };
			targetRanges?: readonly unknown[] | null }) => {
			const transfer = data.dataTransfer;
			const files = Array.from(transfer?.files ?? []);
			if (!files.length || transfer?.getData?.('text/html') || editor !== this.editor) {
				return;
			}
			const types = this.uploadTypes();
			const images = this.uploading() ? files.filter((file) =>
				types.includes(file.type.toLowerCase())) : [];
			const others = files.filter((file) => !images.includes(file));
			if (!others.length) {
				return;
			}
			if (this.attaching()) {
				// where the files dropped go
				const ranges = data.targetRanges;
				const mapper = (editor.editing as { mapper?: { toModelRange(range: unknown): unknown }
					}).mapper;
				if (ranges?.length && mapper) {
					editor.model.change((writer) => (writer as unknown as {
						setSelection(ranges: unknown[]): void }).setSelection(ranges.map((range) =>
						mapper.toModelRange(range))));
				}
				void this.attachFiles(editor, others);
				if (!images.length) {
					event.stop();
				}
				return;
			}
			// of a type not taken, which CKEditor's ImageUpload leaves alone: told why, as the
			// Tiptap editor does
			const texts = this.texts();
			const refused = others.map((file) => refusalOf(file, { types, maxSize: null }, texts))
				.filter((message): message is string => !!message);
			if (refused.length) {
				this.alert(refused.join(' '));
			}
		}, { priority: 'high' });
	}

	/**
	 * @returns whether the tools of a condition show in the default toolbar
	 */
	private hasTools(condition: ToolCondition): boolean {
		switch (condition) {
			case 'upload':
				return this.uploading();
			case 'dialog':
				return this.uploading() && (!!this.options.uploadDialog ||
					typeof this.options.onStorePermanently === 'function');
			case 'find':
				return this.features().has('image') && typeof this.options.onFindImages === 'function';
			case 'attach':
				return this.attaching();
		}
	}

	/**
	 * @returns a plugin of CKEditor of the editor's own, a function CKEditor calls with new as it
	 *          makes its plugins: the files attached and the tools of the uploads
	 */
	private ownPlugin(build: CKEditorBuild): object {
		const extend = (editor: CKEditorInstance) => this.extend(editor, build);
		return function AntiliaUploads(editor: CKEditorInstance): void {
			extend(editor);
		};
	}

	/**
	 * Gives CKEditor, as it starts, the files attached, for the feature attachment, and the tools
	 * of the uploads, by their names.
	 */
	private extend(editor: CKEditorInstance, build: CKEditorBuild): void {
		const own = editor as unknown as CKUploadsEditor;
		const kit = build as unknown as CKUploadsBuild;
		try {
			if (this.features().has('attachment')) {
				ckAttachmentConversion(own, kit, () => attachmentUrlTest(this.options.attachmentUrls),
					() => this.attachTypes());
			}
			ckRegisterTools(own, kit, this.tools(editor, build));
		} catch (error) {
			getHost().log.error('The editor could not give CKEditor its tools of the uploads', error);
		}
	}

	/**
	 * @returns the tools of the uploads, as CKEditor names them: the images of the text, the
	 *          dialog of the uploads and the images stored, for the feature image; attaching a file
	 *          and the files of the text, for the feature attachment while the editor attaches
	 */
	private tools(editor: CKEditorInstance, build: CKEditorBuild): (CKButtonTool | CKPanelTool)[] {
		const texts = this.texts();
		const features = this.features();
		const icon = (name: string, own: string) => {
			const given = build[name];
			return typeof given === 'string' && given.trim().startsWith('<svg') ? given : own;
		};
		const tools: (CKButtonTool | CKPanelTool)[] = [];
		if (features.has('image')) {
			tools.push({ name: 'images', label: texts.images, icon: ICONS.images,
				render: (panel, close) => this.renderImages(editor, panel, close) });
			if (this.uploading()) {
				tools.push({ name: 'uploadImages', label: texts.uploadImage,
					icon: icon('IconImageUpload', ICONS.uploadImages),
					run: () => this.openImageDialog(editor) });
			}
			if (typeof this.options.onFindImages === 'function') {
				tools.push({ name: 'storedImage', label: texts.storedImage,
					icon: icon('IconImageAssetManager', ICONS.storedImage),
					render: (panel, close) => this.renderStoredImages(editor, panel, close) });
			}
		}
		if (this.attaching()) {
			tools.push({ name: 'attachment', label: texts.attachment,
				icon: icon('IconPaperClip', ICONS.attachment),
				run: () => this.chooseAttachments(editor) });
			tools.push({ name: 'attachments', label: texts.attachments,
				icon: icon('IconBrowseFiles', ICONS.attachments),
				render: (panel, close) => this.renderAttachments(editor, panel, close) });
		}
		return tools;
	}

	/**
	 * Lists the images of the text in the panel of the tool images: each selected by a click, and
	 * taken out of the text, every place it is in, by its button.
	 */
	private renderImages(editor: CKEditorInstance, panel: HTMLElement, close: () => void): void {
		const own = editor as unknown as CKUploadsEditor;
		const texts = this.texts();
		ckFillList(panel, 'images', this.look, ckImagesOf(editor.model.document.getRoot())
			.map(({ src, alt }) => {
				const label = imageLabel(src, alt, texts);
				return { label, thumbnail: src, deleteLabel: format(texts.deleteImage, label),
					choose: () => {
						close();
						ckSelect(own, 'image', src);
					},
					remove: () => {
						close();
						ckRemoveImages(own, src);
					} };
			}), texts.noImages);
	}

	/**
	 * Lists the files attached to the text in the panel of the tool attachments, as the images.
	 */
	private renderAttachments(editor: CKEditorInstance, panel: HTMLElement,
		close: () => void): void {
		const own = editor as unknown as CKUploadsEditor;
		const texts = this.texts();
		ckFillList(panel, 'attachments', this.look, ckAttachmentsOf(editor.model.document
			.getRoot()).map((attachment) => {
			const label = attachment.name || attachment.href;
			return { label, size: attachment.size, kind: attachment.kind,
				deleteLabel: format(texts.deleteAttachment, label),
				choose: () => {
					close();
					ckSelect(own, 'attachment', attachment.href);
				},
				remove: () => {
					close();
					ckRemoveAttachments(own, attachment.href);
				} };
		}), texts.noAttachments);
	}

	/**
	 * Puts the form finding the images the page stores in the panel of the tool storedImage: the
	 * image chosen goes into the text where the selection is.
	 *
	 * @returns what stops the form searching
	 */
	private renderStoredImages(editor: CKEditorInstance, panel: HTMLElement,
		close: () => void): () => void {
		const find = this.options.onFindImages;
		if (typeof find !== 'function') {
			return () => undefined;
		}
		const picker = imagePicker(this.texts(), this.look, find, (image) => {
			close();
			this.insertImages(editor, [image.url]);
		});
		picker.input.addEventListener('keydown', (event) => {
			if (event.key === 'Enter') {
				event.preventDefault();
				picker.choose();
			}
		});
		panel.append(picker.element);
		picker.search();
		// once the drop-down shows its panel
		setTimeout(() => picker.input.focus());
		return () => picker.destroy();
	}

	/**
	 * Inserts images where the selection is, by CKEditor's command insertImage.
	 */
	private insertImages(editor: CKEditorInstance, srcs: readonly string[]): void {
		const safe = srcs.filter((src) => isSafeUrl(src));
		const command = editor.commands?.get('insertImage');
		if (!safe.length || !command || editor !== this.editor || this.destroyed) {
			return;
		}
		command.execute({ source: safe });
		editor.editing.view.focus();
	}

	/**
	 * Opens the dialog of the uploads of the images, which go into the text where the selection is
	 * as the user inserts them; those the user ticked are stored permanently.
	 */
	private openImageDialog(editor: CKEditorInstance): void {
		openUploadDialog(this.imageDialogHost(), this.texts(), this.look, (srcs) => {
			if (editor === this.editor && !this.destroyed) {
				this.insertImages(editor, srcs);
			} else {
				this.imagesRemoved(srcs);
			}
		});
	}

	/**
	 * @returns what the dialog of the uploads of the images asks of the editor
	 */
	private imageDialogHost(): UploadDialogHost {
		return {
			types: () => this.uploadTypes(),
			check: (files) => {
				const limits = this.uploadLimits();
				return checkFiles(files, limits, this.texts(), (oversize) => askToScale(oversize,
					limits.maxDimensions!, this.texts(), this.look));
			},
			upload: (file, options) => this.upload(file, options ?? {}),
			uploading: (file) => format(this.texts().uploading, fileNameOf(file, this.texts())),
			texts: () => this.texts(),
			alert: (message) => this.alert(message),
			discard: (urls) => this.imagesRemoved(urls.filter((url) => !this.images.has(url))),
			permanent: () => typeof this.options.onStorePermanently === 'function',
			storePermanently: async (urls) => {
				const keeper = this.options.onStorePermanently;
				return keeper && urls.length ? keeper([...urls]) : [];
			}
		};
	}

	/**
	 * @returns what the dialog of the uploads of the files attached asks of the editor
	 */
	private attachmentDialogHost(): UploadDialogHost {
		return {
			types: () => this.attachAccept(),
			check: async (files) => checkAttachments(files, this.attachLimits(), this.texts()),
			upload: (file, options) => this.attachFile(file, options),
			uploading: (file) => format(this.texts().attaching, fileNameOf(file,
				{ theImage: this.texts().theFile })),
			texts: () => this.texts(),
			alert: (message) => this.alert(message),
			discard: (urls) => this.attachmentsRemoved(urls.filter((url) =>
				!this.attachments.has(url))),
			permanent: () => false,
			storePermanently: async () => [],
			labels: () => {
				const texts = this.texts();
				return { title: texts.attachFilesTitle, drop: texts.dropFiles,
					choose: texts.chooseFiles, insert: texts.insertFiles, nameless: texts.theFile };
			}
		};
	}

	/**
	 * @returns the limits of the uploads of the images, as the options give them
	 */
	private uploadLimits(): UploadLimits {
		const { uploadMaxCount: count, uploadMaxSize: size } = this.options;
		return { types: this.uploadTypes(), maxSize: size ?? null,
			maxCount: positive(count) ? Math.floor(count) : null,
			maxDimensions: this.uploadMaxDimensions(), scaling: !!this.options.uploadScaling };
	}

	/**
	 * @returns whether the editor attaches files: it has the feature attachment and onAttach
	 */
	private attaching(): boolean {
		return this.features().has('attachment') && typeof this.options.onAttach === 'function';
	}

	private attachTypes(): RichTextAttachmentType[] {
		return attachmentTypes(this.options.attachTypes);
	}

	/**
	 * @returns the media types and the extensions of the files attached, as a file field takes them
	 */
	private attachAccept(): string[] {
		return this.attachTypes().flatMap((each) => [each.type, '.' + each.extension]);
	}

	/**
	 * @returns the limits of the files attached, as the options give them
	 */
	private attachLimits(): AttachmentLimits {
		const { attachMaxSize: size, attachMaxCount: count } = this.options;
		return { types: this.attachTypes(), maxSize: positive(size) ? size : null,
			maxCount: positive(count) ? Math.floor(count) : null };
	}

	/**
	 * Lets the user choose files to attach: in the dialog of the uploads when the options want it,
	 * else by the browser's dialog of files, the files going into the text where the selection is
	 * once uploaded.
	 */
	private chooseAttachments(editor: CKEditorInstance): void {
		if (this.options.attachDialog) {
			openUploadDialog(this.attachmentDialogHost(), this.texts(), this.look, (srcs, files) => {
				if (editor !== this.editor || this.destroyed) {
					this.attachmentsRemoved(srcs);
					return;
				}
				ckInsertAttachments(editor as unknown as CKUploadsEditor, srcs.map((src, index) =>
					this.attachmentOf(src, files[index]!)));
				editor.editing.view.focus();
			});
			return;
		}
		this.renderAttachPicker()?.click();
	}

	/**
	 * Gives the editor the field choosing the files to attach, hidden, while it attaches files, the
	 * files chosen going where the selection is; or takes it away.
	 *
	 * @returns the field, null for none
	 */
	private renderAttachPicker(): HTMLInputElement | null {
		if (!this.attaching() || this.destroyed) {
			this.attachPicker?.remove();
			this.attachPicker = null;
			return null;
		}
		if (!this.attachPicker) {
			const picker = document.createElement('input');
			picker.type = 'file';
			picker.multiple = true;
			picker.hidden = true;
			picker.tabIndex = -1;
			picker.className = CK_EDITOR_CLASSES.attachPicker;
			picker.addEventListener('change', () => {
				const files = Array.from(picker.files ?? []);
				picker.value = '';
				const current = this.editor;
				if (files.length && current) {
					void this.attachFiles(current, files);
				}
			});
			this.element.append(picker);
			this.attachPicker = picker;
		}
		this.attachPicker.accept = this.attachAccept().join(',');
		return this.attachPicker;
	}

	/**
	 * @returns what a file attached at an address keeps: its name, its size and its kind
	 */
	private attachmentOf(src: string, file: File) {
		return { href: src, name: file.name || this.texts().theFile, size: sizeText(file.size),
			kind: attachmentKind(file, this.attachTypes()) };
	}

	/**
	 * Attaches files to the text: checks them, uploads them, telling so in the line above the
	 * text, and inserts them where the selection was, in their order; tells why not the others.
	 */
	private async attachFiles(editor: CKEditorInstance, files: readonly File[]): Promise<void> {
		const texts = this.texts();
		const { files: checked, refused } = checkAttachments(files, this.attachLimits(), texts);
		if (!checked.length) {
			if (refused.length) {
				this.alert(refused.join(' '));
			}
			return;
		}
		// where the files go, as the text changes meanwhile
		const LiveRange = (this.build() as { LiveRange?: { fromRange(range: unknown):
			{ detach(): void } } } | null)?.LiveRange;
		const first = (editor.model.document.selection as { getFirstRange?(): unknown })
			.getFirstRange?.();
		const at = LiveRange && first ? LiveRange.fromRange(first) : null;
		this.alert(checked.map((file) => format(texts.attaching, fileNameOf(file,
			{ theImage: texts.theFile }))).join(' '), 'status');
		const results = await Promise.allSettled(checked.map((file) => this.attachFile(file)));
		const attached = results.flatMap((result, index) => result.status === 'fulfilled' ?
			[this.attachmentOf(result.value, checked[index]!)] : []);
		const failed = results.flatMap((result, index) => result.status === 'rejected' &&
			!isAbort(result.reason) ? [result.reason instanceof Error && result.reason.message ?
				result.reason.message : format(texts.uploadFailed, fileNameOf(checked[index]!,
					{ theImage: texts.theFile }))] : []);
		// why the files refused were not attached, and those failing, in place of the status
		if (refused.length || failed.length) {
			this.alert([...refused, ...failed].join(' '));
		} else {
			this.clearMessage();
		}
		if (editor !== this.editor || this.destroyed) {
			at?.detach();
			this.attachmentsRemoved(attached.map((each) => each.href));
			return;
		}
		if (at) {
			editor.model.change((writer) => (writer as unknown as { setSelection(range: unknown):
				void }).setSelection(at));
			at.detach();
		}
		ckInsertAttachments(editor as unknown as CKUploadsEditor, attached);
	}

	/**
	 * Uploads a file attached, by the page.
	 *
	 * @returns the address of the file, a safe one; rejected with the message saying why not
	 */
	private async attachFile(file: File, options?: RichTextUploadOptions): Promise<string> {
		const texts = this.texts();
		const failed = format(texts.uploadFailed, fileNameOf(file, { theImage: texts.theFile }));
		const uploader = this.options.onAttach;
		const refused = attachmentRefusal(file, this.attachLimits(), texts);
		if (!uploader || refused) {
			throw new Error(refused ?? failed);
		}
		let address: string;
		try {
			address = String(await uploader(file, options)).trim();
		} catch (error) {
			if (isAbort(error)) {
				throw error;
			}
			throw new Error(error instanceof Error && error.message ? error.message : failed);
		}
		if (!isSafeUrl(address)) {
			getHost().log.error('The address of a file attached is not one the editor takes: ' +
				address);
			throw new Error(failed);
		}
		return address;
	}

	/**
	 * Notes the images and the files the text no longer holds, after a change of the user or of
	 * the page's code, to tell of in a moment; those it holds again by then are not told of. A text
	 * the page gives tells of nothing.
	 *
	 * @param user whether the user changed the text, not the page
	 */
	private trackRemovals(editor: CKEditorInstance, user: boolean): void {
		const root = editor.model.document.getRoot();
		const before = this.images;
		const after = ckImagesIn(root);
		this.images = after;
		const attachedBefore = this.attachments;
		const attached = ckAttachmentsIn(root);
		this.attachments = attached;
		if (!user) {
			return;
		}
		before.forEach((_count, src) => {
			if (!after.has(src)) {
				this.removed.add(src);
			}
		});
		after.forEach((_count, src) => this.removed.delete(src));
		attachedBefore.forEach((_count, href) => {
			if (!attached.has(href)) {
				this.removedAttachments.add(href);
			}
		});
		attached.forEach((_count, href) => this.removedAttachments.delete(href));
		if ((this.removed.size || this.removedAttachments.size) && this.removedTimer === null) {
			this.removedTimer = setTimeout(() => this.flushRemoved(), REMOVED_DELAY);
		}
	}

	/**
	 * Tells of the images and the files taken out of the text, those it does not hold again.
	 */
	private flushRemoved(): void {
		if (this.removedTimer !== null) {
			clearTimeout(this.removedTimer);
			this.removedTimer = null;
		}
		const urls = Array.from(this.removed).filter((url) => !this.images.has(url));
		this.removed.clear();
		this.imagesRemoved(urls);
		const files = Array.from(this.removedAttachments).filter((url) =>
			!this.attachments.has(url));
		this.removedAttachments.clear();
		this.attachmentsRemoved(files);
	}

	/**
	 * Tells the page of images the text no longer holds: onImagesRemoved, then the event
	 * images-removed.
	 */
	private imagesRemoved(urls: readonly string[]): void {
		if (!urls.length || this.destroyed) {
			return;
		}
		this.options.onImagesRemoved?.([...urls]);
		this.trigger(RICH_TEXT_BUILT_IN_EVENTS.imagesRemoved, { urls: [...urls] });
	}

	/**
	 * Tells the page of files the text no longer holds: onAttachmentsRemoved, then the event
	 * attachments-removed.
	 */
	private attachmentsRemoved(urls: readonly string[]): void {
		if (!urls.length || this.destroyed) {
			return;
		}
		this.options.onAttachmentsRemoved?.([...urls]);
		this.trigger(RICH_TEXT_BUILT_IN_EVENTS.attachmentsRemoved, { urls: [...urls] });
	}

	/**
	 * @returns the upload adapter of a file of CKEditor: checks the file, then uploads it by
	 *          onUpload, telling CKEditor the progress, and stops it as CKEditor aborts it; rejects
	 *          with the message telling why not, which CKEditor gives its warning, or with the
	 *          abort
	 */
	private uploadAdapter(loader: CKEditorFileLoader): CKEditorUploadAdapter {
		const controller = new AbortController();
		return {
			upload: async () => {
				const file = await loader.file;
				const texts = this.texts();
				if (!file) {
					throw format(texts.uploadFailed, texts.theImage);
				}
				try {
					const address = await this.upload(file, { signal: controller.signal,
						onProgress: (sent, total) => {
							loader.uploadTotal = total;
							loader.uploaded = sent;
						} });
					// the user took the image out as it uploaded: the page hears of it, once
					// CKEditor had its chance to put the address in
					const editor = this.editor;
					setTimeout(() => {
						if (editor && editor === this.editor && !this.images.has(address)) {
							this.imagesRemoved([address]);
						}
					});
					return { default: address };
				} catch (error) {
					if (isAbort(error)) {
						throw error;
					}
					throw error instanceof Error && error.message ? error.message :
						format(texts.uploadFailed, fileNameOf(file, texts));
				}
			},
			abort: () => controller.abort()
		};
	}

	/**
	 * Uploads an image by onUpload, once checked: its type, its size and its pixels.
	 *
	 * @returns the address of the image, a safe one; rejected with the message saying why not
	 */
	private async upload(file: File, options: RichTextUploadOptions): Promise<string> {
		const texts = this.texts();
		const name = fileNameOf(file, texts);
		const failed = format(texts.uploadFailed, name);
		const uploader = this.options.onUpload;
		const types = this.uploadTypes();
		let refused = refusalOf(file, { types, maxSize: this.options.uploadMaxSize ?? null },
			texts);
		const max = this.uploadMaxDimensions();
		if (!refused && max) {
			const dimensions = await readImageDimensions(file, file.type);
			if (dimensions && !fits(dimensions, max)) {
				refused = format(texts.uploadTooBig, name, dimensionsText(dimensions),
					dimensionsText(max));
			}
		}
		if (!uploader || refused) {
			throw new Error(refused ?? failed);
		}
		let address: string;
		try {
			address = String(await uploader(file, options)).trim();
		} catch (error) {
			if (isAbort(error)) {
				throw error;
			}
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
	 * Shows the progress of the uploads by classes of the bar CKEditor's ImageUploadProgress puts
	 * on each image uploading, in steps of PROGRESS_STEP: CKEditor would write its width as a
	 * style attribute, which a Content Security Policy without 'unsafe-inline' blocks. The editor
	 * hears of the progress first, and CKEditor no more.
	 */
	private quietUploadProgress(editor: CKEditorInstance, repository: {
		readonly loaders?: { get(id: unknown): CKEditorFileLoader | null | undefined } }): void {
		type ViewElement = { getChildren(): Iterable<ViewElement>;
			getCustomProperty?(key: string): unknown };
		const mapper = (editor.editing as { mapper?: { toViewElement(item: unknown): unknown } })
			.mapper;
		if (!mapper || !repository.loaders) {
			return;
		}
		const heard = new WeakSet<CKEditorFileLoader>();
		editor.conversion.for('editingDowncast').add((dispatcher) => IMAGES.forEach((image) =>
			// after CKEditor's, which puts the bar on the image
			dispatcher.on(`attribute:uploadStatus:${image}`, (_event, data: {
				item: { getAttribute(key: string): unknown }; attributeNewValue: unknown }) => {
				if (data.attributeNewValue !== 'uploading') {
					return;
				}
				const loader = repository.loaders?.get(data.item.getAttribute('uploadId'));
				const figure = mapper.toViewElement(data.item) as ViewElement | undefined;
				const bar = figure && Array.from(figure.getChildren()).find((child) =>
					child.getCustomProperty?.('progressBar'));
				if (!loader || !bar || heard.has(loader)) {
					return;
				}
				heard.add(loader);
				let shown: string | null = null;
				loader.on('change:uploadedPercent', (event: { stop(): void }, _name: string,
					percent: number) => {
					// CKEditor's own would write the width as a style
					event.stop();
					const step = uploadProgressClass(percent);
					if (step === shown || editor !== this.editor) {
						return;
					}
					const before = shown;
					shown = step;
					editor.editing.view.change((writer) => {
						const classes = writer as unknown as { addClass(name: string, element: unknown): void;
							removeClass(name: string, element: unknown): void };
						if (before) {
							classes.removeClass(before, bar);
						}
						classes.addClass(step, bar);
					});
				}, { priority: 'high' });
			}, { priority: 'low' })));
	}

	/**
	 * Tells the user why an image was not uploaded, above the text, for a while, as the Tiptap
	 * editor does; or, politely, what uploads.
	 *
	 * @param role alert for what went wrong, status for what goes on
	 */
	private alert(text: string, role: 'alert' | 'status' = 'alert'): void {
		if (this.destroyed) {
			return;
		}
		if (!this.message) {
			this.message = document.createElement('div');
			this.message.className = CK_EDITOR_CLASSES.message;
		}
		this.message.setAttribute('role', role);
		this.message.textContent = text;
		this.renderMessage();
		if (this.messageTimer !== null) {
			clearTimeout(this.messageTimer);
		}
		this.messageTimer = setTimeout(() => this.clearMessage(), MESSAGE_TIME);
	}

	/**
	 * Puts the line telling why an image was not uploaded above the text, under the toolbar and
	 * the notice of what a paste lost, in CKEditor's element.
	 */
	private renderMessage(): void {
		const message = this.message;
		const main = this.editor?.ui.element?.querySelector(':scope > .ck-editor__main');
		if (!message) {
			return;
		}
		if (main) {
			if (message.nextElementSibling !== main) {
				main.before(message);
			}
		} else if (!message.isConnected) {
			this.element.prepend(message);
		}
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
	 * Shows the line telling what a paste lost under CKEditor's toolbar, or takes it away, as the
	 * options say: in CKEditor's element, before its text, once CKEditor started.
	 */
	private renderNotice(): void {
		if (!this.options.pasteNotice) {
			this.notice?.destroy();
			this.notice = null;
			return;
		}
		this.notice = this.notice ?? new RichTextPasteNotice();
		const element = this.notice.element;
		const ui = this.editor?.ui.element;
		const main = ui?.querySelector(':scope > .ck-editor__main');
		if (main) {
			if (element.nextElementSibling !== main) {
				main.before(element);
			}
		} else if (ui) {
			if (element.previousElementSibling !== ui) {
				ui.after(element);
			}
		} else {
			element.remove();
		}
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
			// a change of the model alone, an image given its natural size once it loaded
			return;
		}
		this.trackRemovals(editor, !this.external);
		// what a paste lost is told until the text changes again
		this.notice?.clear();
		this.value = value;
		this.syncField(value);
		this.renderModified(value, true);
		this.renderStatus();
		this.dispatch('text');
		if (!this.external) {
			// that CKEditor started again is told until the user changes the text
			if (this.errorShown === 'restarted') {
				this.errorShown = null;
				this.renderErrorLine();
			}
			this.textReported = false;
			this.options.onChange?.(value);
			this.fieldEvent('input');
			this.dispatch('change');
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
		// the images taken out a moment ago are told of before the page saves
		this.flushRemoved();
		this.options.onBlur?.(value);
		this.dispatch('blur');
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
		// after CKEditor's and the line telling of an error, before the handle resizing the editor
		const handle = this.element.querySelector(':scope > .antilia-resizable-handle');
		if (handle) {
			handle.before(this.status);
		} else {
			this.element.append(this.status);
		}
		this.errorLine?.after(this.status);
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

	/**
	 * Gives the text the spellcheck of the options, through CKEditor's view, which would write
	 * over an attribute set on the element; none for the browser's default.
	 */
	private renderSpellcheck(): void {
		const editor = this.editor;
		if (!editor) {
			return;
		}
		const spellcheck = this.options.spellcheck;
		const view = editor.editing.view;
		view.change((writer) => {
			const root = view.document.getRoot();
			if (spellcheck === true || spellcheck === false) {
				writer.setAttribute('spellcheck', String(spellcheck), root);
			} else {
				writer.removeAttribute('spellcheck', root);
			}
		});
	}

	private listen<E extends Event>(target: EventTarget, type: string,
		listener: (event: E) => void, options: AddEventListenerOptions | boolean = false): void {
		target.addEventListener(type, listener as EventListener, options);
		this.listeners.push([target, type, listener as EventListener,
			typeof options === 'boolean' ? options : !!options.capture]);
	}

	private bind(): void {
		// a file of another type chosen in CKEditor's dialog of files ("All files"), which its
		// ImageUpload leaves out without a word: told why, with the rest of the files given
		this.listen<Event>(this.element, 'change', (event) => {
			const input = event.target;
			if (!(input instanceof HTMLInputElement) || input.type !== 'file' ||
				input === this.attachPicker || !this.uploading() ||
				!this.editor?.ui.element?.contains(input)) {
				return;
			}
			const types = this.uploadTypes();
			const texts = this.texts();
			const refused = Array.from(input.files ?? []).map((file) => refusalOf(file,
				{ types, maxSize: null }, texts)).filter((message): message is string => !!message);
			if (!refused.length) {
				return;
			}
			this.typeRefusals = refused;
			// told with the others once CKEditor uploads them, or alone after the event: not in a
			// microtask, which the browser runs between the listeners of the event, before
			// CKEditor's own
			setTimeout(() => {
				if (this.typeRefusals === refused) {
					this.typeRefusals = [];
					this.alert(refused.join(' '));
				}
			});
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
				this.dispatch('discard');
			}
		};
		this.listen(document, 'antilia-tabs:discard', discard);
		this.listen(document, 'antilia-desktop:discard', discard);
		this.listen(window, 'pagehide', () => this.dispatch('leave', { leaving: true }));
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
				this.dispatch('scroll', { scroll: { ...this.scroll } });
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
		const event = new CustomEvent(type, { bubbles: true, detail: { ...detail, editor: this } });
		try {
			target.dispatchEvent(event);
		} finally {
			this.dispatching = dispatching;
		}
		// its handlers once the page heard of it
		this.handlers?.handle(event);
		return true;
	}

	/**
	 * Fires input or change on the text area, as a text area does when the user changes it, then
	 * the types of events firing from it, also without a text area.
	 */
	private fieldEvent(type: 'input' | 'change'): void {
		const event = new Event(type, { bubbles: true });
		if (this.field) {
			const dispatching = this.dispatching;
			this.dispatching = true;
			try {
				this.field.dispatchEvent(event);
			} finally {
				this.dispatching = dispatching;
			}
		}
		this.eventTypes.from(type);
		this.handlers?.handle(event);
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
	 * Listens to the events with their handlers: those of the option events, and the default ones.
	 */
	private renderEventHandlers(): void {
		this.handlers?.destroy();
		this.handlers = new EditorEventHandlers<CKEditor>(this, RICH_TEXT_EDITOR_EVENTS,
			this.options.events, (leaving) => this.report(leaving));
	}

	/**
	 * @returns whether the state, or the text, changed since they were last sent (takeState(),
	 *          onStateChange), or since the page gave the state
	 */
	hasUnsentState(): boolean {
		return !this.destroyed && (!this.textReported ||
			!sameState(this.getState(), this.reported ?? this.started));
	}

	/**
	 * Takes the state to send, as sent: hasUnsentState() is false after, until the user changes it
	 * again. Fires the event state. Wicket's editor sends it so with each of its events.
	 *
	 * @returns the state, and whether the text changed since it was last sent
	 */
	takeState(): { state: RichTextEditorState; changed: boolean } {
		return this.take();
	}

	/**
	 * Takes the state to send without the text, as sent, when it changed since it was last sent,
	 * or since the page gave it: the page's other requests carry it so, which do not send the
	 * text. The text stays unsent, for the editor's next event. Fires the event state.
	 *
	 * @returns the state, null when it was sent already
	 */
	takeUnsentState(): RichTextEditorState | null {
		if (this.destroyed) {
			return null;
		}
		const state = this.getState();
		if (sameState(state, this.reported ?? this.started)) {
			return null;
		}
		this.reported = state;
		this.dispatch('state', { state });
		return state;
	}

	/**
	 * @param leaving whether the page is going, for onStateChange; undefined not to call it
	 */
	private take(leaving?: boolean): { state: RichTextEditorState; changed: boolean } {
		const state = this.getState();
		const changed = !this.textReported;
		this.textReported = true;
		this.reported = state;
		if (leaving !== undefined) {
			this.options.onStateChange?.(state, { leaving, changed });
		}
		this.dispatch('state', { state });
		return { state, changed };
	}

	/**
	 * Gives onStateChange the state, when it or the text changed since they were last sent: what
	 * send does for the handlers of the events.
	 *
	 * @param leaving whether the page is going
	 */
	private report(leaving: boolean): void {
		if (this.hasUnsentState()) {
			this.take(leaving);
		}
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
		// the images taken out a moment ago are told of before
		this.flushRemoved();
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
			before.texts?.linkNewTab !== this.options.texts?.linkNewTab ||
			// ImageUpload, or a tool of the uploads, comes or goes
			UPLOAD_SWITCHES.some((name) => !!before[name] !== !!this.options[name]);
		if (restart && this.editor) {
			this.restart(before.format !== this.options.format);
		} else if (restart) {
			// CKEditor is starting with the options before: another starts, that one goes
			this.restartWith(this.value);
		} else {
			this.renderReadOnly();
			if (changed('spellcheck')) {
				this.renderSpellcheck();
			}
		}
		this.renderNotice();
		this.renderStatus();
		this.renderErrorLine();
		this.renderResizable();
		if (before.events !== this.options.events) {
			this.renderEventHandlers();
		}
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
		// the images taken out a moment ago are told of still
		if (this.removedTimer !== null) {
			this.flushRemoved();
		}
		this.destroyed = true;
		this.generation++;
		this.handlers?.destroy();
		this.handlers = null;
		this.listeners.forEach(([target, type, listener, capture]) => {
			target.removeEventListener(type, listener, capture);
		});
		this.listeners = [];
		this.unbindScroller();
		this.stopWatchingRemoval?.();
		this.stopWatchingRemoval = null;
		this.status?.remove();
		this.status = null;
		this.notice?.destroy();
		this.notice = null;
		this.errorLine?.remove();
		this.errorLine = null;
		this.clearMessage();
		this.attachPicker?.remove();
		this.attachPicker = null;
		this.resizable?.destroy();
		this.resizable = null;
		const editor = this.editor;
		this.editor = null;
		if (editor) {
			this.stop(editor);
		}
		this.stopHearingErrors();
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
		this.stopHearingErrors(editor);
		const ui = editor.ui.element;
		try {
			Promise.resolve(editor.destroy()).catch((error: unknown) =>
				getHost().log.error('CKEditor could not stop', error));
		} catch (error) {
			// a CKEditor in a state of its own after an error
			getHost().log.error('CKEditor could not stop', error);
		}
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
