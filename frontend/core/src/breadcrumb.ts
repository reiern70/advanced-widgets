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
 * Breadcrumbs: where the page is in the hierarchy of the application, a row of links from the home
 * page down to the page shown, the last crumb the page itself. The WAI-ARIA breadcrumb pattern: a
 * nav, named, around an ordered list; the style sheet draws the separators between the crumbs,
 * which screen readers do not read.
 *
 * A long trail collapses: with maxItems, a trail of more crumbs shows its first crumb and its last
 * maxItems - 1, and those between them hide behind a button, "…", which shows them in place and
 * gives the focus to the first of them. The trail collapses again when its crumbs change.
 *
 * new Breadcrumb(element, options) works on markup the page already has:
 *
 *   <nav class="antilia-breadcrumb antilia-breadcrumb-chevron" aria-label="Breadcrumb">
 *     <ol class="antilia-breadcrumb-list">
 *       <li class="antilia-breadcrumb-item">
 *         <a class="antilia-breadcrumb-link" href="/">
 *           <span class="antilia-breadcrumb-icon"><svg>…</svg></span>
 *           <span class="antilia-breadcrumb-label">Home</span>
 *         </a>
 *       </li>
 *       <li class="antilia-breadcrumb-item antilia-breadcrumb-current">
 *         <span class="antilia-breadcrumb-page" aria-current="page">
 *           <span class="antilia-breadcrumb-label">Laptops</span>
 *         </span>
 *       </li>
 *     </ol>
 *   </nav>
 *
 * A crumb is an element of the list of the class antilia-breadcrumb-item. The item of the button
 * showing the crumbs hidden, of the classes antilia-breadcrumb-item and antilia-breadcrumb-more,
 * may be in the markup already, as Wicket renders it, the crumbs it hides marked hidden and
 * data-antilia-collapsed, so that nothing moves while the page loads; the breadcrumb makes one when
 * there is none. React, Svelte and Wicket render the crumbs, and change them as they please.
 */

import { onRemoving, type Unsubscribe } from './host.js';
import { lookOf, toggleParts, type Look } from './look.js';

/** The separators drawn between the crumbs: ›, / and →. */
export type BreadcrumbSeparator = 'chevron' | 'slash' | 'arrow';

/** The separators, in the order of BreadcrumbSeparator. */
export const BREADCRUMB_SEPARATORS: readonly BreadcrumbSeparator[] = ['chevron', 'slash', 'arrow'];

/** The texts of a breadcrumb. */
export interface BreadcrumbTexts {
	/** The accessible name of the breadcrumb, that of its nav. */
	label: string;
	/** The label of the button showing the crumbs hidden, {0} how many there are. */
	more: string;
}

/** The texts in English. */
export const DEFAULT_BREADCRUMB_TEXTS: BreadcrumbTexts = {
	label: 'Breadcrumb',
	more: 'Show the whole path ({0} more)'
};

/**
 * The separator, how many crumbs show and the texts.
 */
export interface BreadcrumbOptions {
	/** The separator drawn between the crumbs, chevron when left out. */
	separator?: BreadcrumbSeparator;
	/**
	 * How many crumbs show at most: a longer trail shows its first crumb and its last
	 * maxItems - 1, the others behind a button that shows them. Every crumb shows when left out or
	 * 0; 2 at least otherwise.
	 */
	maxItems?: number;
	/**
	 * The look of the breadcrumb, whose classes its parts get; that of the element it is in when
	 * left out (see lookOf).
	 */
	look?: Look;
	/**
	 * The texts; those left out are in English. The accessible name the element has stays when
	 * label is left out.
	 */
	texts?: Partial<BreadcrumbTexts>;
	/** Called when the user showed the crumbs hidden. */
	onExpand?: () => void;
}

/** The classes of the parts of a breadcrumb. */
export const BREADCRUMB_CLASSES = {
	breadcrumb: 'antilia-breadcrumb',
	/** The ordered list of the crumbs. */
	list: 'antilia-breadcrumb-list',
	/** A crumb, an element of the list; the item of the button "…" too. */
	item: 'antilia-breadcrumb-item',
	/** The link of a crumb leading somewhere, or its button doing something. */
	link: 'antilia-breadcrumb-link',
	/** What the last crumb, the page shown, has instead of a link. */
	page: 'antilia-breadcrumb-page',
	/** What a crumb leading nowhere has instead of a link: its label alone. */
	text: 'antilia-breadcrumb-text',
	icon: 'antilia-breadcrumb-icon',
	label: 'antilia-breadcrumb-label',
	/** On the last crumb, the page shown. */
	current: 'antilia-breadcrumb-current',
	/** The item of the button showing the crumbs hidden. */
	more: 'antilia-breadcrumb-more',
	/** The button showing the crumbs hidden. */
	moreButton: 'antilia-breadcrumb-more-button'
} as const;

/** The classes of the separators. */
export const BREADCRUMB_SEPARATOR_CLASSES: Record<BreadcrumbSeparator, string> = Object.fromEntries(
	BREADCRUMB_SEPARATORS.map((separator) => [separator, 'antilia-breadcrumb-' + separator])) as
	Record<BreadcrumbSeparator, string>;

/** The attribute marking the crumbs a collapsed trail hides. */
export const BREADCRUMB_COLLAPSED_ATTRIBUTE = 'data-antilia-collapsed';

/** What the button showing the crumbs hidden says. */
const ELLIPSIS = '…';

/** The breadcrumbs by their element. */
const instances = new WeakMap<Element, Breadcrumb>();

let counter = 0;

function resolve(target: HTMLElement | string): HTMLElement | null {
	return typeof target === 'string' ? document.getElementById(target) : target;
}

function format(text: string, value: number): string {
	return text.split('{0}').join(String(value));
}

/**
 * The crumbs a trail hides: those after the first, up to the last maxItems - 1.
 *
 * @param count how many crumbs the trail has
 * @param maxItems how many show at most, 2 at least; every crumb shows for 0 or none
 * @returns the place of the first crumb hidden and that of the crumb after the last one, null when
 *          every crumb shows
 */
export function collapsedCrumbs(count: number, maxItems?: number): { from: number; to: number } |
	null {
	if (!maxItems || maxItems <= 0) {
		return null;
	}
	const shown = Math.max(2, Math.floor(maxItems));
	return count > shown ? { from: 1, to: count - (shown - 1) } : null;
}

export class Breadcrumb {
	readonly element: HTMLElement;

	options: BreadcrumbOptions;

	/** Whether the user showed the crumbs hidden. */
	private expanded = false;

	/** The crumbs when the breadcrumb rendered last: other ones collapse the trail again. */
	private crumbs: HTMLElement[] = [];

	/** The item of the button "…" the breadcrumb made, which it takes away when destroyed. */
	private made: HTMLElement | null = null;

	/** The look whose classes the breadcrumb gave its element, when it rendered last. */
	private applied: Look | null = null;

	private observer: MutationObserver | null = null;

	private stopWatchingRemoval: Unsubscribe | null = null;

	private readonly onClick = (event: MouseEvent) => {
		const button = (event.target as Element | null)?.closest?.('.' + BREADCRUMB_CLASSES.moreButton);
		if (button && this.element.contains(button)) {
			event.preventDefault();
			this.expand();
		}
	};

	/**
	 * @param element the element of the breadcrumb, a nav: a child of the class
	 *        antilia-breadcrumb-list holds the crumbs
	 * @param options the separator, how many crumbs show and the texts
	 */
	constructor(element: HTMLElement, options: BreadcrumbOptions = {}) {
		this.element = element;
		this.options = { ...options };
		instances.get(element)?.destroy();
		instances.set(element, this);
		if (!element.id) {
			element.id = 'antilia-breadcrumb-' + (++counter);
		}
		this.render();
		element.addEventListener('click', this.onClick);
		this.observer = new MutationObserver(() => this.refresh());
		this.observer.observe(element, { childList: true, subtree: true });
		this.stopWatchingRemoval = onRemoving(element, () => this.detach());
	}

	/**
	 * Makes a breadcrumb of the element, as Wicket does.
	 *
	 * @param element the element or its id
	 * @param options the separator, how many crumbs show and the texts
	 * @returns the breadcrumb, null when there is no such element
	 */
	static attach(element: HTMLElement | string, options?: BreadcrumbOptions): Breadcrumb | null {
		const resolved = resolve(element);
		return resolved ? new Breadcrumb(resolved, options) : null;
	}

	/**
	 * @param target the element of the breadcrumb or its id
	 * @returns the breadcrumb, if there is one
	 */
	static of(target: HTMLElement | string | null | undefined): Breadcrumb | null {
		const element = typeof target === 'string' ? document.getElementById(target) : target;
		return (element && instances.get(element)) || null;
	}

	separator(): BreadcrumbSeparator {
		const separator = this.options.separator;
		return separator && BREADCRUMB_SEPARATORS.includes(separator) ? separator : 'chevron';
	}

	/**
	 * @returns the look of the breadcrumb: that of the options, else that of the element it is in
	 */
	look(): Look {
		return this.options.look ?? lookOf(this.element);
	}

	texts(): BreadcrumbTexts {
		return { ...DEFAULT_BREADCRUMB_TEXTS, ...this.options.texts };
	}

	/**
	 * @returns whether the trail hides crumbs now
	 */
	isCollapsed(): boolean {
		return this.hidden().length > 0;
	}

	/**
	 * @returns the crumbs of the trail, the button "…" not among them
	 */
	items(): HTMLElement[] {
		const list = this.list();
		return list ? (Array.from(list.children) as HTMLElement[]).filter((child) =>
			child.classList.contains(BREADCRUMB_CLASSES.item) &&
			!child.classList.contains(BREADCRUMB_CLASSES.more)) : [];
	}

	/**
	 * Shows the crumbs hidden, until the crumbs change.
	 *
	 * @param focus whether the first of them takes the focus; true when left out
	 */
	expand(focus = true): void {
		const hidden = this.hidden();
		if (!hidden.length) {
			return;
		}
		this.expanded = true;
		this.refresh();
		if (focus) {
			const first = hidden[0].querySelector<HTMLElement>('a[href], button, [tabindex]') ??
				hidden[0].querySelector<HTMLElement>('.' + BREADCRUMB_CLASSES.link);
			if (first) {
				first.focus();
			} else {
				this.list()?.querySelector<HTMLElement>('a[href], button')?.focus();
			}
		}
		this.options.onExpand?.();
	}

	/**
	 * Renders the crumbs again: after the page changed them.
	 */
	refresh(): void {
		this.render();
		// what the breadcrumb changed is its own, not the page's
		this.observer?.takeRecords();
	}

	/**
	 * Takes other options; those left out stay as they were.
	 */
	configure(options: BreadcrumbOptions = {}): void {
		const defined = Object.fromEntries(Object.entries(options)
			.filter(([, value]) => value !== undefined)) as BreadcrumbOptions;
		const maxItems = this.options.maxItems;
		this.options = { ...this.options, ...defined };
		if (maxItems !== this.options.maxItems) {
			this.expanded = false;
		}
		this.refresh();
	}

	/**
	 * Takes the breadcrumb away, every crumb shown and the button "…" gone; the classes stay, as
	 * the page has them.
	 */
	destroy(): void {
		this.detach();
		this.items().forEach((crumb) => this.show(crumb));
		this.moreItem()?.setAttribute('hidden', '');
		this.made?.remove();
		this.made = null;
		if (instances.get(this.element) === this) {
			instances.delete(this.element);
		}
	}

	/**
	 * Stops listening, leaving the elements in place.
	 */
	private detach(): void {
		this.element.removeEventListener('click', this.onClick);
		this.observer?.disconnect();
		this.observer = null;
		this.stopWatchingRemoval?.();
		this.stopWatchingRemoval = null;
	}

	private list(): HTMLElement | null {
		return (Array.from(this.element.children) as HTMLElement[]).find((child) =>
			child.classList.contains(BREADCRUMB_CLASSES.list)) ?? null;
	}

	private moreItem(): HTMLElement | null {
		const list = this.list();
		return list ? (Array.from(list.children) as HTMLElement[]).find((child) =>
			child.classList.contains(BREADCRUMB_CLASSES.more)) ?? null : null;
	}

	/**
	 * @returns the crumbs of the trail the page shows: those it hid itself are none of the trail
	 */
	private trail(): HTMLElement[] {
		return this.items().filter((crumb) => !crumb.hidden ||
			crumb.hasAttribute(BREADCRUMB_COLLAPSED_ATTRIBUTE));
	}

	/**
	 * @returns the crumbs the trail hides
	 */
	private hidden(): HTMLElement[] {
		return this.items().filter((crumb) => crumb.hasAttribute(BREADCRUMB_COLLAPSED_ATTRIBUTE));
	}

	private hide(crumb: HTMLElement): void {
		crumb.hidden = true;
		crumb.setAttribute(BREADCRUMB_COLLAPSED_ATTRIBUTE, '');
	}

	/**
	 * Shows a crumb the trail hid; one the page hid stays hidden.
	 */
	private show(crumb: HTMLElement): void {
		if (crumb.hasAttribute(BREADCRUMB_COLLAPSED_ATTRIBUTE)) {
			crumb.hidden = false;
			crumb.removeAttribute(BREADCRUMB_COLLAPSED_ATTRIBUTE);
		}
	}

	/**
	 * Gives the breadcrumb, its list, its crumbs and its button "…" the classes of their parts in a
	 * look, those of the separator and of the last crumb among them, taking away those of the
	 * others; none of them when off. The elements are found before: off takes away the classes
	 * they are found by.
	 */
	private renderParts(look: Look, on: boolean, list: HTMLElement | null, crumbs: HTMLElement[],
		more: HTMLElement | null): void {
		const separator = this.separator();
		toggleParts(this.element, look, on, 'breadcrumb');
		BREADCRUMB_SEPARATORS.forEach((each) => toggleParts(this.element, look,
			on && each === separator, `breadcrumb-${each}`));
		if (list) {
			toggleParts(list, look, on, 'breadcrumb-list');
		}
		crumbs.forEach((crumb, index) => {
			toggleParts(crumb, look, on, 'breadcrumb-item');
			toggleParts(crumb, look, on && index === crumbs.length - 1, 'breadcrumb-current');
		});
		if (more) {
			toggleParts(more, look, on, 'breadcrumb-item', 'breadcrumb-more');
			const button = more.querySelector('button');
			if (button) {
				toggleParts(button, look, on, 'breadcrumb-more-button');
			}
		}
	}

	private render(): void {
		const look = this.look();
		const list = this.list();
		const crumbs = this.trail();
		const more = this.moreItem() ?? this.made;
		// a look other than the one before: its classes go
		if (this.applied && this.applied !== look) {
			this.renderParts(this.applied, false, list, crumbs, more);
		}
		this.renderParts(look, true, list, crumbs, more);
		this.applied = look;
		const texts = this.texts();
		if (this.options.texts?.label || !this.element.hasAttribute('aria-label') &&
			!this.element.hasAttribute('aria-labelledby')) {
			this.element.setAttribute('aria-label', texts.label);
		}
		if (!list) {
			return;
		}
		if (crumbs.length !== this.crumbs.length ||
			crumbs.some((crumb, index) => crumb !== this.crumbs[index])) {
			this.expanded = false;
		}
		this.crumbs = crumbs;
		const range = this.expanded ? null : collapsedCrumbs(crumbs.length, this.options.maxItems);
		crumbs.forEach((crumb, index) => {
			if (range && index >= range.from && index < range.to) {
				this.hide(crumb);
			} else {
				this.show(crumb);
			}
		});
		this.renderMore(list, look, more, range ? crumbs[range.from] : null,
			range ? range.to - range.from : 0);
	}

	/**
	 * Shows the button "…" before the first crumb hidden, saying how many there are, or hides it.
	 *
	 * @param more the item of the button, null when the breadcrumb has to make one
	 */
	private renderMore(list: HTMLElement, look: Look, more: HTMLElement | null,
		before: HTMLElement | null, count: number): void {
		if (!before) {
			if (more) {
				more.hidden = true;
			}
			return;
		}
		if (!more) {
			more = document.createElement('li');
			const button = document.createElement('button');
			button.type = 'button';
			button.textContent = ELLIPSIS;
			more.appendChild(button);
			this.made = more;
			toggleParts(more, look, true, 'breadcrumb-item', 'breadcrumb-more');
			toggleParts(button, look, true, 'breadcrumb-more-button');
		}
		more.hidden = false;
		if (more.parentElement !== list || more.nextElementSibling !== before) {
			list.insertBefore(more, before);
		}
		const button = more.querySelector('button');
		if (button) {
			const label = format(this.texts().more, count);
			button.setAttribute('aria-label', label);
			button.title = label;
		}
	}
}

/**
 * Makes a breadcrumb of an element, in the shape of a Svelte action: use:breadcrumb={options}.
 *
 * @param element the element of the breadcrumb
 * @param options the separator, how many crumbs show and the texts
 * @returns update() taking other options, destroy() taking the breadcrumb away
 */
export function breadcrumb(element: HTMLElement, options?: BreadcrumbOptions): {
	update(options?: BreadcrumbOptions): void;
	destroy(): void;
} {
	const instance = new Breadcrumb(element, options);
	return {
		update: (next) => instance.configure(next),
		destroy: () => instance.destroy()
	};
}
