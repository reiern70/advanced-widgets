/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * antilia-button.js: registers Wicket.Button for ButtonDownloadBehavior, which marks its button
 * busy while the download it started runs, Wicket.Button.busy(id, true), and done once the
 * browser told the server how it ended, Wicket.Button.busy(id, false). A busy button ignores
 * clicks.
 */

import { guardBusyButtons, setButtonBusy } from '../button.js';
import { useWicketHost, wicket } from './host.js';

const Wicket = wicket();
if (Wicket.Button === undefined) {
	useWicketHost();
	Wicket.Button = {
		busy(id: string, busy: boolean): void {
			setButtonBusy(document.getElementById(id), busy);
		}
	};
	guardBusyButtons();
}
