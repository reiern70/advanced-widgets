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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * A visitor of the stateless home page, in Chrome and in Firefox, gets no session: no cookie, and
 * no status of the websocket, which needs one. Runs with {@code -Pselenium}, see
 * {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class LiveStatusBrowserTest extends JettyTestCase
{
	private WebDriver driver;

	@Override
	@AfterEach
	public void after() throws Exception
	{
		if (driver != null)
		{
			driver.quit();
		}
		super.after();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void aVisitorWhoOnlyLooksGetsNoSession(SeleniumBrowser browser) throws InterruptedException
	{
		driver = browser.newDriver(1200, 900);
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
		driver.get("http://localhost:" + localPort + "/");
		wait.until(d -> !d.findElements(By.cssSelector(".index__title")).isEmpty());
		// the scripts of the page ran and asked what they ask
		Thread.sleep(1000);

		assertNull(driver.manage().getCookieNamed("JSESSIONID"),
			driver.manage().getCookies().toString());
		assertTrue(driver.findElements(By.cssSelector(".ws-status")).isEmpty());

		// nor does the sign-in page make one, which says how many sessions are in use
		driver.get("http://localhost:" + localPort + "/sign-in");
		wait.until(d -> !d.findElements(By.cssSelector(".register__captcha-image img")).isEmpty());
		Thread.sleep(1000);
		assertNull(driver.manage().getCookieNamed("JSESSIONID"),
			driver.manage().getCookies().toString());
		assertTrue(driver.getPageSource().contains("of 10 sessions in use"));
	}
}
