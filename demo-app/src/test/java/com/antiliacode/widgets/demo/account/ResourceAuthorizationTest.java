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
package com.antiliacode.widgets.demo.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.authorization.IAuthorizationStrategy;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.DemoSession;
import com.antiliacode.widgets.demo.HomePage;
import com.antiliacode.widgets.demo.TestDatabase;
import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.demo.datatable.DatabaseLocator;
import com.antiliacode.widgets.demo.error.NotFoundPage;
import com.antiliacode.widgets.demo.register.RegisterPage;
import com.antiliacode.widgets.theme.Theme;

/**
 * The resources the application serves on their own follow the demos: only visitors who signed
 * in get them, but those the public pages need; see {@link DemosAuthorization}.
 */
class ResourceAuthorizationTest
{
	/** What the pages load from the application: its resources, mounted or of packages. */
	private static final Pattern LOADED = Pattern
		.compile("(?:href|src)=\"(?:\\./|\\.\\./)*((?:wicket/resource|bootstrap|fontawesome)/[^\"]+)\"");

	private WicketTester tester;

	@BeforeEach
	void start()
	{
		TestDatabase.clear();
		tester = new WicketTester(new WicketApplication()
		{
			@Override
			public RuntimeConfigurationType getConfigurationType()
			{
				return RuntimeConfigurationType.DEPLOYMENT;
			}
		});
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	private int status(String method, String path)
	{
		tester.getRequest().setMethod(method);
		tester.executeUrl(path);
		return tester.getLastResponse().getStatus();
	}

	@Test
	void aVisitorWhoDidNotSignInGetsNoneOfTheDemosResources()
	{
		int users = DatabaseLocator.getLargeDatabase().getCount();

		assertEquals(403, status("GET", "api/users"));
		assertEquals(403, status("POST", "api/users"));
		assertEquals(403, status("DELETE", "api/users?key=1"));
		assertEquals(403, status("GET", "api/team"));
		assertEquals(403, status("POST", "api/team"));
		assertEquals(403, status("GET", "api/choices"));
		assertEquals(403, status("GET", "api/tree-of-life"));
		assertEquals(403, status("GET", "files/countries.csv"));
		// the resource the upload demo sends its files to
		assertEquals(403, status("POST", "uploads"));

		assertEquals(users, DatabaseLocator.getLargeDatabase().getCount());
		assertFalse(tester.getLastResponseAsString().contains("api/"),
			"the answer does not tell what was asked for");
	}

	@Test
	void theUsersRefuseAVisitorWhoDidNotSignInThemselvesToo()
	{
		// as if the authorization of the application let them through
		tester.getApplication().getSecuritySettings().setAuthorizationStrategy(
			new IAuthorizationStrategy.AllowAllAuthorizationStrategy());
		int users = DatabaseLocator.getLargeDatabase().getCount();

		assertEquals(403, status("GET", "api/users"));
		tester.getRequest().setHeader("Content-Type", "application/json");
		assertEquals(403, status("POST", "api/users"));
		assertEquals(403, status("DELETE", "api/users?key=1"));
		assertEquals(users, DatabaseLocator.getLargeDatabase().getCount());

		tester.startPage(HomePage.class);
		DemoSession.get().signIn("wicket");
		assertEquals(200, status("GET", "api/users"));
	}

	@Test
	void aUserWhoSignedInGetsThem()
	{
		tester.startPage(HomePage.class);
		DemoSession.get().signIn("wicket");

		assertEquals(200, status("GET", "api/users"));
		assertTrue(tester.getLastResponseAsString().contains("\"itemCount\""));
		assertEquals(200, status("GET", "api/team"));
		assertEquals(200, status("GET", "api/choices"));
	}

	@Test
	void thePublicPagesLoadWhatTheyNeed()
	{
		// in the Bootstrap theme, whose style sheet the pages load too
		tester.startPage(HomePage.class);
		DemoSession.get().setTheme(Theme.BOOTSTRAP);

		List<String> loaded = new ArrayList<>();
		for (Class<? extends WebPage> page : List.of(HomePage.class, SignInPage.class,
			RegisterPage.class, NotFoundPage.class))
		{
			tester.startPage(page);
			Matcher matcher = LOADED.matcher(tester.getLastResponseAsString());
			while (matcher.find())
			{
				loaded.add(matcher.group(1).replace("&amp;", "&"));
			}
		}
		assertTrue(loaded.stream().anyMatch(url -> url.startsWith("bootstrap/")), loaded.toString());
		assertTrue(loaded.stream().anyMatch(url -> url.startsWith("wicket/resource/")),
			loaded.toString());
		for (String url : loaded)
		{
			assertEquals(200, status("GET", url), url);
		}
		assertFalse(DemoSession.get().isSignedIn());
	}

	@Test
	void theClockOfTheVisitorAndTheApiOfTheAdministrationAreForEverybody()
	{
		// the API lets in administrators itself
		assertEquals(401, status("GET", "api/admin/users"));
		// the clock is posted
		assertEquals(405, status("GET", "api/access-time"));
	}

	@Test
	void aComponentOfAPublicPageServesItsResource()
	{
		tester.startPage(new ImagePage());
		Matcher src = Pattern.compile("<img src=\"(?:\\./|\\.\\./)*([^\"]+)\"")
			.matcher(tester.getLastResponseAsString());
		assertTrue(src.find(), tester.getLastResponseAsString());

		tester.executeUrl(src.group(1).replace("&amp;", "&"));

		assertEquals(200, tester.getLastResponse().getStatus());
		assertEquals("image/png", tester.getLastResponse().getContentType());
	}

	/**
	 * A public page with an image a resource of the page draws.
	 */
	public static class ImagePage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		ImagePage()
		{
			add(new NonCachingImage("image", new DynamicImageResource("png")
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected byte[] getImageData(Attributes attributes)
				{
					return new byte[] { (byte)0x89, 'P', 'N', 'G' };
				}
			}));
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream(
				"<html><body><img wicket:id=\"image\"/></body></html>");
		}
	}
}
