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
package com.antiliacode.widgets.demo.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.antiliacode.widgets.demo.Dropdowns;
import com.antiliacode.widgets.demo.SeleniumBrowser;
import com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase;

/**
 * Drives {@link NotificationsPage} in Chrome and in Firefox, in deployment mode, so through the
 * minified script built from frontend/core, in its Wicket, React and Svelte tabs: the notification
 * the form chooses, where it goes, its countdown waiting under the pointer, the note of the page's
 * own, closing them, errors staying, a message of HTML, the colors of the theme and the feedback of
 * saving. Runs with {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class NotificationsBrowserTest extends FrameworkAppsTestCase
{
	private static final String SHOWN = ".antilia-notification:not(.antilia-notification-leaving)";

	private void open(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "notifications");
		// the Wicket tab is there at once; the apps render their forms
		wait.until(d -> !app().findElements(By.cssSelector(".notifications-demo__show")).isEmpty());
	}

	/**
	 * @return the select under a drop-down of the first form
	 */
	private WebElement select(int index)
	{
		return app().findElements(By.cssSelector(".notifications-demo__form select")).get(index);
	}

	private void type(WebElement field, String text)
	{
		field.clear();
		field.sendKeys(text);
	}

	/**
	 * Fills the first form in and shows its notification.
	 */
	private void show(String type, String title, String message, String position, String timeout)
	{
		Dropdowns.chooseByValue(driver, select(0), type);
		type(app().findElement(By.cssSelector(".notifications-demo__form input[type=text]")), title);
		type(app().findElement(By.cssSelector(".notifications-demo__form textarea")), message);
		Dropdowns.chooseByValue(driver, select(1), position);
		Dropdowns.chooseByValue(driver, select(2), timeout);
		app().findElement(By.cssSelector(".notifications-demo__show")).click();
	}

	private List<WebElement> shown()
	{
		return driver.findElements(By.cssSelector(SHOWN));
	}

	private WebElement waitForOne(String selector)
	{
		return wait.until(d -> d.findElements(By.cssSelector(selector)).stream().findFirst()
			.orElse(null));
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void showsTheNotificationTheFormChoosesWhereItSays(SeleniumBrowser browser, String framework)
	{
		open(browser, framework);

		show("error", "Oops", "Something broke.", "bottom-left", "0");

		WebElement notification = waitForOne(
			".antilia-notifications-stack-bottom-left .antilia-notification-error");
		assertEquals("alert", notification.getDomAttribute("role"));
		assertEquals("Oops", text(notification, ".antilia-notification-title"));
		assertEquals("Something broke.", text(notification, ".antilia-notification-body"));
		assertTrue(notification.findElements(By.cssSelector(".antilia-notification-countdown"))
			.isEmpty(), "stays until closed");
		assertEquals(1, notification.findElements(By.cssSelector(".antilia-notification-icon"))
			.size());
		WebElement stack = notification.findElement(By.xpath(".."));
		assertEquals("0px", stack.getCssValue("border-top-width"), "the page's styles stay out");
		Rectangle rect = notification.getRect();
		long height = (Long)js("return window.innerHeight;");
		assertTrue(rect.getX() < 40 && rect.getY() + rect.getHeight() > height - 40,
			"in the bottom left corner: " + rect.getX() + ", " + rect.getY());

		WebElement close = notification.findElement(By.cssSelector(".antilia-notification-close"));
		assertEquals("Close", close.getDomAttribute("title"));
		close.click();
		wait.until(d -> shown().isEmpty());
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void goesAfterItsTimeoutWaitingWhileThePointerIsOverIt(SeleniumBrowser browser,
		String framework)
	{
		open(browser, framework);

		show("success", "Saved", "Done.", "top-right", "3000");
		WebElement notification = waitForOne(
			".antilia-notifications-stack-top-right .antilia-notification-success");
		assertEquals("status", notification.getDomAttribute("role"));
		assertEquals(1, notification.findElements(
			By.cssSelector(".antilia-notification-countdown")).size());

		new Actions(driver).moveToElement(notification).perform();
		wait.until(d -> notification.getDomAttribute("class").contains(
			"antilia-notification-paused"));
		sleep(Duration.ofSeconds(4));
		assertEquals(1, shown().size(), "waits under the pointer");

		new Actions(driver).moveToElement(app().findElement(By.tagName("h3"))).perform();
		wait.withTimeout(Duration.ofSeconds(8)).until(d -> shown().isEmpty());
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void showsTheNoteOfThePageInItsColorWithItsIcon(SeleniumBrowser browser, String framework)
	{
		open(browser, framework);

		show("note", "Remember", "The meeting is at ten.", "top-center", "0");

		WebElement note = waitForOne(
			".antilia-notifications-stack-top-center .antilia-notification-note");
		assertTrue(note.getCssValue("border-left-color").matches("rgba?\\(124, 58, 237(, 1)?\\)"),
			note.getCssValue("border-left-color"));
		assertEquals(NotificationsPage.NOTE_ICON, note.findElement(
			By.cssSelector(".antilia-notification-icon path")).getDomAttribute("d"));
		long width = (Long)js("return window.innerWidth;");
		Rectangle rect = note.getRect();
		assertEquals(width / 2.0, rect.getX() + rect.getWidth() / 2.0, 20, "in the middle");
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void closesTheOneThatHasTheFocusOnEscapeAndAllAtOnce(SeleniumBrowser browser,
		String framework)
	{
		open(browser, framework);
		show("info", "First", "One.", "top-right", "0");
		wait.until(d -> shown().size() == 1);
		show("warning", "Second", "Two.", "top-right", "0");
		wait.until(d -> shown().size() == 2);
		// the newest on top
		assertEquals("Second", text(shown().get(0), ".antilia-notification-title"));

		shown().get(0).findElement(By.cssSelector(".antilia-notification-close")).sendKeys(
			Keys.ESCAPE);
		wait.until(d -> shown().size() == 1);
		assertEquals("First", text(shown().get(0), ".antilia-notification-title"));

		show("info", "Third", "Three.", "bottom-right", "0");
		wait.until(d -> shown().size() == 2);
		app().findElement(By.cssSelector(".notifications-demo__close-all")).click();
		wait.until(d -> shown().isEmpty());
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void keepsErrorsUntilClosedByDefaultAndTakesHtml(SeleniumBrowser browser, String framework)
	{
		open(browser, framework);

		show("info", "Info", "Goes.", "top-left", "-1");
		WebElement info = waitForOne(".antilia-notifications-stack-top-left .antilia-notification-info");
		assertEquals(1, info.findElements(By.cssSelector(".antilia-notification-countdown")).size(),
			"the timeout of the notifications");

		app().findElement(By.cssSelector(".notifications-demo__check input")).click();
		show("error", "Broken", "The server <strong>stopped</strong>.", "top-left", "-1");
		WebElement error = waitForOne(
			".antilia-notifications-stack-top-left .antilia-notification-error");
		assertTrue(error.findElements(By.cssSelector(".antilia-notification-countdown")).isEmpty(),
			"an error stays until closed");
		assertEquals("stopped", text(error, ".antilia-notification-body strong"));
		assertEquals("The server stopped.", text(error, ".antilia-notification-body"));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void takesTheColorsOfTheTheme(SeleniumBrowser browser, String framework)
	{
		open(browser, framework);
		// the dark theme, chosen in the top bar, which renders the page anew
		js("var s = document.querySelector('select[name=theme]'); s.value = 'DARK';" +
			"s.dispatchEvent(new Event('change', {bubbles: true}));");
		wait.until(d -> String.valueOf(d.findElement(By.tagName("body")).getDomAttribute("class"))
			.contains("antilia-theme-dark"));
		tab(framework).click();
		wait.until(d -> !app().findElements(By.cssSelector(".notifications-demo__show")).isEmpty());

		show("error", "Broken", "Dark.", "top-right", "0");
		WebElement error = waitForOne(".antilia-notification-error");
		assertTrue(error.getCssValue("border-left-color").matches("rgba?\\(255, 107, 107(, 1)?\\)"),
			"--antilia-theme-error of the dark theme: " + error.getCssValue("border-left-color"));
		assertTrue(error.getCssValue("background-color").matches("rgba?\\(30, 36, 44(, 1)?\\)"),
			"its surface: " + error.getCssValue("background-color"));
		WebElement header = error.findElement(By.cssSelector(".antilia-notification-header"));
		assertTrue(header.getCssValue("background-color").matches("rgba?\\(52, 85, 122(, 1)?\\)"),
			"the header in its primary color: " + header.getCssValue("background-color"));
		show("note", "Remember", "Lighter.", "top-right", "0");
		WebElement note = waitForOne(".antilia-notification-note");
		assertTrue(note.getCssValue("border-left-color").matches("rgba?\\(177, 151, 252(, 1)?\\)"),
			"the page's own type in the dark theme: " + note.getCssValue("border-left-color"));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void tellsHowSavingWent(SeleniumBrowser browser, String framework)
	{
		open(browser, framework);
		WebElement save = app().findElement(By.cssSelector(".notifications-demo__save"));

		save.click();
		WebElement error = waitForOne(".antilia-notification-error");
		assertEquals("Error", text(error, ".antilia-notification-title"));
		assertEquals("'Name' is required.", text(error, ".antilia-notification-body"));

		List<WebElement> fields = app().findElements(By.cssSelector(
			".notifications-demo__form input[type=text]"));
		type(fields.get(fields.size() - 1), "Ada");
		save.click();
		WebElement success = waitForOne(".antilia-notification-success");
		assertEquals("Hello, Ada! Your name was saved.", text(success, ".antilia-notification-body"));
		assertNoErrors();
	}

	/**
	 * @return the text of an element in another, read while it may still be fading in
	 */
	private static String text(WebElement parent, String selector)
	{
		return parent.findElement(By.cssSelector(selector)).getDomProperty("textContent");
	}

	private Object js(String script)
	{
		return ((org.openqa.selenium.JavascriptExecutor)driver).executeScript(script);
	}

	private static void sleep(Duration duration)
	{
		try
		{
			Thread.sleep(duration.toMillis());
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
	}
}
