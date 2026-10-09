<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  @component
  A progress bar in the markup of the Wicket one, a native progress element under a label, shown
  by the core's style sheet progress-bar.css: striped in the theme's colors, the stripes moving
  unless the system asks for reduced motion.
-->
<script lang="ts">
	type Props = {
		/** The percentage, clamped to 0..100; null or left out for an indeterminate bar. */
		value?: number | null;
		class?: string;
	};

	let { value, class: className = '' }: Props = $props();

	const percentage = $derived(value == null ? null : Math.max(0, Math.min(100,
		Math.round(value))));
</script>

<div class={['antilia-progress-bar', className]}>
	{#if percentage === null}
		<progress class="antilia-progress-bar-value" max="100"></progress>
	{:else}
		<progress class="antilia-progress-bar-value" max="100" value={percentage}>{percentage}%</progress>
		<span class="antilia-progress-bar-label" aria-hidden="true">{percentage}%</span>
	{/if}
</div>
