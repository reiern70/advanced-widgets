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
package com.antilia.widgets.demo;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

import com.antilia.widgets.modal.Modal;

/**
 * A dialog in the markup, look and behaviors of the {@link Modal} of ui-components, opened and
 * closed in the browser alone: the page renders it hidden, and a click on any element with
 * {@code data-browser-modal} naming its markup id opens it, without a request to the server, so
 * a stateless page stays stateless. The script of the {@link Modal} runs it: Escape, the × and a
 * click around it close it, its title drags it and a double click folds it, and the focus stays in
 * it while it is open.
 * <p>
 * Its body is {@link #newBody(String)}, its footer {@link #newFooter(String)}, none by default.
 */
public abstract class BrowserModal extends Panel
{
	private static final long serialVersionUID = 1L;

	/** The attribute of an element that opens the dialog with the markup id it holds. */
	public static final String OPENER_ATTRIBUTE = "data-browser-modal";

	private static final ResourceReference SCRIPT = new JavaScriptResourceReference(
		BrowserModal.class, "browser-modal.js");

	private final String width;

	/**
	 * @param id
	 *            the id of the component
	 * @param title
	 *            the title, in the header
	 * @param width
	 *            the width of the dialog, a CSS length such as {@code 52em}, never wider than the
	 *            window
	 */
	public BrowserModal(String id, IModel<String> title, String width)
	{
		super(id, title);
		this.width = width;
		setOutputMarkupId(true);
		WebMarkupContainer overlay = new WebMarkupContainer("overlay");
		overlay.setOutputMarkupId(true);
		add(overlay);
		Label titleLabel = new Label("title", title);
		titleLabel.setOutputMarkupId(true);
		overlay.add(titleLabel);
		overlay.add(new WebMarkupContainer("close")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onComponentTag(ComponentTag tag)
			{
				super.onComponentTag(tag);
				String label = getString("Modal.close", null, "Close");
				tag.put("aria-label", label);
				tag.put("title", label);
			}
		});
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		WebMarkupContainer overlay = (WebMarkupContainer)get("overlay");
		overlay.add(newBody("body"));
		Component footer = newFooter("footer");
		overlay.add(footer != null ? footer
			: new WebMarkupContainer("footer").setVisible(false));
	}

	/**
	 * @param id
	 *            the id of the body
	 * @return the body of the dialog
	 */
	protected abstract Component newBody(String id);

	/**
	 * @param id
	 *            the id of the footer
	 * @return the footer of the dialog, its buttons on the right; {@code null}, the default, for
	 *         none
	 */
	protected Component newFooter(String id)
	{
		return null;
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		super.onComponentTag(tag);
		tag.put("hidden", "hidden");
		tag.put("data-modal-width", width);
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		Modal.renderHeadItems(response);
		response.render(JavaScriptHeaderItem.forReference(SCRIPT).setDefer(true));
	}
}
