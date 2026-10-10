/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * svelte-demo.js: the Svelte apps of the demos, by the names of the demos, mounted into the
 * elements of the pages marked for Svelte.
 */

import { mount, type Component } from 'svelte';

import { startApps, type Mount } from './apps.js';
import AutocompleteDemo from './svelte/AutocompleteDemo.svelte';
import ButtonsDemo from './svelte/ButtonsDemo.svelte';
import DropdownDemo from './svelte/DropdownDemo.svelte';
import JsonListDemo from './svelte/JsonListDemo.svelte';
import JsonTableDemo from './svelte/JsonTableDemo.svelte';
import ModalDemo from './svelte/ModalDemo.svelte';
import PaletteDemo from './svelte/PaletteDemo.svelte';
import ProgressDemo from './svelte/ProgressDemo.svelte';
import ScrollbarDemo from './svelte/ScrollbarDemo.svelte';
import TreeDemo from './svelte/TreeDemo.svelte';
import TreeGridDemo from './svelte/TreeGridDemo.svelte';

/**
 * @returns the mount of a Svelte app taking the configuration as its only prop
 */
function app<C extends object>(App: Component<{ config: C }>): Mount {
	return (element, config: C) => {
		mount(App, { target: element, props: { config } });
	};
}

startApps('svelte', {
	dropdown: app(DropdownDemo),
	autocomplete: app(AutocompleteDemo),
	palette: app(PaletteDemo),
	buttons: app(ButtonsDemo),
	jsonList: app(JsonListDemo),
	jsonTable: app(JsonTableDemo),
	tree: app(TreeDemo),
	treeGrid: app(TreeGridDemo),
	progressBar: app(ProgressDemo),
	scrollbar: app(ScrollbarDemo),
	modalDialog: app(ModalDemo)
});
