/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * antilia-scrollbar.js: registers Wicket.Scrollbar for ScrollbarBehavior, which calls
 * Wicket.Scrollbar.attach(id, {axis}), and for the dynamic data tables' scrolling bodies.
 */

import { Scrollbar } from '../scrollbar.js';
import { useWicketHost, wicket } from './host.js';

const Wicket = wicket();
if (Wicket.Scrollbar === undefined) {
	useWicketHost();
	Wicket.Scrollbar = Scrollbar;
}
