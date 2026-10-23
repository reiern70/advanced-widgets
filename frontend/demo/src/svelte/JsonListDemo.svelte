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
  The JSON list demo in Svelte: the users as cards, from the users resource, filtered and sorted;
  a click on a card shows the user next to the list, its buttons edit and delete it in dialogs of
  the app. What the app changes stays in the browser.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { Dropdown, InfiniteList } from '@antilia/svelte';

	import { format, loadChoices, type Choice, type User } from '../data.js';
	import { interpolate, Users, type JsonListConfig, type UserFields } from '../json-list.js';
	import Dialog from './Dialog.svelte';
	import Feedback from './Feedback.svelte';
	import type { Message } from './feedback.js';
	import UserForm from './UserForm.svelte';

	/** The time the filter waits for more typing, as the Wicket filter throttles. */
	const FILTER_DELAY = 300;

	let { config }: { config: JsonListConfig } = $props();

	const texts = $derived(config.texts);
	const users = untrack(() => new Users(config.usersUrl, config.perPage, config.sort));
	let list = $state<ReturnType<typeof InfiniteList>>();
	let filter = $state('');
	let sort = $state(untrack(() => config.sort));
	let countries = $state<Choice[]>([]);
	let selected = $state<User | null>(null);
	/** What the dialog shows: the form of a user, new when its key is null, or a deletion. */
	let shown = $state<{ form: string | null } | { deletion: string } | null>(null);
	let messages = $state<Message[]>([]);
	const listTexts = $derived({ loading: texts.loading, end: texts.end, empty: texts.empty });
	const sorts = $derived(config.sorts.map((value) => ({ value,
		label: texts[('sort.' + value) as 'sort.firstName'] })));

	$effect(() => {
		loadChoices(config.choicesUrl).then((loaded) => (countries = loaded.countries),
			(error) => console.error(error));
	});

	// another filter or sort starts over from the first page, the filter once typing pauses
	$effect(() => {
		const next = { filter, sort };
		if (next.filter === users.filter && next.sort === users.sort) {
			return;
		}
		const timer = window.setTimeout(() => {
			users.filter = next.filter;
			users.sort = next.sort;
			users.reset();
			list?.reload();
		}, next.sort !== users.sort ? 0 : FILTER_DELAY);
		return () => window.clearTimeout(timer);
	});

	function action(name: string, key: string | null) {
		const user = users.get(key);
		if (!user || key === null) {
			return;
		}
		if (name !== 'delete') {
			selected = user;
		}
		if (name === 'edit') {
			shown = { form: key };
		} else if (name === 'delete') {
			shown = { deletion: key };
		}
	}

	function save(key: string | null, fields: UserFields) {
		const saved = users.save(key, fields, countries);
		shown = null;
		selected = saved.user;
		if (key === null) {
			messages = [{ level: 'info', text: format(texts.added, saved.user.firstName,
				saved.user.lastName) }];
			list?.reload();
		} else {
			list?.renderItem(key, saved.user);
		}
	}

	function remove(key: string) {
		const user = users.get(key)!;
		users.delete(key);
		shown = null;
		messages = [{ level: 'info', text: format(texts.deleted, user.firstName,
			user.lastName) }];
		if (selected?.id === user.id) {
			selected = null;
		}
		list?.reload();
	}
</script>

<Feedback {messages} />
<div class="user-list-demo">
	<div class="user-list-demo__toolbar">
		<label class="user-filter">
			{texts.filterLabel}
			<input type="search" bind:value={filter} placeholder={texts['filter.placeholder']} />
		</label>
		<label class="user-list-demo__sort">
			{texts.sortBy}
			<Dropdown name="sort" options={sorts} bind:value={sort} filter={texts.filter}
				noMatch={texts.noMatch} more={texts.more} remove={texts.remove} />
		</label>
		<button type="button" onclick={() => (shown = { form: null })}
			class="ant-data-table-button ant-data-table-button-primary">{texts.newUser}</button>
	</div>
	<div class="user-list-demo__columns">
		<InfiniteList bind:this={list} load={users.load} template={config.template}
			height={config.height} texts={listTexts} locale={config.locale} onaction={action}
			class="user-list-demo__cards" />
		<aside class="user-list-demo__details">
			{#if selected}
				<fieldset>
					<legend>{texts.selectedUser}</legend>
					<table>
						<tbody>
							{#each [[texts.id, selected.id], [texts.firstName, selected.firstName],
								[texts.lastName, selected.lastName], [texts.homePhone, selected.homePhone],
								[texts.cellPhone, selected.cellPhone],
								[texts.bornDate, selected.born]] as [label, value] (label)}
								<tr><th>{label}</th><td>{value ?? ''}</td></tr>
							{/each}
						</tbody>
					</table>
				</fieldset>
			{:else}
				<p class="user-list-demo__none">{texts.noneSelected}</p>
			{/if}
		</aside>
	</div>
</div>
{#if shown && 'form' in shown}
	{@const key = shown.form}
	{@const user = users.get(key) ?? null}
	<Dialog title={user ? interpolate(texts.editUser, user) : texts.newUser}
		onclose={() => (shown = null)}>
		<UserForm {texts} {user} {countries} onsave={(fields) => save(key, fields)}
			oncancel={() => (shown = null)} />
	</Dialog>
{:else if shown && 'deletion' in shown}
	{@const key = shown.deletion}
	{@const user = users.get(key)}
	{#if user}
		<Dialog title={texts.delete} onclose={() => (shown = null)}>
			<p>{interpolate(texts['delete.confirm'], user)}</p>
			<div class="ant-data-table-overlay-buttons">
				<button type="button" class="ant-data-table-button"
					onclick={() => (shown = null)}>{texts.cancel}</button>
				<button type="button" onclick={() => remove(key)}
					class="ant-data-table-button ant-data-table-button-primary">{texts.delete}</button>
			</div>
		</Dialog>
	{/if}
{/if}
