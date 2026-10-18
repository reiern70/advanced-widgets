/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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
 * antilia-button.js: registers Wicket.Button for ButtonDownloadBehavior, which marks its button
 * busy while the download it started runs, Wicket.Button.busy(id, true), and done once the
 * browser told the server how it ended, Wicket.Button.busy(id, false). A busy button ignores
 * clicks.
 */

import { guardBusyButtons, setButtonBusy } from '../button.js';
import { useWicketHost, wicket } from './host.js';

const Wicket = wicket();
if (Wicket.Button === undefined) {
	useWicketHost();
	Wicket.Button = {
		busy(id: string, busy: boolean): void {
			setButtonBusy(document.getElementById(id), busy);
		}
	};
	guardBusyButtons();
}
