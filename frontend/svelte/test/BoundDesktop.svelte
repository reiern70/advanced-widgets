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
