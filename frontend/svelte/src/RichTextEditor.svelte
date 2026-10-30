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
  The rich text editor of the core, Tiptap, in the theme and the look of the page: a toolbar of the
  components' own formats the text, Ctrl+S calls onsave; it is resizable by the bar along its
  bottom unless told otherwise. Svelte renders its element, the core the editor in it. The text
  binds both ways (bind:value): it follows the user, and the editor shows the text the page gives
  it. The state, the selection, the scroll position and the size, binds too (bind:state): the
  editor starts in the state given and reports the one the user leaves it in, a moment after; and
  whether the text has changes not saved (bind:modified), which the tabs it is in show. The core's
  editor is the bindable editor (bind:editor). With highlight, its code blocks show in the colors of
  their language, which the menu of the tool code block chooses; with onupload, the user uploads
  images by the form of the image, or by dropping or pasting them into the text. What a paste lost
  to the editor, a script or a link to javascript: for example, comes to onpastecleaned and, with
  pasteNotice, shows in a line under the toolbar until the text changes.

  The HTML it gives comes from the user: a server keeping it cleans it, as Wicket's RichTextEditor
  does.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { lezerHighlighter, RichTextEditor, type HeadingLevel, type ResizableSize,
		type ResizeAxis, type RichTextBackgroundColor, type RichTextCleaningReport, type RichTextColor,
		type RichTextEditorSelection, type RichTextEditorState,
		type RichTextEditorStateChange, type RichTextEditorTexts, type RichTextExtension,
		type RichTextFeature, type RichTextFormat, type RichTextHighlighter,
		type RichTextProofreader, type RichTextSuggester,
		type RichTextTool } from '@antilia/core';

	import { getLook } from './look.js';

	const look = getLook();

	type Props = {
		/** The text, bound both ways. */
		value?: string;
		/** The text last saved, against which the editor tells of changes; the first text when left out. */
		saved?: string;
		/** How the editor keeps the text: html when left out, markdown or json. */
		format?: RichTextFormat;
		/** What the text may hold: all the features when left out (for Markdown, all it has a syntax for). */
		features?: readonly RichTextFeature[];
		/** The levels of the headings, 1 to 4 when left out. */
		headingLevels?: readonly HeadingLevel[];
		/**
		 * The palette of the colors of the text, in the order of its menu: DEFAULT_RICH_TEXT_COLORS
		 * when left out; null or empty for none, without the tool. The page's style sheets color
		 * the classes of colors of its own; Wicket's RichTextEditor.colors() gives a palette with
		 * its labels translated.
		 */
		colors?: readonly RichTextColor[] | null;
		/**
		 * The palette of the colors of the background, apart from those of the text:
		 * DEFAULT_RICH_TEXT_BACKGROUND_COLORS when left out; null or empty for none, without the
		 * tool. Wicket's RichTextEditor.backgroundColors() gives one with its labels translated.
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
		 * Whether the browser checks the spelling of the text: as it does by default when left out
		 * or null.
		 */
		spellcheck?: boolean | null;
		/**
		 * The language of the text, a tag of BCP 47 (en-GB, de), which the browser checks its
		 * spelling in: that of the page when left out or null.
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
		/** The state: the one the editor starts in, then the one the user leaves it in, bound. */
		state?: RichTextEditorState;
		/** How long after the user changed the state it is reported, 400 ms when left out. */
		stateDelay?: number;
		/** Whether the text has changes not saved, bound. */
		modified?: boolean;
		/** The editor of the core, bound. */
		editor?: RichTextEditor | null;
		/** More extensions of Tiptap, or the names of those registered. */
		extensions?: readonly RichTextExtension[];
		/**
		 * Whether the code blocks show in the colors of their language: true with the grammars of
		 * the code editor (lezerHighlighter), or a highlighter of the page's own; false when left
		 * out. The colors are those of code-editor.css.
		 */
		highlight?: boolean | RichTextHighlighter;
		/** The languages the menu of the tool code block offers: those of the highlighter when left out. */
		codeLanguages?: readonly string[] | false;
		/** The types of the images the user may upload: PNG, JPEG, GIF and WebP when left out. */
		uploadTypes?: readonly string[];
		/** The largest image the user may upload, in bytes; none when left out. */
		uploadMaxSize?: number | null;
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
		class?: string;
		onchange?: (value: string) => void;
		onfocus?: () => void;
		onblur?: (value: string) => void;
		onselectionchange?: (selection: RichTextEditorSelection) => void;
		onstatechange?: (state: RichTextEditorState, change: RichTextEditorStateChange) => void;
		onmodifiedchange?: (modified: boolean) => void;
		onsave?: (value: string) => void;
		onresize?: (size: ResizableSize) => void;
		/** Called with what a paste lost to the editor, by name and count, once it is in the text. */
		onpastecleaned?: (report: RichTextCleaningReport) => void;
		/** Uploads an image, answering the address it is served at: see uploadImageTo of the core. */
		onupload?: (file: File) => Promise<string>;
	};

	let {
		value = $bindable(''),
		saved,
		format,
		features,
		headingLevels,
		colors,
		backgroundColors,
		featureOptions,
		toolbar,
		bubbleMenu,
		readOnly,
		placeholder,
		label,
		spellcheck,
		textLanguage,
		proofreader,
		proofreadDelay,
		suggesters,
		limit,
		count,
		resizable,
		minWidth,
		minHeight,
		maxWidth,
		maxHeight,
		// not state, which would hide the rune $state
		state: editorState = $bindable(),
		stateDelay,
		modified = $bindable(false),
		editor = $bindable(null),
		extensions,
		pasteNotice,
		texts,
		highlight,
		codeLanguages,
		uploadTypes,
		uploadMaxSize,
		name,
		id,
		class: className = '',
		onchange,
		onfocus,
		onblur,
		onselectionchange,
		onstatechange,
		onmodifiedchange,
		onsave,
		onresize,
		onpastecleaned,
		onupload
	}: Props = $props();

	/** The highlighter of the prop highlight: the grammars of the code editor for true. */
	const highlighterOf = (given: boolean | RichTextHighlighter | undefined) =>
		given === true ? lezerHighlighter : given || false;

	/** Uploads by the latest onupload, null without one. */
	const uploader = () => onupload ? (file: File) => onupload!(file) : null;

	let element = $state<HTMLDivElement>();
	let instance: RichTextEditor | null = null;

	$effect(() => {
		if (!element) {
			return;
		}
		void name;
		const made = new RichTextEditor(element, untrack(() => ({
			value, saved, format, features, featureOptions, headingLevels, colors, backgroundColors,
			toolbar,
			bubbleMenu, readOnly, placeholder, label, spellcheck, textLanguage, proofreader,
			proofreadDelay, suggesters, limit, count,
			resizable, minWidth, minHeight, maxWidth, maxHeight, state: editorState, stateDelay, extensions, pasteNotice, texts, look, highlight: highlighterOf(highlight),
			codeLanguages, uploadTypes, uploadMaxSize, onUpload: uploader(),
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
		const next = { format, features, featureOptions, headingLevels, colors, backgroundColors,
			toolbar,
			bubbleMenu, readOnly,
			placeholder, label, spellcheck: spellcheck ?? null, textLanguage: textLanguage ?? null,
			proofreader: proofreader ?? null, proofreadDelay, suggesters: suggesters ?? [],
			limit, count, resizable, minWidth, minHeight, maxWidth, maxHeight, stateDelay,
			pasteNotice: !!pasteNotice, texts };
		untrack(() => instance?.configure(next));
	});

	$effect(() => {
		const next = extensions;
		if (next !== undefined) {
			untrack(() => instance?.configure({ extensions: next }));
		}
	});

	$effect(() => {
		const next = { highlight: highlighterOf(highlight), codeLanguages, uploadTypes,
			uploadMaxSize: uploadMaxSize ?? null, onUpload: uploader() };
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
