/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { useState } from 'react';

import { Button, Resizable, type ResizableSize } from '@antilia/react';

import { describe, type ResizableConfig } from '../resizable.js';

/**
 * The Resizable demo in React: the resizable boxes of the Wicket page, in its markup. The app keeps
 * their sizes, and gives the card the size it had when the button puts it back.
 */
export function ResizableDemo({ config }: { config: ResizableConfig }) {
	const { texts } = config;
	const [note, setNote] = useState<number | null>(null);
	const [side, setSide] = useState<number | null>(null);
	const [card, setCard] = useState<ResizableSize>({ width: config.card.width ?? null,
		height: config.card.height ?? null });
	const [cardShown, setCardShown] = useState(true);

	return (
		<div className="resizable-demo">
			<section>
				<h3>{texts.noteTitle}</h3>
				<Resizable className="resizable-demo__note" axis="vertical" height={note} minHeight={80}
					maxHeight={480} label={config.handle} onResized={(size) => setNote(size.height)}>
					<p>{texts.noteText}</p>
				</Resizable>
			</section>
			<section>
				<h3>{texts.sideTitle}</h3>
				<div className="resizable-demo__layout">
					<Resizable className="resizable-demo__side" axis="horizontal" width={side} minWidth={140}
						maxWidth={520} label={config.handle} onResized={(size) => setSide(size.width)}>
						<ul>{config.items.map((item) => <li key={item}>{texts[item]}</li>)}</ul>
					</Resizable>
					<div className="resizable-demo__main" />
				</div>
			</section>
			<section>
				<h3>{texts.cardTitle}</h3>
				<div className="resizable-demo__heading">
					<Button label={cardShown ? texts.takeCardAway : texts.putCardBack} variant="secondary"
						size="small" onClick={() => setCardShown(!cardShown)} />
				</div>
				<div className="resizable-demo__place">
					{cardShown && <Resizable className="resizable-demo__card" width={card.width}
						height={card.height} minWidth={180} minHeight={100} label={config.handle}
						onResized={setCard}>
						<p>{texts.cardText}</p>
					</Resizable>}
				</div>
			</section>
			<p className="resizable-demo__sizes" role="status">{describe(config, note, side,
				cardShown ? card : null)}</p>
		</div>
	);
}
