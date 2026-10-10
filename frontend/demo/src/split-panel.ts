/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The SplitPanel demo: the layout of an editor, the files side by side with an editor above its
 * console, saying where its dividers are, the files in pixels and both collapsing a pane; and a
 * split panel whose orientation, whether its divider moves and whether it collapses its panes,
 * three check boxes switch.
 */

import type { SplitPane, SplitSize } from '@antilia/core';

import { format } from './data.js';

/** The texts of the demo, by the keys of the page's bundle. */
export interface SplitPanelDemoTexts {
	layoutTitle: string;
	settingsTitle: string;
	files: string;
	editor: string;
	console: string;
	vertical: string;
	resizable: string;
	collapsible: string;
	/** Where the dividers are: {0} the size of the files, {1} that of the editor. */
	sizes: string;
	/** The size of a collapsed pane. */
	none: string;
	/** The size of a pane whose other pane is collapsed. */
	all: string;
	first: string;
	second: string;
	/** The accessible name of the dividers. */
	divider: string;
	/** The titles of the collapse buttons. */
	collapseLeft: string;
	collapseRight: string;
	collapseTop: string;
	collapseBottom: string;
	restore: string;
}

/** Where a divider of the layout is, which pane is collapsed, and how far it goes. */
export interface SplitLimits {
	position: SplitSize;
	/** Left out when none. */
	collapsed?: SplitPane | null;
	min?: SplitSize;
	max?: SplitSize;
	minSecond?: SplitSize;
}

/** What the page gives the demo. */
export interface SplitPanelConfig {
	texts: SplitPanelDemoTexts;
	files: string[];
	code: string;
	console: string;
	layout: { files: SplitLimits; editor: SplitLimits };
	vertical: boolean;
	resizable: boolean;
	collapsible: boolean;
}

/** Where a divider is and which pane is collapsed. */
export interface SplitState {
	position: SplitSize;
	collapsed: SplitPane | null;
}

/**
 * @returns the size of the first pane in words: in its unit, none or all of it
 */
function size(texts: SplitPanelDemoTexts, state: SplitState): string {
	if (state.collapsed) {
		return state.collapsed === 'first' ? texts.none : texts.all;
	}
	const pixels = typeof state.position === 'string' && state.position.endsWith('px');
	return Math.round(parseFloat(String(state.position))) + (pixels ? ' px' : ' %');
}

/**
 * @returns where the dividers of the layout are, in words, as the Wicket page says it
 */
export function describe(config: SplitPanelConfig, files: SplitState, editor: SplitState): string {
	return format(config.texts.sizes, size(config.texts, files), size(config.texts, editor));
}
