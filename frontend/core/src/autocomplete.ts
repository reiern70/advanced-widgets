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
 * An autocomplete drawn next to an <input type="hidden">, which holds the ids of the chosen values
 * as a JSON array and fires 'input' and 'change' when they change.
 *
 * new Autocomplete(input, options) puts the input into a div.antilia-autocomplete, or uses the one
 * it is in already, with the chips of the chosen values, a text field and the list of
 * suggestions. Typing asks options.search, or else options.url, for the suggestions matching the
 * text ([{id, label, data}]); each row is options.template evaluated against its data by the
 * engine options.engine of templates(), {{@label}} being its label. Attaching an input that is in
 * a wrapper already, for example one Ajax put in place of the previous one, rebuilds the rest.
 *
 * options.actions are buttons at the end of every row, such as an (i): their content is a template
 * evaluated against the row's data as the row is. One with a tooltip shows it, a template too,
 * while hovered, and F1 toggles the tooltip of the active row; the others are reported to
 * options.onAction and as an 'antilia-autocomplete-action' event of the input, whose detail is
 * {action, id, label}. Up and Down move from row to row, Enter chooses the active one.
 *
 * The templates are the developer's and written into the page as they are; the values are escaped
 * by {{ }}, never by {{{ }}}, so that only values that are markup on purpose go through that.
 *
 * With options.pick it is a find bar: a choice is not kept as a chip, the text field is emptied
 * for the next one, and the input holds the id of the value last picked ([id]) when it fires
 * 'change', for the page to add it to a list of its own.
 */

import { Chips } from './chips.js';
import { getHost, onRemoving, type Unsubscribe } from './host.js';
import { lookOf, partClasses, toggleParts, type Look, type LookPart } from './look.js';
import { Scrollbar, type ScrollbarHandle } from './scrollbar.js';
import { templates, type CompiledTemplate } from './templates.js';

/** A chosen value. */
export interface AutocompleteValue {
	id: string | number;
	label: string;
}

/** A suggestion: a value, and the data its row is rendered from. */
export interface AutocompleteSuggestion extends AutocompleteValue {
	data?: unknown;
}

/**
 * A button at the end of every row of the suggestions.
 */
export interface AutocompleteAction {
	/** The name the action is reported with. */
	name: string;
	/** The content of the button, evaluated against the row's data as the row is: an icon. */
	template: string;
	/** What the button does, for screen readers and as its title; '{0}' stands for the label. */
	label?: string;
	/**
	 * The template of the tooltip of the button, evaluated against the row's data: the button
	 * shows it instead of being reported, while hovered or clicked, and F1 toggles it for the
	 * active row.
	 */
	tooltip?: string;
}

/** The detail of the 'antilia-autocomplete-action' event of the input. */
export interface AutocompleteActionDetail extends AutocompleteValue {
	action: string;
}

/** The event the input fires when an action of a row is clicked. */
export const ACTION_EVENT = 'antilia-autocomplete-action';

/**
 * Where the suggestions come from, how they are shown, and the texts.
 */
export interface AutocompleteOptions {
	/** The URL answering ?term= with the suggestions as JSON, unless search is given. */
	url?: string;
	/** The look of the autocomplete; that of the element it is in when left out (see lookOf). */
	look?: Look;
	/** Finds the suggestions for a text, in place of url. */
	search?: (term: string) => Promise<AutocompleteSuggestion[]>;
	/** Whether many values can be chosen. */
	multiple?: boolean;
	/**
	 * Whether a choice is only reported, not kept: the input holds the id of the value picked and
	 * fires 'change', and the text field is empty again, as in a find bar.
	 */
	pick?: boolean;
	/** The template of a suggestion's row, '{{@label}}' by default. */
	template?: string;
	/** The name of the template engine, the built-in one by default. */
	engine?: string;
	/** The buttons at the end of every row. */
	actions?: AutocompleteAction[];
	/** Called when an action without a tooltip is clicked, with the suggestion of its row. */
	onAction?: (action: string, suggestion: AutocompleteSuggestion) => void;
	/** The number of characters typed from which on suggestions are asked for. */
	minLength?: number;
	disabled?: boolean;
	/** The values chosen to begin with. */
	chosen?: AutocompleteValue[];
	placeholder?: string;
	noMatch?: string;
	searching?: string;
	/** The label of the x of a chip, '{0}' standing for its text: 'Remove {0}'. */
	remove?: string;
}

const WRAPPER_CLASS = 'antilia-autocomplete';

/** How long typing pauses before the suggestions are asked for, in milliseconds. */
const DELAY = 200;

/** The autocompletes by their wrapper. */
const instances = new WeakMap<Element, Autocomplete>();

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

/**
 * The autocomplete of a hidden input.
 */
export class Autocomplete {
	readonly input: HTMLInputElement;

	options: AutocompleteOptions;

	readonly multiple: boolean;

	/** Whether a choice is only reported, not kept. */
	readonly pick: boolean;

	chosen: AutocompleteValue[];

	/** The value picked last, when choices are only reported. */
	picked: AutocompleteValue | null = null;

	wrapper!: HTMLElement;

	id!: string;

	field!: HTMLDivElement;

	chips!: HTMLSpanElement;

	text!: HTMLInputElement;

	menu!: HTMLDivElement;

	list!: HTMLUListElement;

	status!: HTMLDivElement;

	/** The tooltip of an action of a row. */
	tooltip!: HTMLDivElement;

	suggestions: AutocompleteSuggestion[] = [];

	active: HTMLElement | null = null;

	private row!: CompiledTemplate;

	private actions: Array<{ action: AutocompleteAction; content: CompiledTemplate;
		tooltip: CompiledTemplate | null }> = [];

	/** The row whose tooltip shows, and whether F1 or a click keeps it there. */
	private tipped: { item: HTMLElement; pinned: boolean } | null = null;

	private request = 0;

	private timer: number | undefined = undefined;

	private ownWrapper = false;

	/** The look of the autocomplete, whose classes the elements it makes get. */
	private look!: Look;

	private listeners: Array<[EventTarget, string, EventListener]> = [];

	private stopWatchingRemoval: Unsubscribe | null = null;

	private scrollbar: ScrollbarHandle | undefined = undefined;

	/** Gives the list its scroll bars; Wicket gives those of Wicket.Scrollbar, if any. */
	static attachScrollbar: (list: HTMLElement, look?: Look) => ScrollbarHandle | undefined =
		(list, look) => Scrollbar.attach(list, { axis: 'y', look });

	/**
	 * @param input the hidden input
	 * @param options where the suggestions come from, how they are shown, and the texts
	 */
	constructor(input: HTMLInputElement, options?: AutocompleteOptions) {
		this.input = input;
		this.options = { ...options };
		this.pick = !!this.options.pick;
		this.multiple = !!this.options.multiple && !this.pick;
		this.chosen = this.pick ? [] : (this.options.chosen || []).slice();
		this.attach();
	}

	/**
	 * Draws the autocomplete of a hidden input.
	 *
	 * @param input the input or its id
	 * @param options where the suggestions come from, how they are shown, and the texts
	 * @returns the autocomplete, null when there is no such input
	 */
	static attach(input: HTMLInputElement | string, options?: AutocompleteOptions):
		Autocomplete | null {
		const element = typeof input === 'string' ? document.getElementById(input) : input;
		return element instanceof HTMLInputElement ? new Autocomplete(element, options) : null;
	}

	/**
	 * @param input the input or its id
	 * @returns the autocomplete of an input, if it has one
	 */
	static of(input: HTMLInputElement | string | null | undefined): Autocomplete | null {
		const element = typeof input === 'string' ? document.getElementById(input) : input;
		const wrapper = element?.parentElement;
		const autocomplete = wrapper ? instances.get(wrapper) : undefined;
		return autocomplete && autocomplete.input === element ? autocomplete : null;
	}

	/**
	 * @returns the classes of parts of the autocomplete in its look
	 */
	private css(...parts: LookPart[]): string {
		return partClasses(this.look, ...parts);
	}

	private attach(): void {
		const input = this.input;
		this.look = this.options.look ?? lookOf(input);
		let wrapper = input.parentElement;
		if (!wrapper || !wrapper.classList.contains(WRAPPER_CLASS)) {
			wrapper = create('div', this.css('autocomplete'));
			input.parentNode?.insertBefore(wrapper, input);
			wrapper.appendChild(input);
			this.ownWrapper = true;
		}
		instances.get(wrapper)?.detach();
		Array.from(wrapper.children).forEach((child) => {
			if (child !== input) {
				child.remove();
			}
		});
		instances.set(wrapper, this);
		this.wrapper = wrapper;
		this.id = input.id || ('antilia-autocomplete-' + (++counter));
		wrapper.classList.toggle('antilia-autocomplete-multiple', this.multiple);
		wrapper.classList.toggle('antilia-autocomplete-pick', this.pick);

		this.field = create('div', this.css('autocomplete-field'));
		this.chips = create('span', this.css('autocomplete-chips'));
		this.field.appendChild(this.chips);
		this.text = create('input', this.css('autocomplete-text'));
		this.text.type = 'text';
		this.text.autocomplete = 'off';
		this.text.setAttribute('role', 'combobox');
		this.text.setAttribute('aria-autocomplete', 'list');
		this.text.setAttribute('aria-expanded', 'false');
		this.text.setAttribute('aria-controls', this.id + '-list');
		const label = this.labelText();
		if (label) {
			this.text.setAttribute('aria-label', label);
		}
		this.field.appendChild(this.text);
		wrapper.appendChild(this.field);

		this.menu = create('div', this.css('autocomplete-menu'));
		this.menu.hidden = true;
		this.list = create('ul', this.css('autocomplete-list'));
		this.list.id = this.id + '-list';
		this.list.setAttribute('role', 'listbox');
		// the list scrolls in the wrapper of the slim scroll bars of the core
		const listBox = create('div', this.css('scrollbar'));
		listBox.appendChild(this.list);
		this.menu.appendChild(listBox);
		this.status = create('div', this.css('autocomplete-status'));
		this.status.hidden = true;
		this.menu.appendChild(this.status);
		this.tooltip = create('div', this.css('autocomplete-tooltip'));
		this.tooltip.id = this.id + '-tooltip';
		this.tooltip.setAttribute('role', 'tooltip');
		this.tooltip.hidden = true;
		this.menu.appendChild(this.tooltip);
		wrapper.appendChild(this.menu);

		this.configure(this.options);
		this.bind();
		if (this.list.isConnected) {
			this.scrollbar = Autocomplete.attachScrollbar(this.list, this.look);
		}
		this.stopWatchingRemoval = onRemoving(wrapper, () => this.detach());
	}

	/**
	 * Takes other options, keeping the values chosen; options.chosen is left out.
	 *
	 * @param options where the suggestions come from, how they are shown, and the texts
	 */
	configure(options?: AutocompleteOptions): void {
		this.options = { ...options, multiple: this.multiple, pick: this.pick };
		const engine = templates().engine(this.options.engine);
		const compile = (template: string): CompiledTemplate => engine ?
			engine.compile(template) : () => '';
		this.row = engine ? engine.compile(this.options.template || '{{@label}}') :
			(_data, meta) => templates().escapeHtml(meta?.label);
		this.actions = (this.options.actions || []).map((action) => ({
			action,
			content: compile(action.template),
			tooltip: action.tooltip ? compile(action.tooltip) : null
		}));
		this.wrapper.classList.toggle('antilia-autocomplete-disabled', !!this.options.disabled);
		this.text.disabled = !!this.options.disabled;
		this.close();
		this.renderChosen();
	}

	/**
	 * Shows other chosen values, without firing a change: for a framework passing them down.
	 *
	 * @param chosen the chosen values
	 */
	setChosen(chosen: AutocompleteValue[]): void {
		if (this.pick) {
			return;
		}
		this.chosen = (chosen || []).slice();
		this.renderChosen();
	}

	/**
	 * Takes the autocomplete away and leaves the input alone; a wrapper the autocomplete made is
	 * taken away too.
	 */
	destroy(): void {
		this.detach();
		this.field.remove();
		this.menu.remove();
		this.wrapper.classList.remove('antilia-autocomplete-multiple', 'antilia-autocomplete-pick',
			'antilia-autocomplete-disabled', 'antilia-autocomplete-open');
		if (instances.get(this.wrapper) === this) {
			instances.delete(this.wrapper);
		}
		if (this.ownWrapper && this.wrapper.parentNode) {
			this.wrapper.parentNode.insertBefore(this.input, this.wrapper);
			this.wrapper.remove();
		}
	}

	/**
	 * @returns the text of the labels of the input, without the autocomplete itself; a hidden
	 *          input has no labels of its own, so those naming its id are taken
	 */
	labelText(): string {
		const texts: string[] = [];
		const labels = this.input.labels?.length ? Array.from(this.input.labels) :
			this.input.id ? Array.from(document.querySelectorAll<HTMLLabelElement>('label[for="' +
				CSS.escape(this.input.id) + '"]')) : [];
		labels.forEach((label) => {
			const copy = label.cloneNode(true) as HTMLElement;
			copy.querySelectorAll('.' + WRAPPER_CLASS).forEach((element) => element.remove());
			const text = (copy.textContent ?? '').replace(/\s+/g, ' ').trim();
			if (text) {
				texts.push(text);
			}
		});
		return texts.join(' ');
	}

	/**
	 * Shows the chosen values as chips and the text field, writes their ids into the input.
	 */
	private renderChosen(): void {
		this.chips.textContent = '';
		const removeLabel = this.options.disabled ? null : (this.options.remove || 'Remove {0}');
		this.chosen.forEach((value) => {
			const chip = Chips.create(value.label, removeLabel, this.look);
			chip.dataset.id = String(value.id);
			this.chips.appendChild(chip);
		});
		// one value: the chip stands in for the text field until it is removed
		const full = !this.multiple && this.chosen.length > 0;
		this.text.hidden = full;
		this.text.placeholder = this.chosen.length ? '' : (this.options.placeholder || '');
		// empty when nothing is chosen, so that the server tells a required one is missing
		const ids = this.pick ? (this.picked ? [this.picked.id] : []) :
			this.chosen.map((value) => value.id);
		this.input.value = ids.length ? JSON.stringify(ids) : '';
	}

	private listen<E extends Event>(target: EventTarget, type: string,
		listener: (event: E) => void): void {
		target.addEventListener(type, listener as EventListener);
		this.listeners.push([target, type, listener as EventListener]);
	}

	private bind(): void {
		this.listen(this.field, 'click', (event) => {
			const chip = Chips.removeTarget(event);
			if (chip) {
				this.remove(chip.dataset.id ?? '');
				return;
			}
			if (!this.text.hidden) {
				this.text.focus();
			}
		});
		this.listen(this.text, 'input', () => this.schedule());
		this.listen<KeyboardEvent>(this.text, 'keydown', (event) => this.onKey(event));
		// keeps the focus in the text field
		this.listen(this.list, 'mousedown', (event) => event.preventDefault());
		const itemOf = (event: Event) => (event.target as Element).closest<HTMLElement>(
			'.antilia-autocomplete-option');
		const buttonOf = (event: Event) => (event.target as Element).closest<HTMLElement>(
			'.antilia-autocomplete-action');
		this.listen(this.list, 'click', (event) => {
			const item = itemOf(event);
			if (!item) {
				return;
			}
			const button = buttonOf(event);
			if (button) {
				this.act(item, Number(button.dataset.action));
			} else {
				this.choose(Number(item.dataset.index));
			}
		});
		this.listen(this.list, 'mousemove', (event) => {
			const item = itemOf(event);
			if (item && item !== this.active) {
				this.activate(item, false);
			}
		});
		this.listen(this.list, 'mouseover', (event) => {
			const button = buttonOf(event);
			if (button?.classList.contains('antilia-autocomplete-info') && !this.tipped?.pinned) {
				this.showTooltip(itemOf(event)!, false);
			}
		});
		this.listen(this.list, 'mouseout', (event) => {
			const button = buttonOf(event);
			const to = (event as MouseEvent).relatedTarget as Node | null;
			if (button && !(to && button.contains(to)) && this.tipped && !this.tipped.pinned) {
				this.hideTooltip();
			}
		});
		// the list scrolls away from the row of the tooltip
		this.listen(this.list, 'scroll', () => {
			if (this.tipped) {
				this.placeTooltip(this.tipped.item);
			}
		});
		// a click on the label of the hidden input focuses the text field
		this.listen(this.input, 'focus', () => this.text.focus());
		const outside = (event: Event) => {
			if (!this.wrapper.contains(event.target as Node)) {
				this.close();
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
		window.clearTimeout(this.timer);
		this.scrollbar?.destroy();
		this.scrollbar = undefined;
		this.stopWatchingRemoval?.();
		this.stopWatchingRemoval = null;
	}

	/**
	 * Asks for the suggestions once typing paused.
	 */
	private schedule(): void {
		window.clearTimeout(this.timer);
		const term = this.text.value.trim();
		if (term.length < (this.options.minLength || 0)) {
			this.close();
			return;
		}
		this.timer = window.setTimeout(() => this.search(term), DELAY);
	}

	private find(term: string): Promise<AutocompleteSuggestion[]> {
		if (this.options.search) {
			return this.options.search(term);
		}
		const url = this.options.url ?? '';
		return window.fetch(url + (url.indexOf('?') >= 0 ? '&' : '?') + 'term=' +
			encodeURIComponent(term), {
			credentials: 'same-origin',
			headers: { Accept: 'application/json' }
		}).then((response) => {
			if (!response.ok) {
				throw new Error('HTTP ' + response.status);
			}
			return response.json() as Promise<AutocompleteSuggestion[]>;
		});
	}

	/**
	 * Asks for the suggestions matching a text and shows them.
	 */
	search(term: string): Promise<void> {
		const request = ++this.request;
		this.showStatus(this.options.searching || 'Searching…');
		return this.find(term).then((suggestions) => {
			// only the answer to the latest request counts
			if (request === this.request && this.text.value.trim() === term) {
				this.show(suggestions);
			}
		}).catch((error) => {
			getHost().log.error('Wicket.Autocomplete: searching "' + term + '" failed', error);
			this.close();
		});
	}

	/**
	 * Lists the suggestions not chosen yet.
	 */
	show(suggestions: AutocompleteSuggestion[]): void {
		const chosenIds = this.chosen.map((value) => String(value.id));
		this.suggestions = (suggestions || []).filter((suggestion) =>
			chosenIds.indexOf(String(suggestion.id)) < 0);
		this.hideTooltip();
		this.list.textContent = '';
		this.active = null;
		this.suggestions.forEach((suggestion, index) => {
			const item = create('li', this.css('autocomplete-option'));
			item.id = this.id + '-option-' + index;
			item.dataset.index = String(index);
			item.setAttribute('role', 'option');
			const data = suggestion.data || {};
			const meta = { id: suggestion.id, label: suggestion.label, index };
			if (this.actions.length) {
				item.classList.add('antilia-autocomplete-has-actions');
				const content = create('span', this.css('autocomplete-content'));
				content.innerHTML = this.row(data, meta);
				item.appendChild(content);
				item.appendChild(this.renderActions(data, meta));
			} else {
				item.innerHTML = this.row(data, meta);
			}
			this.list.appendChild(item);
		});
		if (!this.suggestions.length) {
			this.showStatus(this.options.noMatch || 'No matches');
			return;
		}
		this.status.hidden = true;
		this.open();
		this.activate(this.list.firstElementChild as HTMLElement, true);
	}

	/**
	 * @returns the buttons of the actions of a row
	 */
	private renderActions(data: unknown, meta: Record<string, unknown>): HTMLSpanElement {
		const buttons = create('span', this.css('autocomplete-actions'));
		this.actions.forEach(({ action, content }, index) => {
			const button = create('button', this.css('autocomplete-action'));
			button.type = 'button';
			// the focus stays in the text field: the keys are those of the text field
			button.tabIndex = -1;
			button.dataset.action = String(index);
			button.dataset.name = action.name;
			if (action.tooltip) {
				button.classList.add('antilia-autocomplete-info');
			}
			if (action.label) {
				const label = action.label.replace('{0}', String(meta.label));
				button.setAttribute('aria-label', label);
				button.title = label;
			}
			button.innerHTML = content(data, meta);
			buttons.appendChild(button);
		});
		return buttons;
	}

	/**
	 * Clicks an action of a row: shows its tooltip, or reports it.
	 *
	 * @param item the row
	 * @param index the index of the action
	 */
	private act(item: HTMLElement, index: number): void {
		const entry = this.actions[index];
		const suggestion = this.suggestions[Number(item.dataset.index)];
		if (!entry || !suggestion) {
			return;
		}
		if (entry.tooltip) {
			if (this.tipped?.item === item && this.tipped.pinned) {
				this.hideTooltip();
			} else {
				this.showTooltip(item, true);
			}
			return;
		}
		this.options.onAction?.(entry.action.name, suggestion);
		this.input.dispatchEvent(new CustomEvent<AutocompleteActionDetail>(ACTION_EVENT, {
			bubbles: true,
			detail: { action: entry.action.name, id: suggestion.id, label: suggestion.label }
		}));
	}

	/**
	 * @returns whether the tooltip of a row shows
	 */
	isTooltipShown(): boolean {
		return !this.tooltip.hidden;
	}

	/**
	 * Shows the tooltip of the first action of a row having one, next to the row.
	 *
	 * @param item the row
	 * @param pinned whether it stays when the mouse leaves its button
	 */
	private showTooltip(item: HTMLElement, pinned: boolean): void {
		const index = this.actions.findIndex((entry) => entry.tooltip);
		const suggestion = this.suggestions[Number(item.dataset.index)];
		if (index < 0 || !suggestion) {
			this.hideTooltip();
			return;
		}
		if (this.tipped?.item !== item) {
			this.tipped?.item.removeAttribute('aria-describedby');
			this.tooltip.innerHTML = this.actions[index].tooltip!(suggestion.data || {}, {
				id: suggestion.id,
				label: suggestion.label,
				index: Number(item.dataset.index)
			});
		}
		this.tipped = { item, pinned };
		item.setAttribute('aria-describedby', this.tooltip.id);
		this.tooltip.hidden = false;
		this.placeTooltip(item);
	}

	private hideTooltip(): void {
		this.tipped?.item.removeAttribute('aria-describedby');
		this.tipped = null;
		this.tooltip.hidden = true;
		this.tooltip.textContent = '';
	}

	/**
	 * Puts the tooltip beside the menu at the height of a row: right of it where there is room,
	 * else left of it, else below the row.
	 */
	private placeTooltip(item: HTMLElement): void {
		const style = this.tooltip.style;
		style.left = style.right = style.top = '';
		this.tooltip.classList.remove('antilia-autocomplete-tooltip-below');
		const menu = this.menu.getBoundingClientRect();
		const row = item.getBoundingClientRect();
		const width = this.tooltip.offsetWidth;
		const gap = 6;
		const top = Math.max(0, row.top - menu.top);
		if (menu.right + gap + width <= document.documentElement.clientWidth) {
			style.left = (menu.width + gap) + 'px';
			style.top = top + 'px';
		} else if (menu.left - gap - width >= 0) {
			style.right = (menu.width + gap) + 'px';
			style.top = top + 'px';
		} else {
			this.tooltip.classList.add('antilia-autocomplete-tooltip-below');
			style.top = (row.bottom - menu.top + gap) + 'px';
		}
	}

	private showStatus(text: string): void {
		this.status.textContent = text;
		this.status.hidden = false;
		this.open();
	}

	isOpen(): boolean {
		return !this.menu.hidden;
	}

	private open(): void {
		this.menu.hidden = false;
		this.wrapper.classList.add('antilia-autocomplete-open');
		this.text.setAttribute('aria-expanded', 'true');
	}

	close(): void {
		window.clearTimeout(this.timer);
		this.request++;
		this.menu.hidden = true;
		this.wrapper.classList.remove('antilia-autocomplete-open');
		this.text.setAttribute('aria-expanded', 'false');
		this.text.removeAttribute('aria-activedescendant');
		this.hideTooltip();
		this.active = null;
	}

	private activate(item: HTMLElement | null, scroll: boolean): void {
		if (this.active) {
			toggleParts(this.active, this.look, false, 'autocomplete-option-active');
		}
		this.active = item;
		if (!item) {
			this.text.removeAttribute('aria-activedescendant');
			return;
		}
		toggleParts(item, this.look, true, 'autocomplete-option-active');
		this.text.setAttribute('aria-activedescendant', item.id);
		if (scroll) {
			item.scrollIntoView?.({ block: 'nearest' });
		}
		// the tooltip F1 showed follows the active row
		if (this.tipped && this.tipped.item !== item) {
			if (this.tipped.pinned) {
				this.showTooltip(item, true);
			} else {
				this.hideTooltip();
			}
		}
	}

	private onKey(event: KeyboardEvent): void {
		const items = Array.from(this.list.children) as HTMLElement[];
		const index = this.active ? items.indexOf(this.active) : -1;
		const open = !this.menu.hidden && items.length > 0;
		switch (event.key) {
			case 'ArrowDown':
				event.preventDefault();
				if (open) {
					this.activate(items[Math.min(index + 1, items.length - 1)], true);
				} else {
					this.schedule();
				}
				break;
			case 'ArrowUp':
				if (open) {
					event.preventDefault();
					this.activate(items[Math.max(index - 1, 0)], true);
				}
				break;
			case 'Enter':
				// never sends the form: the text is not a value until it is chosen
				event.preventDefault();
				if (open && this.active) {
					this.choose(Number(this.active.dataset.index));
				}
				break;
			case 'F1':
				// shows the tooltip of the active row, not the help of the browser
				if (open && this.active && this.actions.some((entry) => entry.tooltip)) {
					event.preventDefault();
					if (this.tipped?.item === this.active && this.tipped.pinned) {
						this.hideTooltip();
					} else {
						this.showTooltip(this.active, true);
					}
				}
				break;
			case 'Escape':
				if (this.tipped) {
					// the tooltip first, the list then
					event.preventDefault();
					event.stopPropagation();
					this.hideTooltip();
				} else if (!this.menu.hidden) {
					event.preventDefault();
					event.stopPropagation();
					this.close();
				}
				break;
			case 'Backspace':
				// in the empty field, removes the last chosen value
				if (this.text.value === '' && this.chosen.length) {
					event.preventDefault();
					this.remove(this.chosen[this.chosen.length - 1].id);
				}
				break;
			case 'Tab':
				this.close();
				break;
		}
	}

	/**
	 * Chooses a suggestion: a single autocomplete takes it instead of its value, a multiple one
	 * adds it, and one only reporting choices tells it and is empty again.
	 */
	choose(index: number): void {
		const suggestion = this.suggestions[index];
		if (!suggestion) {
			return;
		}
		const value = { id: suggestion.id, label: suggestion.label };
		if (this.pick) {
			this.picked = value;
		} else {
			this.chosen = this.multiple ? this.chosen.concat([value]) : [value];
		}
		this.text.value = '';
		this.close();
		this.renderChosen();
		this.changed();
		if (!this.text.hidden) {
			this.text.focus();
		}
	}

	/**
	 * Removes a chosen value, as the x of its chip does.
	 */
	remove(id: string | number): void {
		if (this.options.disabled) {
			return;
		}
		const before = this.chosen.length;
		this.chosen = this.chosen.filter((value) => String(value.id) !== String(id));
		if (this.chosen.length === before) {
			return;
		}
		this.renderChosen();
		this.changed();
		this.text.focus();
	}

	private changed(): void {
		this.input.dispatchEvent(new Event('input', { bubbles: true }));
		this.input.dispatchEvent(new Event('change', { bubbles: true }));
	}
}
