/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * wicket-autocomplete.js: registers Wicket.Autocomplete for Autocomplete and MultiAutocomplete,
 * which call Wicket.Autocomplete.attach(id, options). The rows are rendered with the engines of the
 * page's Wicket.Templates, which wicket-templates.js registered before.
 */

import { Autocomplete } from '../autocomplete.js';
import { useTemplates, type TemplateRegistry } from '../templates.js';
import { useWicketHost, wicket } from './host.js';

const Wicket = wicket();
if (Wicket.Autocomplete === undefined) {
	useWicketHost();
	useTemplates(Wicket.Templates as TemplateRegistry | undefined);
	Wicket.Autocomplete = Autocomplete;
}
