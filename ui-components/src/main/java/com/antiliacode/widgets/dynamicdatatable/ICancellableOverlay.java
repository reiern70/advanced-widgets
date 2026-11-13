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
package com.antiliacode.widgets.dynamicdatatable;

import org.apache.wicket.ajax.AjaxRequestTarget;

/**
 * The content of a table's overlay that wants to know when the user presses Escape in it, such as
 * the confirmation of a row action, which is then cancelled as by its cancel button.
 */
interface ICancellableOverlay
{
	/**
	 * Closes the overlay, the user having pressed Escape in it, and cancels what it asked for.
	 *
	 * @param target
	 *            the Ajax request target
	 */
	void cancel(AjaxRequestTarget target);
}
