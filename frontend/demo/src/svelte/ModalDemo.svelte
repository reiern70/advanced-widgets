<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  The modal dialog demo in Svelte: the team of the session, added to and edited in a Modal, a
  deletion confirmed in one, all through the team resource. Cancel and Save are in the footer; the
  dialog asks before it closes a form whose fields were changed.
-->
<script lang="ts">
	import { Dropdown, Modal } from '@advanced-widgets/svelte';

	import { format } from '../data.js';
	import { interpolate } from '../json-list.js';
	import { MEMBER_FIELDS, TeamApi, type Member, type MemberFields,
		type ModalDemoConfig } from '../team.js';
	import Feedback from './Feedback.svelte';
	import type { Message } from './feedback.js';

	let { config }: { config: ModalDemoConfig } = $props();
	const texts = $derived(config.texts);
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
		<button type="button" class="dynamic-data-table-button dynamic-data-table-button-primary"
			onclick={() => openForm(null)}>{texts.addMember}</button>
	</div>
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
					<td class="team-demo__actions">
						<button type="button" class="dynamic-data-table-button"
							aria-label={texts.edit + ' ' + member.name}
							onclick={() => openForm(member)}>{texts.edit}</button>
						<button type="button" class="dynamic-data-table-button"
							aria-label={texts.delete + ' ' + member.name}
							onclick={() => (deletion = member)}>{texts.delete}</button>
					</td>
				</tr>
			{/each}
		</tbody>
	</table>
	{#if members.length === 0}<p class="team-demo__empty">{texts.empty}</p>{/if}
</div>

<Modal open={formMember !== undefined} onclose={close} closeOnClick closeLabel={texts.close}
	canClose={() => !modified() || window.confirm(texts.discard)}
	title={formMember ? interpolate(texts.editMember, formMember) : texts.newMember}>
	{#snippet footer()}
		<button type="button" class="modal-dialog-button" data-modal-close>{texts.cancel}</button>
		<button type="submit" form={FORM_ID} class="modal-dialog-button modal-dialog-button-primary">
			{texts.save}</button>
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
	title={texts.deleteMember}>
	{#snippet footer()}
		<button type="button" class="modal-dialog-button" data-modal-close>{texts.cancel}</button>
		<button type="button" class="modal-dialog-button modal-dialog-button-primary"
			onclick={() => deletion && remove(deletion)}>{texts.delete}</button>
	{/snippet}
	{deletion ? interpolate(texts['delete.confirm'], deletion) : ''}
</Modal>
