/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { useState } from 'react';

import { Button } from '@advanced-widgets/react';

import { BUTTON_SIZES, BUTTON_VARIANTS, clickedText, sizeName,
	type ButtonsConfig } from '../buttons.js';

/** An icon the server wrote, an inline SVG. */
function Icon({ markup }: { markup: string }) {
	return <span style={{ display: 'contents' }} dangerouslySetInnerHTML={{ __html: markup }} />;
}

/**
 * The Buttons demo in React: the buttons of the Wicket page with the Button of
 * @advanced-widgets/react.
 */
export function ButtonsDemo({ config }: { config: ButtonsConfig }) {
	const { texts, variants } = config;
	const [clicked, setClicked] = useState<string | null>(null);

	return (
		<div className="buttons-demo">
			<p className="buttons-demo__clicked" role="status">{clicked ?? texts['clicked.none']}</p>
			<div className="buttons-demo__scroll">
				<table className="buttons-demo__matrix">
					<thead>
						<tr>
							<th scope="col">{texts.variant}</th>
							{BUTTON_SIZES.map((size) => <th key={size} scope="col">{sizeName(config, size)}</th>)}
						</tr>
					</thead>
					<tbody>
						{BUTTON_VARIANTS.map((variant) => (
							<tr key={variant}>
								<th scope="row">{variants[variant].name}</th>
								{BUTTON_SIZES.map((size) => {
									const sample = variants[variant];
									const click = () => setClicked(clickedText(config, variant, size));
									return (
										<td key={size}>
											<div className="aw-button-bar">
												<Button label={sample.text} variant={variant} size={size}
													onClick={click} />
												<Button label={sample.text} variant={variant} size={size}
													icon={<Icon markup={sample.icon} />} onClick={click} />
												<Button label={sample.text} variant={variant} size={size}
													icon={<Icon markup={sample.icon} />} iconOnly onClick={click} />
											</div>
										</td>
									);
								})}
							</tr>
						))}
					</tbody>
				</table>
			</div>
			<h3>{texts.disabled}</h3>
			<div className="aw-button-bar">
				{BUTTON_VARIANTS.map((variant) => (
					<Button key={variant} label={variants[variant].text} variant={variant} disabled
						icon={<Icon markup={variants[variant].icon} />} />
				))}
			</div>
			<h3>{texts.asLink}</h3>
			<p>
				<a href="#buttons-as-links" className="aw-button aw-button--ghost aw-button--small">
					{texts['asLink.text']}
				</a>
			</p>
		</div>
	);
}
