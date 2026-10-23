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
  A button opening a popover pointing at it on click, the core's Popover, and closing it on a
  second click. Its content is rendered by Svelte all along, hidden in the page, and moves into the
  popover while it is open: it keeps its state and its listeners, and Svelte goes on updating it
  there. The popover is in the look of the closest setLook, unless the options give one. The popover
  closes as the button goes away.
-->
<script lang="ts">
	import { onDestroy, type Snippet } from 'svelte';
	import type { HTMLButtonAttributes } from 'svelte/elements';

	import { partClasses, Popover, type PopoverOptions } from '@antilia/core';

	import { getLook } from './look.js';

	const look = getLook();

	type Props = Omit<HTMLButtonAttributes, 'children' | 'content'> & {
		/** What the popover shows besides the content, and where and how: the core's options. */
		options?: Omit<PopoverOptions, 'content'>;
		/** What the popover shows after its body: any markup, a form for example. */
		content?: Snippet;
		/** Called as the popover opened. */
		onopen?: (popover: Popover) => void;
		/** The button: an icon, a text or both. */
		children?: Snippet;
	};

	let { options = {}, content, onopen, onclick, children, type = 'button', ...rest }: Props =
		$props();

	let holder: HTMLDivElement | undefined = $state();
	let shown: Popover | null = null;

	function toggle(event: MouseEvent & { currentTarget: EventTarget & HTMLButtonElement }) {
		onclick?.(event);
		if (event.defaultPrevented) {
			return;
		}
		shown = Popover.toggle(event.currentTarget, { look, ...options,
			content: content && holder ? holder : undefined,
			onClose: () => {
				shown = null;
				options.onClose?.();
			} });
		if (shown) {
			onopen?.(shown);
		}
	}

	onDestroy(() => shown?.close());
</script>

<button {...rest} {type} onclick={toggle}>{@render children?.()}</button>
{#if content}
	<!-- hidden: the content leaves it for the popover, and comes back -->
	<span hidden><div bind:this={holder} class={partClasses(look, 'popover-content')}>{@render content()}</div></span>
{/if}
