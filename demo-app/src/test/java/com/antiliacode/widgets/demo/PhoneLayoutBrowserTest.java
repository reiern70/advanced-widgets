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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.github.openjson.JSONObject;

/**
 * The pages with a table and a split panel are usable on a phone, which
 * {@link PhoneWidthBrowserTest} does not tell: they fit a frame 380 pixels wide, and there the
 * address book has its filters above the table rather than beside it, and the tables keep their
 * columns at their minimum widths, scrolling sideways in their own box. Runs with
 * {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class PhoneLayoutBrowserTest extends JettyTestCase
{
	/** The width of a small phone, in CSS pixels. */
	private static final int PHONE = 380;

	/**
	 * Loads a page into a frame of the width of a phone and calls back with how it lays out its
	 * split panel and its table, as JSON.
	 */
	private static final String MEASURE = """
		const [url, width, done] = arguments;
		const frame = document.createElement('iframe');
		frame.style.cssText = 'width:' + width + 'px;height:800px;border:0';
		frame.onload = async () => {
			const doc = frame.contentDocument;
			await new Promise((resolve) => setTimeout(resolve, 1500));
			const table = doc.querySelector('table.antilia-resizable-columns');
			const cells = [...table.querySelectorAll('thead th[data-min-width]')];
			const navigation = table.querySelector('.ant-data-table-navigation');
			const result = {
				extra: doc.documentElement.scrollWidth - doc.documentElement.clientWidth,
				stacked: !!doc.querySelector('.antilia-split-stacked'),
				wider: table.classList.contains('antilia-resizable-columns-wider'),
				scrolls: getComputedStyle(table.parentElement).overflowX,
				minimums: cells.length,
				narrower: cells.filter((cell) => cell.getBoundingClientRect().width + 1 <
					Number(cell.getAttribute('data-min-width'))).map((cell) => cell.textContent.trim()),
				navigation: navigation.getBoundingClientRect().width -
					table.parentElement.clientWidth
			};
			frame.remove();
			done(JSON.stringify(result));
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
	void theAddressBookHasItsFiltersAboveTheTableOnAPhone(SeleniumBrowser browser)
	{
		open(browser);
		driver.get(base() + "address-book");
		new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> !d
			.findElements(By.cssSelector(".antilia-split.antilia-split-horizontal")).isEmpty());
		assertTrue(driver.findElements(By.cssSelector(".antilia-split-stacked")).isEmpty(),
			"the filters stay beside the table on a wide screen");

		JSONObject phone = measure("address-book");

		assertTrue(phone.getBoolean("stacked"), "the filters above the table: " + phone);
		assertWithinThePhone(phone);
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void theAdministrationKeepsItsColumnsReadableOnAPhone(SeleniumBrowser browser)
	{
		open(browser);
		driver.get(base() + "sign-in");
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
		wait.until(d -> !d.findElements(By.cssSelector("input[autocomplete='username']")).isEmpty());
		driver.findElement(By.cssSelector("input[autocomplete='username']")).sendKeys("admin");
		driver.findElement(By.cssSelector("input[autocomplete='current-password']"))
			.sendKeys(TestDatabase.ADMIN_PASSWORD);
		driver.findElement(By.cssSelector(".register__submit")).click();
		wait.until(d -> !d.getCurrentUrl().contains("sign-in"));

		JSONObject phone = measure("admin");

		assertWithinThePhone(phone);
	}

	private void open(SeleniumBrowser browser)
	{
		driver = browser.newDriver(1300, 1000);
		driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
	}

	private String base()
	{
		return "http://localhost:" + localPort + "/";
	}

	/**
	 * @return how the page lays out in a frame of the width of a phone
	 */
	private JSONObject measure(String page)
	{
		return new JSONObject((String)((JavascriptExecutor)driver).executeAsyncScript(MEASURE,
			base() + page, PHONE));
	}

	/**
	 * The page does not scroll sideways, its table does in its own box, its columns as wide as
	 * their minimums at least, and the navigation of the table stays in the box.
	 */
	private static void assertWithinThePhone(JSONObject phone)
	{
		assertTrue(phone.getInt("extra") <= 1, "the page scrolls sideways: " + phone);
		assertTrue(phone.getInt("minimums") > 0, phone.toString());
		assertEquals(0, phone.getJSONArray("narrower").length(),
			"columns narrower than their minimum: " + phone);
		assertTrue(phone.getBoolean("wider"), "the table is wider than the phone: " + phone);
		assertEquals("auto", phone.getString("scrolls"), phone.toString());
		assertFalse(phone.getDouble("navigation") > 1,
			"the navigation is wider than the box the table scrolls in: " + phone);
	}
}
