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
	/**
	 * The questions of the captcha in English, each way it words them: a sum of the two numbers,
	 * or the first taken away from the second, or the second from the first.
	 */
	private static final List<Pattern> SUMS = List.of(
		Pattern.compile("How much is (\\w+) plus (\\w+)\\? Write it in digits\\."),
		Pattern.compile("Add (\\w+) to (\\w+): how much do you get\\? Write it in digits\\."),
		Pattern.compile("What is the sum of (\\w+) and (\\w+)\\? Write it in digits\\."));

	private static final List<Pattern> SECOND_MINUS_FIRST = List.of(
		Pattern.compile("Take (\\w+) away from (\\w+): how much is left\\? Write it in digits\\."),
		Pattern.compile("Subtract (\\w+) from (\\w+): how much do you get\\? Write it in digits\\."));

	private static final Pattern FIRST_MINUS_SECOND = Pattern
		.compile("How much is (\\w+) minus (\\w+)\\? Write it in digits\\.");

	private static final List<String> NUMBERS = List.of("zero", "one", "two", "three", "four",
		"five", "six", "seven", "eight", "nine", "ten");

	/**
	 * @return the number answering a question of the captcha
	 */
	private static int answer(String question)
	{
		for (Pattern sum : SUMS)
		{
			Matcher matcher = sum.matcher(question);
			if (matcher.matches())
			{
				return number(matcher, 1) + number(matcher, 2);
			}
		}
		for (Pattern difference : SECOND_MINUS_FIRST)
		{
			Matcher matcher = difference.matcher(question);
			if (matcher.matches())
			{
				return number(matcher, 2) - number(matcher, 1);
			}
		}
		Matcher matcher = FIRST_MINUS_SECOND.matcher(question);
		assertTrue(matcher.matches(), question);
		return number(matcher, 1) - number(matcher, 2);
	}

	private static int number(Matcher matcher, int group)
	{
		int number = NUMBERS.indexOf(matcher.group(group));
		assertTrue(number >= 0, matcher.group(group));
		return number;
	}

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
		int number = answer(question);
		wait.until(d -> answer().equals(d.switchTo().activeElement()));
		assertEquals(TestDatabase.WICKET_PASSWORD, password.getDomProperty("value"));
		answer().sendKeys(number + "", Keys.ENTER);

		wait.until(d -> !d.findElements(By.cssSelector(".topbar__account")).isEmpty());
		assertEquals("wicket", driver.findElement(By.cssSelector(".topbar__account")).getText());
		assertNoErrors();
	}
}
