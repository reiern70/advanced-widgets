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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.csp.ContentSecurityPolicySettings;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.mock.MockApplication;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.Test;

class PageLoadingVeilBehaviorTest extends WicketTestCase
{
	/** A page whose {@code <body>} is veiled. */
	public static class BodyPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		public BodyPage()
		{
			add(new TransparentWebMarkupContainer("body").add(new PageLoadingVeilBehavior()));
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head><title>Veiled</title></head>" +
				"<body wicket:id=\"body\" class=\"app\"><a href=\"next\">Next</a></body></html>");
		}
	}

	/** A page whose {@code <html>} is veiled. */
	public static class HtmlPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		public HtmlPage()
		{
			add(new TransparentWebMarkupContainer("html").add(new PageLoadingVeilBehavior()));
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html wicket:id=\"html\"><head></head>" +
				"<body><a href=\"next\">Next</a></body></html>");
		}
	}

	/** A page rendering a veiled component again through Ajax. */
	static class AjaxPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		final WebMarkupContainer veiled;

		AjaxPage()
		{
			veiled = new WebMarkupContainer("veiled");
			add(veiled.add(new PageLoadingVeilBehavior()).setOutputMarkupId(true));
			add(new AjaxLink<Void>("again")
			{
				private static final long serialVersionUID = 1L;

				@Override
				public void onClick(AjaxRequestTarget target)
				{
					target.add(veiled);
				}
			});
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body>" +
				"<div wicket:id=\"veiled\"></div><a wicket:id=\"again\">Again</a></body></html>");
		}
	}

	@Test
	void veilsTheBodyFromTheFirstPaint()
	{
		BodyPage page = tester.startPage(BodyPage.class);

		TagTester body = TagTester.createTagByName(tester.getLastResponseAsString(), "body");
		assertEquals("app " + PageLoadingVeilBehavior.CSS_CLASS, body.getAttribute("class"));
		String head = head();
		assertTrue(styleSheet(head, "antilia-page-loading-veil.css").startsWith("<link"), head);
		assertTrue(head.matches("(?s).*<noscript><link [^>]*antilia-page-loading-veil-noscript" +
			"\\.css[^>]*>\\s*</noscript>.*"), head);
		String script = tag(head, "<script", "antilia-page-loading-veil.js");
		assertTrue(script.contains("defer"), "loaded deferred: " + script);
		assertTrue(page.isPageStateless(), "the page stays stateless");
	}

	@Test
	void veilsTheHtmlElement()
	{
		tester.startPage(HtmlPage.class);

		TagTester html = TagTester.createTagByName(tester.getLastResponseAsString(), "html");
		assertEquals(PageLoadingVeilBehavior.CSS_CLASS, html.getAttribute("class"));
		assertTrue(head().contains("antilia-page-loading-veil.js"));
	}

	@Test
	void anAjaxUpdateAddsNeitherTheClassNorTheResources()
	{
		AjaxPage page = tester.startPage(new AjaxPage());
		assertEquals(PageLoadingVeilBehavior.CSS_CLASS,
			tester.getTagByWicketId("veiled").getAttribute("class"));

		tester.clickLink("again");

		String answer = tester.getLastResponseAsString();
		assertTrue(answer.contains("id=\"" + page.veiled.getMarkupId() + "\""), answer);
		assertFalse(answer.contains(PageLoadingVeilBehavior.CSS_CLASS), answer);
		assertFalse(answer.contains("antilia-page-loading-veil"), answer);
	}

	@Test
	void aPageHasNoTagForIt()
	{
		WebPage page = new WebPage()
		{
			private static final long serialVersionUID = 1L;
		};

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> page.add(new PageLoadingVeilBehavior()));
		assertTrue(refused.getMessage().contains("<body>"), refused.getMessage());
	}

	@Test
	void holdsUnderTheStrictContentSecurityPolicy()
	{
		// an application whose policy is on from its start, as Wicket's is by default
		WicketTester strict = new WicketTester(new MockApplication()
		{
			@Override
			protected void init()
			{
				super.init();
				getCspSettings().blocking().strict();
			}
		});
		try
		{
			BodyPage page = strict.startPage(BodyPage.class);

			String nonce = page.getMetaData(ContentSecurityPolicySettings.NONCE_KEY);
			assertNotNull(nonce);
			String document = strict.getLastResponseAsString();
			String head = document.substring(document.indexOf("<head"),
				document.indexOf("</head>"));
			for (String tag : List.of(styleSheet(head, "antilia-page-loading-veil.css"),
				styleSheet(head, "antilia-page-loading-veil-noscript.css"),
				tag(head, "<script", "antilia-page-loading-veil.js")))
			{
				assertTrue(tag.contains("nonce=\"" + nonce + "\""), tag);
			}
			assertFalse(document.contains("<style"), "no inline style sheet: " + document);
			assertFalse(document.contains(" style="), "no inline style: " + document);
			String policy = strict.getLastResponse().getHeader("Content-Security-Policy");
			assertTrue(policy.contains("style-src 'nonce-" + nonce + "'"), policy);
			assertFalse(policy.contains("unsafe-inline"), policy);
		}
		finally
		{
			strict.destroy();
		}
	}

	@Test
	void servesTheStyleSheetsAndTheScript()
	{
		tester.startResourceReference(PageLoadingVeilBehavior.CSS);
		String veil = tester.getLastResponseAsString();
		assertTrue(veil.contains(".antilia-page-loading::before"), veil);
		assertTrue(veil.contains("--antilia-page-loading-timeout"), veil);

		tester.startResourceReference(PageLoadingVeilBehavior.NOSCRIPT_CSS);
		assertTrue(tester.getLastResponseAsString().contains("content: none"));

		tester.startResourceReference(PageLoadingVeilBehavior.JS);
		assertTrue(tester.getLastResponseAsString().contains("PageLoadingVeil"));
	}

	@Test
	void theStyleSheetInNoScriptIsRenderedOnceApartFromTheSameOutside()
	{
		PageLoadingVeilBehavior.NoScriptHeaderItem item =
			new PageLoadingVeilBehavior.NoScriptHeaderItem(
				CssHeaderItem.forReference(PageLoadingVeilBehavior.CSS));
		List<Object> tokens = new ArrayList<>();
		item.getRenderTokens().forEach(tokens::add);
		List<Object> outside = new ArrayList<>();
		item.getWrapped().getRenderTokens().forEach(outside::add);

		assertEquals(outside.size(), tokens.size());
		assertTrue(tokens.stream().noneMatch(outside::contains), tokens.toString());
		assertEquals(item, item.wrap(item.getWrapped()));
		assertEquals(item.hashCode(), item.wrap(item.getWrapped()).hashCode());
	}

	/** @return the head of the page last rendered */
	private String head()
	{
		String page = tester.getLastResponseAsString();
		return page.substring(page.indexOf("<head"), page.indexOf("</head>"));
	}

	/** @return the {@code <link>} of the style sheet */
	private static String styleSheet(String head, String name)
	{
		return tag(head, "<link", name);
	}

	/** @return the first tag of the given start holding the text */
	private static String tag(String html, String start, String text)
	{
		Matcher tags = Pattern.compile(Pattern.quote(start) + "[^>]*>").matcher(html);
		while (tags.find())
		{
			if (tags.group().contains(text))
			{
				return tags.group();
			}
		}
		throw new AssertionError("No " + start + " with " + text + " in " + html);
	}
}
