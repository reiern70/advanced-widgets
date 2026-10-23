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
 * A palette: two lists side by side, the choices still available on the left and the chosen ones
 * on the right, with arrow buttons between them moving the highlighted choices, or all of them,
 * from one to the other. A double click, Enter or dragging a choice to the other list moves it
 * too. When the order of the chosen ones matters, they keep the order they were chosen in, and
 * are moved up and down with the buttons next to their list, Alt with the arrow keys or by
 * dragging them; otherwise they show in the order of the choices.
 *
 * new Palette(element, options) draws the palette into an empty element, holding the choice
 * itself and calling onChange with it: React and Svelte render the element. Palette.attach(select)
 * draws it over a <select multiple>, as Wicket does: the select is hidden but stays in the page,
 * holds the choice, in the chosen order, and fires its 'input' and 'change' events, so a form
 * posts it and Ajax behaviors are notified as with a native select.
 */

import { onRemoving, type Unsubscribe } from './host.js';
import { lookOf, partClasses, toggleParts, type Look, type LookPart } from './look.js';
import { Scrollbar, type ScrollbarHandle } from './scrollbar.js';

/**
 * A choice of a palette.
 */
export interface PaletteChoice {
	value: string;
	label: string;
	/** A disabled choice stays where it is. */
	disabled?: boolean;
}

/**
 * The texts of a palette.
 */
export interface PaletteTexts {
	/** The title of the list of the available choices. */
	availableTitle?: string;
	/** The title of the list of the chosen ones. */
	selectedTitle?: string;
	/** The labels of the buttons. */
	add?: string;
	addAll?: string;
	remove?: string;
	removeAll?: string;
	up?: string;
	down?: string;
	/** The placeholder of the filter field. */
	filter?: string;
	/** The text of an empty list. */
	empty?: string;
}

/**
 * What a palette shows and how it behaves.
 */
export interface PaletteOptions extends PaletteTexts {
	/** The choices, in their order; those of the select for Palette.attach(). */
	choices?: PaletteChoice[];
	/** The look of the palette; that of the element it is in when left out (see lookOf). */
	look?: Look;
	/** The values of the chosen ones, in their order. */
	selected?: string[];
	/** Whether the order of the chosen ones matters: they can be moved up and down. */
	ordered?: boolean;
	/** Whether the rows of both lists show their number, from 1, before their text. */
	numbered?: boolean;
	/** Whether the palette is disabled; that of the select for Palette.attach(). */
	disabled?: boolean;
	/** Whether the available ones can have a filter, true when left out. */
	filterable?: boolean;
	/** The number of choices from which on the available ones have a filter, 8 when left out. */
	filterThreshold?: number;
	/**
	 * How the filter matches a choice: 'start' (the label starts with the text), 'anywhere'
	 * (contains it), 'end' (ends with it) or 'words' (each word of the text starts a word of the
	 * label, in their order), the name of a matcher put in Palette.matchers, or a
	 * matcher itself; 'anywhere' when left out. The matched parts of the labels are underlined.
	 */
	match?: PaletteMatch;
	/**
	 * How many rows a list draws at first, and adds each time it is scrolled to its end, 30 when
	 * left out; 0 draws them all. A filter searches all choices, drawn or not.
	 */
	pageSize?: number;
	/** Called with the values of the chosen ones, in their order, after the user changed them. */
	onChange?: (selected: string[]) => void;
}

/**
 * Matches the text of the filter against the label of a choice.
 *
 * @param label the label of the choice
 * @param text the text of the filter, trimmed, never empty
 * @returns the parts of the label it matches, as [start, end) pairs in order, which are
 *          underlined; null or an empty array when it does not match
 */
export type PaletteMatcher = (label: string, text: string) => Array<[number, number]> | null;

/**
 * How the filter matches: the name of a matcher in Palette.matchers, or a matcher.
 */
export type PaletteMatch = 'start' | 'anywhere' | 'end' | 'words' | (string & {}) |
	PaletteMatcher;

/**
 * Matches each word of the text against the start of a word of the label, in their order, each
 * word of the label once: "s ge" matches "South Georgia".
 */
function matchWords(label: string, text: string): Array<[number, number]> | null {
	const words = Array.from(label.matchAll(/[\p{L}\p{N}]+/gu), (word) =>
		[word.index!, word[0].toLowerCase()] as const);
	const parts: Array<[number, number]> = [];
	let next = 0;
	for (const typed of text.toLowerCase().split(/\s+/).filter(Boolean)) {
		while (next < words.length && !words[next][1].startsWith(typed)) {
			next++;
		}
		if (next === words.length) {
			return null;
		}
		const start = words[next++][0];
		parts.push([start, start + typed.length]);
	}
	return parts;
}

/** The matchers built in, ignoring case. */
export const PALETTE_MATCHERS: Readonly<Record<'start' | 'anywhere' | 'end' | 'words',
	PaletteMatcher>> = {
	start: (label, text) => label.toLowerCase().startsWith(text.toLowerCase()) ?
		[[0, text.length]] : null,
	anywhere: (label, text) => {
		const index = label.toLowerCase().indexOf(text.toLowerCase());
		return index < 0 ? null : [[index, index + text.length]];
	},
	end: (label, text) => label.toLowerCase().endsWith(text.toLowerCase()) ?
		[[label.length - text.length, label.length]] : null,
	words: matchWords
};

type Side = 'available' | 'selected';

interface List {
	side: Side;
	box: HTMLDivElement;
	title: HTMLDivElement;
	list: HTMLUListElement;
	scroller: HTMLDivElement;
	empty: HTMLDivElement;
	/** The choices of the list, those the filter lets through, drawn or not. */
	choices: PaletteChoice[];
	/** The items drawn, those of the first choices; more are drawn as the list scrolls. */
	items: HTMLLIElement[];
	/** The values highlighted, which the buttons move. */
	highlighted: Set<string>;
	/** The value the keyboard is on. */
	active: string | null;
	/** Where a click with Shift highlights from. */
	anchor: string | null;
	scrollbar?: ScrollbarHandle;
}

interface Drag {
	from: Side;
	values: string[];
	pointerId: number;
	startX: number;
	startY: number;
	started: boolean;
	ghost?: HTMLDivElement;
	marker?: HTMLDivElement;
	to?: Side;
	index?: number;
}

const CLASS = 'antilia-palette';
const ITEM_CLASS = 'antilia-palette-item';
const DRAGGING_CLASS = 'antilia-palette-dragging';
const OVER_CLASS = 'antilia-palette-over';
const NATIVE_CLASS = 'antilia-palette-native';

/** The number of choices from which on the available ones have a filter, unless set otherwise. */
export const PALETTE_FILTER_THRESHOLD = 8;

/** How many rows a list draws at first and adds at a time, unless set otherwise. */
export const PALETTE_PAGE_SIZE = 30;

/** How close, in pixels, a list has to be scrolled to its end to draw more rows. */
const MORE_DISTANCE = 48;

/** How far, in pixels, the pointer has to travel on a choice before it drags it. */
const DRAG_THRESHOLD = 4;

const DEFAULT_TEXTS: Required<PaletteTexts> = {
	availableTitle: 'Available',
	selectedTitle: 'Selected',
	add: 'Add',
	addAll: 'Add all',
	remove: 'Remove',
	removeAll: 'Remove all',
	up: 'Move up',
	down: 'Move down',
	filter: 'Filter…',
	empty: 'Nothing here'
};

/** The palettes by their element. */
const instances = new WeakMap<Element, Palette>();

let counter = 0;

function create<K extends keyof HTMLElementTagNameMap>(tag: K, className?: string,
	text?: string): HTMLElementTagNameMap[K] {
	const element = document.createElement(tag);
	if (className) {
		element.className = className;
	}
	if (text !== undefined) {
		element.textContent = text;
	}
	return element;
}

function button(look: Look, className: string, symbol: string): HTMLButtonElement {
	const element = create('button', partClasses(look, 'palette-button') + ' ' + className);
	element.type = 'button';
	element.appendChild(create('span', partClasses(look, 'palette-arrow'), symbol))
		.setAttribute('aria-hidden', 'true');
	return element;
}

function resolve(select: HTMLSelectElement | string | null | undefined): HTMLSelectElement | null {
	const element = typeof select === 'string' ? document.getElementById(select) : select;
	return element instanceof HTMLSelectElement ? element : null;
}

/**
 * Moves the values among a list one place up or down, each block of them as a whole, keeping
 * those at the edge where they are.
 *
 * @param list the values in their order
 * @param moved the values to move
 * @param down whether to move them down, or up
 * @returns the new order
 */
export function moveBy(list: string[], moved: Set<string>, down: boolean): string[] {
	const result = list.slice();
	const indexes = result.map((_value, index) => index);
	if (down) {
		indexes.reverse();
	}
	// the edge moves inwards past every moved value already at it
	let edge = down ? result.length - 1 : 0;
	for (const index of indexes) {
		if (!moved.has(result[index])) {
			continue;
		}
		if (index === edge) {
			edge += down ? -1 : 1;
			continue;
		}
		const other = index + (down ? 1 : -1);
		[result[index], result[other]] = [result[other], result[index]];
	}
	return result;
}

/**
 * Puts values in front of the one at an index of a list, keeping their own order.
 *
 * @param list the values in their order
 * @param moved the values to put there, which may be in the list already
 * @param index where to put them, in the list as it is, its length for the end
 * @returns the new order
 */
export function insertAt(list: string[], moved: string[], index: number): string[] {
	const set = new Set(moved);
	const before = list.slice(0, index).filter((value) => !set.has(value));
	const after = list.slice(index).filter((value) => !set.has(value));
	return [...before, ...moved, ...after];
}

/**
 * A palette.
 */
export class Palette {
	readonly element: HTMLElement;

	/** The select the palette is drawn over, by Palette.attach(). */
	readonly select: HTMLSelectElement | null;

	options: PaletteOptions;

	choices: PaletteChoice[] = [];

	/** The values of the chosen ones, in their order. */
	selected: string[] = [];

	readonly id: string;

	available!: List;

	chosen!: List;

	filter: HTMLInputElement | null = null;

	buttons!: {
		add: HTMLButtonElement;
		addAll: HTMLButtonElement;
		remove: HTMLButtonElement;
		removeAll: HTMLButtonElement;
		up: HTMLButtonElement;
		down: HTMLButtonElement;
	};

	/** The buttons moving the chosen ones up and down, shown while the order matters. */
	order!: HTMLDivElement;

	private listeners: Array<[EventTarget, string, EventListener]> = [];

	private drag: Drag | null = null;

	private stopWatchingRemoval: Unsubscribe | null = null;

	/** Whether the element was made by Palette.attach(), and is taken away again by destroy(). */
	private ownElement = false;

	/** The look of the palette, whose classes the elements it makes get. */
	private readonly look: Look;

	/**
	 * The matchers the option match can name: those built in, and any an application puts here,
	 * as a page does for a PaletteBehavior given a matcher of its own.
	 */
	static matchers: Record<string, PaletteMatcher> = { ...PALETTE_MATCHERS };

	/** The parts of the labels of the available choices the filter matched, by their value. */
	private matches = new Map<string, Array<[number, number]>>();

	/** Gives a list its scroll bars; Wicket gives those of Wicket.Scrollbar, if any. */
	static attachScrollbar: (list: HTMLElement, look?: Look) => ScrollbarHandle | undefined =
		(list, look) => Scrollbar.attach(list, { axis: 'y', look });

	/**
	 * @param element the element to draw the palette into, emptied first
	 * @param options the choices, the chosen ones, the texts and onChange
	 * @param select the select the palette is drawn over, for Palette.attach()
	 */
	constructor(element: HTMLElement, options: PaletteOptions = {}, select?: HTMLSelectElement) {
		this.element = element;
		this.select = select ?? null;
		this.options = { ...options };
		this.look = options.look ?? lookOf(element);
		this.id = (select?.id || element.id || 'antilia-palette-' + (++counter));
		instances.get(element)?.detach();
		instances.set(element, this);
		this.choices = this.readChoices();
		this.selected = this.clean(options.selected ?? this.readSelected());
		this.render();
		this.bind();
		this.stopWatchingRemoval = onRemoving(element, () => this.detach());
	}

	/**
	 * Draws the palette over a select, putting it into a div.antilia-palette, or using the one it
	 * is in already; the choices are its options, the chosen ones those selected, unless the
	 * options give them in their order.
	 *
	 * @param select the select or its id
	 * @param options the order of the chosen ones, the texts and whether the order matters
	 * @returns the palette, null when there is no such select
	 */
	static attach(select: HTMLSelectElement | string, options?: PaletteOptions): Palette | null {
		const element = resolve(select);
		if (!element) {
			return null;
		}
		let wrapper = element.parentElement;
		let own = false;
		if (!wrapper || !wrapper.classList.contains(CLASS)) {
			wrapper = create('div', partClasses(options?.look ?? lookOf(element), 'palette'));
			element.parentNode?.insertBefore(wrapper, element);
			wrapper.appendChild(element);
			own = true;
		}
		// a select Ajax put in place of the previous one takes over its palette's wrapper
		own = own || Boolean(instances.get(wrapper)?.ownElement);
		const palette = new Palette(wrapper, options, element);
		palette.ownElement = own;
		return palette;
	}

	/**
	 * @param target the element of the palette, or the select or its id
	 * @returns the palette, if there is one
	 */
	static of(target: HTMLElement | string | null | undefined): Palette | null {
		const element = typeof target === 'string' ? document.getElementById(target) : target;
		if (!element) {
			return null;
		}
		const palette = instances.get(element) ??
			(element.parentElement ? instances.get(element.parentElement) : undefined);
		return palette && (palette.element === element || palette.select === element) ? palette :
			null;
	}

	private texts(): Required<PaletteTexts> {
		const texts = { ...DEFAULT_TEXTS };
		for (const key of Object.keys(DEFAULT_TEXTS) as Array<keyof PaletteTexts>) {
			const text = this.options[key];
			if (text) {
				texts[key] = text;
			}
		}
		return texts;
	}

	private readChoices(): PaletteChoice[] {
		if (this.select) {
			return Array.from(this.select.options, (option) => ({ value: option.value,
				label: option.text, disabled: option.disabled }));
		}
		return (this.options.choices ?? []).map((choice) => ({ ...choice }));
	}

	/**
	 * @returns the values of the selected options of the select, in their order there
	 */
	private readSelected(): string[] {
		return this.select ? Array.from(this.select.selectedOptions, (option) => option.value) : [];
	}

	/**
	 * @returns the values among the choices, once each, in the order of the choices unless the
	 *          order matters
	 */
	private clean(values: string[]): string[] {
		const known = new Set(this.choices.map((choice) => choice.value));
		const unique = Array.from(new Set(values)).filter((value) => known.has(value));
		if (this.options.ordered) {
			return unique;
		}
		const chosen = new Set(unique);
		return this.choices.map((choice) => choice.value).filter((value) => chosen.has(value));
	}

	isDisabled(): boolean {
		return Boolean(this.select ? this.select.disabled : this.options.disabled);
	}

	private choice(value: string): PaletteChoice | undefined {
		return this.choices.find((choice) => choice.value === value);
	}

	private movable(value: string): boolean {
		const choice = this.choice(value);
		return Boolean(choice && !choice.disabled);
	}

	/**
	 * @returns the matcher of the option match, that matching anywhere for one not known
	 */
	private matcher(): PaletteMatcher {
		const match = this.options.match;
		if (typeof match === 'function') {
			return match;
		}
		return (match && Palette.matchers[match]) || PALETTE_MATCHERS.anywhere;
	}

	private threshold(): number {
		const threshold = this.options.filterThreshold;
		return typeof threshold === 'number' ? threshold : PALETTE_FILTER_THRESHOLD;
	}

	/**
	 * @returns how many rows a list draws at a time, Infinity for all
	 */
	private pageSize(): number {
		const size = this.options.pageSize;
		if (typeof size !== 'number' || size < 0) {
			return PALETTE_PAGE_SIZE;
		}
		return size === 0 ? Infinity : Math.max(1, Math.floor(size));
	}

	/**
	 * @returns the classes of parts of the palette in its look
	 */
	private css(...parts: LookPart[]): string {
		return partClasses(this.look, ...parts);
	}

	private values(list: List): string[] {
		return list.choices.map((choice) => choice.value);
	}

	private newList(side: Side): List {
		const box = create('div', this.css('palette-side', side === 'available' ?
			'palette-available' : 'palette-selected'));
		const title = create('div', this.css('palette-title'));
		title.id = this.id + '-' + side + '-title';
		box.appendChild(title);
		const list = create('ul', this.css('palette-list'));
		list.id = this.id + '-' + side;
		list.tabIndex = 0;
		list.setAttribute('role', 'listbox');
		list.setAttribute('aria-multiselectable', 'true');
		list.setAttribute('aria-labelledby', title.id);
		list.dataset.side = side;
		const scroller = create('div', this.css('scrollbar'));
		scroller.appendChild(list);
		box.appendChild(scroller);
		const empty = create('div', this.css('palette-empty'));
		scroller.appendChild(empty);
		return { side, box, title, list, scroller, empty, choices: [], items: [],
			highlighted: new Set(),
			active: null, anchor: null };
	}

	/**
	 * Draws the lists, the buttons and the choice.
	 */
	private render(): void {
		const element = this.element;
		Array.from(element.children).forEach((child) => {
			if (child !== this.select) {
				child.remove();
			}
		});
		element.classList.add(...this.css('palette').split(' '));
		if (this.select) {
			this.select.classList.add(NATIVE_CLASS);
			this.select.tabIndex = -1;
			this.select.setAttribute('aria-hidden', 'true');
		}
		this.available = this.newList('available');
		this.chosen = this.newList('selected');
		const middle = create('div', this.css('palette-buttons'));
		middle.setAttribute('role', 'group');
		this.buttons = {
			add: button(this.look, 'antilia-palette-add', '›'),
			addAll: button(this.look, 'antilia-palette-add-all', '»'),
			remove: button(this.look, 'antilia-palette-remove', '‹'),
			removeAll: button(this.look, 'antilia-palette-remove-all', '«'),
			up: button(this.look, 'antilia-palette-up', '↑'),
			down: button(this.look, 'antilia-palette-down', '↓')
		};
		middle.append(this.buttons.add, this.buttons.addAll, this.buttons.remove,
			this.buttons.removeAll);
		this.order = create('div', this.css('palette-buttons', 'palette-order'));
		this.order.setAttribute('role', 'group');
		this.order.append(this.buttons.up, this.buttons.down);
		element.append(this.available.box, middle, this.chosen.box, this.order);
		for (const list of [this.available, this.chosen]) {
			if (list.list.isConnected) {
				list.scrollbar = Palette.attachScrollbar(list.list, this.look);
			}
		}
		this.renderTexts();
		this.renderItems();
	}

	private renderTexts(): void {
		const texts = this.texts();
		this.available.title.textContent = texts.availableTitle;
		this.chosen.title.textContent = texts.selectedTitle;
		this.available.empty.textContent = texts.empty;
		this.chosen.empty.textContent = texts.empty;
		for (const name of Object.keys(this.buttons) as Array<keyof Palette['buttons']>) {
			this.buttons[name].title = texts[name];
			this.buttons[name].setAttribute('aria-label', texts[name]);
		}
		const withFilter = this.options.filterable !== false &&
			this.choices.length >= this.threshold();
		if (withFilter && !this.filter) {
			this.filter = create('input', this.css('palette-filter'));
			this.filter.type = 'search';
			this.filter.autocomplete = 'off';
			this.filter.setAttribute('aria-controls', this.available.list.id);
			this.listen(this.filter, 'input', () => {
				// the matches are drawn from the first one again
				this.available.items = [];
				this.available.list.scrollTop = 0;
				this.renderItems();
			});
			this.available.box.insertBefore(this.filter, this.available.scroller);
		} else if (!withFilter && this.filter) {
			this.filter.remove();
			this.filter = null;
		}
		if (this.filter) {
			this.filter.placeholder = texts.filter;
			this.filter.setAttribute('aria-label', texts.filter);
		}
		const ordered = Boolean(this.options.ordered);
		this.element.classList.toggle('antilia-palette-ordered', ordered);
		this.element.classList.toggle('antilia-palette-numbered', Boolean(this.options.numbered));
		this.order.hidden = !ordered;
	}

	/**
	 * Draws the items of both lists from the choices and the chosen ones, and the state of the
	 * buttons.
	 */
	private renderItems(): void {
		const chosen = new Set(this.selected);
		const text = (this.filter?.value ?? '').trim();
		const matcher = this.matcher();
		this.matches = new Map();
		const available = this.choices.filter((choice) => {
			if (chosen.has(choice.value)) {
				return false;
			}
			if (!text) {
				return true;
			}
			const parts = matcher(choice.label, text);
			if (!parts?.length) {
				return false;
			}
			this.matches.set(choice.value, parts);
			return true;
		});
		const selected = this.selected.map((value) => this.choice(value)!).filter(Boolean);
		this.fill(this.available, available);
		this.fill(this.chosen, selected);
		this.updateState();
		this.fillView(this.available);
		this.fillView(this.chosen);
	}

	/**
	 * Draws the items of a list: as many as were drawn before, at least a page of them, and the
	 * one the keyboard is on.
	 */
	private fill(list: List, choices: PaletteChoice[]): void {
		const values = new Set(choices.map((choice) => choice.value));
		list.highlighted.forEach((value) => {
			if (!values.has(value)) {
				list.highlighted.delete(value);
			}
		});
		if (list.active !== null && !values.has(list.active)) {
			list.active = null;
		}
		const active = list.active === null ? -1 :
			choices.findIndex((choice) => choice.value === list.active);
		const count = Math.max(list.items.length, this.pageSize(), active + 1);
		list.choices = choices;
		list.list.textContent = '';
		list.items = [];
		this.draw(list, count);
		list.empty.hidden = choices.length > 0;
		this.paint(list);
		list.scrollbar?.update();
	}

	/**
	 * Draws the items of a list up to a number of them, after those drawn already.
	 */
	private draw(list: List, count: number): void {
		const end = Math.min(count, list.choices.length);
		const start = list.items.length;
		if (end <= start) {
			return;
		}
		list.choices.slice(start, end).forEach((choice, offset) => {
			const index = start + offset;
			const item = create('li', this.css('palette-item'));
			item.id = list.list.id + '-' + index;
			item.setAttribute('role', 'option');
			item.dataset.value = choice.value;
			if (this.options.numbered) {
				item.appendChild(create('span', this.css('palette-number'), String(index + 1) + '.'))
					.setAttribute('aria-hidden', 'true');
			}
			item.appendChild(this.label(choice, list.side === 'available' ?
				this.matches.get(choice.value) : undefined));
			if (choice.disabled) {
				item.setAttribute('aria-disabled', 'true');
			}
			list.list.appendChild(item);
			list.items.push(item);
		});
		this.paint(list);
		list.scrollbar?.update();
	}

	/**
	 * @param parts the parts of the label to underline, as [start, end) pairs
	 * @returns the text of a choice, with those parts in a span.antilia-palette-match
	 */
	private label(choice: PaletteChoice, parts?: Array<[number, number]>): HTMLSpanElement {
		const text = create('span', this.css('palette-text'));
		const label = choice.label;
		let at = 0;
		for (const [from, to] of (parts ?? []).slice().sort((a, b) => a[0] - b[0])) {
			const start = Math.max(at, Math.min(from, label.length));
			const end = Math.min(Math.max(to, start), label.length);
			if (end <= start) {
				continue;
			}
			if (start > at) {
				text.appendChild(document.createTextNode(label.slice(at, start)));
			}
			text.appendChild(create('span', this.css('palette-match'), label.slice(start, end)));
			at = end;
		}
		if (at < label.length) {
			text.appendChild(document.createTextNode(label.slice(at)));
		}
		return text;
	}

	/**
	 * Draws the next page of a list's items, if some are not drawn yet.
	 *
	 * @param side the list
	 * @returns whether it drew any
	 */
	more(side: Side): boolean {
		const list = side === 'available' ? this.available : this.chosen;
		const before = list.items.length;
		this.draw(list, before + this.pageSize());
		return list.items.length > before;
	}

	/**
	 * Draws more items of a list while it is scrolled near its end, or too short to scroll.
	 */
	private fillView(list: List): void {
		const box = list.list;
		while (box.clientHeight > 0 && list.items.length < list.choices.length &&
			box.scrollTop + box.clientHeight >= box.scrollHeight - MORE_DISTANCE) {
			if (!this.more(list.side)) {
				break;
			}
		}
	}

	/**
	 * Shows the highlighted items and the active one of a list.
	 */
	private paint(list: List): void {
		let active: HTMLLIElement | null = null;
		list.items.forEach((item) => {
			const value = item.dataset.value!;
			item.setAttribute('aria-selected', String(list.highlighted.has(value)));
			toggleParts(item, this.look, value === list.active, 'palette-item-active');
			if (value === list.active) {
				active = item;
			}
		});
		if (active) {
			list.list.setAttribute('aria-activedescendant', (active as HTMLLIElement).id);
		} else {
			list.list.removeAttribute('aria-activedescendant');
		}
	}

	private updateState(): void {
		const disabled = this.isDisabled();
		this.element.classList.toggle('antilia-palette-disabled', disabled);
		const movable = (list: List) => list.choices.some((choice) => !choice.disabled);
		const highlighted = (list: List) => Array.from(list.highlighted).some((value) =>
			this.movable(value));
		this.buttons.add.disabled = disabled || !highlighted(this.available);
		this.buttons.addAll.disabled = disabled || !movable(this.available);
		this.buttons.remove.disabled = disabled || !highlighted(this.chosen);
		this.buttons.removeAll.disabled = disabled || !movable(this.chosen);
		const marked = this.selected.filter((value) => this.chosen.highlighted.has(value));
		this.buttons.up.disabled = disabled || !marked.length ||
			moveBy(this.selected, new Set(marked), false).join('\n') === this.selected.join('\n');
		this.buttons.down.disabled = disabled || !marked.length ||
			moveBy(this.selected, new Set(marked), true).join('\n') === this.selected.join('\n');
		for (const list of [this.available, this.chosen]) {
			list.list.tabIndex = disabled ? -1 : 0;
			if (disabled) {
				list.list.setAttribute('aria-disabled', 'true');
			} else {
				list.list.removeAttribute('aria-disabled');
			}
		}
		if (this.filter) {
			this.filter.disabled = disabled;
		}
	}

	/**
	 * Takes other texts, another threshold of the filter or none, makes the order matter or not, or shows
	 * the numbers of the rows or not.
	 *
	 * @param options the texts and whether the order matters; choices and selected left out keep
	 *        the palette's own
	 */
	configure(options: PaletteOptions = {}): void {
		const ordered = Boolean(this.options.ordered);
		this.options = { ...this.options, ...options };
		this.renderTexts();
		if (ordered !== Boolean(this.options.ordered)) {
			this.selected = this.clean(this.selected);
		}
		this.renderItems();
	}

	/**
	 * Takes other choices, those of the select after options were added, removed or renamed; the
	 * chosen ones no longer among them are dropped.
	 *
	 * @param choices the choices, left out for those of the select
	 */
	refresh(choices?: PaletteChoice[]): void {
		if (choices) {
			this.options.choices = choices;
		}
		this.choices = this.readChoices();
		let selected = this.selected;
		if (this.select) {
			// the chosen ones keep their order, those newly selected in the select follow them
			const checked = this.readSelected();
			const set = new Set(checked);
			selected = selected.filter((value) => set.has(value))
				.concat(checked.filter((value) => !selected.includes(value)));
		}
		this.selected = this.clean(selected);
		this.renderTexts();
		this.renderItems();
	}

	/**
	 * Shows other chosen ones, or those the select holds, and whether the palette is disabled.
	 *
	 * @param selected the values of the chosen ones, in their order; left out, those selected in
	 *        the select, in its order
	 */
	update(selected?: string[]): void {
		this.selected = this.clean(selected ?? this.readSelected());
		this.renderItems();
	}

	/**
	 * Disables the palette, or enables it again; one drawn over a select follows the select.
	 */
	setDisabled(disabled: boolean): void {
		this.options.disabled = disabled;
		this.updateState();
	}

	/**
	 * @returns the values of the chosen ones, in their order
	 */
	value(): string[] {
		return this.selected.slice();
	}

	/**
	 * Takes the palette away; one drawn over a select shows the native select again and takes
	 * away the wrapper it made.
	 */
	destroy(): void {
		this.detach();
		this.endDrag();
		Array.from(this.element.children).forEach((child) => {
			if (child !== this.select) {
				child.remove();
			}
		});
		this.element.classList.remove(CLASS, 'antilia-palette-ordered', 'antilia-palette-numbered',
			'antilia-palette-disabled');
		if (!this.element.classList.length) {
			this.element.removeAttribute('class');
		}
		if (instances.get(this.element) === this) {
			instances.delete(this.element);
		}
		const select = this.select;
		if (select) {
			select.classList.remove(NATIVE_CLASS);
			if (!select.classList.length) {
				select.removeAttribute('class');
			}
			select.removeAttribute('tabindex');
			select.removeAttribute('aria-hidden');
			if (this.ownElement && this.element.parentNode) {
				this.element.parentNode.insertBefore(select, this.element);
				this.element.remove();
			}
		}
	}

	private listen<E extends Event>(target: EventTarget, type: string,
		listener: (event: E) => void): void {
		target.addEventListener(type, listener as EventListener);
		this.listeners.push([target, type, listener as EventListener]);
	}

	/**
	 * Stops listening, leaving the elements in place.
	 */
	private detach(): void {
		this.listeners.forEach(([target, type, listener]) => {
			target.removeEventListener(type, listener);
		});
		this.listeners = [];
		for (const list of [this.available, this.chosen]) {
			list?.scrollbar?.destroy();
			if (list) {
				list.scrollbar = undefined;
			}
		}
		this.stopWatchingRemoval?.();
		this.stopWatchingRemoval = null;
	}

	private other(list: List): List {
		return list === this.available ? this.chosen : this.available;
	}

	private bind(): void {
		const itemOf = (event: Event) => (event.target as Element).closest<HTMLLIElement>(
			'.' + ITEM_CLASS);
		for (const list of [this.available, this.chosen]) {
			this.listen<MouseEvent>(list.list, 'click', (event) => {
				const item = itemOf(event);
				if (item && !this.isDisabled()) {
					this.pick(list, item.dataset.value!, event.shiftKey,
						event.ctrlKey || event.metaKey);
				}
			});
			this.listen<MouseEvent>(list.list, 'dblclick', (event) => {
				const item = itemOf(event);
				if (item && !this.isDisabled()) {
					this.transfer(list, [item.dataset.value!]);
				}
			});
			this.listen<KeyboardEvent>(list.list, 'keydown', (event) => this.onKey(list, event));
			this.listen<PointerEvent>(list.list, 'pointerdown', (event) => this.pointerDown(list,
				event));
			this.listen(list.list, 'scroll', () => this.fillView(list));
		}
		this.listen(this.buttons.add, 'click', () => this.transfer(this.available,
			Array.from(this.available.highlighted)));
		this.listen(this.buttons.addAll, 'click', () => this.transfer(this.available,
			this.values(this.available)));
		this.listen(this.buttons.remove, 'click', () => this.transfer(this.chosen,
			Array.from(this.chosen.highlighted)));
		this.listen(this.buttons.removeAll, 'click', () => this.transfer(this.chosen,
			this.selected.slice()));
		this.listen(this.buttons.up, 'click', () => this.moveHighlighted(false));
		this.listen(this.buttons.down, 'click', () => this.moveHighlighted(true));
		if (this.select) {
			// a click on the label of the hidden select focuses the available choices
			this.listen(this.select, 'focus', () => this.available.list.focus());
			// the select changed by someone else, for example a script resetting a form
			this.listen(this.select, 'change', () => this.update());
		}
		this.listen<PointerEvent>(document, 'pointermove', (event) => this.pointerMove(event));
		this.listen<PointerEvent>(document, 'pointerup', (event) => this.pointerUp(event));
		this.listen<PointerEvent>(document, 'pointercancel', () => this.endDrag());
	}

	/**
	 * Highlights an item, as a click on it does.
	 *
	 * @param list the list of the item
	 * @param value the value of the item
	 * @param range whether to highlight from the last one clicked to it, as with Shift
	 * @param toggle whether to toggle it, keeping the others, as with Ctrl
	 */
	pick(list: List, value: string, range = false, toggle = false): void {
		if (!this.movable(value)) {
			return;
		}
		if (range && list.anchor !== null) {
			const values = this.values(list);
			const from = values.indexOf(list.anchor);
			const to = values.indexOf(value);
			if (from >= 0 && to >= 0) {
				if (!toggle) {
					list.highlighted.clear();
				}
				values.slice(Math.min(from, to), Math.max(from, to) + 1)
					.filter((each) => this.movable(each))
					.forEach((each) => list.highlighted.add(each));
			}
		} else if (toggle) {
			if (!list.highlighted.delete(value)) {
				list.highlighted.add(value);
			}
			list.anchor = value;
		} else {
			list.highlighted.clear();
			list.highlighted.add(value);
			list.anchor = value;
		}
		list.active = value;
		this.paint(list);
		this.updateState();
	}

	/**
	 * Moves choices of a list to the other one: adds available ones or removes chosen ones.
	 *
	 * @param from the list they are in
	 * @param values their values; disabled ones stay where they are
	 * @param index where to put them among the chosen ones while the order matters, at the end
	 *        when left out
	 */
	transfer(from: List, values: string[], index?: number): void {
		// in the order they have in their list
		const wanted = new Set(values);
		const moving = this.values(from).filter((value) =>
			wanted.has(value) && this.movable(value));
		if (!moving.length || this.isDisabled()) {
			return;
		}
		const to = this.other(from);
		let selected: string[];
		if (from === this.available) {
			selected = insertAt(this.selected, moving, index ?? this.selected.length);
		} else {
			const removed = new Set(moving);
			selected = this.selected.filter((value) => !removed.has(value));
		}
		to.highlighted = new Set(moving);
		to.active = moving[0];
		to.anchor = moving[0];
		from.highlighted.clear();
		this.change(selected);
	}

	/**
	 * Moves the highlighted chosen ones one place up or down, while the order matters.
	 */
	moveHighlighted(down: boolean): void {
		if (!this.options.ordered || this.isDisabled()) {
			return;
		}
		const moved = new Set(Array.from(this.chosen.highlighted).filter((value) =>
			this.movable(value)));
		if (moved.size) {
			this.change(moveBy(this.selected, moved, down));
		}
	}

	/**
	 * Takes a new choice made by the user: shows it, writes it into the select and reports it.
	 */
	private change(selected: string[]): void {
		const before = this.selected.join('\n');
		this.selected = this.clean(selected);
		this.renderItems();
		this.scrollToActive(this.chosen);
		this.scrollToActive(this.available);
		if (this.selected.join('\n') === before) {
			return;
		}
		const select = this.select;
		if (select) {
			const chosen = new Set(this.selected);
			const options = Array.from(select.options);
			options.forEach((option) => {
				option.selected = chosen.has(option.value);
			});
			// a form posts the selected options in their order in the select
			if (this.options.ordered) {
				this.selected.forEach((value) => {
					const option = options.find((each) => each.value === value);
					if (option) {
						select.appendChild(option);
					}
				});
			}
			select.dispatchEvent(new Event('input', { bubbles: true }));
			select.dispatchEvent(new Event('change', { bubbles: true }));
		}
		this.options.onChange?.(this.value());
	}

	private scrollToActive(list: List): void {
		if (list.active !== null) {
			this.draw(list, this.values(list).indexOf(list.active) + 1);
		}
		const item = list.items.find((each) => each.dataset.value === list.active);
		if (!item) {
			return;
		}
		const box = list.list;
		if (item.offsetTop < box.scrollTop) {
			box.scrollTop = item.offsetTop;
		} else if (item.offsetTop + item.offsetHeight > box.scrollTop + box.clientHeight) {
			box.scrollTop = item.offsetTop + item.offsetHeight - box.clientHeight;
		}
	}

	private onKey(list: List, event: KeyboardEvent): void {
		if (this.isDisabled()) {
			return;
		}
		const values = this.values(list);
		const index = list.active !== null ? values.indexOf(list.active) : -1;
		const go = (next: number) => {
			const value = values[Math.max(0, Math.min(values.length - 1, next))];
			if (value === undefined) {
				return;
			}
			this.draw(list, values.indexOf(value) + 1);
			if (event.shiftKey) {
				if (list.anchor === null) {
					list.anchor = list.active ?? value;
				}
				this.pick(list, value, true, false);
			} else {
				list.active = value;
				this.paint(list);
			}
			this.scrollToActive(list);
		};
		switch (event.key) {
			case 'ArrowDown':
			case 'ArrowUp': {
				event.preventDefault();
				const down = event.key === 'ArrowDown';
				if (event.altKey && list === this.chosen) {
					this.moveHighlighted(down);
				} else {
					go(index < 0 ? 0 : index + (down ? 1 : -1));
				}
				break;
			}
			case 'Home':
				event.preventDefault();
				go(0);
				break;
			case 'End':
				event.preventDefault();
				go(values.length - 1);
				break;
			case ' ':
				event.preventDefault();
				if (list.active !== null) {
					this.pick(list, list.active, false, true);
				}
				break;
			case 'Enter': {
				event.preventDefault();
				const moving = list.highlighted.size ? Array.from(list.highlighted) :
					(list.active !== null ? [list.active] : []);
				this.transfer(list, moving);
				// the keyboard stays on the list, on the next item there
				const rest = this.values(list);
				list.active = rest[Math.min(Math.max(index, 0), rest.length - 1)] ?? null;
				this.paint(list);
				break;
			}
			case 'ArrowRight':
			case 'ArrowLeft':
				if ((event.key === 'ArrowRight') === (list === this.available)) {
					event.preventDefault();
					this.other(list).list.focus();
				}
				break;
			case 'a':
				if (event.ctrlKey || event.metaKey) {
					event.preventDefault();
					list.highlighted = new Set(values.filter((value) => this.movable(value)));
					this.paint(list);
					this.updateState();
				}
				break;
		}
	}

	private pointerDown(list: List, event: PointerEvent): void {
		const item = (event.target as Element).closest<HTMLLIElement>('.' + ITEM_CLASS);
		if (!item || event.button !== 0 || this.isDisabled() || !this.movable(item.dataset.value!)) {
			return;
		}
		const value = item.dataset.value!;
		// dragging a highlighted item drags all highlighted ones
		const values = list.highlighted.has(value) ?
			this.values(list).filter((each) => list.highlighted.has(each) && this.movable(each)) :
			[value];
		this.drag = { from: list.side, values, pointerId: event.pointerId, startX: event.clientX,
			startY: event.clientY, started: false };
	}

	private pointerMove(event: PointerEvent): void {
		const drag = this.drag;
		if (!drag || event.pointerId !== drag.pointerId) {
			return;
		}
		if (!drag.started) {
			if (Math.abs(event.clientX - drag.startX) < DRAG_THRESHOLD &&
				Math.abs(event.clientY - drag.startY) < DRAG_THRESHOLD) {
				return;
			}
			drag.started = true;
			this.element.classList.add(DRAGGING_CLASS);
			drag.ghost = create('div', this.css('palette-ghost'),
				drag.values.map((value) => this.choice(value)?.label ?? value).join(', '));
			drag.ghost.setAttribute('aria-hidden', 'true');
			drag.marker = create('div', this.css('palette-marker'));
			drag.marker.setAttribute('aria-hidden', 'true');
			document.body.appendChild(drag.ghost);
		}
		event.preventDefault();
		drag.ghost!.style.transform = 'translate(' + (event.clientX + 12) + 'px, ' +
			(event.clientY + 8) + 'px)';
		this.findDrop(drag, event.clientX, event.clientY);
	}

	/**
	 * Finds the list the pointer is over and, while the order matters, where among the chosen
	 * ones the dragged ones would land, and shows it.
	 */
	private findDrop(drag: Drag, x: number, y: number): void {
		drag.to = undefined;
		drag.index = undefined;
		drag.marker!.remove();
		for (const list of [this.available, this.chosen]) {
			const box = list.box.getBoundingClientRect();
			const over = x >= box.left && x <= box.right && y >= box.top && y <= box.bottom;
			list.box.classList.toggle(OVER_CLASS, over && (list.side !== drag.from ||
				(list === this.chosen && Boolean(this.options.ordered))));
			if (!over) {
				continue;
			}
			if (list === this.available) {
				drag.to = drag.from === 'selected' ? 'available' : undefined;
				continue;
			}
			if (!this.options.ordered) {
				drag.to = drag.from === 'available' ? 'selected' : undefined;
				continue;
			}
			drag.to = 'selected';
			let index = list.items.length;
			for (let i = 0; i < list.items.length; i++) {
				const rect = list.items[i].getBoundingClientRect();
				if (y < rect.top + rect.height / 2) {
					index = i;
					break;
				}
			}
			drag.index = index;
			const before = list.items[index];
			if (before) {
				list.list.insertBefore(drag.marker!, before);
			} else {
				list.list.appendChild(drag.marker!);
			}
		}
	}

	private pointerUp(event: PointerEvent): void {
		const drag = this.drag;
		if (!drag || event.pointerId !== drag.pointerId) {
			return;
		}
		this.endDrag();
		if (!drag.started || !drag.to) {
			return;
		}
		if (drag.from === drag.to) {
			if (drag.to === 'selected' && drag.index !== undefined) {
				this.chosen.highlighted = new Set(drag.values);
				this.change(insertAt(this.selected, drag.values, drag.index));
			}
		} else {
			this.transfer(drag.from === 'available' ? this.available : this.chosen, drag.values,
				drag.index);
		}
	}

	private endDrag(): void {
		const drag = this.drag;
		this.drag = null;
		if (!drag) {
			return;
		}
		drag.ghost?.remove();
		drag.marker?.remove();
		this.element.classList.remove(DRAGGING_CLASS);
		this.available?.box.classList.remove(OVER_CLASS);
		this.chosen?.box.classList.remove(OVER_CLASS);
	}
}

/**
 * Draws the palette of a select multiple, in the shape of a Svelte action: use:palette={options}.
 *
 * @param select the select
 * @param options the texts and whether the order matters
 * @returns update() taking other options, destroy() taking the palette away
 */
export function palette(select: HTMLSelectElement, options?: PaletteOptions): {
	update(options?: PaletteOptions): void;
	destroy(): void;
} {
	const instance = Palette.attach(select, options)!;
	return {
		update: (next) => instance.configure(next),
		destroy: () => instance.destroy()
	};
}
