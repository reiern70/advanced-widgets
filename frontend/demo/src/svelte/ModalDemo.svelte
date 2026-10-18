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
  The modal dialog demo in Svelte: the team of the session, added to and edited in a Modal, a
  deletion confirmed in one, all through the team resource. Cancel and Save are in the footer; the
  dialog asks before it closes a form whose fields were changed. Below the team, the options
  example opens dialogs with the options chosen, nested three deep.
-->
<script lang="ts">
	import { Button, Dropdown, Modal } from '@antilia/svelte';

	import { format } from '../data.js';
	import { interpolate } from '../json-list.js';
	import { MEMBER_FIELDS, TeamApi, type Member, type MemberFields,
		type ModalDemoConfig } from '../team.js';
	import DialogOptionsDemo from './DialogOptionsDemo.svelte';
	import Feedback from './Feedback.svelte';
	import type { Message } from './feedback.js';

	let { config }: { config: ModalDemoConfig } = $props();
	const texts = $derived(config.texts);
	const icons = $derived(config.icons);
	const api = $derived(new TeamApi(config.teamUrl));

	let members = $state<Member[]>([]);
	/** The member of the form, null for a new one, undefined while the form is closed. */
	let formMember = $state<Member | null | undefined>(undefined);
	let deletion = $state<Member | undefined>(undefined);
	let fields = $state<MemberFields>({ name: '', email: '', role: '' });
	/** The fields of the member when the form opened, to tell whether they were changed. */
	let initial: MemberFields = { name: '', email: '', role: '' };

	/** The id of the form of a member, which the Save button of the footer submits. */
	const FORM_ID = 'svelte-member-form';

	function modified(): boolean {
		return MEMBER_FIELDS.some((field) => fields[field] !== initial[field]);
	}
	let errors = $state<Partial<Record<keyof MemberFields, string>>>({});
	let messages = $state<Message[]>([]);

	function reload(): void {
		api.list().then((loaded) => (members = loaded), (error) => console.error(error));
	}

	$effect(() => {
		reload();
	});

	function openForm(member: Member | null): void {
		errors = {};
		initial = { name: member?.name ?? '', email: member?.email ?? '',
			role: member?.role ?? config.roles[0].value };
		fields = { ...initial };
		formMember = member;
	}

	function close(): void {
		formMember = undefined;
		deletion = undefined;
	}

	function gone(): void {
		close();
		messages = [{ level: 'error', text: texts.gone }];
		reload();
	}

	async function save(event: SubmitEvent): Promise<void> {
		event.preventDefault();
		const member = formMember ?? null;
		const result = await api.save(member?.id ?? null, $state.snapshot(fields));
		if ('errors' in result) {
			errors = result.errors;
		} else if ('gone' in result) {
			gone();
		} else {
			close();
			messages = [{ level: 'info', text: format(member ? texts.saved : texts.added,
				result.member.name) }];
			reload();
		}
	}

	async function remove(member: Member): Promise<void> {
		if (await api.delete(member.id)) {
			close();
			messages = [{ level: 'info', text: format(texts.deleted, member.name) }];
			reload();
		} else {
			gone();
		}
	}
</script>

<Feedback {messages} />
<div class="team-demo">
	<div class="team-demo__toolbar">
		<h2 class="team-demo__title">{texts.team}</h2>
		<Button variant="primary" size="small" label={texts.addMember} onclick={() => openForm(null)}>
			{#snippet icon()}{@html icons.add}{/snippet}
		</Button>
	</div>
	<div class="team-demo__scroll">
		<table class="team-demo__table">
			<thead>
				<tr>
					<th scope="col">{texts.name}</th>
					<th scope="col">{texts.email}</th>
					<th scope="col">{texts.role}</th>
					<th scope="col">{texts.actions}</th>
				</tr>
			</thead>
			<tbody>
				{#each members as member (member.id)}
					<tr>
						<td class="team-demo__name">{member.name}</td>
						<td>{member.email}</td>
						<td>{member.roleName}</td>
						<td>
							<div class="team-demo__actions">
								<Button size="small" label={texts.edit} aria-label={texts.edit + ' ' + member.name}
									onclick={() => openForm(member)}>
									{#snippet icon()}{@html icons.edit}{/snippet}
								</Button>
								<Button size="small" label={texts.delete}
									aria-label={texts.delete + ' ' + member.name}
									onclick={() => (deletion = member)}>
									{#snippet icon()}{@html icons.delete}{/snippet}
								</Button>
							</div>
						</td>
					</tr>
				{/each}
			</tbody>
		</table>
	</div>
	{#if members.length === 0}<p class="team-demo__empty">{texts.empty}</p>{/if}
</div>

<Modal open={formMember !== undefined} onclose={close} closeOnClick closeLabel={texts.close}
	size="medium" canClose={() => !modified() || window.confirm(texts.discard)}
	title={formMember ? interpolate(texts.editMember, formMember) : texts.newMember}>
	{#snippet footer()}
		<Button size="small" label={texts.cancel} data-modal-close>
			{#snippet icon()}{@html icons.cancel}{/snippet}
		</Button>
		<Button type="submit" form={FORM_ID} size="small" variant="primary" label={texts.save}>
			{#snippet icon()}{@html icons.save}{/snippet}
		</Button>
	{/snippet}
	<form id={FORM_ID} class="member-form" onsubmit={save} novalidate>
		<Feedback messages={MEMBER_FIELDS.filter((field) => errors[field])
			.map((field) => ({ level: 'error' as const, text: errors[field]! }))} />
		<label>
			{texts.name}
			<input type="text" name="name" bind:value={fields.name} autocomplete="name"
				aria-invalid={errors.name ? true : undefined} />
		</label>
		<label>
			{texts.email}
			<input type="email" name="email" bind:value={fields.email} autocomplete="email"
				aria-invalid={errors.email ? true : undefined} />
		</label>
		<label>
			{texts.role}
			<Dropdown name="role" options={config.roles} bind:value={fields.role}
				filter={texts.filter} noMatch={texts.noMatch} more={texts.more}
				remove={texts.remove} />
		</label>
	</form>
</Modal>

<Modal open={deletion !== undefined} onclose={close} closeOnClick closeLabel={texts.close}
	size="small" title={texts.deleteMember}>
	{#snippet footer()}
		<Button size="small" label={texts.cancel} data-modal-close>
			{#snippet icon()}{@html icons.cancel}{/snippet}
		</Button>
		<Button size="small" variant="danger" label={texts.delete}
			onclick={() => deletion && remove(deletion)}>
			{#snippet icon()}{@html icons.delete}{/snippet}
		</Button>
	{/snippet}
	{deletion ? interpolate(texts['delete.confirm'], deletion) : ''}
</Modal>

<DialogOptionsDemo {config} />
