/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * antilia-tabs.js: registers Wicket.Tabs for the Tabs of ui-components, which calls
 * Wicket.Tabs.attach(id, options, onSelect, onClose); onSelect and onClose, when the server is
 * told, send it via Ajax the tab the user selected or closed.
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
	 * @returns the tabs, null when there is no such element
	 */
	attach(id: string, options: TabsOptions = {}, onSelect?: ((tab: string) => void) | null,
		onClose?: ((tab: string) => void) | null): Tabs | null {
		return Tabs.attach(id, {
			...options,
			onSelect: onSelect ?? options.onSelect,
			onClose: onClose ?? options.onClose
		});
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
