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
  The Desktop demo in Svelte: a desktop of the Wicket page's markup, its windows the JSON grid in
  Svelte, notes, the drop-downs, the autocompletes, the buttons, the date pickers, the JSON list,
  the modal dialogs and the progress bars in Svelte, and the keyboard shortcuts, which start
  minimized; the menu of the taskbar opens them all, and each can be closed, asking first
  when it has changes. The grids, the notes and the keyboard are in the first pane, the drop-downs
  in the second, the other demos in the third; the launcher opens more notes. Above it, the side
  of the taskbar and a button opening the keyboard shortcuts again.
-->
<script lang="ts">
	import { Button, Desktop, DesktopWindow, Dropdown, type TaskbarPosition } from '@antilia/svelte';

	import { untrack } from 'svelte';

	import { dropdownTexts } from '../data.js';
	import { demoBounds, demoPanes, FRAMEWORK_COLORS, iconMarkup, KEYBOARD_ICON, NOTES_BOUNDS,
		NOTES_ICON, paneOf, shortcuts, TASKBAR_POSITIONS, windowId, type DesktopConfig,
		type DesktopDemos, type DesktopDemoTexts, type DesktopWindowName } from '../desktop.js';
	import AutocompleteDemo from './AutocompleteDemo.svelte';
	import ButtonsDemo from './ButtonsDemo.svelte';
	import DatePickerDemo from './DatePickerDemo.svelte';
	import DropdownDemo from './DropdownDemo.svelte';
	import JsonListDemo from './JsonListDemo.svelte';
	import JsonTableDemo from './JsonTableDemo.svelte';
	import ModalDemo from './ModalDemo.svelte';
	import ProgressDemo from './ProgressDemo.svelte';

	let { config }: { config: DesktopConfig } = $props();

	const texts = $derived(config.texts);
	let desktop = $state<Desktop>();
	let taskbar = $state<TaskbarPosition>('bottom');
	const id = (name: DesktopWindowName) => windowId('svelte', name);
	const demos = $derived(config.demos);
	/** The other demos, in the order of their windows. */
	const DEMOS: (keyof DesktopDemos)[] = ['autocomplete', 'buttons', 'datePicker', 'jsonList',
		'modalDialog', 'progressBar'];
	const positionText = (position: TaskbarPosition) =>
		texts[('taskbar.' + position) as keyof DesktopDemoTexts];
	// once: the user renames them since
	const panes = untrack(() => demoPanes(config.texts));
</script>

{#snippet notes()}
	<textarea class="desktop-demo__notes" aria-label={texts.notes}
		defaultValue={texts.notesText}></textarea>
{/snippet}

{#snippet badge()}
	<svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" focusable="false">
		<rect width="24" height="24" rx="6" fill={FRAMEWORK_COLORS.svelte} />
		<text x="12" y="17" text-anchor="middle" font-size="14" font-weight="700"
			font-family="sans-serif" fill="#ffffff">S</text>
	</svg>
{/snippet}

{#snippet iconOf(path: string)}
	<svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" focusable="false"
		fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
		stroke-linejoin="round">
		<path d={path} />
	</svg>
{/snippet}

<div class="desktop-demo">
	<form class="desktop-demo__options" onsubmit={(event) => event.preventDefault()}>
		<label class="desktop-demo__option">
			{texts.taskbar}
			<Dropdown value={taskbar} options={TASKBAR_POSITIONS.map((position) =>
				({ value: position, label: positionText(position) }))}
				onchange={(value) => taskbar = value as TaskbarPosition} {...dropdownTexts(texts)} />
		</label>
		<Button label={texts.openKeyboard} size="small"
			onclick={() => desktop?.activate(id('keyboard'))}>
			{#snippet icon()}
				<svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" focusable="false"
					fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
					stroke-linejoin="round">
					<path d={KEYBOARD_ICON} />
				</svg>
			{/snippet}
		</Button>
	</form>
	<Desktop bind:this={desktop} {taskbar} texts={config.desktop}
		ontaskbar={(position) => (taskbar = position)} {panes}
		windowTypes={[{ type: 'notes', title: texts.notes, icon: iconMarkup(NOTES_ICON),
			window: NOTES_BOUNDS, content: notes }]}>
		<DesktopWindow id={id('users')} pane={paneOf('users')} title={texts.svelteUsers} x={16} y={16} width={900}
			height={460} minWidth={320} minHeight={160} closable modified={false}>
			{#snippet icon()}{@render badge()}{/snippet}
			<JsonTableDemo config={config.jsonTable} />
		</DesktopWindow>
		<DesktopWindow id={id('notes')} pane={paneOf('notes')} title={texts.notes} x={800} y={24} width={360}
			height={240} closable>
			{#snippet icon()}{@render iconOf(NOTES_ICON)}{/snippet}
			<textarea class="desktop-demo__notes" aria-label={texts.notes}
				defaultValue={texts.notesText}></textarea>
		</DesktopWindow>
		<DesktopWindow id={id('dropdowns')} pane={paneOf('dropdowns')}
			title={texts.svelteDropdowns} x={16} y={16}
			width={700} height={460} minWidth={320} minHeight={160} closable>
			{#snippet icon()}{@render badge()}{/snippet}
			<DropdownDemo config={config.dropdown} />
		</DesktopWindow>
		{#each DEMOS as name, index (name)}
			{@const bounds = demoBounds(index)}
			<DesktopWindow id={id(name)} pane={paneOf(name)} title={demos[name].title} x={bounds.x} y={bounds.y}
				width={bounds.width} height={bounds.height} minWidth={320} minHeight={160} closable>
				{#snippet icon()}{@render badge()}{/snippet}
				{#if name === 'autocomplete'}
					<AutocompleteDemo config={demos.autocomplete.config} />
				{:else if name === 'buttons'}
					<ButtonsDemo config={demos.buttons.config} />
				{:else if name === 'datePicker'}
					<DatePickerDemo config={demos.datePicker.config} />
				{:else if name === 'jsonList'}
					<JsonListDemo config={demos.jsonList.config} />
				{:else if name === 'modalDialog'}
					<ModalDemo config={demos.modalDialog.config} />
				{:else}
					<ProgressDemo config={demos.progressBar.config} />
				{/if}
			</DesktopWindow>
		{/each}
		<DesktopWindow id={id('keyboard')} pane={paneOf('keyboard')} title={texts.keyboard} x={560} y={16} width={640}
			height={620} minWidth={320} minHeight={200} closable minimized>
			{#snippet icon()}{@render iconOf(KEYBOARD_ICON)}{/snippet}
			<dl class="desktop-demo__keys">
				{#each shortcuts(texts) as shortcut (shortcut.text)}
					<div class="desktop-demo__key">
						<dt>
							{#if shortcut.keys}
								{#each shortcut.keys as key, index (key)}{#if index > 0}+{/if}<kbd>{key}</kbd>{/each}
							{:else}
								{shortcut.action}
							{/if}
						</dt>
						<dd>{shortcut.text}</dd>
					</div>
				{/each}
			</dl>
		</DesktopWindow>
	</Desktop>
</div>
