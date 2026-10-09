<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  The dialog of the demos: an open Modal, which behaves and looks like Wicket's ModalDialog with
  its default theme, as the Wicket pages open theirs: Escape closes it, and it keeps the focus
  while open.
-->
<script lang="ts">
	import type { Snippet } from 'svelte';
	import { Modal } from '@antilia/svelte';

	let { title, onclose, children }: { title: string; onclose: () => void; children: Snippet } =
		$props();
</script>

<Modal open {title} {onclose}>{@render children()}</Modal>
