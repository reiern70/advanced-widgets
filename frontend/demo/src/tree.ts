/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * The tree demos: a tree of life in a Tree or a TreeGrid, loaded from the tree of life resource
 * group by group, groups and species selected alike, the cards of what is selected shown next to
 * them.
 */

import type { ResizeMode, TreeColumn, TreeIcons, TreeNode, TreeTexts } from '@antilia/core';

import { fetchJson } from './data.js';

/** The card of a group or a species, its texts in the language of the session. */
export interface TaxonCard {
	id: string;
	name: string;
	/** The scientific name, empty for an informal group. */
	scientific: string;
	rank: string;
	/** The periods it lived in. */
	period: string;
	/** When it lived. */
	lived: string;
	description: string;
}

/** A group or a species as the tree of life resource sends it. */
export interface TaxonData extends TaxonCard {
	group: boolean;
	/** The names of the groups it belongs to, the kingdom first, and its own last. */
	lineage: string[];
	/** The card of the group of a species. */
	parent?: TaxonCard;
}

/** The texts of the demos, by the keys of the pages' bundles. */
export interface TreeDemoTexts {
	/** The name of the trees for screen readers. */
	treeOfLife: string;
	selected: string;
	noneSelected: string;
	lineage: string;
	period: string;
	lived: string;
	/** The label of the drop-down choosing the icons, and those of its choices. */
	icons: string;
	'icons.BUILT_IN': string;
	'icons.SVG': string;
	'icons.FONT_AWESOME': string;
	/** What the drop-downs say. */
	filter: string;
	noMatch: string;
	more: string;
	remove: string;
	/** What the trees say, as the Wicket trees say it. */
	tree: Required<TreeTexts>;
}

/**
 * The icons of the trees, as the drop-down of the demos chooses them: those of the trees, the
 * Font Awesome icons as inline SVG, or the same drawn by the Font Awesome font.
 */
export type IconSet = 'BUILT_IN' | 'SVG' | 'FONT_AWESOME';

/** The choices of the drop-down choosing the icons, in their order. */
export const ICON_SETS: IconSet[] = ['BUILT_IN', 'SVG', 'FONT_AWESOME'];

/** What the page gives the demos. */
export interface TreeDemoConfig {
	texts: TreeDemoTexts;
	/** The tree of life: the kingdoms, or the children of ?parent=id. */
	dataUrl: string;
	/** The template of a node. */
	template: string;
	/** The keys of the groups expanded at first. */
	expanded: string[];
	/** The height of the trees, within which they scroll. */
	height: { value: number; unit: string };
	/** The markup of the icons of each set, none for the trees' own. */
	iconSets: Record<IconSet, Partial<TreeIcons>>;
	/** The icons chosen at first, those the Wicket trees of the page show. */
	icons: IconSet;
}

/** How the columns of a tree grid are resized, as the drop-down of the demo chooses it. */
export type Resizing = 'none' | ResizeMode;

/** The texts of the tree grid demo besides, those of its drop-down too. */
export interface TreeGridDemoTexts extends TreeDemoTexts {
	resize: string;
	'resize.none': string;
	'resize.fit': string;
	'resize.stretch': string;
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

/**
 * @returns the options of the drop-down choosing the icons of the trees
 */
export function iconSetOptions(texts: TreeDemoTexts): { value: string; label: string }[] {
	return ICON_SETS.map((value) => ({ value, label: texts[`icons.${value}`] }));
}

/** The facts of a card below its description, by the keys of their labels. */
export const FACTS = ['period', 'lived'] as const;

/**
 * @returns the card of the group of what is selected, the group itself or that of a species
 */
export function groupCard(taxon: TaxonData): TaxonCard | null {
	return taxon.group ? taxon : taxon.parent ?? null;
}

/**
 * @param url the URL of the tree of life resource
 * @returns the load of a tree: the kingdoms, or the children of a group
 */
export function loadTaxa(url: string): (parent: string | null) => Promise<TreeNode[]> {
	return async (parent) => (await fetchJson<{ nodes: TreeNode[] }>(parent === null ? url :
		url + (url.indexOf('?') >= 0 ? '&' : '?') + 'parent=' + encodeURIComponent(parent))).nodes;
}
