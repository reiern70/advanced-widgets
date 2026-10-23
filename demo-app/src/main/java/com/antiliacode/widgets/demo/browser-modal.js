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
 * The dialogs opened in the browser alone (BrowserModal): a click on an element with
 * data-browser-modal shows the dialog of that markup id and gives it the behaviors of the Modal of
 * ui-components, Wicket.Modal; closing it, with Escape, the × or a click around it, hides it
 * again. The dialog has the class of the theme of the Modal only while it is open, so the closed
 * ones do not shift the open one as dialogs opened after another.
 */
(function () {
	'use strict';

	var THEME_CLASS = 'dialog-theme-default';

	function open(id) {
		var modal = document.getElementById(id);
		if (!modal || !window.Wicket || !Wicket.Modal) {
			return false;
		}
		var overlay = modal.querySelector('.modal-dialog-overlay');
		if (Wicket.Modal.of(overlay.id)) {
			return true;
		}
		modal.classList.add(THEME_CLASS);
		modal.hidden = false;
		Wicket.Modal.attach(overlay.id, {
			width: modal.getAttribute('data-modal-width') || undefined,
			closeOnClick: true,
			onClose: function () {
				Wicket.Modal.detach(overlay.id);
				modal.hidden = true;
				modal.classList.remove(THEME_CLASS);
			}
		});
		return true;
	}

	document.addEventListener('click', function (event) {
		var opener = event.target.closest && event.target.closest('[data-browser-modal]');
		if (opener && open(opener.getAttribute('data-browser-modal'))) {
			event.preventDefault();
		}
	});

	window.AntiliaBrowserModal = { open: open };
})();
