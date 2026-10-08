/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * An autocomplete drawn next to an <input type="hidden">, which holds the ids of the chosen values
 * as a JSON array and fires 'change' when they change.
 *
 * Wicket.Autocomplete.attach(inputOrId, options) puts the input into a div.wicket-autocomplete with
 * the chips of the chosen values, a text field and the list of suggestions. Typing asks options.url
 * for the suggestions matching the text ([{id, label, data}]); each row is options.template
 * evaluated against its data by the engine options.engine of Wicket.Templates, {{@label}} being
 * its label. Attaching an input that is in a wrapper already, for example one Ajax put in place of
 * the previous one, rebuilds the rest.
 *
 * Options: url, multiple, template, engine, minLength, disabled, chosen [{id, label}], and the
 * texts placeholder, noMatch, searching and remove ('Remove {0}').
 */
;(function (undefined) {
	'use strict';

	if (typeof(Wicket) === "undefined") {
		window.Wicket = { };
	}

	if (typeof(Wicket.Autocomplete) !== "undefined") {
		return;
	}

	const WRAPPER_CLASS = 'wicket-autocomplete';

	/** How long typing pauses before the suggestions are asked for, in milliseconds. */
	const DELAY = 200;

	let counter = 0;

	const create = function (tag, className, text) {
		const element = document.createElement(tag);
		if (className) {
			element.className = className;
		}
		if (text !== undefined) {
			element.textContent = text;
		}
		return element;
	};

	/**
	 * The autocomplete of a hidden input.
	 */
	Wicket.Autocomplete = function (input, options) {
		this.input = input;
		this.options = options || { };
		this.multiple = !!this.options.multiple;
		this.chosen = (this.options.chosen || []).slice();
		this.attach();
	};

	Wicket.Autocomplete.prototype = {

		attach : function () {
			const input = this.input;
			let wrapper = input.parentElement;
			if (!wrapper || !wrapper.classList.contains(WRAPPER_CLASS)) {
				wrapper = create('div', WRAPPER_CLASS);
				input.parentNode.insertBefore(wrapper, input);
				wrapper.appendChild(input);
			}
			Array.prototype.slice.call(wrapper.children).forEach(function (child) {
				if (child !== input) {
					child.remove();
				}
			});
			if (wrapper.wicketAutocomplete) {
				wrapper.wicketAutocomplete.detach();
			}
			wrapper.wicketAutocomplete = this;
			this.wrapper = wrapper;
			this.id = input.id || ('wicket-autocomplete-' + (++counter));
			wrapper.classList.toggle('wicket-autocomplete-multiple', this.multiple);
			wrapper.classList.toggle('wicket-autocomplete-disabled', !!this.options.disabled);

			const engine = Wicket.Templates.engine(this.options.engine);
			this.row = engine ? engine.compile(this.options.template || '{{@label}}') :
				function (data, meta) {
					return Wicket.Templates.escapeHtml(meta.label);
				};

			this.field = create('div', 'wicket-autocomplete-field');
			this.chips = create('span', 'wicket-autocomplete-chips');
			this.field.appendChild(this.chips);
			this.text = create('input', 'wicket-autocomplete-text');
			this.text.type = 'text';
			this.text.autocomplete = 'off';
			this.text.disabled = !!this.options.disabled;
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

			this.renderChosen();
			this.bind();
		},

		/**
		 * @return the text of the labels of the input, without the autocomplete itself
		 */
		labelText : function () {
			const texts = [];
			Array.prototype.forEach.call(this.input.labels || [], function (label) {
				const copy = label.cloneNode(true);
				copy.querySelectorAll('.' + WRAPPER_CLASS).forEach(function (element) {
					element.remove();
				});
				const text = copy.textContent.replace(/\s+/g, ' ').trim();
				if (text) {
					texts.push(text);
				}
			});
			return texts.join(' ');
		},

		/**
		 * Shows the chosen values as chips and the text field, writes their ids into the input.
		 */
		renderChosen : function () {
			const self = this;
			this.chips.textContent = '';
			const removeLabel = this.options.disabled ? null : (this.options.remove || 'Remove {0}');
			this.chosen.forEach(function (value) {
				const chip = Wicket.Chips.create(value.label, removeLabel);
				chip.dataset.id = value.id;
				self.chips.appendChild(chip);
			});
			// one value: the chip stands in for the text field until it is removed
			const full = !this.multiple && this.chosen.length > 0;
			this.text.hidden = full;
			this.text.placeholder = this.chosen.length ? '' : (this.options.placeholder || '');
			// empty when nothing is chosen, so that the server tells a required one is missing
			this.input.value = this.chosen.length ? JSON.stringify(this.chosen.map(function (value) {
				return value.id;
			})) : '';
		},

		bind : function () {
			const self = this;
			this.listeners = [];
			const listen = function (target, type, listener) {
				target.addEventListener(type, listener);
				self.listeners.push([target, type, listener]);
			};
			listen(this.field, 'click', function (event) {
				const chip = Wicket.Chips.removeTarget(event);
				if (chip) {
					self.remove(chip.dataset.id);
					return;
				}
				if (!self.text.hidden) {
					self.text.focus();
				}
			});
			listen(this.text, 'input', function () {
				self.schedule();
			});
			listen(this.text, 'keydown', function (event) {
				self.onKey(event);
			});
			listen(this.list, 'mousedown', function (event) {
				// keeps the focus in the text field
				event.preventDefault();
			});
			listen(this.list, 'click', function (event) {
				const item = event.target.closest('.wicket-autocomplete-option');
				if (item) {
					self.choose(Number(item.dataset.index));
				}
			});
			listen(this.list, 'mousemove', function (event) {
				const item = event.target.closest('.wicket-autocomplete-option');
				if (item && item !== self.active) {
					self.activate(item, false);
				}
			});
			// a click on the label of the hidden input focuses the text field
			listen(this.input, 'focus', function () {
				self.text.focus();
			});
			this.onDocument = function (event) {
				if (!self.wrapper.contains(event.target)) {
					self.close();
				}
			};
			document.addEventListener('mousedown', this.onDocument);
			document.addEventListener('focusin', this.onDocument);
		},

		detach : function () {
			(this.listeners || []).forEach(function (listener) {
				listener[0].removeEventListener(listener[1], listener[2]);
			});
			document.removeEventListener('mousedown', this.onDocument);
			document.removeEventListener('focusin', this.onDocument);
			window.clearTimeout(this.timer);
		},

		/**
		 * Asks for the suggestions once typing paused.
		 */
		schedule : function () {
			window.clearTimeout(this.timer);
			const term = this.text.value.trim();
			if (term.length < (this.options.minLength || 0)) {
				this.close();
				return;
			}
			this.timer = window.setTimeout(this.search.bind(this, term), DELAY);
		},

		search : function (term) {
			const self = this;
			const request = ++this.request;
			this.showStatus(this.options.searching || 'Searching…');
			const url = this.options.url + (this.options.url.indexOf('?') >= 0 ? '&' : '?') +
				'term=' + encodeURIComponent(term);
			window.fetch(url, {
				credentials: 'same-origin',
				headers: { 'Accept': 'application/json' }
			}).then(function (response) {
				if (!response.ok) {
					throw new Error('HTTP ' + response.status);
				}
				return response.json();
			}).then(function (suggestions) {
				// only the answer to the latest request counts
				if (request === self.request && self.text.value.trim() === term) {
					self.show(suggestions);
				}
			}).catch(function (error) {
				if (Wicket.Log) {
					Wicket.Log.error('Wicket.Autocomplete: searching "' + term + '" failed', error);
				}
				self.close();
			});
		},

		request : 0,

		/**
		 * Lists the suggestions not chosen yet.
		 */
		show : function (suggestions) {
			const self = this;
			const chosenIds = this.chosen.map(function (value) {
				return String(value.id);
			});
			this.suggestions = (suggestions || []).filter(function (suggestion) {
				return chosenIds.indexOf(String(suggestion.id)) < 0;
			});
			this.list.textContent = '';
			this.active = null;
			this.suggestions.forEach(function (suggestion, index) {
				const item = create('li', 'wicket-autocomplete-option');
				item.id = self.id + '-option-' + index;
				item.dataset.index = index;
				item.setAttribute('role', 'option');
				item.innerHTML = self.row(suggestion.data || { }, {
					id: suggestion.id,
					label: suggestion.label,
					index: index
				});
				self.list.appendChild(item);
			});
			if (!this.suggestions.length) {
				this.showStatus(this.options.noMatch || 'No matches');
				return;
			}
			this.status.hidden = true;
			this.open();
			this.activate(this.list.firstElementChild, true);
		},

		showStatus : function (text) {
			this.status.textContent = text;
			this.status.hidden = false;
			this.open();
		},

		open : function () {
			this.menu.hidden = false;
			this.wrapper.classList.add('wicket-autocomplete-open');
			this.text.setAttribute('aria-expanded', 'true');
		},

		close : function () {
			window.clearTimeout(this.timer);
			this.request++;
			this.menu.hidden = true;
			this.wrapper.classList.remove('wicket-autocomplete-open');
			this.text.setAttribute('aria-expanded', 'false');
			this.text.removeAttribute('aria-activedescendant');
			this.active = null;
		},

		activate : function (item, scroll) {
			if (this.active) {
				this.active.classList.remove('wicket-autocomplete-active');
			}
			this.active = item;
			if (!item) {
				this.text.removeAttribute('aria-activedescendant');
				return;
			}
			item.classList.add('wicket-autocomplete-active');
			this.text.setAttribute('aria-activedescendant', item.id);
			if (scroll) {
				item.scrollIntoView({ block: 'nearest' });
			}
		},

		onKey : function (event) {
			const items = Array.prototype.slice.call(this.list.children);
			const index = items.indexOf(this.active);
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
		},

		/**
		 * Chooses a suggestion: a single autocomplete takes it instead of its value, a multiple one
		 * adds it.
		 */
		choose : function (index) {
			const suggestion = this.suggestions && this.suggestions[index];
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
		},

		/**
		 * Removes a chosen value, as the x of its chip does.
		 */
		remove : function (id) {
			if (this.options.disabled) {
				return;
			}
			const before = this.chosen.length;
			this.chosen = this.chosen.filter(function (value) {
				return String(value.id) !== String(id);
			});
			if (this.chosen.length === before) {
				return;
			}
			this.renderChosen();
			this.changed();
			this.text.focus();
		},

		changed : function () {
			this.input.dispatchEvent(new Event('input', { bubbles : true }));
			this.input.dispatchEvent(new Event('change', { bubbles : true }));
		}
	};

	/**
	 * Draws the autocomplete of a hidden input.
	 *
	 * @param input the input or its id
	 * @param options see above
	 * @return the autocomplete
	 */
	Wicket.Autocomplete.attach = function (input, options) {
		const element = typeof(input) === 'string' ? document.getElementById(input) : input;
		if (!element) {
			return null;
		}
		return new Wicket.Autocomplete(element, options);
	};
})();
