/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { useEffect, useRef, useState } from 'react';

import { Scrollable, type ScrollableHandle } from '@advanced-widgets/react';

import { line, LINE, lines, MORE_LINES, POSITION, TILES,
	type ScrollbarConfig } from '../scrollbar.js';

function Lines({ pattern, count }: { pattern: string; count: number }) {
	return <>{lines(pattern, count).map((text, index) =>
		<p key={index} data-line={index + 1}>{text}</p>)}</>;
}

/**
 * The Scrollbar demo in React: the boxes of the Wicket page, those with slim bars a Scrollable, the
 * last one growing in the app and scrolled through its ref.
 */
export function ScrollbarDemo({ config }: { config: ScrollbarConfig }) {
	const { texts } = config;
	const [count, setCount] = useState(config.lines);
	const growing = useRef<ScrollableHandle>(null);
	// the first of the lines added, scrolled to once they are rendered
	const added = useRef(0);
	const add = () => {
		added.current = count + 1;
		setCount(count + MORE_LINES);
	};

	useEffect(() => {
		if (added.current) {
			growing.current?.scrollToElement(line(added.current), true);
			added.current = 0;
		}
	}, [count]);

	return (
		<div className="scrollbar-demo">
			<section>
				<h3>{texts.native}</h3>
				<div className="scrollbar-demo__box"><Lines pattern={texts['line.long']} count={40} /></div>
			</section>
			<section>
				<h3>{texts.vertical}</h3>
				<Scrollable className="scrollbar-demo__box">
					<Lines pattern={texts['line.long']} count={40} />
				</Scrollable>
			</section>
			<section>
				<h3>{texts.horizontal}</h3>
				<Scrollable className="scrollbar-demo__box scrollbar-demo__box--x" axis="x">
					<div className="scrollbar-demo__strip">
						{TILES.map((tile) => <span key={tile}>{tile}</span>)}
					</div>
				</Scrollable>
			</section>
			<section>
				<h3>{texts.both}</h3>
				<Scrollable className="scrollbar-demo__box scrollbar-demo__box--both">
					<Lines pattern={texts['line.long']} count={60} />
				</Scrollable>
			</section>
			<section>
				<h3>{texts.growing}</h3>
				<Scrollable ref={growing} className="scrollbar-demo__box scrollbar-demo__box--short">
					<Lines pattern={texts.line} count={count} />
				</Scrollable>
				<div className="scrollbar-demo__actions">
					<button type="button" className="scrollbar-demo__add" title={texts.addLines}
						aria-label={texts.addLines} onClick={add}>+</button>
					<button type="button" className="scrollbar-demo__to"
						onClick={() => growing.current?.scrollToTop(true)}>{texts.toTop}</button>
					<button type="button" className="scrollbar-demo__to"
						onClick={() => growing.current?.scrollToBottom(true)}>{texts.toBottom}</button>
					<button type="button" className="scrollbar-demo__to"
						onClick={() => growing.current?.scrollTo({ top: POSITION }, true)}>
						{texts.toPosition}</button>
					<button type="button" className="scrollbar-demo__to"
						onClick={() => growing.current?.scrollToElement(line(LINE), true)}>
						{texts.toLine}</button>
				</div>
			</section>
		</div>
	);
}
