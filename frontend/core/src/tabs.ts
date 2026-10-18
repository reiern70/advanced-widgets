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

/*
 * Tabs: a list of tabs, along the top of their panels or down their left side, and the panel of
 * the tab selected below or beside it. A WAI-ARIA tab list: a click selects a tab, and while the
 * focus is on one the arrow keys select the next or the previous, Home and End the first and the
 * last; only the selected tab takes the focus with Tab.
 *
 * A closable tab has a button closing it, after its title; Delete closes the tab that has the
 * focus, and so does a click with the middle button. Closing a tab whose changes are not saved asks
 * first, in a dialog of the markup of modal.ts (so its style sheets, modal.css and button.css, are
 * needed too), or as confirmClose does: closing it then puts its fields back as they were rendered
 * and fires antilia-tabs:discard on its panel, for the page to drop changes it keeps itself. A
 * panel has changes when a field in it holds another value than the one it was rendered with, or
 * as data-modified on it or on an element in it says, see unsaved.ts; its tab shows a mark then,
 * which follows the user typing and the page changing data-modified.
 *
 * Besides its title, a tab may have actions of the page's own, buttons that come before the one
 * closing it: one telling about what the panel holds, for example. A click on them does not select
 * the tab. The actions and the close button of the selected tab take the focus with Tab, after it.
 *
 * new Tabs(element, options) works on markup the page already has:
 *
 *   <div class="antilia-tabs">
 *     <div class="antilia-tabs-list">
 *       <div class="antilia-tab" data-tab="notes" data-closable="true">
 *         <button type="button" class="antilia-tab-button">
 *           <span class="antilia-tab-icon"><svg>…</svg></span>
 *           <span class="antilia-tab-title">Notes</span>
 *         </button>
 *         <span class="antilia-tab-actions"><button type="button">…</button></span>
 *       </div>
 *     </div>
 *     <div class="antilia-tabs-panels">
 *       <div class="antilia-tabs-panel" data-tab="notes">…</div>
 *     </div>
 *   </div>
 *
 * Each tab and its panel share the name in data-tab. A tab is closable when data-closable is
 * "true", and starts closed with data-closed, which the tabs keep as the user closes them;
 * data-selected marks the tab selected at first, unless the options name one. React, Svelte and
 * Wicket render the tabs and panels, and add and remove them as they please: the tabs find them
 * as they come and go. They set the roles, the ids and the states of the tabs and panels, hide the
 * panels not selected, and add the close buttons and the marks of changes, which they own. Tabs go
 * in each other's panels.
 */

import { onRemoving, type Unsubscribe } from './host.js';
import { LOOK_PARTS, lookOf, partClasses, toggleParts, type Look, type LookPart } from './look.js';
import { resetField } from './modal.js';
import { askToDiscard, MODIFIED_ATTRIBUTE, modifiedFields, type DiscardQuestion } from './unsaved.js';

/** horizontal: the tabs along the top of the panels; vertical: down their left side. */
export type TabsOrientation = 'horizontal' | 'vertical';

/**
 * The designs of the tabs. Along the top first: tabs joined to the panel, rounded pills the selected one
 * filled with the theme's primary color, plain titles the selected one underlined, a segmented
 * control the selected segment raised, folder tabs overlapping each other, and cards lifted from
 * the page. Down the side first: the side bar of an application with an accent on the selected
 * entry, numbered steps joined by a line, and cards with an arrow pointing at the panel. Every design
 * works in both orientations.
 */
export type TabsDesign = 'tabs' | 'pills' | 'underline' | 'segmented' | 'folder' | 'lifted' |
	'sidebar' | 'steps' | 'cards';

/** The designs, those for tabs along the top first, then those for tabs down the side. */
export const TABS_DESIGNS: readonly TabsDesign[] = ['tabs', 'pills', 'underline', 'segmented', 'folder',
	'lifted', 'sidebar', 'steps', 'cards'];

/** What the tabs say: the titles of their buttons and the dialog asking before changes are lost. */
export interface TabsTexts {
	/** The title of the button closing a tab. */
	close: string;
	/** What the mark of a tab with changes not saved says. */
	modified: string;
	/** The title of the dialog asking before a tab with changes closes; {0} is its title. */
	closeModifiedTitle: string;
	/** What the dialog asking before a tab with changes closes says; {0} is its title. */
	closeModified: string;
	/** The button closing the tab and losing its changes. */
	discard: string;
	/** The button keeping the tab open. */
	keep: string;
}

/**
 * How the tabs are laid out, their design, what they say, and the callbacks.
 */
export interface TabsOptions {
	/** horizontal (the default) for the tabs along the top, vertical for down the left side. */
	orientation?: TabsOrientation;
	/** The design of the tabs, tabs when left out; see TABS_DESIGNS. */
	design?: TabsDesign;
	/**
	 * The look of the tabs, whose classes their parts get; that of the element they are in when
	 * left out (see lookOf).
	 */
	look?: Look;
	/** The name of the tab selected, the one marked data-selected or the first when left out. */
	selected?: string | null;
	/** The accessible name of the list of tabs. */
	label?: string;
	/** What the tabs say, in English when left out. */
	texts?: Partial<TabsTexts>;
	/**
	 * Asked before the user closes a tab with changes not saved, instead of the dialog of the tabs;
	 * resolving to true closes it. A page asks in a dialog of its own, for example.
	 */
	confirmClose?: (tab: string, title: string) => Promise<boolean> | boolean;
	/** Called with the name of the tab the user selected. */
	onSelect?: (tab: string) => void;
	/** Called with the name of the tab the user closed, once it is closed. */
	onClose?: (tab: string) => void;
}

export const DEFAULT_TABS_TEXTS: TabsTexts = {
	close: 'Close', modified: 'Changes not saved', closeModifiedTitle: 'Close {0}?',
	closeModified: 'The changes in {0} are not saved. Close the tab and lose them?',
	discard: 'Close and lose them', keep: 'Keep it open'
};

/** The classes of the parts of the tabs. */
export const TABS_CLASSES = {
	tabs: 'antilia-tabs',
	list: 'antilia-tabs-list',
	tab: 'antilia-tab',
	button: 'antilia-tab-button',
	icon: 'antilia-tab-icon',
	title: 'antilia-tab-title',
	actions: 'antilia-tab-actions',
	close: 'antilia-tab-close',
	/** The mark of a tab whose panel has changes not saved. */
	mark: 'antilia-tab-mark',
	/** On a tab whose panel has changes not saved. */
	modified: 'antilia-tab-modified',
	panels: 'antilia-tabs-panels',
	panel: 'antilia-tabs-panel',
	/** On the wrapper of the dialog asking before a tab with changes closes. */
	confirm: 'antilia-tabs-confirm'
} as const;

/** The classes of the orientations. */
export const TABS_ORIENTATION_CLASSES: Record<TabsOrientation, string> = {
	horizontal: 'antilia-tabs-horizontal',
	vertical: 'antilia-tabs-vertical'
};

/** The classes of the designs; the first three are those of TabsStyle in Wicket too. */
export const TABS_DESIGN_CLASSES: Record<TabsDesign, string> = Object.fromEntries(TABS_DESIGNS
	.map((design) => [design, 'antilia-tabs-' + design])) as Record<TabsDesign, string>;

/**
 * The event fired on the panel of a tab whose changes the user chose to lose, closing it: its
 * fields are back as they were rendered, the page drops the changes it keeps itself.
 */
export const TABS_DISCARD_EVENT = 'antilia-tabs:discard';

/** The attribute naming a tab and its panel. */
const NAME_ATTRIBUTE = 'data-tab';

/** The path of the icon of the close button, in a 24 by 24 box. */
const CLOSE_ICON = 'M6 6l12 12 M18 6L6 18';

const SVG = 'http://www.w3.org/2000/svg';

/** The tabs by their element. */
const instances = new WeakMap<Element, Tabs>();

let counter = 0;

/**
 * @returns a text with its {0} replaced
 */
function format(text: string, value: string): string {
	return text.split('{0}').join(value);
}

function resolve(target: HTMLElement | string): HTMLElement | null {
	return typeof target === 'string' ? document.getElementById(target) : target;
}

/**
 * @returns the child elements of an element that have a class
 */
function childrenOf(parent: Element | null, className: string): HTMLElement[] {
	return parent ? (Array.from(parent.children) as HTMLElement[])
		.filter((child) => child.classList.contains(className)) : [];
}

function closeIcon(): SVGSVGElement {
	const svg = document.createElementNS(SVG, 'svg');
	svg.setAttribute('viewBox', '0 0 24 24');
	svg.setAttribute('width', '12');
	svg.setAttribute('height', '12');
	svg.setAttribute('aria-hidden', 'true');
	svg.setAttribute('focusable', 'false');
	const shape = document.createElementNS(SVG, 'path');
	shape.setAttribute('d', CLOSE_ICON);
	shape.setAttribute('fill', 'none');
	shape.setAttribute('stroke', 'currentColor');
	shape.setAttribute('stroke-width', '2.5');
	shape.setAttribute('stroke-linecap', 'round');
	svg.append(shape);
	return svg;
}

/**
 * A tab: its element, the parts in it and its panel.
 */
interface Tab {
	name: string;
	element: HTMLElement;
	button: HTMLElement;
	panel: HTMLElement | null;
	/** The close button, added by the tabs when the tab is closable. */
	close: HTMLButtonElement | null;
	/** The mark of changes not saved, added by the tabs. */
	mark: HTMLElement | null;
}

export class Tabs {
	readonly element: HTMLElement;

	options: TabsOptions;

	/** The tabs, in the order of the page. */
	private tabs: Tab[] = [];

	/** The name of the tab selected, if any. */
	private current: string | null = null;

	private listeners: Array<[EventTarget, string, EventListener]> = [];

	private observer: MutationObserver | null = null;

	private stopWatchingRemoval: Unsubscribe | null = null;

	/** The dialog asking before a tab with changes closes, while it is open. */
	private question: DiscardQuestion | null = null;

	/** The look whose classes the tabs gave their element and its list, when they rendered last. */
	private applied: Look | null = null;

	/**
	 * @param element the element of the tabs: a child of the class antilia-tabs-list holds the
	 *        tabs, and one of the class antilia-tabs-panels the panels
	 * @param options the orientation, the design, the tab selected, the texts and the callbacks
	 */
	constructor(element: HTMLElement, options: TabsOptions = {}) {
		this.element = element;
		this.options = { ...options };
		instances.get(element)?.destroy();
		instances.set(element, this);
		if (!element.id) {
			element.id = 'antilia-tabs-' + (++counter);
		}
		this.scan();
		const marked = this.tabs.find((tab) => tab.element.hasAttribute('data-selected') &&
			!this.isClosed(tab.name));
		this.current = this.openTab(options.selected ?? null)?.name ?? marked?.name ??
			this.firstOpen()?.name ?? null;
		this.render();
		this.bind();
		this.stopWatchingRemoval = onRemoving(element, () => this.detach());
	}

	/**
	 * Makes tabs of the element, as Wicket does.
	 *
	 * @param element the element or its id
	 * @param options the orientation, the design, the tab selected, the texts and the callbacks
	 * @returns the tabs, null when there is no such element
	 */
	static attach(element: HTMLElement | string, options?: TabsOptions): Tabs | null {
		const resolved = resolve(element);
		return resolved ? new Tabs(resolved, options) : null;
	}

	/**
	 * @param target the element of the tabs or its id
	 * @returns the tabs, if there are
	 */
	static of(target: HTMLElement | string | null | undefined): Tabs | null {
		const element = typeof target === 'string' ? document.getElementById(target) : target;
		return (element && instances.get(element)) || null;
	}

	private list(): HTMLElement | null {
		return childrenOf(this.element, TABS_CLASSES.list)[0] ?? null;
	}

	private panels(): HTMLElement | null {
		return childrenOf(this.element, TABS_CLASSES.panels)[0] ?? null;
	}

	/**
	 * Finds the tabs and their panels as the page has them now, keeping the parts added to those
	 * it had already.
	 */
	private scan(): void {
		const panels = new Map(childrenOf(this.panels(), TABS_CLASSES.panel)
			.map((panel) => [panel.getAttribute(NAME_ATTRIBUTE) ?? '', panel]));
		const before = new Map(this.tabs.map((tab) => [tab.element, tab]));
		this.tabs = childrenOf(this.list(), TABS_CLASSES.tab).flatMap((element) => {
			const name = element.getAttribute(NAME_ATTRIBUTE);
			const button = childrenOf(element, TABS_CLASSES.button)[0];
			if (!name || !button) {
				return [];
			}
			const known = before.get(element);
			const tab: Tab = known ?? { name, element, button, panel: null, close: null, mark: null };
			tab.name = name;
			tab.button = button;
			tab.panel = panels.get(name) ?? null;
			// parts the framework took away when it rendered the tab again
			if (tab.close && !element.contains(tab.close)) {
				tab.close = null;
			}
			if (tab.mark && !button.contains(tab.mark)) {
				tab.mark = null;
			}
			return [tab];
		});
	}

	private tab(name: string | null | undefined): Tab | undefined {
		return name == null ? undefined : this.tabs.find((tab) => tab.name === name);
	}

	/**
	 * @returns the tab of the name if it is open
	 */
	private openTab(name: string | null): Tab | undefined {
		const tab = this.tab(name);
		return tab && !this.isClosed(tab.name) ? tab : undefined;
	}

	private firstOpen(): Tab | undefined {
		return this.tabs.find((tab) => !this.isClosed(tab.name));
	}

	orientation(): TabsOrientation {
		return this.options.orientation === 'vertical' ? 'vertical' : 'horizontal';
	}

	design(): TabsDesign {
		const design = this.options.design;
		return design && TABS_DESIGNS.includes(design) ? design : 'tabs';
	}

	/**
	 * @returns the look of the tabs: that of the options, else that of the element they are in
	 */
	look(): Look {
		return this.options.look ?? lookOf(this.element);
	}

	/**
	 * @returns the classes of parts of the tabs in their look
	 */
	private css(...parts: LookPart[]): string {
		return partClasses(this.look(), ...parts);
	}

	/**
	 * Gives the element of the tabs and their list the classes of the orientation and the design,
	 * in a look, taking away those of the others; none of them when off.
	 *
	 * @param list the list of the tabs, found before: off takes its class away too
	 */
	private renderParts(look: Look, on: boolean, list: HTMLElement | null): void {
		const orientation = this.orientation();
		const design = this.design();
		const element = this.element;
		toggleParts(element, look, on, 'tabs');
		(Object.keys(TABS_ORIENTATION_CLASSES) as TabsOrientation[]).forEach((each) =>
			toggleParts(element, look, on && each === orientation, `tabs-${each}`));
		TABS_DESIGNS.forEach((each) => {
			toggleParts(element, look, on && each === design, `tabs-${each}`);
		});
		if (list) {
			const parts = TABS_DESIGNS.map((each) => `tabs-list-${each}`)
				.filter((part): part is LookPart => part in LOOK_PARTS);
			const current = parts.find((part) => on && part === `tabs-list-${design}`);
			// the parts of the designs share classes, Bootstrap's nav: those of the others go
			// unless the list keeps them
			const kept = new Set(partClasses(look, 'tabs-list', current).split(' '));
			const gone = partClasses(look, ...parts.filter((part) => part !== current)).split(' ')
				.filter((css) => css && !kept.has(css));
			list.classList.remove(...gone);
			toggleParts(list, look, on, 'tabs-list', current);
		}
	}

	private texts(): TabsTexts {
		return { ...DEFAULT_TABS_TEXTS, ...this.options.texts };
	}

	/**
	 * @returns the names of the tabs, in order, closed ones too
	 */
	names(): string[] {
		return this.tabs.map((tab) => tab.name);
	}

	/**
	 * @returns the names of the tabs that are not closed, in order
	 */
	openNames(): string[] {
		return this.tabs.filter((tab) => !this.isClosed(tab.name)).map((tab) => tab.name);
	}

	/**
	 * @returns the name of the tab selected, null when none is open
	 */
	selected(): string | null {
		return this.current;
	}

	/**
	 * @returns whether the tab is closed, or there is no such tab
	 */
	isClosed(name: string): boolean {
		const tab = this.tab(name);
		return !tab || tab.element.hasAttribute('data-closed');
	}

	private isClosable(tab: Tab): boolean {
		return tab.element.getAttribute('data-closable') === 'true';
	}

	private render(): void {
		const orientation = this.orientation();
		const look = this.look();
		const list = this.list();
		// a look other than the one before: its classes go
		if (this.applied && this.applied !== look) {
			this.renderParts(this.applied, false, list);
		}
		this.renderParts(look, true, list);
		this.applied = look;
		if (list) {
			list.setAttribute('role', 'tablist');
			list.setAttribute('aria-orientation', orientation);
			if (this.options.label) {
				list.setAttribute('aria-label', this.options.label);
			} else {
				list.removeAttribute('aria-label');
			}
		}
		if (this.current !== null && !this.openTab(this.current)) {
			this.current = this.firstOpen()?.name ?? null;
		}
		this.tabs.forEach((tab) => this.renderTab(tab));
	}

	private renderTab(tab: Tab): void {
		const prefix = this.element.id;
		const selected = tab.name === this.current;
		const closed = this.isClosed(tab.name);
		const button = tab.button;
		tab.element.setAttribute('role', 'presentation');
		tab.element.toggleAttribute('data-selected', selected);
		tab.element.hidden = closed;
		if (button instanceof HTMLButtonElement && !button.hasAttribute('type')) {
			button.type = 'button';
		}
		if (!button.id) {
			button.id = prefix + '-tab-' + tab.name;
		}
		button.setAttribute('role', 'tab');
		button.setAttribute('aria-selected', String(selected));
		button.tabIndex = selected ? 0 : -1;
		const panel = tab.panel;
		if (panel) {
			if (!panel.id) {
				panel.id = prefix + '-panel-' + tab.name;
			}
			button.setAttribute('aria-controls', panel.id);
			panel.setAttribute('role', 'tabpanel');
			panel.setAttribute('aria-labelledby', button.id);
			panel.hidden = !selected || closed;
		} else {
			button.removeAttribute('aria-controls');
		}
		// the actions and the close button of the selected tab come after it with Tab
		tab.element.querySelectorAll<HTMLElement>('.' + TABS_CLASSES.actions + ' :is(button, a, ' +
			'input, select, [tabindex])').forEach((control) => {
			control.tabIndex = selected ? 0 : -1;
		});
		this.renderClose(tab, selected);
		this.renderMark(tab);
	}

	/**
	 * Adds the close button to a closable tab, or takes it away.
	 */
	private renderClose(tab: Tab, selected: boolean): void {
		if (!this.isClosable(tab)) {
			tab.close?.remove();
			tab.close = null;
			return;
		}
		if (!tab.close) {
			const close = document.createElement('button');
			close.type = 'button';
			close.append(closeIcon());
			tab.close = close;
		}
		tab.close.className = this.css('tab-close');
		// after the title and the actions, which a framework may have rendered since
		if (tab.element.lastElementChild !== tab.close) {
			tab.element.append(tab.close);
		}
		const label = this.texts().close;
		const title = this.titleOf(tab);
		tab.close.title = label;
		tab.close.setAttribute('aria-label', title ? label + ' ' + title : label);
		tab.close.tabIndex = selected ? 0 : -1;
	}

	/**
	 * Shows the mark of changes not saved on a tab whose panel has them, or hides it.
	 */
	private renderMark(tab: Tab): void {
		const modified = tab.panel !== null && modifiedFields(tab.panel) !== null;
		toggleParts(tab.element, this.look(), modified, 'tab-modified');
		if (!modified) {
			if (tab.mark) {
				tab.mark.hidden = true;
			}
			return;
		}
		if (!tab.mark) {
			const mark = document.createElement('span');
			mark.setAttribute('role', 'img');
			tab.button.append(mark);
			tab.mark = mark;
		}
		tab.mark.className = this.css('tab-mark');
		const text = this.texts().modified;
		tab.mark.title = text;
		tab.mark.setAttribute('aria-label', text);
		tab.mark.hidden = false;
	}

	/**
	 * @returns the title of a tab, as its title element or its button says
	 */
	private titleOf(tab: Tab): string {
		const title = tab.button.querySelector('.' + TABS_CLASSES.title) ?? tab.button;
		return (title.textContent ?? '').trim();
	}

	/**
	 * @returns the title of the tab, empty when there is none
	 */
	title(name: string): string {
		const tab = this.tab(name);
		return tab ? this.titleOf(tab) : '';
	}

	private listen<E extends Event>(target: EventTarget, type: string,
		listener: (event: E) => void): void {
		target.addEventListener(type, listener as EventListener);
		this.listeners.push([target, type, listener as EventListener]);
	}

	private bind(): void {
		this.listen<MouseEvent>(this.element, 'click', (event) => this.onClick(event));
		this.listen<MouseEvent>(this.element, 'auxclick', (event) => this.onAuxClick(event));
		this.listen<MouseEvent>(this.element, 'mousedown', (event) => {
			// no scrolling with the middle button over a closable tab, which it closes
			if (event.button === 1 && this.closableTabOf(event)) {
				event.preventDefault();
			}
		});
		this.listen<KeyboardEvent>(this.element, 'keydown', (event) => this.onKeyDown(event));
		// the marks follow the user changing the fields of the panels
		this.listen<Event>(this.element, 'input', (event) => this.onEdit(event));
		this.listen<Event>(this.element, 'change', (event) => this.onEdit(event));
		this.observer = new MutationObserver((records) => this.onMutation(records));
		this.observer.observe(this.element, { childList: true, subtree: true, attributes: true,
			attributeFilter: [MODIFIED_ATTRIBUTE, 'data-closable', NAME_ATTRIBUTE] });
	}

	/**
	 * @returns the tab of this element the event happened in, not one of tabs in a panel
	 */
	private tabOf(event: Event): Tab | undefined {
		const target = event.target as Element | null;
		const element = target?.closest?.('.' + TABS_CLASSES.tab);
		return element ? this.tabs.find((tab) => tab.element === element) : undefined;
	}

	private closableTabOf(event: Event): Tab | undefined {
		const tab = this.tabOf(event);
		return tab && this.isClosable(tab) ? tab : undefined;
	}

	private onClick(event: MouseEvent): void {
		const tab = this.tabOf(event);
		if (!tab) {
			return;
		}
		const target = event.target as Element;
		if (tab.close?.contains(target)) {
			event.preventDefault();
			void this.requestClose(tab.name);
		} else if (tab.button.contains(target)) {
			this.select(tab.name, true);
		}
	}

	private onAuxClick(event: MouseEvent): void {
		const tab = this.closableTabOf(event);
		if (event.button === 1 && tab) {
			event.preventDefault();
			void this.requestClose(tab.name);
		}
	}

	private onKeyDown(event: KeyboardEvent): void {
		const tab = this.tabOf(event);
		if (!tab || !tab.button.contains(event.target as Node) || event.altKey || event.ctrlKey ||
			event.metaKey) {
			return;
		}
		const open = this.tabs.filter((each) => !this.isClosed(each.name));
		const index = open.indexOf(tab);
		const horizontal = this.orientation() === 'horizontal';
		let next: Tab | undefined;
		switch (event.key) {
			case horizontal ? 'ArrowRight' : 'ArrowDown':
				next = open[(index + 1) % open.length];
				break;
			case horizontal ? 'ArrowLeft' : 'ArrowUp':
				next = open[(index - 1 + open.length) % open.length];
				break;
			case 'Home':
				next = open[0];
				break;
			case 'End':
				next = open[open.length - 1];
				break;
			case 'Delete':
				if (this.isClosable(tab)) {
					event.preventDefault();
					void this.requestClose(tab.name);
				}
				return;
			default:
				return;
		}
		event.preventDefault();
		if (next) {
			this.select(next.name, true);
			next.button.focus();
		}
	}

	private onEdit(event: Event): void {
		const target = event.target as Node | null;
		// the panel of this element the field is in, maybe in the panel of tabs in it
		const tab = target ? this.tabs.find((each) => each.panel?.contains(target)) : undefined;
		if (tab) {
			this.renderMark(tab);
		}
	}

	private onMutation(records: MutationRecord[]): void {
		const list = this.list();
		const panels = this.panels();
		// what the page changed in the tabs, the panels it added or took away, data-modified in
		// a panel; not what changed deep in a panel, nor the parts the tabs add, which are theirs
		const own = (node: Node) => this.tabs.some((tab) => node === tab.close || node === tab.mark);
		const relevant = records.some((record) => record.type === 'attributes' ||
			((record.target === this.element || record.target === panels ||
				!!list?.contains(record.target)) &&
				[...Array.from(record.addedNodes), ...Array.from(record.removedNodes)]
					.some((node) => !own(node))));
		if (relevant) {
			this.refresh();
		}
	}

	/**
	 * Finds the tabs and panels the page added, took away or rendered again, and renders them.
	 */
	refresh(): void {
		this.scan();
		this.render();
		// what the tabs changed is theirs, not the page's
		this.observer?.takeRecords();
	}

	/**
	 * Selects a tab, showing its panel; a closed one opens again.
	 *
	 * @param name the name of the tab
	 * @param user whether the user did, calling back
	 */
	select(name: string, user = false): void {
		if (!this.tab(name)) {
			// one the page has just added
			this.scan();
		}
		const tab = this.tab(name);
		if (!tab) {
			return;
		}
		const reopened = this.isClosed(name);
		tab.element.removeAttribute('data-closed');
		if (this.current === name && !reopened) {
			return;
		}
		this.current = name;
		this.render();
		this.observer?.takeRecords();
		tab.button.scrollIntoView?.({ block: 'nearest', inline: 'nearest' });
		if (user) {
			this.options.onSelect?.(name);
		}
	}

	/**
	 * Opens a closed tab again, selecting it.
	 *
	 * @param name the name of the tab
	 */
	open(name: string): void {
		this.select(name);
	}

	/**
	 * Whether the panel of a tab has changes not saved, which closing it loses: as data-modified
	 * says on it or on an element in it, else whether a field in it holds another value than the
	 * one it was rendered with.
	 *
	 * @param name the name of the tab
	 */
	isModified(name: string): boolean {
		const panel = this.tab(name)?.panel;
		return !!panel && modifiedFields(panel) !== null;
	}

	/**
	 * Says whether the panel of a tab has changes not saved, as data-modified on it does.
	 *
	 * @param name the name of the tab
	 * @param modified whether it has changes; null for the tabs to tell by its fields
	 */
	setModified(name: string, modified: boolean | null): void {
		const tab = this.tab(name);
		if (!tab?.panel) {
			return;
		}
		if (modified === null) {
			tab.panel.removeAttribute(MODIFIED_ATTRIBUTE);
		} else {
			tab.panel.setAttribute(MODIFIED_ATTRIBUTE, String(modified));
		}
		this.renderMark(tab);
		this.observer?.takeRecords();
	}

	/**
	 * Closes a tab: its tab and its panel are hidden until it is opened again, and the next tab
	 * is selected when it was, or the previous one.
	 *
	 * @param name the name of the tab
	 * @param user whether the user did, calling back
	 */
	close(name: string, user = false): void {
		const tab = this.tab(name);
		if (!tab || this.isClosed(name)) {
			return;
		}
		const hadFocus = tab.element.contains(document.activeElement) ||
			!!tab.panel?.contains(document.activeElement);
		if (this.current === name) {
			const open = this.tabs.filter((each) => !this.isClosed(each.name));
			const index = open.indexOf(tab);
			this.current = (open[index + 1] ?? open[index - 1])?.name ?? null;
		}
		tab.element.setAttribute('data-closed', '');
		this.render();
		this.observer?.takeRecords();
		if (hadFocus) {
			this.tab(this.current)?.button.focus();
		}
		if (user) {
			if (this.current !== null) {
				this.options.onSelect?.(this.current);
			}
			this.options.onClose?.(name);
		}
	}

	/**
	 * Closes a tab as its close button does: one with changes not saved asks first, in the dialog
	 * of the tabs or as confirmClose of the options does, and closing it puts the fields of its
	 * panel back as they were rendered.
	 *
	 * @param name the name of the tab
	 * @returns whether the tab closed
	 */
	async requestClose(name: string): Promise<boolean> {
		const tab = this.tab(name);
		if (!tab || this.isClosed(name)) {
			return false;
		}
		const fields = tab.panel ? modifiedFields(tab.panel) : null;
		if (fields !== null) {
			const title = this.titleOf(tab);
			const confirmed = this.options.confirmClose ?
				await this.options.confirmClose(name, title) : await this.askToClose(title);
			if (!confirmed || this.tab(name) !== tab || this.isClosed(name)) {
				return false;
			}
			fields.forEach((field) => resetField(field));
			// for the page to drop the changes it keeps itself
			tab.panel?.dispatchEvent(new CustomEvent(TABS_DISCARD_EVENT, { bubbles: true,
				detail: { tab: name } }));
		}
		this.close(name, true);
		return true;
	}

	/**
	 * Asks whether to close a tab and lose its changes, in the dialog of the tabs, of the markup
	 * of modal.ts; what requestClose() does unless confirmClose of the options asks.
	 *
	 * @param title the title of the tab
	 * @returns whether to close it
	 */
	askToClose(title: string): Promise<boolean> {
		this.question?.cancel();
		const texts = this.texts();
		const question = askToDiscard({ title: format(texts.closeModifiedTitle, title),
			text: format(texts.closeModified, title), discard: texts.discard, keep: texts.keep },
		this.css('tabs-confirm'), this.element.id + '-confirm', this.look());
		this.question = question;
		return question.answer.finally(() => {
			if (this.question === question) {
				this.question = null;
			}
		});
	}

	/**
	 * Takes other options; those left out stay as they were. A tab named selected is selected.
	 */
	configure(options: TabsOptions = {}): void {
		const defined = Object.fromEntries(Object.entries(options)
			.filter(([, value]) => value !== undefined)) as TabsOptions;
		this.options = { ...this.options, ...defined };
		// tabs the page has just added, and may name
		this.scan();
		const selected = this.tab(defined.selected);
		if (selected) {
			selected.element.removeAttribute('data-closed');
			this.current = selected.name;
		}
		this.render();
		this.observer?.takeRecords();
	}

	/**
	 * Takes the tabs away, leaving the tabs and panels of the page with the panels shown; the
	 * close buttons and the marks go.
	 */
	destroy(): void {
		this.detach();
		this.question?.cancel();
		const element = this.element;
		const look = this.applied ?? this.look();
		const list = this.list();
		this.renderParts(look, false, list);
		// the classes of the tabs and their list stay, as the page has them
		element.classList.add(TABS_CLASSES.tabs);
		list?.classList.add(TABS_CLASSES.list);
		this.tabs.forEach((tab) => {
			tab.close?.remove();
			tab.mark?.remove();
			toggleParts(tab.element, look, false, 'tab-modified');
			if (tab.panel) {
				tab.panel.hidden = false;
			}
		});
		this.tabs = [];
		if (instances.get(element) === this) {
			instances.delete(element);
		}
	}

	/**
	 * Stops listening, leaving the elements in place.
	 */
	private detach(): void {
		this.listeners.forEach(([target, type, listener]) => {
			target.removeEventListener(type, listener);
		});
		this.listeners = [];
		this.observer?.disconnect();
		this.observer = null;
		this.stopWatchingRemoval?.();
		this.stopWatchingRemoval = null;
	}
}

/**
 * Makes tabs of an element, in the shape of a Svelte action: use:tabs={options}.
 *
 * @param element the element of the tabs
 * @param options the orientation, the design, the tab selected, the texts and the callbacks
 * @returns update() taking other options, destroy() taking the tabs away
 */
export function tabs(element: HTMLElement, options?: TabsOptions): {
	update(options?: TabsOptions): void;
	destroy(): void;
} {
	const instance = new Tabs(element, options);
	return {
		update: (next) => instance.configure(next),
		destroy: () => instance.destroy()
	};
}
