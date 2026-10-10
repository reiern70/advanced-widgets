/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * antilia-palette.js: registers Wicket.Palette for PaletteBehavior, which calls
 * Wicket.Palette.attach(id, options). The lists get the scroll bars of Wicket.Scrollbar, when the
 * page loaded them.
 */

import { Palette } from '../palette.js';
import type { ScrollbarHandle, ScrollbarOptions } from '../scrollbar.js';
import { useWicketHost, wicket } from './host.js';

interface ScrollbarGlobal {
	attach(element: HTMLElement, options: ScrollbarOptions): ScrollbarHandle | undefined;
}

const Wicket = wicket();
if (Wicket.Palette === undefined) {
	useWicketHost();
	Palette.attachScrollbar = (list) =>
		(window.Wicket?.Scrollbar as ScrollbarGlobal | undefined)?.attach(list, { axis: 'y' });
	Wicket.Palette = Palette;
}
