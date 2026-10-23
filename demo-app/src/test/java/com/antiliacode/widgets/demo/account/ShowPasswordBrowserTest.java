/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.antiliacode.widgets.demo.SeleniumBrowser;
import com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase;

/**
 * Shows and hides the password of the sign-in with the eye at the end of its field, by mouse and
 * by keyboard, in Chrome and in Firefox; sending the form hides it again. Runs with
 * {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class ShowPasswordBrowserTest extends FrameworkAppsTestCase
{
	private static final String TYPED = "A$ecret_4321";

	private void open(SeleniumBrowser browser, String path)
	{
		this.browser = browser;
		driver = browser.newDriver(1300, 1000);
		wait = new WebDriverWait(driver, Duration.ofSeconds(10), Duration.ofMillis(20));
		wait.ignoring(StaleElementReferenceException.class);
		driver.get("http://localhost:" + localPort + "/" + path);
		wait.until(d -> !d.findElements(By.cssSelector(".show-password__button")).isEmpty());
	}

	private WebElement password()
	{
		return driver.findElement(By.name("password"));
	}

	private WebElement eye()
	{
		return driver.findElement(By.cssSelector(".show-password__button"));
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void theEyeShowsAndHidesThePassword(SeleniumBrowser browser)
	{
		open(browser, "sign-in");
		WebElement password = password();
		password.sendKeys(TYPED);
		assertEquals("password", password.getDomProperty("type"));
		assertEquals("Show password", eye().getDomAttribute("aria-label"));

		// the eye is at the end of the field, inside it
		Rectangle field = password.getRect();
		Rectangle button = eye().getRect();
		assertTrue(button.getX() > field.getX() + field.getWidth() / 2 &&
			button.getX() + button.getWidth() <= field.getX() + field.getWidth() &&
			button.getY() >= field.getY() &&
			button.getY() + button.getHeight() <= field.getY() + field.getHeight(),
			"field " + field.getDimension() + " at " + field.getPoint() + ", eye " +
				button.getDimension() + " at " + button.getPoint());

		String color = eye().getCssValue("color");
		eye().click();
		assertEquals("text", password.getDomProperty("type"));
		assertEquals(TYPED, password.getDomProperty("value"));
		assertEquals("Hide password", eye().getDomAttribute("aria-label"));
		// always the same eye, in the accent color while the password is shown
		assertEquals(0, driver.findElements(By.cssSelector(".show-password__button line")).size());
		new Actions(driver).moveToElement(password).perform();
		assertTrue(!color.equals(eye().getCssValue("color")), color);

		// by keyboard: the eye comes after the field
		password.sendKeys(Keys.TAB);
		assertEquals(eye(), driver.switchTo().activeElement());
		driver.switchTo().activeElement().sendKeys(Keys.SPACE);
		assertEquals("password", password.getDomProperty("type"));
		assertEquals("Show password", eye().getDomAttribute("aria-label"));
		assertEquals(TYPED, password.getDomProperty("value"));
		assertNoErrors();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void sendingTheFormHidesThePasswordAgain(SeleniumBrowser browser)
	{
		open(browser, "sign-in");
		WebElement password = password();
		password.sendKeys(TYPED);
		eye().click();
		assertEquals("text", password.getDomProperty("type"));

		// a listener after the eye's notes what the field is when the form is sent, and keeps the
		// page
		((JavascriptExecutor)driver).executeScript("var input = arguments[0];" +
			"input.form.addEventListener('submit', function (event) {" +
			" event.preventDefault(); input.form.setAttribute('data-sent-as', input.type); });",
			password);
		WebElement form = password.findElement(By.xpath("ancestor::form"));
		form.findElement(By.cssSelector(".register__submit")).click();

		wait.until(d -> "password".equals(form.getDomAttribute("data-sent-as")));
		assertEquals("Show password", eye().getDomAttribute("aria-label"));
		assertNoErrors();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void bothPasswordsOfTheRegistrationHaveTheirEye(SeleniumBrowser browser)
	{
		open(browser, "register");
		wait.until(d -> d.findElements(By.cssSelector(".show-password__button")).size() == 2);

		driver.findElements(By.cssSelector(".show-password__button")).get(1).click();
		assertEquals("password", password().getDomProperty("type"));
		assertEquals("text", driver.findElement(By.name("repeatedPassword"))
			.getDomProperty("type"));
		assertNoErrors();
	}
}
