/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * json-tree.js: registers Wicket.JsonTree for JsonBasedTree and JsonBasedTreeGrid, which call
 * new Wicket.JsonTree(cfg, callbacks, roots) and find a tree in its instances. The nodes are
 * rendered with the engines of the page's Wicket.Templates. A tree of a height gets the scroll
 * bars of Wicket.Scrollbar, and the resizable columns of a tree grid are those of
 * Wicket.ResizableColumns, when the page loaded them.
 */

import { JsonTree, type TreeColumnsRegistry } from '../tree.js';
import { useTemplates, type TemplateRegistry } from '../templates.js';
import type { ScrollbarHandle, ScrollbarOptions } from '../scrollbar.js';
import { useWicketHost, wicket } from './host.js';

interface ScrollbarGlobal {
	attach(element: HTMLElement, options: ScrollbarOptions): ScrollbarHandle | undefined;
}

const Wicket = wicket();
if (Wicket.JsonTree === undefined) {
	useWicketHost();
	useTemplates(Wicket.Templates as TemplateRegistry | undefined);
	JsonTree.attachScrollbar = (element, options) =>
		(window.Wicket?.Scrollbar as ScrollbarGlobal | undefined)?.attach(element, options);
	JsonTree.resizableColumns = () =>
		window.Wicket?.ResizableColumns as TreeColumnsRegistry | undefined;
	Wicket.JsonTree = JsonTree;
}
