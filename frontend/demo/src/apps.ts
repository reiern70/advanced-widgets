/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * How the Wicket pages start the apps of a framework. An element an app renders into carries the
 * framework (data-framework-app), the demo (data-demo) and its configuration as JSON
 * (data-config). The tabs of a demo mark the element data-mount the first time it is shown and
 * fire framework-app:mount on it; a page showing an app right away renders it marked already.
 */

/** Renders the app of a demo into an element, with the configuration the page gave. */
export type Mount = (element: HTMLElement, config: never) => void;

const MOUNT_EVENT = 'framework-app:mount';

/**
 * Mounts the apps of a framework into the elements marked for them, now and as they get marked.
 *
 * @param framework the name of the framework, as data-framework-app has it
 * @param apps the apps, by the names of their demos
 */
export function startApps(framework: string, apps: Record<string, Mount>): void {
	const mounted = new WeakSet<HTMLElement>();
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
		app(element, JSON.parse(element.dataset.config ?? '{}') as never);
	};
	document.querySelectorAll<HTMLElement>(`[data-framework-app="${framework}"][data-mount]`)
		.forEach(mount);
	document.addEventListener(MOUNT_EVENT, (event) => {
		if (event.target instanceof HTMLElement) {
			mount(event.target);
		}
	});
}
