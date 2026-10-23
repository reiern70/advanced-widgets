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
package com.antiliacode.widgets.demo;

import org.apache.wicket.feedback.ErrorLevelFeedbackMessageFilter;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.panel.FeedbackPanel;

/**
 * The errors of a form, listed in the form in the order of its fields. The notifications of the
 * page leave them to it, so none is lost when a form has more errors than a stack of
 * notifications shows; the other messages, such as a success, are still notifications. Shown only
 * when there is an error.
 */
public class FormErrors extends FeedbackPanel
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param id
	 *            component id
	 */
	public FormErrors(String id)
	{
		super(id, new ErrorLevelFeedbackMessageFilter(FeedbackMessage.ERROR));
	}

	@Override
	protected void onConfigure()
	{
		super.onConfigure();
		setVisible(anyMessage());
	}
}
