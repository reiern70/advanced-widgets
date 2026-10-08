<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  @component
  A div scrolling with the slim scroll bars of the core, which look the same in every browser and
  take the theme's colors. Give it a height, or a maximum height, and an overflow in its CSS.
  Bound with bind:this, the component scrolls with scrollToTop(), scrollToBottom(),
  scrollTo({top, left}) and scrollToElement(elementOrSelector), smoothly if their last argument is
  true.
-->
<script lang="ts">
	import type { Snippet } from 'svelte';
	import type { HTMLAttributes } from 'svelte/elements';
	import { Scrollbar, scrollbar, type ScrollbarOptions,
		type ScrollPosition } from '@advanced-widgets/core';

	type Props = HTMLAttributes<HTMLDivElement> & {
		/** The axes of the bars: by default those along which the CSS of the element scrolls. */
		axis?: ScrollbarOptions['axis'];
		/** CSS classes of the div wrapping the scrolling one, next to wicket-scrollbar. */
		wrapperClass?: string;
		children?: Snippet;
	};

	let { axis, wrapperClass = '', class: className = '', children, ...rest }: Props = $props();

	let element: HTMLDivElement | undefined = $state();

	/** @returns the scrolling div */
	export function getElement(): HTMLDivElement | undefined {
		return element;
	}

	/** Scrolls to the start. */
	export function scrollToTop(smooth = false): void {
		bars()?.scrollToTop(smooth);
	}

	/** Scrolls to the end. */
	export function scrollToBottom(smooth = false): void {
		bars()?.scrollToBottom(smooth);
	}

	/** Scrolls to a position, in pixels from the start, along either axis or both. */
	export function scrollTo(position: ScrollPosition, smooth = false): void {
		bars()?.scrollTo(position, smooth);
	}

	/**
	 * Scrolls an element inside to the top, the element or a CSS selector of it; false if there is
	 * no such element.
	 */
	export function scrollToElement(target: Element | string, smooth = false): boolean {
		return bars()?.scrollToElement(target, smooth) ?? false;
	}

	function bars(): Scrollbar | undefined {
		return element ? Scrollbar.of(element) : undefined;
	}
</script>

<div class={['wicket-scrollbar', wrapperClass]}>
	<div {...rest} class={['wicket-scrollbar-host', className]} bind:this={element}
		use:scrollbar={{ axis }}>
		{@render children?.()}
	</div>
</div>
