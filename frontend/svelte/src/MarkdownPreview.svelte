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
  A preview of Markdown: the text as a page shows it, with GitHub's tables, task lists and
  strikethrough, its fenced code blocks highlighted by the grammars of the code editor. The core
  renders it, an element at a time, never parsing HTML: HTML in the text shows as text, and links
  and pictures take only web addresses. Give it the text of a CodeEditor as the user types it
  (bind:value on the editor), or the editor's id as editor, and a height to scroll; with
  scrollWithEditor too, it keeps in step with the editor.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { MarkdownPreview, partClasses } from '@antilia/core';

	import { getLook } from './look.js';

	const look = getLook();

	type Props = {
		/** The text, in Markdown; that of the editor when left out and it follows one. */
		value?: string;
		/**
		 * A code editor the preview follows: its element, or the id of the element, a CodeEditor's
		 * id. The preview shows its text as it changes, and can keep in step with it.
		 */
		editor?: HTMLElement | string | null;
		/**
		 * Whether the preview keeps in step with its editor: as the user moves the cursor, types or
		 * scrolls the editor, the preview scrolls to the part matching the place in the editor.
		 * False when left out.
		 */
		scrollWithEditor?: boolean;
		/** The accessible name of the preview, a region of the page. */
		label?: string;
		/** What the preview says while the text is empty, nothing when left out. */
		placeholder?: string;
		id?: string;
		/** CSS classes of the preview, next to antilia-markdown-preview. */
		class?: string;
	};

	let { value, editor, scrollWithEditor = false, label, placeholder, id,
		class: className = '' }: Props = $props();

	let element = $state<HTMLDivElement>();
	let instance: MarkdownPreview | null = null;

	$effect(() => {
		if (!element) {
			return;
		}
		const made = new MarkdownPreview(element, untrack(() => ({ value, editor, scrollWithEditor,
			label, placeholder, look })));
		instance = made;
		return () => {
			made.destroy();
			instance = null;
		};
	});

	$effect(() => {
		const next = value;
		untrack(() => {
			if (next !== undefined) {
				instance?.setValue(next);
			}
		});
	});

	$effect(() => {
		const next = { editor: editor ?? null, scrollWithEditor };
		untrack(() => {
			if (instance && ((instance.options.editor ?? null) !== next.editor ||
				!!instance.options.scrollWithEditor !== next.scrollWithEditor)) {
				instance.configure(next);
			}
		});
	});

	$effect(() => {
		const next = { label: label ?? '', placeholder: placeholder ?? '' };
		untrack(() => {
			if (instance && ((instance.options.label ?? '') !== next.label ||
				(instance.options.placeholder ?? '') !== next.placeholder)) {
				instance.configure(next);
			}
		});
	});

	// the classes and the attributes the core gives the element, so that Svelte keeps them; the
	// core makes it take the focus, so that the keyboard scrolls it
	const classes = $derived([partClasses(look, 'markdown-preview'), className].filter(Boolean)
		.join(' '));
</script>

<div bind:this={element} {id} class={classes} role={label ? 'region' : undefined}
	aria-label={label || undefined}></div>
