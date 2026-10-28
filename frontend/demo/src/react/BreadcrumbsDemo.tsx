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

import { useState, type MouseEvent } from 'react';

import { Breadcrumb, Dropdown, Tab, Tabs, type BreadcrumbItem,
	type BreadcrumbSeparator } from '@antilia/react';

import { dropdownTexts } from '../data.js';
import { below, folderCrumbs, label, maxItemsLabel, placeAt, settingsCrumbs, siteCrumbs, siteHref,
	type BreadcrumbsConfig, type DemoCrumb, type Place } from '../breadcrumbs.js';

/** The markup of an icon, as the page gives it. */
function PlaceIcon({ markup }: { markup?: string }) {
	return markup ? <span className="breadcrumbs-demo__icon"
		dangerouslySetInnerHTML={{ __html: markup }} /> : null;
}

/**
 * @returns the crumbs of the demo as React's Breadcrumb takes them
 */
function items(crumbs: DemoCrumb[]): BreadcrumbItem[] {
	return crumbs.map((crumb) => ({ id: crumb.id, label: crumb.label, href: crumb.href,
		icon: crumb.icon ? <PlaceIcon markup={crumb.icon} /> : undefined,
		onClick: crumb.action && ((event) => crumb.action!(event.nativeEvent)) }));
}

/**
 * The Breadcrumbs demo in React: the breadcrumbs of the Wicket page, in its markup. The app keeps
 * the page of the site and the folder open, and says which crumb of the settings the user chose.
 */
export function BreadcrumbsDemo({ config }: { config: BreadcrumbsConfig }) {
	const { texts } = config;
	const [sitePath, setSitePath] = useState<string[]>([]);
	const [folder, setFolder] = useState(config.folder);
	const [separator, setSeparator] = useState<BreadcrumbSeparator>(config.separator);
	const [maxItems, setMaxItems] = useState(config.maxItems);
	const [message, setMessage] = useState(texts.settingsAppStart);
	const page = placeAt(config.site, sitePath);
	const open = placeAt(config.files, folder);

	const place = (each: Place, onClick: (event: MouseEvent) => void, href?: string) => {
		const content = <><PlaceIcon markup={each.icon} /><span>{label(config, each)}</span></>;
		return <li key={each.name}>{href !== undefined ?
			<a className="breadcrumbs-demo__place" href={href} onClick={onClick}>{content}</a> :
			<button type="button" className="breadcrumbs-demo__place" onClick={onClick}>{content}</button>}
		</li>;
	};

	return (
		<div className="breadcrumbs-demo">
			<Tabs design="pills" label={texts.cases}>
				<Tab id="siteTitle" title={texts.siteTitle} modified={false}>
					<section>
						<p className="breadcrumbs-demo__intro">{texts.siteAppIntro}</p>
						<Breadcrumb texts={config.breadcrumbTexts}
							items={items(siteCrumbs(config, sitePath, setSitePath))} />
						<div className="breadcrumbs-demo__page">
							<h4 className="breadcrumbs-demo__heading">{label(config, page)}</h4>
							<p className="breadcrumbs-demo__text">{below(config, page)}</p>
							<ul className="breadcrumbs-demo__places">
								{(page.children ?? []).map((child) => place(child, (event) => {
									event.preventDefault();
									setSitePath([...sitePath, child.name]);
								}, siteHref([...sitePath, child.name])))}
							</ul>
						</div>
					</section>
				</Tab>
				<Tab id="foldersTitle" title={texts.foldersTitle} modified={false}>
					<section>
						<p className="breadcrumbs-demo__intro">{texts.foldersAppIntro}</p>
						<Breadcrumb texts={{ ...config.breadcrumbTexts, label: texts.folders }}
							maxItems={config.foldersMaxItems}
							items={items(folderCrumbs(config, folder, setFolder))} />
						<div className="breadcrumbs-demo__page">
							<h4 className="breadcrumbs-demo__heading">{label(config, open)}</h4>
							<ul className="breadcrumbs-demo__places">
								{(open.children ?? []).map((child) => place(child,
									() => setFolder([...folder, child.name])))}
							</ul>
							{!open.children?.length && <p className="breadcrumbs-demo__text">{texts.empty}</p>}
						</div>
					</section>
				</Tab>
				<Tab id="settingsTitle" title={texts.settingsTitle} modified={false}>
					<section>
						<p className="breadcrumbs-demo__intro">{texts.settingsAppIntro}</p>
						<div className="breadcrumbs-demo__controls">
							<label className="breadcrumbs-demo__control">
								{texts.separator}{' '}
								<Dropdown value={separator} options={config.separators.map((each) =>
									({ value: each, label: texts['separator.' + each.toUpperCase()] }))}
									onChange={(value) => setSeparator(value as BreadcrumbSeparator)}
									{...dropdownTexts(texts)} />
							</label>
							<label className="breadcrumbs-demo__control">
								{texts.maxItems}{' '}
								<Dropdown value={String(maxItems)} options={config.maxItemsChoices.map((each) =>
									({ value: String(each), label: maxItemsLabel(config, each) }))}
									onChange={(value) => setMaxItems(Number(value))}
									{...dropdownTexts(texts)} />
							</label>
						</div>
						<div className="breadcrumbs-demo__preview">
							<Breadcrumb texts={config.breadcrumbTexts} separator={separator}
								maxItems={maxItems} items={items(settingsCrumbs(config, setMessage))} />
						</div>
						<p className="breadcrumbs-demo__status" role="status">{message}</p>
					</section>
				</Tab>
			</Tabs>
		</div>
	);
}
