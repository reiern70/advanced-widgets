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
 * Menus: a bar of items along the top of a page or down its side, each leading somewhere, a link,
 * doing something, a button, or opening a submenu of more items, nested as deep as one likes. A
 * WAI-ARIA menu bar: a click on an item with a submenu opens it, and while one of the bar is open
 * the pointer resting on another item of the bar opens that one; in a submenu the pointer resting
 * on an item opens its submenu. The keys move along the bar and up and down the submenus, the
 * arrow pointing away from an item opening its submenu, Escape closing it; Home and End go to the
 * first and the last item, a letter to the next item starting with it. Only one item of the bar
 * takes the focus with Tab. Choosing an item, a click outside or the focus leaving the menu closes
 * the submenus.
 *
 * The submenus pop up below the items of a bar along the top, beside those of a bar down the side
 * and beside the items of a submenu, on the side with room, kept in the window as the page scrolls.
 * A menu down the side may open them in place instead (inline), pushing the items after them down
 * as the side bar of an application does: the arrow keys then go through the items shown, top to
 * bottom, and the arrows across open and close the submenus. The submenus holding the item of the
 * page shown start open then.
 *
 * new Menu(element, options) works on markup the page already has:
 *
 *   <div class="antilia-menu">
 *     <ul class="antilia-menu-list">
 *       <li class="antilia-menu-item">
 *         <a class="antilia-menu-link" href="/home" aria-current="page">
 *           <span class="antilia-menu-icon"><svg>…</svg></span>
 *           <span class="antilia-menu-label">Home</span>
 *         </a>
 *       </li>
 *       <li class="antilia-menu-item">
 *         <button type="button" class="antilia-menu-link"><span class="antilia-menu-label">File</span></button>
 *         <ul class="antilia-menu-submenu">
 *           <li class="antilia-menu-submenu-item">
 *             <button type="button" class="antilia-menu-submenu-link">…</button>
 *           </li>
 *           <li class="antilia-menu-separator"></li>
 *         </ul>
 *       </li>
 *     </ul>
 *   </div>
 *
 * An item is the link or the button among the children of an element of a list, of the class
 * antilia-menu-link on the bar and antilia-menu-submenu-link in a submenu; its submenu, if it has
 * one, the list of the class antilia-menu-submenu next to it. A separator is an element of the
 * class antilia-menu-separator. aria-current="page" marks the item of the page shown, whose
 * submenus' items get the class antilia-menu-current-parent, and aria-disabled="true" an item the
 * user cannot choose. React, Svelte and Wicket render the items, and change them as they please:
 * the menu finds them as they come and go. It sets their roles and states, opens and closes the
 * submenus, which it shows and hides by the attribute hidden, and places those popping up by the
 * custom properties --antilia-menu-x, --antilia-menu-y and --antilia-menu-max-height.
 */

import { onRemoving, type Unsubscribe } from './host.js';
import { LOOK_PARTS, lookOf, partClasses, toggleParts, type Look, type LookPart } from './look.js';

/** horizontal: the bar along the top of the page; vertical: down its side. */
export type MenuOrientation = 'horizontal' | 'vertical';

/**
 * The designs of a menu: a bar of the toolbar's color, its items as rounded pills the current one
 * filled with the theme's primary color, plain items the current one underlined, and the side bar
 * of an application with an accent on the current item. Every design works in both orientations.
 */
export type MenuDesign = 'bar' | 'pills' | 'underline' | 'sidebar';

/** The designs, in the order of MenuDesign. */
export const MENU_DESIGNS: readonly MenuDesign[] = ['bar', 'pills', 'underline', 'sidebar'];

/**
 * How the menu is laid out, its design and how its submenus open.
 */
export interface MenuOptions {
	/** horizontal (the default) for the bar along the top, vertical for down the side. */
	orientation?: MenuOrientation;
	/** The design of the menu, bar when left out; see MENU_DESIGNS. */
	design?: MenuDesign;
	/**
	 * The look of the menu, whose classes its parts get; that of the element it is in when left out
	 * (see lookOf).
	 */
	look?: Look;
	/**
	 * Whether the submenus of a menu down the side open in place, pushing the items after them
	 * down, instead of popping up beside their items; false when left out. A menu along the top
	 * pops them up always.
	 */
	inline?: boolean;
	/**
	 * Whether the submenus of the bar open as the pointer rests on their items, not only on a click;
	 * false when left out. Those of submenus always do.
	 */
	openOnHover?: boolean;
	/** The accessible name of the bar. */
	label?: string;
	/** Called with the element of the item the user chose: a link or a button without a submenu. */
	onSelect?: (item: HTMLElement) => void;
}

/** The classes of the parts of a menu. */
export const MENU_CLASSES = {
	menu: 'antilia-menu',
	/** The list of the items of the bar. */
	list: 'antilia-menu-list',
	/** An item of the bar, the element holding its link and its submenu. */
	item: 'antilia-menu-item',
	/** The link or the button of an item of the bar. */
	link: 'antilia-menu-link',
	icon: 'antilia-menu-icon',
	label: 'antilia-menu-label',
	/** The list of the items of a submenu. */
	submenu: 'antilia-menu-submenu',
	/** An item of a submenu, the element holding its link and its submenu. */
	submenuItem: 'antilia-menu-submenu-item',
	/** The link or the button of an item of a submenu. */
	submenuLink: 'antilia-menu-submenu-link',
	separator: 'antilia-menu-separator',
	/** On the link of the item of the page shown, which aria-current marks. */
	current: 'antilia-menu-current',
	/** On the links of the items whose submenus hold the item of the page shown. */
	currentParent: 'antilia-menu-current-parent'
} as const;

/** The classes of the orientations. */
export const MENU_ORIENTATION_CLASSES: Record<MenuOrientation, string> = {
	horizontal: 'antilia-menu-horizontal',
	vertical: 'antilia-menu-vertical'
};

/** The classes of the designs. */
export const MENU_DESIGN_CLASSES: Record<MenuDesign, string> = Object.fromEntries(MENU_DESIGNS
	.map((design) => [design, 'antilia-menu-' + design])) as Record<MenuDesign, string>;

/** The custom properties placing a submenu that pops up, in pixels from the corner of the window. */
export const MENU_X_PROPERTY = '--antilia-menu-x';
export const MENU_Y_PROPERTY = '--antilia-menu-y';
/** The custom property holding how high a submenu that pops up may be, the height of the window. */
export const MENU_MAX_HEIGHT_PROPERTY = '--antilia-menu-max-height';

/** How long the pointer rests on an item before its submenu opens, in milliseconds. */
const HOVER_DELAY = 150;
/** How long the pointer is away from a menu opening on hover before its submenus close. */
const LEAVE_DELAY = 400;
/**
 * How long after a submenu of the bar opened as the pointer came over its item a click on the item
 * leaves it open, rather than closing it, in milliseconds: the click of a user who did not wait.
 */
const HOVER_CLICK_GRACE = 600;
/** How far a submenu stays from the edges of the window, in pixels. */
const MARGIN = 8;

/** The menus by their element. */
const instances = new WeakMap<Element, Menu>();

let counter = 0;

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

/**
 * @returns the link or the button of an item, among the children of its element
 */
function linkOf(item: Element | null): HTMLElement | null {
	return item ? (Array.from(item.children) as HTMLElement[]).find((child) =>
		child.classList.contains(MENU_CLASSES.link) ||
		child.classList.contains(MENU_CLASSES.submenuLink)) ?? null : null;
}

/**
 * @returns the submenu of an item, among the children of its element
 */
function submenuOf(item: Element | null): HTMLElement | null {
	return childrenOf(item, MENU_CLASSES.submenu)[0] ?? null;
}

/**
 * @returns the text of an item, as its label or its link says
 */
function labelOf(link: HTMLElement): string {
	return ((link.querySelector('.' + MENU_CLASSES.label) ?? link).textContent ?? '').trim();
}

function isExpanded(link: HTMLElement): boolean {
	return link.getAttribute('aria-expanded') === 'true';
}

function isDisabled(link: HTMLElement): boolean {
	return link.getAttribute('aria-disabled') === 'true' ||
		(link instanceof HTMLButtonElement && link.disabled);
}

function isCurrent(link: HTMLElement): boolean {
	const current = link.getAttribute('aria-current');
	return current !== null && current !== 'false';
}

export class Menu {
	readonly element: HTMLElement;

	options: MenuOptions;

	private listeners: Array<[EventTarget, string, EventListener, boolean]> = [];

	/** The listeners following the window as it scrolls and resizes, while a submenu pops up. */
	private followers: Array<[EventTarget, string, EventListener, boolean]> = [];

	private observer: MutationObserver | null = null;

	private stopWatchingRemoval: Unsubscribe | null = null;

	/** The look whose classes the menu gave its element and its bar, when it rendered last. */
	private applied: Look | null = null;

	private hoverTimer: ReturnType<typeof setTimeout> | undefined = undefined;

	private leaveTimer: ReturnType<typeof setTimeout> | undefined = undefined;

	private frame = 0;

	/** When the submenus of the bar opened as the pointer came over their items. */
	private hovered = new WeakMap<HTMLElement, number>();

	/**
	 * @param element the element of the menu: a child of the class antilia-menu-list is the bar
	 * @param options the orientation, the design, how the submenus open and the accessible name
	 */
	constructor(element: HTMLElement, options: MenuOptions = {}) {
		this.element = element;
		this.options = { ...options };
		instances.get(element)?.destroy();
		instances.set(element, this);
		if (!element.id) {
			element.id = 'antilia-menu-' + (++counter);
		}
		this.render();
		if (this.inline()) {
			this.expandCurrent();
		}
		this.bind();
		this.stopWatchingRemoval = onRemoving(element, () => this.detach());
	}

	/**
	 * Makes a menu of the element, as Wicket does.
	 *
	 * @param element the element or its id
	 * @param options the orientation, the design, how the submenus open and the accessible name
	 * @returns the menu, null when there is no such element
	 */
	static attach(element: HTMLElement | string, options?: MenuOptions): Menu | null {
		const resolved = resolve(element);
		return resolved ? new Menu(resolved, options) : null;
	}

	/**
	 * @param target the element of the menu or its id
	 * @returns the menu, if there is one
	 */
	static of(target: HTMLElement | string | null | undefined): Menu | null {
		const element = typeof target === 'string' ? document.getElementById(target) : target;
		return (element && instances.get(element)) || null;
	}

	orientation(): MenuOrientation {
		return this.options.orientation === 'vertical' ? 'vertical' : 'horizontal';
	}

	design(): MenuDesign {
		const design = this.options.design;
		return design && MENU_DESIGNS.includes(design) ? design : 'bar';
	}

	/**
	 * @returns whether the submenus open in place: those of a menu down the side asked to
	 */
	inline(): boolean {
		return !!this.options.inline && this.orientation() === 'vertical';
	}

	/**
	 * @returns the look of the menu: that of the options, else that of the element it is in
	 */
	look(): Look {
		return this.options.look ?? lookOf(this.element);
	}

	/**
	 * @returns the list of the items of the bar
	 */
	private bar(): HTMLElement | null {
		return childrenOf(this.element, MENU_CLASSES.list)[0] ?? null;
	}

	private isBar(list: Element | null | undefined): boolean {
		return !!list && list === this.bar();
	}

	/**
	 * @returns the list an item is in
	 */
	private listOf(link: HTMLElement): HTMLElement | null {
		return link.parentElement?.parentElement ?? null;
	}

	private submenuOfLink(link: HTMLElement): HTMLElement | null {
		return submenuOf(link.parentElement);
	}

	/**
	 * @returns the item a submenu belongs to
	 */
	private triggerOf(submenu: HTMLElement | null): HTMLElement | null {
		return linkOf(submenu?.parentElement ?? null);
	}

	/**
	 * @returns the items of a list the user sees: neither hidden nor separators
	 */
	private itemsOf(list: HTMLElement | null): HTMLElement[] {
		return list ? (Array.from(list.children) as HTMLElement[])
			.filter((item) => !item.hidden && !item.classList.contains(MENU_CLASSES.separator))
			.map((item) => linkOf(item))
			.filter((link): link is HTMLElement => !!link && !link.hidden) : [];
	}

	/**
	 * @returns the items of the menu the user sees, top to bottom: those of the bar and of the
	 *          submenus open
	 */
	private shownItems(): HTMLElement[] {
		const shown: HTMLElement[] = [];
		const walk = (list: HTMLElement | null) => this.itemsOf(list).forEach((link) => {
			shown.push(link);
			if (isExpanded(link)) {
				walk(this.submenuOfLink(link));
			}
		});
		walk(this.bar());
		return shown;
	}

	/**
	 * @returns the item of the bar an item is, or is in the submenus of
	 */
	private barItemOf(link: HTMLElement): HTMLElement {
		let current = link;
		for (let list = this.listOf(current); list && !this.isBar(list); list = this.listOf(current)) {
			const trigger = this.triggerOf(list);
			if (!trigger) {
				break;
			}
			current = trigger;
		}
		return current;
	}

	/**
	 * @returns the item of this menu an event happened on
	 */
	private itemOf(event: Event): HTMLElement | null {
		const target = event.target as Element | null;
		const link = target?.closest?.(`.${MENU_CLASSES.link}, .${MENU_CLASSES.submenuLink}`) as
			HTMLElement | null;
		if (!link || !this.element.contains(link)) {
			return null;
		}
		// not one of a menu inside this one
		const menu = link.closest('.' + MENU_CLASSES.menu);
		return menu === this.element || menu === null ? link : null;
	}

	/**
	 * Gives the element of the menu and its bar the classes of the orientation and the design, in a
	 * look, taking away those of the others; none of them when off.
	 *
	 * @param bar the bar, found before: off takes its class away too
	 */
	private renderParts(look: Look, on: boolean, bar: HTMLElement | null): void {
		const element = this.element;
		const orientation = this.orientation();
		const design = this.design();
		toggleParts(element, look, on, 'menu');
		(Object.keys(MENU_ORIENTATION_CLASSES) as MenuOrientation[]).forEach((each) =>
			toggleParts(element, look, on && each === orientation, `menu-${each}`));
		MENU_DESIGNS.forEach((each) => toggleParts(element, look, on && each === design,
			`menu-${each}`));
		toggleParts(element, look, on && this.inline(), 'menu-inline');
		if (bar) {
			const parts = MENU_DESIGNS.map((each) => `menu-list-${each}`)
				.filter((part): part is LookPart => part in LOOK_PARTS);
			const current = parts.find((part) => on && part === `menu-list-${design}`);
			// the parts of the designs share classes, Bootstrap's nav: those of the others go
			// unless the bar keeps them
			const kept = new Set(partClasses(look, 'menu-list', current).split(' '));
			const gone = partClasses(look, ...parts.filter((part) => part !== current)).split(' ')
				.filter((css) => css && !kept.has(css));
			bar.classList.remove(...gone);
			toggleParts(bar, look, on, 'menu-list', current);
		}
	}

	private render(): void {
		const look = this.look();
		const bar = this.bar();
		// a look other than the one before: its classes go
		if (this.applied && this.applied !== look) {
			this.renderParts(this.applied, false, bar);
		}
		this.renderParts(look, true, bar);
		this.applied = look;
		if (!bar) {
			return;
		}
		const roving = this.itemsOf(bar).find((link) => link.tabIndex === 0 &&
			link.getAttribute('tabindex') === '0');
		bar.setAttribute('role', 'menubar');
		bar.setAttribute('aria-orientation', this.orientation());
		if (this.options.label) {
			bar.setAttribute('aria-label', this.options.label);
		} else {
			bar.removeAttribute('aria-label');
		}
		this.renderList(bar, look);
		this.renderFocus(roving);
		if (!this.inline()) {
			this.placeAll();
		}
	}

	/**
	 * Gives the items of a list, and of the submenus in it, their roles and states.
	 *
	 * @returns whether the list holds the item of the page shown
	 */
	private renderList(list: HTMLElement, look: Look): boolean {
		let current = false;
		const vertical = !this.isBar(list) || this.orientation() === 'vertical';
		for (const child of Array.from(list.children) as HTMLElement[]) {
			if (child.classList.contains(MENU_CLASSES.separator)) {
				child.setAttribute('role', 'separator');
				child.setAttribute('aria-orientation', vertical ? 'horizontal' : 'vertical');
				continue;
			}
			const link = linkOf(child);
			if (!link) {
				continue;
			}
			child.setAttribute('role', 'none');
			link.setAttribute('role', 'menuitem');
			link.tabIndex = -1;
			if (link instanceof HTMLButtonElement && !link.hasAttribute('type')) {
				link.type = 'button';
			}
			let here = isCurrent(link);
			toggleParts(link, look, here, 'menu-current');
			const submenu = submenuOf(child);
			if (submenu) {
				if (!submenu.id) {
					submenu.id = this.element.id + '-submenu-' + (++counter);
				}
				if (!link.id) {
					link.id = submenu.id + '-item';
				}
				const open = isExpanded(link) && !isDisabled(link);
				link.setAttribute('aria-haspopup', 'menu');
				link.setAttribute('aria-controls', submenu.id);
				link.setAttribute('aria-expanded', String(open));
				submenu.setAttribute('role', 'menu');
				submenu.setAttribute('aria-labelledby', link.id);
				submenu.hidden = !open;
				const inside = this.renderList(submenu, look);
				toggleParts(link, look, inside, 'menu-current-parent');
				here = here || inside;
			} else {
				link.removeAttribute('aria-haspopup');
				link.removeAttribute('aria-controls');
				link.removeAttribute('aria-expanded');
			}
			current = current || here;
		}
		return current;
	}

	/**
	 * Lets one item of the bar take the focus with Tab: the one that had it, else the item of the
	 * page shown or the one holding it, else the first.
	 */
	private renderFocus(roving: HTMLElement | undefined): void {
		const items = this.itemsOf(this.bar());
		const current = (link: HTMLElement) => isCurrent(link) ||
			link.classList.contains(MENU_CLASSES.currentParent);
		const kept = roving && items.includes(roving) ? roving : items.find(current) ?? items[0];
		if (kept) {
			kept.tabIndex = 0;
		}
	}

	/**
	 * Opens the submenus holding the item of the page shown, which a menu opening them in place
	 * starts with.
	 */
	private expandCurrent(): void {
		this.element.querySelectorAll<HTMLElement>('.' + MENU_CLASSES.submenu).forEach((submenu) => {
			const trigger = this.triggerOf(submenu);
			const holds = Array.from(submenu.querySelectorAll<HTMLElement>('[aria-current]'))
				.some((link) => isCurrent(link));
			if (trigger && holds && !isDisabled(trigger)) {
				trigger.setAttribute('aria-expanded', 'true');
				submenu.hidden = false;
			}
		});
		this.observer?.takeRecords();
	}

	private listen<E extends Event>(target: EventTarget, type: string,
		listener: (event: E) => void, capture = false): void {
		target.addEventListener(type, listener as EventListener, capture);
		this.listeners.push([target, type, listener as EventListener, capture]);
	}

	private bind(): void {
		this.listen<MouseEvent>(this.element, 'click', (event) => this.onClick(event));
		this.listen<KeyboardEvent>(this.element, 'keydown', (event) => this.onKeyDown(event));
		this.listen<PointerEvent>(this.element, 'pointerover', (event) => this.onPointerOver(event));
		this.listen<PointerEvent>(this.element, 'pointerout', (event) => this.onPointerOut(event));
		this.listen<FocusEvent>(this.element, 'focusin', (event) => this.onFocusIn(event));
		this.listen<FocusEvent>(this.element, 'focusout', (event) => {
			const next = event.relatedTarget as Node | null;
			if (next && !this.element.contains(next)) {
				this.closeAll();
			}
		});
		this.listen<PointerEvent>(document, 'pointerdown', (event) => {
			if (!this.element.contains(event.target as Node)) {
				this.closeAll();
			}
		}, true);
		this.observer = new MutationObserver(() => this.refresh());
		this.observer.observe(this.element, { childList: true, subtree: true, attributes: true,
			attributeFilter: ['aria-current', 'aria-disabled', 'disabled'] });
	}

	private onClick(event: MouseEvent): void {
		const link = this.itemOf(event);
		if (!link) {
			return;
		}
		if (isDisabled(link)) {
			event.preventDefault();
			event.stopPropagation();
			return;
		}
		const submenu = this.submenuOfLink(link);
		if (!submenu) {
			// an item chosen: the submenus popping up close
			this.closeAll();
			this.options.onSelect?.(link);
			return;
		}
		event.preventDefault();
		if (!isExpanded(link)) {
			// the keys clicked it: into the submenu, as its first item
			this.open(link, event.detail === 0 && !this.inline() ? 'first' : null);
			return;
		}
		const hovered = this.hovered.get(submenu);
		this.hovered.delete(submenu);
		// the click of a user who did not wait for the submenu to open as the pointer came
		if (hovered === undefined || Date.now() - hovered > HOVER_CLICK_GRACE) {
			this.closeSubmenu(link);
		}
	}

	private onFocusIn(event: FocusEvent): void {
		const link = this.itemOf(event);
		if (!link) {
			return;
		}
		// the item of the bar the user is at takes the focus with Tab
		const roving = this.barItemOf(link);
		this.itemsOf(this.bar()).forEach((each) => {
			each.tabIndex = each === roving ? 0 : -1;
		});
	}

	private onKeyDown(event: KeyboardEvent): void {
		const link = this.itemOf(event);
		if (!link || event.target !== link || event.altKey || event.ctrlKey || event.metaKey) {
			return;
		}
		if (event.key === 'Tab') {
			// the focus goes on from the bar, the submenus close
			this.closeAll();
			return;
		}
		if (event.key === 'Enter' || event.key === ' ') {
			// a button or a link clicks itself, other elements do not
			const native = link instanceof HTMLButtonElement ||
				(event.key === 'Enter' && link instanceof HTMLAnchorElement && link.hasAttribute('href'));
			if (!native) {
				event.preventDefault();
				link.click();
			}
			return;
		}
		const handled = this.inline() ? this.onInlineKey(event.key, link) : this.onKey(event.key, link);
		if (handled) {
			event.preventDefault();
			event.stopPropagation();
		}
	}

	/**
	 * Takes a key pressed on an item of a menu whose submenus pop up.
	 *
	 * @returns whether the menu took it
	 */
	private onKey(key: string, link: HTMLElement): boolean {
		const list = this.listOf(link);
		const items = this.itemsOf(list);
		const index = items.indexOf(link);
		const onBar = this.isBar(list);
		const along = this.orientation() === 'horizontal';
		const across = onBar && along;
		const submenu = this.submenuOfLink(link);
		switch (key) {
			case across ? 'ArrowRight' : 'ArrowDown':
				this.moveTo(items[(index + 1) % items.length], onBar);
				return true;
			case across ? 'ArrowLeft' : 'ArrowUp':
				this.moveTo(items[(index - 1 + items.length) % items.length], onBar);
				return true;
			case 'Home':
				this.moveTo(items[0], onBar);
				return true;
			case 'End':
				this.moveTo(items[items.length - 1], onBar);
				return true;
			case across ? 'ArrowDown' : 'ArrowRight':
				if (submenu) {
					this.open(link, 'first');
				} else if (!onBar && along) {
					// past the end of a submenu: the next item of the bar, and its submenu
					this.moveAlongBar(link, 1);
				}
				return true;
			case across ? 'ArrowUp' : 'ArrowLeft':
				if (across) {
					if (submenu) {
						this.open(link, 'last');
					}
				} else if (!onBar) {
					const parent = this.triggerOf(list);
					if (parent && this.isBar(this.listOf(parent)) && along) {
						this.moveAlongBar(link, -1);
					} else if (parent) {
						this.closeSubmenu(parent);
						parent.focus();
					}
				}
				return true;
			case 'Escape': {
				const parent = onBar ? (submenu && isExpanded(link) ? link : null) : this.triggerOf(list);
				if (!parent) {
					return false;
				}
				this.closeSubmenu(parent);
				parent.focus();
				return true;
			}
			default:
				return this.typeAhead(key, items, index, onBar);
		}
	}

	/**
	 * Takes a key pressed on an item of a menu opening its submenus in place, as a tree.
	 *
	 * @returns whether the menu took it
	 */
	private onInlineKey(key: string, link: HTMLElement): boolean {
		const shown = this.shownItems();
		const index = shown.indexOf(link);
		const submenu = this.submenuOfLink(link);
		switch (key) {
			case 'ArrowDown':
				shown[Math.min(index + 1, shown.length - 1)]?.focus();
				return true;
			case 'ArrowUp':
				shown[Math.max(index - 1, 0)]?.focus();
				return true;
			case 'Home':
				shown[0]?.focus();
				return true;
			case 'End':
				shown[shown.length - 1]?.focus();
				return true;
			case 'ArrowRight':
				if (submenu && !isExpanded(link)) {
					this.open(link);
				} else if (submenu) {
					this.itemsOf(submenu)[0]?.focus();
				}
				return true;
			case 'ArrowLeft': {
				if (submenu && isExpanded(link)) {
					this.closeSubmenu(link);
				} else {
					const list = this.listOf(link);
					if (!this.isBar(list)) {
						this.triggerOf(list)?.focus();
					}
				}
				return true;
			}
			default:
				return this.typeAhead(key, shown, index, false);
		}
	}

	/**
	 * Moves to the next item whose label starts with a letter typed.
	 *
	 * @returns whether the key was a letter
	 */
	private typeAhead(key: string, items: HTMLElement[], index: number, onBar: boolean): boolean {
		if (key.length !== 1 || key === ' ') {
			return false;
		}
		const letter = key.toLocaleLowerCase();
		for (let step = 1; step <= items.length; step++) {
			const item = items[(index + step) % items.length];
			if (labelOf(item).toLocaleLowerCase().startsWith(letter)) {
				this.moveTo(item, onBar);
				break;
			}
		}
		return true;
	}

	/**
	 * Gives an item the focus; on the bar, while a submenu of it is open, the item's opens instead.
	 */
	private moveTo(link: HTMLElement | undefined, onBar: boolean): void {
		if (!link) {
			return;
		}
		if (onBar && this.anyOpen()) {
			if (this.submenuOfLink(link)) {
				this.open(link);
			} else {
				this.closeAll();
			}
		}
		link.focus();
	}

	/**
	 * Goes from an item of a submenu to the next or the previous item of a bar along the top,
	 * opening its submenu.
	 */
	private moveAlongBar(link: HTMLElement, step: 1 | -1): void {
		const items = this.itemsOf(this.bar());
		const index = items.indexOf(this.barItemOf(link));
		const next = items[(index + step + items.length) % items.length];
		if (!next) {
			return;
		}
		this.closeAll();
		next.focus();
		if (this.submenuOfLink(next)) {
			this.open(next, 'first');
		}
	}

	private onPointerOver(event: PointerEvent): void {
		if (event.pointerType !== 'mouse') {
			return;
		}
		clearTimeout(this.leaveTimer);
		const link = this.itemOf(event);
		if (!link || this.inline()) {
			return;
		}
		const onBar = this.isBar(this.listOf(link));
		if (onBar) {
			const armed = this.anyOpen();
			if (armed || this.options.openOnHover) {
				this.wait(() => this.hoverTo(link, true), armed ? 0 : HOVER_DELAY);
			} else {
				clearTimeout(this.hoverTimer);
			}
		} else {
			this.wait(() => this.hoverTo(link, false), HOVER_DELAY);
		}
	}

	private onPointerOut(event: PointerEvent): void {
		const next = event.relatedTarget as Node | null;
		if (event.pointerType !== 'mouse' || (next && this.element.contains(next))) {
			return;
		}
		// the pointer left the menu
		clearTimeout(this.hoverTimer);
		if (this.options.openOnHover && !this.inline()) {
			clearTimeout(this.leaveTimer);
			this.leaveTimer = setTimeout(() => this.closeAll(), LEAVE_DELAY);
		}
	}

	private wait(action: () => void, delay: number): void {
		clearTimeout(this.hoverTimer);
		this.hoverTimer = setTimeout(action, delay);
	}

	/**
	 * Follows the pointer resting on an item: its submenu opens, those of the others close.
	 */
	private hoverTo(link: HTMLElement, onBar: boolean): void {
		if (!link.isConnected) {
			return;
		}
		const submenu = this.submenuOfLink(link);
		// the focus follows the pointer when it is in the menu already, as after a click on it, so
		// the keys go on from where the pointer is; not when it is elsewhere in the page
		if (!onBar && this.element.contains(document.activeElement)) {
			link.focus({ preventScroll: true });
		}
		if (submenu && !isDisabled(link)) {
			if (!isExpanded(link)) {
				this.open(link);
				this.hovered.set(submenu, Date.now());
			}
		} else if (onBar) {
			this.closeAll();
		} else {
			this.closeSiblings(link);
		}
	}

	/**
	 * @returns whether a submenu of the bar pops up
	 */
	private anyOpen(): boolean {
		return !this.inline() && this.itemsOf(this.bar()).some((link) => isExpanded(link));
	}

	/**
	 * Opens the submenu of an item, closing those of the other items of its list.
	 *
	 * @param link the item
	 * @param focus the item of the submenu to take the focus, none when left out
	 */
	open(link: HTMLElement, focus: 'first' | 'last' | null = null): void {
		const submenu = this.submenuOfLink(link);
		if (!submenu || isDisabled(link)) {
			return;
		}
		this.closeSiblings(link);
		if (!isExpanded(link)) {
			link.setAttribute('aria-expanded', 'true');
			submenu.hidden = false;
			this.hovered.delete(submenu);
			if (!this.inline()) {
				this.place(submenu);
				this.follow(true);
			}
			this.observer?.takeRecords();
		}
		const items = this.itemsOf(submenu);
		const target = focus === 'first' ? items[0] : focus === 'last' ? items[items.length - 1] :
			undefined;
		target?.focus();
	}

	/**
	 * Closes the submenus of the other items of the list of an item.
	 */
	private closeSiblings(link: HTMLElement): void {
		if (this.inline()) {
			return;
		}
		this.itemsOf(this.listOf(link)).filter((each) => each !== link)
			.forEach((each) => this.closeSubmenu(each));
	}

	/**
	 * Closes the submenu of an item, and those open in it.
	 *
	 * @param link the item
	 */
	closeSubmenu(link: HTMLElement): void {
		const submenu = this.submenuOfLink(link);
		if (!submenu || !isExpanded(link)) {
			return;
		}
		this.itemsOf(submenu).forEach((each) => this.closeSubmenu(each));
		link.setAttribute('aria-expanded', 'false');
		submenu.hidden = true;
		if (!this.anyOpen()) {
			this.follow(false);
		}
		this.observer?.takeRecords();
	}

	/**
	 * Closes the submenus popping up; those open in place stay. The focus in one of them goes back
	 * to its item of the bar.
	 */
	closeAll(): void {
		clearTimeout(this.hoverTimer);
		clearTimeout(this.leaveTimer);
		if (this.inline()) {
			return;
		}
		const active = document.activeElement as HTMLElement | null;
		const items = this.itemsOf(this.bar());
		const open = items.find((link) => isExpanded(link));
		if (!open) {
			return;
		}
		const inside = !!active && !!this.submenuOfLink(open)?.contains(active);
		items.forEach((link) => this.closeSubmenu(link));
		if (inside) {
			open.focus({ preventScroll: true });
		}
	}

	/**
	 * @returns whether the submenu of an item is open
	 */
	isOpen(link: HTMLElement): boolean {
		return isExpanded(link);
	}

	/**
	 * Places a submenu popping up: below an item of a bar along the top, else beside its item, on
	 * the side with room, in the window.
	 */
	private place(submenu: HTMLElement): void {
		const link = this.triggerOf(submenu);
		if (!link) {
			return;
		}
		const below = this.isBar(this.listOf(link)) && this.orientation() === 'horizontal';
		const anchor = link.getBoundingClientRect();
		const viewport = { width: document.documentElement.clientWidth || window.innerWidth,
			height: window.innerHeight };
		const style = submenu.style;
		style.setProperty(MENU_MAX_HEIGHT_PROPERTY, Math.max(0, viewport.height - 2 * MARGIN) + 'px');
		style.setProperty(MENU_X_PROPERTY, '0px');
		style.setProperty(MENU_Y_PROPERTY, '0px');
		const width = submenu.offsetWidth;
		const height = submenu.offsetHeight;
		const clamp = (value: number, min: number, max: number) =>
			Math.min(Math.max(value, min), Math.max(min, max));
		let x: number;
		let y: number;
		if (below) {
			// below the item, its left edges with the item's, else its right edges
			x = anchor.left + width > viewport.width - MARGIN ? anchor.right - width : anchor.left;
			y = anchor.bottom;
			if (y + height > viewport.height - MARGIN && anchor.top - height >= MARGIN) {
				y = anchor.top - height;
			}
		} else {
			// beside the item, its first item level with it: on the right, else on the left
			const padding = parseFloat(getComputedStyle(submenu).paddingTop) || 0;
			x = anchor.right;
			if (x + width > viewport.width - MARGIN && anchor.left - width >= MARGIN) {
				x = anchor.left - width;
			}
			y = anchor.top - padding;
		}
		x = clamp(x, MARGIN, viewport.width - MARGIN - width);
		y = clamp(y, MARGIN, viewport.height - MARGIN - height);
		style.setProperty(MENU_X_PROPERTY, Math.round(x) + 'px');
		style.setProperty(MENU_Y_PROPERTY, Math.round(y) + 'px');
		submenu.setAttribute('data-placement', below ? 'below' : 'beside');
		// in an element placing it other than the window, a transformed one: where it is says by
		// how much it is off
		const placed = submenu.getBoundingClientRect();
		const dx = placed.left - Math.round(x);
		const dy = placed.top - Math.round(y);
		if (width && (Math.abs(dx) >= 1 || Math.abs(dy) >= 1)) {
			style.setProperty(MENU_X_PROPERTY, Math.round(x - dx) + 'px');
			style.setProperty(MENU_Y_PROPERTY, Math.round(y - dy) + 'px');
		}
	}

	/**
	 * Places the submenus popping up again, those of the bar first.
	 */
	private placeAll(): void {
		this.element.querySelectorAll<HTMLElement>('.' + MENU_CLASSES.submenu).forEach((submenu) => {
			if (!submenu.hidden) {
				this.place(submenu);
			}
		});
	}

	/**
	 * Follows the window scrolling and resizing while a submenu pops up, or stops.
	 */
	private follow(on: boolean): void {
		if (on === this.followers.length > 0) {
			return;
		}
		if (!on) {
			this.followers.forEach(([target, type, listener, capture]) =>
				target.removeEventListener(type, listener, capture));
			this.followers = [];
			cancelAnimationFrame(this.frame);
			return;
		}
		const again = () => {
			cancelAnimationFrame(this.frame);
			this.frame = requestAnimationFrame(() => this.placeAll());
		};
		this.followers = [[window, 'scroll', again, true], [window, 'resize', again, false]];
		this.followers.forEach(([target, type, listener, capture]) =>
			target.addEventListener(type, listener, capture));
	}

	/**
	 * Finds the items the page added, took away or rendered again, and renders them.
	 */
	refresh(): void {
		this.render();
		// what the menu changed is its own, not the page's
		this.observer?.takeRecords();
	}

	/**
	 * Takes other options; those left out stay as they were.
	 */
	configure(options: MenuOptions = {}): void {
		const inline = this.inline();
		const defined = Object.fromEntries(Object.entries(options)
			.filter(([, value]) => value !== undefined)) as MenuOptions;
		this.options = { ...this.options, ...defined };
		if (inline !== this.inline()) {
			// the submenus open in place close as they start popping up, and the other way round
			this.itemsOf(this.bar()).forEach((link) => this.closeSubmenu(link));
			this.follow(false);
			if (this.inline()) {
				this.expandCurrent();
			}
		}
		this.render();
		this.observer?.takeRecords();
	}

	/**
	 * Takes the menu away, leaving the items of the page with the submenus closed.
	 */
	destroy(): void {
		this.itemsOf(this.bar()).forEach((link) => this.closeSubmenu(link));
		this.detach();
		const element = this.element;
		const bar = this.bar();
		this.renderParts(this.applied ?? this.look(), false, bar);
		// the classes of the menu and its bar stay, as the page has them
		element.classList.add(MENU_CLASSES.menu);
		bar?.classList.add(MENU_CLASSES.list);
		element.querySelectorAll<HTMLElement>('.' + MENU_CLASSES.submenu).forEach((submenu) => {
			[MENU_X_PROPERTY, MENU_Y_PROPERTY, MENU_MAX_HEIGHT_PROPERTY]
				.forEach((property) => submenu.style.removeProperty(property));
		});
		if (instances.get(element) === this) {
			instances.delete(element);
		}
	}

	/**
	 * Stops listening, leaving the elements in place.
	 */
	private detach(): void {
		clearTimeout(this.hoverTimer);
		clearTimeout(this.leaveTimer);
		this.follow(false);
		this.listeners.forEach(([target, type, listener, capture]) =>
			target.removeEventListener(type, listener, capture));
		this.listeners = [];
		this.observer?.disconnect();
		this.observer = null;
		this.stopWatchingRemoval?.();
		this.stopWatchingRemoval = null;
	}
}

/**
 * Makes a menu of an element, in the shape of a Svelte action: use:menu={options}.
 *
 * @param element the element of the menu
 * @param options the orientation, the design, how the submenus open and the accessible name
 * @returns update() taking other options, destroy() taking the menu away
 */
export function menu(element: HTMLElement, options?: MenuOptions): {
	update(options?: MenuOptions): void;
	destroy(): void;
} {
	const instance = new Menu(element, options);
	return {
		update: (next) => instance.configure(next),
		destroy: () => instance.destroy()
	};
}
