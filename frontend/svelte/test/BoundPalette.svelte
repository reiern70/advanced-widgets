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
<!-- A Palette whose value is bound, shown next to it. -->
<script lang="ts">
	import { Palette, type PaletteChoice } from '../src/index.js';

	let { options, value = $bindable([]), ordered = false }:
		{ options: PaletteChoice[]; value?: string[]; ordered?: boolean } = $props();
</script>

<Palette {options} {ordered} name="fruits" bind:value />
<p>{JSON.stringify(value)}</p>
<button type="button" class="reset" onclick={() => (value = ['cherry'])}>reset</button>
