/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * antilia-split-panel.js: registers Wicket.SplitPanel for the SplitPanel of ui-components, which
 * calls Wicket.SplitPanel.attach(id, options); onResized, when the server remembers the position,
 * reports it via Ajax.
 */

import { SplitPanel } from '../split-panel.js';
import { useWicketHost, wicket } from './host.js';

const Wicket = wicket();
if (Wicket.SplitPanel === undefined) {
	useWicketHost();
	Wicket.SplitPanel = SplitPanel;
}
