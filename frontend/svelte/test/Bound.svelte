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
<!-- A Dropdown whose value is bound, shown next to it. -->
<script lang="ts">
	import { Dropdown, type DropdownOption } from '../src/index.js';

	let { options, value = $bindable(), multiple = false }:
		{ options: DropdownOption[]; value?: string | string[]; multiple?: boolean } = $props();
</script>

<Dropdown {options} {multiple} bind:value />
<p>{JSON.stringify(value)}</p>
<button type="button" class="reset" onclick={() => (value = multiple ? [] : 'apple')}>reset</button>
