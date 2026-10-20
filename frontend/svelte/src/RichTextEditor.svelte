<!--
  Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)

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
  editor is the bindable editor (bind:editor).

  The HTML it gives comes from the user: a server keeping it cleans it, as Wicket's RichTextEditor
  does.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { RichTextEditor, type HeadingLevel, type ResizableSize, type ResizeAxis,
		type RichTextEditorSelection, type RichTextEditorState, type RichTextEditorStateChange,
		type RichTextEditorTexts, type RichTextExtension, type RichTextFeature, type RichTextFormat,
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
	};

	let {
		value = $bindable(''),
		saved,
		format,
		features,
		headingLevels,
		toolbar,
		bubbleMenu,
		readOnly,
		placeholder,
		label,
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
		texts,
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
		onresize
	}: Props = $props();

	let element = $state<HTMLDivElement>();
	let instance: RichTextEditor | null = null;

	$effect(() => {
		if (!element) {
			return;
		}
		void name;
		const made = new RichTextEditor(element, untrack(() => ({
			value, saved, format, features, headingLevels, toolbar, bubbleMenu, readOnly, placeholder,
			label, limit, count, resizable, minWidth, minHeight, maxWidth, maxHeight,
			state: editorState, stateDelay, extensions, texts, look,
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
			onResize: (next: ResizableSize) => onresize?.(next)
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
		const next = { format, features, headingLevels, toolbar, bubbleMenu, readOnly, placeholder,
			label, limit, count, resizable, minWidth, minHeight, maxWidth, maxHeight, stateDelay,
			texts };
		untrack(() => instance?.configure(next));
	});

	$effect(() => {
		const next = extensions;
		if (next !== undefined) {
			untrack(() => instance?.configure({ extensions: next }));
		}
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
