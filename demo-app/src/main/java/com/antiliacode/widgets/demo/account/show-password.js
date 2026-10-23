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
 * The button with an eye at the end of a password field (ShowPasswordBehavior): a click shows
 * what is typed, another hides it again. The field is wrapped in a span.show-password, which
 * places the button; its labels are the field's data-show-password and data-hide-password.
 * Submitting the form hides the password again, so that the browser still offers to save it.
 */
(function () {
	'use strict';

	// an eye, in the color of the text around it, in the accent color while the password is shown
	var ICON = '<svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true" focusable="false">' +
		'<path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z"/>' +
		'<circle cx="12" cy="12" r="3"/></svg>';

	function update(input, button) {
		var shown = input.type === 'text';
		var label = input.getAttribute(shown ? 'data-hide-password' : 'data-show-password');
		button.setAttribute('aria-label', label);
		button.title = label;
		button.classList.toggle('show-password__button--shown', shown);
	}

	function attach(id) {
		var input = document.getElementById(id);
		if (!input || input.hasAttribute('data-show-password-ready')) {
			return;
		}
		input.setAttribute('data-show-password-ready', '');
		// what is shown is a password: no capitals, corrections or spelling marks added to it
		input.setAttribute('autocapitalize', 'off');
		input.setAttribute('autocorrect', 'off');
		input.spellcheck = false;

		var wrapper = input.parentNode;
		if (!wrapper.classList.contains('show-password')) {
			wrapper = document.createElement('span');
			wrapper.className = 'show-password';
			input.parentNode.insertBefore(wrapper, input);
			wrapper.appendChild(input);
		}
		var old = wrapper.querySelector('.show-password__button');
		if (old) {
			old.remove();
		}
		var button = document.createElement('button');
		button.type = 'button';
		button.className = 'show-password__button';
		button.setAttribute('aria-controls', id);
		button.innerHTML = ICON;
		wrapper.appendChild(button);
		update(input, button);

		button.addEventListener('click', function () {
			input.type = input.type === 'password' ? 'text' : 'password';
			update(input, button);
		});
		if (input.form) {
			input.form.addEventListener('submit', function () {
				input.type = 'password';
				update(input, button);
			});
		}
	}

	window.AntiliaShowPassword = { attach: attach };
})();
