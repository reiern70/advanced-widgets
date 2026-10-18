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
package com.antilia.widgets.popover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.theme.Look;
import com.antilia.widgets.theme.LookAspect;
import com.antilia.widgets.theme.LookPart;
import com.github.openjson.JSONObject;

class PopoverTest extends WicketTestCase
{
	@Test
	void showsAPopoverPointingAtItsTriggerViaAjax()
	{
		tester.startPage(new TestPage(new Popover("3 lines, saved").setTitle("notes.txt")));
		tester.assertContains("antilia-popover.*\\.js");
		tester.assertContains("antilia-popover.*\\.css");

		tester.clickLink("link");

		String response = tester.getLastResponseAsString();
		String id = tester.getComponentFromLastRenderedPage("link").getMarkupId();
		assertTrue(response.contains("Wicket.Popover.show(\"" + id + "\", {"), response);
		JSONObject options = new JSONObject(response.substring(response.indexOf(", {") + 2,
			response.indexOf("});") + 1));
		assertEquals("3 lines, saved", options.getString("body"));
		assertEquals("notes.txt", options.getString("title"));
		assertEquals(false, options.getBoolean("html"));
		assertEquals("bottom-center", options.getString("placement"));
		assertEquals("default", options.getString("variant"));
		assertEquals("card", options.getString("design"));
		assertTrue(!options.has("content"));
		assertEquals(true, options.getBoolean("closable"));
		assertEquals("Close", options.getString("closeLabel"));
	}

	@Test
	void takesItsPlacementVariantAndMarkup()
	{
		Popover popover = new Popover("<b>bold</b>").setEscapeBody(false)
			.setPlacement(PopoverPlacement.AUTO).setVariant(PopoverVariant.WARNING)
			.setDesign(PopoverDesign.GLASS)
			.setClosable(false).setMaxWidth("30rem");
		tester.getSession().setLocale(Locale.of("es"));
		tester.startPage(new TestPage(popover));
		JSONObject options = new JSONObject(popover.toJson(tester.getLastRenderedPage()));

		assertEquals(true, options.getBoolean("html"));
		assertEquals("auto", options.getString("placement"));
		assertEquals("warning", options.getString("variant"));
		assertEquals("glass", options.getString("design"));
		assertEquals(false, options.getBoolean("closable"));
		assertEquals("30rem", options.getString("maxWidth"));
		assertEquals("Cerrar", options.getString("closeLabel"));
		assertTrue(!options.has("title"));

		popover.setPlacement(null).setVariant(null).setDesign(null);
		assertEquals(PopoverPlacement.BOTTOM_CENTER, popover.getPlacement());
		assertEquals("top-center", PopoverPlacement.TOP_CENTER.cssName());
		assertEquals(PopoverVariant.DEFAULT, popover.getVariant());
		assertEquals(PopoverDesign.CARD, popover.getDesign());
		assertEquals("top-left", PopoverPlacement.TOP_LEFT.cssName());
		assertEquals("left-bottom", PopoverPlacement.LEFT_BOTTOM.cssName());
		assertEquals("callout", PopoverDesign.CALLOUT.cssName());
	}

	@Test
	void drawsItsBubbleInTheClassesOfALook()
	{
		assertEquals("antilia-popover antilia-popover-default antilia-popover-card " +
			"antilia-look-antilia", Popover.getCssClasses(Look.ANTILIA, PopoverDesign.CARD,
				PopoverVariant.DEFAULT));
		assertEquals("antilia-popover antilia-popover-error antilia-popover-neon " +
			"antilia-look-bootstrap antilia-look-antilia", Popover.getCssClasses(
				Look.BOOTSTRAP.with(LookAspect.COLOR, Look.ANTILIA), PopoverDesign.NEON,
				PopoverVariant.ERROR));
		assertEquals(LookPart.POPOVER_GLASS, PopoverDesign.GLASS.getPart());
		assertEquals(LookPart.POPOVER_INFO, PopoverVariant.INFO.getPart());
	}

	@Test
	void showsAComponentOfThePageRenderedHidden()
	{
		ContentPage page = new ContentPage();
		tester.startPage(page);
		String id = page.content.getMarkupId();
		tester.assertContains("class=\"rich antilia-popover-content\" id=\"" + id + "\"");
		assertEquals(1, page.content.getBehaviors().size());

		tester.clickLink("link");

		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("Wicket.Popover.show(\"" +
			tester.getComponentFromLastRenderedPage("link").getMarkupId() + "\", {"), response);
		// the content rendered again first, then shown
		assertTrue(response.indexOf("id=\"" + id + "\"") < response.indexOf("Wicket.Popover.show"),
			response);
		JSONObject options = new JSONObject(page.popover.toJson(page));
		assertEquals(id, options.getString("content"));
		assertEquals("Before", options.getString("body"));
		assertEquals(page.content, page.popover.getContent());
		// a second popover of the same content adds nothing to it
		new Popover(page.content);
		assertEquals(1, page.content.getBehaviors().size());

		tester.clickLink("close");
		assertTrue(tester.getLastResponseAsString().contains("Wicket.Popover.closeAll();"));
	}

	public static class ContentPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		final Label content = new Label("content", "rich");

		final Popover popover;

		ContentPage()
		{
			add(content);
			popover = new Popover("Before", content);
			add(new AjaxLink<Void>("link")
			{
				private static final long serialVersionUID = 1L;

				@Override
				public void onClick(AjaxRequestTarget target)
				{
					target.add(content);
					popover.show(target, this);
				}
			}.setOutputMarkupId(true));
			add(new AjaxLink<Void>("close")
			{
				private static final long serialVersionUID = 1L;

				@Override
				public void onClick(AjaxRequestTarget target)
				{
					Popover.close(target);
				}
			});
		}

		@Override
		public void renderHead(IHeaderResponse response)
		{
			super.renderHead(response);
			Popover.renderHeadItems(response);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body>" +
				"<a wicket:id=\"link\">i</a><a wicket:id=\"close\">x</a>" +
				"<div wicket:id=\"content\" class=\"rich\"></div></body></html>");
		}
	}

	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		TestPage(Popover popover)
		{
			add(new AjaxLink<Void>("link")
			{
				private static final long serialVersionUID = 1L;

				@Override
				public void onClick(AjaxRequestTarget target)
				{
					popover.show(target, this);
				}
			}.setOutputMarkupId(true));
		}

		@Override
		public void renderHead(IHeaderResponse response)
		{
			super.renderHead(response);
			Popover.renderHeadItems(response);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body>" +
				"<a wicket:id=\"link\">i</a></body></html>");
		}
	}
}
