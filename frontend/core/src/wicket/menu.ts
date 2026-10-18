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
