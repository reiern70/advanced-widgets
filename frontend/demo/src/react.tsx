/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
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
import { DatePickerDemo } from './react/DatePickerDemo.js';
import { DesktopDemo } from './react/DesktopDemo.js';
import { DropdownDemo } from './react/DropdownDemo.js';
import { JsonListDemo } from './react/JsonListDemo.js';
import { JsonTableDemo } from './react/JsonTableDemo.js';
import { ModalDemo } from './react/ModalDemo.js';
import { NotificationsDemo } from './react/NotificationsDemo.js';
import { PaletteDemo } from './react/PaletteDemo.js';
import { ProgressDemo } from './react/ProgressDemo.js';
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
	tabs: app(TabsDemo),
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
