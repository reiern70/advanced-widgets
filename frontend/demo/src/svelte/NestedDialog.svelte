<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  A dialog of the options example: Close, a button opening a dialog nested in it while it may, and
  OK in its footer.
-->
<script lang="ts">
	import { Button, Modal } from '@antilia/svelte';

	import { format } from '../data.js';
	import { modalOptions, type DialogOptions } from '../dialog-options.js';
	import type { ModalDemoConfig } from '../team.js';
	import NestedDialog from './NestedDialog.svelte';

	let { config, options, level, onclose }: { config: ModalDemoConfig; options: DialogOptions;
		level: number; onclose: () => void } = $props();
	const texts = $derived(config.texts);
	const icons = $derived(config.icons);
	const deepest = $derived(level >= config.maxLevel);
	let nested = $state(false);
</script>

<Modal open {onclose} {...modalOptions(options)} closeLabel={texts.close}
	title={format(texts['dialog.title'], level)}>
	{#snippet footer()}
		<Button size="small" label={texts.close} data-modal-close>
			{#snippet icon()}{@html icons.cancel}{/snippet}
		</Button>
		{#if !deepest}
			<Button size="small" label={texts['dialog.nested']} onclick={() => (nested = true)}>
				{#snippet icon()}{@html icons.nested}{/snippet}
			</Button>
		{/if}
		<Button size="small" variant="primary" label={texts['dialog.ok']} onclick={onclose}>
			{#snippet icon()}{@html icons.ok}{/snippet}
		</Button>
	{/snippet}
	<p class="dialog-options__text">{deepest ? texts['dialog.deepest'] : texts['dialog.text']}</p>
	{#if nested}
		<NestedDialog {config} {options} level={level + 1} onclose={() => (nested = false)} />
	{/if}
</Modal>
