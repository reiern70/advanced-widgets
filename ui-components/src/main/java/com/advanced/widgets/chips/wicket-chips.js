/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The chips of the chosen values of a drop-down or an autocomplete: a label and an x removing the
 * value. Wicket.Chips.create(text, removeLabel) returns a span.wicket-chip; a click on its x is
 * recognized with Wicket.Chips.removeTarget(event), which returns the chip whose x was clicked.
 */
;(function (undefined) {
	'use strict';

	if (typeof(Wicket) === "undefined") {
		window.Wicket = { };
	}

	if (typeof(Wicket.Chips) !== "undefined") {
		return;
	}

	Wicket.Chips = {

		/**
		 * @param text the text of the chip
		 * @param removeLabel the label of its x for screen readers, '{0}' replaced by the text;
		 *            no x when empty
		 * @return the chip
		 */
		create : function (text, removeLabel) {
			const chip = document.createElement('span');
			chip.className = 'wicket-chip';
			const label = document.createElement('span');
			label.className = 'wicket-chip-text';
			label.textContent = text;
			chip.appendChild(label);
			if (removeLabel) {
				const remove = document.createElement('span');
				remove.className = 'wicket-chip-remove';
				remove.setAttribute('role', 'button');
				remove.setAttribute('aria-label', removeLabel.replace('{0}', text));
				remove.title = remove.getAttribute('aria-label');
				chip.appendChild(remove);
			}
			return chip;
		},

		/**
		 * @param text the text, such as '+2 more'
		 * @return a chip without an x
		 */
		more : function (text) {
			const chip = Wicket.Chips.create(text, null);
			chip.classList.add('wicket-chip-more');
			return chip;
		},

		/**
		 * @return the chip whose x the event happened on, null if none
		 */
		removeTarget : function (event) {
			const remove = event.target.closest && event.target.closest('.wicket-chip-remove');
			return remove ? remove.closest('.wicket-chip') : null;
		}
	};
})();
