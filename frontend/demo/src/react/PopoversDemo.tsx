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

import { useState, type FormEvent, type ReactNode } from 'react';

import { buttonClasses, partClasses, popoverClasses, type ButtonVariant,
	type Look } from '@antilia/core';
import { Dropdown, Popover, PopoverButton, Tab, Tabs, useLook, type PopoverDesign, type PopoverOptions,
	type PopoverPlacement, type PopoverVariant } from '@antilia/react';

import { dropdownTexts, format } from '../data.js';
import { facts, nameOf, placementText, sideOf, type PopoversConfig } from '../popovers.js';
import { Icon } from './DialogOptionsDemo.js';

/**
 * The Popovers demo in React: the popovers of the Wicket page, opened by PopoverButton, whose
 * content React renders into them: the facts of the browser as the popover opens, and a form
 * renaming the project.
 */
export function PopoversDemo({ config }: { config: PopoversConfig }) {
	const { texts, icons } = config;
	const look = useLook();
	const [design, setDesign] = useState<PopoverDesign>(config.design);
	const [variant, setVariant] = useState<PopoverVariant>(config.variant);
	const [position, setPosition] = useState<PopoverPlacement>(config.position);
	const [project, setProject] = useState(config.project);
	const [opened, setOpened] = useState(0);
	const closeLabel = texts.close;

	return (
		<div className="popovers-demo">
			<Tabs design="pills" label={texts.cases}>
				<Tab id="placementsTitle" title={texts.placementsTitle} modified={false}>
					<section>
						<p className="popovers-demo__text">{texts.placementsText}</p>
						<div className="popovers-demo__controls">
							<label className="popovers-demo__control">
								{texts.design}{' '}
								<Dropdown value={design} options={config.designs.map((each) =>
									({ value: each, label: nameOf(config, 'design', each) }))}
									onChange={(value) => setDesign(value as PopoverDesign)}
									{...dropdownTexts(texts)} />
							</label>
							<label className="popovers-demo__control">
								{texts.variant}{' '}
								<Dropdown value={variant} options={config.variants.map((each) =>
									({ value: each, label: nameOf(config, 'variant', each) }))}
									onChange={(value) => setVariant(value as PopoverVariant)}
									{...dropdownTexts(texts)} />
							</label>
						</div>
						<div className="popovers-demo__compass">
							<div className="popovers-demo__target" aria-hidden="true" />
							{config.placements.map((placement) => {
								const name = nameOf(config, 'placement', placement);
								return (
									<PopoverButton key={placement} className={buttonClasses({ look, size: 'small',
										variant: placement === 'auto' ? 'primary' : 'secondary',
										className: 'popovers-demo__place popovers-demo__place--' + placement })}
										options={{ title: name, body: placementText(config, placement), placement,
											design, variant, maxWidth: config.placementWidth, closeLabel }}>
										<Body label={name} icon={placement === 'auto' ? icons.auto : undefined} />
									</PopoverButton>
								);
							})}
						</div>
					</section>
				</Tab>
				<Tab id="galleryTitle" title={texts.galleryTitle} modified={false}>
					<section>
						<div className="popovers-demo__controls">
							<label className="popovers-demo__control">
								{texts.position}{' '}
								<Dropdown value={position} options={config.positions.map((each) =>
									({ value: each, label: nameOf(config, 'placement', each) }))}
									onChange={(value) => setPosition(value as PopoverPlacement)}
									{...dropdownTexts(texts)} />
							</label>
						</div>
						<div className="popovers-demo__gallery">
							{config.designs.map((each) => {
								const name = nameOf(config, 'design', each);
								const text = texts['design.' + each.toUpperCase() + '.text'];
								return (
									<figure key={each} className="popovers-demo__example">
										<div className="popovers-demo__stage">
											<Preview design={each} variant={variant} position={position} title={name}
												text={text} />
										</div>
										<PopoverButton className={buttonClasses({ look, size: 'small' })}
											options={{ title: name, body: text, design: each, variant, placement: position,
												maxWidth: config.galleryWidth, closeLabel }}>
											<Body label={texts.try} />
										</PopoverButton>
									</figure>
								);
							})}
						</div>
					</section>
				</Tab>
				<Tab id="richTitle" title={texts.richTitle} modified={false}>
					<section>
						<div className="popovers-demo__rich">
							<PopoverButton className={buttonClasses({ look })} onOpen={() => setOpened(opened + 1)}
								options={{ title: texts.statusTitle, body: texts.statusText,
									placement: 'bottom-left', maxWidth: '24rem', closeLabel }}
								content={<dl className="popovers-demo__facts">
									{facts(opened).map(([key, value]) => <Fact key={key} name={texts['fact.' + key]}
										value={value} />)}
								</dl>}>
								<Body label={texts['status.app']} icon={icons.status} />
							</PopoverButton>
							<p className="popovers-demo__project">
								{texts.project} <strong>{project}</strong>
								<PopoverButton {...iconOnly(texts.rename, 'ghost', look)}
									options={{ title: texts.renameTitle, placement: 'right-top', design: 'aurora',
										closeLabel }}
									content={<Rename config={config} project={project} onRename={setProject} />}>
									<Body label={texts.rename} icon={icons.rename} iconOnly />
								</PopoverButton>
							</p>
						</div>
					</section>
				</Tab>
				<Tab id="fieldsTitle" title={texts.fieldsTitle} modified={false}>
					<section>
						<form className="popovers-demo__fields" onSubmit={(event) => event.preventDefault()}>
							{config.fields.map((field) => {
								const help = format(texts.help, field.label);
								return (
									<div key={field.key} className="popovers-demo__field">
										<div className="popovers-demo__label">
											<label htmlFor={'react-' + field.key}>{field.label}</label>
											<PopoverButton {...iconOnly(help, 'ghost', look)} options={helpOf(field)}>
												<Body label={help} icon={icons.help} iconOnly />
											</PopoverButton>
										</div>
										<input id={'react-' + field.key} className="popovers-demo__input"
											type={field.key === 'password' ? 'password' : 'text'} />
									</div>
								);
							})}
						</form>
					</section>
				</Tab>
				<Tab id="tableTitle" title={texts.tableTitle} modified={false}>
					<section>
						<div className="popovers-demo__scroll">
							<table className="popovers-demo__table">
								<thead>
									<tr><th scope="col">{texts.country}</th><th scope="col">{texts.language}</th>
										<th scope="col">{texts.currency}</th><th scope="col" /></tr>
								</thead>
								<tbody>
									{config.countries.map((country) => {
										const about = format(texts.aboutCountry, country.name);
										return (
											<tr key={country.code}>
												<td>{country.name}</td><td>{country.language}</td><td>{country.currency}</td>
												<td className="popovers-demo__action">
													<PopoverButton {...iconOnly(about, 'ghost', look)}
														options={{ title: country.name, body: country.about, placement: 'left-center',
															design: 'callout', closeLabel }}>
														<Body label={about} icon={icons.info} iconOnly />
													</PopoverButton>
												</td>
											</tr>
										);
									})}
								</tbody>
							</table>
						</div>
					</section>
				</Tab>
			</Tabs>
		</div>
	);

	function helpOf(field: PopoversConfig['fields'][number]): PopoverOptions {
		return { title: field.label, body: field.help, placement: 'right-center', design: 'inverse',
			closeLabel };
	}
}

/**
 * @returns the class, the title and the name of a small button showing only its icon
 */
function iconOnly(label: string, variant: ButtonVariant, look: Look) {
	return { className: buttonClasses({ size: 'small', variant, iconOnly: true, look }), title: label,
		'aria-label': label };
}

/**
 * The body of a button in the markup of the Wicket buttons: its icon, its text or both.
 */
function Body({ label, icon, iconOnly = false }: { label: string; icon?: string;
	iconOnly?: boolean }) {
	return (
		<>
			{icon && <span className="ant-button__icon" aria-hidden="true"><Icon markup={icon} /></span>}
			{!iconOnly && <span className="ant-button__label">{label}</span>}
		</>
	);
}

function Fact({ name, value }: { name: string; value: string }): ReactNode {
	return <><dt>{name}</dt><dd>{value}</dd></>;
}

/**
 * A popover of a design drawn open in the page, in the markup the core gives it, on the side and
 * along the edge of a position.
 */
function Preview({ design, variant, position, title, text }: { design: PopoverDesign;
	variant: PopoverVariant; position: PopoverPlacement; title: string; text: string }) {
	const { side, align } = sideOf(position);
	const look = useLook();
	return (
		<div className={popoverClasses(look, design, variant)} data-side={side} data-align={align}>
			<div className={partClasses(look, 'popover-arrow')} />
			<div className={partClasses(look, 'popover-header')}>
				<div className={partClasses(look, 'popover-title')}>{title}</div>
				<span className={partClasses(look, 'popover-close')} aria-hidden="true">
					<svg viewBox="0 0 24 24" width="12" height="12" focusable="false">
						<path d="M6 6l12 12 M18 6L6 18" fill="none" stroke="currentColor" strokeWidth="2.5"
							strokeLinecap="round" />
					</svg>
				</span>
			</div>
			<div className={partClasses(look, 'popover-body')}>{text}</div>
		</div>
	);
}

/**
 * The form renaming the project, in its popover: it starts with the name of the project, says
 * what is wrong with the one typed, and closes the popover once it took it.
 */
function Rename({ config, project, onRename }: { config: PopoversConfig; project: string;
	onRename: (name: string) => void }) {
	const { texts } = config;
	const look = useLook();
	const [draft, setDraft] = useState(project);
	const [error, setError] = useState<string | null>(null);

	const save = (event: FormEvent) => {
		event.preventDefault();
		if (draft.trim() === '') {
			setError(texts.required);
			return;
		}
		onRename(draft.trim());
		Popover.closeAll();
	};

	return (
		<form className="popovers-demo__rename" onSubmit={save}>
			<label className="popovers-demo__rename-field">
				{texts.name}{' '}
				<input type="text" value={draft} autoFocus
					onChange={(event) => setDraft(event.currentTarget.value)} />
			</label>
			<p className="popovers-demo__error" role="alert" hidden={error === null}>{error}</p>
			<div className="popovers-demo__actions">
				<button type="submit" className={buttonClasses({ look, size: 'small', variant: 'primary' })}>
					<span className="ant-button__label">{texts.save}</span>
				</button>
				<button type="button" className={buttonClasses({ look, size: 'small', variant: 'ghost' })}
					onClick={() => Popover.closeAll()}>
					<span className="ant-button__label">{texts.cancel}</span>
				</button>
			</div>
		</form>
	);
}
