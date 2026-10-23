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
 * antilia-rich-text-highlight.js: the grammars of the code blocks of the RichTextEditor of
 * ui-components, which renders it only where an editor highlights. It holds Lezer's parsers of
 * Java, JavaScript, TypeScript, JSON and Markdown and nothing of the editor, and registers them as
 * Wicket.RichTextHighlighter: with antilia-rich-text-editor.js when that came first, else for it to
 * take once it comes.
 */

import { lezerHighlighter } from '../rich-text-highlight.js';
import type { RichTextHighlighter } from '../rich-text-languages.js';
import { wicket } from './host.js';

const Wicket = wicket();
Wicket.RichTextHighlighter = lezerHighlighter;
const editors = Wicket.RichTextEditor as {
	registerHighlighter?: (highlighter: RichTextHighlighter) => void;
} | undefined;
editors?.registerHighlighter?.(lezerHighlighter);
