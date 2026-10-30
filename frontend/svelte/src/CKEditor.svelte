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
  in it, once CKEditor started (onready); should it not, onerror tells why. The text binds both
  ways (bind:value): it follows the user, and the editor shows the text the page gives it. The
  state, the selection, the scroll position and the size, binds too (bind:state): the editor
  starts in the state given and reports the one the user leaves it in, a moment after; and whether
  the text has changes not saved (bind:modified), which the tabs it is in show. The core's editor
  is the bindable editor (bind:editor). What a paste lost to CKEditor, a script or a link to
  javascript: for example, comes to onpastecleaned and, with pasteNotice, shows in a line under the
  toolbar until the text changes.

  The package has no code of CKEditor, which is licensed under the GPL or CKSource's commercial
  terms: the page brings it, its classes (build) or the global CKEDITOR of its browser build, with
  its license key and its style sheet, ckeditor5.css.

  The HTML it gives comes from the user: a server keeping it cleans it, as Wicket's CKEditor does.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { CKEditor, type CKEditorBuild, type CKEditorInstance, type CKEditorPlugin,
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
		/** What the text may hold: all the features when left out (for Markdown, all it has a syntax for). */
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
		/** How long after the user changed the state it is reported, 400 ms when left out. */
		stateDelay?: number;
		/** Whether the text has changes not saved, bound. */
		modified?: boolean;
		/** The editor of the core, bound. */
		editor?: CKEditor | null;
		/**
		 * Whether a line under the toolbar tells the user what a paste lost, until the text changes
		 * again; false when left out.
		 */
		pasteNotice?: boolean;
		/** What the editor says of its own, in English when left out: CKEditor translates the rest. */
		texts?: Partial<CKEditorTexts>;
		/** The name a form posts the text under, in a hidden text area; none when left out. */
		name?: string;
		id?: string;
		/** CSS classes of the editor, next to antilia-ck-editor. */
		class?: string;
		onready?: (editor: CKEditorInstance) => void;
		onerror?: (error: unknown) => void;
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
		stateDelay,
		modified = $bindable(false),
		editor = $bindable(null),
		pasteNotice,
		texts,
		name,
		id,
		class: className = '',
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
			resizable, minWidth, minHeight, maxWidth, maxHeight, state: editorState, stateDelay, pasteNotice,
			texts, look,
			onReady: (next: CKEditorInstance) => onready?.(next),
			onError: (error: unknown) => onerror?.(error),
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
			count, resizable, minWidth, minHeight, maxWidth, maxHeight, stateDelay,
			pasteNotice: !!pasteNotice, texts };
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
