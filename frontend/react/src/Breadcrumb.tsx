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

import { Breadcrumb as CoreBreadcrumb, DEFAULT_BREADCRUMB_TEXTS, partClasses,
	type BreadcrumbSeparator, type BreadcrumbTexts } from '@antilia/core';

import { useLook } from './Look.js';

/**
 * A crumb of a {@link Breadcrumb}: a link when it has href, a button doing what onClick does
 * otherwise, its label alone when it has neither. The last crumb is the page shown, its label
 * alone whatever it has.
 */
export interface BreadcrumbItem {
	/** The name of the crumb, the key React knows it by; its place in the trail when left out. */
	id?: string;
	/** The label of the crumb. */
	label: ReactNode;
	/** An icon before the label; an svg, for example. */
	icon?: ReactNode;
	/** The tooltip of the crumb, its title. */
	tooltip?: string;
	/** Where the crumb leads: it is a link then. */
	href?: string;
	/** Where the link opens, _blank for a new window. */
	target?: string;
	/** Called when the user chose the crumb, a link or a button. */
	onClick?(event: MouseEvent<HTMLElement>): void;
	/** Whether the user cannot choose the crumb, which shows dimmed; false when left out. */
	disabled?: boolean;
	/** CSS classes of the link or the button of the crumb. */
	className?: string;
}

export interface BreadcrumbProps {
	/** The crumbs, from the home page down to the page shown; they may change. */
	items: BreadcrumbItem[];
	/** The separator drawn between the crumbs, chevron when left out. */
	separator?: BreadcrumbSeparator;
	/**
	 * How many crumbs show at most: a longer trail shows its first crumb and its last
	 * maxItems - 1, the others behind a button "…" that shows them; every crumb when left out.
	 */
	maxItems?: number;
	/** The texts: the accessible name and the label of the button "…"; English when left out. */
	texts?: Partial<BreadcrumbTexts>;
	/** Called with each crumb the user chooses, after its own onClick. */
	onSelect?(item: BreadcrumbItem): void;
	/** Called when the user showed the crumbs hidden. */
	onExpand?(): void;
	id?: string;
	/** CSS classes of the breadcrumb, next to antilia-breadcrumb. */
	className?: string;
}

/**
 * What a ref to {@link Breadcrumb} can do.
 */
export interface BreadcrumbHandle {
	/** The breadcrumb of the core, null before it is mounted. */
	readonly breadcrumb: CoreBreadcrumb | null;
	/** Shows the crumbs hidden, the focus on the first of them. */
	expand(): void;
}

/**
 * The breadcrumb of the core: where the page is, a row of crumbs from the home page down to the
 * page shown, the last crumb. A nav, named, around an ordered list; the style sheet draws the
 * separators. A long trail collapses: maxItems crumbs show, the others behind a button "…" that
 * shows them in place. React renders the crumbs, the core collapses them, in the look of the
 * closest LookProvider. A ref gets a {@link BreadcrumbHandle}.
 */
export const Breadcrumb = forwardRef<BreadcrumbHandle, BreadcrumbProps>(function Breadcrumb(props,
	ref) {
	const { items, separator = 'chevron', maxItems = 0, texts, id, className } = props;
	const look = useLook();
	const element = useRef<HTMLElement>(null);
	const instance = useRef<CoreBreadcrumb | null>(null);
	const latest = useRef(props);
	latest.current = props;
	const label = texts?.label ?? DEFAULT_BREADCRUMB_TEXTS.label;

	useImperativeHandle(ref, () => ({
		get breadcrumb() {
			return instance.current;
		},
		expand: () => instance.current?.expand()
	}), []);

	useLayoutEffect(() => {
		const core = new CoreBreadcrumb(element.current!, { separator, maxItems, look, texts,
			onExpand: () => latest.current.onExpand?.() });
		instance.current = core;
		return () => {
			core.destroy();
			instance.current = null;
		};
	}, []);

	const textsKey = JSON.stringify(texts ?? {});
	useLayoutEffect(() => {
		instance.current?.configure({ separator, maxItems, look, texts });
	}, [separator, maxItems, look, textsKey]);

	// React wrote the crumbs again: the core collapses them, or keeps them as the user left them
	useLayoutEffect(() => {
		instance.current?.refresh();
	});

	const crumbs = items.map((item, index) => {
		const current = index === items.length - 1;
		const content = <>
			{item.icon && <span className={partClasses(look, 'breadcrumb-icon')}>{item.icon}</span>}
			<span className={partClasses(look, 'breadcrumb-label')}>{item.label}</span>
		</>;
		const classes = [partClasses(look, 'breadcrumb-link'), item.className].filter(Boolean)
			.join(' ');
		const disabled = item.disabled ? 'true' : undefined;
		const chosen = item.disabled ? undefined : (event: MouseEvent<HTMLElement>) => {
			item.onClick?.(event);
			latest.current.onSelect?.(item);
		};
		let crumb: ReactNode;
		if (current) {
			crumb = <span className={partClasses(look, 'breadcrumb-page')} aria-current="page"
				title={item.tooltip}>{content}</span>;
		} else if (item.href !== undefined) {
			crumb = <a href={item.disabled ? undefined : item.href} target={item.target}
				className={classes} title={item.tooltip} aria-disabled={disabled}
				onClick={chosen}>{content}</a>;
		} else if (item.onClick) {
			crumb = <button type="button" className={classes} title={item.tooltip}
				aria-disabled={disabled} onClick={chosen}>{content}</button>;
		} else {
			crumb = <span className={partClasses(look, 'breadcrumb-text')}
				title={item.tooltip}>{content}</span>;
		}
		return <li key={item.id ?? String(index)}
			className={partClasses(look, 'breadcrumb-item', current && 'breadcrumb-current')}>
			{crumb}
		</li>;
	});

	return (
		<nav ref={element} id={id} aria-label={label} className={[partClasses(look, 'breadcrumb',
			`breadcrumb-${separator}`), className].filter(Boolean).join(' ')}>
			<ol className={partClasses(look, 'breadcrumb-list')}>{crumbs}</ol>
		</nav>
	);
});
