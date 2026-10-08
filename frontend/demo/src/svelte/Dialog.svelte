<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  A modal dialog in the markup of Wicket's ModalDialog with its default theme, so that the page's
  style sheet shows it the same. Escape closes it; it takes the focus while open and gives it back
  when closed.
-->
<script lang="ts">
	import type { Snippet } from 'svelte';

	let { title, onclose, children }: { title: string; onclose: () => void; children: Snippet } =
		$props();

	const titleId = $props.id();
	let dialog = $state<HTMLDivElement>();

	$effect(() => {
		const opener = document.activeElement as HTMLElement | null;
		const element = dialog!;
		(element.querySelector<HTMLElement>('input, select, button') ?? element).focus();
		return () => opener?.focus?.();
	});

	function keydown(event: KeyboardEvent) {
		if (event.key === 'Escape') {
			event.stopPropagation();
			onclose();
		}
	}
</script>

<div class="dialog-theme-default">
	<div class="modal-dialog-overlay">
		<div bind:this={dialog} class="modal-dialog" role="dialog" aria-modal="true"
			aria-labelledby={titleId} tabindex="-1" onkeydown={keydown}>
			<div class="modal-dialog-content">
				<h1 class="modal-dialog-header" id={titleId}>{title}</h1>
				<div class="modal-dialog-body">{@render children()}</div>
			</div>
		</div>
	</div>
</div>
