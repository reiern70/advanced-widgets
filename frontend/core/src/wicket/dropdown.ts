/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * antilia-dropdown.js: registers Wicket.Dropdown for DropdownBehavior, which calls
 * Wicket.Dropdown.attach(id, options); the theme editor calls Wicket.Dropdown.of(select). The list
 * gets the scroll bars of Wicket.Scrollbar, when the page loaded them.
 */

import { Dropdown } from '../dropdown.js';
import type { ScrollbarHandle, ScrollbarOptions } from '../scrollbar.js';
import { useWicketHost, wicket } from './host.js';

interface ScrollbarGlobal {
	attach(element: HTMLElement, options: ScrollbarOptions): ScrollbarHandle | undefined;
}

const Wicket = wicket();
if (Wicket.Dropdown === undefined) {
	useWicketHost();
	Dropdown.attachScrollbar = (list) =>
		(window.Wicket?.Scrollbar as ScrollbarGlobal | undefined)?.attach(list, { axis: 'y' });
	Wicket.Dropdown = Dropdown;
}
