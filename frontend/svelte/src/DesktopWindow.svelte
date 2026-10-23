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
  A window of a Desktop: its title bar, with the icon and the title, and its body, the children.
  The desktop adds the buttons and the handles, as the window allows.
-->
<script lang="ts">
	import { untrack, type Snippet } from 'svelte';
	import { partClasses } from '@antilia/core';

	import { getLook } from './look.js';

	const look = getLook();

	type Props = {
		/** The id of the window, which the desktop knows it by. */
		id: string;
		/** The title, in the title bar and on the button of the taskbar. */
		title: string;
		/** An icon before the title, which the taskbar shows too; an svg, for example. */
		icon?: Snippet;
		/** The markup of the icon, an svg, instead of icon. */
		iconMarkup?: string;
		/**
		 * The id of the pane the window is in, the one shown when left out; the user moves it to
		 * another too.
		 */
		pane?: string;
		/** The body of the window. */
		children?: Snippet;
		/** Where the window starts, from the left of the desktop, in pixels; cascading when left out. */
		x?: number;
		/** Where the window starts, from the top of the desktop, in pixels; cascading when left out. */
		y?: number;
		/** The width the window starts with, in pixels, 420 when left out. */
		width?: number;
		/** The height the window starts with, in pixels, 280 when left out. */
		height?: number;
		/** The smallest width of the window, in pixels, 180 when left out. */
		minWidth?: number;
		/** The smallest height of the window, in pixels, 100 when left out. */
		minHeight?: number;
		/** Whether the edges of the window resize it, true when left out. */
		resizable?: boolean;
		/** Whether the window can be maximized, true when left out. */
		maximizable?: boolean;
		/** Whether the window can be minimized to the taskbar, true when left out. */
		minimizable?: boolean;
		/** Whether a double click on the title bar folds the window, true when left out. */
		foldable?: boolean;
		/** Whether the window has a button closing it, false when left out. */
		closable?: boolean;
		/**
		 * Whether the window has changes not saved, which the user is asked about before it closes;
		 * when left out, whether a field in it holds another value than the one it was rendered with.
		 */
		modified?: boolean;
		/** Whether the window starts minimized. */
		minimized?: boolean;
		/** Whether the window starts maximized. */
		maximized?: boolean;
		/** Whether the window starts folded. */
		folded?: boolean;
		/** Whether the window starts closed. */
		closed?: boolean;
		/** CSS classes of the window, next to antilia-window. */
		class?: string;
		/** CSS classes of the body of the window, next to antilia-window-body. */
		bodyClass?: string;
	};

	let { id, title, icon, iconMarkup, pane, children, x, y, width, height, minWidth, minHeight, resizable,
		maximizable, minimizable, foldable, closable, modified, minimized = false, maximized = false,
		folded = false, closed = false, class: className = '', bodyClass = '' }: Props = $props();

	const flag = (value: boolean | undefined) => value === undefined ? undefined : String(value);
	const present = (value: boolean) => value ? '' : undefined;
	// the state it starts in: the desktop keeps it from then on
	const start = untrack(() => ({
		minimized: present(minimized), maximized: present(maximized), folded: present(folded),
		closed: present(closed)
	}));
</script>

<section {id} class={[partClasses(look, 'window'), className].filter(Boolean).join(' ')} data-x={x}
	data-y={y} data-width={width} data-height={height} data-min-width={minWidth}
	data-min-height={minHeight} data-resizable={flag(resizable)}
	data-maximizable={flag(maximizable)} data-minimizable={flag(minimizable)}
	data-foldable={flag(foldable)} data-closable={flag(closable)} data-modified={flag(modified)}
	data-minimized={start.minimized} data-maximized={start.maximized} data-folded={start.folded}
	data-closed={start.closed} data-pane={pane}>
	<header class={partClasses(look, 'window-header')}>
		{#if iconMarkup}<span class={partClasses(look, 'window-icon')}>{@html iconMarkup}</span>
		{:else if icon}<span class={partClasses(look, 'window-icon')}>{@render icon()}</span>{/if}
		<span class={partClasses(look, 'window-title')}>{title}</span>
	</header>
	<div class={[partClasses(look, 'window-body'), bodyClass].filter(Boolean).join(' ')}>
		{@render children?.()}
	</div>
</section>
