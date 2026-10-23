/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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

import com.antiliacode.widgets.demo.DemoPage;

/**
 * A demo page working on the users: keeps the selected user.
 */
public abstract class UsersPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	private User selected;

	/**
	 * @return the selected user, {@code null} if there is none
	 */
	public User getSelected()
	{
		return selected;
	}

	/**
	 * @param selected
	 *            the selected user, {@code null} for none
	 */
	public void setSelected(User selected)
	{
		this.selected = selected;
	}
}
