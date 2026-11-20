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

import { forwardRef, useImperativeHandle, useLayoutEffect, useRef } from 'react';

import { CKEditor as CoreCKEditor, type CKEditorBuild, type CKEditorFailure, type CKEditorInstance,
	type CKEditorRecovery, type EditorEvents, type RichTextUploader, type RichTextUploadOptions,
	type RichTextAttachmentType, type RichTextAttachmentUrls, type RichTextImageFinder,
	type RichTextImageKeeper, type RichTextStoredImage, type RichTextEditorEventKey,
	type CKEditorPlugin, type CKEditorTexts, type HeadingLevel, type ResizableSize,
	type ResizeAxis, type RichTextCleaningReport, type RichTextEditorSelection,
	type RichTextEditorState,
	type RichTextEditorStateChange, type RichTextFeature } from '@antilia/core';

import { useLook } from './Look.js';

export interface CKEditorProps {
	/**
	 * The classes of CKEditor: its ClassicEditor and plugins, imported from the ckeditor5 package
	 * by the page; the global CKEDITOR of its browser build when left out. Keep it the same: another
	 * makes CKEditor anew.
	 */
	build?: CKEditorBuild;
	/** The license key: 'GPL' for the open-source distribution, or the key CKSource gave. */
	licenseKey?: string;
	/** The language of CKEditor's texts, en when left out; its translations must be there. */
	language?: string;
	/** CKEditor's translations, imported from ckeditor5/translations; keep the array the same. */
	translations?: readonly unknown[];
	/**
	 * The text: what the editor starts with, and what it shows whenever this changes; the user's
	 * changes come with onChange.
	 */
	value?: string;
	/** The text last saved, against which the editor tells of changes; the first text by default. */
	saved?: string;
	/** How the editor keeps the text: html when left out, or markdown. */
	format?: 'html' | 'markdown';
	/**
	 * What the text may hold: all the features when left out (for Markdown, all it has a syntax
	 * for), the files attached with onAttach alone.
	 */
	features?: readonly RichTextFeature[];
	/** The levels of the headings, 1 to 4 when left out. */
	headingLevels?: readonly HeadingLevel[];
	/**
	 * The tools of the toolbar, as CKEditor names them, '|' between groups: those of the features
	 * when left out; false for none.
	 */
	toolbar?: readonly string[] | false;
	/** More plugins of CKEditor: their classes, or their names in the build; keep the array the same. */
	plugins?: readonly CKEditorPlugin[];
	/** More of CKEditor's configuration, over the editor's own; keep the object the same. */
	config?: Readonly<Record<string, unknown>>;
	/** Whether the user cannot change the text, false when left out. */
	readOnly?: boolean;
	/** The text shown while the editor is empty. */
	placeholder?: string;
	/** The accessible name of the text. */
	label?: string;
	/**
	 * Whether the browser checks the spelling of the text: as it does by default when left out or
	 * null.
	 */
	spellcheck?: boolean | null;
	/**
	 * The language of the text, a tag of BCP 47 (en-GB, de), which the browser checks its spelling
	 * in: that of the page when left out or null.
	 */
	textLanguage?: string | null;
	/** Whether the editor counts the characters and the words below the text, false when left out. */
	count?: boolean;
	/** The dimensions the user resizes the editor in, vertical when left out; false for none. */
	resizable?: ResizeAxis | false;
	minWidth?: number;
	minHeight?: number;
	maxWidth?: number | null;
	maxHeight?: number | null;
	/** The state the editor starts in: the selection, the scroll position and the size. */
	state?: RichTextEditorState;
	/**
	 * The handlers of the editor's events, by their keys (RICH_TEXT_EDITOR_EVENTS) or the names of
	 * other events it fires, each sending the state to onStateChange or not (send); those left out
	 * keep theirs: change, select, scroll and resize send the state 400 ms after the user paused,
	 * blur and leave at once; an empty list for none. Keep the object the same: another one puts
	 * the handlers anew.
	 */
	events?: EditorEvents<CoreCKEditor, RichTextEditorEventKey>;
	/**
	 * Whether a line under the toolbar tells the user what a paste lost, until the text changes
	 * again; false when left out.
	 */
	pasteNotice?: boolean;
	/** The types of the images the user may upload: PNG, JPEG, GIF, WebP and AVIF when left out. */
	uploadTypes?: readonly string[];
	/** The largest image the user may upload, in bytes; none when left out. */
	uploadMaxSize?: number | null;
	/**
	 * The widest and the highest picture the user may upload, in pixels, read from the header of
	 * the file; none when left out. Both, or none.
	 */
	uploadMaxWidth?: number | null;
	uploadMaxHeight?: number | null;
	/**
	 * Whether a dialog offers the user to scale down the pictures larger than that, each by a box
	 * to tick, rather than refusing them; false when left out.
	 */
	uploadScaling?: boolean;
	/** How many images one drop, choice or paste takes at most, the first; none when left out. */
	uploadMaxCount?: number | null;
	/**
	 * Whether the images are uploaded in the dialog of the uploads, the tool uploadImages, rather
	 * than by CKEditor's own button; false when left out.
	 */
	uploadDialog?: boolean;
	/**
	 * The types of the files the user may attach, each a media type, an extension and a label:
	 * those of the server when left out.
	 */
	attachTypes?: readonly RichTextAttachmentType[];
	/** The largest file the user may attach, in bytes; none when left out. */
	attachMaxSize?: number | null;
	/** How many files one drop, choice or paste takes at most, the first; none when left out. */
	attachMaxCount?: number | null;
	/** Whether the tool attachment opens the dialog of the uploads; false when left out. */
	attachDialog?: boolean;
	/**
	 * The addresses of the files attached to a text in Markdown, or a test of an address: a link
	 * to one of them comes back as a file attached; none when left out.
	 */
	attachmentUrls?: RichTextAttachmentUrls | null;
	/** What the editor says of its own, in English when left out: CKEditor translates the rest. */
	texts?: Partial<CKEditorTexts>;
	/** The name a form posts the text under, in a hidden text area; none when left out. */
	name?: string;
	id?: string;
	/** CSS classes of the editor, next to antilia-ck-editor. */
	className?: string;
	/**
	 * Uploads an image the user chose, pasted or dropped, resolving to its address, or rejecting
	 * with the message saying why not; it is told the progress and canceled as the user takes the
	 * image out (uploadImageTo(url) posts it to a URL). With it, the feature image has CKEditor's
	 * upload button, and takes images pasted and dropped; the latest is called. One coming or
	 * going makes CKEditor anew.
	 */
	onUpload?(file: File, options?: RichTextUploadOptions): Promise<string>;
	/**
	 * Stores the images ticked in the dialog of the uploads permanently, resolving to the addresses
	 * of those stored (storePermanentlyAt(url)); the latest is called. With it, the tool
	 * uploadImages opens the dialog, whose lines have the box to tick.
	 */
	onStorePermanently?(urls: string[]): Promise<string[]>;
	/**
	 * Finds the images stored permanently for what the user typed, which the tool storedImage,
	 * "Insert image from store", inserts (findImagesAt(url)); the latest is called.
	 */
	onFindImages?(query: string, signal: AbortSignal): Promise<readonly RichTextStoredImage[]>;
	/**
	 * Called a moment after the user took images out of the text, with their addresses, for the
	 * page to delete them (deleteImagesAt(url)).
	 */
	onImagesRemoved?(urls: string[]): void;
	/**
	 * Uploads a file the user attaches, a PDF or a spreadsheet, resolving to its address
	 * (uploadFileTo(url)): the tools attachment and attachments show, for the feature attachment;
	 * the latest is called. One coming or going makes CKEditor anew.
	 */
	onAttach?(file: File, options?: RichTextUploadOptions): Promise<string>;
	/** Called a moment after the user took files attached out of the text, with their addresses. */
	onAttachmentsRemoved?(urls: string[]): void;
	/** Called with CKEditor's editor once it started. */
	onReady?(editor: CKEditorInstance): void;
	/**
	 * How the editor starts CKEditor again after an error escaped it, as CKEditor's Watchdog did:
	 * after each of 3 crashes, then while they come more than 5 s apart on average, when left out;
	 * false to leave CKEditor running as it is.
	 */
	recovery?: CKEditorRecovery | false;
	/**
	 * Called with what went wrong when CKEditor could not start, or ran into an error after it
	 * started, and what the editor does about it.
	 */
	onError?(error: unknown, failure: CKEditorFailure): void;
	/** Called with the text each time the user changed it. */
	onChange?(value: string): void;
	onFocus?(): void;
	onBlur?(value: string): void;
	onSelectionChange?(selection: RichTextEditorSelection): void;
	/** Called with the state a moment after the user changed it, for the page to keep it. */
	onStateChange?(state: RichTextEditorState, change: RichTextEditorStateChange): void;
	/** Called when the text came to have changes not saved, or no longer has. */
	onModifiedChange?(modified: boolean): void;
	/** Called with the text when the user pressed Ctrl+S, or Cmd+S. */
	onSave?(value: string): void;
	onResize?(size: ResizableSize): void;
	/**
	 * Called with what a paste lost to CKEditor, by name and count, once it is in the text: a
	 * script, a handler of events, a link to an address the editor does not take...
	 */
	onPasteCleaned?(report: RichTextCleaningReport): void;
}

/**
 * What a ref to a {@link CKEditor} can do with it.
 */
export interface CKEditorHandle {
	/** The editor of the core, null until it is made. */
	readonly editor: CoreCKEditor | null;
	/** @returns the text */
	getValue(): string;
	/** Puts the focus in the text. */
	focus(): void;
	/** @returns the selection, the scroll position and the size */
	getState(): RichTextEditorState | null;
	/** Puts the editor in a state. */
	setState(state: RichTextEditorState): void;
	/** Takes the text, or another, as the text last saved. */
	markSaved(value?: string): void;
	/** @returns whether the text has changes not saved */
	isModified(): boolean;
}

/**
 * CKEditor 5 through the core, with the contract of the {@link RichTextEditor}: CKEditor's own
 * toolbar, balloons and dialogs, in the theme and the look of the page; Ctrl+S calls onSave; it is
 * resizable by the bar along its bottom unless told otherwise. React renders its element, the core
 * CKEditor in it, once CKEditor started (onReady); should it not, onError tells why. It tells
 * whether the text has changes not saved, with data-modified, which the tabs it is in show. Give
 * it a state to start in, one onStateChange reported, to show it again as the user left it. A ref
 * gets a {@link CKEditorHandle}.
 *
 * Its uploads are those of the {@link RichTextEditor}: images uploaded by onUpload within the
 * limits given, scaled down when the user ticks them, in the dialog of the uploads too; the tool
 * images lists them, and onImagesRemoved hears of those taken out of the text. With
 * onStorePermanently the user ticks the images to store permanently, and with onFindImages
 * inserts those stored by the tool storedImage; with onAttach the user attaches files. Their
 * lists, dialogs and files take the look of rich-text-editor.css, which the page loads too.
 *
 * The package has no code of CKEditor, which is licensed under the GPL or CKSource's commercial
 * terms: the page brings it, with its license key and its style sheet, ckeditor5.css.
 *
 * The HTML it gives comes from the user: a server keeping it cleans it, as Wicket's CKEditor does.
 */
export const CKEditor = forwardRef<CKEditorHandle, CKEditorProps>(
	function CKEditor(props, ref) {
		const { value, saved, build, licenseKey, language, translations, format, features,
			headingLevels, toolbar, plugins, config, readOnly, placeholder, label, spellcheck,
			textLanguage, count, resizable, minWidth, minHeight, maxWidth, maxHeight, events,
			pasteNotice, recovery, uploadTypes, uploadMaxSize, uploadMaxWidth, uploadMaxHeight,
			uploadScaling, uploadMaxCount, uploadDialog, attachTypes, attachMaxSize, attachMaxCount,
			attachDialog, attachmentUrls, texts, name, id, className } = props;
		const uploads = props.onUpload !== undefined;
		const keeps = props.onStorePermanently !== undefined;
		const finds = props.onFindImages !== undefined;
		const attaches = props.onAttach !== undefined;
		const element = useRef<HTMLDivElement>(null);
		const instance = useRef<CoreCKEditor | null>(null);
		const look = useLook();
		const latest = useRef(props);
		latest.current = props;
		const rendered = useRef(className);

		useImperativeHandle(ref, () => ({
			get editor() {
				return instance.current;
			},
			getValue: () => instance.current?.getValue() ?? latest.current.value ?? '',
			focus: () => instance.current?.focus(),
			getState: () => instance.current?.getState() ?? null,
			setState: (state) => instance.current?.setState(state),
			markSaved: (text) => instance.current?.markSaved(text),
			isModified: () => instance.current?.isModified() ?? false
		}), []);

		useLayoutEffect(() => {
			const current = latest.current;
			const editor = new CoreCKEditor(element.current!, {
				build: current.build, licenseKey: current.licenseKey, language: current.language,
				translations: current.translations, value: current.value ?? '', saved: current.saved,
				format: current.format, features: current.features,
				headingLevels: current.headingLevels, toolbar: current.toolbar,
				plugins: current.plugins, config: current.config, readOnly: current.readOnly,
				placeholder: current.placeholder, label: current.label,
				spellcheck: current.spellcheck, textLanguage: current.textLanguage, count: current.count,
				resizable: current.resizable, minWidth: current.minWidth,
				minHeight: current.minHeight, maxWidth: current.maxWidth, maxHeight: current.maxHeight,
				state: current.state, events: current.events,
				pasteNotice: current.pasteNotice, recovery: current.recovery, texts: current.texts,
				uploadTypes: current.uploadTypes, uploadMaxSize: current.uploadMaxSize,
				uploadMaxWidth: current.uploadMaxWidth, uploadMaxHeight: current.uploadMaxHeight,
				uploadScaling: current.uploadScaling, uploadMaxCount: current.uploadMaxCount,
				uploadDialog: current.uploadDialog, onUpload: uploaderOf(latest),
				onStorePermanently: keeperOf(latest), onFindImages: finderOf(latest),
				onImagesRemoved: (urls) => latest.current.onImagesRemoved?.(urls),
				attachTypes: current.attachTypes, attachMaxSize: current.attachMaxSize,
				attachMaxCount: current.attachMaxCount, attachDialog: current.attachDialog,
				attachmentUrls: current.attachmentUrls, onAttach: attacherOf(latest),
				onAttachmentsRemoved: (urls) => latest.current.onAttachmentsRemoved?.(urls), look,
				onReady: (next) => latest.current.onReady?.(next),
				onError: (error, failure) => latest.current.onError?.(error, failure),
				onChange: (next) => latest.current.onChange?.(next),
				onFocus: () => latest.current.onFocus?.(),
				onBlur: (next) => latest.current.onBlur?.(next),
				onSelectionChange: (next) => latest.current.onSelectionChange?.(next),
				onStateChange: (next, change) => latest.current.onStateChange?.(next, change),
				onModifiedChange: (next) => latest.current.onModifiedChange?.(next),
				onSave: (next) => latest.current.onSave?.(next),
				onResize: (next) => latest.current.onResize?.(next),
				onPasteCleaned: (report) => latest.current.onPasteCleaned?.(report)
			});
			instance.current = editor;
			return () => {
				editor.destroy();
				instance.current = null;
			};
		}, [look, name]);

		useLayoutEffect(() => {
			instance.current?.configure({ licenseKey, language, format, features, headingLevels,
				toolbar, readOnly, placeholder, label, spellcheck: spellcheck ?? null,
				textLanguage: textLanguage ?? null, count, resizable, minWidth, minHeight, maxWidth,
				maxHeight, pasteNotice: !!pasteNotice, recovery, texts });
		}, [licenseKey, language, format, features, headingLevels, toolbar, readOnly, placeholder,
			label, spellcheck, textLanguage, count, resizable, minWidth, minHeight, maxWidth,
			maxHeight, pasteNotice, recovery, texts]);

		useLayoutEffect(() => {
			if (events !== undefined) {
				instance.current?.configure({ events });
			}
		}, [events]);

		useLayoutEffect(() => {
			instance.current?.configure({ uploadTypes, uploadMaxSize: uploadMaxSize ?? null,
				uploadMaxWidth: uploadMaxWidth ?? null, uploadMaxHeight: uploadMaxHeight ?? null,
				uploadScaling: !!uploadScaling, uploadMaxCount: uploadMaxCount ?? null,
				uploadDialog: !!uploadDialog, onUpload: uploaderOf(latest),
				onStorePermanently: keeperOf(latest), onFindImages: finderOf(latest),
				attachTypes, attachMaxSize: attachMaxSize ?? null,
				attachMaxCount: attachMaxCount ?? null, attachDialog: !!attachDialog,
				attachmentUrls: attachmentUrls ?? null, onAttach: attacherOf(latest) });
		}, [uploadTypes, uploadMaxSize, uploadMaxWidth, uploadMaxHeight, uploadScaling,
			uploadMaxCount, uploadDialog, uploads, keeps, finds, attachTypes, attachMaxSize,
			attachMaxCount, attachDialog, attachmentUrls, attaches]);

		// compared by what they are: another one makes CKEditor anew
		useLayoutEffect(() => {
			instance.current?.configure({ build, plugins, config, translations });
		}, [build, plugins, config, translations]);

		useLayoutEffect(() => {
			const editor = instance.current;
			if (editor && value !== undefined && value !== editor.getValue()) {
				editor.setValue(value);
			}
		}, [value]);

		useLayoutEffect(() => {
			if (saved !== undefined) {
				instance.current?.markSaved(saved);
			}
		}, [saved]);

		// React wrote other classes over the element's: those the core keeps come back
		useLayoutEffect(() => {
			if (rendered.current !== className) {
				rendered.current = className;
				instance.current?.configure({});
			}
		}, [className]);

		return (
			<div ref={element} id={id} className={className}>
				{name !== undefined && <textarea name={name} defaultValue={value ?? ''} />}
			</div>
		);
	});

/**
 * @returns the uploader of the props, calling the latest onUpload; null without one
 */
function uploaderOf(props: { current: CKEditorProps }): RichTextUploader | null {
	return props.current.onUpload ? (file, options) => props.current.onUpload!(file, options) :
		null;
}

/**
 * @returns the uploader of the files attached of the props, calling the latest onAttach; null
 *          without one
 */
function attacherOf(props: { current: CKEditorProps }): RichTextUploader | null {
	return props.current.onAttach ? (file, options) => props.current.onAttach!(file, options) :
		null;
}

/**
 * @returns what stores the images permanently, calling the latest onStorePermanently; null
 *          without one
 */
function keeperOf(props: { current: CKEditorProps }): RichTextImageKeeper | null {
	return props.current.onStorePermanently ? (urls) => props.current.onStorePermanently!(urls) :
		null;
}

/**
 * @returns what finds the images stored, calling the latest onFindImages; null without one
 */
function finderOf(props: { current: CKEditorProps }): RichTextImageFinder | null {
	return props.current.onFindImages ? (query, signal) => props.current.onFindImages!(query,
		signal) : null;
}
