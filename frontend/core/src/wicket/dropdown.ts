/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * wicket-dropdown.js: registers Wicket.Dropdown for DropdownBehavior, which calls
 * Wicket.Dropdown.attach(id, options); the theme editor calls Wicket.Dropdown.of(select).
 */

import { Dropdown } from '../dropdown.js';
import { useWicketHost, wicket } from './host.js';

const Wicket = wicket();
if (Wicket.Dropdown === undefined) {
	useWicketHost();
	Wicket.Dropdown = Dropdown;
}
