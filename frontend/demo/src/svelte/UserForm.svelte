<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!-- The form of a user, every field required, in the markup of the Wicket one. -->
<script lang="ts">
	import { untrack } from 'svelte';
	import { Dropdown } from '@advanced-widgets/svelte';

	import type { Choice, User } from '../data.js';
	import { FIELDS, fieldsOf, MAX_LENGTH, validate, type UserFields,
		type UserFormTexts } from '../json-list.js';
	import Feedback from './Feedback.svelte';
	import type { Message } from './feedback.js';

	let { texts, user, countries, onsave, oncancel }: { texts: UserFormTexts; user: User | null;
		countries: Choice[]; onsave: (fields: UserFields) => void; oncancel: () => void } = $props();

	let fields = $state(untrack(() => fieldsOf(user)));
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
		<Dropdown name="country" options={countries} bind:value={fields.country}
			aria-invalid={errors.country ? true : undefined} filter={texts.filter}
			noMatch={texts.noMatch} more={texts.more} remove={texts.remove} />
	</label>
	{@render input('city', 'address-level2')}
	{@render input('address', 'street-address')}
	<div class="dynamic-data-table-overlay-buttons">
		<button type="button" class="dynamic-data-table-button" onclick={oncancel}>{texts.cancel}</button>
		<button type="submit" class="dynamic-data-table-button dynamic-data-table-button-primary">
			{texts.save}
		</button>
	</div>
</form>
