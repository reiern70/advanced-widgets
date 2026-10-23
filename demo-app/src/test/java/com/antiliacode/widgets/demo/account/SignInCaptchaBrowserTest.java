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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.antiliacode.widgets.demo.SeleniumBrowser;
import com.antiliacode.widgets.demo.TestDatabase;
import com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase;

/**
 * The captcha of the sign-in, in Chrome and in Firefox: after failed attempts it asks for the
 * letters of an image, which other letters replace without losing what was typed; wrong letters
 * keep the name and the password too; a question can be answered instead, which signs in. Runs
 * with {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class SignInCaptchaBrowserTest extends FrameworkAppsTestCase
{
	private static final Pattern QUESTION = Pattern
		.compile("How much is (\\w+) (plus|minus) (\\w+)\\? Write it in digits\\.");

	private static final List<String> NUMBERS = List.of("zero", "one", "two", "three", "four",
		"five", "six", "seven", "eight", "nine", "ten");

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

	private WebElement password()
	{
		return field("password");
	}

	private WebElement answer()
	{
		return field("captcha:answer");
	}

	private WebElement image()
	{
		return driver.findElement(By.cssSelector(".register__captcha-image img"));
	}

	private void send()
	{
		driver.findElement(By.cssSelector(".register__submit")).click();
	}

	private void waitForError(String text)
	{
		wait.until(d -> d.findElements(By.cssSelector(".register__errors")).stream()
			.anyMatch(errors -> errors.getText().contains(text)));
	}

	/**
	 * Fails to sign in until the captcha is asked, each wrong password emptied.
	 */
	private void failUntilTheCaptchaIsAsked()
	{
		for (int attempt = 0; attempt < SignInAttempts.BEFORE_CAPTCHA; attempt++)
		{
			WebElement password = password();
			field("p::name").clear();
			field("p::name").sendKeys("wicket");
			password.sendKeys("wrong-" + attempt);
			send();
			wait.until(ExpectedConditions.stalenessOf(password));
			assertEquals("", password().getDomProperty("value"));
		}
		wait.until(d -> image().isDisplayed());
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void theCaptchaKeepsWhatWasTypedAndAQuestionSignsIn(SeleniumBrowser browser)
	{
		open(browser);
		failUntilTheCaptchaIsAsked();
		// the image is drawn at twice its size, sharp on screens of a high density
		wait.until(d -> Long.valueOf(CaptchaImage.WIDTH * CaptchaImage.SCALE).equals(
			((JavascriptExecutor)d).executeScript("return arguments[0].naturalWidth;", image())));
		WebElement password = password();
		password.sendKeys(TestDatabase.WICKET_PASSWORD);

		// other letters: the image only is drawn again
		String src = image().getDomAttribute("src");
		driver.findElement(By.linkText("Show other letters")).click();
		wait.until(d -> !src.equals(image().getDomAttribute("src")));
		assertEquals("wicket", field("p::name").getDomProperty("value"));
		assertEquals(TestDatabase.WICKET_PASSWORD, password.getDomProperty("value"));

		// wrong letters: the password typed stays, the focus goes to the letters
		answer().sendKeys("zzzzz");
		send();
		waitForError("The letters are not those of the image. Try the new ones.");
		assertEquals(TestDatabase.WICKET_PASSWORD, password.getDomProperty("value"));
		assertEquals("wicket", field("p::name").getDomProperty("value"));
		wait.until(d -> answer().equals(d.switchTo().activeElement()));
		assertEquals("", answer().getDomProperty("value"));

		// a question instead, answered with the keyboard, signs in with the password still typed
		driver.findElement(By.linkText("Hard to read? Answer a question instead")).click();
		wait.until(d -> d.findElements(By.cssSelector(".register__captcha-image")).isEmpty());
		String question = driver.findElement(By.cssSelector(".register__captcha label span"))
			.getText();
		Matcher matcher = QUESTION.matcher(question);
		assertTrue(matcher.matches(), question);
		int first = NUMBERS.indexOf(matcher.group(1));
		int second = NUMBERS.indexOf(matcher.group(3));
		int number = "plus".equals(matcher.group(2)) ? first + second : first - second;
		wait.until(d -> answer().equals(d.switchTo().activeElement()));
		assertEquals(TestDatabase.WICKET_PASSWORD, password.getDomProperty("value"));
		answer().sendKeys(number + "", Keys.ENTER);

		wait.until(d -> !d.findElements(By.cssSelector(".topbar__account")).isEmpty());
		assertEquals("wicket", driver.findElement(By.cssSelector(".topbar__account")).getText());
		assertNoErrors();
	}
}
