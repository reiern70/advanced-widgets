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
<!-- The form of a user, every field required, in the markup of the Wicket one. -->
<script lang="ts">
	import { untrack } from 'svelte';
	import { Button, Dropdown } from '@antilia/svelte';

	import type { Choice, User } from '../data.js';
	import { FIELDS, fieldsOf, MAX_LENGTH, validate, type UserFields,
		type UserFormTexts } from '../json-list.js';
	import Feedback from './Feedback.svelte';
	import type { Message } from './feedback.js';

	let { texts, user, countries, onsave, oncancel }: { texts: UserFormTexts; user: User | null;
		countries: Choice[]; onsave: (fields: UserFields) => void; oncancel: () => void } = $props();

	let fields = $state(untrack(() => fieldsOf(user)));
	// a new user has no country until one is chosen, rather than the first of the list
	const choices = untrack(() => user?.country ? countries :
		[{ value: '', label: texts.chooseOne }, ...countries]);
	let errors = $state<Partial<UserFields>>({});
	const messages = $derived<Message[]>(FIELDS.filter((field) => errors[field])
		.map((field) => ({ level: 'error', text: errors[field]! })));

	function submit(event: SubmitEvent) {
		event.preventDefault();
		errors = validate(texts, fields);
		if (!Object.keys(errors).length) {
			onsave($state.snapshot(fields));
		}
	}
</script>

{#snippet input(field: Exclude<keyof UserFields, 'country'>, autocomplete: AutoFill)}
	<label>
		{texts[field]}
		<input type="text" name={field} bind:value={fields[field]} maxlength={MAX_LENGTH[field]}
			{autocomplete} aria-invalid={errors[field] ? true : undefined} />
	</label>
{/snippet}

<form class="user-edit" onsubmit={submit} novalidate>
	<Feedback {messages} />
	{@render input('firstName', 'given-name')}
	{@render input('lastName', 'family-name')}
	<label>
		{texts.country}
		<Dropdown name="country" options={choices} bind:value={fields.country}
			aria-invalid={errors.country ? true : undefined} filter={texts.filter}
			noMatch={texts.noMatch} more={texts.more} remove={texts.remove} />
	</label>
	{@render input('city', 'address-level2')}
	{@render input('address', 'street-address')}
	<div class="ant-button-bar ant-data-table-overlay-buttons">
		<Button label={texts.cancel} onclick={oncancel} />
		<Button type="submit" label={texts.save} variant="primary" />
	</div>
</form>
