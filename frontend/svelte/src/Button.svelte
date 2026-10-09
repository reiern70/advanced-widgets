<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  @component
  A button in the colors of the theme, in the markup of Wicket's AjaxActionButton and shown by the
  core's style sheet button.css: an icon, a text or both, in a variant and a size. It is a button
  of type button unless told otherwise, so it never submits a form by accident.
-->
<script lang="ts">
	import type { Snippet } from 'svelte';
	import type { HTMLButtonAttributes } from 'svelte/elements';

	import { buttonClasses, type ButtonSize, type ButtonVariant } from '@antilia/core';

	type Props = Omit<HTMLButtonAttributes, 'children'> & {
		/** The text of the button; with only the icon shown, its tooltip and accessible name. */
		label: string;
		/** The icon before the text, an inline SVG mostly. */
		icon?: Snippet;
		/** Whether only the icon is shown, the button then square. */
		iconOnly?: boolean;
		/** What the button stands for: secondary by default. */
		variant?: ButtonVariant;
		/** How big the button is: medium by default. */
		size?: ButtonSize;
	};

	let { label, icon, iconOnly = false, variant, size, class: className, type = 'button', title,
		...rest }: Props = $props();
</script>

<button {...rest} {type} class={buttonClasses({ variant, size, iconOnly,
	className: typeof className === 'string' ? className : undefined })}
	title={title ?? (iconOnly ? label : undefined)} aria-label={iconOnly ? label : undefined}>
	{#if icon}<span class="ant-button__icon" aria-hidden="true">{@render icon()}</span>{/if}
	{#if !iconOnly}<span class="ant-button__label">{label}</span>{/if}
</button>
