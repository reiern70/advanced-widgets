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

import { startApps, type Mount } from './apps.js';
import { AutocompleteDemo } from './react/AutocompleteDemo.js';
import { DropdownDemo } from './react/DropdownDemo.js';
import { JsonListDemo } from './react/JsonListDemo.js';
import { JsonTableDemo } from './react/JsonTableDemo.js';
import { Overview } from './react/Overview.js';
import { ProgressDemo } from './react/ProgressDemo.js';
import { ScrollbarDemo } from './react/ScrollbarDemo.js';

/**
 * @returns the mount of a React app taking the configuration as its only prop
 */
function app<C>(App: ComponentType<{ config: C }>): Mount {
	return (element, config: C) => createRoot(element).render(
		<StrictMode><App config={config} /></StrictMode>);
}

startApps('react', {
	overview: app(Overview),
	dropdown: app(DropdownDemo),
	autocomplete: app(AutocompleteDemo),
	jsonList: app(JsonListDemo),
	jsonTable: app(JsonTableDemo),
	progressBar: app(ProgressDemo),
	scrollbar: app(ScrollbarDemo)
});
