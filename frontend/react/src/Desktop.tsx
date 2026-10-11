/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { forwardRef, useEffect, useImperativeHandle, useLayoutEffect, useRef, useState,
	type ReactNode } from 'react';

import { Desktop as CoreDesktop, DEFAULT_DESKTOP_TEXTS, DESKTOP_COMPONENTS, isLicensed,
	UNLICENSED_CLASS, type DesktopTexts, type TaskbarPosition, type TileLayout,
	type WindowState } from '@antilia/core';

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
	 * Asked before the user closes a window with changes not saved, instead of the dialog of the
	 * desktop; resolving to true closes it.
	 */
	confirmClose?(id: string, title: string): Promise<boolean> | boolean;
	/** What the desktop says, in English when left out. */
	texts?: Partial<DesktopTexts>;
	/**
	 * A license key activating the desktop, a private component; the one given to activate() of
	 * the core when left out. Without one the desktop shows a placeholder.
	 */
	licenseKey?: string;
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
}

/**
 * The desktop of the core: floating windows that are not modal, many at once, and a taskbar. A
 * click brings a window to the front; its title bar drags it and a double click folds it; its
 * buttons minimize, maximize and close it, as it allows, and its edges resize it. F11 maximizes
 * the window the focus is in, Ctrl+F6 goes round the windows. The user drags the taskbar to
 * another side, and a button of it lets the desktop fill the whole page; its menu lists the
 * windows open and closed. Closing a window with changes not saved asks first. React renders the
 * windows, the core moves them and fills the taskbar. A ref gets a {@link DesktopHandle}. Give it
 * a height. A private component: it needs a license key.
 */
export const Desktop = forwardRef<DesktopHandle, DesktopProps>(function Desktop(props, ref) {
	const { children, taskbar = 'bottom', tiling = true, expandable = true, expanded = false,
		movableTaskbar = true, menu = true, texts, licenseKey, id, className } = props;
	const element = useRef<HTMLDivElement>(null);
	const instance = useRef<CoreDesktop | null>(null);
	const latest = useRef(props);
	latest.current = props;
	const [licensed, setLicensed] = useState<boolean | null>(null);
	// the side of the taskbar: as the props say, or where the user moved it since
	const [position, setPosition] = useState<TaskbarPosition>(taskbar);
	const [filling, setFilling] = useState(expanded);

	useEffect(() => setPosition(taskbar), [taskbar]);
	useEffect(() => setFilling(expanded), [expanded]);

	useEffect(() => {
		let live = true;
		isLicensed(DESKTOP_COMPONENTS, licenseKey).then((active) => live && setLicensed(active));
		return () => {
			live = false;
		};
	}, [licenseKey]);

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
		}
	}), []);

	useLayoutEffect(() => {
		if (!licensed) {
			return;
		}
		const desktop = new CoreDesktop(element.current!, {
			taskbar: position, tiling, expandable, expanded: filling, movableTaskbar, menu, texts,
			confirmClose: (window, title) => latest.current.confirmClose ?
				latest.current.confirmClose(window, title) : desktop.askToClose(title),
			onChange: (states) => latest.current.onChange?.(states),
			onActivate: (window) => latest.current.onActivate?.(window),
			onClose: (window) => latest.current.onClose?.(window),
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
	}, [licensed]);

	const textsKey = JSON.stringify(texts ?? {});
	useLayoutEffect(() => {
		instance.current?.configure({ taskbar: position, tiling, expandable, expanded: filling,
			movableTaskbar, menu, texts });
	}, [position, tiling, expandable, filling, movableTaskbar, menu, textsKey]);

	if (licensed === null) {
		return null;
	}
	if (!licensed) {
		return <div id={id} className={UNLICENSED_CLASS} role="note" data-component="Desktop">
			{texts?.unlicensed ?? DEFAULT_DESKTOP_TEXTS.unlicensed}</div>;
	}
	return (
		<div ref={element} id={id} className={['antilia-desktop', 'antilia-desktop-taskbar-' + position,
			className].filter(Boolean).join(' ')}>
			<div className="antilia-desktop-area">{children}</div>
			<div className="antilia-desktop-taskbar" />
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
export function DesktopWindow({ id, title, icon, x, y, width, height, minWidth, minHeight,
	resizable, maximizable, minimizable, foldable, closable, modified, minimized, maximized, folded,
	closed, className, bodyClassName, children }: DesktopWindowProps) {
	return (
		<section id={id} className={['antilia-window', className].filter(Boolean).join(' ')}
			data-x={x} data-y={y} data-width={width} data-height={height} data-min-width={minWidth}
			data-min-height={minHeight} data-resizable={flag(resizable)}
			data-maximizable={flag(maximizable)} data-minimizable={flag(minimizable)}
			data-foldable={flag(foldable)} data-closable={flag(closable)}
			data-modified={flag(modified)}
			data-minimized={present(minimized)} data-maximized={present(maximized)}
			data-folded={present(folded)} data-closed={present(closed)}>
			<header className="antilia-window-header">
				{icon && <span className="antilia-window-icon">{icon}</span>}
				<span className="antilia-window-title">{title}</span>
			</header>
			<div className={['antilia-window-body', bodyClassName].filter(Boolean).join(' ')}>
				{children}
			</div>
		</section>
	);
}
