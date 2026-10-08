/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * json-tree.js: registers Wicket.JsonTree for JsonBasedTree and JsonBasedTreeGrid, which call
 * new Wicket.JsonTree(cfg, callbacks, roots) and find a tree in its instances. The nodes are
 * rendered with the engines of the page's Wicket.Templates.
 */

import { JsonTree } from '../tree.js';
import { useTemplates, type TemplateRegistry } from '../templates.js';
import { useWicketHost, wicket } from './host.js';

const Wicket = wicket();
if (Wicket.JsonTree === undefined) {
	useWicketHost();
	useTemplates(Wicket.Templates as TemplateRegistry | undefined);
	Wicket.JsonTree = JsonTree;
}
