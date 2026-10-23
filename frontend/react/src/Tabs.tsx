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

import { Children, forwardRef, isValidElement, useImperativeHandle, useLayoutEffect, useRef,
	type ReactElement, type ReactNode } from 'react';

import { LOOK_PARTS, partClasses, Tabs as CoreTabs, type LookPart, type TabsDesign,
	type TabsOrientation, type TabsTexts } from '@antilia/core';

import { useLook } from './Look.js';

export interface TabProps {
	/** The name of the tab, which the tabs know it by. */
	id: string;
	/** The title of the tab. */
	title: ReactNode;
	/** An icon before the title; an svg, for example. */
	icon?: ReactNode;
	/** Whether the tab has a button closing it, false when left out. */
	closable?: boolean;
	/**
	 * Whether the panel has changes not saved, which the user is asked about before the tab
	 * closes; when left out, whether a field in it holds another value than the one it was
	 * rendered with, which a controlled field does not tell.
	 */
	modified?: boolean;
	/**
	 * Buttons of the page's own after the title, before the one closing the tab: one telling
	 * about what the panel holds, for example. A click on them does not select the tab.
	 */
	actions?: ReactNode;
	/** CSS classes of the tab, next to antilia-tab. */
	className?: string;
	/** CSS classes of the panel, next to antilia-tabs-panel. */
	panelClassName?: string;
	/** The panel of the tab. */
	children?: ReactNode;
}

/**
 * A tab of {@link Tabs}: its title, maybe with an icon and actions, and its panel, the children.
 * It renders nothing of its own: the tabs render its title among the tabs and its panel below
 * them.
 */
export function Tab(_props: TabProps): null {
	return null;
}

export interface TabsProps {
	/** The tabs, {@link Tab}s; they may come and go. */
	children?: ReactNode;
	/**
	 * The name of the tab selected: where the tabs start, and the tab they select whenever this
	 * changes. The first tab when left out.
	 */
	selected?: string;
	/** horizontal (the default) for the tabs along the top, vertical for down the left side. */
	orientation?: TabsOrientation;
	/** The design of the tabs, tabs when left out. */
	design?: TabsDesign;
	/** The accessible name of the list of tabs. */
	label?: string;
	/** What the tabs say, in English when left out. */
	texts?: Partial<TabsTexts>;
	/**
	 * Asked before the user closes a tab with changes not saved, instead of the dialog of the tabs;
	 * resolving to true closes it.
	 */
	confirmClose?(tab: string, title: string): Promise<boolean> | boolean;
	/** Called with the name of the tab the user selected. */
	onSelect?(tab: string): void;
	/**
	 * Called with the name of the tab the user closed: hidden until opened again, it is for the
	 * page to take it away.
	 */
	onClose?(tab: string): void;
	id?: string;
	/** CSS classes of the tabs, next to antilia-tabs. */
	className?: string;
}

/**
 * What a ref to {@link Tabs} can do with the tabs, by their names, without calling back.
 */
export interface TabsHandle {
	/** The tabs of the core, null before they are mounted. */
	readonly tabs: CoreTabs | null;
	/** The name of the tab selected, null when none is open. */
	selected(): string | null;
	/** Selects a tab, opening it when closed. */
	select(tab: string): void;
	/** Closes a tab at once. */
	close(tab: string): void;
	/**
	 * Closes a tab as its close button does, asking first when it has changes not saved;
	 * resolves to whether it closed.
	 */
	requestClose(tab: string): Promise<boolean>;
	/** Opens a closed tab again, selecting it. */
	open(tab: string): void;
	/** Whether the panel of a tab has changes not saved. */
	isModified(tab: string): boolean;
}

function flag(value: boolean | undefined): string | undefined {
	return value === undefined ? undefined : String(value);
}

/**
 * The tabs of the core: a list of tabs, along the top of their panels or down their left side, and
 * the panel of the tab selected. A click or the arrow keys select a tab. A closable tab has a
 * button closing it, as do Delete and the middle button; closing one with changes not saved asks
 * first, and its tab shows a mark meanwhile. A tab may have actions of the page's own before the
 * button closing it. React renders the tabs and their panels, the {@link Tab} children, the core
 * selects and closes them, in the look of the closest LookProvider. A ref gets a
 * {@link TabsHandle}.
 */
export const Tabs = forwardRef<TabsHandle, TabsProps>(function Tabs(props, ref) {
	const { children, selected, orientation = 'horizontal', design = 'tabs', label, texts, id,
		className } = props;
	const look = useLook();
	const element = useRef<HTMLDivElement>(null);
	const instance = useRef<CoreTabs | null>(null);
	const latest = useRef(props);
	latest.current = props;
	const tabs = Children.toArray(children).filter((child): child is ReactElement<TabProps> =>
		isValidElement(child) && child.type === Tab).map((child) => child.props);

	useImperativeHandle(ref, () => ({
		get tabs() {
			return instance.current;
		},
		selected: () => instance.current?.selected() ?? null,
		select: (tab) => instance.current?.select(tab),
		close: (tab) => instance.current?.close(tab),
		requestClose: (tab) => instance.current?.requestClose(tab) ?? Promise.resolve(false),
		open: (tab) => instance.current?.open(tab),
		isModified: (tab) => instance.current?.isModified(tab) ?? false
	}), []);

	useLayoutEffect(() => {
		const core: CoreTabs = new CoreTabs(element.current!, {
			orientation, design, look, label, texts, selected,
			confirmClose: (tab, title) => latest.current.confirmClose ?
				latest.current.confirmClose(tab, title) : core.askToClose(title),
			onSelect: (tab) => latest.current.onSelect?.(tab),
			onClose: (tab) => latest.current.onClose?.(tab)
		});
		instance.current = core;
		return () => {
			core.destroy();
			instance.current = null;
		};
	}, []);

	const textsKey = JSON.stringify(texts ?? {});
	useLayoutEffect(() => {
		instance.current?.configure({ orientation, design, look, label, texts });
	}, [orientation, design, look, label, textsKey]);

	useLayoutEffect(() => {
		if (selected !== undefined) {
			instance.current?.configure({ selected });
		}
	}, [selected]);

	// React wrote the tabs again: the states the core keeps on them come back, and tabs it
	// added are found at once
	useLayoutEffect(() => {
		instance.current?.refresh();
	});

	// the classes of the core's own, in the look: the design of the list, a nav in Bootstrap's
	const listDesign = 'tabs-list-' + design;
	return (
		<div ref={element} id={id} className={[partClasses(look, 'tabs', `tabs-${orientation}`,
			`tabs-${design}`), className].filter(Boolean).join(' ')}>
			<div className={partClasses(look, 'tabs-list', listDesign in LOOK_PARTS &&
				listDesign as LookPart)}>
				{tabs.map((tab) => (
					<div key={tab.id} className={[partClasses(look, 'tab'), tab.className].filter(Boolean)
						.join(' ')} data-tab={tab.id} data-closable={tab.closable ? 'true' : undefined}>
						<button type="button" className={partClasses(look, 'tab-button')}>
							{tab.icon && <span className={partClasses(look, 'tab-icon')}>{tab.icon}</span>}
							<span className={partClasses(look, 'tab-title')}>{tab.title}</span>
						</button>
						{tab.actions && <span className={partClasses(look, 'tab-actions')}>{tab.actions}</span>}
					</div>
				))}
			</div>
			<div className={partClasses(look, 'tabs-panels')}>
				{tabs.map((tab) => (
					<div key={tab.id} className={[partClasses(look, 'tabs-panel'), tab.panelClassName]
						.filter(Boolean).join(' ')} data-tab={tab.id} data-modified={flag(tab.modified)}>
						{tab.children}
					</div>
				))}
			</div>
		</div>
	);
});
