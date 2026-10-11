<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!-- A Desktop with as many windows as count says, the first with an icon, the second closable. -->
<script lang="ts">
	import type { TaskbarPosition, WindowState } from '@antilia/core';

	import { Desktop, DesktopWindow } from '../src/index.js';

	let { count = 2, taskbar = 'bottom', licenseKey, menu = true, confirmClose, modified,
		onchange, ontaskbar, onexpand, onactivate, desktop = $bindable() }: {
		count?: number; taskbar?: TaskbarPosition; licenseKey?: string; menu?: boolean;
		confirmClose?: (id: string, title: string) => boolean; modified?: boolean;
		onchange?: (states: Record<string, WindowState>) => void;
		ontaskbar?: (position: TaskbarPosition) => void; onexpand?: (expanded: boolean) => void;
		onactivate?: (id: string) => void; desktop?: Desktop } = $props();
</script>

<Desktop bind:this={desktop} id="d" class="tall" {taskbar} {licenseKey} {menu} {confirmClose}
	{onchange} {ontaskbar} {onexpand} {onactivate} texts={{ grid: 'Grid', unlicensed: 'Needs a key' }}>
	{#each Array.from({ length: count }, (_, index) => index) as index (index)}
		<DesktopWindow id={'w' + index} title={'Window ' + index} x={10 * index} y={20}
			width={300} height={200} closable={index === 1}
			modified={index === 1 ? modified : undefined} minimized={index === 2}>
			{#snippet icon()}{#if index === 0}<svg></svg>{/if}{/snippet}
			<p>Body {index}</p>
		</DesktopWindow>
	{/each}
</Desktop>
