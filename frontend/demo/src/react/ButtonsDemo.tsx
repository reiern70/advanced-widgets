/*
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

import { useState } from 'react';

import { Button, buttonClasses, Dropdown, Scrollable, useLook } from '@antilia/react';

import { BUTTON_SIZES, BUTTON_VARIANTS, clickedText, iconSetOptions, sizeName,
	type ButtonIconSet, type ButtonsConfig } from '../buttons.js';

/** An icon the server wrote, an inline SVG or the element of an icon font. */
function Icon({ markup }: { markup: string }) {
	return <span style={{ display: 'contents' }} dangerouslySetInnerHTML={{ __html: markup }} />;
}

/**
 * The Buttons demo in React: the buttons of the Wicket page with the Button of
 * @antilia/react, the drop-down above them choosing their icons.
 */
export function ButtonsDemo({ config }: { config: ButtonsConfig }) {
	const { texts, variants } = config;
	const [clicked, setClicked] = useState<string | null>(null);
	const [icons, setIcons] = useState<ButtonIconSet>(config.icons);
	// a link drawn as a button, in the look of the page as the buttons
	const look = useLook();

	return (
		<div className="buttons-demo">
			<div className="buttons-demo__options">
				<label htmlFor="react-buttons-icons">{texts.icons}</label>
				<Dropdown id="react-buttons-icons" name="icons" value={icons}
					options={iconSetOptions(texts)}
					onChange={(value) => setIcons(value as ButtonIconSet)} filter={texts.filter}
					noMatch={texts.noMatch} more={texts.more} remove={texts.remove} />
			</div>
			<p className="buttons-demo__clicked" role="status">{clicked ?? texts['clicked.none']}</p>
			<Scrollable axis="x" className="buttons-demo__scroll">
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
											<div className="ant-button-bar">
												<Button label={sample.text} variant={variant} size={size}
													onClick={click} />
												<Button label={sample.text} variant={variant} size={size}
													icon={<Icon markup={sample.icons[icons]} />} onClick={click} />
												<Button label={sample.text} variant={variant} size={size}
													icon={<Icon markup={sample.icons[icons]} />} iconOnly onClick={click} />
											</div>
										</td>
									);
								})}
							</tr>
						))}
					</tbody>
				</table>
			</Scrollable>
			<h3>{texts.disabled}</h3>
			<div className="ant-button-bar">
				{BUTTON_VARIANTS.map((variant) => (
					<Button key={variant} label={variants[variant].text} variant={variant} disabled
						icon={<Icon markup={variants[variant].icons[icons]} />} />
				))}
			</div>
			<h3>{texts.asLink}</h3>
			<p>
				<a href="#buttons-as-links"
					className={buttonClasses({ variant: 'ghost', size: 'small', look })}>
					{texts['asLink.text']}
				</a>
			</p>
		</div>
	);
}
