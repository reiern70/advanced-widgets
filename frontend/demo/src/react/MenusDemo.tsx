/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { useState } from 'react';

import { Dropdown, Menu, Tab, Tabs, type MenuDesign, type MenuEntry,
	type MenuOrientation } from '@antilia/react';

import { dropdownTexts, format } from '../data.js';
import { applicationEntries, at, galleryEntries, navigationEntries, sidebarEntries, describe,
	pathOf, type DemoEntry, type MenusConfig } from '../menus.js';
import { Icon } from './DialogOptionsDemo.js';

/**
 * @returns the items of a menu of the demo as React's Menu takes them
 */
function entries(items: DemoEntry[]): MenuEntry[] {
	return items.map((item) => item.separator ? { id: item.id, separator: true } : {
		id: item.id, label: item.label, tooltip: item.tooltip, disabled: item.disabled,
		href: item.href, current: item.current, onClick: item.action && ((event) => item.action!(
			event.nativeEvent)),
		icon: item.icon ? <Icon markup={item.icon} /> : undefined,
		items: item.items && entries(item.items)
	});
}

/**
 * The Menus demo in React: the menus of the Wicket page, in its markup. The app keeps the section
 * of the navigation and the view of the side bar, whose items are current, and says what the user
 * chose.
 */
export function MenusDemo({ config }: { config: MenusConfig }) {
	const { texts } = config;
	const [applicationMessage, setApplicationMessage] = useState(texts.applicationStart);
	const [section, setSection] = useState(config.section);
	const [view, setView] = useState(config.view);
	const [vertical, setVertical] = useState(config.vertical);
	const [design, setDesign] = useState<MenuDesign>(config.design);
	const [inline, setInline] = useState(config.inline);
	const [hover, setHover] = useState(config.hover);
	const [settingsMessage, setSettingsMessage] = useState(texts.settingsAppStart);

	return (
		<div className="menus-demo">
			<Tabs design="pills" label={texts.cases}>
				<Tab id="applicationTitle" title={texts.applicationTitle} modified={false}>
					<section>
						<p className="menus-demo__intro">{texts.applicationAppIntro}</p>
						<div className="menus-demo__window">
							<nav className="menus-demo__application">
								<Menu label={texts.application}
									items={entries(applicationEntries(config, setApplicationMessage))} />
							</nav>
							<div className="menus-demo__document">{texts.documentText}</div>
						</div>
						<p className="menus-demo__status" role="status">{applicationMessage}</p>
					</section>
				</Tab>
				<Tab id="navigationTitle" title={texts.navigationTitle} modified={false}>
					<section>
						<p className="menus-demo__intro">{texts.navigationAppIntro}</p>
						<nav className="menus-demo__navigation">
							<Menu label={texts.navigation} design="underline" openOnHover
								items={entries(navigationEntries(config, section, setSection))} />
						</nav>
						<div className="menus-demo__page">
							<h4 className="menus-demo__where">{at(config, section)}</h4>
							<p className="menus-demo__text">{format(texts.routed, section)}</p>
						</div>
					</section>
				</Tab>
				<Tab id="sidebarTitle" title={texts.sidebarTitle} modified={false}>
					<section className="menus-demo__app">
						<nav className="menus-demo__sidebar">
							<Menu label={texts.sidebar} orientation="vertical" design="sidebar" inline
								items={entries(sidebarEntries(config, view, setView))} />
						</nav>
						<div className="menus-demo__content">
							<h4 className="menus-demo__where">{describe(config, pathOf(config.sidebar, view))}</h4>
							<p className="menus-demo__text">{texts.sidebarAppText}</p>
						</div>
					</section>
				</Tab>
				<Tab id="settingsTitle" title={texts.settingsTitle} modified={false}>
					<section>
						<div className="menus-demo__controls">
							<label className="menus-demo__control">
								<input type="checkbox" checked={vertical}
									onChange={(event) => setVertical(event.currentTarget.checked)} />
								{' '}{texts.vertical}
							</label>
							<label className="menus-demo__control">
								{texts.design}{' '}
								<Dropdown value={design} options={config.designs.map((each) =>
									({ value: each, label: texts['design.' + each.toUpperCase()] }))}
									onChange={(value) => setDesign(value as MenuDesign)}
									{...dropdownTexts(texts)} />
							</label>
							<label className="menus-demo__control">
								<input type="checkbox" checked={inline}
									onChange={(event) => setInline(event.currentTarget.checked)} />
								{' '}{texts.inline}
							</label>
							<label className="menus-demo__control">
								<input type="checkbox" checked={hover}
									onChange={(event) => setHover(event.currentTarget.checked)} />
								{' '}{texts.hover}
							</label>
						</div>
						<div className="menus-demo__preview">
							<nav>
								<Menu label={texts.application} orientation={vertical ? 'vertical' : 'horizontal'}
									design={design} inline={inline} openOnHover={hover}
									items={entries(applicationEntries(config, setSettingsMessage))} />
							</nav>
						</div>
						<p className="menus-demo__status" role="status">{settingsMessage}</p>
					</section>
				</Tab>
				<Tab id="galleryTitle" title={texts.galleryTitle} modified={false}>
					<section>
						<h4 className="menus-demo__group">{texts.alongTheTop}</h4>
						<Gallery config={config} orientation="horizontal" />
						<h4 className="menus-demo__group">{texts.downTheSide}</h4>
						<Gallery config={config} orientation="vertical" />
					</section>
				</Tab>
			</Tabs>
		</div>
	);
}

/**
 * A part of the gallery: a menu in each design, under the name of the design.
 */
function Gallery({ config, orientation }: { config: MenusConfig; orientation: MenuOrientation }) {
	const items = entries(galleryEntries(config));
	return (
		<div className={'menus-demo__gallery menus-demo__gallery--' +
			(orientation === 'horizontal' ? 'top' : 'side')}>
			{config.designs.map((design) => {
				const name = config.texts['design.' + design.toUpperCase()];
				return (
					<figure key={design} className="menus-demo__example">
						<figcaption>{name}</figcaption>
						<nav><Menu label={name} design={design} orientation={orientation} items={items} /></nav>
					</figure>
				);
			})}
		</div>
	);
}
