/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The JSON list demo: the users as cards in an infinite list, filtered and sorted, a click on a
 * card showing the user, its buttons editing and deleting it. The users come from the users
 * resource; what the apps change stays in the browser.
 */

import type { InfiniteListData } from '@antilia/core';

import { loadUsers, type Choice, type DropdownTexts, type User, type UserRow } from './data.js';

/** The texts of the demo, by the keys of the page's bundles. */
export interface JsonListTexts extends DropdownTexts {
	users: string;
	sortBy: string;
	'sort.firstName': string;
	'sort.lastName': string;
	'sort.bornDate': string;
	/** The label of the filter of the users; filter is the drop-downs' own. */
	filterLabel: string;
	'filter.placeholder': string;
	newUser: string;
	/** What the country of a new user shows until one is chosen. */
	chooseOne: string;
	/** The title of the form of a user, ${firstName} and ${lastName} standing for the names. */
	editUser: string;
	delete: string;
	cancel: string;
	/** Whether to delete a user, ${firstName} and ${lastName} standing for the names. */
	'delete.confirm': string;
	/** That a user was deleted, {0} and {1} standing for the names. */
	deleted: string;
	/** That a user was added, {0} and {1} standing for the names. */
	added: string;
	noneSelected: string;
	selectedUser: string;
	id: string;
	firstName: string;
	lastName: string;
	homePhone: string;
	cellPhone: string;
	bornDate: string;
	country: string;
	city: string;
	address: string;
	save: string;
	/** That a field is required, {0} standing for its label, as Wicket says it. */
	required: string;
	/** What the list says below the cards; ${count} stands for the number of users. */
	loading: string;
	end: string;
	empty: string;
}

/** What the page gives the demo. */
export interface JsonListConfig {
	texts: JsonListTexts;
	/** The template of a card, in the language of the session. */
	template: string;
	/** The users, by first, count, filter and sort. */
	usersUrl: string;
	/** The choices, whose countries the form of a user offers. */
	choicesUrl: string;
	/** The number of users loaded at a time. */
	perPage: number;
	/** The property the users are sorted by at first. */
	sort: string;
	/** The properties the users can be sorted by. */
	sorts: string[];
	/** The height of the list. */
	height: { value: number; unit: string };
	/** The locale numbers are written in. */
	locale: string;
}

/** The fields of the form of a user, all of them required. */
export const FIELDS = ['firstName', 'lastName', 'country', 'city', 'address'] as const;

/** What the form of a user edits. */
export type UserFields = Record<(typeof FIELDS)[number], string>;

/** The longest text a field takes, as the Wicket form has it. */
export const MAX_LENGTH: Record<(typeof FIELDS)[number], number> = {
	firstName: 60, lastName: 60, country: 2, city: 60, address: 120
};

/**
 * @param pattern a text with ${name} standing for the properties of an object, as Wicket's
 *            resources have
 * @returns the text with the properties in place
 */
export function interpolate(pattern: string, values: object): string {
	return pattern.replace(/\$\{(\w+)\}/g, (match, name: string) =>
		name in values ? String((values as Record<string, unknown>)[name]) : match);
}

/**
 * @returns the fields of the form of a user, empty for a new one
 */
export function fieldsOf(user: User | null): UserFields {
	return {
		firstName: user?.firstName ?? '',
		lastName: user?.lastName ?? '',
		country: user?.country ?? '',
		city: user?.city ?? '',
		address: user?.address ?? ''
	};
}

/**
 * @returns the messages of the fields left empty, as Wicket's feedback has them
 */
export function validate(texts: Pick<JsonListTexts, (typeof FIELDS)[number] | 'required'>,
	fields: UserFields): Partial<UserFields> {
	const errors: Partial<UserFields> = {};
	for (const field of FIELDS) {
		if (!fields[field].trim()) {
			errors[field] = texts.required.replace('{0}', texts[field]);
		}
	}
	return errors;
}

/** The texts of the form of a user. */
export type UserFormTexts = Pick<JsonListTexts, 'firstName' | 'lastName' | 'country' | 'city' |
	'address' | 'save' | 'cancel' | 'required' | 'filter' | 'noMatch' | 'more' | 'remove' |
	'chooseOne'>;

/**
 * What the app changed of the users the users resource sends: edited, deleted and added users,
 * kept in the browser.
 */
export class UserChanges {
	private readonly edited = new Map<string, User>();

	/** The users deleted, by their keys. */
	private readonly deleted = new Map<string, User>();

	private added: UserRow[] = [];

	private readonly known = new Map<string, User>();

	private nextId = 1;

	/**
	 * @param rows the rows the users resource sent
	 * @param filter the filter they were loaded with
	 * @param withAdded whether the users added come first, as on the first page
	 * @returns the rows with the changes applied
	 */
	apply(rows: UserRow[], filter: string, withAdded: boolean): UserRow[] {
		const changed = (withAdded ? this.added.filter((row) => matches(row.data, filter)) : [])
			.concat(rows.filter((row) => !this.deleted.has(row.key))
				.map((row) => ({ ...row, data: this.edited.get(row.key) ?? row.data })));
		changed.forEach((row) => this.known.set(row.key, row.data));
		return changed;
	}

	/**
	 * @returns the number of users there are with the filter, those added counted and those
	 *          deleted not
	 */
	itemCount(sent: number, filter: string): number {
		return sent + this.added.filter((row) => matches(row.data, filter)).length -
			Array.from(this.deleted.values()).filter((user) => matches(user, filter)).length;
	}

	/**
	 * @returns the user of a key shown
	 */
	get(key: string | null): User | undefined {
		return key === null ? undefined : this.known.get(key);
	}

	/**
	 * Changes a user shown, as a pushed change does.
	 */
	update(key: string, user: User): void {
		if (this.added.some((row) => row.key === key)) {
			this.added = this.added.map((row) => row.key === key ? { ...row, data: user } : row);
		} else {
			this.edited.set(key, user);
		}
		this.known.set(key, user);
	}

	/**
	 * Saves the fields of a user: a new one is added first, the others are changed in place.
	 *
	 * @param key the key of the user, null for a new one
	 * @param countries the countries, which name the country of the user
	 * @returns the key and the user saved
	 */
	save(key: string | null, fields: UserFields, countries: Choice[]): { key: string; user: User } {
		const user: User = {
			...(key !== null ? this.known.get(key) : { id: 0, cellPhone: '', homePhone: '',
				born: '', progress: 0 }),
			...fields,
			countryName: countries.find((country) => country.value === fields.country)?.label ??
				fields.country,
			initials: (fields.firstName.charAt(0) + fields.lastName.charAt(0)).toUpperCase()
		} as User;
		if (key === null) {
			user.id = Math.max(1000, ...Array.from(this.known.values(), (known) => known.id)) +
				this.nextId++;
			key = String(user.id);
			this.added.unshift({ key, number: 1, data: user });
			this.known.set(key, user);
		} else {
			this.update(key, user);
		}
		return { key, user };
	}

	/**
	 * Deletes a user: the users are shown without it from now on.
	 */
	delete(key: string): void {
		if (this.added.some((row) => row.key === key)) {
			this.added = this.added.filter((row) => row.key !== key);
		} else if (this.known.has(key)) {
			this.deleted.set(key, this.known.get(key)!);
		}
		this.known.delete(key);
	}
}

/**
 * @returns whether the first or the last name of a user contains the filter, as the users
 *          resource filters them
 */
function matches(user: User, filter: string): boolean {
	const lower = filter.toLowerCase();
	return !lower || user.firstName.toLowerCase().includes(lower) ||
		user.lastName.toLowerCase().includes(lower);
}

/**
 * The users of the list: loaded page by page from the users resource, with what the app changed
 * applied, users added first. The list shows what load gives it.
 */
export class Users extends UserChanges {
	/** The filter and the sort the users are loaded with. */
	filter = '';

	sort: string;

	private shownOfServer = 0;

	/** The number of users the list shows, the last one's number. */
	private shown = 0;

	/** Whether the next load starts over, scrolled to the top. */
	private startOver = false;

	constructor(private readonly url: string, private readonly perPage: number, sort: string) {
		super();
		this.sort = sort;
	}

	/**
	 * Starts again from the first page, as after another filter or sort.
	 */
	reset(): void {
		this.shownOfServer = 0;
		this.startOver = true;
	}

	/**
	 * Loads the users shown, or the next page of them, for the list.
	 */
	readonly load = async (more: boolean): Promise<InfiniteListData> => {
		const first = more ? this.shownOfServer : 0;
		const rows = await loadUsers(this.url, { first, filter: this.filter, sort: this.sort,
			count: more ? this.perPage : Math.max(this.shownOfServer, this.perPage) });
		this.shownOfServer = first + rows.rows.length;
		const items = this.apply(rows.rows, this.filter, !more);
		const offset = more ? this.shown : 0;
		this.shown = offset + items.length;
		const top = this.startOver;
		this.startOver = false;
		return {
			items: items.map((row, index) => ({ ...row, number: offset + index + 1 })),
			itemCount: this.itemCount(rows.itemCount, this.filter),
			top
		};
	};
}
