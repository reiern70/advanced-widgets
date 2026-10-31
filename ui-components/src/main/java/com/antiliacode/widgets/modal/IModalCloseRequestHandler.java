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
package com.antiliacode.widgets.modal;

import org.apache.wicket.ajax.AjaxRequestTarget;

/**
 * A content of a {@link Modal} that decides itself what happens when the user asks to close the
 * dialog, with Escape, the close button of the header, a click around the dialog or a
 * {@link ModalCloseButton}: a {@link com.antiliacode.widgets.wizard.ModalWizard ModalWizard}
 * cancels, for example. The dialog hands it the request once the function of
 * {@link Modal#setCanClose(String)} in the browser and {@link Modal#canClose(AjaxRequestTarget)}
 * on the server let it; it closes the dialog, or not.
 *
 * @since 10.11.0
 */
@FunctionalInterface
public interface IModalCloseRequestHandler
{
	/**
	 * Called when the user asks to close the dialog this is the content of.
	 *
	 * @param modal
	 *            the dialog
	 * @param target
	 *            the Ajax request
	 */
	void onCloseRequest(Modal modal, AjaxRequestTarget target);
}
