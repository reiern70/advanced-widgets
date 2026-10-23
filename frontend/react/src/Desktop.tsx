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

import { forwardRef, useEffect, useImperativeHandle, useLayoutEffect, useRef, useState,
	type ReactNode } from 'react';

import { Desktop as CoreDesktop, partClasses, type DesktopPane, type DesktopTexts,
	type TaskbarPosition, type TileLayout, type WindowState } from '@antilia/core';

import { useLook } from './Look.js';

/**
 * A type of window, which the launcher of the taskbar opens: the desktop renders a new window of
 * it in the pane shown, each time the user opens one, and takes it away when the user closes it.
 */
export interface DesktopWindowType {
	/** The name of the type. */
	type: string;
	/** What the launcher says for it, and the title of its windows. */
	title: string;
	/** The markup of its icon, an svg, in the launcher and on its windows. */
	icon?: string;
	/** Renders the body of a window of the type. */
	render(id: string): ReactNode;
	/** What else its windows are: where they start, their size, and the like. */
	window?: Omit<DesktopWindowProps, 'id' | 'title' | 'icon' | 'iconMarkup' | 'pane' | 'children'>;
}

/** A window of a type the user opened. */
interface Launched {
	id: string;
	type: string;
	pane: string;
}

export interface DesktopProps {
	/** The windows, {@link DesktopWindow}s; they may come and go. */
	children?: ReactNode;
	/** The side of the desktop the taskbar is on, bottom when left out. */
	taskbar?: TaskbarPosition;
	/** Whether the taskbar has the buttons tiling the windows, true when left out. */
	tiling?: boolean;
	/** Whether the taskbar has the button letting the desktop fill the page, true when left out. */
	expandable?: boolean;
	/** Whether the desktop fills the whole page, false when left out; the user changes it too. */
	expanded?: boolean;
	/** Whether the user may move the taskbar to another side, true when left out. */
	movableTaskbar?: boolean;
	/** Whether the taskbar has the menu listing the windows open and closed, true when left out. */
	menu?: boolean;
	/**
	 * The panes, the default one, of the id DEFAULT_PANE, first; that one alone when left out.
	 * The user changes them too.
	 */
	panes?: DesktopPane[];
	/** The id of the pane shown, the default one when left out; the user changes it too. */
	activePane?: string;
	/** Whether the taskbar shows the panes, true when left out. */
	paneSwitcher?: boolean;
	/** Whether the user may add, rename and remove panes, true when left out. */
	editablePanes?: boolean;
	/** The types of windows the launcher of the taskbar opens; none when left out. */
	windowTypes?: DesktopWindowType[];
	/**
	 * Asked before the user closes a window with changes not saved, instead of the dialog of the
	 * desktop; resolving to true closes it.
	 */
	confirmClose?(id: string, title: string): Promise<boolean> | boolean;
	/** What the desktop says, in English when left out. */
	texts?: Partial<DesktopTexts>;
	/**
	 * Called with the states of all windows, by their ids, once the user has changed any, moved
	 * the taskbar or let the desktop fill the page.
	 */
	onChange?(states: Record<string, WindowState>): void;
	/** Called with the side the user moved the taskbar to. */
	onTaskbar?(position: TaskbarPosition): void;
	/** Called with whether the desktop fills the page, once the user changed it. */
	onExpand?(expanded: boolean): void;
	/** Called with the id of the window the user brought to the front. */
	onActivate?(id: string): void;
	/** Called with the id of the window the user closed. */
	onClose?(id: string): void;
	/** Called with the panes and the id of the one shown, once the user changed them. */
	onPanes?(panes: DesktopPane[], active: string): void;
	/** Called with the type and the pane of a window the user opened from the launcher. */
	onOpenWindow?(type: string, pane: string): void;
	id?: string;
	/** CSS classes of the desktop, next to antilia-desktop; one giving it a height, say. */
	className?: string;
}

/**
 * What a ref to a {@link Desktop} can do with its windows, by their ids, without calling back.
 */
export interface DesktopHandle {
	/** The desktop of the core, null before it is mounted. */
	readonly desktop: CoreDesktop | null;
	/** The states of all windows, by their ids. */
	states(): Record<string, WindowState>;
	/** Brings a window to the front, restoring it when minimized and opening it when closed. */
	activate(id: string): void;
	minimize(id: string): void;
	maximize(id: string, maximized?: boolean): void;
	restore(id: string): void;
	fold(id: string, folded?: boolean): void;
	close(id: string): void;
	/**
	 * Closes a window as its close button does, asking first when it has changes not saved;
	 * resolves to whether it closed.
	 */
	requestClose(id: string): Promise<boolean>;
	open(id: string): void;
	/** Lays out the windows shown in a grid, side by side, one above the other, or cascading. */
	tile(layout?: TileLayout): void;
	/** Brings the next window to the front, or the previous one. */
	cycle(direction?: 1 | -1): void;
	/** Lets the desktop fill the whole page, or takes it back into the page. */
	expand(expanded?: boolean): void;
	/** The panes, the default one first. */
	panes(): DesktopPane[];
	/** The id of the pane shown. */
	activePane(): string;
	/** Shows another pane. */
	showPane(id: string): void;
	/** Adds a pane, without showing it. */
	addPane(name: string): DesktopPane | null;
	renamePane(id: string, name: string): void;
	/** Removes a pane, its windows going to the default pane. */
	removePane(id: string): void;
	/** Moves a window to another pane. */
	moveToPane(window: string, pane: string): void;
	/** Opens a window of a type, in the pane shown when none is given. */
	openWindow(type: string, pane?: string): void;
}

/**
 * The desktop of the core: floating windows that are not modal, many at once, and a taskbar. A
 * click brings a window to the front; its title bar drags it and a double click folds it; its
 * buttons minimize, maximize and close it, as it allows, and its edges resize it. F11 maximizes
 * the window the focus is in, Ctrl+F6 goes round the windows. The user drags the taskbar to
 * another side, and a button of it lets the desktop fill the whole page; its menu lists the
 * windows open and closed. Closing a window with changes not saved asks first. The windows are in
 * panes, which the taskbar shows, one at a time; Ctrl+Shift and the arrow keys go from pane to pane
 * and a button of each title bar moves its window to another pane. The launcher of the taskbar
 * opens windows of the types given, which the desktop renders. React renders the windows, the core
 * moves them and fills the taskbar. A ref gets a {@link DesktopHandle}. Give it a height.
 */
export const Desktop = forwardRef<DesktopHandle, DesktopProps>(function Desktop(props, ref) {
	const { children, taskbar = 'bottom', tiling = true, expandable = true, expanded = false,
		movableTaskbar = true, menu = true, panes, activePane, paneSwitcher = true,
		editablePanes = true, windowTypes, texts, id, className } = props;
	const element = useRef<HTMLDivElement>(null);
	const instance = useRef<CoreDesktop | null>(null);
	const look = useLook();
	const latest = useRef(props);
	latest.current = props;
	// the side of the taskbar: as the props say, or where the user moved it since
	const [position, setPosition] = useState<TaskbarPosition>(taskbar);
	const [filling, setFilling] = useState(expanded);
	// the panes and the one shown: as the props say, or as the user changed them since
	const [paneList, setPaneList] = useState<DesktopPane[] | undefined>(panes);
	const [shown, setShown] = useState<string | undefined>(activePane);
	// the windows of types the user opened
	const [launched, setLaunched] = useState<Launched[]>([]);
	const count = useRef(0);
	const panesKey = JSON.stringify(panes ?? null);

	useEffect(() => setPosition(taskbar), [taskbar]);
	useEffect(() => setFilling(expanded), [expanded]);
	useEffect(() => setPaneList(panes), [panesKey]);
	useEffect(() => setShown(activePane), [activePane]);

	const coreTypes = (windowTypes ?? []).map(({ type, title, icon }) => ({ type, title, icon }));
	const typesKey = JSON.stringify(coreTypes);

	/** Renders a window of a type in a pane, with an id unique in the page. */
	const launch = (type: string, pane: string) => {
		let window: string;
		do {
			window = (id ?? 'antilia-desktop') + '-' + type + '-' + (++count.current);
		} while (document.getElementById(window));
		const added = window;
		setLaunched((current) => [...current, { id: added, type, pane }]);
	};

	useImperativeHandle(ref, () => ({
		get desktop() {
			return instance.current;
		},
		states: () => instance.current?.states() ?? {},
		activate: (window) => instance.current?.activate(window),
		minimize: (window) => instance.current?.minimize(window),
		maximize: (window, maximized) => instance.current?.maximize(window, maximized),
		restore: (window) => instance.current?.restore(window),
		fold: (window, folded) => instance.current?.fold(window, folded),
		close: (window) => instance.current?.close(window),
		requestClose: (window) => instance.current?.requestClose(window) ?? Promise.resolve(false),
		open: (window) => instance.current?.open(window),
		tile: (layout) => instance.current?.tile(layout),
		cycle: (direction) => instance.current?.cycle(direction),
		expand: (value = true) => {
			instance.current?.expand(value);
			setFilling(value);
		},
		panes: () => instance.current?.panes() ?? [],
		activePane: () => instance.current?.activePane() ?? '0',
		showPane: (pane) => {
			if (instance.current?.showPane(pane)) {
				setShown(pane);
			}
		},
		addPane: (name) => {
			const pane = instance.current?.addPane(name) ?? null;
			setPaneList(instance.current?.panes());
			return pane;
		},
		renamePane: (pane, name) => {
			instance.current?.renamePane(pane, name);
			setPaneList(instance.current?.panes());
		},
		removePane: (pane) => {
			instance.current?.removePane(pane);
			setPaneList(instance.current?.panes());
			setShown(instance.current?.activePane());
		},
		moveToPane: (window, pane) => instance.current?.moveToPane(window, pane),
		openWindow: (type, pane) => instance.current?.openWindow(type, pane)
	}), []);

	useLayoutEffect(() => {
		const desktop = new CoreDesktop(element.current!, {
			look,
			taskbar: position, tiling, expandable, expanded: filling, movableTaskbar, menu, texts,
			panes: paneList, activePane: shown, paneSwitcher, editablePanes, windowTypes: coreTypes,
			confirmClose: (window, title) => latest.current.confirmClose ?
				latest.current.confirmClose(window, title) : desktop.askToClose(title),
			onChange: (states) => latest.current.onChange?.(states),
			onActivate: (window) => latest.current.onActivate?.(window),
			onClose: (window) => {
				setLaunched((current) => current.some((other) => other.id === window) ?
					current.filter((other) => other.id !== window) : current);
				latest.current.onClose?.(window);
			},
			onPanes: (list, active) => {
				setPaneList(list);
				setShown(active);
				latest.current.onPanes?.(list, active);
			},
			onOpenWindow: (type, pane) => {
				launch(type, pane);
				latest.current.onOpenWindow?.(type, pane);
			},
			onTaskbar: (side) => {
				setPosition(side);
				latest.current.onTaskbar?.(side);
			},
			onExpand: (value) => {
				setFilling(value);
				latest.current.onExpand?.(value);
			}
		});
		instance.current = desktop;
		return () => {
			desktop.destroy();
			instance.current = null;
		};
	}, [look]);

	const textsKey = JSON.stringify(texts ?? {});
	const paneListKey = JSON.stringify(paneList ?? null);
	useLayoutEffect(() => {
		instance.current?.configure({ taskbar: position, tiling, expandable, expanded: filling,
			movableTaskbar, menu, texts, panes: paneList, activePane: shown, paneSwitcher,
			editablePanes, windowTypes: coreTypes });
	}, [position, tiling, expandable, filling, movableTaskbar, menu, textsKey, paneListKey, shown,
		paneSwitcher, editablePanes, typesKey]);

	return (
		<div ref={element} id={id} className={[partClasses(look, 'desktop'),
			'antilia-desktop-taskbar-' + position, className].filter(Boolean).join(' ')}>
			<div className={partClasses(look, 'desktop-area')}>
				{children}
				{launched.map((window) => {
					const type = windowTypes?.find((other) => other.type === window.type);
					return type && (
						<DesktopWindow key={window.id} {...type.window} id={window.id}
							title={type.title} iconMarkup={type.icon} pane={window.pane} closable>
							{type.render(window.id)}
						</DesktopWindow>
					);
				})}
			</div>
			<div className={partClasses(look, 'desktop-taskbar')} />
		</div>
	);
});

export interface DesktopWindowProps {
	/** The id of the window, which the desktop knows it by. */
	id: string;
	/** The title, in the title bar and on the button of the taskbar. */
	title: ReactNode;
	/** An icon before the title, which the taskbar shows too; an svg, for example. */
	icon?: ReactNode;
	/** The markup of the icon, an svg, instead of icon. */
	iconMarkup?: string;
	/**
	 * The id of the pane the window is in, the one shown when left out; the user moves it to
	 * another too.
	 */
	pane?: string;
	/** Where the window starts, from the left of the desktop, in pixels; cascading when left out. */
	x?: number;
	/** Where the window starts, from the top of the desktop, in pixels; cascading when left out. */
	y?: number;
	/** The width the window starts with, in pixels, 420 when left out. */
	width?: number;
	/** The height the window starts with, in pixels, 280 when left out. */
	height?: number;
	/** The smallest width of the window, in pixels, 180 when left out. */
	minWidth?: number;
	/** The smallest height of the window, in pixels, 100 when left out. */
	minHeight?: number;
	/** Whether the edges of the window resize it, true when left out. */
	resizable?: boolean;
	/** Whether the window can be maximized, true when left out. */
	maximizable?: boolean;
	/** Whether the window can be minimized to the taskbar, true when left out. */
	minimizable?: boolean;
	/** Whether a double click on the title bar folds the window, true when left out. */
	foldable?: boolean;
	/** Whether the window has a button closing it, false when left out. */
	closable?: boolean;
	/**
	 * Whether the window has changes not saved, which the user is asked about before it closes;
	 * when left out, whether a field in it holds another value than the one it was rendered with,
	 * which a controlled field does not tell.
	 */
	modified?: boolean;
	/** Whether the window starts minimized. */
	minimized?: boolean;
	/** Whether the window starts maximized. */
	maximized?: boolean;
	/** Whether the window starts folded. */
	folded?: boolean;
	/** Whether the window starts closed. */
	closed?: boolean;
	/** CSS classes of the window, next to antilia-window. */
	className?: string;
	/** CSS classes of the body of the window, next to antilia-window-body. */
	bodyClassName?: string;
	/** The body of the window. */
	children?: ReactNode;
}

function flag(value: boolean | undefined): string | undefined {
	return value === undefined ? undefined : String(value);
}

function present(value: boolean | undefined): '' | undefined {
	return value ? '' : undefined;
}

/**
 * A window of a {@link Desktop}: its title bar, with the icon and the title, and its body. The
 * desktop adds the buttons and the handles, as the window allows.
 */
export function DesktopWindow({ id, title, icon, iconMarkup, pane, x, y, width, height, minWidth,
	minHeight,
	resizable, maximizable, minimizable, foldable, closable, modified, minimized, maximized, folded,
	closed, className, bodyClassName, children }: DesktopWindowProps) {
	const look = useLook();
	return (
		<section id={id} className={[partClasses(look, 'window'), className].filter(Boolean)
			.join(' ')}
			data-x={x} data-y={y} data-width={width} data-height={height} data-min-width={minWidth}
			data-min-height={minHeight} data-resizable={flag(resizable)}
			data-maximizable={flag(maximizable)} data-minimizable={flag(minimizable)}
			data-foldable={flag(foldable)} data-closable={flag(closable)}
			data-modified={flag(modified)}
			data-minimized={present(minimized)} data-maximized={present(maximized)}
			data-folded={present(folded)} data-closed={present(closed)} data-pane={pane}>
			<header className={partClasses(look, 'window-header')}>
				{iconMarkup ? <span className={partClasses(look, 'window-icon')}
					dangerouslySetInnerHTML={{ __html: iconMarkup }} /> :
					icon && <span className={partClasses(look, 'window-icon')}>{icon}</span>}
				<span className={partClasses(look, 'window-title')}>{title}</span>
			</header>
			<div className={[partClasses(look, 'window-body'), bodyClassName].filter(Boolean)
				.join(' ')}>
				{children}
			</div>
		</section>
	);
}
