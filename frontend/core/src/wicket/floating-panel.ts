/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * antilia-floating-panel.js: registers Wicket.FloatingPanel, which makes the panels of
 * FloatingPanel movable.
 */

import { FloatingPanel } from '../floating-panel.js';
import { useWicketHost, wicket } from './host.js';

const Wicket = wicket();
if (Wicket.FloatingPanel === undefined) {
	useWicketHost();
	Wicket.FloatingPanel = FloatingPanel;
	FloatingPanel.install();
}
