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
  @component
  A progress bar in the markup of the Wicket one, a native progress element under a label, shown
  by the core's style sheet progress-bar.css: striped in the theme's colors, the stripes moving
  unless the system asks for reduced motion, or solid when the bar is still.
-->
<script lang="ts">
	import { partClasses } from '@antilia/core';

	import { getLook } from './look.js';

	const look = getLook();
	type Props = {
		/** The percentage, clamped to 0..100; null or left out for an indeterminate bar. */
		value?: number | null;
		/**
		 * Whether the bar is still: solid at any percentage, neither striped nor moving, for a
		 * figure such as how much is paid rather than something under way. An indeterminate bar
		 * moves anyway.
		 */
		still?: boolean;
		class?: string;
	};

	let { value, still = false, class: className = '' }: Props = $props();

	const percentage = $derived(value == null ? null : Math.max(0, Math.min(100,
		Math.round(value))));
</script>

<div class={[partClasses(look, 'progress', still && 'progress-still'), className]}>
	{#if percentage === null}
		<progress class={partClasses(look, 'progress-value')} max="100"></progress>
	{:else}
		<progress class={partClasses(look, 'progress-value')} max="100" value={percentage}>{percentage}%</progress>
		<span class={partClasses(look, 'progress-label')} aria-hidden="true">{percentage}%</span>
	{/if}
</div>
