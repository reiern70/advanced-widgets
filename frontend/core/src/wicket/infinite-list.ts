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
 * json-infinite-list.js: registers Wicket.JsonInfiniteList for JsonBasedInfiniteList, which calls
 * new Wicket.JsonInfiniteList(cfg, action, messages, data) and finds a list in its instances. The
 * items are rendered with the engines of the page's Wicket.Templates; a list of a fixed height gets
 * the scroll bars of Wicket.Scrollbar when the page loaded them, as before.
 */

import { JsonInfiniteList } from '../infinite-list.js';
import { useTemplates, type TemplateRegistry } from '../templates.js';
import { useWicketHost, wicket } from './host.js';

interface Bars {
	attach(element: HTMLElement, options: { axis: 'y' }): { update(): void; destroy(): void };
}

const Wicket = wicket();
if (Wicket.JsonInfiniteList === undefined) {
	useWicketHost();
	useTemplates(Wicket.Templates as TemplateRegistry | undefined);
	JsonInfiniteList.attachScrollbar = (element) =>
		(window.Wicket?.Scrollbar as Bars | undefined)?.attach(element, { axis: 'y' });
	Wicket.JsonInfiniteList = JsonInfiniteList;
}
