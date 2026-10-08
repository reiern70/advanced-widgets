/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * An autocomplete drawn next to an <input type="hidden">, which holds the ids of the chosen values
 * as a JSON array and fires 'input' and 'change' when they change.
 *
 * new Autocomplete(input, options) puts the input into a div.wicket-autocomplete, or uses the one
 * it is in already, with the chips of the chosen values, a text field and the list of
 * suggestions. Typing asks options.search, or else options.url, for the suggestions matching the
 * text ([{id, label, data}]); each row is options.template evaluated against its data by the
 * engine options.engine of templates(), {{@label}} being its label. Attaching an input that is in
 * a wrapper already, for example one Ajax put in place of the previous one, rebuilds the rest.
 */

import { Chips } from './chips.js';
import { getHost, onRemoving, type Unsubscribe } from './host.js';
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
 * Where the suggestions come from, how they are shown, and the texts.
 */
export interface AutocompleteOptions {
	/** The URL answering ?term= with the suggestions as JSON, unless search is given. */
	url?: string;
	/** Finds the suggestions for a text, in place of url. */
	search?: (term: string) => Promise<AutocompleteSuggestion[]>;
	/** Whether many values can be chosen. */
	multiple?: boolean;
	/** The template of a suggestion's row, '{{@label}}' by default. */
	template?: string;
	/** The name of the template engine, the built-in one by default. */
	engine?: string;
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

const WRAPPER_CLASS = 'wicket-autocomplete';

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

	chosen: AutocompleteValue[];

	wrapper!: HTMLElement;

	id!: string;

	field!: HTMLDivElement;

	chips!: HTMLSpanElement;

	text!: HTMLInputElement;

	menu!: HTMLDivElement;

	list!: HTMLUListElement;

	status!: HTMLDivElement;

	suggestions: AutocompleteSuggestion[] = [];

	active: HTMLElement | null = null;

	private row!: CompiledTemplate;

	private request = 0;

	private timer: number | undefined = undefined;

	private ownWrapper = false;

	private listeners: Array<[EventTarget, string, EventListener]> = [];

	private stopWatchingRemoval: Unsubscribe | null = null;

	/**
	 * @param input the hidden input
	 * @param options where the suggestions come from, how they are shown, and the texts
	 */
	constructor(input: HTMLInputElement, options?: AutocompleteOptions) {
		this.input = input;
		this.options = { ...options };
		this.multiple = !!this.options.multiple;
		this.chosen = (this.options.chosen || []).slice();
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

	private attach(): void {
		const input = this.input;
		let wrapper = input.parentElement;
		if (!wrapper || !wrapper.classList.contains(WRAPPER_CLASS)) {
			wrapper = create('div', WRAPPER_CLASS);
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
		this.id = input.id || ('wicket-autocomplete-' + (++counter));
		wrapper.classList.toggle('wicket-autocomplete-multiple', this.multiple);

		this.field = create('div', 'wicket-autocomplete-field');
		this.chips = create('span', 'wicket-autocomplete-chips');
		this.field.appendChild(this.chips);
		this.text = create('input', 'wicket-autocomplete-text');
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

		this.menu = create('div', 'wicket-autocomplete-menu');
		this.menu.hidden = true;
		this.list = create('ul', 'wicket-autocomplete-list');
		this.list.id = this.id + '-list';
		this.list.setAttribute('role', 'listbox');
		this.menu.appendChild(this.list);
		this.status = create('div', 'wicket-autocomplete-status');
		this.status.hidden = true;
		this.menu.appendChild(this.status);
		wrapper.appendChild(this.menu);

		this.configure(this.options);
		this.bind();
		this.stopWatchingRemoval = onRemoving(wrapper, () => this.detach());
	}

	/**
	 * Takes other options, keeping the values chosen; options.chosen is left out.
	 *
	 * @param options where the suggestions come from, how they are shown, and the texts
	 */
	configure(options?: AutocompleteOptions): void {
		this.options = { ...options, multiple: this.multiple };
		const engine = templates().engine(this.options.engine);
		this.row = engine ? engine.compile(this.options.template || '{{@label}}') :
			(_data, meta) => templates().escapeHtml(meta?.label);
		this.wrapper.classList.toggle('wicket-autocomplete-disabled', !!this.options.disabled);
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
		this.wrapper.classList.remove('wicket-autocomplete-multiple', 'wicket-autocomplete-disabled',
			'wicket-autocomplete-open');
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
			const chip = Chips.create(value.label, removeLabel);
			chip.dataset.id = String(value.id);
			this.chips.appendChild(chip);
		});
		// one value: the chip stands in for the text field until it is removed
		const full = !this.multiple && this.chosen.length > 0;
		this.text.hidden = full;
		this.text.placeholder = this.chosen.length ? '' : (this.options.placeholder || '');
		// empty when nothing is chosen, so that the server tells a required one is missing
		this.input.value = this.chosen.length ?
			JSON.stringify(this.chosen.map((value) => value.id)) : '';
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
			'.wicket-autocomplete-option');
		this.listen(this.list, 'click', (event) => {
			const item = itemOf(event);
			if (item) {
				this.choose(Number(item.dataset.index));
			}
		});
		this.listen(this.list, 'mousemove', (event) => {
			const item = itemOf(event);
			if (item && item !== this.active) {
				this.activate(item, false);
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
		this.list.textContent = '';
		this.active = null;
		this.suggestions.forEach((suggestion, index) => {
			const item = create('li', 'wicket-autocomplete-option');
			item.id = this.id + '-option-' + index;
			item.dataset.index = String(index);
			item.setAttribute('role', 'option');
			item.innerHTML = this.row(suggestion.data || {}, {
				id: suggestion.id,
				label: suggestion.label,
				index
			});
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
		this.wrapper.classList.add('wicket-autocomplete-open');
		this.text.setAttribute('aria-expanded', 'true');
	}

	close(): void {
		window.clearTimeout(this.timer);
		this.request++;
		this.menu.hidden = true;
		this.wrapper.classList.remove('wicket-autocomplete-open');
		this.text.setAttribute('aria-expanded', 'false');
		this.text.removeAttribute('aria-activedescendant');
		this.active = null;
	}

	private activate(item: HTMLElement | null, scroll: boolean): void {
		this.active?.classList.remove('wicket-autocomplete-active');
		this.active = item;
		if (!item) {
			this.text.removeAttribute('aria-activedescendant');
			return;
		}
		item.classList.add('wicket-autocomplete-active');
		this.text.setAttribute('aria-activedescendant', item.id);
		if (scroll) {
			item.scrollIntoView?.({ block: 'nearest' });
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
			case 'Escape':
				if (!this.menu.hidden) {
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
	 * adds it.
	 */
	choose(index: number): void {
		const suggestion = this.suggestions[index];
		if (!suggestion) {
			return;
		}
		const value = { id: suggestion.id, label: suggestion.label };
		this.chosen = this.multiple ? this.chosen.concat([value]) : [value];
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
