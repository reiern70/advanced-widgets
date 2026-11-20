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

/*
 * The menu of all tabs, an extension of the tabs (tabs.ts) for more tabs than fit. While the list
 * of tabs overflows, scrolling sideways, or down for tabs down the side, a button at its end opens
 * a menu of all the tabs open, in their order: each with the icon and the title of its tab and the
 * mark of changes not saved, the tab selected checked (aria-current). Choosing one selects its tab
 * as a click on it does, which scrolls it into view, and gives it the focus. The button stays at
 * the end of the list (position: sticky) as the tabs scroll under it, and is hidden while all the
 * tabs fit.
 *
 * The menu is a Menu of menu.ts, with its keyboard and its placement, so menu.css is needed too.
 * new TabsOverflowMenu(element, options) works on the markup of the tabs alone, whatever renders
 * them and whichever Tabs instance drives them: element is the element of the tabs, of the class
 * antilia-tabs. It follows the tabs as they come and go, change their titles, their marks and the
 * tab selected, and the list as it is resized.
 */

import { onRemoving, type Unsubscribe } from './host.js';
import { type Look } from './look.js';
import { Menu, MENU_CLASSES } from './menu.js';

/** The options of the menu of all tabs. */
export interface TabsOverflowMenuOptions {
	/** The accessible name and the title of the button opening the menu; "All tabs" by default. */
	label?: string;
	/** The look of the menu; that of the element of the tabs when left out (see lookOf). */
	look?: Look;
}

/** The classes of the parts of the menu of all tabs. */
export const TABS_OVERFLOW_CLASSES = {
	/** The element at the end of the list of tabs, holding the button and its menu. */
	menu: 'antilia-tabs-overflow',
	/** The button opening the menu. */
	button: 'antilia-tabs-overflow-button',
	/** The mark of a tab with changes not saved, in the menu. */
	mark: 'antilia-tabs-overflow-mark'
} as const;

export const DEFAULT_TABS_OVERFLOW_LABEL = 'All tabs';

/** The attribute naming the tab of an item of the menu. */
const TAB_ATTRIBUTE = 'data-tabs-overflow-tab';

/** The classes of the tabs this menu reads, as tabs.ts has them. */
const TABS = {
	list: 'antilia-tabs-list',
	tab: 'antilia-tab',
	button: 'antilia-tab-button',
	icon: 'antilia-tab-icon',
	title: 'antilia-tab-title',
	mark: 'antilia-tab-mark'
};

/** The path of the icon of the button, a chevron pointing down, in a 24 by 24 box. */
const CHEVRON = 'M6 9l6 6 6-6';

const SVG = 'http://www.w3.org/2000/svg';

/** The menus by the element of their tabs. */
const instances = new WeakMap<Element, TabsOverflowMenu>();

function chevron(): SVGSVGElement {
	const svg = document.createElementNS(SVG, 'svg');
	svg.setAttribute('viewBox', '0 0 24 24');
	svg.setAttribute('width', '14');
	svg.setAttribute('height', '14');
	svg.setAttribute('aria-hidden', 'true');
	svg.setAttribute('focusable', 'false');
	const path = document.createElementNS(SVG, 'path');
	path.setAttribute('d', CHEVRON);
	path.setAttribute('fill', 'none');
	path.setAttribute('stroke', 'currentColor');
	path.setAttribute('stroke-width', '2.5');
	path.setAttribute('stroke-linecap', 'round');
	path.setAttribute('stroke-linejoin', 'round');
	svg.append(path);
	return svg;
}

/** A tab as the menu shows it. */
interface Entry {
	name: string;
	title: string;
	/** The picture of the icon of the tab, an svg or an img, null for none. */
	icon: Element | null;
	selected: boolean;
	/** What the mark of changes says, null without changes. */
	modified: string | null;
}

export class TabsOverflowMenu {
	/** The element of the tabs. */
	readonly element: HTMLElement;

	options: TabsOverflowMenuOptions;

	/** The element at the end of the list, holding the button and the menu. */
	readonly container: HTMLElement;

	readonly button: HTMLButtonElement;

	private readonly submenu: HTMLUListElement;

	private readonly menu: Menu;

	private observer: MutationObserver | null = null;

	private resizes: ResizeObserver | null = null;

	private stopWatchingRemoval: Unsubscribe | null = null;

	/** The tabs as the menu last showed them, to build it again only when they change. */
	private shown = '';

	private scheduled = false;

	/**
	 * @param element the element of the tabs, of the class antilia-tabs
	 * @param options the label of the button and the look
	 */
	constructor(element: HTMLElement, options: TabsOverflowMenuOptions = {}) {
		this.element = element;
		this.options = { ...options };
		instances.get(element)?.destroy();
		instances.set(element, this);

		this.container = document.createElement('div');
		this.container.className = TABS_OVERFLOW_CLASSES.menu + ' ' + MENU_CLASSES.menu;
		this.container.hidden = true;
		const bar = document.createElement('ul');
		bar.className = MENU_CLASSES.list;
		const item = document.createElement('li');
		item.className = MENU_CLASSES.item;
		this.button = document.createElement('button');
		this.button.type = 'button';
		this.button.className = MENU_CLASSES.link + ' ' + TABS_OVERFLOW_CLASSES.button;
		this.button.append(chevron());
		this.submenu = document.createElement('ul');
		this.submenu.className = MENU_CLASSES.submenu;
		item.append(this.button, this.submenu);
		bar.append(item);
		this.container.append(bar);
		this.renderLabel();

		this.list()?.append(this.container);
		this.sync();
		this.menu = new Menu(this.container, { orientation: this.orientation(), look: options.look,
			onSelect: (link) => this.choose(link.getAttribute(TAB_ATTRIBUTE)) });
		this.update();
		this.bind();
		this.stopWatchingRemoval = onRemoving(element, () => this.detach());
	}

	/**
	 * Adds the menu of all tabs to tabs, as Wicket does.
	 *
	 * @param element the element of the tabs or its id
	 * @param options the label of the button and the look
	 * @returns the menu, null when there is no such element
	 */
	static attach(element: HTMLElement | string, options?: TabsOverflowMenuOptions):
		TabsOverflowMenu | null {
		const resolved = typeof element === 'string' ? document.getElementById(element) : element;
		return resolved ? new TabsOverflowMenu(resolved, options) : null;
	}

	/**
	 * @param target the element of the tabs or its id
	 * @returns the menu of all tabs of the tabs, if they have one
	 */
	static of(target: HTMLElement | string | null | undefined): TabsOverflowMenu | null {
		const element = typeof target === 'string' ? document.getElementById(target) : target;
		return (element && instances.get(element)) || null;
	}

	private list(): HTMLElement | null {
		return (Array.from(this.element.children) as HTMLElement[])
			.find((child) => child.classList.contains(TABS.list)) ?? null;
	}

	private tabElements(): HTMLElement[] {
		const list = this.list();
		return list ? (Array.from(list.children) as HTMLElement[])
			.filter((child) => child.classList.contains(TABS.tab)) : [];
	}

	private orientation(): 'horizontal' | 'vertical' {
		return this.list()?.getAttribute('aria-orientation') === 'vertical' ? 'vertical' :
			'horizontal';
	}

	/**
	 * @returns whether the list of tabs overflows, so that the menu shows
	 */
	overflows(): boolean {
		return !this.container.hidden;
	}

	/**
	 * @returns the names of the tabs the menu offers, in order
	 */
	names(): string[] {
		return this.entries().map((entry) => entry.name);
	}

	private renderLabel(): void {
		const label = this.options.label ?? DEFAULT_TABS_OVERFLOW_LABEL;
		this.button.title = label;
		this.button.setAttribute('aria-label', label);
	}

	/**
	 * @returns the tabs open, as the menu shows them
	 */
	private entries(): Entry[] {
		return this.tabElements().filter((tab) => !tab.hidden && tab.getAttribute('data-tab'))
			.map((tab) => {
				const button = tab.querySelector<HTMLElement>('.' + TABS.button);
				const title = button?.querySelector('.' + TABS.title) ?? button;
				const mark = button?.querySelector<HTMLElement>('.' + TABS.mark);
				return {
					name: tab.getAttribute('data-tab')!,
					title: (title?.textContent ?? '').trim(),
					icon: button?.querySelector('.' + TABS.icon + ' :is(svg, img)') ?? null,
					selected: button?.getAttribute('aria-selected') === 'true',
					modified: mark && !mark.hidden ? mark.getAttribute('aria-label') ?? '' : null
				};
			});
	}

	/**
	 * Builds the items of the menu again when the tabs changed since.
	 */
	private sync(): void {
		const entries = this.entries();
		const shown = JSON.stringify(entries.map((entry) => ({ ...entry,
			icon: entry.icon?.outerHTML ?? '' })));
		if (shown === this.shown) {
			return;
		}
		this.shown = shown;
		const focused = this.submenu.contains(document.activeElement) ?
			(document.activeElement as HTMLElement).getAttribute(TAB_ATTRIBUTE) : null;
		this.submenu.replaceChildren(...entries.map((entry) => {
			const item = document.createElement('li');
			item.className = MENU_CLASSES.submenuItem;
			const link = document.createElement('button');
			link.type = 'button';
			link.className = MENU_CLASSES.submenuLink;
			link.setAttribute(TAB_ATTRIBUTE, entry.name);
			if (entry.selected) {
				link.setAttribute('aria-current', 'true');
			}
			if (entry.icon) {
				// the picture alone, cloned: markup parsed again with the style of a framework's
				// wrapper would break a strict Content Security Policy
				const icon = document.createElement('span');
				icon.className = MENU_CLASSES.icon;
				icon.append(entry.icon.cloneNode(true));
				link.append(icon);
			}
			const label = document.createElement('span');
			label.className = MENU_CLASSES.label;
			label.textContent = entry.title;
			link.append(label);
			if (entry.modified !== null) {
				const mark = document.createElement('span');
				mark.className = TABS_OVERFLOW_CLASSES.mark;
				mark.setAttribute('role', 'img');
				mark.setAttribute('aria-label', entry.modified);
				mark.title = entry.modified;
				link.append(mark);
			}
			item.append(link);
			return item;
		}));
		if (focused) {
			Array.from(this.submenu.querySelectorAll<HTMLElement>('[' + TAB_ATTRIBUTE + ']'))
				.find((link) => link.getAttribute(TAB_ATTRIBUTE) === focused)?.focus();
		}
	}

	/**
	 * Keeps the button at the end of the list, the items as the tabs are, and shows the button
	 * while the list overflows.
	 */
	update(): void {
		const list = this.list();
		if (!list) {
			return;
		}
		if (list.lastElementChild !== this.container) {
			list.append(this.container);
		}
		this.sync();
		const orientation = this.orientation();
		if (this.menu.orientation() !== orientation) {
			this.menu.configure({ orientation });
		}
		// measured without the button, which takes room of its own
		this.container.hidden = true;
		const overflows = orientation === 'vertical' ?
			list.scrollHeight > list.clientHeight + 1 : list.scrollWidth > list.clientWidth + 1;
		this.container.hidden = !overflows;
		if (!overflows) {
			this.menu.closeAll();
		}
	}

	/**
	 * Updates at the next frame, once for many changes.
	 */
	private schedule(): void {
		if (this.scheduled) {
			return;
		}
		this.scheduled = true;
		const run = () => {
			this.scheduled = false;
			this.update();
			// what this menu changed is its own
			this.observer?.takeRecords();
		};
		if (typeof requestAnimationFrame === 'function') {
			requestAnimationFrame(run);
		} else {
			setTimeout(run, 0);
		}
	}

	/**
	 * Selects the tab chosen in the menu, as a click on it does, and gives it the focus.
	 */
	private choose(name: string | null): void {
		const tab = this.tabElements().find((each) => each.getAttribute('data-tab') === name);
		const button = tab?.querySelector<HTMLElement>('.' + TABS.button);
		if (!button) {
			return;
		}
		button.click();
		button.focus();
		button.scrollIntoView?.({ block: 'nearest', inline: 'nearest' });
	}

	private bind(): void {
		this.observer = new MutationObserver((records) => {
			// the tabs, their titles, marks and states; not this menu
			if (records.some((record) => !this.container.contains(record.target) &&
				record.target !== this.container)) {
				this.schedule();
			}
		});
		this.observer.observe(this.element, { childList: true, subtree: true, attributes: true,
			characterData: true, attributeFilter: ['aria-selected', 'aria-orientation', 'hidden',
				'class', 'data-tab'] });
		if (typeof ResizeObserver === 'function') {
			this.resizes = new ResizeObserver(() => this.schedule());
			this.resizes.observe(this.element);
			const list = this.list();
			if (list) {
				this.resizes.observe(list);
			}
		}
	}

	/**
	 * Takes other options; those left out stay as they were.
	 */
	configure(options: TabsOverflowMenuOptions = {}): void {
		const defined = Object.fromEntries(Object.entries(options)
			.filter(([, value]) => value !== undefined)) as TabsOverflowMenuOptions;
		this.options = { ...this.options, ...defined };
		this.renderLabel();
		if (defined.look) {
			this.menu.configure({ look: defined.look });
		}
		this.update();
	}

	/**
	 * Takes the menu away, the button with it.
	 */
	destroy(): void {
		this.detach();
		this.menu.destroy();
		this.container.remove();
		if (instances.get(this.element) === this) {
			instances.delete(this.element);
		}
	}

	/**
	 * Stops listening, leaving the elements in place.
	 */
	private detach(): void {
		this.observer?.disconnect();
		this.observer = null;
		this.resizes?.disconnect();
		this.resizes = null;
		this.stopWatchingRemoval?.();
		this.stopWatchingRemoval = null;
	}
}

/**
 * Adds the menu of all tabs to tabs, in the shape of a Svelte action:
 * use:tabsOverflowMenu={options}, after use:tabs.
 *
 * @param element the element of the tabs
 * @param options the label of the button and the look
 * @returns update() taking other options, destroy() taking the menu away
 */
export function tabsOverflowMenu(element: HTMLElement, options?: TabsOverflowMenuOptions): {
	update(options?: TabsOverflowMenuOptions): void;
	destroy(): void;
} {
	const instance = new TabsOverflowMenu(element, options);
	return {
		update: (next) => instance.configure(next),
		destroy: () => instance.destroy()
	};
}
