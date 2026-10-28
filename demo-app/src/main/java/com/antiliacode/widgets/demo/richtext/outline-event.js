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
 * A type of events of the demo's own, OutlineEvent: rich-text-demo:outline, which fires on the
 * rich text editors, Tiptap's and CKEditor's, as their text has other headings, telling them. It
 * fires from the editors' own event text, and keeps for each editor the headings it told last.
 */
(function () {
	'use strict';

	/** The texts of the headings of some HTML, in their order; DOMParser runs no script of it. */
	function headings(html) {
		var doc = new DOMParser().parseFromString(html, 'text/html');
		return Array.prototype.map.call(doc.querySelectorAll('h1, h2, h3, h4, h5, h6'),
			function (heading) {
				return heading.textContent.replace(/\s+/g, ' ').trim();
			}).filter(function (text) {
			return text.length > 0;
		});
	}

	Wicket.RichTextEvents.register('rich-text-demo:outline', {
		from: [Wicket.RichTextEvents.events.text],
		init: function () {
			return { told: null };
		},
		fire: function (editor, state) {
			var found = headings(editor.getValue());
			var told = JSON.stringify(found);
			if (told === state.told) {
				return undefined;
			}
			state.told = told;
			return { headings: found };
		}
	});
})();
