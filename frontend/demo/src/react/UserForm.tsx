/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { useState, type FormEvent } from 'react';

import { Button, Dropdown } from '@antilia/react';

import type { Choice, User } from '../data.js';
import { FIELDS, fieldsOf, MAX_LENGTH, validate, type UserFields,
	type UserFormTexts } from '../json-list.js';
import { Feedback } from './Feedback.js';

/**
 * The form of a user, every field required, in the markup of the Wicket one.
 */
export function UserForm({ texts, user, countries, onSave, onCancel }: { texts: UserFormTexts;
	user: User | null; countries: Choice[]; onSave(fields: UserFields): void; onCancel(): void }) {
	const [fields, setFields] = useState(() => fieldsOf(user));
	// a new user has no country until one is chosen, rather than the first of the list
	const [choices] = useState(() => user?.country ? countries :
		[{ value: '', label: texts.chooseOne }, ...countries]);
	const [errors, setErrors] = useState<Partial<UserFields>>({});
	const submit = (event: FormEvent) => {
		event.preventDefault();
		const found = validate(texts, fields);
		setErrors(found);
		if (!Object.keys(found).length) {
			onSave(fields);
		}
	};
	const set = (field: keyof UserFields) => (value: string) =>
		setFields((current) => ({ ...current, [field]: value }));
	const input = (field: Exclude<keyof UserFields, 'country'>, autoComplete: string) => (
		<label>
			{texts[field]}{' '}
			<input type="text" name={field} value={fields[field]} maxLength={MAX_LENGTH[field]}
				autoComplete={autoComplete} aria-invalid={errors[field] ? true : undefined}
				onChange={(event) => set(field)(event.target.value)} />
		</label>
	);
	return (
		<form className="user-edit" onSubmit={submit} noValidate>
			<Feedback messages={FIELDS.filter((field) => errors[field])
				.map((field) => ({ level: 'error', text: errors[field]! }))} />
			{input('firstName', 'given-name')}
			{input('lastName', 'family-name')}
			<label>
				{texts.country}{' '}
				<Dropdown name="country" options={choices} value={fields.country}
					onChange={set('country')} aria-invalid={errors.country ? true : undefined}
					filter={texts.filter} noMatch={texts.noMatch} more={texts.more}
					remove={texts.remove} />
			</label>
			{input('city', 'address-level2')}
			{input('address', 'street-address')}
			<div className="ant-button-bar ant-data-table-overlay-buttons">
				<Button label={texts.cancel} onClick={onCancel} />
				<Button type="submit" label={texts.save} variant="primary" />
			</div>
		</form>
	);
}
