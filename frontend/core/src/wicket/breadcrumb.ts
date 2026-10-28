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
 * antilia-breadcrumb.js: registers Wicket.Breadcrumb for the Breadcrumb of ui-components, which
 * calls Wicket.Breadcrumb.attach(id, options). The links of the crumbs call the server themselves.
 */

import { Breadcrumb, type BreadcrumbOptions } from '../breadcrumb.js';
import { useWicketHost, wicket } from './host.js';

const WicketBreadcrumb = {

	/**
	 * Makes a breadcrumb of an element.
	 *
	 * @param id the markup id of the breadcrumb
	 * @param options the separator, how many crumbs show and the texts
	 * @returns the breadcrumb, null when there is no such element
	 */
	attach(id: string, options: BreadcrumbOptions = {}): Breadcrumb | null {
		return Breadcrumb.attach(id, options);
	},

	/**
	 * @param id the markup id of the breadcrumb
	 * @returns the breadcrumb, if there is one
	 */
	of(id: string): Breadcrumb | null {
		return Breadcrumb.of(id);
	}
};

const Wicket = wicket();
if (Wicket.Breadcrumb === undefined) {
	useWicketHost();
	Wicket.Breadcrumb = WicketBreadcrumb;
}
