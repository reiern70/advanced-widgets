/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * dynamic-data-table.js: registers Wicket.DynamicDataTable for the dynamic data tables, which call
 * new Wicket.DynamicDataTable(cfg, action, navigation, messages, data) and find a table in its
 * instances. The rows are rendered with the engines of the page's Wicket.Templates; the table uses
 * the page's Wicket.Scrollbar, Wicket.ResizableColumns and Wicket.Veil when the page loaded them,
 * as before.
 */

import { DynamicDataTable, type Bars, type ResizableColumnsRegistry,
	type Veil } from '../data-table.js';
import { useTemplates, type TemplateRegistry } from '../templates.js';
import { useWicketHost, wicket } from './host.js';

interface ScrollbarGlobal {
	attach(element: HTMLElement, options: { axis: 'y' }): Bars | undefined;
}

const Wicket = wicket();
if (Wicket.DynamicDataTable === undefined) {
	useWicketHost();
	useTemplates(Wicket.Templates as TemplateRegistry | undefined);
	DynamicDataTable.attachScrollbar = (element) =>
		(window.Wicket?.Scrollbar as ScrollbarGlobal | undefined)?.attach(element, { axis: 'y' });
	DynamicDataTable.resizableColumns = () =>
		window.Wicket?.ResizableColumns as ResizableColumnsRegistry | undefined;
	DynamicDataTable.veil = () => window.Wicket?.Veil as Veil | undefined;
	Wicket.DynamicDataTable = DynamicDataTable;
}
