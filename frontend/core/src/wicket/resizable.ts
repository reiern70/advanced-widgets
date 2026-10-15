/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * antilia-resizable.js: registers Wicket.Resizable for the ResizableBehavior of ui-components,
 * which calls Wicket.Resizable.attach(id, options, onResized); onResized, when the server
 * remembers the size, reports it via Ajax.
 */

import { Resizable, type ResizableOptions, type ResizableSize } from '../resizable.js';
import { useWicketHost, wicket } from './host.js';

const WicketResizable = {

	/**
	 * Makes an element resizable.
	 *
	 * @param id the markup id of the element
	 * @param options the dimensions the user changes, the sizes, the limits and the texts
	 * @param onResized the Ajax callback of the server given the width and the height, in pixels,
	 *        empty for none
	 * @returns the resizable, null when there is no such element
	 */
	attach(id: string, options: ResizableOptions = {},
		onResized?: ((width: string, height: string) => void) | null): Resizable | null {
		return Resizable.attach(id, {
			...options,
			onResized: onResized ? (size: ResizableSize) => onResized(
				size.width === null ? '' : String(size.width),
				size.height === null ? '' : String(size.height)) : options.onResized
		});
	},

	/**
	 * @param id the markup id of the element
	 * @returns the resizable, if there is one
	 */
	of(id: string): Resizable | null {
		return Resizable.of(id);
	}
};

const Wicket = wicket();
if (Wicket.Resizable === undefined) {
	useWicketHost();
	Wicket.Resizable = WicketResizable;
}
