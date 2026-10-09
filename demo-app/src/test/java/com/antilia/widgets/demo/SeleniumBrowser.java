/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

/**
 * The browsers the Selenium tests of the demos run in, headless unless
 * {@code -Dwicket.selenium.headless=false}. Selenium Manager resolves the drivers, and the
 * browsers too when none is installed; {@code -Dwicket.selenium.firefox=<path>} names the Firefox
 * binary to use instead. Where Firefox is a snap, its own geckodriver on the path cannot start
 * another Firefox; {@code -Dwebdriver.gecko.driver=<path>} names a standalone driver then.
 */
public enum SeleniumBrowser
{
	/** Google Chrome. */
	CHROME
	{
		@Override
		WebDriver start()
		{
			ChromeOptions options = new ChromeOptions();
			if (isHeadless())
			{
				options.addArguments("--headless=new");
			}
			return new ChromeDriver(options);
		}
	},

	/** Mozilla Firefox. */
	FIREFOX
	{
		@Override
		WebDriver start()
		{
			FirefoxOptions options = new FirefoxOptions();
			String binary = System.getProperty("wicket.selenium.firefox");
			if (binary != null && !binary.isBlank())
			{
				options.setBinary(binary);
			}
			if (isHeadless())
			{
				options.addArguments("-headless");
			}
			return new FirefoxDriver(options);
		}
	};

	/**
	 * Starts the browser.
	 *
	 * @param width
	 *            the width of the window
	 * @param height
	 *            the height of the window
	 * @return the driver of the started browser
	 */
	public WebDriver newDriver(int width, int height)
	{
		WebDriver driver = start();
		driver.manage().window().setSize(new Dimension(width, height));
		return driver;
	}

	abstract WebDriver start();

	private static boolean isHeadless()
	{
		return !"false".equals(System.getProperty("wicket.selenium.headless"));
	}
}
