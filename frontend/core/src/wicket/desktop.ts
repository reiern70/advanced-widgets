/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * antilia-desktop.js: registers Wicket.Desktop for the Desktop of ui-components, which calls
 * Wicket.Desktop.attach(id, options, onChange, onWindow); onChange, when the server keeps the
 * states of the windows, sends them via Ajax as JSON, with the side of the taskbar, whether the
 * desktop fills the page, the panes as JSON and the pane shown. onWindow, when the server has types
 * of windows, asks it to add a window of a type to a pane, and to take away one it added, which the
 * user closed.
 */

import { Desktop, type DesktopOptions, type WindowState } from '../desktop.js';
import { useWicketHost, wicket } from './host.js';

/** The attribute of the windows the server added of a type, which it takes away when closed. */
const DISPOSABLE = 'data-disposable';

const WicketDesktop = {

	/**
	 * Makes a desktop of an element.
	 *
	 * @param id the markup id of the desktop
	 * @param options the side of the taskbar, whether the desktop fills the page, the panes, the
	 *        types of windows and the texts
	 * @param onChange the Ajax callback of the server, given the states of the windows as JSON,
	 *        the side of the taskbar, whether the desktop fills the page, the panes as JSON and
	 *        the id of the pane shown
	 * @param onWindow the Ajax callback of the server, given "open", a type and a pane to add a
	 *        window, or "dispose" and the id of one it added, which the user closed
	 * @returns the desktop, null when there is no such element
	 */
	attach(id: string, options: DesktopOptions = {},
		onChange?: (states: string, taskbar: string, expanded: string, panes: string,
			pane: string) => void,
		onWindow?: (action: string, type: string, pane: string, window: string) => void):
		Desktop | null {
		const desktop: Desktop | null = Desktop.attach(id, {
			...options,
			onChange: onChange ? (states: Record<string, WindowState>) =>
				onChange(JSON.stringify(states), desktop?.position() ?? '',
					String(desktop?.isExpanded() ?? false), JSON.stringify(desktop?.panes() ?? []),
					desktop?.activePane() ?? '') : options.onChange,
			onOpenWindow: onWindow ? (type: string, pane: string) =>
				onWindow('open', type, pane, '') : options.onOpenWindow,
			onClose: onWindow ? (window: string) => {
				if (document.getElementById(window)?.hasAttribute(DISPOSABLE)) {
					onWindow('dispose', '', '', window);
				}
			} : options.onClose
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
