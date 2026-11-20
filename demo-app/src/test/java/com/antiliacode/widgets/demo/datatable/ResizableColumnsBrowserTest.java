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
package com.antiliacode.widgets.demo.datatable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;

import com.antiliacode.widgets.demo.JettyTestCase;
import com.antiliacode.widgets.demo.SeleniumBrowser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Drives the stretched columns of the address book's table, which takes the width of its parent,
 * in Chrome and in Firefox: a table with borders of its own and border spacing is as wide as its
 * parent, not wider by them (#170). Runs with {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class ResizableColumnsBrowserTest extends JettyTestCase
{
	/**
	 * Gives the table the style of the first argument and its parent {@code overflow: auto}, lays
	 * the columns out for the parent again, and tells how the table fits in its parent.
	 */
	private static final String FIT_WITH_STYLE = """
		var table = document.querySelector('table.antilia-resizable-columns');
		table.style.cssText += ';' + arguments[0];
		table.parentElement.style.overflow = 'auto';
		Wicket.ResizableColumns.instances[table.id].refit();
		var parent = table.parentElement;
		var style = getComputedStyle(parent);
		var contentRight = parent.getBoundingClientRect().right -
			parseFloat(style.borderRightWidth) - parseFloat(style.paddingRight) -
			(parent.offsetWidth - parent.clientWidth - parseFloat(style.borderLeftWidth) -
				parseFloat(style.borderRightWidth));
		return {
			scrolls: parent.scrollWidth - parent.clientWidth,
			gap: contentRight - table.getBoundingClientRect().right,
			wider: table.classList.contains('antilia-resizable-columns-wider'),
			overhead: Wicket.ResizableColumns.instances[table.id].overhead()
		};
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

	static Stream<Arguments> borders()
	{
		Stream.Builder<Arguments> cases = Stream.builder();
		for (SeleniumBrowser browser : SeleniumBrowser.values())
		{
			cases.add(Arguments.of(browser,
				"border: 1px solid; border-collapse: separate; border-spacing: 2px; padding: 3px"));
			cases.add(Arguments.of(browser, "border: 4px solid; border-collapse: collapse"));
		}
		return cases.build();
	}

	@ParameterizedTest
	@MethodSource("borders")
	void aStretchedTableWithBordersIsAsWideAsItsParent(SeleniumBrowser browser, String style)
	{
		// wide enough for the columns at more than their data-min-width, which would make the
		// table wider than its parent on purpose
		driver = browser.newDriver(1900, 1000);
		driver.get("http://localhost:" + localPort + "/address-book");
		new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> !d
			.findElements(By.cssSelector("table.antilia-resizable-columns-laid-out")).isEmpty());

		@SuppressWarnings("unchecked")
		Map<String, Object> fit = (Map<String, Object>)((JavascriptExecutor)driver)
			.executeScript(FIT_WITH_STYLE, style);

		assertEquals(0L, ((Number)fit.get("scrolls")).longValue(),
			"the parent does not scroll sideways: " + fit);
		assertTrue(Math.abs(((Number)fit.get("gap")).doubleValue()) <= 1,
			"the table ends where the parent's content does: " + fit);
		assertFalse((Boolean)fit.get("wider"), "the table is not wider than its parent: " + fit);
		assertTrue(((Number)fit.get("overhead")).doubleValue() > 0,
			"the borders count: " + fit);
	}
}
