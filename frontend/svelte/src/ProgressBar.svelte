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
  @component
  A progress bar in the markup of the Wicket one, a native progress element under a label, shown
  by the core's style sheet progress-bar.css: striped in the theme's colors, the stripes moving
  unless the system asks for reduced motion.
-->
<script lang="ts">
	import { partClasses } from '@antilia/core';

	import { getLook } from './look.js';

	const look = getLook();
	type Props = {
		/** The percentage, clamped to 0..100; null or left out for an indeterminate bar. */
		value?: number | null;
		class?: string;
	};

	let { value, class: className = '' }: Props = $props();

	const percentage = $derived(value == null ? null : Math.max(0, Math.min(100,
		Math.round(value))));
</script>

<div class={[partClasses(look, 'progress'), className]}>
	{#if percentage === null}
		<progress class={partClasses(look, 'progress-value')} max="100"></progress>
	{:else}
		<progress class={partClasses(look, 'progress-value')} max="100" value={percentage}>{percentage}%</progress>
		<span class={partClasses(look, 'progress-label')} aria-hidden="true">{percentage}%</span>
	{/if}
</div>
