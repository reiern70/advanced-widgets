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
 * The log of the hooks of the field uploading to the resource, in the upload demo: the hooks of
 * Wicket's FileUploadToResourceField call UploadDemo.hook() as they are called, in the browser,
 * and the server's response tells of those called on the server the same way.
 */
;(function () {
	'use strict';

	window.UploadDemo = {

		/**
		 * Logs that a hook was called: when, where, the hook and what happened.
		 *
		 * @param log the id of the list
		 * @param side where it was called, 'browser' or 'server'
		 * @param where the words of the side
		 * @param hook the hook
		 * @param text what happened
		 */
		hook: function (log, side, where, hook, text) {
			const list = document.getElementById(log);
			if (!list) {
				return;
			}
			const item = document.createElement('li');
			item.className = 'upload-demo__hook upload-demo__hook--' + side;
			const time = document.createElement('span');
			time.className = 'upload-demo__hook-time';
			time.textContent = new Date().toLocaleTimeString(document.documentElement.lang ||
				undefined);
			const place = document.createElement('span');
			place.className = 'upload-demo__hook-side';
			place.textContent = where;
			const name = document.createElement('code');
			name.textContent = hook;
			const what = document.createElement('span');
			what.textContent = text;
			item.append(time, place, name, what);
			list.append(item);
		},

		/**
		 * Empties a log.
		 *
		 * @param log the id of the list
		 */
		clear: function (log) {
			document.getElementById(log)?.replaceChildren();
		}
	};
})();
