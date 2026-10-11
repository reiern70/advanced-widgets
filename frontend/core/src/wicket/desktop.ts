/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * antilia-desktop.js: registers Wicket.Desktop for the Desktop of ui-components, which calls
 * Wicket.Desktop.attach(id, options, onChange); onChange, when the server keeps the states of the
 * windows, sends them via Ajax as JSON, with the side of the taskbar and whether the desktop fills
 * the page.
 */

import { Desktop, type DesktopOptions, type WindowState } from '../desktop.js';
import { useWicketHost, wicket } from './host.js';

const WicketDesktop = {

	/**
	 * Makes a desktop of an element.
	 *
	 * @param id the markup id of the desktop
	 * @param options the side of the taskbar, whether the desktop fills the page, and the texts
	 * @param onChange the Ajax callback of the server, given the states of the windows as JSON,
	 *        the side of the taskbar and whether the desktop fills the page
	 * @returns the desktop, null when there is no such element
	 */
	attach(id: string, options: DesktopOptions = {},
		onChange?: (states: string, taskbar: string, expanded: string) => void): Desktop | null {
		const desktop: Desktop | null = Desktop.attach(id, {
			...options,
			onChange: onChange ? (states: Record<string, WindowState>) =>
				onChange(JSON.stringify(states), desktop?.position() ?? '',
					String(desktop?.isExpanded() ?? false)) : options.onChange
		});
		return desktop;
	},

	/**
	 * @param id the markup id of the desktop
	 * @returns the desktop, if there is one
	 */
	of(id: string): Desktop | null {
		return Desktop.of(id);
	}
};

const Wicket = wicket();
if (Wicket.Desktop === undefined) {
	useWicketHost();
	Wicket.Desktop = WicketDesktop;
}
