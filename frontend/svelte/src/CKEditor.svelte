<!--
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<!--
  @component
  CKEditor 5 through the core, with the contract of the RichTextEditor: CKEditor's own toolbar,
  balloons and dialogs, in the theme and the look of the page; Ctrl+S calls onsave; it is resizable
  by the bar along its bottom unless told otherwise. Svelte renders its element, the core CKEditor
  in it, once CKEditor started (onready); should it not, onerror tells why, as it tells of an error
  escaping CKEditor after it started, which CKEditor starts again after (recovery). The text binds both
  ways (bind:value): it follows the user, and the editor shows the text the page gives it. The
  state, the selection, the scroll position and the size, binds too (bind:state): the editor
  starts in the state given and reports the one the user leaves it in, a moment after; and whether
  the text has changes not saved (bind:modified), which the tabs it is in show. The core's editor
  is the bindable editor (bind:editor). What a paste lost to CKEditor, a script or a link to
  javascript: for example, comes to onpastecleaned and, with pasteNotice, shows in a line under the
  toolbar until the text changes.

  Its uploads are those of the RichTextEditor: images uploaded by onupload within the limits
  given, scaled down when the user ticks them, in the dialog of the uploads too; the tool images
  lists them, and onimagesremoved hears of those taken out of the text. With onstorepermanently
  the user ticks the images to store permanently, and with onfindimages inserts those stored by
  the tool storedImage; with onattach the user attaches files. Their lists, dialogs and files take
  the look of rich-text-editor.css, which the page loads too.

  The package has no code of CKEditor, which is licensed under the GPL or CKSource's commercial
  terms: the page brings it, its classes (build) or the global CKEDITOR of its browser build, with
  its license key and its style sheet, ckeditor5.css.

  The HTML it gives comes from the user: a server keeping it cleans it, as Wicket's CKEditor does.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { CKEditor, type CKEditorBuild, type CKEditorFailure, type CKEditorInstance,
		type CKEditorPlugin, type CKEditorRecovery, type RichTextUploadOptions,
		type RichTextAttachmentType, type RichTextAttachmentUrls, type RichTextStoredImage,
		type EditorEvents, type RichTextEditorEventKey,
		type CKEditorTexts, type HeadingLevel, type ResizableSize, type ResizeAxis,
		type RichTextCleaningReport, type RichTextEditorSelection, type RichTextEditorState,
		type RichTextEditorStateChange, type RichTextFeature } from '@antilia/core';

	import { getLook } from './look.js';

	const look = getLook();

	type Props = {
		/**
		 * The classes of CKEditor: its ClassicEditor and plugins, imported from the ckeditor5
		 * package by the page; the global CKEDITOR of its browser build when left out. Another
		 * makes CKEditor anew.
		 */
		build?: CKEditorBuild;
		/** The license key: 'GPL' for the open-source distribution, or the key CKSource gave. */
		licenseKey?: string;
		/** The language of CKEditor's texts, en when left out; its translations must be there. */
		language?: string;
		/** CKEditor's translations, imported from ckeditor5/translations. */
		translations?: readonly unknown[];
		/** The text, bound both ways. */
		value?: string;
		/** The text last saved, against which the editor tells of changes; the first text when left out. */
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
		 * The tools of the toolbar, as CKEditor names them, '|' between groups: those of the
		 * features when left out; false for none.
		 */
		toolbar?: readonly string[] | false;
		/** More plugins of CKEditor: their classes, or their names in the build. */
		plugins?: readonly CKEditorPlugin[];
		/** More of CKEditor's configuration, over the editor's own. */
		config?: Readonly<Record<string, unknown>>;
		/** Whether the user cannot change the text, false when left out. */
		readOnly?: boolean;
		/** The text shown while the editor is empty. */
		placeholder?: string;
		/** The accessible name of the text. */
		label?: string;
		/**
		 * Whether the browser checks the spelling of the text: as it does by default when left out
		 * or null.
		 */
		spellcheck?: boolean | null;
		/**
		 * The language of the text, a tag of BCP 47 (en-GB, de), which the browser checks its
		 * spelling in: that of the page when left out or null.
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
		/** The state: the one the editor starts in, then the one the user leaves it in, bound. */
		state?: RichTextEditorState;
		/**
		 * The handlers of the editor's events, by their keys (RICH_TEXT_EDITOR_EVENTS) or the names of
		 * other events it fires, each sending the state or not (send); those left out keep theirs:
		 * change, select, scroll and resize send the state 400 ms after the user paused, blur and
		 * leave at once; an empty list for none. Keep the object the same: another one puts the
		 * handlers anew.
		 */
		events?: EditorEvents<CKEditor, RichTextEditorEventKey>;
		/** Whether the text has changes not saved, bound. */
		modified?: boolean;
		/** The editor of the core, bound. */
		editor?: CKEditor | null;
		/**
		 * Whether a line under the toolbar tells the user what a paste lost, until the text changes
		 * again; false when left out.
		 */
		pasteNotice?: boolean;
		/**
		 * How the editor starts CKEditor again after an error escaped it, as CKEditor's Watchdog
		 * did: after each of 3 crashes, then while they come more than 5 s apart on average, when
		 * left out; false to leave CKEditor running as it is.
		 */
		recovery?: CKEditorRecovery | false;
		/** The types of the images the user may upload: PNG, JPEG, GIF, WebP and AVIF when left out. */
		uploadTypes?: readonly string[];
		/** The largest image the user may upload, in bytes; none when left out. */
		uploadMaxSize?: number | null;
		/**
		 * The widest and the highest picture the user may upload, in pixels, read from the header
		 * of the file; none when left out. Both, or none.
		 */
		uploadMaxWidth?: number | null;
		uploadMaxHeight?: number | null;
		/**
		 * Whether a dialog offers the user to scale down the pictures larger than that, each by a
		 * box to tick, rather than refusing them; false when left out.
		 */
		uploadScaling?: boolean;
		/** How many images one drop, choice or paste takes at most, the first; none when left out. */
		uploadMaxCount?: number | null;
		/**
		 * Whether the images are uploaded in the dialog of the uploads, the tool uploadImages,
		 * rather than by CKEditor's own button; false when left out.
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
		 * The addresses of the files attached to a text in Markdown, or a test of an address: a
		 * link to one of them comes back as a file attached; none when left out.
		 */
		attachmentUrls?: RichTextAttachmentUrls | null;
		/** What the editor says of its own, in English when left out: CKEditor translates the rest. */
		texts?: Partial<CKEditorTexts>;
		/** The name a form posts the text under, in a hidden text area; none when left out. */
		name?: string;
		id?: string;
		/** CSS classes of the editor, next to antilia-ck-editor. */
		class?: string;
		/**
		 * Uploads an image the user chose, pasted or dropped, answering the address it is served
		 * at, or rejecting with the message saying why not: see uploadImageTo of the core. With
		 * it, the feature image has CKEditor's upload button, and takes images pasted and dropped;
		 * the latest is called. One coming or going makes CKEditor anew.
		 */
		onupload?: (file: File, options?: RichTextUploadOptions) => Promise<string>;
		/**
		 * Stores the images ticked in the dialog of the uploads permanently, resolving to the
		 * addresses of those stored (storePermanentlyAt of the core); the latest is called. With
		 * it, the tool uploadImages opens the dialog, whose lines have the box to tick.
		 */
		onstorepermanently?: (urls: string[]) => Promise<string[]>;
		/**
		 * Finds the images stored permanently for what the user typed, which the tool storedImage,
		 * "Insert image from store", inserts (findImagesAt of the core); the latest is called.
		 */
		onfindimages?: (query: string, signal: AbortSignal) =>
			Promise<readonly RichTextStoredImage[]>;
		/** Called a moment after the user took images out of the text, with their addresses. */
		onimagesremoved?: (urls: string[]) => void;
		/**
		 * Uploads a file the user attaches, resolving to its address (uploadFileTo of the core):
		 * the tools attachment and attachments show, for the feature attachment; the latest is
		 * called. One coming or going makes CKEditor anew.
		 */
		onattach?: (file: File, options?: RichTextUploadOptions) => Promise<string>;
		/** Called a moment after the user took files attached out of the text, with their addresses. */
		onattachmentsremoved?: (urls: string[]) => void;
		onready?: (editor: CKEditorInstance) => void;
		/**
		 * Called with what went wrong when CKEditor could not start, or ran into an error after it
		 * started, and what the editor does about it.
		 */
		onerror?: (error: unknown, failure: CKEditorFailure) => void;
		onchange?: (value: string) => void;
		onfocus?: () => void;
		onblur?: (value: string) => void;
		onselectionchange?: (selection: RichTextEditorSelection) => void;
		onstatechange?: (state: RichTextEditorState, change: RichTextEditorStateChange) => void;
		onmodifiedchange?: (modified: boolean) => void;
		onsave?: (value: string) => void;
		onresize?: (size: ResizableSize) => void;
		/** Called with what a paste lost to CKEditor, by name and count, once it is in the text. */
		onpastecleaned?: (report: RichTextCleaningReport) => void;
	};

	let {
		build,
		licenseKey,
		language,
		translations,
		value = $bindable(''),
		saved,
		format,
		features,
		headingLevels,
		toolbar,
		plugins,
		config,
		readOnly,
		placeholder,
		label,
		spellcheck,
		textLanguage,
		count,
		resizable,
		minWidth,
		minHeight,
		maxWidth,
		maxHeight,
		// not state, which would hide the rune $state
		state: editorState = $bindable(),
		events,
		modified = $bindable(false),
		editor = $bindable(null),
		pasteNotice,
		recovery,
		uploadTypes,
		uploadMaxSize,
		uploadMaxWidth,
		uploadMaxHeight,
		uploadScaling,
		uploadMaxCount,
		uploadDialog,
		attachTypes,
		attachMaxSize,
		attachMaxCount,
		attachDialog,
		attachmentUrls,
		texts,
		name,
		id,
		class: className = '',
		onupload,
		onstorepermanently,
		onfindimages,
		onimagesremoved,
		onattach,
		onattachmentsremoved,
		onready,
		onerror,
		onchange,
		onfocus,
		onblur,
		onselectionchange,
		onstatechange,
		onmodifiedchange,
		onsave,
		onresize,
		onpastecleaned
	}: Props = $props();

	/** Uploads by the latest onupload, null without one. */
	const uploader = () => onupload ? (file: File, options?: RichTextUploadOptions) =>
		onupload!(file, options) : null;

	/** Attaches files by the latest onattach, null without one. */
	const attacher = () => onattach ? (file: File, options?: RichTextUploadOptions) =>
		onattach!(file, options) : null;

	/** Stores permanently by the latest onstorepermanently, null without one. */
	const keeper = () => onstorepermanently ? (urls: string[]) => onstorepermanently!(urls) :
		null;

	/** Finds the images stored by the latest onfindimages, null without one. */
	const finder = () => onfindimages ? (query: string, signal: AbortSignal) =>
		onfindimages!(query, signal) : null;

	let element = $state<HTMLDivElement>();
	let instance: CKEditor | null = null;

	/** The name, by its value: another makes the editor anew, the same does not. */
	const formName = $derived(name);

	$effect(() => {
		if (!element) {
			return;
		}
		void formName;
		const made = new CKEditor(element, untrack(() => ({
			build, licenseKey, language, translations, value, saved, format, features, headingLevels,
			toolbar, plugins, config, readOnly, placeholder, label, spellcheck, textLanguage, count,
			resizable, minWidth, minHeight, maxWidth, maxHeight, state: editorState, events, pasteNotice,
			recovery, texts, look, uploadTypes, uploadMaxSize, uploadMaxWidth, uploadMaxHeight,
			uploadScaling, uploadMaxCount, uploadDialog, attachTypes, attachMaxSize, attachMaxCount,
			attachDialog, attachmentUrls, onUpload: uploader(), onStorePermanently: keeper(),
			onFindImages: finder(), onAttach: attacher(),
			onImagesRemoved: (urls: string[]) => onimagesremoved?.(urls),
			onAttachmentsRemoved: (urls: string[]) => onattachmentsremoved?.(urls),
			onReady: (next: CKEditorInstance) => onready?.(next),
			onError: (error: unknown, failure: CKEditorFailure) => onerror?.(error, failure),
			onChange: (next: string) => {
				value = next;
				onchange?.(next);
			},
			onFocus: () => onfocus?.(),
			onBlur: (next: string) => onblur?.(next),
			onSelectionChange: (next: RichTextEditorSelection) => onselectionchange?.(next),
			onStateChange: (next: RichTextEditorState, change: RichTextEditorStateChange) => {
				editorState = next;
				onstatechange?.(next, change);
			},
			onModifiedChange: (next: boolean) => {
				modified = next;
				onmodifiedchange?.(next);
			},
			onSave: (next: string) => onsave?.(next),
			onResize: (next: ResizableSize) => onresize?.(next),
			onPasteCleaned: (report: RichTextCleaningReport) => onpastecleaned?.(report)
		})));
		instance = made;
		editor = made;
		untrack(() => {
			modified = made.isModified();
		});
		return () => {
			made.destroy();
			instance = null;
			editor = null;
		};
	});

	$effect(() => {
		const next = { licenseKey, language, format, features, headingLevels, toolbar, readOnly,
			placeholder, label, spellcheck: spellcheck ?? null, textLanguage: textLanguage ?? null,
			count, resizable, minWidth, minHeight, maxWidth, maxHeight,
			pasteNotice: !!pasteNotice, recovery, texts };
		untrack(() => instance?.configure(next));
	});

	$effect(() => {
		const next = events;
		if (next !== undefined) {
			untrack(() => instance?.configure({ events: next }));
		}
	});

	$effect(() => {
		const next = { uploadTypes, uploadMaxSize: uploadMaxSize ?? null,
			uploadMaxWidth: uploadMaxWidth ?? null, uploadMaxHeight: uploadMaxHeight ?? null,
			uploadScaling: !!uploadScaling, uploadMaxCount: uploadMaxCount ?? null,
			uploadDialog: !!uploadDialog, onUpload: uploader(), onStorePermanently: keeper(),
			onFindImages: finder(), attachTypes, attachMaxSize: attachMaxSize ?? null,
			attachMaxCount: attachMaxCount ?? null, attachDialog: !!attachDialog,
			attachmentUrls: attachmentUrls ?? null, onAttach: attacher() };
		untrack(() => instance?.configure(next));
	});

	// compared by what they are: another one makes CKEditor anew
	$effect(() => {
		const next = { build, plugins, config, translations };
		untrack(() => instance?.configure(next));
	});

	$effect(() => {
		const next = value;
		untrack(() => {
			if (instance && next !== instance.getValue()) {
				instance.setValue(next ?? '');
				modified = instance.isModified();
			}
		});
	});

	$effect(() => {
		const next = saved;
		if (next !== undefined) {
			untrack(() => {
				instance?.markSaved(next);
				modified = instance?.isModified() ?? false;
			});
		}
	});

	// Svelte wrote other classes over the element's: those the core keeps come back
	$effect(() => {
		void className;
		untrack(() => instance?.configure({}));
	});
</script>

<div bind:this={element} {id} class={className || undefined}>
	{#if name !== undefined}<textarea {name}>{value}</textarea>{/if}
</div>
