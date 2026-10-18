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

/*
 * How the Wicket pages start the apps of a framework. An element an app renders into carries the
 * framework (data-framework-app), the demo (data-demo) and its configuration as JSON
 * (data-config). The tabs of a demo mark the element data-mount the first time it is shown and
 * fire framework-app:mount on it.
 * Every configuration carries the license key of the demo application, which is activated before
 * the first app mounts, so the private components show in every demo.
 */

import { activate } from '@antilia/core';

/**
 * Renders the app of a demo into an element, with the configuration the page gave; returns what
 * takes it away again, if anything.
 */
export type Mount = (element: HTMLElement, config: never) => (() => void) | void;

type Subscriber = (event: unknown, element: unknown) => void;

interface WicketEvents {
	subscribe(topic: string, subscriber: Subscriber): void;
}

const MOUNT_EVENT = 'framework-app:mount';

/** The license key activated, which every configuration carries: the demo application's. */
let activated: string | undefined = undefined;

/**
 * Mounts the apps of a framework into the elements marked for them, now and as they get marked.
 *
 * @param framework the name of the framework, as data-framework-app has it
 * @param apps the apps, by the names of their demos
 */
export function startApps(framework: string, apps: Record<string, Mount>): void {
	const mounted = new WeakSet<HTMLElement>();
	const unmounts = new Map<HTMLElement, () => void>();
	const mount = (element: HTMLElement) => {
		if (mounted.has(element) || element.dataset.frameworkApp !== framework) {
			return;
		}
		const app = apps[element.dataset.demo ?? ''];
		if (!app) {
			console.error(`No ${framework} app for the demo "${element.dataset.demo}"`);
			return;
		}
		mounted.add(element);
		const config = JSON.parse(element.dataset.config ?? '{}') as { licenseKey?: string };
		if (config.licenseKey && config.licenseKey !== activated) {
			// the key of the demo: the demo shows every private component
			activated = config.licenseKey;
			activate(config.licenseKey);
		}
		const unmount = app(element, config as never);
		if (unmount) {
			unmounts.set(element, unmount);
		}
	};
	const selector = `[data-framework-app="${framework}"][data-mount]`;
	document.querySelectorAll<HTMLElement>(selector).forEach(mount);
	document.addEventListener(MOUNT_EVENT, (event) => {
		if (event.target instanceof HTMLElement) {
			mount(event.target);
		}
	});
	// an update of a Wicket page by Ajax renders the elements of apps again, in the windows of a
	// desktop for example: the apps of the elements it adds mount, those of the elements it removes
	// go
	const events = (window as { Wicket?: { Event?: WicketEvents } }).Wicket?.Event;
	events?.subscribe('/dom/node/added', (_event, added) => {
		if (added instanceof HTMLElement) {
			[added, ...Array.from(added.querySelectorAll<HTMLElement>(selector))]
				.filter((element) => element.matches(selector)).forEach(mount);
		}
	});
	events?.subscribe('/dom/node/removing', (_event, removing) => {
		if (removing instanceof Element) {
			unmounts.forEach((unmount, element) => {
				if (removing.contains(element)) {
					unmounts.delete(element);
					unmount();
				}
			});
		}
	});
}
