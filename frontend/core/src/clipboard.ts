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
 * Copies text to the clipboard and shows a check mark where the copy was made.
 *
 * Clipboard.copy(text, iconTarget) copies the text and briefly shows a check icon over the end of
 * iconTarget, inside its box, so a cell clipping its content does not hide it.
 *
 * An element carrying a data-antilia-copy attribute copies its value when it is clicked, or double
 * clicked if data-antilia-copy-on="dblclick", showing the icon in itself, once the page said it
 * may: Clipboard.attach(element) for the element itself, as Wicket's ClipboardCopyBehavior does,
 * and Clipboard.attachWithin(container) for the elements inside a container, markup a template
 * rendered for example. Other elements carrying the attribute, in HTML a user wrote say, copy
 * nothing: no markup of the page chooses on its own what lands on the clipboard.
 * Clipboard.install() makes every element of the document carrying the attribute copy, for a page
 * whose markup is all its own.
 */

import { getHost } from './host.js';
import { lookOf, partClasses } from './look.js';

const SVG = 'http://www.w3.org/2000/svg';

const ICON_CLASS = 'antilia-clipboard-copied';

const HOST_CLASS = 'antilia-clipboard-host';

function writeText(text: string): Promise<void> {
	if (navigator.clipboard?.writeText) {
		return navigator.clipboard.writeText(text);
	}
	return new Promise((resolve, reject) => {
		const area = document.createElement('textarea');
		area.value = text;
		area.setAttribute('readonly', '');
		area.className = 'antilia-clipboard-buffer';
		document.body.appendChild(area);
		area.select();
		const copied = typeof document.execCommand === 'function' && document.execCommand('copy');
		area.remove();
		if (copied) {
			resolve();
		} else {
			reject(new Error('copying is not supported'));
		}
	});
}

function announce(message: string): void {
	let region = document.getElementById('antilia-clipboard-status');
	if (!region) {
		region = document.createElement('span');
		region.id = 'antilia-clipboard-status';
		region.className = partClasses(lookOf(document.body), 'clipboard-status');
		region.setAttribute('role', 'status');
		document.body.appendChild(region);
	}
	region.textContent = message;
}

function createIcon(classes: string): HTMLSpanElement {
	const icon = document.createElement('span');
	icon.className = classes;
	icon.setAttribute('aria-hidden', 'true');
	const svg = document.createElementNS(SVG, 'svg');
	svg.setAttribute('viewBox', '0 0 24 24');
	const path = document.createElementNS(SVG, 'path');
	path.setAttribute('d', 'M4 12l5 5L20 6');
	path.setAttribute('fill', 'none');
	path.setAttribute('stroke', 'currentColor');
	path.setAttribute('stroke-width', '3');
	path.setAttribute('stroke-linecap', 'round');
	path.setAttribute('stroke-linejoin', 'round');
	svg.appendChild(path);
	icon.appendChild(svg);
	return icon;
}

function showIcon(target: Element | null | undefined): void {
	if (!target) {
		return;
	}
	target.querySelector(':scope > .' + ICON_CLASS)?.remove();
	const look = lookOf(target);
	const icon = createIcon(partClasses(look, 'clipboard-copied'));
	target.classList.add(...partClasses(look, 'clipboard-host').split(' '));
	target.appendChild(icon);
	window.setTimeout(() => {
		icon.remove();
		if (!target.querySelector(':scope > .' + ICON_CLASS)) {
			target.classList.remove(HOST_CLASS);
		}
	}, Clipboard.iconMillis);
}

const SELECTOR = '[data-antilia-copy]';

/** The elements that copy their own data-antilia-copy. */
const attached = new WeakSet<Element>();

/** The containers whose elements carrying data-antilia-copy copy. */
const containers = new WeakSet<Element>();

/** How many installations of the whole document there are. */
let installed = 0;

/** How many installations and attachments listen to the document. */
let listening = 0;

function listen(): () => void {
	if (listening++ === 0) {
		document.addEventListener('click', Clipboard.onEvent);
		document.addEventListener('dblclick', Clipboard.onEvent);
	}
	let undone = false;
	return () => {
		if (!undone && (undone = true) && --listening === 0) {
			document.removeEventListener('click', Clipboard.onEvent);
			document.removeEventListener('dblclick', Clipboard.onEvent);
		}
	};
}

/** @returns whether an element carrying data-antilia-copy may copy */
function mayCopy(element: Element): boolean {
	if (installed > 0 || attached.has(element)) {
		return true;
	}
	for (let parent = element.parentElement; parent; parent = parent.parentElement) {
		if (containers.has(parent)) {
			return true;
		}
	}
	return false;
}

export const Clipboard = {

	/** How long the check icon stays, in milliseconds. */
	iconMillis: 1500,

	/** The text read to screen readers after a copy. */
	copiedMessage: 'Copied',

	/**
	 * Copies text to the clipboard and shows the check icon at the end of iconTarget.
	 *
	 * @param text the text to copy
	 * @param iconTarget where to show the icon, optional
	 * @returns resolved once the text is copied
	 */
	copy(text: unknown, iconTarget?: Element | null): Promise<void> {
		return writeText(String(text)).then(() => {
			showIcon(iconTarget);
			announce(Clipboard.copiedMessage);
		});
	},

	/**
	 * Copies the value of the data-antilia-copy attribute of the element an event happened in, if
	 * the event is the one the element copies on and the element may copy: the closest one that
	 * may, past those that may not.
	 */
	onEvent(event: Event): void {
		let element = (event.target as Element | null)?.closest?.(SELECTOR) ?? null;
		while (element && !mayCopy(element)) {
			element = element.parentElement?.closest(SELECTOR) ?? null;
		}
		if (!element) {
			return;
		}
		const on = element.getAttribute('data-antilia-copy-on') || 'click';
		if (on !== event.type) {
			return;
		}
		if (event.type === 'dblclick') {
			window.getSelection?.()?.removeAllRanges();
		}
		Clipboard.copy(element.getAttribute('data-antilia-copy'), element)
			.catch((error) => getHost().log.error('Wicket.Clipboard: copying failed', error));
	},

	/**
	 * Makes an element carrying a data-antilia-copy attribute copy on a click or a double click.
	 *
	 * @param element the element, nothing when null
	 * @returns undoes this attachment
	 */
	attach(element: Element | null | undefined): () => void {
		if (!element) {
			return () => {};
		}
		attached.add(element);
		const unlisten = listen();
		return () => {
			attached.delete(element);
			unlisten();
		};
	},

	/**
	 * Makes the elements carrying a data-antilia-copy attribute inside a container copy on a click
	 * or a double click, those rendered later too: the markup of the page's templates, for
	 * example. Only for a container whose markup is the page's own, not HTML a user wrote.
	 *
	 * @param container the container, nothing when null
	 * @returns undoes this attachment
	 */
	attachWithin(container: Element | null | undefined): () => void {
		if (!container) {
			return () => {};
		}
		containers.add(container);
		const unlisten = listen();
		return () => {
			containers.delete(container);
			unlisten();
		};
	},

	/**
	 * Makes every element of the document carrying a data-antilia-copy attribute copy on a click
	 * or a double click: only for a page whose markup is all its own, never one showing HTML a user
	 * wrote, which could choose what lands on the clipboard. Installing again only counts; the
	 * listeners are removed when every installation was undone.
	 *
	 * @returns undoes this installation
	 */
	install(): () => void {
		installed++;
		const unlisten = listen();
		let undone = false;
		return () => {
			if (!undone && (undone = true)) {
				installed--;
				unlisten();
			}
		};
	}
};
