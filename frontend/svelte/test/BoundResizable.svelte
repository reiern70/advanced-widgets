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
<!-- A Resizable whose size is bound, shown next to it. -->
<script lang="ts">
	import { Resizable, type ResizeAxis } from '../src/index.js';

	let { width = $bindable(null), height = $bindable(120), axis = 'both', enabled = true,
		className = 'notes', onresized }: { width?: number | null; height?: number | null;
			axis?: ResizeAxis; enabled?: boolean; className?: string;
			onresized?: (size: unknown) => void } = $props();
</script>

<Resizable id="r" class={className} bind:width bind:height {axis} {enabled} {onresized}
	label="Resize the notes">
	<p>Text</p>
</Resizable>
<output>{width} × {height}</output>
<button type="button" class="reset" onclick={() => (height = 300)}>reset</button>
