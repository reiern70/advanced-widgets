/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The tree demos: the files of advanced-widgets in a Tree or a TreeGrid, loaded from the files
 * resource folder by folder, the file selected shown next to them.
 */

import type { ResizeMode, TreeColumn, TreeNode, TreeTexts } from '@advanced-widgets/core';

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
	/** The height of the trees, within which they scroll. */
	height: { value: number; unit: string };
}

/** How the columns of a tree grid are resized, as the drop-down of the demo chooses it. */
export type Resizing = 'none' | ResizeMode;

/** The texts of the tree grid demo besides, those of its drop-down too. */
export interface TreeGridDemoTexts extends TreeDemoTexts {
	resize: string;
	'resize.none': string;
	'resize.fit': string;
	'resize.stretch': string;
	filter: string;
	noMatch: string;
	more: string;
	remove: string;
}

/** What the page gives the tree grid demo besides. */
export interface TreeGridDemoConfig extends TreeDemoConfig {
	texts: TreeGridDemoTexts;
	/** The header of the tree column. */
	header: string;
	/** The other columns, as the JSON tree grid of the page has them. */
	columns: TreeColumn[];
	/** How the columns are resized at first. */
	resizableColumns: Resizing;
}

/** The choices of the drop-down of the tree grid demo, in their order. */
export const RESIZINGS: Resizing[] = ['none', 'fit', 'stretch'];

/**
 * @returns the options of the drop-down choosing how the columns of the tree grid are resized
 */
export function resizingOptions(texts: TreeGridDemoTexts): { value: string; label: string }[] {
	return RESIZINGS.map((value) => ({ value, label: texts[`resize.${value}`] }));
}

/**
 * @returns how the tree grid resizes its columns, false when it does not
 */
export function resizeMode(resizing: Resizing): ResizeMode | false {
	return resizing === 'none' ? false : resizing;
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
