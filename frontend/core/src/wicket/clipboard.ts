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
 * antilia-clipboard.js: registers Wicket.Clipboard for ClipboardCopyBehavior, which sets its
 * copiedMessage, and for the theme editor, which calls Wicket.Clipboard.copy(text, element).
 */

import { Clipboard } from '../clipboard.js';
import { useWicketHost, wicket } from './host.js';

const Wicket = wicket();
if (Wicket.Clipboard === undefined) {
	useWicketHost();
	Wicket.Clipboard = Clipboard;
	Clipboard.install();
}
