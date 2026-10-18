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

import { useState } from 'react';

import { Button, Dropdown, Popover, Tab, Tabs, type TabsDesign } from '@antilia/react';

import { dropdownTexts, format } from '../data.js';
import { documentsOf, newDocument, popoverOf, type Document, type GalleryConfig,
	type TabsConfig } from '../tabs.js';
import { Icon } from './DialogOptionsDemo.js';

/**
 * The Tabs demo in React: the tabs of the Wicket page, in its markup. The app keeps the documents:
 * a tab is marked while its text differs from the one saved, and closing it takes the document
 * away. Its action shows what the app knows of the document in a popover pointing at it.
 */
export function TabsDemo({ config }: { config: TabsConfig }) {
	const { texts, icons } = config;
	const [documents, setDocuments] = useState<Document[]>(() => documentsOf(config));
	const [selected, setSelected] = useState('welcome');
	const [untitled, setUntitled] = useState(0);
	const [message, setMessage] = useState(texts.start);
	const [vertical, setVertical] = useState(config.vertical);
	const [design, setDesign] = useState<TabsDesign>(config.design);
	const [section, setSection] = useState(config.sections[0]);

	const update = (id: string, change: Partial<Document>) => setDocuments((all) =>
		all.map((document) => document.id === id ? { ...document, ...change } : document));

	return (
		<div className="tabs-demo">
			<Tabs design="pills" label={texts.cases}>
				<Tab id="documentsTitle" title={texts.documentsTitle} modified={false}>
					<section>
						<div className="tabs-demo__heading">
							<Button label={texts.newDocument} size="small" className="tabs-demo__new"
								onClick={() => {
									const document = newDocument(config, untitled + 1);
									setUntitled(untitled + 1);
									setDocuments([...documents, document]);
									setSelected(document.id);
									setMessage(format(texts.opened, document.name));
								}} />
						</div>
						<Tabs className="tabs-demo__documents" label={texts.documents} texts={config.tabs}
							selected={selected} onSelect={setSelected}
							onClose={(id) => {
								const closed = documents.find((document) => document.id === id);
								setDocuments(documents.filter((document) => document.id !== id));
								if (closed) {
									setMessage(format(texts.closed, closed.name));
								}
							}}>
							<Tab id="welcome" title={texts.welcome} icon={<Icon markup={icons.welcome} />}>
								<p className="tabs-demo__text">{texts.welcomeText}</p>
							</Tab>
							{documents.map((document) => (
								<Tab key={document.id} id={document.id} title={document.name} closable
									modified={document.text !== document.saved}
									icon={<Icon markup={icons.document} />}
									actions={<button type="button" title={texts.about} aria-label={texts.about}
										onClick={(event) => Popover.toggle(event.currentTarget,
											popoverOf(config, document))}>
										<Icon markup={icons.info} />
									</button>}>
									<form className="tabs-demo__document" onSubmit={(event) => {
										event.preventDefault();
										update(document.id, { saved: document.text });
										setMessage(format(texts.saved, document.name));
									}}>
										<textarea className="tabs-demo__editor" rows={6} aria-label={document.name}
											value={document.text}
											onChange={(event) => update(document.id, { text: event.currentTarget.value })} />
										<div><Button type="submit" label={texts.save} variant="primary" size="small" /></div>
									</form>
								</Tab>
							))}
						</Tabs>
						<p className="tabs-demo__status" role="status">{message}</p>
					</section>
				</Tab>
				<Tab id="settingsTitle" title={texts.settingsTitle} modified={false}>
					<section>
						<div className="tabs-demo__controls">
							<label className="tabs-demo__control">
								<input type="checkbox" checked={vertical}
									onChange={(event) => setVertical(event.currentTarget.checked)} />
								{' '}{texts.vertical}
							</label>
							<label className="tabs-demo__control">
								{texts.design}{' '}
								<Dropdown value={design} options={config.designs.map((each) =>
									({ value: each, label: texts['design.' + each.toUpperCase()] }))}
									onChange={(value) => setDesign(value as TabsDesign)}
									{...dropdownTexts(texts)} />
							</label>
						</div>
						<Tabs className="tabs-demo__settings" label={texts.settings} texts={config.tabs}
							orientation={vertical ? 'vertical' : 'horizontal'} design={design} selected={section}
							onSelect={setSection}>
							{config.sections.map((name) => (
								<Tab key={name} id={name} title={texts[name]}>{texts[name + '.text']}</Tab>
							))}
						</Tabs>
						<p className="tabs-demo__status">{format(texts.selected, texts[section])}</p>
					</section>
				</Tab>
				<Tab id="galleryTitle" title={texts.galleryTitle} modified={false}>
					<section>
						<h4 className="tabs-demo__group">{texts.alongTheTop}</h4>
						<Gallery config={config} part={config.gallery.top} orientation="horizontal" />
						<h4 className="tabs-demo__group">{texts.downTheSide}</h4>
						<Gallery config={config} part={config.gallery.side} orientation="vertical" />
					</section>
				</Tab>
			</Tabs>
		</div>
	);
}

/**
 * A part of the gallery: tabs in each of its designs, under the name of the design.
 */
function Gallery({ config, part, orientation }: { config: TabsConfig; part: GalleryConfig;
	orientation: 'horizontal' | 'vertical' }) {
	const { texts } = config;
	return (
		<div className={'tabs-demo__gallery tabs-demo__gallery--' +
			(orientation === 'horizontal' ? 'top' : 'side')}>
			{part.designs.map((design) => {
				const name = texts['design.' + design.toUpperCase()];
				return (
					<figure key={design} className="tabs-demo__example">
						<figcaption>{name}</figcaption>
						<Tabs design={design} orientation={orientation} label={name} texts={config.tabs}
							selected={part.selected}>
							{part.tabs.map((tab) => (
								<Tab key={tab.name} id={tab.name} title={texts[tab.name]}
									icon={<Icon markup={tab.icon} />}>{texts[tab.name + '.text']}</Tab>
							))}
						</Tabs>
					</figure>
				);
			})}
		</div>
	);
}
