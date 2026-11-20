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
 * antilia-tabs.js: registers Wicket.Tabs for the Tabs of ui-components, which calls
 * Wicket.Tabs.attach(id, options, onSelect, onClose, onLoad); onSelect and onClose, when the
 * server is told, send it via Ajax the tab the user selected or closed, and onLoad a tab selected
 * whose panel the server creates now, which tells the server the tab selected too.
 */

import { Tabs, type TabsOptions } from '../tabs.js';
import { useWicketHost, wicket } from './host.js';

const WicketTabs = {

	/**
	 * Makes tabs of an element.
	 *
	 * @param id the markup id of the tabs
	 * @param options the orientation, the design, the tab selected and the texts
	 * @param onSelect the Ajax callback of the server given the name of the tab the user selected
	 * @param onClose the Ajax callback of the server given the name of the tab the user closed
	 * @param onLoad the Ajax callback of the server given the name of a tab selected whose panel it
	 *        renders now
	 * @returns the tabs, null when there is no such element
	 */
	attach(id: string, options: TabsOptions = {}, onSelect?: ((tab: string) => void) | null,
		onClose?: ((tab: string) => void) | null,
		onLoad?: ((tab: string) => void) | null): Tabs | null {
		const select = onSelect ?? options.onSelect;
		const tabs: Tabs | null = Tabs.attach(id, {
			...options,
			// a tab loading tells the server it is selected, in the same request
			onSelect: select && ((tab: string) => {
				if (!tabs?.isLoading(tab)) {
					select(tab);
				}
			}),
			onClose: onClose ?? options.onClose,
			onLoad: onLoad ?? options.onLoad
		});
		return tabs;
	},

	/**
	 * @param id the markup id of the tabs
	 * @returns the tabs, if there are
	 */
	of(id: string): Tabs | null {
		return Tabs.of(id);
	}
};

const Wicket = wicket();
if (Wicket.Tabs === undefined) {
	useWicketHost();
	Wicket.Tabs = WicketTabs;
}
