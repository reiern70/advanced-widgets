/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase;

/**
 * Drives the carousel of the demos on the home page in Chrome and in Firefox: its arrows show the
 * slide after and the one before, a slide at a time in its window, and it moves on by itself once
 * the mouse left it, though a click left the focus on an arrow; in either look. Runs with
 * {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class DemoCarouselBrowserTest extends FrameworkAppsTestCase
{
	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void goesThroughTheDemosByHandAndByItself(SeleniumBrowser browser)
	{
		this.browser = browser;
		driver = browser.newDriver(1300, 1000);
		wait = new WebDriverWait(driver, Duration.ofSeconds(10), Duration.ofMillis(20));
		wait.ignoring(StaleElementReferenceException.class);
		driver.get("http://localhost:" + localPort + "/");

		WebElement carousel = wait.until(d -> d.findElement(By.cssSelector(
			".carousel.carousel--scripted")));
		int slides = Demos.all().size();
		assertEquals("1 / " + slides, count());
		assertShown(0);

		carousel.findElement(By.cssSelector("[data-carousel='next']")).click();
		wait.until(d -> count().equals("2 / " + slides));
		assertShown(1);
		carousel.findElement(By.cssSelector("[data-carousel='previous']")).click();
		wait.until(d -> count().equals("1 / " + slides));
		carousel.findElement(By.cssSelector("[data-carousel='previous']")).click();
		wait.until(d -> count().equals(slides + " / " + slides));
		assertShown(slides - 1);

		// by itself once the mouse left it, the focus still on the arrow clicked
		WebElement next = carousel.findElement(By.cssSelector("[data-carousel='next']"));
		new Actions(driver).moveToElement(next).click().perform();
		wait.until(d -> count().equals("1 / " + slides));
		new Actions(driver).moveToElement(driver.findElement(By.tagName("h1"))).perform();
		assertEquals(next, driver.switchTo().activeElement());
		new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> count().equals("2 / " +
			slides));
		assertShown(1);

		// in the Bootstrap look, chosen in the top bar
		((JavascriptExecutor)driver).executeScript("var s = arguments[0]; s.selectedIndex = " +
			"Array.from(s.options).findIndex(o => o.text === 'Bootstrap'); s.dispatchEvent(" +
			"new Event('change', {bubbles: true}));",
			driver.findElement(By.cssSelector(".topbar__setting select[name$='look']")));
		wait.until(d -> d.findElement(By.tagName("body")).getDomAttribute("class")
			.contains("antilia-look-bootstrap"));
		carousel = wait.until(d -> d.findElement(By.cssSelector(".carousel.carousel--scripted")));
		assertEquals("1 / " + slides, count());
		carousel.findElement(By.cssSelector("[data-carousel='next']")).click();
		wait.until(d -> count().equals("2 / " + slides));
		assertShown(1);
		assertNoErrors();
	}

	/**
	 * @return what the counter of the carousel says
	 */
	private String count()
	{
		return driver.findElement(By.cssSelector(".carousel__count")).getText();
	}

	/**
	 * Checks that the slide of the index fills the window of the carousel, the others out of it.
	 */
	private void assertShown(int index)
	{
		// once the track slid there
		wait.until(d -> (Boolean)((JavascriptExecutor)d).executeScript("var viewport = document" +
			".querySelector('.carousel__viewport').getBoundingClientRect(); var slide = document" +
			".querySelectorAll('.carousel__slide')[arguments[0]].getBoundingClientRect(); return " +
			"Math.abs(slide.left - viewport.left) < 2 && Math.abs(slide.width - viewport.width) < 2;",
			index));
		WebElement slide = driver.findElements(By.cssSelector(".carousel__slide")).get(index);
		assertEquals("false", slide.getDomAttribute("aria-hidden"));
		assertTrue(slide.isDisplayed());
	}
}
