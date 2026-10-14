/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * svelte-demo.js: the Svelte apps of the demos, by the names of the demos, mounted into the
 * elements of the pages marked for Svelte.
 */

import { mount, unmount, type Component } from 'svelte';

import { LOOK_CONTEXT, lookOf } from '@antilia/svelte';

import { startApps, type Mount } from './apps.js';
import AutocompleteDemo from './svelte/AutocompleteDemo.svelte';
import ButtonsDemo from './svelte/ButtonsDemo.svelte';
import DatePickerDemo from './svelte/DatePickerDemo.svelte';
import DesktopDemo from './svelte/DesktopDemo.svelte';
import DropdownDemo from './svelte/DropdownDemo.svelte';
import JsonListDemo from './svelte/JsonListDemo.svelte';
import JsonTableDemo from './svelte/JsonTableDemo.svelte';
import MenusDemo from './svelte/MenusDemo.svelte';
import ModalDemo from './svelte/ModalDemo.svelte';
import NotificationsDemo from './svelte/NotificationsDemo.svelte';
import PaletteDemo from './svelte/PaletteDemo.svelte';
import ProgressDemo from './svelte/ProgressDemo.svelte';
import ScrollbarDemo from './svelte/ScrollbarDemo.svelte';
import SplitPanelDemo from './svelte/SplitPanelDemo.svelte';
import PopoversDemo from './svelte/PopoversDemo.svelte';
import TabsDemo from './svelte/TabsDemo.svelte';
import TreeDemo from './svelte/TreeDemo.svelte';
import TreeGridDemo from './svelte/TreeGridDemo.svelte';

/**
 * @returns the mount of a Svelte app taking the configuration as its only prop, in the look of the
 * page around it
 */
function app<C extends object>(App: Component<{ config: C }>): Mount {
	return (element, config: C) => {
		const app = mount(App, { target: element, props: { config },
			context: new Map([[LOOK_CONTEXT, lookOf(element)]]) });
		return () => {
			void unmount(app);
		};
	};
}

startApps('svelte', {
	dropdown: app(DropdownDemo),
	autocomplete: app(AutocompleteDemo),
	palette: app(PaletteDemo),
	datePicker: app(DatePickerDemo),
	splitPanel: app(SplitPanelDemo),
	tabs: app(TabsDemo),
	menus: app(MenusDemo),
	popovers: app(PopoversDemo),
	desktop: app(DesktopDemo),
	buttons: app(ButtonsDemo),
	jsonList: app(JsonListDemo),
	jsonTable: app(JsonTableDemo),
	tree: app(TreeDemo),
	treeGrid: app(TreeGridDemo),
	progressBar: app(ProgressDemo),
	scrollbar: app(ScrollbarDemo),
	modalDialog: app(ModalDemo),
	notifications: app(NotificationsDemo)
});
