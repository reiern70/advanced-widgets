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
  The code editor of the core, CodeMirror, in the theme and the look of the page: the grammar of a
  language highlights the text, Ctrl+F searches, Ctrl+S calls onsave; it is resizable by the bar
  along its bottom unless told otherwise. Svelte renders its element, the core the editor in it.
  The text binds both ways (bind:value): it follows the user, and the editor shows the text the
  page gives it. The state, the selection, the scroll position and the size, binds too
  (bind:state): the editor starts in the state given and reports the one the user leaves it in, a
  moment after; and whether the text has changes not saved (bind:modified), which the tabs it is
  in show. The core's editor is the bindable editor (bind:editor).
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { CodeEditor, type CodeEditorLanguage, type CodeEditorSelection, type CodeEditorState,
		type CodeEditorStateChange, type CodeEditorTexts, type Extension, type ResizableSize,
		type ResizeAxis } from '@antilia/core';

	import { getLook } from './look.js';

	const look = getLook();

	type Props = {
		/** The text, bound both ways. */
		value?: string;
		/** The text last saved, against which the editor tells of changes; the first text when left out. */
		saved?: string;
		/** The grammar: the name of a language the editor knows or one registered, or an extension. */
		language?: CodeEditorLanguage | null;
		/** Whether the lines show their numbers, true when left out. */
		lineNumbers?: boolean;
		/** Whether blocks fold, true when left out. */
		folding?: boolean;
		/** Whether the user cannot change the text, false when left out. */
		readOnly?: boolean;
		/** Whether the user can put the cursor in the text and select it, true when left out. */
		selectable?: boolean;
		/** Whether long lines wrap, false when left out. */
		lineWrapping?: boolean;
		/** The width of a tab and of a level of indentation, in spaces: 4 when left out. */
		tabSize?: number;
		/** Whether Tab indents, true when left out. */
		indentWithTab?: boolean;
		/** The text shown while the editor is empty. */
		placeholder?: string;
		/** The accessible name of the text. */
		label?: string;
		/** The dimensions the user resizes the editor in, vertical when left out; false for none. */
		resizable?: ResizeAxis | false;
		minWidth?: number;
		minHeight?: number;
		maxWidth?: number | null;
		maxHeight?: number | null;
		/** The state: the one the editor starts in, then the one the user leaves it in, bound. */
		state?: CodeEditorState;
		/** How long after the user changed the state it is reported, 400 ms when left out. */
		stateDelay?: number;
		/** Whether the text has changes not saved, bound. */
		modified?: boolean;
		/** The editor of the core, bound. */
		editor?: CodeEditor | null;
		/** More CodeMirror extensions. */
		extensions?: Extension[];
		/** What the editor says, in English when left out. */
		texts?: Partial<CodeEditorTexts>;
		/** The name a form posts the text under, in a hidden text area; none when left out. */
		name?: string;
		id?: string;
		/** CSS classes of the editor, next to antilia-code-editor. */
		class?: string;
		onchange?: (value: string) => void;
		onfocus?: () => void;
		onblur?: (value: string) => void;
		onselectionchange?: (selection: CodeEditorSelection) => void;
		onstatechange?: (state: CodeEditorState, change: CodeEditorStateChange) => void;
		onmodifiedchange?: (modified: boolean) => void;
		onsave?: (value: string) => void;
		onresize?: (size: ResizableSize) => void;
	};

	let {
		value = $bindable(''),
		saved,
		language,
		lineNumbers,
		folding,
		readOnly,
		selectable,
		lineWrapping,
		tabSize,
		indentWithTab,
		placeholder,
		label,
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
	let instance: CodeEditor | null = null;

	$effect(() => {
		if (!element) {
			return;
		}
		void name;
		const made = new CodeEditor(element, untrack(() => ({
			value, saved, language, lineNumbers, folding, readOnly, selectable, lineWrapping, tabSize,
			indentWithTab, placeholder, label, resizable, minWidth, minHeight, maxWidth, maxHeight,
			state: editorState, stateDelay, extensions, texts, look,
			onChange: (next: string) => {
				value = next;
				onchange?.(next);
			},
			onFocus: () => onfocus?.(),
			onBlur: (next: string) => onblur?.(next),
			onSelectionChange: (next: CodeEditorSelection) => onselectionchange?.(next),
			onStateChange: (next: CodeEditorState, change: CodeEditorStateChange) => {
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
		const next = { language: language ?? null, lineNumbers, folding, readOnly, selectable,
			lineWrapping, tabSize, indentWithTab, placeholder, label, resizable, minWidth, minHeight,
			maxWidth, maxHeight, stateDelay, texts };
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
