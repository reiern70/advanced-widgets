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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * No demo is wider than a phone: every demo of the home page, in each of its framework tabs, fits
 * a frame 380 pixels wide, so that a phone never scrolls the page sideways. The frame, of the same
 * origin as the page measuring it, lays the demo out at that width, which headless browsers do not
 * make their windows. Runs with {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class PhoneWidthBrowserTest extends JettyTestCase
{
	/** The width of a small phone, in CSS pixels. */
	private static final int PHONE = 380;

	/**
	 * Loads a page into a frame of the width of a phone and calls back, once each framework tab
	 * was shown, with the widths its document took beyond the frame, in "tab: pixels" entries.
	 */
	private static final String MEASURE = """
		const [url, width, done] = arguments;
		const frame = document.createElement('iframe');
		frame.style.cssText = 'width:' + width + 'px;height:800px;border:0';
		frame.onload = async () => {
			const doc = frame.contentDocument;
			const pause = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
			const wider = [];
			const measure = (tab) => {
				const extra = doc.documentElement.scrollWidth - doc.documentElement.clientWidth;
				if (extra > 1) {
					wider.push(tab + ': ' + extra);
				}
			};
			await pause(500);
			const tabs = [...doc.querySelectorAll('[role=tab][data-framework]')];
			if (!tabs.length) {
				measure('page');
			}
			for (const tab of tabs) {
				tab.click();
				await pause(1500);
				measure(tab.getAttribute('data-framework'));
			}
			frame.remove();
			done(wider);
		};
		frame.src = url;
		document.body.appendChild(frame);
		""";

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
	void noDemoIsWiderThanAPhone(SeleniumBrowser browser)
	{
		driver = browser.newDriver(1200, 1000);
		driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
		String base = "http://localhost:" + localPort + "/";
		driver.get(base);
		List<String> demos = driver.findElements(By.cssSelector("a.index__link"))
			.stream()
			.map(link -> link.getDomAttribute("href").replaceFirst("^\\./", ""))
			.toList();
		assertEquals(Demos.all().size(), demos.size(), demos.toString());

		List<String> wider = new ArrayList<>();
		for (String demo : demos)
		{
			@SuppressWarnings("unchecked")
			List<String> found = (List<String>)((JavascriptExecutor)driver)
				.executeAsyncScript(MEASURE, base + demo, PHONE);
			found.forEach(each -> wider.add(demo + " " + each));
		}
		assertTrue(wider.isEmpty(), "wider than a phone, in pixels: " + wider);
	}
}
