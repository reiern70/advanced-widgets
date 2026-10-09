/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * wicket-autocomplete.js: registers Wicket.Autocomplete for Autocomplete and MultiAutocomplete,
 * which call Wicket.Autocomplete.attach(id, options). The rows are rendered with the engines of the
 * page's Wicket.Templates, which wicket-templates.js registered before. The list of suggestions
 * gets the scroll bars of Wicket.Scrollbar, when the page loaded them.
 */

import { Autocomplete } from '../autocomplete.js';
import type { ScrollbarHandle, ScrollbarOptions } from '../scrollbar.js';
import { useTemplates, type TemplateRegistry } from '../templates.js';
import { useWicketHost, wicket } from './host.js';

interface ScrollbarGlobal {
	attach(element: HTMLElement, options: ScrollbarOptions): ScrollbarHandle | undefined;
}

const Wicket = wicket();
if (Wicket.Autocomplete === undefined) {
	useWicketHost();
	useTemplates(Wicket.Templates as TemplateRegistry | undefined);
	Autocomplete.attachScrollbar = (list) =>
		(window.Wicket?.Scrollbar as ScrollbarGlobal | undefined)?.attach(list, { axis: 'y' });
	Wicket.Autocomplete = Autocomplete;
}
