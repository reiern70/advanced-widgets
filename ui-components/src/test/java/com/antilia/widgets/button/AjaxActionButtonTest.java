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
package com.antilia.widgets.button;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.button.ButtonBehavior.Size;
import com.antilia.widgets.button.ButtonBehavior.Variant;

/**
 * Draws buttons with an icon, a text or both, in a variant and a size, and runs their actions.
 */
class AjaxActionButtonTest extends WicketTestCase
{
	private static final String ICON = "<svg viewBox=\"0 0 1 1\"></svg>";

	@Test
	void rendersTheTextAndTheIconInItsClasses()
	{
		ButtonsPage page = new ButtonsPage();
		page.save.setIcon(() -> ICON).setVariant(Variant.PRIMARY).setSize(Size.LARGE);
		tester.startPage(page);

		TagTester tag = tester.getTagByWicketId("save");
		assertEquals("button", tag.getName());
		assertEquals("button", tag.getAttribute("type"));
		assertEquals("ant-button ant-button--primary ant-button--large", tag.getAttribute("class"));
		assertEquals("<span class=\"ant-button__icon\" aria-hidden=\"true\">" + ICON +
			"</span><span class=\"ant-button__label\">Save &amp; close</span>", tag.getValue());
		assertTrue(tester.getLastResponseAsString().contains("antilia-button.css"));
	}

	@Test
	void aButtonShowingItsIconOnlyHasItsTextAsTooltip()
	{
		ButtonsPage page = new ButtonsPage();
		page.save.setIcon(() -> ICON).setIconOnly(true);
		tester.startPage(page);

		TagTester tag = tester.getTagByWicketId("save");
		assertEquals("ant-button ant-button--secondary ant-button--medium ant-button--icon-only",
			tag.getAttribute("class"));
		assertEquals("Save & close", tag.getAttribute("title"));
		assertEquals("Save & close", tag.getAttribute("aria-label"));
		assertFalse(tag.getValue().contains("ant-button__label"), tag.getValue());
	}

	@Test
	void runsItsActionViaAjax()
	{
		ButtonsPage page = new ButtonsPage();
		tester.startPage(page);

		tester.executeAjaxEvent("save", "click");

		assertEquals(1, page.clicks);
	}

	@Test
	void theBehaviorDrawsLinksAndFormButtonsLeavingTheTypeOfASubmit()
	{
		tester.startPage(new ButtonsPage());

		TagTester link = tester.getTagByWicketId("link");
		assertEquals("ant-button ant-button--ghost ant-button--small", link.getAttribute("class"));
		TagTester submit = tester.getTagByWicketId("submit");
		assertEquals(null, submit.getAttribute("type"), "a form's button keeps submitting");
		assertEquals("ant-button ant-button--danger ant-button--medium", submit.getAttribute("class"));
	}

	@Test
	void aDisabledLinkIsMarkedDisabled()
	{
		ButtonsPage page = new ButtonsPage();
		page.get("link").setEnabled(false);
		tester.startPage(page);

		assertEquals("true", tester.getTagByWicketId("link").getAttribute("aria-disabled"));
	}

	public static class ButtonsPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		final AjaxActionButton save;

		int clicks;

		ButtonsPage()
		{
			save = new AjaxActionButton("save", Model.of("Save & close"))
			{
				private static final long serialVersionUID = 1L;

				@Override
				public void onClick(AjaxRequestTarget target)
				{
					clicks++;
				}
			};
			add(save);
			add(new ExternalLink("link", "#more", "More")
				.add(new ButtonBehavior(Variant.GHOST, Size.SMALL)));
			Form<Void> form = new Form<>("form");
			add(form);
			form.add(new Button("submit").add(new ButtonBehavior(Variant.DANGER, Size.MEDIUM)));
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body>" +
				"<button wicket:id='save'></button><a wicket:id='link'></a>" +
				"<form wicket:id='form'><button wicket:id='submit'>Delete</button></form>" +
				"</body></html>");
		}
	}
}
