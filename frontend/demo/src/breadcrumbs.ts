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

/*
 * The Breadcrumbs demo: the path of a site, its crumbs links the app follows as a router does; a
 * drill-down through folders, a crumb added for the folder opened and the trail going back to the
 * folder of a crumb chosen, at most a few crumbs shown; and a long trail whose separator and number
 * of crumbs shown controls choose.
 */

import type { BreadcrumbSeparator, BreadcrumbTexts } from '@antilia/core';

import { format, type DropdownTexts } from './data.js';

/** The texts of the demo, by the keys of the page's bundle: the labels of the places among them. */
export interface BreadcrumbsDemoTexts extends DropdownTexts {
	/** The accessible name of the pills of the cases. */
	cases: string;
	siteTitle: string;
	siteAppIntro: string;
	/** What the page says above the pages below it, {0} its label. */
	siteBelow: string;
	/** What a page without pages below it says, {0} its label. */
	siteLeaf: string;
	foldersTitle: string;
	foldersAppIntro: string;
	/** The accessible name of the trail of the folders. */
	folders: string;
	empty: string;
	settingsTitle: string;
	settingsAppIntro: string;
	settingsAppStart: string;
	separator: string;
	maxItems: string;
	all: string;
	/** What the app says of a crumb chosen, {0} its label. */
	picked: string;
	[key: string]: string;
}

/** A place of a tree, a page of the site or a folder, as the page gives it. */
export interface Place {
	name: string;
	/** The markup of its icon. */
	icon?: string;
	children?: Place[];
}

/** What the page gives the demo. */
export interface BreadcrumbsConfig {
	texts: BreadcrumbsDemoTexts;
	/** The texts of the breadcrumbs, in the language of the page. */
	breadcrumbTexts: BreadcrumbTexts;
	site: Place;
	files: Place;
	/** The folder the drill-down starts at, the names down to it. */
	folder: string[];
	foldersMaxItems: number;
	/** The trail of the settings, the names down to its last folder. */
	longTrail: string[];
	separator: BreadcrumbSeparator;
	maxItems: number;
	separators: BreadcrumbSeparator[];
	maxItemsChoices: number[];
}

/** A crumb of the demo, as the frameworks' Breadcrumb takes it but for the icon and the click. */
export interface DemoCrumb {
	id: string;
	label: string;
	/** The markup of its icon. */
	icon?: string;
	href?: string;
	action?: (event: MouseEvent) => void;
}

/**
 * @returns the label of a place, in the language of the page
 */
export function label(config: BreadcrumbsConfig, place: Place): string {
	return config.texts['place.' + place.name] ?? place.name;
}

/**
 * @returns the root and the places of the path below it, down to the last; null when there is no
 *          such place
 */
export function placesAt(root: Place, path: string[]): Place[] | null {
	const places = [root];
	for (const name of path) {
		const next = places[places.length - 1].children?.find((child) => child.name === name);
		if (!next) {
			return null;
		}
		places.push(next);
	}
	return places;
}

/**
 * @returns the place at the end of a path, the root for a path leading nowhere
 */
export function placeAt(root: Place, path: string[]): Place {
	const places = placesAt(root, path);
	return places ? places[places.length - 1] : root;
}

/**
 * @returns the address of a page of the site in the app, an anchor
 */
export function siteHref(path: string[]): string {
	return '#site=' + path.join('/');
}

/**
 * @param go called with the path of the page a crumb leads to, which the app shows
 * @returns the crumbs of the site: links to the pages down to the one shown, which the app follows
 */
export function siteCrumbs(config: BreadcrumbsConfig, path: string[],
	go: (path: string[]) => void): DemoCrumb[] {
	return (placesAt(config.site, path) ?? [config.site]).map((place, index) => {
		const prefix = path.slice(0, index);
		return { id: place.name, label: label(config, place), icon: index ? undefined : place.icon,
			href: siteHref(prefix), action: (event: MouseEvent) => {
				event.preventDefault();
				go(prefix);
			} };
	});
}

/**
 * @param go called with the path of the folder of a crumb, which the app opens
 * @returns the crumbs of the drill-down: buttons going back to the folders down to the one open
 */
export function folderCrumbs(config: BreadcrumbsConfig, folder: string[],
	go: (folder: string[]) => void): DemoCrumb[] {
	return (placesAt(config.files, folder) ?? [config.files]).map((place, index) => ({
		id: place.name, label: label(config, place), icon: index ? undefined : place.icon,
		action: () => go(folder.slice(0, index))
	}));
}

/**
 * @param say called with what the app says of the crumb chosen
 * @returns the crumbs of the settings: the long trail, buttons saying they were chosen
 */
export function settingsCrumbs(config: BreadcrumbsConfig, say: (message: string) => void):
	DemoCrumb[] {
	return (placesAt(config.files, config.longTrail) ?? []).map((place, index) => ({
		id: place.name, label: label(config, place), icon: index ? undefined : place.icon,
		action: () => say(format(config.texts.picked, label(config, place)))
	}));
}

/**
 * @returns what the page says above the pages below a page of the site
 */
export function below(config: BreadcrumbsConfig, place: Place): string {
	return format(place.children?.length ? config.texts.siteBelow : config.texts.siteLeaf,
		label(config, place));
}

/**
 * @returns the label of a choice of how many crumbs show
 */
export function maxItemsLabel(config: BreadcrumbsConfig, maxItems: number): string {
	return maxItems ? String(maxItems) : config.texts.all;
}
