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
 * antilia-form-version.js: registers Wicket.FormVersion for FormVersionBehavior, which writes the
 * version of a form on its tag, data-antilia-form-version, calls Wicket.FormVersion.watch() once
 * the page is ready, and Wicket.FormVersion.set(id, version) when an Ajax answer raised it.
 *
 * A form's submit carries the version the form had when the user acted, as the parameter
 * antilia-form-version:<markup id of the form>, and the server refuses a version it already took.
 * The version is read at the click, not when the request leaves: Wicket's Ajax serializes a queued
 * submit's form only when it sends it, after the previous answer has been applied, so a second
 * click queued behind the first would otherwise send the new version and pass.
 */

import { onWicketTopic, useWicketHost, wicket } from './host.js';
import { asParameters, type AjaxAttributes } from './send-state.js';

/** The attribute of a form's tag holding its version: FormVersionBehavior.ATTRIBUTE. */
export const ATTRIBUTE = 'data-antilia-form-version';

/**
 * Followed by the markup id of the form, the parameter carrying the version:
 * FormVersionBehavior.PARAMETER_PREFIX.
 */
export const PARAMETER_PREFIX = 'antilia-form-version:';

/** The attribute marking the hidden inputs this script writes for a normal submit. */
const INPUT_ATTRIBUTE = 'data-antilia-form-version-input';

/**
 * @param element where the user acted, or the form submitted
 * @returns the nearest element around it carrying a version, the element itself included: the
 *          form the server processes, a nested one too; null for none
 */
export function versionedForm(element: Element | null | undefined): HTMLElement | null {
	return element?.closest<HTMLElement>('[' + ATTRIBUTE + ']') ?? null;
}

/**
 * @param component the component of an Ajax request, as Wicket's attributes give it: its markup
 *        id, or the element
 * @returns the element, null for none
 */
function elementOf(component: unknown): Element | null {
	if (typeof component === 'string') {
		return document.getElementById(component);
	}
	return component instanceof Element ? component : null;
}

/**
 * @param form an element carrying a version
 * @returns the parameter carrying it, null when the form has no markup id or no version
 */
function parameterOf(form: HTMLElement): { name: string; value: string } | null {
	const version = form.getAttribute(ATTRIBUTE);
	return form.id && version !== null ? { name: PARAMETER_PREFIX + form.id, value: version } :
		null;
}

/**
 * @param root the form element the browser submits
 * @returns the hidden inputs this script wrote in it for a normal submit
 */
function hiddenInputs(root: Element): HTMLInputElement[] {
	return Array.from(root.querySelectorAll<HTMLInputElement>('input[' + INPUT_ATTRIBUTE + ']'));
}

/**
 * Makes an Ajax request submitting a form carry the version the form has now, as the user acts:
 * called with the attributes of the request at /ajax/call/init, which Wicket's Ajax publishes
 * within the event's handler, before it throttles or queues the request.
 *
 * @param attrs the attributes of this request alone: Wicket's Ajax copies them for each, but not
 *        their extra parameters, which this replaces rather than adds to
 * @returns whether the request carries a version: not when it submits no form, nor when the form
 *          has none
 */
export function stamp(attrs: AjaxAttributes | null | undefined): boolean {
	if (!attrs || typeof attrs !== 'object' || typeof attrs.f !== 'string' || attrs.f === '') {
		return false;
	}
	const root = document.getElementById(attrs.f);
	const form = versionedForm(elementOf(attrs.c) ?? root);
	const parameter = form ? parameterOf(form) : null;
	if (!parameter) {
		return false;
	}
	if (root) {
		// what a normal submit left would go with the form's fields, older than the version
		hiddenInputs(root).forEach((input) => input.remove());
	}
	attrs.ep = [...asParameters(attrs.ep).filter(({ name }) => name !== parameter.name),
		parameter];
	return true;
}

/**
 * Makes a normal submit, one no Ajax handler took, carry the version the form has now, in a
 * hidden input of the form the browser submits.
 *
 * @param event a submit event, once its handlers ran
 * @returns whether the submit carries a version
 */
export function stampSubmit(event: Event): boolean {
	const root = event.target;
	if (event.defaultPrevented || !(root instanceof HTMLFormElement)) {
		return false;
	}
	const submitter = (event as SubmitEvent).submitter ?? null;
	const form = versionedForm(submitter ?? root);
	const parameter = form ? parameterOf(form) : null;
	if (!parameter) {
		return false;
	}
	let input = hiddenInputs(root).find((each) => each.name === parameter.name);
	if (!input) {
		input = document.createElement('input');
		input.type = 'hidden';
		input.name = parameter.name;
		input.setAttribute(INPUT_ATTRIBUTE, '');
		root.appendChild(input);
	}
	input.value = parameter.value;
	return true;
}

let stopListening: (() => void) | null = null;

/**
 * Starts stamping the submits of the page's versioned forms: the Ajax ones at /ajax/call/init, the
 * normal ones as their submit event reaches the document. Once per page.
 */
export function watch(): void {
	if (stopListening) {
		return;
	}
	const stopInit = onWicketTopic<AjaxAttributes>('AJAX_CALL_INIT', '/ajax/call/init', stamp);
	// on the way up, once Wicket's Ajax had its say: a submit it took is prevented
	document.addEventListener('submit', stampSubmit);
	stopListening = () => {
		stopInit();
		document.removeEventListener('submit', stampSubmit);
	};
}

/**
 * Sets the version of a form, as an Ajax answer raising it says.
 *
 * @param id the markup id of the form
 * @param version its new version
 */
export function set(id: string, version: number | string): void {
	document.getElementById(id)?.setAttribute(ATTRIBUTE, String(version));
}

/**
 * Forgets that the page is watched, for the tests.
 */
export function unwatch(): void {
	stopListening?.();
	stopListening = null;
}

/** Wicket.FormVersion: what FormVersionBehavior calls. */
export const WicketFormVersion = {
	watch,
	set,
	/** The attribute holding a form's version. */
	attribute: ATTRIBUTE,
	/** Followed by a form's markup id, the parameter carrying its version. */
	parameterPrefix: PARAMETER_PREFIX
};

const Wicket = wicket();
if (Wicket.FormVersion === undefined) {
	useWicketHost();
	Wicket.FormVersion = WicketFormVersion;
}
