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
  A modal dialog that behaves and looks like Wicket's ModalDialog with its default theme: the same
  markup, so the same style sheets show it (the core's styles/modal.css, or Wicket's and the
  theme's in a Wicket page), Escape and a click on the overlay closing it, and the focus kept in it
  while it is open and given back when it closes. A dialog rendered in another one, or after it,
  opens over it. Its header holds the title and a close button, drags the dialog around and folds
  it on a double click; canClose may keep it open. It fits its content, or comes in a size or a
  width.
-->
<script lang="ts">
	import type { Snippet } from 'svelte';
	import { ModalDialog, partClasses, type ModalSize } from '@antilia/core';

	import { getLook } from './look.js';

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
		/** The size of the dialog, small, medium or large; it fits its content when left out. */
		size?: ModalSize;
		/** The width of the dialog, a CSS length such as '40em'; it overrides the size's. */
		width?: string;
		/** CSS classes next to dialog-theme-default. */
		class?: string;
		/** The body of the dialog. */
		children?: Snippet;
	};

	let { open, onclose, canClose, title, footer, closeButton = true, closeLabel = 'Close',
		closeOnEscape = true, closeOnClick = false, trapFocus = true, draggable = true,
		foldable = true, size, width, class: className = '', children }: Props = $props();

	const titleId = $props.id();
	const look = getLook();
	let overlay = $state<HTMLDivElement>();

	$effect(() => {
		if (!open || !overlay) {
			return;
		}
		const modal = new ModalDialog(overlay, { closeOnEscape, closeOnClick, trapFocus, draggable,
			foldable, size, width, look, onClose: () => onclose(), canClose: () => canClose?.() ?? true });
		return () => modal.destroy();
	});
</script>

{#if open}
	<div class={[partClasses(look, 'dialog-theme'), className]}>
		<div bind:this={overlay} class={partClasses(look, 'dialog-overlay')}>
			<div class={partClasses(look, 'dialog')} role="dialog" aria-modal="true"
				aria-labelledby={title ? titleId : undefined}>
				<div class={partClasses(look, 'dialog-content')}>
					{#if title || closeButton}
						<div class={partClasses(look, 'dialog-header')}>
							{#if title}<h1 class={partClasses(look, 'dialog-title')} id={titleId}>{title}</h1>{/if}
							{#if closeButton}
								<button type="button" class={partClasses(look, 'dialog-close')} aria-label={closeLabel}
									title={closeLabel}>×</button>
							{/if}
						</div>
					{/if}
					<div class={partClasses(look, 'scrollbar')}>
						<div class={partClasses(look, 'dialog-body')}>{@render children?.()}</div>
					</div>
					{#if footer}<div class={partClasses(look, 'dialog-footer')}>{@render footer()}</div>{/if}
				</div>
			</div>
		</div>
	</div>
{/if}
