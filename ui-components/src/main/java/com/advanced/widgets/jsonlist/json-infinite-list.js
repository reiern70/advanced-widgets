/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * Client side of a JsonBasedInfiniteList: renders its items from JSON with the item template,
 * fetches the next page of items when the last one comes into view, and sends clicks on
 * [data-dt-action] elements to the list's Ajax behavior. The template engines are those of
 * Wicket.Templates (wicket-templates.js).
 */
;(function (undefined) {
	'use strict';

	if (typeof(Wicket) === "undefined") {
		window.Wicket = { };
	}

	if (typeof(Wicket.JsonInfiniteList) !== "undefined") {
		return;
	}

	const ITEM_CLASS = 'json-infinite-list-item';

	const STATUS_CLASS = 'json-infinite-list-status';

	const LOADING_CLASS = 'json-infinite-list-loading';

	/**
	 * A list that scrolls infinitely.
	 *
	 * @param cfg {id, dataUrl, engine, template, itemCount, push, locale, pollMillis, height
	 *            {value, unit}, texts {loading, end, empty}}
	 * @param action function(action, key) calling the list's Ajax behavior
	 * @param messages the handlers of the messages pushed to the list, by type id, each called
	 *            as function(message)
	 * @param data {items, itemCount, top}, the items the browser shows when the server sends them
	 *            along, for example when it re-renders the list; fetched otherwise
	 */
	Wicket.JsonInfiniteList = Wicket.Class.create();
	Wicket.JsonInfiniteList.prototype = {

		initialize : function (cfg, action, messages, data) {
			const previous = Wicket.JsonInfiniteList.instances[cfg.id];
			if (previous) {
				previous.destroy();
			}

			this.id = cfg.id;
			this.cfg = cfg;
			this.action = action;
			this.messages = messages || { };
			this.itemCount = cfg.itemCount;
			this.element = Wicket.$(cfg.id);
			if (!this.element) {
				return;
			}
			const engine = Wicket.Templates.engines[cfg.engine];
			if (!engine) {
				Wicket.Log.error('Wicket.JsonInfiniteList: no template engine registered as "' +
					cfg.engine + '"');
				return;
			}
			this.item = engine.compile(cfg.template);

			this.status = document.createElement('div');
			this.status.className = STATUS_CLASS;
			this.status.setAttribute('role', 'status');
			this.element.replaceChildren(this.status);

			if (cfg.height) {
				this.element.classList.add('json-infinite-list--scrolling');
				this.element.style.height = cfg.height.value + cfg.height.unit;
				if (Wicket.Scrollbar) {
					this.scrollbar = Wicket.Scrollbar.attach(this.element, { axis: 'y' });
				}
			}

			this.clickHandler = Wicket.bind(this.onClick, this);
			this.element.addEventListener('click', this.clickHandler);
			this.keyHandler = Wicket.bind(this.onKeyDown, this);
			this.element.addEventListener('keydown', this.keyHandler);
			Wicket.JsonInfiniteList.instances[this.id] = this;

			if (data) {
				this.update(data);
			} else {
				this.load();
			}

			if (cfg.pollMillis) {
				this.timer = window.setInterval(Wicket.bind(this.poll, this), cfg.pollMillis);
			}
		},

		/**
		 * Fetches the items the server says the browser shows, from the list's data URL, and
		 * renders them in place of those shown.
		 *
		 * @param more whether to fetch the next page of items, to add them after the last one
		 * @return a promise of the JSON, or of nothing if the list is gone
		 */
		fetch : function (more) {
			const self = this;
			const url = this.cfg.dataUrl + (more ?
				(this.cfg.dataUrl.indexOf('?') >= 0 ? '&' : '?') + 'more=true' : '');
			return window.fetch(url, {
				credentials: 'same-origin',
				headers: { 'Accept': 'application/json' }
			}).then(function (response) {
				if (!response.ok) {
					throw new Error('HTTP ' + response.status);
				}
				return response.json();
			}).then(function (json) {
				return Wicket.JsonInfiniteList.instances[self.id] === self ? json : undefined;
			});
		},

		/**
		 * Fetches the items the browser shows again and renders them.
		 */
		load : function () {
			const self = this;
			this.setBusy(true);
			this.fetch(false).then(function (json) {
				if (json) {
					self.update(json);
				}
			}).catch(function (error) {
				Wicket.Log.error('Wicket.JsonInfiniteList: loading the items of #' + self.id +
					' failed', error);
			}).finally(function () {
				self.setBusy(false);
			});
		},

		/**
		 * Fetches the next page of items and adds them after the last one, the status below the
		 * items telling meanwhile that more are loading.
		 */
		loadMore : function () {
			if (this.loading) {
				return;
			}
			const self = this;
			this.setBusy(true);
			if (this.endObserver) {
				this.endObserver.disconnect();
			}
			this.fetch(true).then(function (json) {
				if (json) {
					self.itemCount = json.itemCount;
					self.append(json.items);
				}
			}).catch(function (error) {
				Wicket.Log.error('Wicket.JsonInfiniteList: loading more items of #' + self.id +
					' failed', error);
			}).finally(function () {
				self.setBusy(false);
			});
		},

		/**
		 * Marks the list as loading, or done loading, and watches its end again when it is done.
		 */
		setBusy : function (busy) {
			this.loading = busy;
			this.element.setAttribute('aria-busy', String(busy));
			this.updateStatus();
			if (!busy && Wicket.JsonInfiniteList.instances[this.id] === this) {
				this.watchEnd();
			}
		},

		/**
		 * Replaces the items with those the server sent.
		 *
		 * @param data {items, itemCount, top}
		 */
		update : function (data) {
			this.itemCount = data.itemCount;
			const self = this;
			const fragment = document.createDocumentFragment();
			(data.items || []).forEach(function (entry) {
				fragment.appendChild(self.buildItem(entry.key, entry.number, entry.data));
			});
			this.items().forEach(function (item) {
				item.remove();
			});
			this.element.insertBefore(fragment, this.status);
			this.itemsChanged();
			if (data.top) {
				this.scrollToTop();
			}
		},

		/**
		 * Adds items after the last one, leaving out those shown already.
		 *
		 * @param entries [{key, number, data}]
		 */
		append : function (entries) {
			const self = this;
			const fragment = document.createDocumentFragment();
			(entries || []).forEach(function (entry) {
				if (!document.getElementById(self.itemId(entry.key))) {
					fragment.appendChild(self.buildItem(entry.key, entry.number, entry.data));
				}
			});
			this.element.insertBefore(fragment, this.status);
			this.itemsChanged();
		},

		/**
		 * Repaints one item in place, keeping its position. An item the list does not show is
		 * ignored.
		 *
		 * @param key the key of the item
		 * @param data the item's data
		 */
		renderItem : function (key, data) {
			const item = document.getElementById(this.itemId(key));
			if (item && item.parentNode === this.element) {
				const focused = document.activeElement === item;
				const replacement = this.buildItem(key, Number(item.getAttribute('data-number')),
					data);
				item.replaceWith(replacement);
				if (focused) {
					replacement.focus();
				}
				this.itemsChanged();
			}
		},

		/**
		 * Builds the element of an item: an article holding the template evaluated as
		 * item(data, meta), meta holding the item's key and number, for {{@key}} and {{@number}}.
		 */
		buildItem : function (key, number, data) {
			const article = document.createElement('article');
			article.id = this.itemId(key);
			article.className = ITEM_CLASS;
			article.tabIndex = 0;
			article.setAttribute('data-key', key);
			article.setAttribute('data-number', number);
			article.setAttribute('aria-posinset', number);
			article.innerHTML = this.item(data, { key: key, number: number });
			const action = article.firstElementChild;
			if (action && article.children.length === 1 && action.hasAttribute('data-dt-action')) {
				// the item's only element carries an action: so does the article, for Enter
				article.setAttribute('data-dt-action', action.getAttribute('data-dt-action'));
				action.removeAttribute('data-dt-action');
			}
			return article;
		},

		itemId : function (key) {
			return this.id + '-item-' + key;
		},

		/**
		 * @return the elements of the items, in order
		 */
		items : function () {
			return Array.prototype.filter.call(this.element.children, function (child) {
				return child.classList.contains(ITEM_CLASS);
			});
		},

		/**
		 * Tells every item the number of items, says below them what there is to say and watches
		 * the last one.
		 */
		itemsChanged : function () {
			const count = String(this.itemCount);
			this.items().forEach(function (item) {
				item.setAttribute('aria-setsize', count);
			});
			this.updateStatus();
			if (this.scrollbar) {
				this.scrollbar.update();
			}
			if (!this.loading) {
				this.watchEnd();
			}
		},

		/**
		 * Says below the items that more are loading, that all are shown, or that there are none.
		 */
		updateStatus : function () {
			const texts = this.cfg.texts;
			const shown = this.items().length;
			let text = '';
			if (this.loading) {
				text = texts.loading;
			} else if (this.itemCount === 0) {
				text = texts.empty;
			} else if (shown > 0 && shown >= this.itemCount) {
				text = texts.end.replace('${count}', this.formatNumber(this.itemCount));
			}
			this.status.classList.toggle(LOADING_CLASS, !!this.loading);
			this.status.textContent = text;
			this.status.hidden = !text;
		},

		formatNumber : function (number) {
			try {
				return Number(number).toLocaleString(this.cfg.locale);
			} catch (e) {
				return String(number);
			}
		},

		/**
		 * Watches the last item, loading the next page of items once it comes into view, in the
		 * list, if it scrolls, or else in the window.
		 */
		watchEnd : function () {
			if (!window.IntersectionObserver) {
				return;
			}
			if (!this.endObserver) {
				const self = this;
				this.endObserver = new window.IntersectionObserver(function (entries) {
					if (entries.some(function (entry) {
						return entry.isIntersecting;
					})) {
						self.loadMore();
					}
				}, { root: this.cfg.height ? this.element : null, rootMargin: '0px 0px 80px 0px' });
			}
			this.endObserver.disconnect();
			const items = this.items();
			if (items.length > 0 && items.length < this.itemCount) {
				this.endObserver.observe(items[items.length - 1]);
			}
		},

		/**
		 * Scrolls back to the first item: the list, if it scrolls, or else the page to the top of
		 * the list, if it is above the window.
		 */
		scrollToTop : function () {
			if (this.cfg.height) {
				this.element.scrollTop = 0;
			} else if (this.element.getBoundingClientRect().top < 0) {
				this.element.scrollIntoView();
			}
		},

		poll : function () {
			if (Wicket.$(this.id) !== this.element) {
				this.destroy();
			} else if (!this.loading) {
				this.load();
			}
		},

		/**
		 * Sends the action of a clicked [data-dt-action] element with the key of its item.
		 */
		onClick : function (event) {
			const actionElement = event.target.closest('[data-dt-action]');
			if (!actionElement || !this.element.contains(actionElement)) {
				return;
			}
			if (event.target.closest('a[href]:not([href="#"])') &&
				!event.target.closest('a[href]').hasAttribute('data-dt-action')) {
				// a real link in an item carrying an action goes where it points to
				return;
			}
			event.preventDefault();
			this.send(actionElement);
		},

		send : function (actionElement) {
			const item = actionElement.closest('.' + ITEM_CLASS);
			this.action(actionElement.getAttribute('data-dt-action'),
				item ? item.getAttribute('data-key') : null);
		},

		/**
		 * Enter on an item carrying an action sends it; Page Down and Page Up move the focus to
		 * the next and the previous item, as in a feed.
		 */
		onKeyDown : function (event) {
			const item = event.target.closest && event.target.closest('.' + ITEM_CLASS);
			if (!item || item.parentNode !== this.element) {
				return;
			}
			if (event.key === 'Enter' && event.target === item &&
				item.hasAttribute('data-dt-action')) {
				event.preventDefault();
				this.send(item);
			} else if (event.key === 'PageDown' || event.key === 'PageUp') {
				const next = event.key === 'PageDown' ? item.nextElementSibling :
					item.previousElementSibling;
				if (next && next.classList.contains(ITEM_CLASS)) {
					event.preventDefault();
					next.focus();
				}
			}
		},

		destroy : function () {
			if (this.endObserver) {
				this.endObserver.disconnect();
			}
			if (this.scrollbar) {
				this.scrollbar.destroy();
			}
			if (this.timer) {
				window.clearInterval(this.timer);
			}
			if (this.element) {
				this.element.removeEventListener('click', this.clickHandler);
				this.element.removeEventListener('keydown', this.keyHandler);
			}
			const instances = Wicket.JsonInfiniteList.instances;
			if (instances[this.id] === this) {
				delete instances[this.id];
			}
		}
	};

	/**
	 * The live lists, by the markup id of their element.
	 */
	Wicket.JsonInfiniteList.instances = { };

	Wicket.Event.subscribe(Wicket.Event.Topic.DOM_NODE_REMOVING, function (jqEvent, element) {
		const instances = Wicket.JsonInfiniteList.instances;
		Object.keys(instances).forEach(function (id) {
			const list = instances[id].element;
			if (element === list || (element.contains && element.contains(list))) {
				instances[id].destroy();
			}
		});
	});

	Wicket.Event.subscribe('/websocket/message', function (jqEvent, message) {
		if (typeof message !== 'string' || message.charAt(0) !== '{') {
			return;
		}
		let parsed;
		try {
			parsed = JSON.parse(message);
		} catch (e) {
			return;
		}
		const instance = Wicket.JsonInfiniteList.instances[parsed.tableId];
		if (!instance || !instance.cfg.push) {
			return;
		}
		const handler = instance.messages[parsed.typeId];
		if (handler) {
			handler(parsed);
		} else {
			Wicket.Log.error('Wicket.JsonInfiniteList: no handler for messages of type "' +
				parsed.typeId + '"');
		}
	});
})();
