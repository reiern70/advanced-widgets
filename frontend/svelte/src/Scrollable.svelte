<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  @component
  A div scrolling with the slim scroll bars of the core, which look the same in every browser and
  take the theme's colors. Give it a height, or a maximum height, and an overflow in its CSS.
-->
<script lang="ts">
	import type { Snippet } from 'svelte';
	import type { HTMLAttributes } from 'svelte/elements';
	import { scrollbar, type ScrollbarOptions } from '@advanced-widgets/core';

	type Props = HTMLAttributes<HTMLDivElement> & {
		/** The axes of the bars: by default those along which the CSS of the element scrolls. */
		axis?: ScrollbarOptions['axis'];
		/** CSS classes of the div wrapping the scrolling one, next to wicket-scrollbar. */
		wrapperClass?: string;
		children?: Snippet;
	};

	let { axis, wrapperClass = '', class: className = '', children, ...rest }: Props = $props();
</script>

<div class={['wicket-scrollbar', wrapperClass]}>
	<div {...rest} class={['wicket-scrollbar-host', className]} use:scrollbar={{ axis }}>
		{@render children?.()}
	</div>
</div>
