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
  The drop-down choosing the icons of the trees, as that of the Wicket page does.
-->
<script lang="ts">
	import { Dropdown } from '@antilia/svelte';

	import { iconSetOptions, type IconSet, type TreeDemoTexts } from '../tree.js';

	let { id, value, texts, onchange }: { id: string; value: IconSet; texts: TreeDemoTexts;
		onchange: (value: IconSet) => void } = $props();
</script>

<label for={id}>{texts.icons}</label>
<Dropdown {id} name="icons" {value} options={iconSetOptions(texts)}
	onchange={(chosen) => onchange(String(chosen) as IconSet)}
	filter={texts.filter} noMatch={texts.noMatch} more={texts.more} remove={texts.remove} />
