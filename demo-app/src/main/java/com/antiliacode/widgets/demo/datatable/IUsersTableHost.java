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
package com.antiliacode.widgets.demo.datatable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.util.io.IClusterable;

import com.antiliacode.widgets.modal.Modal;

/**
 * What a {@link UsersTablePanel} needs of the page it is on: the selected user, which the page may
 * show, the icons of the actions, and the dialog the edit form shows in, if any.
 */
public interface IUsersTableHost extends IClusterable
{
	/**
	 * @return the selected user, {@code null} if there is none
	 */
	User getSelected();

	/**
	 * Selects a user, or none, showing it where the page shows it.
	 *
	 * @param user
	 *            the user, {@code null} for none
	 * @param target
	 *            the Ajax request target, which the page adds what shows the user to
	 */
	void select(User user, AjaxRequestTarget target);

	/**
	 * @return the icons of the actions, the built-in ones by default
	 */
	default ActionIcons getIcons()
	{
		return ActionIcons.BUILT_IN;
	}

	/**
	 * @return the dialog the edit form and the confirmation of a deletion show in; {@code null},
	 *         as by default, to show them in the table's overlay
	 */
	default Modal getDialog()
	{
		return null;
	}
}
