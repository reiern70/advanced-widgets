/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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
 * Changes not saved, which closing a window of the desktop or a tab loses: telling whether a part
 * of the page has them, and asking the user before losing them, in a dialog of the markup of
 * modal.ts that its style sheets, modal.css and button.css, show, in the look of the part.
 *
 * A part has changes when a field in it holds another value than the one it was rendered with,
 * see isFieldModified() of modal.ts, or when the page says so with data-modified on it or on an
 * element in it: "true" or empty for changes, "false" for none. The fields and the marks in an
 * element of the part marked data-modified="false" do not count, unless in one marked
 * data-modified="auto" in it.
 */

import { buttonClasses } from './button.js';
import { ANTILIA_LOOK, partClasses, type Look } from './look.js';
import { isFieldModified, ModalDialog } from './modal.js';

/** The fields whose changes closing a part of the page may lose. */
const FIELDS = 'input, textarea, select';

/** The attribute saying whether a part of the page, or an element in it, has changes not saved. */
export const MODIFIED_ATTRIBUTE = 'data-modified';

/**
 * @returns how the region of a node counts its changes: "false" when it is in an element marked so,
 *          up to the part of the page, "auto" when in one marked "auto" in it, null when in neither
 */
function regionOf(node: Element | null, part: Element): string | null {
	for (let current = node; current; current = current.parentElement) {
		const value = current.getAttribute(MODIFIED_ATTRIBUTE);
		if (value === 'false' || value === 'auto') {
			return value;
		}
		if (current === part) {
			return null;
		}
	}
	return null;
}

/**
 * @param element the part of the page, a window or the panel of a tab
 * @returns the fields of the part the user changed, empty when the page says it has changes of its
 *          own; null when it has none. In the part, what an element marked "false" holds does not
 *          count, its fields and its marks, unless in one marked "auto" in it: the panels of tabs
 *          in a tab marked "false", or an editor in it, tell of their changes to their own tabs
 *          alone.
 */
export function modifiedFields(element: Element): Element[] | null {
	const marks = [element, ...Array.from(element.querySelectorAll('[' + MODIFIED_ATTRIBUTE +
		']'))];
	if (marks.some((mark) => ['', 'true'].includes(mark.getAttribute(MODIFIED_ATTRIBUTE) ??
		'false') && regionOf(mark === element ? null : mark.parentElement, element) !== 'false')) {
		return [];
	}
	const fields = Array.from(element.querySelectorAll(FIELDS)).filter((field) =>
		regionOf(field, element) !== 'false' && isFieldModified(field));
	return fields.length ? fields : null;
}

/** What the dialog asking before changes are lost says. */
export interface DiscardTexts {
	/** The title of the dialog. */
	title: string;
	/** What the dialog says. */
	text: string;
	/** The button losing the changes. */
	discard: string;
	/** The button keeping them. */
	keep: string;
}

/** A dialog asking before changes are lost, while it is open. */
export interface DiscardQuestion {
	/** Resolves to whether the user chose to lose the changes. */
	readonly answer: Promise<boolean>;
	/** Takes the dialog away, answering no. */
	cancel(): void;
}

/**
 * Asks the user whether to lose changes not saved, in a dialog of the markup of modal.ts appended to
 * the body of the page. Its buttons are marked data-confirm="keep" and data-confirm="discard";
 * Escape keeps the changes too.
 *
 * @param texts what the dialog says
 * @param className the classes of the wrapper of the dialog, next to those of the modal's theme
 * @param prefix the start of the ids of the title and the text of the dialog
 * @param look the look of the dialog, whose classes its parts get; Antilia's when left out
 * @returns the dialog while it is open
 */
export function askToDiscard(texts: DiscardTexts, className: string, prefix: string,
	look: Look = ANTILIA_LOOK): DiscardQuestion {
	let finish: (value: boolean) => void = () => undefined;
	const answer = new Promise<boolean>((resolve) => {
		const wrapper = document.createElement('div');
		wrapper.className = partClasses(look, 'dialog-theme') + ' ' + className;
		const overlay = document.createElement('div');
		overlay.className = partClasses(look, 'dialog-overlay');
		const dialog = document.createElement('div');
		dialog.className = partClasses(look, 'dialog');
		dialog.setAttribute('role', 'alertdialog');
		dialog.setAttribute('aria-modal', 'true');
		dialog.setAttribute('aria-labelledby', prefix + '-title');
		dialog.setAttribute('aria-describedby', prefix + '-text');
		const content = document.createElement('div');
		content.className = partClasses(look, 'dialog-content');
		const header = document.createElement('div');
		header.className = partClasses(look, 'dialog-header');
		const heading = document.createElement('h1');
		heading.className = partClasses(look, 'dialog-title');
		heading.id = prefix + '-title';
		heading.textContent = texts.title;
		header.append(heading);
		const body = document.createElement('div');
		body.className = partClasses(look, 'dialog-body');
		const text = document.createElement('p');
		text.id = prefix + '-text';
		text.textContent = texts.text;
		body.append(text);
		const footer = document.createElement('div');
		footer.className = partClasses(look, 'dialog-footer');
		const keep = dialogButton(texts.keep, 'secondary', 'keep', look);
		const discard = dialogButton(texts.discard, 'danger', 'discard', look);
		footer.append(keep, discard);
		content.append(header, body, footer);
		dialog.append(content);
		overlay.append(dialog);
		wrapper.append(overlay);
		document.body.append(wrapper);
		let modal: ModalDialog | null = null;
		let done = false;
		finish = (value: boolean) => {
			if (done) {
				return;
			}
			done = true;
			modal?.destroy();
			wrapper.remove();
			resolve(value);
		};
		keep.addEventListener('click', () => finish(false));
		discard.addEventListener('click', () => finish(true));
		modal = new ModalDialog(overlay, { size: 'small', foldable: false,
			onClose: () => finish(false), look });
	});
	return { answer, cancel: () => finish(false) };
}

function dialogButton(label: string, variant: 'secondary' | 'danger', name: string,
	look: Look): HTMLButtonElement {
	const element = document.createElement('button');
	element.type = 'button';
	// in the look of the dialog, as the buttons rendered with it
	element.className = buttonClasses({ variant, size: 'small', look });
	element.dataset.confirm = name;
	const text = document.createElement('span');
	text.className = partClasses(look, 'button-label');
	text.textContent = label;
	element.append(text);
	return element;
}
