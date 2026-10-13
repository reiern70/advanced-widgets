/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { getContext, hasContext, setContext } from 'svelte';

import { ANTILIA_LOOK, lookNamed, type Look } from '@antilia/core';

/**
 * The key of the look in the context of the components, for mount(App, { context: new
 * Map([[LOOK_CONTEXT, look]]) }), which gives an app a look from the start.
 */
export const LOOK_CONTEXT = Symbol('antilia-look');

/**
 * Gives the components a component renders a look, such as Bootstrap's, whose classes their parts
 * then get; without one they are in the Antilia look. Called while the component initializes, as
 * Svelte's setContext.
 *
 * @param look the look, or its name: 'antilia' or 'bootstrap', or that of a look registered
 * @returns the look
 */
export function setLook(look: Look | string): Look {
	return setContext(LOOK_CONTEXT, typeof look === 'string' ? lookNamed(look) : look);
}

/**
 * @returns the look the component is in, from the closest setLook; the Antilia look without one.
 * Called while the component initializes, as Svelte's getContext.
 */
export function getLook(): Look {
	return hasContext(LOOK_CONTEXT) ? getContext<Look>(LOOK_CONTEXT) : ANTILIA_LOOK;
}
