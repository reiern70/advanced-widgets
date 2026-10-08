/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { useEffect, useMemo, useRef, useState } from 'react';

import { DataTable, Dropdown, type DataTableHandle } from '@advanced-widgets/react';

import { format, loadChoices, type Choice } from '../data.js';
import { interpolate, type UserFields } from '../json-list.js';
import { download, simulatedPushes, TableUsers, type JsonTableConfig } from '../json-table.js';
import { Dialog } from './Dialog.js';
import { Feedback, type Message } from './Feedback.js';
import { UserForm } from './UserForm.js';

/** What the dialog shows: the form of a user, new when its key is null, or a deletion. */
type Shown = { form: string | null } | { deletion: string } | null;

/** The time the filter waits for more typing, as the Wicket filter throttles. */
const FILTER_DELAY = 300;

function AddIcon() {
	return (
		<svg aria-hidden="true" focusable="false" viewBox="0 0 24 24" width="18" height="18"
			fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
			<path d="M12 5v14 M5 12h14" />
		</svg>
	);
}

/**
 * The JSON table demo in React: the users in a DataTable, from the users resource, with the
 * columns of the Wicket table; the actions of a row edit, delete and download the user in the
 * app, which keeps what it changes in the browser. The progress of the users shown advances as if
 * the server pushed it.
 */
export function JsonTableDemo({ config }: { config: JsonTableConfig }) {
	const { texts } = config;
	const users = useMemo(() => new TableUsers(config.usersUrl), [config.usersUrl]);
	const subscribe = useMemo(() => simulatedPushes(users, config.pushMillis),
		[users, config.pushMillis]);
	const table = useRef<DataTableHandle>(null);
	const [typed, setTyped] = useState('');
	const [filter, setFilter] = useState('');
	const [infinite, setInfinite] = useState(false);
	const [selected, setSelected] = useState<string[]>([]);
	const [countries, setCountries] = useState<Choice[]>([]);
	const [shown, setShown] = useState<Shown>(null);
	const [messages, setMessages] = useState<Message[]>([]);

	useEffect(() => {
		loadChoices(config.choicesUrl).then((choices) => setCountries(choices.countries),
			(error) => console.error(error));
	}, [config.choicesUrl]);

	useEffect(() => {
		const timer = window.setTimeout(() => setFilter(typed), FILTER_DELAY);
		return () => window.clearTimeout(timer);
	}, [typed]);

	const action = (name: string, key: string | null) => {
		const user = users.get(key);
		if (!user || key === null) {
			return;
		}
		if (name === 'edit') {
			setShown({ form: key });
		} else if (name === 'delete') {
			setShown({ deletion: key });
		} else if (name === 'download') {
			download(user);
		}
	};

	const save = (key: string | null, fields: UserFields) => {
		const saved = users.save(key, fields, countries);
		setShown(null);
		if (key === null) {
			setMessages([{ level: 'info', text: format(texts.added, saved.user.firstName,
				saved.user.lastName) }]);
			table.current?.reload();
		} else {
			table.current?.renderRow(key, saved.user);
		}
	};

	const remove = (key: string) => {
		const user = users.get(key)!;
		users.delete(key);
		setShown(null);
		setMessages([{ level: 'info', text: format(texts.deleted, user.firstName,
			user.lastName) }]);
		setSelected((current) => current.filter((selectedKey) => selectedKey !== key));
		table.current?.reload();
	};

	const dialogUser = shown && 'form' in shown ? users.get(shown.form) ?? null : null;
	const deletion = shown && 'deletion' in shown ? users.get(shown.deletion) : undefined;

	return (
		<>
			<Feedback messages={messages} />
			<p className="dynamic-dataview-options">
				<label>
					{texts.infiniteScroll}{' '}
					<Dropdown name="infiniteScroll" value={String(infinite)}
						onChange={(value) => setInfinite(value === 'true')}
						options={[{ value: 'false', label: texts['infiniteScroll.off'] },
							{ value: 'true', label: texts['infiniteScroll.on'] }]}
						filter={texts.filter} noMatch={texts.noMatch} more={texts.more}
						remove={texts.remove} />
				</label>
			</p>
			<div>
				<DataTable ref={table} columns={config.columns} loadRows={users.loadRows}
					rowsPerPage={config.rowsPerPage} pageSizes={config.pageSizes} infinite={infinite}
					selection="many" onSelectionChange={setSelected} onAction={action} filter={filter}
					resizableColumns="stretch" bodyHeight={config.bodyHeight} adjustToParent
					texts={texts.table} subscribe={subscribe} licenseKey={config.licenseKey}
					toolbar={
						<label className="user-filter">
							{texts.filterLabel}{' '}
							<input type="search" value={typed} placeholder={texts['filter.placeholder']}
								onChange={(event) => setTyped(event.target.value)} />
						</label>
					}
					actions={
						<span>
							<button type="button" onClick={() => setShown({ form: null })}
								className="dynamic-data-table-action dynamic-data-table-icon-action"
								title={texts.newUser} aria-label={texts.newUser}><AddIcon /></button>
						</span>
					} />
			</div>
			<p className="dynamic-dataview-selection">
				{selected.length ? format(texts['selection.some'], selected.length) :
					texts['selection.none']}
			</p>
			{shown && 'form' in shown && (
				<Dialog title={dialogUser ? interpolate(texts.editUser, dialogUser) : texts.newUser}
					onClose={() => setShown(null)}>
					<UserForm texts={texts} user={dialogUser} countries={countries}
						onSave={(fields) => save(shown.form, fields)} onCancel={() => setShown(null)} />
				</Dialog>
			)}
			{deletion && shown && 'deletion' in shown && (
				<Dialog title={texts.delete} onClose={() => setShown(null)}>
					<p>{interpolate(texts['delete.confirm'], deletion)}</p>
					<div className="dynamic-data-table-overlay-buttons">
						<button type="button" className="dynamic-data-table-button"
							onClick={() => setShown(null)}>{texts.cancel}</button>
						<button type="button" onClick={() => remove(shown.deletion)}
							className="dynamic-data-table-button dynamic-data-table-button-primary">
							{texts.delete}
						</button>
					</div>
				</Dialog>
			)}
		</>
	);
}
