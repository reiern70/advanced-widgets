/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * svelte-demo.js: mounts the Svelte demo into every element with data-framework-demo="svelte".
 */

import { mount } from 'svelte';

import { settingsOf } from './data.js';
import SvelteDemo from './SvelteDemo.svelte';

document.querySelectorAll<HTMLElement>('[data-framework-demo="svelte"]').forEach((element) => {
	mount(SvelteDemo, { target: element, props: settingsOf(element) });
});
