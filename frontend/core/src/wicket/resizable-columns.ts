/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * antilia-resizable-columns.js: registers Wicket.ResizableColumns for ResizableColumnsBehavior,
 * which calls Wicket.ResizableColumns.attach(id, options) with onResize calling back the server,
 * and for the dynamic data tables, which use its instances, tableOptions, fit and detach.
 */

import { ResizableColumns } from '../resizable-columns.js';
import { useWicketHost, wicket } from './host.js';

const Wicket = wicket();
if (Wicket.ResizableColumns === undefined) {
	useWicketHost();
	Wicket.ResizableColumns = ResizableColumns;
}
