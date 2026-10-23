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
  A button in the colors of the theme, in the markup of Wicket's AjaxActionButton and shown by the
  core's style sheet button.css: an icon, a text or both, in a variant and a size. It is a button
  of type button unless told otherwise, so it never submits a form by accident. In the look given
  by setLook, such as Bootstrap's, it has the classes of that look.
-->
<script lang="ts">
	import type { Snippet } from 'svelte';
	import type { HTMLButtonAttributes } from 'svelte/elements';

	import { buttonClasses, type ButtonSize, type ButtonVariant, partClasses } from '@antilia/core';

	import { getLook } from './look.js';

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
		'aria-label': ariaLabel, ...rest }: Props = $props();

	const look = getLook();
</script>

<button {...rest} {type} class={buttonClasses({ variant, size, iconOnly, look,
	className: typeof className === 'string' ? className : undefined })}
	title={title ?? (iconOnly ? label : undefined)}
	aria-label={ariaLabel ?? (iconOnly ? label : undefined)}>
	{#if icon}<span class={partClasses(look, 'button-icon')} aria-hidden="true">{@render icon()}</span>{/if}
	{#if !iconOnly}<span class={partClasses(look, 'button-label')}>{label}</span>{/if}
</button>
