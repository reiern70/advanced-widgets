/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { forwardRef, useImperativeHandle, useLayoutEffect, useRef,
	type HTMLAttributes } from 'react';

import { Scrollbar, type ScrollbarOptions, type ScrollPosition } from '@antilia/core';

export interface ScrollableProps extends HTMLAttributes<HTMLDivElement> {
	/** The axes of the bars: by default those along which the CSS of the element scrolls. */
	axis?: ScrollbarOptions['axis'];
	/** CSS classes of the div wrapping the scrolling one, next to antilia-scrollbar. */
	wrapperClassName?: string;
}

/**
 * What a ref to a {@link Scrollable} can do with it; smooth scrolls smoothly rather than at once.
 */
export interface ScrollableHandle {
	/** The scrolling div. */
	readonly element: HTMLDivElement | null;
	/** Scrolls to the start. */
	scrollToTop(smooth?: boolean): void;
	/** Scrolls to the end. */
	scrollToBottom(smooth?: boolean): void;
	/** Scrolls to a position, in pixels from the start, along either axis or both. */
	scrollTo(position: ScrollPosition, smooth?: boolean): void;
	/**
	 * Scrolls an element inside to the top, the element or a CSS selector of it; false if there is
	 * no such element.
	 */
	scrollToElement(target: Element | string, smooth?: boolean): boolean;
}

/**
 * A div scrolling with the slim scroll bars of the core, which look the same in every browser and
 * take the theme's colors. Give it a height, or a maximum height, and an overflow in its CSS. A ref
 * gets a {@link ScrollableHandle}.
 */
export const Scrollable = forwardRef<ScrollableHandle, ScrollableProps>(function Scrollable(
	{ axis, wrapperClassName, className, children, ...rest }, ref) {
	const element = useRef<HTMLDivElement>(null);
	const bars = useRef<Scrollbar | undefined>(undefined);

	useImperativeHandle(ref, () => ({
		get element() {
			return element.current;
		},
		scrollToTop: (smooth) => bars.current?.scrollToTop(smooth),
		scrollToBottom: (smooth) => bars.current?.scrollToBottom(smooth),
		scrollTo: (position, smooth) => bars.current?.scrollTo(position, smooth),
		scrollToElement: (target, smooth) => bars.current?.scrollToElement(target, smooth) ?? false
	}), []);

	useLayoutEffect(() => {
		const instance = Scrollbar.attach(element.current!, { axis });
		bars.current = instance;
		return () => {
			instance?.destroy();
			bars.current = undefined;
		};
	}, [axis]);

	return (
		<div className={['antilia-scrollbar', wrapperClassName].filter(Boolean).join(' ')}>
			<div {...rest} ref={element}
				className={['antilia-scrollbar-host', className].filter(Boolean).join(' ')}>
				{children}
			</div>
		</div>
	);
});
