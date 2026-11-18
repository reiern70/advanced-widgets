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
package com.antiliacode.widgets.demo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.csp.ContentSecurityPolicySettings;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.account.SignInPage;
import com.antiliacode.widgets.veil.PageLoadingVeilBehavior;

/**
 * The content security policy of the pages: their scripts by nonce, and no page in a frame of
 * another site; the veil over a page while it loads holds under it, without inline style.
 */
class ContentSecurityPolicyTest
{
	private WicketTester tester;

	@BeforeEach
	void start()
	{
		tester = new WicketTester(new WicketApplication());
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	@Test
	void onlyTheSiteFramesItsPages()
	{
		for (Class<? extends BasePage> page : List.of(HomePage.class, SignInPage.class))
		{
			tester.startPage(page);
			String policy = tester.getLastResponse().getHeader("Content-Security-Policy");
			assertTrue(policy != null && policy.contains("frame-ancestors 'self'"), policy);
			assertTrue(policy.contains("'nonce-"), policy);
		}
	}

	@Test
	void thePagesAreVeiledWhileTheyLoadByStyleSheetsAndAScriptWithTheNonce() throws Exception
	{
		for (Class<? extends BasePage> page : List.of(HomePage.class, SignInPage.class))
		{
			tester.startPage(page);
			String document = tester.getLastResponseAsString();
			String nonce = tester.getLastRenderedPage()
				.getMetaData(ContentSecurityPolicySettings.NONCE_KEY);

			TagTester body = TagTester.createTagByName(document, "body");
			assertTrue(body.getAttribute("class").contains(PageLoadingVeilBehavior.CSS_CLASS),
				body.getAttribute("class"));
			assertNull(body.getAttribute("style"));
			// the style sheets, one in a <noscript>, and the script, their names with a version
			for (String name : List.of("<link [^>]*/antilia-page-loading-veil-ver-\\d+\\.css",
				"<noscript><link [^>]*/antilia-page-loading-veil-noscript-ver-\\d+\\.css",
				"<script [^>]*/antilia-page-loading-veil-ver-\\d+\\.js"))
			{
				Matcher tag = Pattern.compile(name + "[^>]*>").matcher(document);
				assertTrue(tag.find(), name + " in " + document);
				assertTrue(tag.group().contains("nonce=\"" + nonce + "\""), tag.group());
			}
			// the slow script of the browser tests, which production never has
			assertFalse(document.contains(SlowScript.PATH), document);
		}
		String basedir = System.getProperty("basedir");
		String webXml = Files.readString(Path.of(basedir != null ? basedir : ".",
			"src/main/webapp/WEB-INF/web.xml"));
		assertFalse(webXml.contains(SlowScript.PARAMETER), webXml);
	}
}
