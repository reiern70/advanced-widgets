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
package com.antilia.widgets.modal;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;

import com.antilia.widgets.theme.LookBehavior;
import com.antilia.widgets.theme.LookContainer;
import com.antilia.widgets.theme.LookPart;

/**
 * The content of a {@link Modal} opened with a title, a body and buttons: a header
 * ({@code modal-dialog-header}) holding the title ({@code modal-dialog-title}) and the close
 * button ({@code modal-dialog-close}), the body ({@code modal-dialog-body}), in the wrapper of
 * its slim scroll bars ({@code antilia-scrollbar}), and a footer ({@code modal-dialog-footer}) with
 * the buttons, on the right. Contents of their own follow the same markup, so the dialog drags,
 * folds, scrolls and closes the same.
 *
 * @since 11.0.0
 */
public class ModalContent extends Panel
{
	private static final long serialVersionUID = 1L;

	/** The id of the body. */
	public static final String BODY_ID = "body";

	private final IModel<String> title;

	/**
	 * @param id
	 *            the component id, {@link Modal#CONTENT_ID}
	 * @param title
	 *            the title; none when {@code null}
	 * @param body
	 *            the body, whose id is {@link #BODY_ID}
	 * @param buttons
	 *            the factories of the buttons of the footer; no footer without any
	 */
	public ModalContent(String id, IModel<String> title, Component body,
		List<? extends IModalButtonFactory> buttons)
	{
		super(id);
		Args.notNull(body, "body");
		if (!BODY_ID.equals(body.getId()))
		{
			throw new IllegalArgumentException("The body must have the id Modal.BODY_ID");
		}
		this.title = title;

		WebMarkupContainer header = new WebMarkupContainer("header")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(ModalContent.this.title != null || hasCloseButton());
			}
		};
		add(header.add(new LookBehavior(LookPart.DIALOG_HEADER)));
		header.add(new Label("title", title)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(ModalContent.this.title != null);
			}
		}.setOutputMarkupId(true).add(new LookBehavior(LookPart.DIALOG_TITLE)));
		header.add(new WebMarkupContainer("close")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(hasCloseButton());
			}

			@Override
			protected void onComponentTag(ComponentTag tag)
			{
				super.onComponentTag(tag);
				String label = getString("Modal.close", null, "Close");
				tag.put("aria-label", label);
				tag.put("title", label);
			}
		}.add(new LookBehavior(LookPart.DIALOG_CLOSE)));

		add(new LookContainer("scroller", LookPart.SCROLLBAR));
		add(body.add(new LookBehavior(LookPart.DIALOG_BODY)));

		List<IModalButtonFactory> factories = new ArrayList<>(Args.notNull(buttons, "buttons"));
		WebMarkupContainer footer = new WebMarkupContainer("footer");
		footer.setVisible(!factories.isEmpty());
		footer.add(new LookBehavior(LookPart.DIALOG_FOOTER));
		add(footer);
		RepeatingView repeater = new RepeatingView("buttons");
		footer.add(repeater);
		for (IModalButtonFactory factory : factories)
		{
			repeater.add(factory.createButton(repeater.newChildId()));
		}
	}

	private boolean hasCloseButton()
	{
		Modal modal = findParent(Modal.class);
		return modal == null || modal.isCloseButton();
	}

	/**
	 * @return the model of the title, {@code null} if none
	 */
	public IModel<String> getTitle()
	{
		return title;
	}

	@Override
	protected void onDetach()
	{
		super.onDetach();
		if (title != null)
		{
			title.detach();
		}
	}
}
