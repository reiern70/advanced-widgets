<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  The Progress bar demo in Svelte: the bars of the Wicket page, the first one advanced by a timer
  of the app.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { ProgressBar } from '@advanced-widgets/svelte';

	import { advance, TICK_MILLIS, type ProgressConfig } from '../progress.js';

	let { config }: { config: ProgressConfig } = $props();

	const texts = $derived(config.texts);
	// the progress starts as the page has it, then is the app's
	let progress = $state(untrack(() => config.progress));
	let running = $state(false);

	$effect(() => {
		if (!running) {
			return;
		}
		const timer = window.setInterval(() => {
			progress = advance(progress);
			running = progress < 100;
		}, TICK_MILLIS);
		return () => window.clearInterval(timer);
	});

	function toggle(event: MouseEvent) {
		event.preventDefault();
		running = !running && progress < 100;
	}

	function reset(event: MouseEvent) {
		event.preventDefault();
		progress = 0;
		running = false;
	}
</script>

<div>
	<h3>{texts.known}</h3>
	<ProgressBar value={progress} />
	<p>
		<a href="#top" onclick={toggle}>{running ? texts.pause : texts.start}</a> |
		<a href="#top" onclick={reset}>{texts.reset}</a>
	</p>
	<h3>{texts.finished}</h3>
	<ProgressBar value={100} />
	<h3>{texts.unknown}</h3>
	<ProgressBar />
</div>
