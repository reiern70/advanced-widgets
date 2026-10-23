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
  The split panel of the core: two panes, side by side or one above the other, and a divider
  between them the user drags, or moves with the keyboard, to share the space differently; when
  collapsible, its buttons collapse a pane and restore it. Svelte renders the panes, the snippets
  first and second, and the divider; the core moves it. The position, the size of the first pane
  in percent or as '250px', and the collapsed pane bind both ways (bind:position,
  bind:collapsed): they follow the user, and change the split panel when the page changes them.
  Give a vertical one a height.
-->
<script lang="ts">
	import { untrack, type Snippet } from 'svelte';
	import { parseSplitSize, partClasses, type SplitOrientation, type SplitPane, SplitPanel,
		type SplitSize } from '@antilia/core';

	import { getLook } from './look.js';

	const look = getLook();

	type Props = {
		/** The left or top pane. */
		first: Snippet;
		/** The right or bottom pane. */
		second: Snippet;
		/** horizontal (the default) for the panes side by side, vertical for one above the other. */
		orientation?: SplitOrientation;
		/**
		 * The size of the first pane, 50 (percent) when left out. In pixels, '250px', the first
		 * pane keeps its size when the split panel grows or shrinks.
		 */
		position?: SplitSize;
		/** The smallest size of the first pane, in percent or as '200px', 0 when left out. */
		min?: SplitSize;
		/** The largest size of the first pane, in percent or as '600px', 100 when left out. */
		max?: SplitSize;
		/** The smallest size of the second pane, in percent or as '200px', 0 when left out. */
		minSecond?: SplitSize;
		/** How far an arrow key moves the divider, in percent or as '20px', 5 when left out. */
		step?: SplitSize;
		/** Whether the user can move the divider, true when left out. */
		resizable?: boolean;
		/** Whether the divider has buttons collapsing a pane and restoring it, false when left out. */
		collapsible?: boolean;
		/** The collapsed pane, null for none; left out, it is the user's alone. */
		collapsed?: SplitPane | null;
		/** The accessible name of the divider. */
		label?: string;
		/** The title of the button collapsing the first pane. */
		collapseFirstLabel?: string;
		/** The title of the button collapsing the second pane. */
		collapseSecondLabel?: string;
		/** The title of the button restoring a collapsed pane. */
		restoreLabel?: string;
		id?: string;
		/** CSS classes of the split panel, next to antilia-split. */
		class?: string;
		/** Called with the position while the user moves the divider, in the unit it was given. */
		onresize?: (position: SplitSize) => void;
		/** Called with the position once the user has moved the divider. */
		onresized?: (position: SplitSize) => void;
		/** Called with the collapsed pane, or null, once the user has collapsed or restored one. */
		oncollapse?: (collapsed: SplitPane | null) => void;
	};

	let {
		first,
		second,
		orientation = 'horizontal',
		position = $bindable(50),
		min,
		max,
		minSecond,
		step,
		resizable = true,
		collapsible = false,
		collapsed = $bindable(),
		label,
		collapseFirstLabel,
		collapseSecondLabel,
		restoreLabel,
		id,
		class: className = '',
		onresize,
		onresized,
		oncollapse
	}: Props = $props();

	let element = $state<HTMLDivElement>();
	let instance: SplitPanel | null = null;

	// the classes the core gives the element, so that Svelte keeps them when it updates it
	const classes = $derived([partClasses(look, 'split'), 'antilia-split-' + orientation,
		!resizable && 'antilia-split-fixed',
		parseSplitSize(position)?.unit === 'px' && 'antilia-split-pixels',
		collapsible && 'antilia-split-collapsible', className].filter(Boolean).join(' '));

	$effect(() => {
		if (!element) {
			return;
		}
		const split = new SplitPanel(element, untrack(() => ({
			orientation, position, min, max, minSecond, step, resizable, collapsible, collapsed,
			label, collapseFirstLabel, collapseSecondLabel, restoreLabel, look,
			onResize: (next: SplitSize) => onresize?.(next),
			onResized: (next: SplitSize) => {
				position = next;
				onresized?.(next);
			},
			onCollapse: (next: SplitPane | null) => {
				collapsed = next;
				oncollapse?.(next);
			}
		})));
		instance = split;
		return () => {
			split.destroy();
			instance = null;
		};
	});

	$effect(() => {
		const next = { orientation, min, max, minSecond, step, resizable, collapsible, label,
			collapseFirstLabel, collapseSecondLabel, restoreLabel };
		untrack(() => instance?.configure(next));
	});

	$effect(() => {
		const next = position;
		untrack(() => instance?.setPosition(next));
	});

	$effect(() => {
		const next = collapsed;
		if (next !== undefined) {
			untrack(() => instance?.setCollapsed(next));
		}
	});

	// Svelte wrote other classes over the element's: those the core keeps, such as the collapsed
	// pane's, come back
	$effect(() => {
		void classes;
		untrack(() => instance?.configure({}));
	});
</script>

<div bind:this={element} {id} class={classes}>
	<div class={partClasses(look, 'split-pane', 'split-first')}>{@render first()}</div>
	<div class={partClasses(look, 'split-divider')}></div>
	<div class={partClasses(look, 'split-pane', 'split-second')}>{@render second()}</div>
</div>
