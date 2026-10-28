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
 * svelte-demo.js: the Svelte apps of the demos, by the names of the demos, mounted into the
 * elements of the pages marked for Svelte.
 */

import { mount, unmount, type Component } from 'svelte';

import { LOOK_CONTEXT, lookOf } from '@antilia/svelte';

import { startApps, type Mount } from './apps.js';
import AutocompleteDemo from './svelte/AutocompleteDemo.svelte';
import ButtonsDemo from './svelte/ButtonsDemo.svelte';
import CKEditorDemo from './svelte/CKEditorDemo.svelte';
import CodeEditorDemo from './svelte/CodeEditorDemo.svelte';
import DatePickerDemo from './svelte/DatePickerDemo.svelte';
import DesktopDemo from './svelte/DesktopDemo.svelte';
import DropdownDemo from './svelte/DropdownDemo.svelte';
import JsonListDemo from './svelte/JsonListDemo.svelte';
import JsonTableDemo from './svelte/JsonTableDemo.svelte';
import BreadcrumbsDemo from './svelte/BreadcrumbsDemo.svelte';
import MenusDemo from './svelte/MenusDemo.svelte';
import ModalDemo from './svelte/ModalDemo.svelte';
import NotificationsDemo from './svelte/NotificationsDemo.svelte';
import PaletteDemo from './svelte/PaletteDemo.svelte';
import ProgressDemo from './svelte/ProgressDemo.svelte';
import ResizableDemo from './svelte/ResizableDemo.svelte';
import RichTextEditorDemo from './svelte/RichTextEditorDemo.svelte';
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
	resizable: app(ResizableDemo),
	codeEditor: app(CodeEditorDemo),
	richTextEditor: app(RichTextEditorDemo),
	ckEditor: app(CKEditorDemo),
	tabs: app(TabsDemo),
	menus: app(MenusDemo),
	breadcrumbs: app(BreadcrumbsDemo),
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
