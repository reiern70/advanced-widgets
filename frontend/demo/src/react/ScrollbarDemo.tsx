/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { useState, type MouseEvent } from 'react';

import { Scrollable } from '@advanced-widgets/react';

import { lines, MORE_LINES, TILES, type ScrollbarConfig } from '../scrollbar.js';

function Lines({ pattern, count }: { pattern: string; count: number }) {
	return <>{lines(pattern, count).map((line, index) => <p key={index}>{line}</p>)}</>;
}

/**
 * The Scrollbar demo in React: the boxes of the Wicket page, those with slim bars a Scrollable, the
 * last one growing in the app.
 */
export function ScrollbarDemo({ config }: { config: ScrollbarConfig }) {
	const { texts } = config;
	const [count, setCount] = useState(config.lines);
	const add = (event: MouseEvent) => {
		event.preventDefault();
		setCount((current) => current + MORE_LINES);
	};

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
				<Scrollable className="scrollbar-demo__box scrollbar-demo__box--short">
					<Lines pattern={texts.line} count={count} />
				</Scrollable>
				<p><a href="#" onClick={add}>{texts.addLines}</a></p>
			</section>
		</div>
	);
}
