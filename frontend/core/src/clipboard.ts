/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * Copies text to the clipboard and shows a check mark where the copy was made.
 *
 * Clipboard.copy(text, iconTarget) copies the text and briefly shows a check icon over the end of
 * iconTarget, inside its box, so a cell clipping its content does not hide it. Once
 * Clipboard.install() ran, every element carrying a data-wicket-copy attribute copies its value
 * when it is clicked, or double clicked if data-wicket-copy-on="dblclick", showing the icon in
 * itself.
 */

import { getHost } from './host.js';

const SVG = 'http://www.w3.org/2000/svg';

const ICON_CLASS = 'wicket-clipboard-copied';

const HOST_CLASS = 'wicket-clipboard-host';

function writeText(text: string): Promise<void> {
	if (navigator.clipboard?.writeText) {
		return navigator.clipboard.writeText(text);
	}
	return new Promise((resolve, reject) => {
		const area = document.createElement('textarea');
		area.value = text;
		area.setAttribute('readonly', '');
		area.className = 'wicket-clipboard-buffer';
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
	let region = document.getElementById('wicket-clipboard-status');
	if (!region) {
		region = document.createElement('span');
		region.id = 'wicket-clipboard-status';
		region.className = 'wicket-clipboard-status';
		region.setAttribute('role', 'status');
		document.body.appendChild(region);
	}
	region.textContent = message;
}

function createIcon(): HTMLSpanElement {
	const icon = document.createElement('span');
	icon.className = ICON_CLASS;
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
	const icon = createIcon();
	target.classList.add(HOST_CLASS);
	target.appendChild(icon);
	window.setTimeout(() => {
		icon.remove();
		if (!target.querySelector(':scope > .' + ICON_CLASS)) {
			target.classList.remove(HOST_CLASS);
		}
	}, Clipboard.iconMillis);
}

let installed = 0;

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
	 * Copies the value of the data-wicket-copy attribute of the element an event happened in, if
	 * the event is the one the element copies on.
	 */
	onEvent(event: Event): void {
		const element = (event.target as Element | null)?.closest?.('[data-wicket-copy]');
		if (!element) {
			return;
		}
		const on = element.getAttribute('data-wicket-copy-on') || 'click';
		if (on !== event.type) {
			return;
		}
		if (event.type === 'dblclick') {
			window.getSelection?.()?.removeAllRanges();
		}
		Clipboard.copy(element.getAttribute('data-wicket-copy'), element)
			.catch((error) => getHost().log.error('Wicket.Clipboard: copying failed', error));
	},

	/**
	 * Makes the elements with a data-wicket-copy attribute copy on a click or a double click.
	 * Installing again only counts; the listeners are removed when every installation was undone.
	 *
	 * @returns undoes this installation
	 */
	install(): () => void {
		if (installed++ === 0) {
			document.addEventListener('click', Clipboard.onEvent);
			document.addEventListener('dblclick', Clipboard.onEvent);
		}
		let undone = false;
		return () => {
			if (!undone && (undone = true) && --installed === 0) {
				document.removeEventListener('click', Clipboard.onEvent);
				document.removeEventListener('dblclick', Clipboard.onEvent);
			}
		};
	}
};
