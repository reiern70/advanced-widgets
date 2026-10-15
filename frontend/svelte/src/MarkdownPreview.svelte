<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  @component
  A preview of Markdown: the text as a page shows it, with GitHub's tables, task lists and
  strikethrough, its fenced code blocks highlighted by the grammars of the code editor. The core
  renders it, an element at a time, never parsing HTML: HTML in the text shows as text, and links
  and pictures take only web addresses. Give it the text of a CodeEditor as the user types it
  (bind:value on the editor), and a height to scroll.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { MarkdownPreview, partClasses } from '@antilia/core';

	import { getLook } from './look.js';

	const look = getLook();

	type Props = {
		/** The text, in Markdown. */
		value?: string;
		/** The accessible name of the preview, a region of the page. */
		label?: string;
		/** What the preview says while the text is empty, nothing when left out. */
		placeholder?: string;
		id?: string;
		/** CSS classes of the preview, next to antilia-markdown-preview. */
		class?: string;
	};

	let { value = '', label, placeholder, id, class: className = '' }: Props = $props();

	let element = $state<HTMLDivElement>();
	let instance: MarkdownPreview | null = null;

	$effect(() => {
		if (!element) {
			return;
		}
		const made = new MarkdownPreview(element, untrack(() => ({ value, label, placeholder,
			look })));
		instance = made;
		return () => {
			made.destroy();
			instance = null;
		};
	});

	$effect(() => {
		const next = value;
		untrack(() => instance?.setValue(next));
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
