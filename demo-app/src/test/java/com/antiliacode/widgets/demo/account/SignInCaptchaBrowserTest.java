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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.antiliacode.widgets.demo.SeleniumBrowser;
import com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase;

/**
 * Signing in, in Chrome and in Firefox: the page shows the account everybody shares, asks the
 * letters of an image every time and keeps no session until the visitor signed in, wrong letters
 * included. Runs with {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class SignInCaptchaBrowserTest extends FrameworkAppsTestCase
{
	private void open(SeleniumBrowser browser)
	{
		this.browser = browser;
		driver = browser.newDriver(1300, 1000);
		wait = new WebDriverWait(driver, Duration.ofSeconds(10), Duration.ofMillis(20));
		wait.ignoring(StaleElementReferenceException.class);
		driver.get("http://localhost:" + localPort + "/sign-in");
		wait.until(d -> !d.findElements(By.cssSelector(".show-password__button")).isEmpty());
	}

	private WebElement field(String name)
	{
		return driver.findElement(By.name(name));
	}

	private WebElement image()
	{
		return driver.findElement(By.cssSelector(".register__captcha-image img"));
	}

	/**
	 * @return the letters of the image shown, read from the token of the form: the test runs in
	 *         the server's JVM, which alone has the key
	 */
	private String letters()
	{
		return CaptchaToken.letters(field("captcha:token").getDomProperty("value"), Instant.now())
			.orElseThrow();
	}

	/**
	 * Types the account and the password the page shows, and letters.
	 */
	private void fillIn(String letters)
	{
		String user = driver.findElement(By.cssSelector(".register__shared code")).getText();
		String password = driver.findElements(By.cssSelector(".register__shared code")).get(1)
			.getText();
		field("p::name").clear();
		field("p::name").sendKeys(user);
		field("password").sendKeys(password);
		field("captcha:answer").sendKeys(letters);
		driver.findElement(By.cssSelector(".register__submit")).click();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void theLettersOfAnImageSignInAndNothingIsKeptBefore(SeleniumBrowser browser)
	{
		open(browser);
		// the image is drawn at twice its size, sharp on screens of a high density
		wait.until(d -> Long.valueOf(CaptchaImage.WIDTH * CaptchaImage.SCALE).equals(
			((JavascriptExecutor)d).executeScript("return arguments[0].naturalWidth;", image())));
		assertNull(driver.manage().getCookieNamed("JSESSIONID"));

		// wrong letters: told, other letters, and still no session
		String src = image().getDomAttribute("src");
		fillIn("zzzzz");
		wait.until(d -> d.findElements(By.cssSelector(".register__errors")).stream()
			.anyMatch(errors -> errors.getText()
				.contains("The letters are not those of the image. Try the new ones.")));
		wait.until(d -> !src.equals(image().getDomAttribute("src")));
		assertNull(driver.manage().getCookieNamed("JSESSIONID"));

		fillIn(letters());
		wait.until(d -> !d.findElements(By.cssSelector(".topbar__account")).isEmpty());
		assertEquals("user", driver.findElement(By.cssSelector(".topbar__account")).getText());
		assertNotNull(driver.manage().getCookieNamed("JSESSIONID"));
		assertNoErrors();
	}
}
