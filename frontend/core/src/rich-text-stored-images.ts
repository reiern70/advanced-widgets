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
 * The images a page stores permanently: the user ticks those to store as they upload
 * (onStorePermanently), and inserts those stored into any text later, by the tool storedImage,
 * "Insert image from store". Its form is a WAI-ARIA combobox: a field the user types a name into,
 * and the list of the images the page finds for it (onFindImages), each with a thumbnail, a moment
 * after the user stopped typing, which scrolls with the scroll bars of the components. The arrow
 * keys go through the list, Enter or a click inserts the image; Escape closes the form.
 *
 * storePermanentlyAt() and findImagesAt() ask Wicket's RichTextImageUploadBehavior, which stores
 * permanently only the images it took on its page, by its store, and finds those its store keeps.
 */

import type { Look } from './look.js';
import { partClasses } from './look.js';
import { isSafeUrl } from './rich-text-schema.js';
import { Scrollbar, type ScrollbarHandle } from './scrollbar.js';

/** An image the page stores permanently: its address, and the name the user finds it by. */
export interface RichTextStoredImage {
	url: string;
	name: string;
}

/**
 * Finds the images the page stores permanently whose names hold what the user typed; all of them
 * for nothing typed, the latest first. Aborted as the user types on.
 */
export type RichTextImageFinder = (query: string,
	signal: AbortSignal) => Promise<readonly RichTextStoredImage[]>;

/** Has the page store images uploaded permanently, resolving to the addresses of those stored. */
export type RichTextImageKeeper = (urls: string[]) => Promise<string[]>;

/**
 * @returns the images of an answer: those with an address, named by it when they have no name
 */
export function storedImagesOf(images: unknown): RichTextStoredImage[] {
	if (!Array.isArray(images)) {
		return [];
	}
	return images.flatMap((image: unknown): RichTextStoredImage[] => {
		const { url, name } = (image ?? {}) as { url?: unknown; name?: unknown };
		if (typeof url !== 'string' || !url.trim()) {
			return [];
		}
		return [{ url: url.trim(), name: typeof name === 'string' && name.trim() ? name.trim() :
			url.trim() }];
	});
}

/**
 * @returns a finder posting what the user typed to a URL, under a name, as Wicket's
 *          RichTextImageUploadBehavior takes it, and reading the images from the answer,
 *          {"images": [{"url": "...", "name": "..."}]}; {"error": "..."} says why not
 */
export function findImagesAt(url: string, name = 'find'): RichTextImageFinder {
	return async (query, signal) => {
		const body = new URLSearchParams();
		body.append(name, query);
		const response = await fetch(url, { method: 'POST', body, signal,
			credentials: 'same-origin', headers: { Accept: 'application/json' } });
		let answer: { images?: unknown; error?: unknown } = {};
		try {
			answer = await response.json() as typeof answer;
		} catch {
			// no JSON: the server failed
		}
		if (typeof answer.error === 'string' && answer.error) {
			throw new Error(answer.error);
		}
		if (!response.ok || !Array.isArray(answer.images)) {
			throw new Error('');
		}
		return storedImagesOf(answer.images);
	};
}

/**
 * @returns a function asking a URL to store images permanently, by their addresses: it posts
 *          them as a JSON array under a name, as Wicket's RichTextImageUploadBehavior takes them,
 *          which stores those it took alone, and resolves to the addresses stored, as the answer
 *          says, {"stored": [...]}
 */
export function storePermanentlyAt(url: string, name = 'permanent'): RichTextImageKeeper {
	return async (urls) => {
		const body = new URLSearchParams();
		body.append(name, JSON.stringify(urls));
		const response = await fetch(url, { method: 'POST', body, credentials: 'same-origin',
			headers: { Accept: 'application/json' } });
		let answer: { stored?: unknown } = {};
		try {
			answer = await response.json() as typeof answer;
		} catch {
			// no JSON: the server failed
		}
		return Array.isArray(answer.stored) ? answer.stored.filter((each): each is string =>
			typeof each === 'string') : [];
	};
}

/** What the form of the tool storedImage says. */
export interface ImagePickerTexts {
	/** The label of the field. */
	findImages: string;
	/** While the page searches, and when it found nothing. */
	findingImages: string;
	noStoredImages: string;
}

/** The form of the tool storedImage, while it is open. */
export interface ImagePicker {
	/** The label, the field, the list and the line telling the search. */
	element: HTMLElement;
	/** The field the user types into, which takes the focus. */
	input: HTMLInputElement;
	/** Searches for what the field holds now. */
	search(): void;
	/** Chooses the image active in the list, else the first one; nothing for an empty list. */
	choose(): void;
	/** Stops searching. */
	destroy(): void;
}

/** Counts the pickers, for the ids of their parts. */
let pickers = 0;

/**
 * Makes the form of the tool storedImage: a field the user types a name into, a WAI-ARIA combobox,
 * and the list of the images found for it, each with a thumbnail and its name.
 *
 * @param find finds the images for what the user typed
 * @param onChoose called with the image the user chose
 * @param delay how long after the user stopped typing the picker searches, in ms
 */
export function imagePicker(texts: ImagePickerTexts, look: Look, find: RichTextImageFinder,
	onChoose: (image: RichTextStoredImage) => void, delay = 150): ImagePicker {
	const id = 'antilia-rich-text-picker-' + (++pickers);
	const element = document.createElement('div');
	element.className = 'antilia-rich-text-picker';
	const label = document.createElement('label');
	label.className = partClasses(look, 'rich-text-form-label');
	label.htmlFor = id + '-field';
	label.textContent = texts.findImages;
	const input = document.createElement('input');
	input.className = partClasses(look, 'rich-text-form-input');
	input.id = id + '-field';
	input.type = 'search';
	input.autocomplete = 'off';
	input.spellcheck = false;
	input.setAttribute('role', 'combobox');
	input.setAttribute('aria-autocomplete', 'list');
	input.setAttribute('aria-expanded', 'false');
	input.setAttribute('aria-controls', id + '-list');
	const status = document.createElement('p');
	status.className = 'antilia-rich-text-picker-status';
	status.setAttribute('role', 'status');
	const list = document.createElement('ul');
	list.className = 'antilia-rich-text-picker-list';
	list.id = id + '-list';
	list.setAttribute('role', 'listbox');
	list.setAttribute('aria-label', texts.findImages);
	list.hidden = true;
	element.append(label, input, status, list);
	let scrollbar: ScrollbarHandle | undefined;

	let images: RichTextStoredImage[] = [];
	let active = -1;
	let timer: ReturnType<typeof setTimeout> | null = null;
	let pending: AbortController | null = null;
	let destroyed = false;

	const options = () => Array.from(list.querySelectorAll<HTMLElement>('[role="option"]'));
	const activate = (index: number) => {
		active = images.length ? (index + images.length) % images.length : -1;
		options().forEach((option, each) => {
			const on = each === active;
			option.setAttribute('aria-selected', String(on));
			option.classList.toggle('antilia-rich-text-picker-active', on);
			if (on) {
				option.scrollIntoView?.({ block: 'nearest' });
			}
		});
		if (active >= 0) {
			input.setAttribute('aria-activedescendant', id + '-option-' + active);
		} else {
			input.removeAttribute('aria-activedescendant');
		}
	};
	const show = (found: readonly RichTextStoredImage[], message: string) => {
		images = found.filter((image) => isSafeUrl(image.url));
		list.replaceChildren(...images.map((image, index) => {
			const option = document.createElement('li');
			option.id = id + '-option-' + index;
			option.className = 'antilia-rich-text-picker-option';
			option.setAttribute('role', 'option');
			option.setAttribute('aria-selected', 'false');
			const thumbnail = document.createElement('img');
			thumbnail.className = 'antilia-rich-text-images-thumbnail';
			thumbnail.alt = '';
			thumbnail.loading = 'lazy';
			thumbnail.referrerPolicy = 'no-referrer';
			thumbnail.src = image.url;
			const name = document.createElement('span');
			name.className = 'antilia-rich-text-images-name';
			name.textContent = image.name;
			option.append(thumbnail, name);
			// the focus stays in the field
			option.addEventListener('mousedown', (event) => event.preventDefault());
			option.addEventListener('mousemove', () => {
				if (active !== index) {
					activate(index);
				}
			});
			option.addEventListener('click', () => onChoose(image));
			return option;
		}));
		list.hidden = !images.length;
		if (images.length && !scrollbar && list.isConnected) {
			scrollbar = Scrollbar.attach(list, { axis: 'y', look });
		}
		scrollbar?.update();
		input.setAttribute('aria-expanded', String(images.length > 0));
		status.textContent = images.length ? '' : message;
		status.hidden = !!images.length;
		active = -1;
		activate(images.length ? 0 : -1);
	};
	const search = () => {
		if (timer !== null) {
			clearTimeout(timer);
			timer = null;
		}
		pending?.abort();
		if (destroyed) {
			return;
		}
		const controller = new AbortController();
		pending = controller;
		status.textContent = texts.findingImages;
		status.hidden = false;
		find(input.value.trim(), controller.signal).then((found) => {
			if (pending === controller && !destroyed) {
				pending = null;
				show(found, texts.noStoredImages);
			}
		}, (error: unknown) => {
			if (pending === controller && !destroyed) {
				pending = null;
				show([], error instanceof Error && error.message && error.name !== 'AbortError' ?
					error.message : texts.noStoredImages);
			}
		});
	};
	const choose = () => {
		const image = images[active] ?? images[0];
		if (image) {
			onChoose(image);
		}
	};
	input.addEventListener('input', () => {
		if (timer !== null) {
			clearTimeout(timer);
		}
		timer = setTimeout(search, delay);
	});
	input.addEventListener('keydown', (event) => {
		if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
			event.preventDefault();
			if (images.length) {
				activate(active + (event.key === 'ArrowDown' ? 1 : -1));
			}
		}
	});
	return {
		element, input, search, choose,
		destroy: () => {
			destroyed = true;
			if (timer !== null) {
				clearTimeout(timer);
			}
			pending?.abort();
			pending = null;
			scrollbar?.destroy();
			scrollbar = undefined;
		}
	};
}
