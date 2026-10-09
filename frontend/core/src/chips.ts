/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The chips of the chosen values of a drop-down or an autocomplete: a label and an x removing the
 * value. Chips.create(text, removeLabel) returns a span.antilia-chip; a click on its x is recognized
 * with Chips.removeTarget(event), which returns the chip whose x was clicked.
 */

export const Chips = {

	/**
	 * @param text the text of the chip
	 * @param removeLabel the label of its x for screen readers, '{0}' replaced by the text; no x
	 *            when empty
	 * @returns the chip
	 */
	create(text: string, removeLabel?: string | null): HTMLSpanElement {
		const chip = document.createElement('span');
		chip.className = 'antilia-chip';
		const label = document.createElement('span');
		label.className = 'antilia-chip-text';
		label.textContent = text;
		chip.appendChild(label);
		if (removeLabel) {
			const remove = document.createElement('span');
			remove.className = 'antilia-chip-remove';
			remove.setAttribute('role', 'button');
			remove.setAttribute('aria-label', removeLabel.replace('{0}', text));
			remove.title = remove.getAttribute('aria-label') ?? '';
			chip.appendChild(remove);
		}
		return chip;
	},

	/**
	 * @param text the text, such as '+2 more'
	 * @returns a chip without an x
	 */
	more(text: string): HTMLSpanElement {
		const chip = Chips.create(text, null);
		chip.classList.add('antilia-chip-more');
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
