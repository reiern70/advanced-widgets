<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  @component
  A div the user resizes by the handle the core adds to its bottom edge, its right edge or its
  bottom right corner: dragged, or moved with the arrow keys; a double click takes it back to its
  first size. Its width and height, in pixels, null for the page's, bind both ways (bind:width,
  bind:height): they follow the user, and resize the div when the page changes them.
-->
<script lang="ts">
	import { untrack, type Snippet } from 'svelte';
	import { Resizable, type ResizableSize, type ResizeAxis } from '@antilia/core';

	import { getLook } from './look.js';

	const look = getLook();

	type Props = {
		/** What the div holds. */
		children?: Snippet;
		/** The dimensions the user changes, both when left out. */
		axis?: ResizeAxis;
		/** The width in pixels, null for the page's, bound. */
		width?: number | null;
		/** The height in pixels, null for the page's, bound. */
		height?: number | null;
		/** The smallest width, in pixels, 32 when left out. */
		minWidth?: number;
		/** The smallest height, in pixels, 32 when left out. */
		minHeight?: number;
		/** The largest width, in pixels, none when left out. */
		maxWidth?: number | null;
		/** The largest height, in pixels, none when left out. */
		maxHeight?: number | null;
		/** How far an arrow key resizes the div, in pixels, 10 when left out. */
		step?: number;
		/** Whether the user can resize the div, true when left out. */
		enabled?: boolean;
		/** The accessible name of the handle. */
		label?: string;
		id?: string;
		/** CSS classes of the div, next to antilia-resizable. */
		class?: string;
		/** Called with the size while the user drags the handle. */
		onresize?: (size: ResizableSize) => void;
		/** Called with the size once the user has resized the div. */
		onresized?: (size: ResizableSize) => void;
	};

	let {
		children,
		axis,
		width = $bindable(null),
		height = $bindable(null),
		minWidth,
		minHeight,
		maxWidth,
		maxHeight,
		step,
		enabled,
		label,
		id,
		class: className = '',
		onresize,
		onresized
	}: Props = $props();

	let element = $state<HTMLDivElement>();
	let instance: Resizable | null = null;

	$effect(() => {
		if (!element) {
			return;
		}
		const made = new Resizable(element, untrack(() => ({
			axis, width, height, minWidth, minHeight, maxWidth, maxHeight, step, enabled, label, look,
			onResize: (size: ResizableSize) => onresize?.(size),
			onResized: (size: ResizableSize) => {
				width = size.width;
				height = size.height;
				onresized?.(size);
			}
		})));
		instance = made;
		return () => {
			made.destroy();
			instance = null;
		};
	});

	$effect(() => {
		const next = { axis, minWidth, minHeight, maxWidth, maxHeight, step, enabled, label };
		untrack(() => instance?.configure(next));
	});

	$effect(() => {
		const next = { width, height };
		untrack(() => {
			const size = instance?.getSize();
			if (instance && size && (size.width !== next.width || size.height !== next.height)) {
				instance.setSize(next);
			}
		});
	});

	// Svelte wrote other classes over the element's: those the core keeps come back
	$effect(() => {
		void className;
		untrack(() => instance?.configure({}));
	});
</script>

<div bind:this={element} {id} class={className || undefined}>
	{@render children?.()}
</div>
