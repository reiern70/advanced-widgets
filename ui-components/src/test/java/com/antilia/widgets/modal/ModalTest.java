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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

class ModalTest extends WicketTestCase
{
	private static final String BUTTONS = "modal:overlay:dialog:content:footer:buttons:";

	@Test
	void rendersClosedInTheDefaultTheme()
	{
		TestPage page = tester.startPage(new TestPage());

		assertFalse(page.modal.isOpen());
		assertTrue(tester.getTagByWicketId("modal").getAttribute("class")
			.contains(Modal.THEME_CLASS));
		tester.assertContains("antilia-modal.*\\.js");
		tester.assertContains("antilia-modal.*\\.css");
		assertFalse(tester.getLastResponseAsString().contains("Wicket.Modal.attach"));
	}

	@Test
	void opensWithTitleBodyAndButtonsOnTheRight()
	{
		TestPage page = tester.startPage(new TestPage());
		page.clicks = 0;

		page.modal.open(Model.of("Edit Ada"), new Label(Modal.BODY_ID, "The body"), null,
			IModalButtonFactory.close(Model.of("Cancel")),
			IModalButtonFactory.primary(Model.of("Save"), target -> page.clicks++));
		tester.startPage(page);

		TagTester dialog = tester.getTagByWicketId("dialog");
		assertEquals("dialog", dialog.getAttribute("role"));
		assertEquals("true", dialog.getAttribute("aria-modal"));
		assertEquals("Edit Ada", tester.getTagByWicketId("title").getValue());
		assertEquals("Close", tester.getTagByWicketId("close").getAttribute("aria-label"));
		assertEquals("The body", tester.getTagByWicketId("body").getValue());
		assertTrue(tester.getTagByWicketId("body").getAttribute("class")
			.contains("modal-dialog-body"));

		String response = tester.getLastResponseAsString();
		int cancel = response.indexOf("<span class=\"ant-button__label\">Cancel</span></button>");
		int save = response.indexOf("<span class=\"ant-button__label\">Save</span></button>");
		assertTrue(cancel > 0 && save > cancel, "Cancel, then Save");
		TagTester cancelTag = tester.getTagByWicketId("buttons");
		assertEquals("", cancelTag.getAttribute(ModalCloseButton.CLOSE_ATTRIBUTE));
		assertTrue(cancelTag.getAttributeContains("class", "ant-button--small"));
		assertTrue(response.contains("ant-button ant-button--primary ant-button--small"));
		tester.assertContains("antilia-button.*\\.css");
		assertFalse(response.contains("\"size\""));
		assertFalse(response.contains("\"width\""));

		assertTrue(response.contains("Wicket.Modal.attach(\"" +
			page.modal.getOverlayMarkupId() + "\""));
		assertTrue(response.contains("\"closeOnEscape\":true"));
		assertTrue(response.contains("\"closeOnClick\":false"));
		assertTrue(response.contains("\"draggable\":true"));
		assertTrue(response.contains("\"foldable\":true"));
		assertTrue(response.contains("\"onClose\":function(){"));
		assertFalse(response.contains("canClose"));

		tester.executeAjaxEvent(BUTTONS + "2", "click");
		assertEquals(1, page.clicks);
	}

	@Test
	void leavesOutTheFooterWithoutButtonsAndTheCloseButtonWhenTold()
	{
		TestPage page = tester.startPage(new TestPage());
		page.modal.setCloseButton(false).setDraggable(false).setFoldable(false)
			.setCloseOnClick(true);

		page.modal.open(null, new Label(Modal.BODY_ID, "The body"), null);
		tester.startPage(page);

		tester.assertInvisible("modal:overlay:dialog:content:footer");
		tester.assertInvisible("modal:overlay:dialog:content:header");
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("\"closeOnClick\":true"));
		assertTrue(response.contains("\"draggable\":false"));
		assertTrue(response.contains("\"foldable\":false"));
	}

	@Test
	void showsIconsInTheButtons()
	{
		TestPage page = tester.startPage(new TestPage());
		page.modal.open(Model.of("Edit"), new Label(Modal.BODY_ID, "The body"), null,
			IModalButtonFactory.close(Model.of("Cancel"), () -> "<svg class=\"x\"></svg>"),
			IModalButtonFactory.button(Model.of("Help"), () -> "<svg class=\"q\"></svg>",
				target -> page.clicks++));
		tester.startPage(page);

		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("<span class=\"ant-button__icon\" aria-hidden=\"true\">" +
			"<svg class=\"x\"></svg></span><span class=\"ant-button__label\">Cancel</span>"));
		assertTrue(response.contains("<svg class=\"q\"></svg></span>" +
			"<span class=\"ant-button__label\">Help</span>"));
		assertTrue(response.contains("ant-button ant-button--secondary ant-button--small"));
	}

	@Test
	void comesInASizeOrAWidth()
	{
		TestPage page = tester.startPage(new TestPage());
		page.modal.setSize(Modal.Size.LARGE).setWidth("40em");
		page.modal.open(Model.of("Edit"), new Label(Modal.BODY_ID, "The body"), null);
		tester.startPage(page);

		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("\"size\":\"large\""));
		assertTrue(response.contains("\"width\":\"40em\""));
		assertEquals(Modal.Size.LARGE, page.modal.getSize());
		assertEquals("40em", page.modal.getWidth());
	}

	@Test
	void opensADialogNestedInAnother()
	{
		NestingPage page = tester.startPage(new NestingPage());
		page.outer.open(Model.of("Outer"), new NestedBody(Modal.BODY_ID, page), null);
		tester.startPage(page);

		tester.clickLink("outer:overlay:dialog:content:body:more");
		assertTrue(page.inner.isOpen());
		tester.assertComponentOnAjaxResponse(page.inner);
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("Wicket.Modal.attach(\"" +
			page.inner.getOverlayMarkupId() + "\""));
		assertTrue(response.contains("\"closeOnEscape\":false"));

		tester.executeBehavior(page.inner.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));
		assertFalse(page.inner.isOpen());
		assertTrue(page.outer.isOpen());
	}

	@Test
	void closesWhenTheBrowserAsks()
	{
		TestPage page = tester.startPage(new TestPage());
		page.modal.open(Model.of("Edit"), new Label(Modal.BODY_ID, "The body"), null);
		tester.startPage(page);

		tester.executeBehavior(page.modal.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));

		assertFalse(page.modal.isOpen());
		assertEquals(1, page.closeRequests);
		tester.assertComponentOnAjaxResponse(page.modal);
	}

	@Test
	void staysOpenWhenTheServerSaysNo()
	{
		TestPage page = tester.startPage(new TestPage());
		page.closable = false;
		page.modal.open(Model.of("Edit"), new Label(Modal.BODY_ID, "The body"), null);
		tester.startPage(page);

		tester.executeBehavior(page.modal.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));
		assertTrue(page.modal.isOpen());

		page.closable = true;
		tester.executeBehavior(page.modal.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));
		assertFalse(page.modal.isOpen());
	}

	@Test
	void asksTheBrowserBeforeClosing()
	{
		TestPage page = tester.startPage(new TestPage());
		page.modal.confirmCloseIfModified(Model.of("Discard \"it\"?"));
		page.modal.open(Model.of("Edit"), new Label(Modal.BODY_ID, "The body"), null);
		tester.startPage(page);

		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("\"canClose\":function(){"));
		assertTrue(response.contains("Wicket.Modal.isModified(dialog) || " +
			"window.confirm(\"Discard \\\"it\\\"?\")"));

		page.modal.confirmCloseIfModified(new ResourceModel("Modal.close"));
		assertTrue(page.modal.getCanClose().contains("window.confirm(\"Close\")"));

		page.modal.setCanClose(null);
		assertNull(page.modal.getCanClose());
		page.modal.setCanClose("function() { return false; }");
		assertEquals("function() { return false; }", page.modal.getCanClose());
	}

	@Test
	void labelsTheCloseButtonInTheLanguageOfTheSession()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		TestPage page = tester.startPage(new TestPage());
		page.modal.open(Model.of("Bearbeiten"), new Label(Modal.BODY_ID, "Text"), null);
		tester.startPage(page);

		assertEquals("Schließen", tester.getTagByWicketId("close").getAttribute("title"));
	}

	@Test
	void wantsTheBodyId()
	{
		TestPage page = tester.startPage(new TestPage());

		assertThrows(IllegalArgumentException.class,
			() -> page.modal.open(Model.of("Edit"), new Label("other", "x"), null));
	}

	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		final Modal modal;

		int clicks;

		int closeRequests;

		boolean closable = true;

		TestPage()
		{
			modal = new Modal("modal")
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void onCloseRequest(AjaxRequestTarget target)
				{
					closeRequests++;
					super.onCloseRequest(target);
				}

				@Override
				protected boolean canClose(AjaxRequestTarget target)
				{
					return closable;
				}
			};
			add(modal);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body>" +
				"<div wicket:id=\"modal\"></div></body></html>");
		}
	}

	public static class NestingPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		final Modal outer = new Modal("outer");

		Modal inner;

		NestingPage()
		{
			add(outer);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body>" +
				"<div wicket:id=\"outer\"></div></body></html>");
		}
	}

	/** The body of the outer dialog: a link opening the inner one, nested in it. */
	public static class NestedBody extends org.apache.wicket.markup.html.panel.Panel
		implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		NestedBody(String id, NestingPage page)
		{
			super(id);
			Modal inner = new Modal("inner").setCloseOnEscape(false);
			page.inner = inner;
			add(inner);
			add(new org.apache.wicket.ajax.markup.html.AjaxLink<Void>("more")
			{
				private static final long serialVersionUID = 1L;

				@Override
				public void onClick(AjaxRequestTarget target)
				{
					inner.open(Model.of("Inner"), new Label(Modal.BODY_ID, "Nested"), target);
				}
			});
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<wicket:panel><a wicket:id=\"more\">More</a>" +
				"<div wicket:id=\"inner\"></div></wicket:panel>");
		}
	}
}
