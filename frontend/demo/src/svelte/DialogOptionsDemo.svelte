<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  The options example in Svelte: the form setting the options of a dialog, as the Wicket one, and
  the button opening it.
-->
<script lang="ts">
	import { Button } from '@antilia/svelte';

	import { DEFAULT_DIALOG_OPTIONS, DIALOG_FLAGS, DIALOG_SIZES, isValidWidth,
		type DialogOptions } from '../dialog-options.js';
	import type { ModalDemoConfig } from '../team.js';
	import NestedDialog from './NestedDialog.svelte';

	let { config }: { config: ModalDemoConfig } = $props();
	const texts = $derived(config.texts);
	const id = $props.id();
	let options = $state<DialogOptions>({ ...DEFAULT_DIALOG_OPTIONS });
	let open = $state(false);
</script>

<form class="dialog-options" onsubmit={(event) => event.preventDefault()}>
	<h2 class="team-demo__title">{texts.options}</h2>
	<div class="dialog-options__groups">
		<fieldset>
			<legend>{texts['options.behavior']}</legend>
			{#each DIALOG_FLAGS as flag (flag)}
				<label><input type="checkbox" bind:checked={options[flag]} />
					{texts[`option.${flag}`]}</label>
			{/each}
		</fieldset>
		<fieldset>
			<legend>{texts.size}</legend>
			<div class="dialog-options__sizes">
				{#each DIALOG_SIZES as size (size)}
					<label><input type="radio" name={id + '-size'} value={size}
						bind:group={options.size} /> {texts[`size.${size}`]}</label>
				{/each}
			</div>
			<label class="dialog-options__width">
				{texts.width}
				<input type="text" bind:value={options.width} placeholder="40em" size="8"
					aria-invalid={isValidWidth(options.width.trim()) ? undefined : true} />
			</label>
		</fieldset>
	</div>
	<Button variant="primary" label={texts.open} onclick={() => (open = true)}>
		{#snippet icon()}{@html config.icons.open}{/snippet}
	</Button>
	{#if open}
		<NestedDialog {config} options={{ ...$state.snapshot(options), width: options.width.trim() }}
			level={1} onclose={() => (open = false)} />
	{/if}
</form>
