/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * wicket-movable-columns.js: registers Wicket.MovableColumns for MovableColumnsBehavior, which
 * calls Wicket.MovableColumns.attach(id, {movable, label, onMove}) with onMove calling back the
 * server.
 */

import { MovableColumns } from '../movable-columns.js';
import { useWicketHost, wicket } from './host.js';

const Wicket = wicket();
if (Wicket.MovableColumns === undefined) {
	useWicketHost();
	Wicket.MovableColumns = MovableColumns;
}
