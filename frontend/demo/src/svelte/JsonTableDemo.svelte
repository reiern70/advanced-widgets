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
  The JSON grid demo in Svelte: an empty element of the Wicket page the app renders into. The users
  are in a DataTable with the columns of the Wicket grid, its rows either a local list of JSON the
  page gave, changed in the browser, or loaded from the users resource, which saves and deletes
  them too. The drop-downs on top change the grid as the settings of the Wicket page do.
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { Button, DataTable, Dropdown } from '@antilia/svelte';

	import { format, loadChoices, type Choice } from '../data.js';
	import { interpolate, type UserFields } from '../json-list.js';
	import { DEFAULT_SETTINGS, download, LocalUsers, matchesName, ResourceUsers, settingDefs,
		simulatedPushes, withSetting, type GridSettings, type JsonTableConfig } from '../json-table.js';
	import Dialog from './Dialog.svelte';
	import Feedback from './Feedback.svelte';
	import type { Message } from './feedback.js';
	import UserForm from './UserForm.svelte';

	/** The time the filter waits for more typing, as the Wicket filter throttles. */
	const FILTER_DELAY = 300;

	let { config }: { config: JsonTableConfig } = $props();

	const texts = $derived(config.texts);
	const remote = untrack(() => new ResourceUsers(config.usersUrl));
	const local = untrack(() => new LocalUsers(config.localUsers));
	const subscribe = untrack(() => simulatedPushes(remote, config.pushMillis));
	let table = $state<ReturnType<typeof DataTable>>();
	let settings = $state<GridSettings>({ ...DEFAULT_SETTINGS });
	let localRows = $state.raw(untrack(() => local.rows));
	let typed = $state('');
	let filter = $state('');
	let countries = $state<Choice[]>([]);
	/** What the dialog shows: the form of a user, new when its key is null, or a deletion. */
	let shown = $state<{ form: string | null } | { deletion: string } | null>(null);
	let messages = $state<Message[]>([]);

	const fromResource = $derived(settings.rowsFrom === 'resource');
	const users = $derived(fromResource ? remote : local);

	$effect(() => {
		loadChoices(config.choicesUrl).then((loaded) => (countries = loaded.countries),
			(error) => console.error(error));
	});

	$effect(() => {
		const next = typed;
		const timer = window.setTimeout(() => (filter = next), FILTER_DELAY);
		return () => window.clearTimeout(timer);
	});

	function failed(error: unknown) {
		messages = [{ level: 'error', text: format(texts.failed,
			error instanceof Error ? error.message : String(error)) }];
	}

	function choose(key: keyof GridSettings, value: string) {
		settings = withSetting(settings, key, value);
	}

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

	async function save(key: string | null, fields: UserFields) {
		try {
			const user = fromResource ? await remote.save(key, fields) :
				local.save(key, fields, countries);
			shown = null;
			messages = [{ level: 'info', text: format(key === null ? texts.added : texts.saved,
				user.firstName, user.lastName) }];
			if (!fromResource) {
				localRows = local.rows;
			} else if (key === null) {
				await table?.reload();
			} else {
				table?.renderRow(key, user);
			}
		} catch (error) {
			failed(error);
		}
	}

	async function remove(key: string) {
		const user = users.get(key)!;
		try {
			if (fromResource) {
				await remote.delete(key);
			} else {
				local.delete(key);
			}
			shown = null;
			messages = [{ level: 'info', text: format(texts.deleted, user.firstName,
				user.lastName) }];
			if (fromResource) {
				await table?.reload();
			} else {
				localRows = local.rows;
			}
		} catch (error) {
			failed(error);
		}
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
			class="ant-data-table-action ant-data-table-icon-action" title={texts.newUser}
			aria-label={texts.newUser}>
			<svg aria-hidden="true" focusable="false" viewBox="0 0 24 24" width="18" height="18"
				fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
				<path d="M12 5v14 M5 12h14" />
			</svg>
		</button>
	</span>
{/snippet}

<Feedback {messages} />
{#if !config.compact}
<div class="ant-dataview-options">
	{#each settingDefs(texts) as setting (setting.key)}
		<div class="ant-dataview-option">
			<span class="ant-dataview-option-title">
				<label for={'svelte-grid-' + setting.key}>{setting.label}</label>
			</span>
			<Dropdown id={'svelte-grid-' + setting.key} name={setting.key}
				value={String(settings[setting.key])} options={setting.options}
				onchange={(value) => choose(setting.key, String(value))}
				filter={texts.filter} noMatch={texts.noMatch} more={texts.more} remove={texts.remove} />
		</div>
	{/each}
</div>
{/if}
<div>
	{#key settings.rowsFrom}
		<DataTable bind:this={table} columns={config.columns}
			loadRows={fromResource ? remote.loadRows : undefined}
			rows={fromResource ? undefined : localRows}
			filterRows={fromResource ? undefined : matchesName}
			subscribe={fromResource ? subscribe : undefined}
			rowsPerPage={config.rowsPerPage} pageSizes={config.pageSizes}
			infinite={settings.infinite} selection={settings.selection}
			selectionCount={settings.selectionCount} navigationPosition={settings.navigation}
			clearSelectionOnNavigation={settings.clearSelectionOnNavigation}
			onaction={action} {filter}
			resizableColumns={settings.resizable ? 'stretch' : false} bodyHeight={config.bodyHeight}
			adjustToParent={config.adjustToParent ?? true} texts={texts.table} {toolbar} {actions} />
	{/key}
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
			<div class="ant-button-bar ant-data-table-overlay-buttons">
				<Button label={texts.cancel} onclick={() => (shown = null)} />
				<Button label={texts.delete} variant="danger" onclick={() => remove(key)} />
			</div>
		</Dialog>
	{/if}
{/if}
