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

import { lezerHighlighter, RichTextEditor as CoreRichTextEditor, type HeadingLevel,
	type ResizableSize, type ResizeAxis, type RichTextEditorSelection, type RichTextEditorState,
	type RichTextBackgroundColor, type RichTextCleaningReport, type RichTextColor,
	type RichTextEditorStateChange,
	type RichTextEditorTexts,
	type RichTextExtension,
	type RichTextFeature, type RichTextFormat, type RichTextHighlighter,
	type RichTextImageFinder, type RichTextImageKeeper, type RichTextProofreader,
	type RichTextStoredImage, type RichTextSuggester, type RichTextTool, type RichTextUploader,
	type RichTextUploadOptions } from '@antilia/core';

import { useLook } from './Look.js';

export interface RichTextEditorProps {
	/**
	 * The text: what the editor starts with, and what it shows whenever this changes; the user's
	 * changes come with onChange.
	 */
	value?: string;
	/** The text last saved, against which the editor tells of changes; the first text by default. */
	saved?: string;
	/** How the editor keeps the text: html when left out, markdown or json. */
	format?: RichTextFormat;
	/** What the text may hold: all the features when left out (for Markdown, all it has a syntax for). */
	features?: readonly RichTextFeature[];
	/** The levels of the headings, 1 to 4 when left out. */
	headingLevels?: readonly HeadingLevel[];
	/**
	 * The palette of the colors of the text, in the order of its menu: DEFAULT_RICH_TEXT_COLORS
	 * when left out; null or empty for none, without the tool. The page's style sheets color the
	 * classes of colors of its own; Wicket's RichTextEditor.colors() gives a palette with its
	 * labels translated.
	 */
	colors?: readonly RichTextColor[] | null;
	/**
	 * The palette of the colors of the background, apart from those of the text:
	 * DEFAULT_RICH_TEXT_BACKGROUND_COLORS when left out; null or empty for none, without the tool.
	 * Wicket's RichTextEditor.backgroundColors() gives one with its labels translated.
	 */
	backgroundColors?: readonly RichTextBackgroundColor[] | null;
	/**
	 * The options of the features registered (registerRichTextFeature), by their names, which
	 * make their extensions.
	 */
	featureOptions?: Readonly<Record<string, Readonly<Record<string, unknown>>>>;
	/** The tools of the toolbar, in groups; the default toolbar when left out, false for none. */
	toolbar?: readonly (readonly RichTextTool[])[] | false;
	/** Whether a menu of the inline formats shows over the text selected, false when left out. */
	bubbleMenu?: boolean;
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
	/**
	 * What checks the spelling, the grammar and the style of the text, which the editor underlines:
	 * languageToolProofreader(), proofreadAt() or one of the page's own; none when left out.
	 */
	proofreader?: RichTextProofreader | null;
	/** How long after the user stopped typing the proofreader checks the text, 600 ms when left out. */
	proofreadDelay?: number;
	/**
	 * What offers items for what the user types: users to mention after @, words to complete on
	 * Ctrl+Space (suggestAt() for a Wicket page); none when left out.
	 */
	suggesters?: readonly RichTextSuggester[];
	/** The most characters the text may have, none when left out. */
	limit?: number | null;
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
	/** How long after the user changed the state onStateChange reports it, 400 ms when left out. */
	stateDelay?: number;
	/** More extensions of Tiptap, or the names of those registered; keep the array the same. */
	extensions?: readonly RichTextExtension[];
	/**
	 * Whether the code blocks show in the colors of their language: true with the grammars of the
	 * code editor (lezerHighlighter, which loads each the first time a block needs it), or a
	 * highlighter of the page's own; false when left out. The colors are those of code-editor.css.
	 */
	highlight?: boolean | RichTextHighlighter;
	/**
	 * The languages the menu of the tool code block offers, by their names: those of the
	 * highlighter when left out while highlighting, none otherwise; false for none.
	 */
	codeLanguages?: readonly string[] | false;
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
	/** Whether the user may tick pictures too large to scale down rather than refusing them. */
	uploadScaling?: boolean;
	/** How many images one drop, choice or paste takes at most; none when left out. */
	uploadMaxCount?: number | null;
	/** Whether the form of the image opens the dialog of the uploads rather than its own zone. */
	uploadDialog?: boolean;
	/**
	 * Whether a line under the toolbar tells the user what a paste lost, until the text changes
	 * again; false when left out.
	 */
	pasteNotice?: boolean;
	/** What the editor says, in English when left out. */
	texts?: Partial<RichTextEditorTexts>;
	/** The name a form posts the text under, in a hidden text area; none when left out. */
	name?: string;
	id?: string;
	/** CSS classes of the editor, next to antilia-rich-text-editor. */
	className?: string;
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
	 * Called with what a paste lost to the editor, by name and count, once it is in the text: a
	 * script, a handler of events, a link to an address the editor does not take...
	 */
	onPasteCleaned?(report: RichTextCleaningReport): void;
	/**
	 * Uploads an image the user chose, dropped or pasted, answering the address it is served at
	 * (see uploadImageTo of the core); without it an image is an address the user gives. The
	 * options tell the share sent, which the mark of the image shows, and abort as the user
	 * cancels the upload.
	 */
	onUpload?(file: File, options?: RichTextUploadOptions): Promise<string>;
	/**
	 * Stores images the user uploaded permanently, those ticked as they upload, resolving to the
	 * addresses of those stored (see storePermanentlyAt of the core); without it the uploads offer
	 * no box to tick.
	 */
	onStorePermanently?(urls: string[]): Promise<string[]>;
	/**
	 * Finds the images the page stores permanently for what the user typed, which the tool
	 * storedImage lists and inserts (see findImagesAt of the core); without it the tool does not
	 * show.
	 */
	onFindImages?(query: string, signal: AbortSignal): Promise<readonly RichTextStoredImage[]>;
	/**
	 * Called a moment after the user took images out of the text, or uploaded some that never went
	 * into it, with their addresses, for the page to delete them, at once or once the text is saved
	 * (see deleteImagesAt of the core).
	 */
	onImagesRemoved?(urls: string[]): void;
}

/**
 * What a ref to a {@link RichTextEditor} can do with it.
 */
export interface RichTextEditorHandle {
	/** The editor of the core, null until it is made. */
	readonly editor: CoreRichTextEditor | null;
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
 * The rich text editor of the core, Tiptap, in the theme and the look of the page: a toolbar of the
 * components' own formats the text, Ctrl+S calls onSave; it is resizable by the bar along its
 * bottom unless told otherwise. React renders its element, the core the editor in it. It tells
 * whether the text has changes not saved, with data-modified, which the tabs it is in show. Give
 * it a state to start in, one onStateChange reported, to show it again as the user left it. A ref
 * gets a {@link RichTextEditorHandle}. With highlight, its code blocks show in the colors of their
 * language, which the menu of the tool code block chooses; with onUpload, the user uploads images
 * by the form of the image or its dialog, or by dropping or pasting them into the text, within the
 * limits given, each telling the share sent and canceled by a button; onImagesRemoved hears of
 * those taken out of the text. With onStorePermanently the user ticks the images to store
 * permanently, and with onFindImages inserts those stored by the tool storedImage.
 *
 * The HTML it gives comes from the user: a server keeping it cleans it, as Wicket's
 * RichTextEditor does.
 */
export const RichTextEditor = forwardRef<RichTextEditorHandle, RichTextEditorProps>(
	function RichTextEditor(props, ref) {
		const { value, saved, format, features, featureOptions, headingLevels, colors,
			backgroundColors, toolbar,
			bubbleMenu,
			readOnly, placeholder, label, spellcheck, textLanguage, proofreader, proofreadDelay,
			suggesters, limit, count, resizable, minWidth,
			minHeight, maxWidth, maxHeight, stateDelay, extensions, pasteNotice, texts, highlight,
			codeLanguages, uploadTypes, uploadMaxSize, uploadMaxWidth, uploadMaxHeight, uploadScaling,
			uploadMaxCount, uploadDialog, name, id, className } = props;
		const uploads = props.onUpload !== undefined;
		const keeps = props.onStorePermanently !== undefined;
		const finds = props.onFindImages !== undefined;
		const element = useRef<HTMLDivElement>(null);
		const instance = useRef<CoreRichTextEditor | null>(null);
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
			const editor = new CoreRichTextEditor(element.current!, {
				value: current.value ?? '', saved: current.saved, format: current.format,
				features: current.features, headingLevels: current.headingLevels,
				featureOptions: current.featureOptions,
				colors: current.colors, backgroundColors: current.backgroundColors,
				toolbar: current.toolbar, bubbleMenu: current.bubbleMenu,
				readOnly: current.readOnly,
				placeholder: current.placeholder, label: current.label,
				spellcheck: current.spellcheck, textLanguage: current.textLanguage,
				proofreader: current.proofreader, proofreadDelay: current.proofreadDelay,
				suggesters: current.suggesters, limit: current.limit,
				count: current.count, resizable: current.resizable, minWidth: current.minWidth,
				minHeight: current.minHeight, maxWidth: current.maxWidth, maxHeight: current.maxHeight,
				state: current.state, stateDelay: current.stateDelay, extensions: current.extensions,
				pasteNotice: current.pasteNotice, texts: current.texts, look, highlight: highlighterOf(current.highlight) ?? false,
				codeLanguages: current.codeLanguages, uploadTypes: current.uploadTypes,
				uploadMaxSize: current.uploadMaxSize, uploadMaxWidth: current.uploadMaxWidth,
				uploadMaxHeight: current.uploadMaxHeight, uploadScaling: current.uploadScaling,
				uploadMaxCount: current.uploadMaxCount, uploadDialog: current.uploadDialog,
				onUpload: uploaderOf(latest), onStorePermanently: keeperOf(latest),
				onFindImages: finderOf(latest),
				onImagesRemoved: (urls) => latest.current.onImagesRemoved?.(urls),
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
			instance.current?.configure({ format, features, featureOptions, headingLevels, colors,
				backgroundColors,
				toolbar, bubbleMenu, readOnly, placeholder, label, spellcheck: spellcheck ?? null,
				textLanguage: textLanguage ?? null, proofreader: proofreader ?? null, proofreadDelay,
				suggesters: suggesters ?? [], limit, count, resizable, minWidth, minHeight,
				maxWidth, maxHeight, stateDelay, pasteNotice: !!pasteNotice, texts });
		}, [format, features, featureOptions, headingLevels, colors, backgroundColors, toolbar,
			bubbleMenu,
			readOnly, placeholder, label, spellcheck, textLanguage, proofreader, proofreadDelay,
			suggesters, limit, count, resizable,
			minWidth, minHeight, maxWidth, maxHeight, stateDelay, pasteNotice, texts]);

		useLayoutEffect(() => {
			if (extensions !== undefined) {
				instance.current?.configure({ extensions });
			}
		}, [extensions]);

		useLayoutEffect(() => {
			instance.current?.configure({ highlight: highlighterOf(highlight) ?? false,
				codeLanguages, uploadTypes, uploadMaxSize: uploadMaxSize ?? null,
				uploadMaxWidth: uploadMaxWidth ?? null, uploadMaxHeight: uploadMaxHeight ?? null,
				uploadScaling: !!uploadScaling, uploadMaxCount: uploadMaxCount ?? null,
				uploadDialog: !!uploadDialog, onUpload: uploaderOf(latest),
				onStorePermanently: keeperOf(latest), onFindImages: finderOf(latest) });
		}, [highlight, codeLanguages, uploadTypes, uploadMaxSize, uploadMaxWidth, uploadMaxHeight,
			uploadScaling, uploadMaxCount, uploadDialog, uploads, keeps, finds]);

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
 * @returns the highlighter of a prop highlight: the grammars of the code editor for true
 */
function highlighterOf(highlight: boolean | RichTextHighlighter | undefined):
	RichTextHighlighter | undefined {
	return highlight === true ? lezerHighlighter : highlight || undefined;
}

/**
 * @returns the uploader of the props, calling the latest onUpload; null without one
 */
function uploaderOf(props: { current: RichTextEditorProps }): RichTextUploader | null {
	return props.current.onUpload ? (file, options) => props.current.onUpload!(file, options) :
		null;
}

/**
 * @returns what stores the images permanently, calling the latest onStorePermanently; null
 *          without one
 */
function keeperOf(props: { current: RichTextEditorProps }): RichTextImageKeeper | null {
	return props.current.onStorePermanently ? (urls) => props.current.onStorePermanently!(urls) :
		null;
}

/**
 * @returns what finds the images stored, calling the latest onFindImages; null without one
 */
function finderOf(props: { current: RichTextEditorProps }): RichTextImageFinder | null {
	return props.current.onFindImages ? (query, signal) =>
		props.current.onFindImages!(query, signal) : null;
}
