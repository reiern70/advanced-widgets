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

import { forwardRef, useId, useImperativeHandle, useLayoutEffect, useRef } from 'react';

import { type InfiniteListConfig, type InfiniteListData, JsonInfiniteList, partClasses } from '@antilia/core';

import { useLook } from './Look.js';

export interface InfiniteListProps {
	/** Loads the items shown, or the next page of them when more is true. */
	load: (more: boolean) => Promise<InfiniteListData>;
	/** The template of an item, rendered from its data; {{@key}} and {{@number}} are its key and number. */
	template: string;
	/** The name of the template engine, the built-in one by default. */
	engine?: string;
	/** The height of the list, which then scrolls itself with slim scroll bars. */
	height?: InfiniteListConfig['height'];
	/** What the list says below its items; ${count} stands for the number of items. */
	texts: InfiniteListConfig['texts'];
	/** The locale numbers are written in. */
	locale?: string;
	/** Called with the action of a clicked [data-dt-action] element and the key of its item. */
	onAction?(action: string, key: string | null): void;
	/** CSS classes of the list, next to json-infinite-list. */
	className?: string;
}

/**
 * What a ref to an {@link InfiniteList} can do with the list.
 */
export interface InfiniteListHandle {
	/** Loads the items shown again, as after a filter or a change of them. */
	reload(): Promise<void>;
	/** Repaints one item with new data, in place; an item the list does not show is ignored. */
	renderItem(key: string, data: unknown): void;
	/** Scrolls the list back to its first item. */
	scrollToTop(): void;
}

/**
 * A list rendering its items from JSON with a template, loading the next page of them when the
 * last one comes into view. The items are rendered by the core into the list React renders empty;
 * a new key, or another template, loads them again. A ref gets an {@link InfiniteListHandle}.
 */
export const InfiniteList = forwardRef<InfiniteListHandle, InfiniteListProps>(function InfiniteList(
	{ load, template, engine, height, texts, locale, onAction, className }, ref) {
	const id = useId();
	const latest = useRef({ load, onAction });
	latest.current = { load, onAction };
	const instance = useRef<JsonInfiniteList | null>(null);
	const look = useLook();

	useImperativeHandle(ref, () => ({
		reload: () => instance.current?.load() ?? Promise.resolve(),
		renderItem: (key, data) => instance.current?.renderItem(key, data),
		scrollToTop: () => instance.current?.scrollToTop()
	}), []);

	const heightKey = height ? height.value + height.unit : '';
	const textsKey = JSON.stringify(texts);
	useLayoutEffect(() => {
		const list = new JsonInfiniteList({
			id, template, engine, height, texts, locale, look,
			load: (more) => latest.current.load(more)
		}, (action, key) => latest.current.onAction?.(action, key));
		instance.current = list;
		return () => {
			list.destroy();
			instance.current = null;
		};
	}, [id, template, engine, heightKey, textsKey, locale, look]);

	const list = <div id={id} className={[partClasses(look, 'list'), className].filter(Boolean)
		.join(' ')} />;
	// a list of a height scrolls in the wrapper of its scroll bars, which React renders
	return height ? <div className={partClasses(look, 'scrollbar')}>{list}</div> : list;
});
