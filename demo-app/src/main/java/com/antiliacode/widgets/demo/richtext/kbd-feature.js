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
 * A feature of the demo's own, KbdFeature: keys of the keyboard, <kbd>, made with the classes of
 * the editor's kit, and the tool of the toolbar turning them on and off.
 */
(function () {
	'use strict';

	Wicket.RichTextEditor.registerFeature('kbd', {
		extensions: function (context) {
			var kit = context.kit;
			return kit.Mark.create({
				name: 'kbd',
				parseHTML: function () {
					return [{ tag: 'kbd' }];
				},
				renderHTML: function (props) {
					return ['kbd', kit.mergeAttributes(props.HTMLAttributes), 0];
				},
				addKeyboardShortcuts: function () {
					var editor = this.editor;
					return {
						'Mod-Shift-k': function () {
							return editor.commands.toggleMark('kbd');
						}
					};
				}
			});
		},
		tools: {
			kbd: {
				label: 'Keyboard key',
				kind: 'toggle',
				shortcut: 'Mod-Shift-K',
				icon: '<svg viewBox="0 0 24 24" width="1em" height="1em" fill="none" ' +
					'stroke="currentColor" stroke-width="2" stroke-linecap="round" ' +
					'stroke-linejoin="round" aria-hidden="true" focusable="false">' +
					'<rect x="2" y="6" width="20" height="12" rx="2"/>' +
					'<path d="M6 10h.01M10 10h.01M14 10h.01M18 10h.01M7 14h10"/></svg>',
				active: function (editor) {
					return editor.isActive('kbd');
				},
				enabled: function (editor) {
					return editor.can().toggleMark('kbd');
				},
				run: function (editor) {
					editor.chain().focus().toggleMark('kbd').run();
				}
			}
		}
	});
})();
