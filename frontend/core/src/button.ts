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
 * The buttons of the theme: their look is the style sheet button.css, the same for Wicket's
 * ButtonBehavior and AjaxActionButton and for the React and Svelte Button, or the classes of
 * another look, such as Bootstrap's (see look.ts). A button is an element with these classes;
 * this module names them, and marks a button busy with what it started, a download for example.
 */

import { ANTILIA_LOOK, type Look, lookClasses } from './look.js';

/** What a button stands for, which sets its colors. */
export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';

/** How big a button is. */
export type ButtonSize = 'small' | 'medium' | 'large';

/** The variants, in the order a demo shows them. */
export const BUTTON_VARIANTS: readonly ButtonVariant[] = ['primary', 'secondary', 'ghost', 'danger'];

/** The sizes, from the smallest. */
export const BUTTON_SIZES: readonly ButtonSize[] = ['small', 'medium', 'large'];

/**
 * @returns the CSS classes of a button in a look, the Antilia look by default: there ant-button,
 * its variant's and its size's, and ant-button--icon-only for a button showing its icon only;
 * secondary and medium by default
 */
export function buttonClasses(options: { variant?: ButtonVariant; size?: ButtonSize;
	iconOnly?: boolean; className?: string; look?: Look } = {}): string {
	const { variant = 'secondary', size = 'medium', iconOnly = false, className,
		look = ANTILIA_LOOK } = options;
	return [lookClasses(look, 'button', `button-${variant}`, `button-${size}`,
		iconOnly && 'button-icon-only'), className ?? ''].filter(Boolean).join(' ');
}

/** The class of a button busy with what it started, a download for example. */
export const BUTTON_BUSY_CLASS = 'ant-button--busy';

/**
 * Marks a button busy with what it started, or done with it. Busy, it shows a spinner instead of
 * its icon, or before its text, and screen readers hear it is busy (aria-busy); clicks on it are
 * ignored once guardBusyButtons() ran, while it keeps the focus. The class is a state, not a look:
 * the button has it in every look.
 */
export function setButtonBusy(button: Element | null | undefined, busy: boolean): void {
	if (!button) {
		return;
	}
	button.classList.toggle(BUTTON_BUSY_CLASS, busy);
	if (busy) {
		button.setAttribute('aria-busy', 'true');
	} else {
		button.removeAttribute('aria-busy');
	}
}

/**
 * @returns whether a button is busy with what it started
 */
export function isButtonBusy(button: Element | null | undefined): boolean {
	return !!button?.classList.contains(BUTTON_BUSY_CLASS);
}

const guarded = new WeakMap<Document, () => void>();

/**
 * Makes the busy buttons of a document ignore clicks, by the mouse or the keyboard: a click on one
 * is stopped before any listener of the button hears it. Once per document: the next calls return
 * the same function, which removes the guard.
 */
export function guardBusyButtons(doc: Document = document): () => void {
	const existing = guarded.get(doc);
	if (existing) {
		return existing;
	}
	const guard = (event: Event): void => {
		const target = event.target instanceof Element ? event.target : null;
		if (isButtonBusy(target?.closest('.' + BUTTON_BUSY_CLASS))) {
			event.preventDefault();
			event.stopImmediatePropagation();
		}
	};
	doc.addEventListener('click', guard, true);
	const remove = (): void => {
		doc.removeEventListener('click', guard, true);
		guarded.delete(doc);
	};
	guarded.set(doc, remove);
	return remove;
}
