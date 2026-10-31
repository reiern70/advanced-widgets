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
package com.antiliacode.widgets.demo.wizard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.antiliacode.widgets.demo.JettyTestCase;
import com.antiliacode.widgets.demo.SeleniumBrowser;
import com.antiliacode.widgets.demo.Toasts;

/**
 * Drives {@link WizardPage} in Chrome and in Firefox, in deployment mode, so through the minified
 * script built from frontend/core: the key of mosquitoes answered with the mouse and with Enter,
 * the focus on the title of each new step, a crumb going back, the image card uploaded by Ajax and
 * given the text its step suggests, and the Bootstrap look. Runs with {@code -Pselenium}, see
 * {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class WizardBrowserTest extends JettyTestCase
{
	/** The files to upload, in target: a browser of a snap does not see the /tmp of the JVM. */
	private final Path folder = Path.of(System.getProperty("basedir", "."), "target",
		"wizard-browser-test");

	private WebDriver driver;

	private WebDriverWait wait;

	private SeleniumBrowser browser;

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

	private void open(SeleniumBrowser browser)
	{
		this.browser = browser;
		driver = browser.newDriver(1300, 1000);
		wait = new WebDriverWait(driver, Duration.ofSeconds(10), Duration.ofMillis(20));
		wait.ignoring(StaleElementReferenceException.class);
		driver.get("http://localhost:" + localPort + "/wizard");
		wait.until(d -> d.findElements(By.cssSelector(".antilia-wizard")).size() == 2);
	}

	/**
	 * @return the wizard of the key of mosquitoes, the first one, or that of the image card
	 */
	private WebElement wizard(boolean key)
	{
		return driver.findElements(By.cssSelector(".antilia-wizard")).get(key ? 0 : 1);
	}

	private String title(boolean key)
	{
		return wizard(key).findElement(By.cssSelector(".antilia-wizard-title")).getText().trim();
	}

	private WebElement button(boolean key, String kind)
	{
		return wizard(key).findElement(By.cssSelector(".antilia-wizard-" + kind));
	}

	/**
	 * Clicks the label of an answer of the step shown, by its text.
	 */
	private void choose(boolean key, String answer)
	{
		wizard(key).findElements(By.cssSelector(".antilia-wizard-body label")).stream()
			.filter(label -> label.getText().trim().equals(answer)).findFirst().orElseThrow()
			.click();
	}

	private WebElement focused()
	{
		return driver.switchTo().activeElement();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void theKeyLeadsToAMosquitoWithTheMouseAndWithEnter(SeleniumBrowser browser)
	{
		open(browser);
		assertEquals("Couplet 1", title(true));
		assertTrue(button(true, "previous").getDomAttribute("disabled") != null);

		button(true, "next").click();
		wait.until(d -> Toasts.latest(d).contains("required"));
		assertEquals("Couplet 1", title(true));

		choose(true, "Much shorter than the proboscis; at rest the body is parallel to the surface");
		button(true, "next").click();
		wait.until(d -> "Couplet 2".equals(title(true)));
		wait.until(d -> focused().getText().trim().equals("Couplet 2"));

		// Enter in an answer clicks Next, the default button of the wizard's form
		choose(true, "Pointed, the cerci sticking out; legs often banded in white");
		focused().sendKeys(Keys.ENTER);
		wait.until(d -> "Couplet 3".equals(title(true)));

		choose(true, "In the shape of a lyre");
		button(true, "next").click();
		wait.until(d -> "Aedes aegypti".equals(title(true)));
		assertEquals(List.of(), wizard(true).findElements(By.cssSelector(".antilia-wizard-next")));

		// the crumb of the second couplet goes back to it
		wizard(true).findElements(By.cssSelector(".antilia-breadcrumb-link")).stream()
			.filter(link -> link.getText().trim().equals("Couplet 2")).findFirst().orElseThrow()
			.click();
		wait.until(d -> "Couplet 2".equals(title(true)));
		button(true, "last").click();
		wait.until(d -> "Aedes aegypti".equals(title(true)));

		button(true, "finish").click();
		wait.until(d -> Toasts.latest(d).equals("Identified as Aedes aegypti."));
		wait.until(d -> "Couplet 1".equals(title(true)));
		assertNoErrors();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void theCardUploadsAnImageAndTakesTheTextItsStepSuggests(SeleniumBrowser browser)
		throws IOException
	{
		open(browser);
		assertEquals("Upload an image", title(false));
		Path image = Files.createDirectories(folder).resolve("sunset_beach.png").toAbsolutePath();
		ImageIO.write(new BufferedImage(64, 48, BufferedImage.TYPE_INT_RGB), "png", image.toFile());

		wizard(false).findElement(By.cssSelector("input[type=file]")).sendKeys(image.toString());
		button(false, "next").click();

		wait.until(d -> "What to do with sunset_beach.png".equals(title(false)));
		WebElement thumbnail = wizard(false).findElement(By.cssSelector(
			".wizard-demo__thumbnail"));
		wait.until(d -> ((Number)((JavascriptExecutor)d).executeScript(
			"return arguments[0].naturalWidth", thumbnail)).intValue() == 64);

		choose(false, "Write a text over it");
		button(false, "next").click();
		wait.until(d -> "A text over sunset_beach.png".equals(title(false)));

		wizard(false).findElements(By.cssSelector(".antilia-wizard-buttons button")).stream()
			.filter(button -> button.getText().trim().equals("Suggest a text")).findFirst()
			.orElseThrow().click();
		wait.until(d -> wizard(false).findElement(By.cssSelector("input[type=text]"))
			.getDomProperty("value").startsWith("sunset beach · 64 × 48 px · "));

		button(false, "next").click();
		wait.until(d -> "Preview of sunset_beach.png".equals(title(false)));
		assertTrue(wizard(false).findElement(By.cssSelector(".wizard-demo__text")).getText()
			.startsWith("sunset beach"));

		button(false, "finish").click();
		wait.until(d -> Toasts.latest(d).equals("The card of sunset_beach.png is ready."));
		wait.until(d -> "Upload an image".equals(title(false)));
		assertNoErrors();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void takesBootstrapsLook(SeleniumBrowser browser)
	{
		open(browser);
		((JavascriptExecutor)driver).executeScript("var s = arguments[0]; s.selectedIndex = " +
			"Array.from(s.options).findIndex(o => o.text === 'Bootstrap'); s.dispatchEvent(" +
			"new Event('change', {bubbles: true}));",
			driver.findElement(By.cssSelector(".topbar__setting select[name$='look']")));
		wait.until(d -> d.findElement(By.tagName("body")).getDomAttribute("class")
			.contains("antilia-look-bootstrap"));
		wait.until(d -> d.findElements(By.cssSelector(".antilia-wizard")).size() == 2);

		assertTrue(wizard(true).getDomAttribute("class").contains("card"));
		assertTrue(wizard(true).findElement(By.cssSelector(".antilia-wizard-header"))
			.getDomAttribute("class").contains("card-header"));
		assertTrue(wizard(true).findElement(By.cssSelector(".antilia-wizard-buttons-bottom"))
			.getDomAttribute("class").contains("card-footer"));

		choose(true, "As long as the proboscis; at rest the body is at an angle to the surface");
		button(true, "next").click();
		wait.until(d -> "Anopheles".equals(title(true)));
		assertNoErrors();
	}

	/**
	 * Fails on errors in Chrome's console, a script or a style the policy blocked for one.
	 */
	private void assertNoErrors()
	{
		if (browser != SeleniumBrowser.CHROME)
		{
			return;
		}
		List<LogEntry> errors = driver.manage().logs().get("browser").getAll().stream()
			.filter(entry -> entry.getLevel().intValue() >= java.util.logging.Level.SEVERE
				.intValue())
			// the demo application has no icon
			.filter(entry -> !entry.getMessage().toLowerCase(Locale.ROOT).contains("favicon"))
			.toList();
		assertEquals(List.of(), errors, "errors in the console");
	}
}
