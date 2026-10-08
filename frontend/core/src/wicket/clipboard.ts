/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * wicket-clipboard.js: registers Wicket.Clipboard for ClipboardCopyBehavior, which sets its
 * copiedMessage, and for the theme editor, which calls Wicket.Clipboard.copy(text, element).
 */

import { Clipboard } from '../clipboard.js';
import { useWicketHost, wicket } from './host.js';

const Wicket = wicket();
if (Wicket.Clipboard === undefined) {
	useWicketHost();
	Wicket.Clipboard = Clipboard;
	Clipboard.install();
}
