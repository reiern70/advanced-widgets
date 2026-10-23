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
 * A list rendering its items from JSON with an item template, fetching the next page of items
 * when the last one comes into view, and reporting clicks on [data-dt-action] elements with the key
 * of their item. The template engines are those of templates(). In Wicket it is the client side of
 * JsonBasedInfiniteList; the items come from cfg.dataUrl, or from cfg.load in React and Svelte.
 */

import { getHost, onRemoving, type Unsubscribe } from './host.js';
import { ANTILIA_LOOK, lookOf, partClasses, type Look } from './look.js';
import { Scrollbar } from './scrollbar.js';
import { templates, type CompiledTemplate } from './templates.js';

/** An item: its key, its number counting from 1, and the data its template renders. */
export interface InfiniteListEntry {
	key: string;
	number: number;
	data: unknown;
}

/** Items to show: those of the page asked for, how many there are in all, and whether to scroll to the first one. */
export interface InfiniteListData {
	items: InfiniteListEntry[];
	itemCount: number;
	top?: boolean;
}

/** A pushed message: the list it is for, its type, and what the type's handler needs. */
export interface InfiniteListMessage {
	tableId: string;
	typeId: string;
	[name: string]: unknown;
}

/**
 * How a list loads and renders its items.
 */
export interface InfiniteListConfig {
	/** The id of the list's element. */
	id: string;
	/** The look of the list; that of the element it is in when left out (see lookOf). */
	look?: Look;
	/** The URL answering with the items shown, or with the next page of them for ?more=true. */
	dataUrl?: string;
	/** Loads the items shown, or the next page of them, in place of dataUrl. */
	load?: (more: boolean) => Promise<InfiniteListData>;
	/** The name of the template engine. */
	engine?: string;
	/** The template of an item; {{@key}} and {{@number}} are the item's key and number. */
	template: string;
	/** The number of items there are, until the first answer tells. */
	itemCount?: number;
	/** Whether messages pushed to the list are handled. */
	push?: boolean;
	/** The locale numbers are written in. */
	locale?: string;
	/** How often the items shown are loaded again, in milliseconds; never when left out. */
	pollMillis?: number;
	/** The height of the list, which then scrolls itself; the page scrolls when left out. */
	height?: { value: number; unit: string };
	/** What the list says below its items; ${count} stands for the number of items. */
	texts: { loading: string; end: string; empty: string };
}

const ITEM_CLASS = 'json-infinite-list-item';

const LOADING_CLASS = 'json-infinite-list-loading';

/**
 * A list that scrolls infinitely.
 */
export class JsonInfiniteList {
	/** The live lists, by the id of their element. */
	static readonly instances: Record<string, JsonInfiniteList> = {};

	/**
	 * Gives a list of a fixed height its scroll bars; Wicket gives the bars of Wicket.Scrollbar
	 * only when the page loaded them.
	 */
	static attachScrollbar: (element: HTMLElement, look?: Look) =>
		{ update(): void; destroy(): void } | undefined = (element, look) =>
		Scrollbar.attach(element, { axis: 'y', look });

	readonly id: string;

	readonly cfg: InfiniteListConfig;

	readonly action: (action: string, key: string | null) => void;

	readonly messages: Record<string, (message: InfiniteListMessage) => void>;

	itemCount: number;

	readonly element: HTMLElement | null;

	/** The look of the list, whose classes the elements it makes get. */
	private look: Look = ANTILIA_LOOK;

	loading = false;

	private item!: CompiledTemplate;

	private status!: HTMLDivElement;

	private scrollbar: { update(): void; destroy(): void } | undefined = undefined;

	private endObserver: IntersectionObserver | undefined = undefined;

	private timer: number | undefined = undefined;

	private readonly clickHandler = (event: MouseEvent) => this.onClick(event);

	private readonly keyHandler = (event: KeyboardEvent) => this.onKeyDown(event);

	private stops: Unsubscribe[] = [];

	/**
	 * @param cfg how the list loads and renders its items
	 * @param action called with the action of a clicked [data-dt-action] element and the key of
	 *            its item
	 * @param messages the handlers of the messages pushed to the list, by type id
	 * @param data the items to show, when the server sends them along; loaded otherwise
	 */
	constructor(cfg: InfiniteListConfig, action: (action: string, key: string | null) => void,
		messages?: Record<string, (message: InfiniteListMessage) => void> | null,
		data?: InfiniteListData | null) {
		JsonInfiniteList.instances[cfg.id]?.destroy();

		this.id = cfg.id;
		this.cfg = cfg;
		this.action = action;
		this.messages = messages || {};
		this.itemCount = cfg.itemCount ?? 0;
		this.element = document.getElementById(cfg.id);
		if (!this.element) {
			return;
		}
		const engine = templates().engine(cfg.engine);
		if (!engine) {
			return;
		}
		this.item = engine.compile(cfg.template);
		this.look = cfg.look ?? lookOf(this.element);

		this.status = document.createElement('div');
		this.status.className = partClasses(this.look, 'list-status');
		this.status.setAttribute('role', 'status');
		this.element.replaceChildren(this.status);

		if (cfg.height) {
			this.element.classList.add('json-infinite-list--scrolling');
			this.element.style.height = cfg.height.value + cfg.height.unit;
			this.scrollbar = JsonInfiniteList.attachScrollbar(this.element, this.look);
		}

		this.element.addEventListener('click', this.clickHandler);
		this.element.addEventListener('keydown', this.keyHandler);
		JsonInfiniteList.instances[this.id] = this;
		this.stops.push(onRemoving(this.element, () => this.destroy()));
		if (cfg.push) {
			this.stops.push(getHost().onMessage((message) => this.onMessage(message)));
		}

		if (data) {
			this.update(data);
		} else {
			this.load();
		}

		if (cfg.pollMillis) {
			this.timer = window.setInterval(() => this.poll(), cfg.pollMillis);
		}
	}

	private isLive(): boolean {
		return JsonInfiniteList.instances[this.id] === this;
	}

	/**
	 * Fetches the items to show, or the next page of them.
	 *
	 * @param more whether to fetch the next page of items, to add them after the last one
	 * @returns a promise of the items, or of nothing if the list is gone
	 */
	fetch(more: boolean): Promise<InfiniteListData | undefined> {
		let loaded: Promise<InfiniteListData>;
		if (this.cfg.load) {
			loaded = this.cfg.load(more);
		} else {
			const url = (this.cfg.dataUrl ?? '') + (more ?
				((this.cfg.dataUrl ?? '').indexOf('?') >= 0 ? '&' : '?') + 'more=true' : '');
			loaded = window.fetch(url, {
				credentials: 'same-origin',
				headers: { Accept: 'application/json' }
			}).then((response) => {
				if (!response.ok) {
					throw new Error('HTTP ' + response.status);
				}
				return response.json() as Promise<InfiniteListData>;
			});
		}
		return loaded.then((json) => this.isLive() ? json : undefined);
	}

	/**
	 * Fetches the items shown again and renders them.
	 */
	load(): Promise<void> {
		this.setBusy(true);
		return this.fetch(false).then((json) => {
			if (json) {
				this.update(json);
			}
		}).catch((error) => {
			getHost().log.error('Wicket.JsonInfiniteList: loading the items of #' + this.id +
				' failed', error);
		}).finally(() => this.setBusy(false));
	}

	/**
	 * Fetches the next page of items and adds them after the last one, the status below the items
	 * telling meanwhile that more are loading.
	 */
	loadMore(): Promise<void> {
		if (this.loading) {
			return Promise.resolve();
		}
		this.setBusy(true);
		this.endObserver?.disconnect();
		return this.fetch(true).then((json) => {
			if (json) {
				this.itemCount = json.itemCount;
				this.append(json.items);
			}
		}).catch((error) => {
			getHost().log.error('Wicket.JsonInfiniteList: loading more items of #' + this.id +
				' failed', error);
		}).finally(() => this.setBusy(false));
	}

	/**
	 * Marks the list as loading, or done loading, and watches its end again when it is done.
	 */
	private setBusy(busy: boolean): void {
		this.loading = busy;
		this.element?.setAttribute('aria-busy', String(busy));
		this.updateStatus();
		if (!busy && this.isLive()) {
			this.watchEnd();
		}
	}

	/**
	 * Replaces the items with those given.
	 */
	update(data: InfiniteListData): void {
		this.itemCount = data.itemCount;
		const fragment = document.createDocumentFragment();
		(data.items || []).forEach((entry) => {
			fragment.appendChild(this.buildItem(entry.key, entry.number, entry.data));
		});
		this.items().forEach((item) => item.remove());
		this.element!.insertBefore(fragment, this.status);
		this.itemsChanged();
		if (data.top) {
			this.scrollToTop();
		}
	}

	/**
	 * Adds items after the last one, leaving out those shown already.
	 */
	append(entries: InfiniteListEntry[]): void {
		const fragment = document.createDocumentFragment();
		(entries || []).forEach((entry) => {
			if (!document.getElementById(this.itemId(entry.key))) {
				fragment.appendChild(this.buildItem(entry.key, entry.number, entry.data));
			}
		});
		this.element!.insertBefore(fragment, this.status);
		this.itemsChanged();
	}

	/**
	 * Repaints one item in place, keeping its position. An item the list does not show is ignored.
	 *
	 * @param key the key of the item
	 * @param data the item's data
	 */
	renderItem(key: string, data: unknown): void {
		const item = document.getElementById(this.itemId(key));
		if (item && item.parentNode === this.element) {
			const focused = document.activeElement === item;
			const replacement = this.buildItem(key, Number(item.getAttribute('data-number')), data);
			item.replaceWith(replacement);
			if (focused) {
				replacement.focus();
			}
			this.itemsChanged();
		}
	}

	/**
	 * Builds the element of an item: an article holding the template evaluated as item(data,
	 * meta), meta holding the item's key and number, for {{@key}} and {{@number}}.
	 */
	private buildItem(key: string, number: number, data: unknown): HTMLElement {
		const article = document.createElement('article');
		article.id = this.itemId(key);
		article.className = partClasses(this.look, 'list-item');
		article.tabIndex = 0;
		article.setAttribute('data-key', key);
		article.setAttribute('data-number', String(number));
		article.setAttribute('aria-posinset', String(number));
		article.innerHTML = this.item(data, { key, number });
		const action = article.firstElementChild;
		if (action && article.children.length === 1 && action.hasAttribute('data-dt-action')) {
			// the item's only element carries an action: so does the article, for Enter
			article.setAttribute('data-dt-action', action.getAttribute('data-dt-action') ?? '');
			action.removeAttribute('data-dt-action');
		}
		return article;
	}

	itemId(key: string): string {
		return this.id + '-item-' + key;
	}

	/**
	 * @returns the elements of the items, in order
	 */
	items(): HTMLElement[] {
		return Array.from(this.element?.children ?? []).filter((child) =>
			child.classList.contains(ITEM_CLASS)) as HTMLElement[];
	}

	/**
	 * Tells every item the number of items, says below them what there is to say and watches the
	 * last one.
	 */
	private itemsChanged(): void {
		const count = String(this.itemCount);
		this.items().forEach((item) => item.setAttribute('aria-setsize', count));
		this.updateStatus();
		this.scrollbar?.update();
		if (!this.loading) {
			this.watchEnd();
		}
	}

	/**
	 * Says below the items that more are loading, that all are shown, or that there are none.
	 */
	private updateStatus(): void {
		if (!this.status) {
			return;
		}
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
		this.status.classList.toggle(LOADING_CLASS, this.loading);
		this.status.textContent = text;
		this.status.hidden = !text;
	}

	private formatNumber(number: number): string {
		try {
			return Number(number).toLocaleString(this.cfg.locale);
		} catch {
			return String(number);
		}
	}

	/**
	 * Watches the last item, loading the next page of items once it comes into view, in the list,
	 * if it scrolls, or else in the window.
	 */
	private watchEnd(): void {
		if (typeof IntersectionObserver === 'undefined') {
			return;
		}
		if (!this.endObserver) {
			this.endObserver = new IntersectionObserver((entries) => {
				if (entries.some((entry) => entry.isIntersecting)) {
					this.loadMore();
				}
			}, { root: this.cfg.height ? this.element : null, rootMargin: '0px 0px 80px 0px' });
		}
		this.endObserver.disconnect();
		const items = this.items();
		if (items.length > 0 && items.length < this.itemCount) {
			this.endObserver.observe(items[items.length - 1]);
		}
	}

	/**
	 * Scrolls back to the first item: the list, if it scrolls, or else the page to the top of the
	 * list, if it is above the window.
	 */
	scrollToTop(): void {
		if (this.cfg.height) {
			this.element!.scrollTop = 0;
		} else if (this.element!.getBoundingClientRect().top < 0) {
			this.element!.scrollIntoView();
		}
	}

	private poll(): void {
		if (document.getElementById(this.id) !== this.element) {
			this.destroy();
		} else if (!this.loading) {
			this.load();
		}
	}

	/**
	 * Hands a message pushed to this list to the handler of its type.
	 */
	private onMessage(message: unknown): void {
		if (typeof message !== 'string' || message.charAt(0) !== '{') {
			return;
		}
		let parsed: InfiniteListMessage;
		try {
			parsed = JSON.parse(message);
		} catch {
			return;
		}
		if (parsed.tableId !== this.id) {
			return;
		}
		const handler = this.messages[parsed.typeId];
		if (handler) {
			handler(parsed);
		} else {
			getHost().log.error('Wicket.JsonInfiniteList: no handler for messages of type "' +
				parsed.typeId + '"');
		}
	}

	/**
	 * Sends the action of a clicked [data-dt-action] element with the key of its item.
	 */
	private onClick(event: MouseEvent): void {
		const target = event.target as Element;
		const actionElement = target.closest<HTMLElement>('[data-dt-action]');
		if (!actionElement || !this.element!.contains(actionElement)) {
			return;
		}
		const link = target.closest('a[href]');
		if (target.closest('a[href]:not([href="#"])') && !link?.hasAttribute('data-dt-action')) {
			// a real link in an item carrying an action goes where it points to
			return;
		}
		event.preventDefault();
		this.send(actionElement);
	}

	private send(actionElement: HTMLElement): void {
		const item = actionElement.closest('.' + ITEM_CLASS);
		this.action(actionElement.getAttribute('data-dt-action') ?? '',
			item ? item.getAttribute('data-key') : null);
	}

	/**
	 * Enter on an item carrying an action sends it; Page Down and Page Up move the focus to the
	 * next and the previous item, as in a feed.
	 */
	private onKeyDown(event: KeyboardEvent): void {
		const target = event.target as Element;
		const item = target.closest?.<HTMLElement>('.' + ITEM_CLASS);
		if (!item || item.parentNode !== this.element) {
			return;
		}
		if (event.key === 'Enter' && target === item && item.hasAttribute('data-dt-action')) {
			event.preventDefault();
			this.send(item);
		} else if (event.key === 'PageDown' || event.key === 'PageUp') {
			const next = event.key === 'PageDown' ? item.nextElementSibling :
				item.previousElementSibling;
			if (next && next.classList.contains(ITEM_CLASS)) {
				event.preventDefault();
				(next as HTMLElement).focus();
			}
		}
	}

	destroy(): void {
		this.endObserver?.disconnect();
		this.scrollbar?.destroy();
		if (this.timer) {
			window.clearInterval(this.timer);
		}
		this.element?.removeEventListener('click', this.clickHandler);
		this.element?.removeEventListener('keydown', this.keyHandler);
		this.stops.forEach((stop) => stop());
		this.stops = [];
		if (this.isLive()) {
			delete JsonInfiniteList.instances[this.id];
		}
	}
}
