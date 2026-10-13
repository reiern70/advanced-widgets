/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * antilia-popover.js: registers Wicket.Popover for the Popover of ui-components, whose
 * show(target, trigger, ...) has the browser call Wicket.Popover.show(triggerId, options) or
 * toggle(triggerId, options) in the response to an Ajax request.
 */

import { Popover } from '../popover.js';
import { useWicketHost, wicket } from './host.js';

const Wicket = wicket();
if (Wicket.Popover === undefined) {
	useWicketHost();
	Wicket.Popover = Popover;
}
