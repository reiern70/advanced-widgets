<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  @component
  A modal dialog that behaves and looks like Wicket's ModalDialog with its default theme: the same
  markup, so the same style sheets show it (the core's styles/modal.css, or Wicket's and the
  theme's in a Wicket page), Escape and a click on the overlay closing it, and the focus kept in it
  while it is open and given back when it closes. A dialog rendered in another one, or after it,
  opens over it.
-->
<script lang="ts">
	import type { Snippet } from 'svelte';
	import { MODAL_CLASSES, MODAL_THEME_CLASS, ModalDialog } from '@advanced-widgets/core';

	type Props = {
		/** Whether the dialog is open; nothing is rendered while it is closed. */
		open: boolean;
		/** Called when the user asks to close the dialog: Escape, or a click on the overlay. */
		onclose: () => void;
		/** The title, in the header of the dialog; none when left out. */
		title?: string;
		/** The footer of the dialog, its buttons for example; none when left out. */
		footer?: Snippet;
		/** Whether Escape closes the dialog; true by default. */
		closeOnEscape?: boolean;
		/** Whether a click on the overlay around the dialog closes it; false by default. */
		closeOnClick?: boolean;
		/** Whether the dialog keeps the focus while open; true by default. */
		trapFocus?: boolean;
		/** CSS classes next to dialog-theme-default. */
		class?: string;
		/** The body of the dialog. */
		children?: Snippet;
	};

	let { open, onclose, title, footer, closeOnEscape = true, closeOnClick = false,
		trapFocus = true, class: className = '', children }: Props = $props();

	const titleId = $props.id();
	let overlay = $state<HTMLDivElement>();

	$effect(() => {
		if (!open || !overlay) {
			return;
		}
		const modal = new ModalDialog(overlay, { closeOnEscape, closeOnClick, trapFocus,
			onClose: () => onclose() });
		return () => modal.destroy();
	});
</script>

{#if open}
	<div class={[MODAL_THEME_CLASS, className]}>
		<div bind:this={overlay} class={MODAL_CLASSES.overlay}>
			<div class={MODAL_CLASSES.dialog} role="dialog" aria-modal="true"
				aria-labelledby={title ? titleId : undefined}>
				<div class={MODAL_CLASSES.content}>
					{#if title}<h1 class={MODAL_CLASSES.header} id={titleId}>{title}</h1>{/if}
					<div class={MODAL_CLASSES.body}>{@render children?.()}</div>
					{#if footer}<div class={MODAL_CLASSES.footer}>{@render footer()}</div>{/if}
				</div>
			</div>
		</div>
	</div>
{/if}
