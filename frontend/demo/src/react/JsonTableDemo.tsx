/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { useEffect, useMemo, useRef, useState } from 'react';

import { Button, DataTable, Dropdown, type DataTableHandle } from '@antilia/react';

import { format, loadChoices, type Choice } from '../data.js';
import { interpolate, type UserFields } from '../json-list.js';
import { DEFAULT_SETTINGS, download, LocalUsers, matchesName, ResourceUsers, settingDefs,
	simulatedPushes, withSetting, type JsonTableConfig } from '../json-table.js';
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
 * The JSON grid demo in React: an empty element of the Wicket page the app renders into. The
 * users are in a DataTable with the columns of the Wicket grid, its rows either a local list of
 * JSON the page gave, changed in the browser, or loaded from the users resource, which saves and
 * deletes them too. The drop-downs on top change the grid as the settings of the Wicket page do.
 */
export function JsonTableDemo({ config }: { config: JsonTableConfig }) {
	const { texts } = config;
	const remote = useMemo(() => new ResourceUsers(config.usersUrl), [config.usersUrl]);
	const local = useMemo(() => new LocalUsers(config.localUsers), [config.localUsers]);
	const subscribe = useMemo(() => simulatedPushes(remote, config.pushMillis),
		[remote, config.pushMillis]);
	const table = useRef<DataTableHandle>(null);
	const [settings, setSettings] = useState(DEFAULT_SETTINGS);
	const [localRows, setLocalRows] = useState(local.rows);
	const [typed, setTyped] = useState('');
	const [filter, setFilter] = useState('');
	const [countries, setCountries] = useState<Choice[]>([]);
	const [shown, setShown] = useState<Shown>(null);
	const [messages, setMessages] = useState<Message[]>([]);
	const fromResource = settings.rowsFrom === 'resource';
	const users = fromResource ? remote : local;

	useEffect(() => {
		loadChoices(config.choicesUrl).then((choices) => setCountries(choices.countries),
			(error) => console.error(error));
	}, [config.choicesUrl]);

	useEffect(() => {
		const timer = window.setTimeout(() => setFilter(typed), FILTER_DELAY);
		return () => window.clearTimeout(timer);
	}, [typed]);

	const failed = (error: unknown) => setMessages([{ level: 'error',
		text: format(texts.failed, error instanceof Error ? error.message : String(error)) }]);

	const choose = (key: keyof typeof settings, value: string) =>
		setSettings((current) => withSetting(current, key, value));

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

	const save = async (key: string | null, fields: UserFields) => {
		try {
			const user = fromResource ? await remote.save(key, fields) :
				local.save(key, fields, countries);
			setShown(null);
			setMessages([{ level: 'info', text: format(key === null ? texts.added : texts.saved,
				user.firstName, user.lastName) }]);
			if (!fromResource) {
				setLocalRows(local.rows);
			} else if (key === null) {
				await table.current?.reload();
			} else {
				table.current?.renderRow(key, user);
			}
		} catch (error) {
			failed(error);
		}
	};

	const remove = async (key: string) => {
		const user = users.get(key)!;
		try {
			if (fromResource) {
				await remote.delete(key);
			} else {
				local.delete(key);
			}
			setShown(null);
			setMessages([{ level: 'info', text: format(texts.deleted, user.firstName,
				user.lastName) }]);
			if (fromResource) {
				await table.current?.reload();
			} else {
				setLocalRows(local.rows);
			}
		} catch (error) {
			failed(error);
		}
	};

	const dialogUser = shown && 'form' in shown ? users.get(shown.form) ?? null : null;
	const deletion = shown && 'deletion' in shown ? users.get(shown.deletion) : undefined;

	return (
		<>
			<Feedback messages={messages} />
			{!config.compact && <div className="ant-dataview-options">
				{settingDefs(texts).map((setting) => (
					<div className="ant-dataview-option" key={setting.key}>
						<span className="ant-dataview-option-title">
							<label htmlFor={'react-grid-' + setting.key}>{setting.label}</label>
						</span>
						<Dropdown id={'react-grid-' + setting.key} name={setting.key}
							value={String(settings[setting.key])} options={setting.options}
							onChange={(value) => choose(setting.key, value)}
							filter={texts.filter} noMatch={texts.noMatch} more={texts.more}
							remove={texts.remove} />
					</div>
				))}
			</div>}
			<div>
				<DataTable key={settings.rowsFrom} ref={table} columns={config.columns}
					{...(fromResource ? { loadRows: remote.loadRows, subscribe } :
						{ rows: localRows, filterRows: matchesName })}
					rowsPerPage={config.rowsPerPage} pageSizes={config.pageSizes}
					infinite={settings.infinite} selection={settings.selection}
					selectionCount={settings.selectionCount} navigationPosition={settings.navigation}
					clearSelectionOnNavigation={settings.clearSelectionOnNavigation}
					onAction={action} filter={filter}
					resizableColumns={settings.resizable ? 'stretch' : false}
					bodyHeight={config.bodyHeight} adjustToParent={config.adjustToParent ?? true}
					texts={texts.table}
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
								className="ant-data-table-action ant-data-table-icon-action"
								title={texts.newUser} aria-label={texts.newUser}><AddIcon /></button>
						</span>
					} />
			</div>
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
					<div className="ant-button-bar ant-data-table-overlay-buttons">
						<Button label={texts.cancel} onClick={() => setShown(null)} />
						<Button label={texts.delete} variant="danger"
							onClick={() => remove(shown.deletion)} />
					</div>
				</Dialog>
			)}
		</>
	);
}
