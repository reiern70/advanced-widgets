/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from 'react';

import { Dropdown, Modal } from '@antilia/react';

import { format } from '../data.js';
import { interpolate } from '../json-list.js';
import { MEMBER_FIELDS, TeamApi, type Member, type MemberFields,
	type ModalDemoConfig } from '../team.js';
import { Feedback, type Message } from './Feedback.js';

/** What the dialog shows: the form of a member, new when null, or the deletion of one. */
type Shown = { form: Member | null } | { deletion: Member } | null;

/** The id of the form of a member, which the Save button of the footer submits. */
const FORM_ID = 'react-member-form';

/**
 * The form of a member, in the markup of the Wicket one; the messages of the resource show above
 * the fields, as Wicket's feedback panel shows them. Its buttons are in the footer of the dialog;
 * it tells whether its fields differ from the member's.
 */
function MemberForm({ config, member, errors, onSave, onModified }: { config: ModalDemoConfig;
	member: Member | null; errors: Partial<Record<keyof MemberFields, string>>;
	onSave(fields: MemberFields): void; onModified(modified: boolean): void }) {
	const { texts } = config;
	const [initial] = useState<MemberFields>(() => ({ name: member?.name ?? '',
		email: member?.email ?? '', role: member?.role ?? config.roles[0].value }));
	const [fields, setFields] = useState<MemberFields>(initial);
	const set = (field: keyof MemberFields) => (value: string) =>
		setFields((current) => ({ ...current, [field]: value }));
	const submit = (event: FormEvent) => {
		event.preventDefault();
		onSave(fields);
	};
	useEffect(() => {
		onModified(MEMBER_FIELDS.some((field) => fields[field] !== initial[field]));
	}, [fields, initial, onModified]);
	return (
		<form id={FORM_ID} className="member-form" onSubmit={submit} noValidate>
			<Feedback messages={MEMBER_FIELDS.filter((field) => errors[field])
				.map((field) => ({ level: 'error', text: errors[field]! }))} />
			<label>
				{texts.name}{' '}
				<input type="text" name="name" value={fields.name} autoComplete="name"
					aria-invalid={errors.name ? true : undefined}
					onChange={(event) => set('name')(event.target.value)} />
			</label>
			<label>
				{texts.email}{' '}
				<input type="email" name="email" value={fields.email} autoComplete="email"
					aria-invalid={errors.email ? true : undefined}
					onChange={(event) => set('email')(event.target.value)} />
			</label>
			<label>
				{texts.role}{' '}
				<Dropdown name="role" options={config.roles} value={fields.role}
					onChange={set('role')} filter={texts.filter} noMatch={texts.noMatch}
					more={texts.more} remove={texts.remove} />
			</label>
		</form>
	);
}

/**
 * The modal dialog demo in React: the team of the session, added to and edited in a Modal, a
 * deletion confirmed in one, all through the team resource. Cancel and Save are in the footer;
 * the dialog asks before it closes a form whose fields were changed.
 */
export function ModalDemo({ config }: { config: ModalDemoConfig }) {
	const { texts } = config;
	const api = useMemo(() => new TeamApi(config.teamUrl), [config.teamUrl]);
	const [members, setMembers] = useState<Member[]>([]);
	const [shown, setShown] = useState<Shown>(null);
	const [errors, setErrors] = useState<Partial<Record<keyof MemberFields, string>>>({});
	const [messages, setMessages] = useState<Message[]>([]);

	const reload = useCallback(() => api.list().then(setMembers, (error) => console.error(error)),
		[api]);
	useEffect(() => {
		reload();
	}, [reload]);

	/** Whether the form shown was modified, asked before the dialog closes. */
	const modified = useRef(false);
	const onModified = useCallback((value: boolean) => {
		modified.current = value;
	}, []);

	const open = (next: Shown) => {
		modified.current = false;
		setErrors({});
		setShown(next);
	};
	const gone = () => {
		setShown(null);
		setMessages([{ level: 'error', text: texts.gone }]);
		reload();
	};

	const save = async (fields: MemberFields) => {
		const member = shown && 'form' in shown ? shown.form : null;
		const result = await api.save(member?.id ?? null, fields);
		if ('errors' in result) {
			setErrors(result.errors);
		} else if ('gone' in result) {
			gone();
		} else {
			setShown(null);
			setMessages([{ level: 'info', text: format(member ? texts.saved : texts.added,
				result.member.name) }]);
			reload();
		}
	};

	const remove = async (member: Member) => {
		if (await api.delete(member.id)) {
			setShown(null);
			setMessages([{ level: 'info', text: format(texts.deleted, member.name) }]);
			reload();
		} else {
			gone();
		}
	};

	const formMember = shown && 'form' in shown ? shown.form : undefined;
	const deletion = shown && 'deletion' in shown ? shown.deletion : undefined;
	return (
		<>
			<Feedback messages={messages} />
			<div className="team-demo">
				<div className="team-demo__toolbar">
					<h2 className="team-demo__title">{texts.team}</h2>
					<button type="button"
						className="ant-data-table-button ant-data-table-button-primary"
						onClick={() => open({ form: null })}>{texts.addMember}</button>
				</div>
				<table className="team-demo__table">
					<thead>
						<tr>
							<th scope="col">{texts.name}</th>
							<th scope="col">{texts.email}</th>
							<th scope="col">{texts.role}</th>
							<th scope="col">{texts.actions}</th>
						</tr>
					</thead>
					<tbody>
						{members.map((member) => (
							<tr key={member.id}>
								<td className="team-demo__name">{member.name}</td>
								<td>{member.email}</td>
								<td>{member.roleName}</td>
								<td>
									<div className="team-demo__actions">
										<button type="button" className="ant-data-table-button"
											aria-label={texts.edit + ' ' + member.name}
											onClick={() => open({ form: member })}>{texts.edit}</button>
										<button type="button" className="ant-data-table-button"
											aria-label={texts.delete + ' ' + member.name}
											onClick={() => open({ deletion: member })}>{texts.delete}</button>
									</div>
								</td>
							</tr>
						))}
					</tbody>
				</table>
				{members.length === 0 ? <p className="team-demo__empty">{texts.empty}</p> : null}
			</div>
			<Modal open={formMember !== undefined} onClose={() => setShown(null)} closeOnClick
				canClose={() => !modified.current || window.confirm(texts.discard)}
				closeLabel={texts.close}
				title={formMember ? interpolate(texts.editMember, formMember) : texts.newMember}
				footer={<>
					<button type="button" className="modal-dialog-button" data-modal-close>
						{texts.cancel}</button>
					<button type="submit" form={FORM_ID}
						className="modal-dialog-button modal-dialog-button-primary">{texts.save}</button>
				</>}>
				<MemberForm key={formMember?.id ?? 'new'} config={config} member={formMember ?? null}
					errors={errors} onSave={save} onModified={onModified} />
			</Modal>
			<Modal open={deletion !== undefined} onClose={() => setShown(null)} closeOnClick
				closeLabel={texts.close} title={texts.deleteMember}
				footer={<>
					<button type="button" className="modal-dialog-button" data-modal-close>
						{texts.cancel}</button>
					<button type="button" className="modal-dialog-button modal-dialog-button-primary"
						onClick={() => deletion && remove(deletion)}>{texts.delete}</button>
				</>}>
				{deletion ? interpolate(texts['delete.confirm'], deletion) : null}
			</Modal>
		</>
	);
}
