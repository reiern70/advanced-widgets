/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { useRef, useState, type ReactNode } from 'react';

import { Button, Desktop, DesktopWindow, type DesktopHandle,
	type TaskbarPosition } from '@antilia/react';

import { demoBounds, FRAMEWORK_COLORS, KEYBOARD_ICON, NOTES_ICON, shortcuts, TASKBAR_POSITIONS,
	windowId, type DesktopConfig, type DesktopDemos, type DesktopWindowName } from '../desktop.js';
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
 * when it has changes. Above it, the side of the taskbar and a button opening the keyboard
 * shortcuts again.
 */
export function DesktopDemo({ config }: { config: DesktopConfig }) {
	const { texts } = config;
	const desktop = useRef<DesktopHandle>(null);
	const [taskbar, setTaskbar] = useState<TaskbarPosition>('bottom');
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
					<select value={taskbar}
						onChange={(event) => setTaskbar(event.target.value as TaskbarPosition)}>
						{TASKBAR_POSITIONS.map((position) => (
							<option key={position} value={position}>{texts['taskbar.' + position as
								keyof typeof texts]}</option>
						))}
					</select>
				</label>
				<Button label={texts.openKeyboard} size="small" icon={<Icon path={KEYBOARD_ICON} />}
					onClick={() => desktop.current?.activate(id('keyboard'))} />
			</form>
			<Desktop ref={desktop} taskbar={taskbar} texts={config.desktop} onTaskbar={setTaskbar}>
				<DesktopWindow id={id('users')} title={texts.reactUsers} icon={<Badge />} x={16}
					y={16} width={900} height={460} minWidth={320} minHeight={160} closable
					modified={false}>
					<JsonTableDemo config={config.jsonTable} />
				</DesktopWindow>
				<DesktopWindow id={id('notes')} title={texts.notes} icon={<Icon path={NOTES_ICON} />}
					x={800} y={24} width={360} height={240} closable>
					<textarea className="desktop-demo__notes" aria-label={texts.notes}
						defaultValue={texts.notesText} />
				</DesktopWindow>
				<DesktopWindow id={id('dropdowns')} title={texts.reactDropdowns} icon={<Badge />}
					x={240} y={60} width={700} height={460} minWidth={320} minHeight={160} closable>
					<DropdownDemo config={config.dropdown} />
				</DesktopWindow>
				{(Object.keys(apps) as (keyof DesktopDemos)[]).map((name, index) => (
					<DesktopWindow key={name} id={id(name)} title={demos[name].title}
						icon={<Badge />} {...demoBounds(index)} minWidth={320} minHeight={160}
						closable>
						{apps[name]}
					</DesktopWindow>
				))}
				<DesktopWindow id={id('keyboard')} title={texts.keyboard}
					icon={<Icon path={KEYBOARD_ICON} />} x={560} y={40} width={600} height={480}
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
