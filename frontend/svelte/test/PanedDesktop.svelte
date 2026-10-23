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
<!-- A Desktop of two panes, a window in each, and a type of window, notes. -->
<script lang="ts">
	import type { DesktopPane } from '@antilia/core';

	import { Desktop, DesktopWindow } from '../src/index.js';

	let { activePane, onpanes, onopenwindow }: { activePane?: string;
		onpanes?: (panes: DesktopPane[], active: string) => void;
		onopenwindow?: (type: string, pane: string) => void } = $props();
</script>

{#snippet notes(window: string)}<p>Notes of {window}</p>{/snippet}

<Desktop id="d" panes={[{ id: '0', name: 'Grids' }, { id: '1', name: 'Dropdowns' }]} {activePane}
	{onpanes} {onopenwindow}
	windowTypes={[{ type: 'notes', title: 'Notes', icon: '<svg class="notes"></svg>',
		content: notes, window: { width: 300 } }]}>
	<DesktopWindow id="one" title="One" pane="0" />
	<DesktopWindow id="two" title="Two" pane="1" />
</Desktop>
