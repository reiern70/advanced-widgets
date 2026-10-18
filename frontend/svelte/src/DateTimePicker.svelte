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
  A DatePicker taking a time too: its value, which binds both ways, is ISO text such as
  2026-10-09T14:30.
-->
<script lang="ts">
	import type { ComponentProps } from 'svelte';
	import DatePicker from './DatePicker.svelte';

	let { value = $bindable(''), ...rest }: Omit<ComponentProps<typeof DatePicker>, 'time'> =
		$props();
</script>

<DatePicker {...rest} time bind:value />
