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
 * The chips of the chosen values of a drop-down or an autocomplete: a label and an x removing the
 * value. Chips.create(text, removeLabel) returns a span.antilia-chip; a click on its x is recognized
 * with Chips.removeTarget(event), which returns the chip whose x was clicked.
 */

import { ANTILIA_LOOK, partClasses, type Look } from './look.js';

export const Chips = {

	/**
	 * @param text the text of the chip
	 * @param removeLabel the label of its x for screen readers, '{0}' replaced by the text; no x
	 *            when empty
	 * @param look the look of the chip
	 * @returns the chip
	 */
	create(text: string, removeLabel?: string | null, look: Look = ANTILIA_LOOK): HTMLSpanElement {
		const chip = document.createElement('span');
		chip.className = partClasses(look, 'chip');
		const label = document.createElement('span');
		label.className = partClasses(look, 'chip-text');
		label.textContent = text;
		chip.appendChild(label);
		if (removeLabel) {
			const remove = document.createElement('span');
			remove.className = partClasses(look, 'chip-remove');
			remove.setAttribute('role', 'button');
			remove.setAttribute('aria-label', removeLabel.replace('{0}', text));
			remove.title = remove.getAttribute('aria-label') ?? '';
			chip.appendChild(remove);
		}
		return chip;
	},

	/**
	 * @param text the text, such as '+2 more'
	 * @param look the look of the chip
	 * @returns a chip without an x
	 */
	more(text: string, look: Look = ANTILIA_LOOK): HTMLSpanElement {
		const chip = Chips.create(text, null, look);
		chip.className = partClasses(look, 'chip', 'chip-more');
		return chip;
	},

	/**
	 * @returns the chip whose x the event happened on, null if none
	 */
	removeTarget(event: Event): HTMLElement | null {
		const remove = (event.target as Element | null)?.closest?.('.antilia-chip-remove');
		return remove ? remove.closest<HTMLElement>('.antilia-chip') : null;
	}
};
