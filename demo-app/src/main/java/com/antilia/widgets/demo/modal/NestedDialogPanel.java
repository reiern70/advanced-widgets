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
package com.antilia.widgets.demo.modal;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

import com.antilia.widgets.icon.SvgIcon;
import com.antilia.widgets.modal.IModalButtonFactory;
import com.antilia.widgets.modal.Modal;

/**
 * The body of a dialog of the options example. It holds a {@link Modal} of its own, so the dialog
 * it opens is nested in this one, and opens over it with the same options, up to
 * {@link DialogOptions#MAX_LEVEL} dialogs deep.
 */
public class NestedDialogPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	private final DialogOptions options;

	private final int level;

	private final Modal nested;

	/**
	 * @param id
	 *            the component id, {@link Modal#BODY_ID}
	 * @param options
	 *            the options of the dialogs
	 * @param level
	 *            how deep the dialog of this body is, from 1
	 */
	public NestedDialogPanel(String id, DialogOptions options, int level)
	{
		super(id);
		this.options = options;
		this.level = level;
		add(new Label("text", new ResourceModel(level < DialogOptions.MAX_LEVEL ?
			"dialog.text" : "dialog.deepest")));
		nested = new Modal("nested");
		add(nested);
	}

	/**
	 * Opens a dialog with the options, its body a {@link NestedDialogPanel}: Close, a button
	 * opening a nested dialog while it may, and OK in its footer, with icons.
	 *
	 * @param modal
	 *            the dialog
	 * @param options
	 *            its options
	 * @param level
	 *            how deep it is, from 1
	 * @param target
	 *            the Ajax target
	 */
	public static void open(Modal modal, DialogOptions options, int level,
		AjaxRequestTarget target)
	{
		NestedDialogPanel body = new NestedDialogPanel(Modal.BODY_ID, options, level);
		List<IModalButtonFactory> buttons = new ArrayList<>();
		buttons.add(IModalButtonFactory.close(new ResourceModel("close"), SvgIcon.XMARK));
		if (level < DialogOptions.MAX_LEVEL)
		{
			buttons.add(IModalButtonFactory.button(new ResourceModel("dialog.nested"),
				SvgIcon.LAYER_GROUP, body::openNested));
		}
		buttons.add(IModalButtonFactory.primary(new ResourceModel("dialog.ok"), SvgIcon.CHECK,
			modal::close));
		options.applyTo(modal).open(new StringResourceModel("dialog.title", modal)
			.setParameters(level), body, buttons, target);
	}

	/**
	 * Opens a dialog nested in the one of this body.
	 *
	 * @param target
	 *            the Ajax target
	 */
	void openNested(AjaxRequestTarget target)
	{
		open(nested, options, level + 1, target);
	}
}
