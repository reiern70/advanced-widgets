/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * antilia-menu.js: registers Wicket.Menu for the Menu of ui-components, which calls
 * Wicket.Menu.attach(id, options). The links of the items call the server themselves.
 */

import { Menu, type MenuOptions } from '../menu.js';
import { useWicketHost, wicket } from './host.js';

const WicketMenu = {

	/**
	 * Makes a menu of an element.
	 *
	 * @param id the markup id of the menu
	 * @param options the orientation, the design, how the submenus open and the accessible name
	 * @returns the menu, null when there is no such element
	 */
	attach(id: string, options: MenuOptions = {}): Menu | null {
		return Menu.attach(id, options);
	},

	/**
	 * @param id the markup id of the menu
	 * @returns the menu, if there is one
	 */
	of(id: string): Menu | null {
		return Menu.of(id);
	}
};

const Wicket = wicket();
if (Wicket.Menu === undefined) {
	useWicketHost();
	Wicket.Menu = WicketMenu;
}
