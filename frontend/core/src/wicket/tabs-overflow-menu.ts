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
 * antilia-tabs-overflow-menu.js: registers Wicket.TabsOverflowMenu for the
 * TabsOverflowMenuBehavior of ui-components, which calls Wicket.TabsOverflowMenu.attach(id,
 * options) with the markup id of the tabs. Choosing a tab clicks it, so the tabs tell the server
 * as they do for a click.
 */

import { TabsOverflowMenu, type TabsOverflowMenuOptions } from '../tabs-overflow-menu.js';
import { useWicketHost, wicket } from './host.js';

const WicketTabsOverflowMenu = {

	/**
	 * Adds the menu of all tabs to tabs.
	 *
	 * @param id the markup id of the element of the tabs
	 * @param options the label of the button
	 * @returns the menu, null when there is no such element
	 */
	attach(id: string, options: TabsOverflowMenuOptions = {}): TabsOverflowMenu | null {
		return TabsOverflowMenu.attach(id, options);
	},

	/**
	 * @param id the markup id of the element of the tabs
	 * @returns the menu of all tabs, if there is one
	 */
	of(id: string): TabsOverflowMenu | null {
		return TabsOverflowMenu.of(id);
	}
};

const Wicket = wicket();
if (Wicket.TabsOverflowMenu === undefined) {
	useWicketHost();
	Wicket.TabsOverflowMenu = WicketTabsOverflowMenu;
}
