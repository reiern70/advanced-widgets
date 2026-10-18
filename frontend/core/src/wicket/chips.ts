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
 * antilia-chips.js: registers Wicket.Chips, the chips of the chosen values of the drop-downs and
 * the autocompletes, for the scripts still outside the core.
 */

import { Chips } from '../chips.js';
import { wicket } from './host.js';

const Wicket = wicket();
if (Wicket.Chips === undefined) {
	Wicket.Chips = Chips;
}
