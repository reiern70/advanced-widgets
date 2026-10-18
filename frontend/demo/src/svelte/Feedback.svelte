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
