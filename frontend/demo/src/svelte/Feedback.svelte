<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  The messages of the app, rendered as Wicket's FeedbackPanel renders them, so that the page's
  style sheet shows them the same.
-->
<script lang="ts">
	import type { Message } from './feedback.js';

	let { messages }: { messages: Message[] } = $props();
</script>

{#if messages.length}
	<ul class="feedbackPanel">
		{#each messages as message, index (index)}
			{@const level = 'feedbackPanel' + message.level.toUpperCase()}
			<li class={level}><span class={level}>{message.text}</span></li>
		{/each}
	</ul>
{/if}
