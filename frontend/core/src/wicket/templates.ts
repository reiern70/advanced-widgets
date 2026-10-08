/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * wicket-templates.js: registers Wicket.Templates, the registry of the template engines that the
 * dynamic data tables, the infinite lists and the autocompletes render with, and that
 * handlebars-adapter.js adds Handlebars to.
 */

import { Templates } from '../templates.js';
import { useWicketHost, wicket } from './host.js';

const Wicket = wicket();
if (Wicket.Templates === undefined) {
	useWicketHost();
	Wicket.Templates = Templates;
}
