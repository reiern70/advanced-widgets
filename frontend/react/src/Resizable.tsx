/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { forwardRef, useImperativeHandle, useLayoutEffect, useRef,
	type HTMLAttributes } from 'react';

import { Resizable as CoreResizable, type ResizableSize, type ResizeAxis } from '@antilia/core';

import { useLook } from './Look.js';

export interface ResizableProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onResize'> {
	/** The dimensions the user changes, both when left out. */
	axis?: ResizeAxis;
	/**
	 * The width, in pixels: the one the element starts with, and the one it takes whenever this
	 * changes; null or left out for the page's.
	 */
	width?: number | null;
	/** The height, in pixels, as the width. */
	height?: number | null;
	/** The smallest width, in pixels, 32 when left out. */
	minWidth?: number;
	/** The smallest height, in pixels, 32 when left out. */
	minHeight?: number;
	/** The largest width, in pixels, none when left out. */
	maxWidth?: number | null;
	/** The largest height, in pixels, none when left out. */
	maxHeight?: number | null;
	/** How far an arrow key resizes the element, in pixels, 10 when left out. */
	step?: number;
	/** Whether the user can resize the element, true when left out. */
	enabled?: boolean;
	/** The accessible name of the handle. */
	label?: string;
	/** Called with the size while the user drags the handle. */
	onResize?(size: ResizableSize): void;
	/** Called with the size once the user has resized the element. */
	onResized?(size: ResizableSize): void;
}

/**
 * What a ref to a {@link Resizable} can do with it.
 */
export interface ResizableHandle {
	/** The resizable div. */
	readonly element: HTMLDivElement | null;
	/** @returns the size of the element, null for a dimension the page's style sheets set */
	getSize(): ResizableSize;
}

/**
 * A div the user resizes by the handle the core adds to its bottom edge, its right edge or its
 * bottom right corner: dragged, or moved with the arrow keys; a double click takes it back to its
 * first size. The size is the user's: onResized tells it, for the page to keep it and give it back
 * as width and height, after the div was rendered anew for example. A ref gets a
 * {@link ResizableHandle}.
 */
export const Resizable = forwardRef<ResizableHandle, ResizableProps>(function Resizable(props,
	ref) {
	const { axis, width, height, minWidth, minHeight, maxWidth, maxHeight, step, enabled, label,
		onResize: _onResize, onResized: _onResized, className, children, ...rest } = props;
	const element = useRef<HTMLDivElement>(null);
	const instance = useRef<CoreResizable | null>(null);
	const look = useLook();
	const latest = useRef(props);
	latest.current = props;
	const rendered = useRef(className);

	useImperativeHandle(ref, () => ({
		get element() {
			return element.current;
		},
		getSize: () => instance.current?.getSize() ?? { width: null, height: null }
	}), []);

	useLayoutEffect(() => {
		const current = latest.current;
		const resizable = new CoreResizable(element.current!, {
			axis: current.axis, width: current.width, height: current.height,
			minWidth: current.minWidth, minHeight: current.minHeight, maxWidth: current.maxWidth,
			maxHeight: current.maxHeight, step: current.step, enabled: current.enabled,
			label: current.label, look,
			onResize: (size) => latest.current.onResize?.(size),
			onResized: (size) => latest.current.onResized?.(size)
		});
		instance.current = resizable;
		return () => {
			resizable.destroy();
			instance.current = null;
		};
	}, [look]);

	useLayoutEffect(() => {
		instance.current?.configure({ axis, minWidth, minHeight, maxWidth, maxHeight, step, enabled,
			label });
	}, [axis, minWidth, minHeight, maxWidth, maxHeight, step, enabled, label]);

	useLayoutEffect(() => {
		const resizable = instance.current;
		if (resizable && (width !== undefined || height !== undefined)) {
			const size = resizable.getSize();
			resizable.setSize({ width: width === undefined ? size.width : width,
				height: height === undefined ? size.height : height });
		}
	}, [width, height]);

	// React wrote other classes over the element's: those the core keeps come back
	useLayoutEffect(() => {
		if (rendered.current !== className) {
			rendered.current = className;
			instance.current?.configure({});
		}
	}, [className]);

	return (
		<div {...rest} ref={element} className={className}>
			{children}
		</div>
	);
});
