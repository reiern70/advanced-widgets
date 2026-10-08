/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { useLayoutEffect, useRef, type HTMLAttributes } from 'react';

import { Scrollbar, type ScrollbarOptions } from '@advanced-widgets/core';

export interface ScrollableProps extends HTMLAttributes<HTMLDivElement> {
	/** The axes of the bars: by default those along which the CSS of the element scrolls. */
	axis?: ScrollbarOptions['axis'];
	/** CSS classes of the div wrapping the scrolling one, next to wicket-scrollbar. */
	wrapperClassName?: string;
}

/**
 * A div scrolling with the slim scroll bars of the core, which look the same in every browser and
 * take the theme's colors. Give it a height, or a maximum height, and an overflow in its CSS.
 */
export function Scrollable({ axis, wrapperClassName, className, children, ...rest }:
	ScrollableProps) {
	const element = useRef<HTMLDivElement>(null);

	useLayoutEffect(() => {
		const bars = Scrollbar.attach(element.current!, { axis });
		return () => bars?.destroy();
	}, [axis]);

	return (
		<div className={['wicket-scrollbar', wrapperClassName].filter(Boolean).join(' ')}>
			<div {...rest} ref={element}
				className={['wicket-scrollbar-host', className].filter(Boolean).join(' ')}>
				{children}
			</div>
		</div>
	);
}
