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
  opens over it. Its header holds the title and a close button, drags the dialog around and folds
  it on a double click; canClose may keep it open.
-->
<script lang="ts">
	import type { Snippet } from 'svelte';
	import { MODAL_CLASSES, MODAL_THEME_CLASS, ModalDialog,
		SCROLLBAR_WRAPPER_CLASS } from '@advanced-widgets/core';

	type Props = {
		/** Whether the dialog is open; nothing is rendered while it is closed. */
		open: boolean;
		/**
		 * Called when the user asks to close the dialog, with Escape, a click on the overlay or on
		 * the close button, and canClose lets it.
		 */
		onclose: () => void;
		/**
		 * Asked before the user closes the dialog; returning false keeps it open, when its form
		 * was modified for example. The dialog closes always when left out.
		 */
		canClose?: () => boolean;
		/** The title, in the header of the dialog; none when left out. */
		title?: string;
		/** The footer of the dialog, its buttons on the right; none when left out. */
		footer?: Snippet;
		/** Whether the header shows a button closing the dialog; true by default. */
		closeButton?: boolean;
		/** The label of the close button, for screen readers; "Close" by default. */
		closeLabel?: string;
		/** Whether Escape closes the dialog; true by default. */
		closeOnEscape?: boolean;
		/** Whether a click on the overlay around the dialog closes it; false by default. */
		closeOnClick?: boolean;
		/** Whether the dialog keeps the focus while open; true by default. */
		trapFocus?: boolean;
		/** Whether dragging the header moves the dialog; true by default. */
		draggable?: boolean;
		/** Whether a double click on the header folds the dialog to it, and back; true by default. */
		foldable?: boolean;
		/** CSS classes next to dialog-theme-default. */
		class?: string;
		/** The body of the dialog. */
		children?: Snippet;
	};

	let { open, onclose, canClose, title, footer, closeButton = true, closeLabel = 'Close',
		closeOnEscape = true, closeOnClick = false, trapFocus = true, draggable = true,
		foldable = true, class: className = '', children }: Props = $props();

	const titleId = $props.id();
	let overlay = $state<HTMLDivElement>();

	$effect(() => {
		if (!open || !overlay) {
			return;
		}
		const modal = new ModalDialog(overlay, { closeOnEscape, closeOnClick, trapFocus, draggable,
			foldable, onClose: () => onclose(), canClose: () => canClose?.() ?? true });
		return () => modal.destroy();
	});
</script>

{#if open}
	<div class={[MODAL_THEME_CLASS, className]}>
		<div bind:this={overlay} class={MODAL_CLASSES.overlay}>
			<div class={MODAL_CLASSES.dialog} role="dialog" aria-modal="true"
				aria-labelledby={title ? titleId : undefined}>
				<div class={MODAL_CLASSES.content}>
					{#if title || closeButton}
						<div class={MODAL_CLASSES.header}>
							{#if title}<h1 class={MODAL_CLASSES.title} id={titleId}>{title}</h1>{/if}
							{#if closeButton}
								<button type="button" class={MODAL_CLASSES.close} aria-label={closeLabel}
									title={closeLabel}>×</button>
							{/if}
						</div>
					{/if}
					<div class={SCROLLBAR_WRAPPER_CLASS}>
						<div class={MODAL_CLASSES.body}>{@render children?.()}</div>
					</div>
					{#if footer}<div class={MODAL_CLASSES.footer}>{@render footer()}</div>{/if}
				</div>
			</div>
		</div>
	</div>
{/if}
