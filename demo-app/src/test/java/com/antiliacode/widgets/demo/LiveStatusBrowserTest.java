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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.antiliacode.widgets.demo.data.Access;
import com.antiliacode.widgets.demo.data.IAccessRepository;

/**
 * A visitor of the stateless home page, in Chrome and in Firefox: its script posts the visitor's
 * clock, which makes the session and records the access, under the name the visitor told, and
 * the websocket of the page then connects and says that the site is live. Runs with
 * {@code -Pselenium}, see {@link SeleniumBrowser}.
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
	void theHomePageRecordsTheVisitAndGoesLive(SeleniumBrowser browser)
	{
		String visitor = "live-" + browser.name().toLowerCase();
		driver = browser.newDriver(1200, 900);
		driver.get("http://localhost:" + localPort + "/?" + DemoSession.VISITOR_PARAMETER + "=" +
			visitor);

		new WebDriverWait(driver, Duration.ofSeconds(15)).until(d -> d
			.findElement(By.cssSelector(".ws-status"))
			.getDomAttribute("class")
			.contains("ws-status--live"));
		List<Access> accesses = TestDatabase.context().getBean(IAccessRepository.class).findAll();
		assertTrue(accesses.stream().anyMatch(access -> access.getUserName()
			.equals("user-" + visitor) && access.getTimeZone() != null), accesses.toString());
	}
}
