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
<!-- A popover button with a counter as its content, for the tests. -->
<script lang="ts">
	import type { Popover, PopoverOptions } from '@antilia/core';

	import { PopoverButton } from '../src/index.js';

	let { popover = {}, rich = true, shown = true, onopen }: { popover?: PopoverOptions;
		rich?: boolean; shown?: boolean; onopen?: (popover: Popover) => void } = $props();

	let count = $state(0);
</script>

{#snippet counter()}
	<button type="button" class="count" onclick={() => count++}>{count}</button>
{/snippet}

<p id="before">before</p>
{#if shown}
	<PopoverButton class="mine" aria-label="More" options={popover} {onopen}
		content={rich ? counter : undefined}>i</PopoverButton>
{/if}
<p id="after">after</p>
