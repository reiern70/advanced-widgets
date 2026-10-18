/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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

import { useRef, useState, type ReactNode } from 'react';

import { Button, Desktop, DesktopWindow, Dropdown, type DesktopHandle,
	type TaskbarPosition } from '@antilia/react';

import { dropdownTexts } from '../data.js';
import { demoBounds, demoPanes, FRAMEWORK_COLORS, iconMarkup, KEYBOARD_ICON, NOTES_BOUNDS,
	NOTES_ICON, paneOf, shortcuts, TASKBAR_POSITIONS, windowId, type DesktopConfig,
	type DesktopDemos, type DesktopWindowName } from '../desktop.js';
import { AutocompleteDemo } from './AutocompleteDemo.js';
import { ButtonsDemo } from './ButtonsDemo.js';
import { DatePickerDemo } from './DatePickerDemo.js';
import { DropdownDemo } from './DropdownDemo.js';
import { JsonListDemo } from './JsonListDemo.js';
import { JsonTableDemo } from './JsonTableDemo.js';
import { ModalDemo } from './ModalDemo.js';
import { ProgressDemo } from './ProgressDemo.js';

function Icon({ path }: { path: string }) {
	return (
		<svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" focusable="false"
			fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"
			strokeLinejoin="round">
			<path d={path} />
		</svg>
	);
}

function Badge() {
	return (
		<svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" focusable="false">
			<rect width="24" height="24" rx="6" fill={FRAMEWORK_COLORS.react} />
			<text x="12" y="17" textAnchor="middle" fontSize="14" fontWeight="700"
				fontFamily="sans-serif" fill="#ffffff">R</text>
		</svg>
	);
}

/**
 * The Desktop demo in React: a desktop of the Wicket page's markup, its windows the JSON grid in
 * React, notes, the drop-downs, the autocompletes, the buttons, the date pickers, the JSON list,
 * the modal dialogs and the progress bars in React, and the keyboard shortcuts, which start
 * minimized; the menu of the taskbar opens them all, and each can be closed, asking first
 * when it has changes. The grids, the notes and the keyboard are in the first pane, the
 * drop-downs in the second, the other demos in the third; the launcher opens more notes. Above
 * it, the side of the taskbar and a button opening the keyboard shortcuts again.
 */
export function DesktopDemo({ config }: { config: DesktopConfig }) {
	const { texts } = config;
	const desktop = useRef<DesktopHandle>(null);
	const [taskbar, setTaskbar] = useState<TaskbarPosition>('bottom');
	// once: the user renames them since
	const [panes] = useState(() => demoPanes(texts));
	const id = (name: DesktopWindowName) => windowId('react', name);
	const { demos } = config;
	const apps: { [name in keyof DesktopDemos]: ReactNode } = {
		autocomplete: <AutocompleteDemo config={demos.autocomplete.config} />,
		buttons: <ButtonsDemo config={demos.buttons.config} />,
		datePicker: <DatePickerDemo config={demos.datePicker.config} />,
		jsonList: <JsonListDemo config={demos.jsonList.config} />,
		modalDialog: <ModalDemo config={demos.modalDialog.config} />,
		progressBar: <ProgressDemo config={demos.progressBar.config} />
	};

	return (
		<div className="desktop-demo">
			<form className="desktop-demo__options" onSubmit={(event) => event.preventDefault()}>
				<label className="desktop-demo__option">
					{texts.taskbar}{' '}
					<Dropdown value={taskbar} options={TASKBAR_POSITIONS.map((position) => ({
						value: position, label: texts['taskbar.' + position as keyof typeof texts] }))}
						onChange={(value) => setTaskbar(value as TaskbarPosition)}
						{...dropdownTexts(texts)} />
				</label>
				<Button label={texts.openKeyboard} size="small" icon={<Icon path={KEYBOARD_ICON} />}
					onClick={() => desktop.current?.activate(id('keyboard'))} />
			</form>
			<Desktop ref={desktop} taskbar={taskbar} texts={config.desktop} onTaskbar={setTaskbar}
				panes={panes} windowTypes={[{ type: 'notes', title: texts.notes,
					icon: iconMarkup(NOTES_ICON), window: NOTES_BOUNDS,
					render: () => <textarea className="desktop-demo__notes" aria-label={texts.notes}
						defaultValue={texts.notesText} /> }]}>
				<DesktopWindow id={id('users')} pane={paneOf('users')} title={texts.reactUsers} icon={<Badge />} x={16}
					y={16} width={900} height={460} minWidth={320} minHeight={160} closable
					modified={false}>
					<JsonTableDemo config={config.jsonTable} />
				</DesktopWindow>
				<DesktopWindow id={id('notes')} pane={paneOf('notes')} title={texts.notes} icon={<Icon path={NOTES_ICON} />}
					x={800} y={24} width={360} height={240} closable>
					<textarea className="desktop-demo__notes" aria-label={texts.notes}
						defaultValue={texts.notesText} />
				</DesktopWindow>
				<DesktopWindow id={id('dropdowns')} pane={paneOf('dropdowns')}
					title={texts.reactDropdowns} icon={<Badge />} x={16} y={16} width={700} height={460} minWidth={320} minHeight={160} closable>
					<DropdownDemo config={config.dropdown} />
				</DesktopWindow>
				{(Object.keys(apps) as (keyof DesktopDemos)[]).map((name, index) => (
					<DesktopWindow key={name} id={id(name)} pane={paneOf(name)} title={demos[name].title}
						icon={<Badge />} {...demoBounds(index)} minWidth={320} minHeight={160}
						closable>
						{apps[name]}
					</DesktopWindow>
				))}
				<DesktopWindow id={id('keyboard')} pane={paneOf('keyboard')} title={texts.keyboard}
					icon={<Icon path={KEYBOARD_ICON} />} x={560} y={16} width={640} height={620}
					minWidth={320} minHeight={200} closable minimized>
					<dl className="desktop-demo__keys">
						{shortcuts(texts).map((shortcut) => (
							<div key={shortcut.text} className="desktop-demo__key">
								<dt>{shortcut.keys ? shortcut.keys.map((key, index) => (
									<span key={key}>{index > 0 && '+'}<kbd>{key}</kbd></span>
								)) : shortcut.action}</dt>
								<dd>{shortcut.text}</dd>
							</div>
						))}
					</dl>
				</DesktopWindow>
			</Desktop>
		</div>
	);
}
