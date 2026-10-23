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
package com.antiliacode.widgets.demo.resizable;

import java.util.List;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.CssResourceReference;

import com.antiliacode.widgets.button.ButtonBehavior;
import com.antiliacode.widgets.demo.DemoPage;
import com.antiliacode.widgets.demo.frameworks.FrameworkTabs;
import com.antiliacode.widgets.resizable.ResizableBehavior;
import com.antiliacode.widgets.resizable.ResizableSize;
import com.antiliacode.widgets.resizable.ResizeAxis;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * {@link ResizableBehavior}: a note resizing its height, a side panel its width, and a card both.
 * The server hears each size the user gives, and says so below them: the note's and the side
 * panel's are kept in the session, models of their own, so that the page shows them again when
 * opened anew; the card's in its behavior, and so in the page, as by default. A button takes the
 * card out of the page and puts it back, and the card comes back in the size it had. The React and
 * Svelte tabs show the same in those frameworks.
 */
public class ResizablePage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	/** The session attributes keeping the sizes of the note and of the side panel. */
	static final String NOTE_ATTRIBUTE = "resizable-demo.note";

	static final String SIDE_ATTRIBUTE = "resizable-demo.side";

	/** The size the card starts with. */
	static final ResizableSize CARD_SIZE = ResizableSize.of(320, 180);

	/** The items of the side panel. */
	static final List<String> ITEMS = List.of("inbox", "drafts", "sent", "archive");

	private final Label sizes;

	private final WebMarkupContainer card;

	private final WebMarkupContainer cardPlace;

	private final ResizableBehavior note;

	private final ResizableBehavior side;

	private final ResizableBehavior cardBehavior;

	private boolean cardShown = true;

	/**
	 * Constructor.
	 */
	public ResizablePage()
	{
		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);

		sizes = new Label("sizes", this::describeSizes);
		sizes.setOutputMarkupId(true);
		themed.add(sizes);

		note = resizable(ResizeAxis.VERTICAL, new SessionModel(NOTE_ATTRIBUTE)).setMinHeight(80)
			.setMaxHeight(480);
		themed.add(new WebMarkupContainer("note").add(note));

		side = resizable(ResizeAxis.HORIZONTAL, new SessionModel(SIDE_ATTRIBUTE)).setMinWidth(140)
			.setMaxWidth(520);
		WebMarkupContainer sidePanel = new WebMarkupContainer("side");
		sidePanel.add(side);
		themed.add(sidePanel);
		ITEMS.forEach(item -> sidePanel.add(new Label(item, getString(item))));

		cardBehavior = resizable(ResizeAxis.BOTH, Model.of(CARD_SIZE)).setMinWidth(180)
			.setMinHeight(100);
		card = new WebMarkupContainer("card");
		card.add(cardBehavior);
		cardPlace = new WebMarkupContainer("cardPlace");
		cardPlace.setOutputMarkupId(true);
		cardPlace.add(card);
		themed.add(cardPlace);

		themed.add(new AjaxLink<Void>("toggleCard")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				// out of the page, and back in: the card keeps the size its behavior holds
				cardShown = !cardShown;
				if (cardShown)
				{
					cardPlace.replace(card);
				}
				else
				{
					cardPlace.replace(new WebMarkupContainer("card").setVisible(false));
				}
				target.add(cardPlace, sizes, this);
			}
		}.setBody(() -> getString(cardShown ? "takeCardAway" : "putCardBack"))
			.add(new ButtonBehavior(ButtonBehavior.Variant.SECONDARY, ButtonBehavior.Size.SMALL))
			.setOutputMarkupId(true));
	}

	/**
	 * @return a behavior telling the server the sizes the user gives, which it then shows
	 */
	private ResizableBehavior resizable(ResizeAxis axis, IModel<ResizableSize> model)
	{
		return new ResizableBehavior(axis, model)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onResized(AjaxRequestTarget target, ResizableSize size)
			{
				target.add(sizes);
			}
		}.setSendToServer(true);
	}

	/**
	 * @return the sizes the server knows, in words
	 */
	String describeSizes()
	{
		return getString("sizes")
			.replace("{0}", height(note.getSize()))
			.replace("{1}", width(side.getSize()))
			.replace("{2}", cardShown ? width(cardBehavior.getSize()) + " × " +
				height(cardBehavior.getSize()) : getString("away"));
	}

	private String height(ResizableSize size)
	{
		return size.getHeight() == null ? getString("pageSize") : size.getHeight() + " px";
	}

	private String width(ResizableSize size)
	{
		return size.getWidth() == null ? getString("pageSize") : size.getWidth() + " px";
	}

	/**
	 * @return whether the card is in the page
	 */
	public boolean isCardShown()
	{
		return cardShown;
	}

	@Override
	protected String getFrameworkDemo()
	{
		return "resizable";
	}

	/**
	 * @return the texts, the items of the side panel and the size the card starts with
	 */
	@Override
	protected JSONObject getFrameworkConfig()
	{
		JSONObject texts = FrameworkTabs.texts(this, "noteTitle", "noteText", "sideTitle",
			"cardTitle", "cardText", "takeCardAway", "putCardBack", "pageSize", "away");
		// the app, not the server, keeps the sizes
		texts.put("sizes", getString("appSizes"));
		ITEMS.forEach(item -> texts.put(item, getString(item)));
		return new JSONObject()
			.put("texts", texts)
			.put("handle", getString("Resizable.handle"))
			.put("items", new JSONArray(ITEMS))
			.put("card", CARD_SIZE.putInto(new JSONObject()));
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		ResizableBehavior.renderHeadItems(response);
		response.render(CssHeaderItem.forReference(
			new CssResourceReference(ResizablePage.class, "ResizablePage.css")));
	}

	/**
	 * A size kept in the session.
	 */
	static class SessionModel implements IModel<ResizableSize>
	{
		private static final long serialVersionUID = 1L;

		private final String attribute;

		SessionModel(String attribute)
		{
			this.attribute = attribute;
		}

		@Override
		public ResizableSize getObject()
		{
			return (ResizableSize)Session.get().getAttribute(attribute);
		}

		@Override
		public void setObject(ResizableSize size)
		{
			Session.get().setAttribute(attribute, size);
		}
	}
}
