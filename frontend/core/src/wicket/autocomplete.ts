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
 * antilia-autocomplete.js: registers Wicket.Autocomplete for Autocomplete and MultiAutocomplete,
 * which call Wicket.Autocomplete.attach(id, options). The rows are rendered with the engines of the
 * page's Wicket.Templates, which antilia-templates.js registered before. The list of suggestions
 * gets the scroll bars of Wicket.Scrollbar, when the page loaded them.
 */

import { Autocomplete } from '../autocomplete.js';
import type { ScrollbarHandle, ScrollbarOptions } from '../scrollbar.js';
import { useTemplates, type TemplateRegistry } from '../templates.js';
import { useWicketHost, wicket } from './host.js';

interface ScrollbarGlobal {
	attach(element: HTMLElement, options: ScrollbarOptions): ScrollbarHandle | undefined;
}

const Wicket = wicket();
if (Wicket.Autocomplete === undefined) {
	useWicketHost();
	useTemplates(Wicket.Templates as TemplateRegistry | undefined);
	Autocomplete.attachScrollbar = (list) =>
		(window.Wicket?.Scrollbar as ScrollbarGlobal | undefined)?.attach(list, { axis: 'y' });
	Wicket.Autocomplete = Autocomplete;
}
