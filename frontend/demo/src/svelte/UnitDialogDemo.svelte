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
  The unit of measure of the drop-down page in Svelte, chosen in a Modal as the Wicket page does:
  the drop-down of the long list of units, long options in groups, is the last thing in the body
  of the dialog, so its open list reaches over the footer, Cancel and Choose.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { Button, Dropdown, Modal } from '@antilia/svelte';

	import { format, labelOf, type DropdownTexts } from '../data.js';
	import type { UnitsConfig } from '../dropdown.js';

	let { config, dropdown }: { config: UnitsConfig; dropdown: DropdownTexts } = $props();

	const texts = $derived(config.texts);
	const icons = $derived(config.icons);
	// the unit starts as the page has it, then is the app's
	let unit = $state(untrack(() => config.value));
	let open = $state(false);
	// the unit of the drop-down of the dialog, taken when the dialog is sent
	let chosen = $state('');

	/** The id of the form of the dialog, which the Choose button of the footer submits. */
	const FORM_ID = 'svelte-unit-form';

	function openDialog(): void {
		chosen = unit;
		open = true;
	}

	function choose(event: SubmitEvent): void {
		event.preventDefault();
		unit = chosen;
		open = false;
	}
</script>

<section class="dropdown-demo__dialog">
	<h3>{texts['units.title']}</h3>
	<p class="dropdown-demo__unit">{format(texts['units.chosen'], labelOf(config.options, unit))}</p>
	<Button label={texts['units.choose']} onclick={openDialog}>
		{#snippet icon()}{@html icons.choose}{/snippet}
	</Button>
</section>

<Modal {open} onclose={() => (open = false)} size="medium" closeLabel={texts['units.close']}
	title={texts['units.dialog']}>
	{#snippet footer()}
		<Button size="small" label={texts['units.cancel']} data-modal-close>
			{#snippet icon()}{@html icons.cancel}{/snippet}
		</Button>
		<Button type="submit" form={FORM_ID} size="small" variant="primary" label={texts['units.ok']}>
			{#snippet icon()}{@html icons.ok}{/snippet}
		</Button>
	{/snippet}
	<form id={FORM_ID} class="dropdown-demo__unit-form" onsubmit={choose}>
		<p>{texts['units.text']}</p>
		<label>
			{texts['units.unit']}
			<Dropdown name="unit" options={config.options} bind:value={chosen} {...dropdown} />
		</label>
	</form>
</Modal>
