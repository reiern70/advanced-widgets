/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { useEffect, useMemo, useRef, useState } from 'react';

import { Dropdown, FILTER_DELAY, InfiniteList, type InfiniteListHandle } from '@antilia/react';

import { format, loadChoices, type Choice, type User } from '../data.js';
import { interpolate, Users, type JsonListConfig, type JsonListTexts,
	type UserFields } from '../json-list.js';
import { Dialog } from './Dialog.js';
import { Feedback, type Message } from './Feedback.js';
import { UserForm } from './UserForm.js';

/** What the dialog shows: the form of a user, new when its key is null, or a deletion. */
type Shown = { form: string | null } | { deletion: string } | null;

function UserDetails({ texts, user }: { texts: JsonListTexts; user: User }) {
	const rows: Array<[string, unknown]> = [[texts.id, user.id], [texts.firstName, user.firstName],
		[texts.lastName, user.lastName], [texts.homePhone, user.homePhone],
		[texts.cellPhone, user.cellPhone], [texts.bornDate, user.born]];
	return (
		<fieldset>
			<legend>{texts.selectedUser}</legend>
			<table>
				<tbody>
					{rows.map(([label, value]) => <tr key={label}><th>{label}</th><td>{String(value ?? '')}</td></tr>)}
				</tbody>
			</table>
		</fieldset>
	);
}

/**
 * The JSON list demo in React: the users as cards, from the users resource, filtered and sorted;
 * a click on a card shows the user next to the list, its buttons edit and delete it in dialogs of
 * the app. What the app changes stays in the browser.
 */
export function JsonListDemo({ config }: { config: JsonListConfig }) {
	const { texts } = config;
	const users = useMemo(() => new Users(config.usersUrl, config.perPage, config.sort),
		[config.usersUrl, config.perPage, config.sort]);
	const list = useRef<InfiniteListHandle>(null);
	const [filter, setFilter] = useState('');
	const [sort, setSort] = useState(config.sort);
	const [countries, setCountries] = useState<Choice[]>([]);
	const [selected, setSelected] = useState<User | null>(null);
	const [shown, setShown] = useState<Shown>(null);
	const [messages, setMessages] = useState<Message[]>([]);
	const listTexts = useMemo(() => ({ loading: texts.loading, end: texts.end,
		empty: texts.empty }), [texts]);

	useEffect(() => {
		loadChoices(config.choicesUrl).then((choices) => setCountries(choices.countries),
			(error) => console.error(error));
	}, [config.choicesUrl]);

	// another filter or sort starts over from the first page, the filter once typing pauses
	const first = useRef(true);
	useEffect(() => {
		if (first.current) {
			first.current = false;
			return;
		}
		const timer = window.setTimeout(() => {
			users.filter = filter;
			users.sort = sort;
			users.reset();
			list.current?.reload();
		}, users.sort !== sort ? 0 : FILTER_DELAY);
		return () => window.clearTimeout(timer);
	}, [filter, sort, users]);

	const action = (name: string, key: string | null) => {
		const user = users.get(key);
		if (!user || key === null) {
			return;
		}
		if (name !== 'delete') {
			setSelected(user);
		}
		if (name === 'edit') {
			setShown({ form: key });
		} else if (name === 'delete') {
			setShown({ deletion: key });
		}
	};

	const save = (key: string | null, fields: UserFields) => {
		const saved = users.save(key, fields, countries);
		setShown(null);
		setSelected(saved.user);
		if (key === null) {
			setMessages([{ level: 'info', text: format(texts.added, saved.user.firstName,
				saved.user.lastName) }]);
			list.current?.reload();
		} else {
			list.current?.renderItem(key, saved.user);
		}
	};

	const remove = (key: string) => {
		const user = users.get(key)!;
		users.delete(key);
		setShown(null);
		setMessages([{ level: 'info', text: format(texts.deleted, user.firstName,
			user.lastName) }]);
		if (selected?.id === user.id) {
			setSelected(null);
		}
		list.current?.reload();
	};

	const sorts = config.sorts.map((value) => ({ value,
		label: texts[('sort.' + value) as 'sort.firstName'] }));
	const dialogUser = shown && 'form' in shown ? users.get(shown.form) ?? null : null;
	const deletion = shown && 'deletion' in shown ? users.get(shown.deletion) : undefined;

	return (
		<>
			<Feedback messages={messages} />
			<div className="user-list-demo">
				<div className="user-list-demo__toolbar">
					<label className="user-filter">
						{texts.filterLabel}{' '}
						<input type="search" value={filter} placeholder={texts['filter.placeholder']}
							onChange={(event) => setFilter(event.target.value)} />
					</label>
					<label className="user-list-demo__sort">
						{texts.sortBy}{' '}
						<Dropdown name="sort" options={sorts} value={sort} onChange={setSort}
							filter={texts.filter} noMatch={texts.noMatch} more={texts.more}
							remove={texts.remove} />
					</label>
					<button type="button" onClick={() => setShown({ form: null })}
						className="ant-data-table-button ant-data-table-button-primary">
						{texts.newUser}
					</button>
				</div>
				<div className="user-list-demo__columns">
					<InfiniteList ref={list} load={users.load} template={config.template}
						height={config.height} texts={listTexts} locale={config.locale}
						onAction={action} className="user-list-demo__cards" />
					<aside className="user-list-demo__details">
						{selected ? <UserDetails texts={texts} user={selected} />
							: <p className="user-list-demo__none">{texts.noneSelected}</p>}
					</aside>
				</div>
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
					<div className="ant-data-table-overlay-buttons">
						<button type="button" className="ant-data-table-button"
							onClick={() => setShown(null)}>{texts.cancel}</button>
						<button type="button" onClick={() => remove(shown.deletion)}
							className="ant-data-table-button ant-data-table-button-primary">
							{texts.delete}
						</button>
					</div>
				</Dialog>
			)}
		</>
	);
}
