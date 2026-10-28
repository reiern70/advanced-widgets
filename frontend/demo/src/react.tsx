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
 * react-demo.js: the React apps of the demos, by the names of the demos, rendered into the
 * elements of the pages marked for React.
 */

import { StrictMode, type ComponentType } from 'react';
import { createRoot } from 'react-dom/client';

import { LookProvider, lookOf } from '@antilia/react';

import { startApps, type Mount } from './apps.js';
import { AutocompleteDemo } from './react/AutocompleteDemo.js';
import { ButtonsDemo } from './react/ButtonsDemo.js';
import { CodeEditorDemo } from './react/CodeEditorDemo.js';
import { DatePickerDemo } from './react/DatePickerDemo.js';
import { DesktopDemo } from './react/DesktopDemo.js';
import { DropdownDemo } from './react/DropdownDemo.js';
import { JsonListDemo } from './react/JsonListDemo.js';
import { JsonTableDemo } from './react/JsonTableDemo.js';
import { BreadcrumbsDemo } from './react/BreadcrumbsDemo.js';
import { MenusDemo } from './react/MenusDemo.js';
import { ModalDemo } from './react/ModalDemo.js';
import { NotificationsDemo } from './react/NotificationsDemo.js';
import { PaletteDemo } from './react/PaletteDemo.js';
import { ProgressDemo } from './react/ProgressDemo.js';
import { ResizableDemo } from './react/ResizableDemo.js';
import { RichTextEditorDemo } from './react/RichTextEditorDemo.js';
import { ScrollbarDemo } from './react/ScrollbarDemo.js';
import { SplitPanelDemo } from './react/SplitPanelDemo.js';
import { PopoversDemo } from './react/PopoversDemo.js';
import { TabsDemo } from './react/TabsDemo.js';
import { TreeDemo, TreeGridDemo } from './react/TreeDemo.js';

/**
 * @returns the mount of a React app taking the configuration as its only prop, in the look of the
 * page around it
 */
function app<C>(App: ComponentType<{ config: C }>): Mount {
	return (element, config: C) => {
		const root = createRoot(element);
		root.render(<StrictMode><LookProvider look={lookOf(element)}><App config={config} />
			</LookProvider></StrictMode>);
		return () => root.unmount();
	};
}

startApps('react', {
	dropdown: app(DropdownDemo),
	autocomplete: app(AutocompleteDemo),
	palette: app(PaletteDemo),
	datePicker: app(DatePickerDemo),
	splitPanel: app(SplitPanelDemo),
	resizable: app(ResizableDemo),
	codeEditor: app(CodeEditorDemo),
	richTextEditor: app(RichTextEditorDemo),
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
