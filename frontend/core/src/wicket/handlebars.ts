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
 * handlebars-adapter.js: registers the page's Handlebars as the engine "handlebars" of the page's
 * Wicket.Templates, for HandlebarsTemplateEngine.
 */

import { registerHandlebars } from '../handlebars.js';
import { Templates, type TemplateRegistry } from '../templates.js';
import { useWicketHost, wicket } from './host.js';

useWicketHost();
registerHandlebars(undefined, (wicket().Templates as TemplateRegistry | undefined) ?? Templates);
