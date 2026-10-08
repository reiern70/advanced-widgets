/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The tree demos: the files of advanced-widgets in a Tree or a TreeGrid, loaded from the files
 * resource folder by folder, the file selected shown next to them.
 */

import type { TreeColumn, TreeNode, TreeTexts } from '@advanced-widgets/core';

import { fetchJson } from './data.js';

/** A file or a folder as the files resource sends it, its texts in the language of the session. */
export interface FileData {
	id: number;
	name: string;
	path: string;
	folder: boolean;
	kind: string;
	size: string;
	bytes: number;
	items: string;
	modified: string;
}

/** The texts of the demos, by the keys of the pages' bundles. */
export interface TreeDemoTexts {
	/** The name of the trees for screen readers. */
	files: string;
	selectedFile: string;
	noneSelected: string;
	name: string;
	path: string;
	kind: string;
	size: string;
	modified: string;
	/** What the trees say, as the Wicket trees say it. */
	tree: Required<TreeTexts>;
}

/** What the page gives the demos. */
export interface TreeDemoConfig {
	texts: TreeDemoTexts;
	/** The files: the roots, or the children of ?parent=id. */
	filesUrl: string;
	/** The template of a node. */
	template: string;
	/** The keys of the folders expanded at first. */
	expanded: string[];
}

/** What the page gives the tree grid demo besides. */
export interface TreeGridDemoConfig extends TreeDemoConfig {
	/** The header of the tree column. */
	header: string;
	/** The other columns, as the JSON tree grid of the page has them. */
	columns: TreeColumn[];
}

/** The facts of a file the details show, by the keys of their labels. */
export const FACTS = ['name', 'path', 'kind', 'size', 'modified'] as const;

/**
 * @param url the URL of the files resource
 * @returns the load of a tree: the roots, or the children of a folder
 */
export function loadFiles(url: string): (parent: string | null) => Promise<TreeNode[]> {
	return async (parent) => (await fetchJson<{ nodes: TreeNode[] }>(parent === null ? url :
		url + (url.indexOf('?') >= 0 ? '&' : '?') + 'parent=' + encodeURIComponent(parent))).nodes;
}
