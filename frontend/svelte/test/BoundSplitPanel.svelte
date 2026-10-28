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
<!-- A SplitPanel whose position and collapsed pane are bound, shown next to it. -->
<script lang="ts">
	import { SplitPanel, type SplitOrientation, type SplitPane, type SplitSize }
		from '../src/index.js';

	let { position = $bindable(40), collapsed = $bindable(null), orientation = 'horizontal',
		resizable = true, collapsible = false, stackBelow, onresized }: { position?: SplitSize;
			collapsed?: SplitPane | null; orientation?: SplitOrientation; resizable?: boolean;
			collapsible?: boolean; stackBelow?: number;
			onresized?: (position: SplitSize) => void } = $props();
</script>

<SplitPanel id="s" bind:position bind:collapsed {orientation} {resizable} {collapsible}
	{stackBelow} {onresized} label="Resize" stackedCollapseFirstLabel="Hide the filters">
	{#snippet first()}<p>One</p>{/snippet}
	{#snippet second()}<p>Two</p>{/snippet}
</SplitPanel>
<output>{position}</output>
<span class="collapsed">{collapsed}</span>
<button type="button" class="reset" onclick={() => (position = 20)}>reset</button>
<button type="button" class="restore" onclick={() => (collapsed = null)}>restore</button>
