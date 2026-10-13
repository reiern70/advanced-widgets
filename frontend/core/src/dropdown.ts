/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * A drop-down drawn over a <select>, single or multiple, that looks the same in every browser.
 *
 * new Dropdown(select, options) puts the select into a div.antilia-dropdown, or uses the one it is
 * in already, and adds a button and a list of the select's options to it; the select itself is
 * hidden but stays in the page, holds the choice and fires its 'input' and 'change' events when
 * the user chooses, so forms, Ajax behaviors and the bindings of a framework work as with a native
 * select. Attaching a select that is in a wrapper already, for example one Ajax put in place of
 * the previous one, rebuilds the button and the list.
 *
 * A framework rendering the select renders the wrapper too, so that the drop-down never moves an
 * element the framework owns: <div class="antilia-dropdown"><select>...</select></div>.
 */

import { Chips } from './chips.js';
import { onRemoving, type Unsubscribe } from './host.js';
import { lookOf, partClasses, toggleParts, type Look, type LookPart } from './look.js';
import { Scrollbar, type ScrollbarHandle } from './scrollbar.js';

/**
 * The texts of a drop-down and when it shows a filter.
 */
export interface DropdownOptions {
	/** The text while nothing is chosen. */
	placeholder?: string;
	/** The placeholder of the filter field. */
	filter?: string;
	/** The text when the filter matches nothing. */
	noMatch?: string;
	/** The chip of the chosen options a multiple drop-down's button has no room for, '+{0}'. */
	more?: string;
	/** The label of the x of a chip, '{0}' standing for its text: 'Remove {0}'. */
	remove?: string;
	/** The number of options from which on the list has a filter, 8 when left out. */
	filterThreshold?: number;
	/** The look of the drop-down; that of the element it is in when left out (see lookOf). */
	look?: Look;
}

const WRAPPER_CLASS = 'antilia-dropdown';

/** The chosen options a multiple drop-down shows in its button before it shows '+n'. */
const MAX_CHIPS = 3;

/** The number of options from which on the list has a filter, unless set otherwise. */
export const DEFAULT_FILTER_THRESHOLD = 8;

/** The drop-downs by their wrapper. */
const instances = new WeakMap<Element, Dropdown>();

let counter = 0;

function create<K extends keyof HTMLElementTagNameMap>(tag: K, look: Look, part: LookPart,
	text?: string): HTMLElementTagNameMap[K] {
	const element = document.createElement(tag);
	element.className = partClasses(look, part);
	if (text !== undefined) {
		element.textContent = text;
	}
	return element;
}

function resolve(select: HTMLSelectElement | string | null | undefined): HTMLSelectElement | null {
	const element = typeof select === 'string' ? document.getElementById(select) : select;
	return element instanceof HTMLSelectElement ? element : null;
}

/**
 * The drop-down of a select.
 */
export class Dropdown {
	readonly select: HTMLSelectElement;

	options: DropdownOptions;

	readonly multiple: boolean;

	wrapper!: HTMLElement;

	id!: string;

	toggle!: HTMLButtonElement;

	value!: HTMLSpanElement;

	menu!: HTMLDivElement;

	filter: HTMLInputElement | null = null;

	list!: HTMLUListElement;

	/** The wrapper of the scroll bars of the list. */
	listBox!: HTMLDivElement;

	empty!: HTMLDivElement;

	items: HTMLLIElement[] = [];

	active: HTMLLIElement | null = null;

	/** Whether the wrapper was made by the drop-down, and is taken away again by destroy(). */
	private ownWrapper = false;

	/** The look of the drop-down, whose classes the elements it makes get. */
	private look!: Look;

	private listeners: Array<[EventTarget, string, EventListener]> = [];

	private stopWatchingRemoval: Unsubscribe | null = null;

	private scrollbar: ScrollbarHandle | undefined = undefined;

	/** Gives the list its scroll bars; Wicket gives those of Wicket.Scrollbar, if any. */
	static attachScrollbar: (list: HTMLElement, look?: Look) => ScrollbarHandle | undefined =
		(list, look) => Scrollbar.attach(list, { axis: 'y', look });

	/**
	 * @param select the select
	 * @param options the texts and when to show the filter
	 */
	constructor(select: HTMLSelectElement, options?: DropdownOptions) {
		this.select = select;
		this.options = { ...options };
		this.multiple = select.multiple;
		this.attach();
	}

	/**
	 * Draws the drop-down of a select.
	 *
	 * @param select the select or its id
	 * @param options the texts and when to show the filter
	 * @returns the drop-down, null when there is no such select
	 */
	static attach(select: HTMLSelectElement | string, options?: DropdownOptions): Dropdown | null {
		const element = resolve(select);
		return element ? new Dropdown(element, options) : null;
	}

	/**
	 * @param select the select or its id
	 * @returns the drop-down of a select, if it has one
	 */
	static of(select: HTMLSelectElement | string | null | undefined): Dropdown | null {
		const element = resolve(select);
		const wrapper = element?.parentElement;
		const dropdown = wrapper ? instances.get(wrapper) : undefined;
		return dropdown && dropdown.select === element ? dropdown : null;
	}

	private attach(): void {
		const select = this.select;
		this.look = this.options.look ?? lookOf(select);
		let wrapper = select.parentElement;
		if (!wrapper || !wrapper.classList.contains(WRAPPER_CLASS)) {
			wrapper = create('div', this.look, 'dropdown');
			select.parentNode?.insertBefore(wrapper, select);
			wrapper.appendChild(select);
			this.ownWrapper = true;
		}
		instances.get(wrapper)?.detach();
		Array.from(wrapper.children).forEach((child) => {
			if (child !== select) {
				child.remove();
			}
		});
		instances.set(wrapper, this);
		wrapper.classList.toggle('antilia-dropdown-multiple', this.multiple);
		this.wrapper = wrapper;
		this.id = select.id || ('antilia-dropdown-' + (++counter));
		select.classList.add('antilia-dropdown-native');
		select.tabIndex = -1;
		select.setAttribute('aria-hidden', 'true');

		this.toggle = create('button', this.look, 'dropdown-toggle');
		this.toggle.type = 'button';
		this.toggle.id = this.id + '-toggle';
		this.toggle.setAttribute('aria-haspopup', 'listbox');
		this.toggle.setAttribute('aria-expanded', 'false');
		this.toggle.setAttribute('aria-controls', this.id + '-list');
		this.value = create('span', this.look, 'dropdown-value');
		this.toggle.appendChild(this.value);
		this.toggle.appendChild(create('span', this.look, 'dropdown-arrow'));
		wrapper.appendChild(this.toggle);

		this.menu = create('div', this.look, 'dropdown-menu');
		this.menu.hidden = true;
		this.list = create('ul', this.look, 'dropdown-list');
		this.list.id = this.id + '-list';
		this.list.tabIndex = -1;
		this.list.setAttribute('role', 'listbox');
		if (this.multiple) {
			this.list.setAttribute('aria-multiselectable', 'true');
		}
		// the list scrolls in the wrapper of the slim scroll bars of the core
		this.listBox = create('div', this.look, 'scrollbar');
		this.listBox.appendChild(this.list);
		this.menu.appendChild(this.listBox);
		this.empty = create('div', this.look, 'dropdown-empty');
		this.empty.hidden = true;
		this.menu.appendChild(this.empty);
		wrapper.appendChild(this.menu);

		this.renderOptions();
		this.bind();
		if (this.list.isConnected) {
			this.scrollbar = Dropdown.attachScrollbar(this.list, this.look);
		}
		this.stopWatchingRemoval = onRemoving(wrapper, () => this.detach());
	}

	/**
	 * Takes other texts or another threshold of the filter.
	 *
	 * @param options the texts and when to show the filter
	 */
	configure(options?: DropdownOptions): void {
		this.options = { ...options };
		this.refresh();
	}

	/**
	 * Draws the options again, after options of the select were added, removed or renamed, and
	 * shows its choice.
	 */
	refresh(): void {
		const open = this.isOpen();
		this.close(false);
		this.renderOptions();
		if (open) {
			this.open();
		}
	}

	/**
	 * Takes the drop-down away and shows the native select again; a wrapper the drop-down made is
	 * taken away too.
	 */
	destroy(): void {
		this.detach();
		const select = this.select;
		this.toggle.remove();
		this.menu.remove();
		select.classList.remove('antilia-dropdown-native');
		if (!select.classList.length) {
			select.removeAttribute('class');
		}
		select.removeAttribute('tabindex');
		select.removeAttribute('aria-hidden');
		this.wrapper.classList.remove('antilia-dropdown-multiple', 'antilia-dropdown-disabled',
			'antilia-dropdown-open', 'antilia-dropdown-up');
		if (instances.get(this.wrapper) === this) {
			instances.delete(this.wrapper);
		}
		if (this.ownWrapper && this.wrapper.parentNode) {
			this.wrapper.parentNode.insertBefore(select, this.wrapper);
			this.wrapper.remove();
		}
	}

	private threshold(): number {
		const threshold = this.options.filterThreshold;
		return typeof threshold === 'number' ? threshold : DEFAULT_FILTER_THRESHOLD;
	}

	/**
	 * @returns the text of the labels of the select, if it has any
	 */
	labelText(): string {
		const texts: string[] = [];
		Array.from(this.select.labels ?? []).forEach((label) => {
			const copy = label.cloneNode(true) as HTMLElement;
			copy.querySelectorAll('select, .' + WRAPPER_CLASS).forEach((element) => element.remove());
			const text = (copy.textContent ?? '').replace(/\s+/g, ' ').trim();
			if (text) {
				texts.push(text);
			}
		});
		return texts.join(' ') || this.select.getAttribute('aria-label') || this.select.title || '';
	}

	/**
	 * Draws the label, the filter, the options and the choice.
	 */
	private renderOptions(): void {
		const label = this.labelText();
		for (const element of [this.toggle, this.list]) {
			if (label) {
				element.setAttribute('aria-label', label);
			} else {
				element.removeAttribute('aria-label');
			}
		}
		const withFilter = this.select.options.length >= this.threshold();
		if (withFilter && !this.filter) {
			this.filter = create('input', this.look, 'dropdown-filter');
			this.filter.type = 'search';
			this.filter.autocomplete = 'off';
			this.filter.setAttribute('aria-controls', this.id + '-list');
			this.listen(this.filter, 'input', () => this.applyFilter());
			this.menu.insertBefore(this.filter, this.listBox);
		} else if (!withFilter && this.filter) {
			this.filter.remove();
			this.filter = null;
		}
		if (this.filter) {
			this.filter.placeholder = this.options.filter || 'Filter…';
		}
		this.empty.textContent = this.options.noMatch || 'No matches';

		this.list.textContent = '';
		this.items = [];
		this.active = null;
		Array.from(this.select.options).forEach((option, index) => {
			const item = create('li', this.look, 'dropdown-option');
			item.id = this.id + '-option-' + index;
			item.setAttribute('role', 'option');
			item.dataset.index = String(index);
			if (this.multiple) {
				item.appendChild(create('span', this.look, 'dropdown-check'));
			}
			item.appendChild(create('span', this.look, 'dropdown-text', option.text));
			if (option.disabled) {
				item.setAttribute('aria-disabled', 'true');
			}
			if (option.value === '' && !this.multiple) {
				toggleParts(item, this.look, true, 'dropdown-option-none');
			}
			this.list.appendChild(item);
			this.items.push(item);
		});
		this.update();
	}

	private optionOf(item: HTMLElement): HTMLOptionElement {
		return this.select.options[Number(item.dataset.index)];
	}

	/**
	 * Shows the choice of the select: in the button and in the list.
	 */
	update(): void {
		const select = this.select;
		this.items.forEach((item) => {
			item.setAttribute('aria-selected', String(Boolean(this.optionOf(item)?.selected)));
		});
		this.toggle.disabled = select.disabled;
		this.wrapper.classList.toggle('antilia-dropdown-disabled', select.disabled);

		this.value.textContent = '';
		this.toggle.removeAttribute('title');
		const chosen = Array.from(select.options).filter((option) => option.selected &&
			option.value !== '');
		if (!chosen.length) {
			const none = !this.multiple && select.selectedIndex >= 0 ?
				select.options[select.selectedIndex].text : '';
			this.value.textContent = none || this.options.placeholder || '';
			toggleParts(this.value, this.look, true, 'dropdown-placeholder');
			return;
		}
		toggleParts(this.value, this.look, false, 'dropdown-placeholder');
		if (!this.multiple) {
			this.value.textContent = chosen[0].text;
			return;
		}
		const removeLabel = select.disabled ? null : (this.options.remove || 'Remove {0}');
		chosen.slice(0, MAX_CHIPS).forEach((option) => {
			const chip = Chips.create(option.text, removeLabel, this.look);
			chip.dataset.index = String(option.index);
			this.value.appendChild(chip);
		});
		if (chosen.length > MAX_CHIPS) {
			const more = (this.options.more || '+{0}').replace('{0}',
				String(chosen.length - MAX_CHIPS));
			this.value.appendChild(Chips.more(more, this.look));
		}
		this.toggle.title = chosen.map((option) => option.text).join(', ');
	}

	private listen<E extends Event>(target: EventTarget, type: string,
		listener: (event: E) => void): void {
		target.addEventListener(type, listener as EventListener);
		this.listeners.push([target, type, listener as EventListener]);
	}

	private bind(): void {
		this.listen(this.toggle, 'click', (event) => {
			// the x of a chip deselects its option, without opening the list
			const chip = Chips.removeTarget(event);
			if (chip) {
				event.preventDefault();
				this.deselect(Number(chip.dataset.index));
				return;
			}
			if (this.isOpen()) {
				this.close(false);
			} else {
				this.open();
			}
		});
		this.listen<KeyboardEvent>(this.toggle, 'keydown', (event) => {
			if (this.multiple && (event.key === 'Backspace' || event.key === 'Delete')) {
				// removes the last chosen option, as the x of its chip does
				event.preventDefault();
				const chosen = Array.from(this.select.options).filter((option) => option.selected &&
					option.value !== '');
				if (chosen.length) {
					this.deselect(chosen[chosen.length - 1].index);
				}
				return;
			}
			if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
				event.preventDefault();
				this.open();
			}
		});
		const itemOf = (event: Event) => (event.target as Element).closest<HTMLLIElement>(
			'.antilia-dropdown-option');
		this.listen(this.list, 'click', (event) => {
			const item = itemOf(event);
			if (item) {
				this.choose(item);
			}
		});
		this.listen(this.list, 'mousemove', (event) => {
			const item = itemOf(event);
			if (item && item !== this.active) {
				this.activate(item, false);
			}
		});
		this.listen<KeyboardEvent>(this.menu, 'keydown', (event) => this.onKey(event));
		// a click on the label of the hidden select focuses the button
		this.listen(this.select, 'focus', () => this.toggle.focus());
		// the select changed by someone else, for example a script resetting a form
		this.listen(this.select, 'change', () => this.update());
		const outside = (event: Event) => {
			if (this.isOpen() && !this.wrapper.contains(event.target as Node)) {
				this.close(false);
			}
		};
		this.listen(document, 'mousedown', outside);
		this.listen(document, 'focusin', outside);
	}

	/**
	 * Stops listening, leaving the elements in place.
	 */
	private detach(): void {
		this.listeners.forEach(([target, type, listener]) => {
			target.removeEventListener(type, listener);
		});
		this.listeners = [];
		this.scrollbar?.destroy();
		this.scrollbar = undefined;
		this.stopWatchingRemoval?.();
		this.stopWatchingRemoval = null;
	}

	isOpen(): boolean {
		return !this.menu.hidden;
	}

	open(): void {
		if (this.select.disabled || this.isOpen()) {
			return;
		}
		this.menu.hidden = false;
		this.toggle.setAttribute('aria-expanded', 'true');
		this.wrapper.classList.add('antilia-dropdown-open');
		// open upwards when there is no room below but there is above
		this.wrapper.classList.remove('antilia-dropdown-up');
		const box = this.menu.getBoundingClientRect();
		const toggle = this.toggle.getBoundingClientRect();
		if (box.bottom > window.innerHeight && toggle.top > box.height) {
			this.wrapper.classList.add('antilia-dropdown-up');
		}
		if (this.filter) {
			this.filter.value = '';
			this.applyFilter();
		}
		const selected = this.items.find((item) => item.getAttribute('aria-selected') === 'true');
		this.activate(selected || this.visibleItems()[0] || null, true);
		(this.filter || this.list).focus();
	}

	/**
	 * @param focus whether to focus the button again
	 */
	close(focus: boolean): void {
		if (!this.isOpen()) {
			return;
		}
		this.menu.hidden = true;
		this.toggle.setAttribute('aria-expanded', 'false');
		this.wrapper.classList.remove('antilia-dropdown-open', 'antilia-dropdown-up');
		if (focus) {
			this.toggle.focus();
		}
	}

	visibleItems(): HTMLLIElement[] {
		return this.items.filter((item) => !item.hidden &&
			item.getAttribute('aria-disabled') !== 'true');
	}

	/**
	 * Marks an option as the one the keyboard acts on.
	 */
	activate(item: HTMLLIElement | null, scroll: boolean): void {
		if (this.active) {
			toggleParts(this.active, this.look, false, 'dropdown-option-active');
		}
		this.active = item;
		const owner = this.filter || this.list;
		if (!item) {
			owner.removeAttribute('aria-activedescendant');
			return;
		}
		toggleParts(item, this.look, true, 'dropdown-option-active');
		owner.setAttribute('aria-activedescendant', item.id);
		if (scroll) {
			const list = this.list;
			if (item.offsetTop < list.scrollTop) {
				list.scrollTop = item.offsetTop;
			} else if (item.offsetTop + item.offsetHeight > list.scrollTop + list.clientHeight) {
				list.scrollTop = item.offsetTop + item.offsetHeight - list.clientHeight;
			}
		}
	}

	private applyFilter(): void {
		const text = (this.filter?.value ?? '').trim().toLowerCase();
		let shown = 0;
		this.items.forEach((item) => {
			const match = !text || (item.textContent ?? '').toLowerCase().includes(text);
			item.hidden = !match;
			if (match) {
				shown++;
			}
		});
		this.empty.hidden = shown > 0;
		if (!this.active || this.active.hidden) {
			this.activate(this.visibleItems()[0] || null, true);
		}
	}

	private onKey(event: KeyboardEvent): void {
		const visible = this.visibleItems();
		const index = this.active ? visible.indexOf(this.active) : -1;
		switch (event.key) {
			case 'ArrowDown':
				event.preventDefault();
				this.activate(visible[Math.min(index + 1, visible.length - 1)] || null, true);
				break;
			case 'ArrowUp':
				event.preventDefault();
				this.activate(visible[Math.max(index - 1, 0)] || null, true);
				break;
			case 'Home':
			case 'End':
				if (event.target === this.filter) {
					return;
				}
				event.preventDefault();
				this.activate(visible[event.key === 'Home' ? 0 : visible.length - 1] || null, true);
				break;
			case 'Enter':
				event.preventDefault();
				if (this.active) {
					this.choose(this.active);
				}
				break;
			case ' ':
				if (event.target === this.filter) {
					return;
				}
				event.preventDefault();
				if (this.active) {
					this.choose(this.active);
				}
				break;
			case 'Escape':
				event.preventDefault();
				event.stopPropagation();
				this.close(true);
				break;
			case 'Tab':
				this.close(false);
				break;
		}
	}

	/**
	 * Deselects an option of a multiple drop-down, as the x of its chip does.
	 *
	 * @param index the index of the option in the select
	 */
	deselect(index: number): void {
		const option = this.select.options[index];
		if (!option || !option.selected || this.select.disabled) {
			return;
		}
		option.selected = false;
		this.update();
		this.changed();
	}

	/**
	 * Fires the select's input and change events, then shows the choice it holds: a listener may
	 * have set it back, as React does with a controlled select whose value its parent did not
	 * change.
	 */
	private changed(): void {
		this.select.dispatchEvent(new Event('input', { bubbles: true }));
		this.select.dispatchEvent(new Event('change', { bubbles: true }));
		this.update();
	}

	/**
	 * Chooses an option: a single drop-down takes it and closes, a multiple one toggles it.
	 */
	choose(item: HTMLLIElement): void {
		if (item.getAttribute('aria-disabled') === 'true') {
			return;
		}
		const option = this.optionOf(item);
		let changed: boolean;
		if (this.multiple) {
			option.selected = !option.selected;
			changed = true;
			this.activate(item, false);
		} else {
			changed = !option.selected;
			option.selected = true;
			this.close(true);
		}
		this.update();
		if (changed) {
			this.changed();
		}
	}
}

/**
 * Draws the drop-down of a select, in the shape of a Svelte action: use:dropdown={options}.
 *
 * @param select the select
 * @param options the texts and when to show the filter
 * @returns update() taking other options, destroy() taking the drop-down away
 */
export function dropdown(select: HTMLSelectElement, options?: DropdownOptions): {
	update(options?: DropdownOptions): void;
	destroy(): void;
} {
	const instance = new Dropdown(select, options);
	return {
		update: (next) => instance.configure(next),
		destroy: () => instance.destroy()
	};
}
