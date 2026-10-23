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
 * antilia-scrollbar.js: registers Wicket.Scrollbar for ScrollbarBehavior, which calls
 * Wicket.Scrollbar.attach(id, {axis}), and for the dynamic data tables' scrolling bodies.
 */

import { Scrollbar } from '../scrollbar.js';
import { useWicketHost, wicket } from './host.js';

const Wicket = wicket();
if (Wicket.Scrollbar === undefined) {
	useWicketHost();
	Wicket.Scrollbar = Scrollbar;
}
