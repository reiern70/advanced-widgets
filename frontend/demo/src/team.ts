/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The modal dialog demo: the team of the visitor's session, added to, edited and deleted in a
 * Modal through the team resource, which validates as the Wicket form does.
 */

import type { Choice, DropdownTexts } from './data.js';

/** A member of the team, as the team resource sends it. */
export interface Member {
	id: number;
	name: string;
	email: string;
	role: string;
	/** The name of the role, in the language of the session. */
	roleName: string;
}

/** What the form of a member edits. */
export interface MemberFields {
	name: string;
	email: string;
	role: string;
}

/** The texts of the demo, by the keys of the page's bundles. */
export interface ModalDemoTexts extends DropdownTexts {
	team: string;
	addMember: string;
	newMember: string;
	/** The title of the form of a member, ${name} standing for the name. */
	editMember: string;
	edit: string;
	delete: string;
	deleteMember: string;
	/** Whether to delete a member, ${name} standing for the name. */
	'delete.confirm': string;
	save: string;
	cancel: string;
	name: string;
	email: string;
	role: string;
	actions: string;
	/** That a member was added, saved or deleted, {0} standing for the name. */
	added: string;
	saved: string;
	deleted: string;
	gone: string;
	empty: string;
}

/** What the page gives the demo. */
export interface ModalDemoConfig {
	texts: ModalDemoTexts;
	/** The roles of a member, named in the language of the session. */
	roles: Choice[];
	/** The team resource. */
	teamUrl: string;
}

/** The answer of the team resource to a save: the member saved, or the messages of the fields. */
export type SaveResult = { member: Member } | { errors: Partial<Record<keyof MemberFields, string>> }
	| { gone: true };

/**
 * The team resource: list, create, update and delete.
 */
export class TeamApi {
	constructor(private readonly url: string) {}

	private at(id: number): string {
		return this.url + (this.url.indexOf('?') >= 0 ? '&' : '?') + 'id=' + id;
	}

	async list(): Promise<Member[]> {
		const response = await fetch(this.url, { headers: { Accept: 'application/json' },
			credentials: 'same-origin' });
		if (!response.ok) {
			throw new Error(`Loading the team failed: ${response.status}`);
		}
		return ((await response.json()) as { members: Member[] }).members;
	}

	/**
	 * Saves a member: a new one if id is null.
	 */
	async save(id: number | null, fields: MemberFields): Promise<SaveResult> {
		const response = await fetch(id === null ? this.url : this.at(id), {
			method: id === null ? 'POST' : 'PUT',
			credentials: 'same-origin',
			headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
			body: JSON.stringify(fields)
		});
		if (response.status === 404) {
			return { gone: true };
		}
		if (response.status === 400) {
			return (await response.json()) as SaveResult;
		}
		if (!response.ok) {
			throw new Error(`Saving the member failed: ${response.status}`);
		}
		return (await response.json()) as SaveResult;
	}

	/**
	 * @returns whether the member was there, deleted now
	 */
	async delete(id: number): Promise<boolean> {
		const response = await fetch(this.at(id), { method: 'DELETE',
			credentials: 'same-origin' });
		if (response.status === 404) {
			return false;
		}
		if (!response.ok) {
			throw new Error(`Deleting the member failed: ${response.status}`);
		}
		return true;
	}
}

/** The fields of the form, in the order of the Wicket one, whose messages show in this order. */
export const MEMBER_FIELDS = ['name', 'email', 'role'] as const;
