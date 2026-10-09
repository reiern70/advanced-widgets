/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * antilia-chips.js: registers Wicket.Chips, the chips of the chosen values of the drop-downs and
 * the autocompletes, for the scripts still outside the core.
 */

import { Chips } from '../chips.js';
import { wicket } from './host.js';

const Wicket = wicket();
if (Wicket.Chips === undefined) {
	Wicket.Chips = Chips;
}
