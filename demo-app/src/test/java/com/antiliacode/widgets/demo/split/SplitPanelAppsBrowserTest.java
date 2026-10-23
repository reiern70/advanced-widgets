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
package com.antiliacode.widgets.demo.split;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import com.antiliacode.widgets.demo.SeleniumBrowser;
import com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase;

/**
 * Drives the React and Svelte apps of {@link SplitPanelPage} in their tabs: the same split panels
 * as the Wicket ones, the sentence below the layout following its dividers and its collapsed panes
 * in the browser. Runs with {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class SplitPanelAppsBrowserTest extends FrameworkAppsTestCase
{
	private static final String START =
		"The files take 220 px of the width, the editor 65 % of the height.";

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void startsWhereThePageHasTheDividers(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "split-panel");

		wait.until(d -> sizes().equals(START));
		assertEquals(220, Splits.size(driver, layout()), 1);
		assertEquals(65, Splits.share(driver, editor()), 1);
		assertEquals("Drag, or use the arrow keys, to resize the panes",
			Splits.divider(layout()).getDomAttribute("aria-label"));
		assertEquals("Hide the left pane", Splits.divider(layout())
			.findElement(By.cssSelector(".antilia-split-collapse-first")).getDomProperty("title"));
		assertEquals("HomePage.java", editor().findElement(By.cssSelector(".split-demo__title"))
			.getText());
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void theSentenceFollowsTheDividers(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "split-panel");
		wait.until(d -> sizes().equals(START));

		Splits.drag(driver, Splits.divider(layout()), 80, 0);
		wait.until(d -> sizes().startsWith("The files take 300 px"));
		assertEquals(300, Splits.size(driver, layout()), 1);

		WebElement divider = Splits.divider(editor());
		divider.sendKeys(Keys.HOME);
		wait.until(d -> sizes().endsWith("the editor 20 % of the height."));
		// only the body of the pane scrolls, below its title
		assertEquals(0, Splits.scrollBody(driver, editor()), 0.5);
		Splits.doubleClick(driver, divider);
		wait.until(d -> sizes().endsWith("the editor 65 % of the height."));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void collapsesAPaneAndRestoresIt(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "split-panel");
		wait.until(d -> sizes().equals(START));

		Splits.collapse(driver, layout(), "first");
		wait.until(d -> sizes().startsWith("The files take none"));
		assertEquals(0, Splits.size(driver, layout()), 1);
		Splits.collapse(driver, layout(), "second");
		wait.until(d -> sizes().equals(START));
		assertEquals(220, Splits.size(driver, layout()), 1);

		Splits.collapse(driver, editor(), "second");
		wait.until(d -> sizes().endsWith("the editor all of the height."));
		assertTrue(Splits.collapsed(editor(), "second"));
		Splits.divider(editor()).sendKeys(Keys.ENTER);
		wait.until(d -> sizes().equals(START));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void theCheckBoxesSwitchTheThirdSplitPanel(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "split-panel");

		Splits.collapse(driver, settings(), "first");
		wait.until(d -> Splits.collapsed(settings(), "first"));
		app().findElement(By.xpath(".//label[contains(., 'One above the other')]/input")).click();
		wait.until(d -> settings().getDomAttribute("class").contains("antilia-split-vertical"));
		assertTrue(Splits.collapsed(settings(), "first"), "kept when the classes change");
		Splits.divider(settings()).sendKeys(Keys.ENTER);
		wait.until(d -> !Splits.collapsed(settings(), "first"));

		double height = Splits.space(driver, settings());
		Splits.drag(driver, Splits.divider(settings()), 0, -(int)Math.round(height * 0.2));
		assertEquals(30, Splits.share(driver, settings()), 1);

		app().findElement(By.xpath(".//label[contains(., 'Buttons hide the panes')]/input"))
			.click();
		wait.until(d -> Splits.divider(settings()).findElements(By.xpath("./*")).isEmpty());

		app().findElement(By.xpath(".//label[contains(., 'The divider moves')]/input")).click();
		wait.until(d -> settings().getDomAttribute("class").contains("antilia-split-fixed"));
		Splits.drag(driver, Splits.divider(settings()), 0, (int)Math.round(height * 0.3));
		assertEquals(30, Splits.share(driver, settings()), 1);
		assertTrue(Splits.divider(settings()).getDomAttribute("tabindex") == null);
		assertNoErrors();
	}

	private WebElement layout()
	{
		return app().findElement(By.cssSelector(".split-demo__layout > .antilia-split"));
	}

	private WebElement editor()
	{
		return layout().findElement(By.cssSelector(".antilia-split-second > .antilia-split"));
	}

	private WebElement settings()
	{
		return app().findElement(By.cssSelector(".split-demo__settings > .antilia-split"));
	}

	private String sizes()
	{
		return app().findElement(By.cssSelector(".split-demo__sizes")).getText();
	}
}
