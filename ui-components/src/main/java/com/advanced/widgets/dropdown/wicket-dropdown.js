/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * A drop-down drawn over a <select>, single or multiple, that looks the same in every browser.
 *
 * Wicket.Dropdown.attach(selectOrId, options) puts the select into a div.wicket-dropdown and adds
 * a button and a list of the select's options to it; the select itself is hidden but stays in the
 * page, holds the choice and fires its 'change' event when the user chooses, so forms and Ajax
 * behaviors work as with a native select. Attaching a select that is in a wrapper already, for
 * example one Ajax put in place of the previous one, rebuilds the button and the list.
 *
 * Options: placeholder (the text while nothing is chosen), filter (the placeholder of the filter
 * field), noMatch (the text when the filter matches nothing), more ('+{0}', the chosen options a
 * multiple drop-down's button has no room for) and filterThreshold (the number of options from
 * which on the list has a filter).
 */
;(function (undefined) {
	'use strict';

	if (typeof(Wicket) === "undefined") {
		window.Wicket = { };
	}

	if (typeof(Wicket.Dropdown) !== "undefined") {
		return;
	}

	const WRAPPER_CLASS = 'wicket-dropdown';

	/** The chosen options a multiple drop-down shows in its button before it shows '+n'. */
	const MAX_CHIPS = 3;

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
	 * The drop-down of a select.
	 *
	 * @param select the select
	 * @param options see above
	 */
	Wicket.Dropdown = function (select, options) {
		this.select = select;
		this.options = options || { };
		this.multiple = select.multiple;
		this.attach();
	};

	Wicket.Dropdown.prototype = {

		attach : function () {
			const select = this.select;
			let wrapper = select.parentElement;
			if (!wrapper || !wrapper.classList.contains(WRAPPER_CLASS)) {
				wrapper = create('div', WRAPPER_CLASS);
				select.parentNode.insertBefore(wrapper, select);
				wrapper.appendChild(select);
			}
			Array.prototype.slice.call(wrapper.children).forEach(function (child) {
				if (child !== select) {
					child.remove();
				}
			});
			if (wrapper.wicketDropdown) {
				wrapper.wicketDropdown.detach();
			}
			wrapper.wicketDropdown = this;
			wrapper.classList.toggle('wicket-dropdown-multiple', this.multiple);
			this.wrapper = wrapper;
			this.id = select.id || ('wicket-dropdown-' + (++counter));
			select.classList.add('wicket-dropdown-native');
			select.tabIndex = -1;
			select.setAttribute('aria-hidden', 'true');

			this.toggle = create('button', 'wicket-dropdown-toggle');
			this.toggle.type = 'button';
			this.toggle.id = this.id + '-toggle';
			this.toggle.setAttribute('aria-haspopup', 'listbox');
			this.toggle.setAttribute('aria-expanded', 'false');
			this.toggle.setAttribute('aria-controls', this.id + '-list');
			const label = this.labelText();
			if (label) {
				this.toggle.setAttribute('aria-label', label);
			}
			this.value = create('span', 'wicket-dropdown-value');
			this.toggle.appendChild(this.value);
			this.toggle.appendChild(create('span', 'wicket-dropdown-arrow'));
			wrapper.appendChild(this.toggle);

			this.menu = create('div', 'wicket-dropdown-menu');
			this.menu.hidden = true;
			if (select.options.length >= this.threshold()) {
				this.filter = create('input', 'wicket-dropdown-filter');
				this.filter.type = 'search';
				this.filter.autocomplete = 'off';
				this.filter.placeholder = this.options.filter || 'Filter…';
				this.filter.setAttribute('aria-controls', this.id + '-list');
				this.menu.appendChild(this.filter);
			}
			this.list = create('ul', 'wicket-dropdown-list');
			this.list.id = this.id + '-list';
			this.list.tabIndex = -1;
			this.list.setAttribute('role', 'listbox');
			if (label) {
				this.list.setAttribute('aria-label', label);
			}
			if (this.multiple) {
				this.list.setAttribute('aria-multiselectable', 'true');
			}
			this.menu.appendChild(this.list);
			this.empty = create('div', 'wicket-dropdown-empty', this.options.noMatch || 'No matches');
			this.empty.hidden = true;
			this.menu.appendChild(this.empty);
			wrapper.appendChild(this.menu);

			this.renderOptions();
			this.update();
			this.bind();
		},

		threshold : function () {
			const threshold = this.options.filterThreshold;
			return typeof(threshold) === 'number' ? threshold : 8;
		},

		/**
		 * @return the text of the labels of the select, if it has any
		 */
		labelText : function () {
			const labels = this.select.labels;
			const texts = [];
			Array.prototype.forEach.call(labels || [], function (label) {
				const copy = label.cloneNode(true);
				copy.querySelectorAll('select, .' + WRAPPER_CLASS).forEach(function (element) {
					element.remove();
				});
				const text = copy.textContent.replace(/\s+/g, ' ').trim();
				if (text) {
					texts.push(text);
				}
			});
			return texts.join(' ') || this.select.getAttribute('aria-label') || this.select.title ||
				'';
		},

		renderOptions : function () {
			const self = this;
			this.list.textContent = '';
			this.items = [];
			Array.prototype.forEach.call(this.select.options, function (option, index) {
				const item = create('li', 'wicket-dropdown-option');
				item.id = self.id + '-option-' + index;
				item.setAttribute('role', 'option');
				item.dataset.index = index;
				if (self.multiple) {
					item.appendChild(create('span', 'wicket-dropdown-check'));
				}
				item.appendChild(create('span', 'wicket-dropdown-text', option.text));
				if (option.disabled) {
					item.setAttribute('aria-disabled', 'true');
				}
				if (option.value === '' && !self.multiple) {
					item.classList.add('wicket-dropdown-option-none');
				}
				self.list.appendChild(item);
				self.items.push(item);
			});
		},

		/**
		 * Shows the choice of the select: in the button and in the list.
		 */
		update : function () {
			const select = this.select;
			this.items.forEach(function (item) {
				item.setAttribute('aria-selected',
					String(select.options[item.dataset.index].selected));
			});
			this.toggle.disabled = select.disabled;
			this.wrapper.classList.toggle('wicket-dropdown-disabled', select.disabled);

			this.value.textContent = '';
			const chosen = Array.prototype.filter.call(select.options, function (option) {
				return option.selected && option.value !== '';
			});
			if (!chosen.length) {
				const none = !this.multiple && select.selectedIndex >= 0 ?
					select.options[select.selectedIndex].text : '';
				this.value.textContent = none || this.options.placeholder || '';
				this.value.classList.add('wicket-dropdown-placeholder');
				return;
			}
			this.value.classList.remove('wicket-dropdown-placeholder');
			if (!this.multiple) {
				this.value.textContent = chosen[0].text;
				return;
			}
			const self = this;
			const removeLabel = this.select.disabled ? null : (this.options.remove || 'Remove {0}');
			chosen.slice(0, MAX_CHIPS).forEach(function (option) {
				const chip = Wicket.Chips.create(option.text, removeLabel);
				chip.dataset.index = option.index;
				self.value.appendChild(chip);
			});
			if (chosen.length > MAX_CHIPS) {
				const more = (this.options.more || '+{0}').replace('{0}', chosen.length - MAX_CHIPS);
				this.value.appendChild(Wicket.Chips.more(more));
			}
			this.toggle.title = chosen.map(function (option) {
				return option.text;
			}).join(', ');
		},

		bind : function () {
			const self = this;
			this.listeners = [];
			const listen = function (target, type, listener) {
				target.addEventListener(type, listener);
				self.listeners.push([target, type, listener]);
			};
			listen(this.toggle, 'click', function (event) {
				// the x of a chip deselects its option, without opening the list
				const chip = Wicket.Chips.removeTarget(event);
				if (chip) {
					event.preventDefault();
					self.deselect(Number(chip.dataset.index));
					return;
				}
				if (self.isOpen()) {
					self.close(false);
				} else {
					self.open();
				}
			});
			listen(this.toggle, 'keydown', function (event) {
				if (self.multiple && (event.key === 'Backspace' || event.key === 'Delete')) {
					// removes the last chosen option, as the x of its chip does
					event.preventDefault();
					const chosen = Array.prototype.filter.call(self.select.options,
						function (option) {
							return option.selected && option.value !== '';
						});
					if (chosen.length) {
						self.deselect(chosen[chosen.length - 1].index);
					}
					return;
				}
				if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
					event.preventDefault();
					self.open();
				}
			});
			listen(this.list, 'click', function (event) {
				const item = event.target.closest('.wicket-dropdown-option');
				if (item) {
					self.choose(item);
				}
			});
			listen(this.list, 'mousemove', function (event) {
				const item = event.target.closest('.wicket-dropdown-option');
				if (item && item !== self.active) {
					self.activate(item, false);
				}
			});
			listen(this.menu, 'keydown', function (event) {
				self.onKey(event);
			});
			if (this.filter) {
				listen(this.filter, 'input', function () {
					self.applyFilter();
				});
			}
			// a click on the label of the hidden select focuses the button
			listen(this.select, 'focus', function () {
				self.toggle.focus();
			});
			// the select changed by someone else, for example a script resetting a form
			listen(this.select, 'change', function () {
				self.update();
			});
			this.onDocument = function (event) {
				if (self.isOpen() && !self.wrapper.contains(event.target)) {
					self.close(false);
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
		},

		isOpen : function () {
			return !this.menu.hidden;
		},

		open : function () {
			if (this.select.disabled || this.isOpen()) {
				return;
			}
			this.menu.hidden = false;
			this.toggle.setAttribute('aria-expanded', 'true');
			this.wrapper.classList.add('wicket-dropdown-open');
			// open upwards when there is no room below but there is above
			this.wrapper.classList.remove('wicket-dropdown-up');
			const box = this.menu.getBoundingClientRect();
			const toggle = this.toggle.getBoundingClientRect();
			if (box.bottom > window.innerHeight && toggle.top > box.height) {
				this.wrapper.classList.add('wicket-dropdown-up');
			}
			if (this.filter) {
				this.filter.value = '';
				this.applyFilter();
			}
			const selected = this.items.filter(function (item) {
				return item.getAttribute('aria-selected') === 'true';
			})[0];
			this.activate(selected || this.visibleItems()[0], true);
			(this.filter || this.list).focus();
		},

		/**
		 * @param focus whether to focus the button again
		 */
		close : function (focus) {
			if (!this.isOpen()) {
				return;
			}
			this.menu.hidden = true;
			this.toggle.setAttribute('aria-expanded', 'false');
			this.wrapper.classList.remove('wicket-dropdown-open', 'wicket-dropdown-up');
			if (focus) {
				this.toggle.focus();
			}
		},

		visibleItems : function () {
			return this.items.filter(function (item) {
				return !item.hidden && item.getAttribute('aria-disabled') !== 'true';
			});
		},

		/**
		 * Marks an option as the one the keyboard acts on.
		 */
		activate : function (item, scroll) {
			if (this.active) {
				this.active.classList.remove('wicket-dropdown-active');
			}
			this.active = item;
			const owner = this.filter || this.list;
			if (!item) {
				owner.removeAttribute('aria-activedescendant');
				return;
			}
			item.classList.add('wicket-dropdown-active');
			owner.setAttribute('aria-activedescendant', item.id);
			if (scroll) {
				const list = this.list;
				if (item.offsetTop < list.scrollTop) {
					list.scrollTop = item.offsetTop;
				} else if (item.offsetTop + item.offsetHeight > list.scrollTop + list.clientHeight) {
					list.scrollTop = item.offsetTop + item.offsetHeight - list.clientHeight;
				}
			}
		},

		applyFilter : function () {
			const text = this.filter.value.trim().toLowerCase();
			let shown = 0;
			this.items.forEach(function (item) {
				const match = !text || item.textContent.toLowerCase().indexOf(text) >= 0;
				item.hidden = !match;
				if (match) {
					shown++;
				}
			});
			this.empty.hidden = shown > 0;
			if (!this.active || this.active.hidden) {
				this.activate(this.visibleItems()[0], true);
			}
		},

		onKey : function (event) {
			const visible = this.visibleItems();
			const index = visible.indexOf(this.active);
			switch (event.key) {
				case 'ArrowDown':
					event.preventDefault();
					this.activate(visible[Math.min(index + 1, visible.length - 1)], true);
					break;
				case 'ArrowUp':
					event.preventDefault();
					this.activate(visible[Math.max(index - 1, 0)], true);
					break;
				case 'Home':
				case 'End':
					if (event.target === this.filter) {
						return;
					}
					event.preventDefault();
					this.activate(visible[event.key === 'Home' ? 0 : visible.length - 1], true);
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
		},

		/**
		 * Deselects an option of a multiple drop-down, as the x of its chip does.
		 *
		 * @param index the index of the option in the select
		 */
		deselect : function (index) {
			const option = this.select.options[index];
			if (!option || !option.selected || this.select.disabled) {
				return;
			}
			option.selected = false;
			this.update();
			this.select.dispatchEvent(new Event('input', { bubbles : true }));
			this.select.dispatchEvent(new Event('change', { bubbles : true }));
		},

		/**
		 * Chooses an option: a single drop-down takes it and closes, a multiple one toggles it.
		 */
		choose : function (item) {
			if (item.getAttribute('aria-disabled') === 'true') {
				return;
			}
			const option = this.select.options[item.dataset.index];
			let changed;
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
				this.select.dispatchEvent(new Event('input', { bubbles : true }));
				this.select.dispatchEvent(new Event('change', { bubbles : true }));
			}
		}
	};

	/**
	 * Draws the drop-down of a select.
	 *
	 * @param select the select or its id
	 * @param options see above
	 * @return the drop-down
	 */
	Wicket.Dropdown.attach = function (select, options) {
		const element = typeof(select) === 'string' ? document.getElementById(select) : select;
		if (!element) {
			return null;
		}
		return new Wicket.Dropdown(element, options);
	};

	/**
	 * @return the drop-down of a select, if it has one
	 */
	Wicket.Dropdown.of = function (select) {
		const element = typeof(select) === 'string' ? document.getElementById(select) : select;
		const wrapper = element && element.parentElement;
		return wrapper && wrapper.wicketDropdown || null;
	};
})();
