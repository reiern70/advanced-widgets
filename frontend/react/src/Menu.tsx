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

import { forwardRef, useImperativeHandle, useLayoutEffect, useRef, type MouseEvent,
	type ReactNode } from 'react';

import { Menu as CoreMenu, partClasses, type MenuDesign, type MenuOrientation } from '@antilia/core';

import { useLook } from './Look.js';

/**
 * An item of a {@link Menu}: a link when it has href, a button doing what onClick does otherwise,
 * or the item opening a submenu when it has items.
 */
export interface MenuItem {
	/** The name of the item, the key React knows it by; its place in its list when left out. */
	id?: string;
	/** The label of the item. */
	label: ReactNode;
	/** An icon before the label; an svg, for example. */
	icon?: ReactNode;
	/** The tooltip of the item, its title. */
	tooltip?: string;
	/** Where the item leads: it is a link then. */
	href?: string;
	/** Where the link opens, _blank for a new window. */
	target?: string;
	/** Called when the user chose the item, a link or a button. */
	onClick?(event: MouseEvent<HTMLElement>): void;
	/** Whether the user cannot choose the item, which shows dimmed; false when left out. */
	disabled?: boolean;
	/** Whether the item leads to the page shown, which the menu marks, and the items holding it. */
	current?: boolean;
	/** The items of the submenu the item opens; it has neither href nor onClick then. */
	items?: MenuEntry[];
	/** CSS classes of the link or the button of the item. */
	className?: string;
}

/** A line between the items of a submenu, or upright between those of a bar along the top. */
export interface MenuSeparator {
	separator: true;
	/** The key React knows it by; its place in its list when left out. */
	id?: string;
}

/** An item or a separator. */
export type MenuEntry = MenuItem | MenuSeparator;

export interface MenuProps {
	/** The items of the bar; they may change. */
	items: MenuEntry[];
	/** horizontal (the default) for the bar along the top, vertical for down the side. */
	orientation?: MenuOrientation;
	/** The design of the menu, bar when left out. */
	design?: MenuDesign;
	/**
	 * Whether the submenus of a menu down the side open in place, pushing the items after them
	 * down, instead of popping up beside their items; false when left out.
	 */
	inline?: boolean;
	/** Whether the submenus of the bar open as the pointer rests on their items; false when left out. */
	openOnHover?: boolean;
	/** The accessible name of the bar. */
	label?: string;
	/** Called with each item the user chooses, after its own onClick. */
	onSelect?(item: MenuItem): void;
	id?: string;
	/** CSS classes of the menu, next to antilia-menu. */
	className?: string;
}

/**
 * What a ref to {@link Menu} can do.
 */
export interface MenuHandle {
	/** The menu of the core, null before it is mounted. */
	readonly menu: CoreMenu | null;
	/** Closes the submenus popping up. */
	closeAll(): void;
}

/**
 * The menu of the core: a bar of items along the top or down the side, links and buttons, and
 * items opening submenus, nested as deep as one likes. A click opens a submenu, and while one of
 * the bar is open the pointer opens the others; the keys move along the bar and through the
 * submenus. The submenus pop up, or open in place down the side. React renders the items, the core
 * opens and closes the submenus, in the look of the closest LookProvider. A ref gets a
 * {@link MenuHandle}.
 */
export const Menu = forwardRef<MenuHandle, MenuProps>(function Menu(props, ref) {
	const { items, orientation = 'horizontal', design = 'bar', inline = false,
		openOnHover = false, label, id, className } = props;
	const look = useLook();
	const element = useRef<HTMLDivElement>(null);
	const instance = useRef<CoreMenu | null>(null);
	const latest = useRef(props);
	latest.current = props;

	useImperativeHandle(ref, () => ({
		get menu() {
			return instance.current;
		},
		closeAll: () => instance.current?.closeAll()
	}), []);

	useLayoutEffect(() => {
		const core = new CoreMenu(element.current!, { orientation, design, look, inline,
			openOnHover, label });
		instance.current = core;
		return () => {
			core.destroy();
			instance.current = null;
		};
	}, []);

	useLayoutEffect(() => {
		instance.current?.configure({ orientation, design, look, inline, openOnHover, label });
	}, [orientation, design, look, inline, openOnHover, label]);

	// React wrote the items again: the states the core keeps on them come back, and items it
	// added are found at once
	useLayoutEffect(() => {
		instance.current?.refresh();
	});

	const list = (entries: MenuEntry[], depth: number): ReactNode => entries.map((entry, index) => {
		const key = entry.id ?? String(index);
		if ('separator' in entry) {
			return <li key={key} className={partClasses(look, 'menu-separator')} />;
		}
		const top = depth === 0;
		const classes = [partClasses(look, top ? 'menu-link' : 'menu-submenu-link',
			entry.current && 'menu-current'), entry.className].filter(Boolean).join(' ');
		const content = <>
			{entry.icon && <span className={partClasses(look, 'menu-icon')}>{entry.icon}</span>}
			<span className={partClasses(look, 'menu-label')}>{entry.label}</span>
		</>;
		const disabled = entry.disabled ? 'true' : undefined;
		let link: ReactNode;
		if (entry.items) {
			link = <>
				<button type="button" className={classes} title={entry.tooltip}
					aria-disabled={disabled}>{content}</button>
				<ul className={partClasses(look, 'menu-submenu')}>{list(entry.items, depth + 1)}</ul>
			</>;
		} else {
			const chosen = entry.disabled ? undefined : (event: MouseEvent<HTMLElement>) => {
				entry.onClick?.(event);
				latest.current.onSelect?.(entry);
			};
			link = entry.href !== undefined ?
				<a href={entry.disabled ? undefined : entry.href} target={entry.target}
					className={classes} title={entry.tooltip} aria-disabled={disabled}
					aria-current={entry.current ? 'page' : undefined} onClick={chosen}>{content}</a> :
				<button type="button" className={classes} title={entry.tooltip}
					aria-disabled={disabled} aria-current={entry.current ? 'page' : undefined}
					onClick={chosen}>{content}</button>;
		}
		return <li key={key} className={partClasses(look, top ? 'menu-item' : 'menu-submenu-item')}>
			{link}
		</li>;
	});

	return (
		<div ref={element} id={id} className={[partClasses(look, 'menu', `menu-${orientation}`,
			`menu-${design}`, inline && orientation === 'vertical' && 'menu-inline'), className]
			.filter(Boolean).join(' ')}>
			<ul className={partClasses(look, 'menu-list', `menu-list-${design}`)}>{list(items, 0)}</ul>
		</div>
	);
});
