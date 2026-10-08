<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  The JSON table demo in Svelte: the users in a DataTable, from the users resource, with the
  columns of the Wicket table; the actions of a row edit, delete and download the user in the app,
  which keeps what it changes in the browser. The progress of the users shown advances as if the
  server pushed it.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { DataTable, Dropdown } from '@advanced-widgets/svelte';

	import { format, loadChoices, type Choice } from '../data.js';
	import { interpolate, type UserFields } from '../json-list.js';
	import { download, simulatedPushes, TableUsers, type JsonTableConfig } from '../json-table.js';
	import Dialog from './Dialog.svelte';
	import Feedback from './Feedback.svelte';
	import type { Message } from './feedback.js';
	import UserForm from './UserForm.svelte';

	/** The time the filter waits for more typing, as the Wicket filter throttles. */
	const FILTER_DELAY = 300;

	let { config }: { config: JsonTableConfig } = $props();

	const texts = $derived(config.texts);
	const users = untrack(() => new TableUsers(config.usersUrl));
	const subscribe = untrack(() => simulatedPushes(users, config.pushMillis));
	let table = $state<ReturnType<typeof DataTable>>();
	let typed = $state('');
	let filter = $state('');
	let infinite = $state('false');
	let selected = $state<string[]>([]);
	let countries = $state<Choice[]>([]);
	/** What the dialog shows: the form of a user, new when its key is null, or a deletion. */
	let shown = $state<{ form: string | null } | { deletion: string } | null>(null);
	let messages = $state<Message[]>([]);

	$effect(() => {
		loadChoices(config.choicesUrl).then((loaded) => (countries = loaded.countries),
			(error) => console.error(error));
	});

	$effect(() => {
		const next = typed;
		const timer = window.setTimeout(() => (filter = next), FILTER_DELAY);
		return () => window.clearTimeout(timer);
	});

	function action(name: string, key: string | null) {
		const user = users.get(key);
		if (!user || key === null) {
			return;
		}
		if (name === 'edit') {
			shown = { form: key };
		} else if (name === 'delete') {
			shown = { deletion: key };
		} else if (name === 'download') {
			download(user);
		}
	}

	function save(key: string | null, fields: UserFields) {
		const saved = users.save(key, fields, countries);
		shown = null;
		if (key === null) {
			messages = [{ level: 'info', text: format(texts.added, saved.user.firstName,
				saved.user.lastName) }];
			table?.reload();
		} else {
			table?.renderRow(key, saved.user);
		}
	}

	function remove(key: string) {
		const user = users.get(key)!;
		users.delete(key);
		shown = null;
		messages = [{ level: 'info', text: format(texts.deleted, user.firstName,
			user.lastName) }];
		selected = selected.filter((selectedKey) => selectedKey !== key);
		table?.reload();
	}
</script>

{#snippet toolbar()}
	<label class="user-filter">
		{texts.filterLabel}
		<input type="search" bind:value={typed} placeholder={texts['filter.placeholder']} />
	</label>
{/snippet}

{#snippet actions()}
	<span>
		<button type="button" onclick={() => (shown = { form: null })}
			class="dynamic-data-table-action dynamic-data-table-icon-action" title={texts.newUser}
			aria-label={texts.newUser}>
			<svg aria-hidden="true" focusable="false" viewBox="0 0 24 24" width="18" height="18"
				fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
				<path d="M12 5v14 M5 12h14" />
			</svg>
		</button>
	</span>
{/snippet}

<Feedback {messages} />
<p class="dynamic-dataview-options">
	<label>
		{texts.infiniteScroll}
		<Dropdown name="infiniteScroll" bind:value={infinite}
			options={[{ value: 'false', label: texts['infiniteScroll.off'] },
				{ value: 'true', label: texts['infiniteScroll.on'] }]}
			filter={texts.filter} noMatch={texts.noMatch} more={texts.more} remove={texts.remove} />
	</label>
</p>
<div>
	<DataTable bind:this={table} columns={config.columns} loadRows={users.loadRows}
		rowsPerPage={config.rowsPerPage} pageSizes={config.pageSizes} infinite={infinite === 'true'}
		selection="many" onselectionchange={(keys) => (selected = keys)} onaction={action} {filter}
		resizableColumns="stretch" bodyHeight={config.bodyHeight} adjustToParent
		texts={texts.table} {subscribe} licenseKey={config.licenseKey} {toolbar} {actions} />
</div>
<p class="dynamic-dataview-selection">
	{selected.length ? format(texts['selection.some'], selected.length) : texts['selection.none']}
</p>
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
			<div class="dynamic-data-table-overlay-buttons">
				<button type="button" class="dynamic-data-table-button"
					onclick={() => (shown = null)}>{texts.cancel}</button>
				<button type="button" onclick={() => remove(key)}
					class="dynamic-data-table-button dynamic-data-table-button-primary">{texts.delete}</button>
			</div>
		</Dialog>
	{/if}
{/if}
