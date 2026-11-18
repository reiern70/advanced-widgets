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
package com.antiliacode.widgets.veil;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.veil.LocalVeilBehavior;
import org.apache.wicket.extensions.ajax.veil.PageVeilBehavior;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

class VeilGapsTest extends WicketTestCase
{
	private static final String WICKET_VEIL = "wicket-veil";

	private static final String ADD_ON = "antilia-veil-gaps";

	@Test
	void aPageWithWicketsVeilHasTheAddOnAfterIt()
	{
		tester.startPage(new TestPage(true));

		String page = tester.getLastResponseAsString();
		int veil = page.indexOf(WICKET_VEIL + ".js");
		int addOn = page.indexOf(ADD_ON + ".js");
		assertTrue(veil >= 0 && addOn > veil, page);
		assertTrue(page.contains(ADD_ON + ".css"), page);
	}

	@Test
	void aPageWithoutAVeilHasNoAddOn()
	{
		tester.startPage(new TestPage(false));

		assertFalse(tester.getLastResponseAsString().contains(ADD_ON), "no add-on");
	}

	@Test
	void aLocalVeilAnAjaxAnswerBringsHasTheAddOnToo()
	{
		tester.startPage(new TestPage(false));

		tester.clickLink("show");

		String answer = tester.getLastResponseAsString();
		int veil = answer.indexOf(WICKET_VEIL + ".js");
		int addOn = answer.indexOf(ADD_ON + ".js");
		assertTrue(veil >= 0 && addOn > veil, answer);
	}

	@Test
	void anApplicationCanLeaveWicketsVeilAsItIs()
	{
		VeilGaps.setEnabled(tester.getApplication(), false);
		tester.startPage(new TestPage(true));

		assertTrue(tester.getLastResponseAsString().contains(WICKET_VEIL + ".js"));
		assertFalse(tester.getLastResponseAsString().contains(ADD_ON), "no add-on");
		assertFalse(VeilGaps.isEnabled(tester.getApplication()));

		VeilGaps.setEnabled(tester.getApplication(), true);
		tester.startPage(new TestPage(true));
		assertTrue(tester.getLastResponseAsString().contains(ADD_ON + ".js"));
	}

	/**
	 * A page with Wicket's page veil or none, and a link showing a part with a local veil.
	 */
	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		TestPage(boolean pageVeil)
		{
			if (pageVeil)
			{
				add(new PageVeilBehavior());
			}
			WebMarkupContainer part = new WebMarkupContainer("part");
			part.setOutputMarkupPlaceholderTag(true).setVisible(false);
			part.add(new LocalVeilBehavior());
			add(part);
			add(new AjaxLink<Void>("show")
			{
				private static final long serialVersionUID = 1L;

				@Override
				public void onClick(AjaxRequestTarget target)
				{
					part.setVisible(true);
					target.add(part);
				}
			});
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body>" +
				"<div wicket:id=\"part\"></div><a wicket:id=\"show\">Show</a></body></html>");
		}
	}
}
