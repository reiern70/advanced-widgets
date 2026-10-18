/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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

import { Children, useLayoutEffect, useRef, type ReactNode } from 'react';

import { parseSplitSize, partClasses, type SplitOrientation, type SplitPane,
	SplitPanel as CoreSplitPanel, type SplitSize } from '@antilia/core';

import { useLook } from './Look.js';

export interface SplitPanelProps {
	/** The two panes: the first child is the left or top one, the second the right or bottom one. */
	children: [ReactNode, ReactNode];
	/** horizontal (the default) for the panes side by side, vertical for one above the other. */
	orientation?: SplitOrientation;
	/**
	 * The size of the first pane, 50 (percent) when left out: where the divider starts, and where
	 * it goes whenever this changes. In pixels, '250px', the first pane keeps its size when the
	 * split panel grows or shrinks.
	 */
	position?: SplitSize;
	/** The smallest size of the first pane, in percent or as '200px', 0 when left out. */
	min?: SplitSize;
	/** The largest size of the first pane, in percent or as '600px', 100 when left out. */
	max?: SplitSize;
	/** The smallest size of the second pane, in percent or as '200px', 0 when left out. */
	minSecond?: SplitSize;
	/** How far an arrow key moves the divider, in percent or as '20px', 5 when left out. */
	step?: SplitSize;
	/** Whether the user can move the divider, true when left out. */
	resizable?: boolean;
	/** Whether the divider has buttons collapsing a pane and restoring it, false when left out. */
	collapsible?: boolean;
	/** The collapsed pane: where it starts, and what it becomes whenever this changes. */
	collapsed?: SplitPane | null;
	/** The accessible name of the divider. */
	label?: string;
	/** The title of the button collapsing the first pane. */
	collapseFirstLabel?: string;
	/** The title of the button collapsing the second pane. */
	collapseSecondLabel?: string;
	/** The title of the button restoring a collapsed pane. */
	restoreLabel?: string;
	/** Called with the position while the user moves the divider, in the unit it was given. */
	onResize?(position: SplitSize): void;
	/** Called with the position once the user has moved the divider. */
	onResized?(position: SplitSize): void;
	/** Called with the collapsed pane, or null, once the user has collapsed or restored a pane. */
	onCollapse?(collapsed: SplitPane | null): void;
	id?: string;
	/** CSS classes of the split panel, next to antilia-split. */
	className?: string;
}

/**
 * The split panel of the core: two panes, side by side or one above the other, and a divider
 * between them the user drags, or moves with the keyboard, to share the space differently; when
 * collapsible, its buttons collapse a pane and restore it. React renders the panes and the
 * divider, the core moves it. Give a vertical one a height.
 */
export function SplitPanel(props: SplitPanelProps) {
	const { children, orientation = 'horizontal', position, min, max, minSecond, step,
		resizable = true, collapsible = false, collapsed, label, collapseFirstLabel,
		collapseSecondLabel, restoreLabel, id, className } = props;
	const element = useRef<HTMLDivElement>(null);
	const instance = useRef<CoreSplitPanel | null>(null);
	const look = useLook();
	const latest = useRef(props);
	latest.current = props;
	const [first, second] = Children.toArray(children);
	// the classes the core gives the element, so that React keeps them when it renders again
	const classes = [partClasses(look, 'split'), 'antilia-split-' + orientation,
		resizable ? null : 'antilia-split-fixed',
		parseSplitSize(position)?.unit === 'px' ? 'antilia-split-pixels' : null,
		collapsible ? 'antilia-split-collapsible' : null, className].filter(Boolean).join(' ');
	const rendered = useRef(classes);

	useLayoutEffect(() => {
		const split = new CoreSplitPanel(element.current!, {
			orientation, position, min, max, minSecond, step, resizable, collapsible, collapsed,
			label, collapseFirstLabel, collapseSecondLabel, restoreLabel, look,
			onResize: (next) => latest.current.onResize?.(next),
			onResized: (next) => latest.current.onResized?.(next),
			onCollapse: (next) => latest.current.onCollapse?.(next)
		});
		instance.current = split;
		return () => {
			split.destroy();
			instance.current = null;
		};
	}, [look]);

	useLayoutEffect(() => {
		instance.current?.configure({ orientation, min, max, minSecond, step, resizable,
			collapsible, label, collapseFirstLabel, collapseSecondLabel, restoreLabel });
	}, [orientation, min, max, minSecond, step, resizable, collapsible, label, collapseFirstLabel,
		collapseSecondLabel, restoreLabel]);

	useLayoutEffect(() => {
		if (position !== undefined) {
			instance.current?.setPosition(position);
		}
	}, [position]);

	useLayoutEffect(() => {
		if (collapsed !== undefined) {
			instance.current?.setCollapsed(collapsed);
		}
	}, [collapsed]);

	// React wrote other classes over the element's: those the core keeps, such as the collapsed
	// pane's, come back
	useLayoutEffect(() => {
		if (rendered.current !== classes) {
			rendered.current = classes;
			instance.current?.configure({});
		}
	});

	return (
		<div ref={element} id={id} className={classes}>
			<div className={partClasses(look, 'split-pane', 'split-first')}>{first}</div>
			<div className={partClasses(look, 'split-divider')} />
			<div className={partClasses(look, 'split-pane', 'split-second')}>{second}</div>
		</div>
	);
}
