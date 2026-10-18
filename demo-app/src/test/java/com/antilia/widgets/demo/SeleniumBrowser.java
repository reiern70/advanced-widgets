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
package com.antilia.widgets.demo;

import java.nio.file.Path;
import java.util.Map;

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
		WebDriver start(Path downloads)
		{
			ChromeOptions options = new ChromeOptions();
			if (isHeadless())
			{
				options.addArguments("--headless=new");
			}
			if (downloads != null)
			{
				options.setExperimentalOption("prefs", Map.of(
					"download.default_directory", downloads.toAbsolutePath().toString(),
					"download.prompt_for_download", false,
					"download.directory_upgrade", true));
			}
			return new ChromeDriver(options);
		}
	},

	/** Mozilla Firefox. */
	FIREFOX
	{
		@Override
		WebDriver start(Path downloads)
		{
			FirefoxOptions options = new FirefoxOptions();
			if (downloads != null)
			{
				options.addPreference("browser.download.folderList", 2);
				options.addPreference("browser.download.dir", downloads.toAbsolutePath().toString());
				options.addPreference("browser.download.useDownloadDir", true);
				options.addPreference("browser.download.always_ask_before_handling_new_types", false);
				options.addPreference("browser.download.manager.showWhenStarting", false);
			}
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
		return newDriver(width, height, null);
	}

	/**
	 * Starts the browser, saving what it downloads into a folder without asking. Firefox in a snap
	 * writes only below the home folder, outside of hidden folders: the folder is best in the
	 * target folder of the build.
	 *
	 * @param width
	 *            the width of the window
	 * @param height
	 *            the height of the window
	 * @param downloads
	 *            the folder the browser saves the files it downloads into, {@code null} for its own
	 * @return the driver of the started browser
	 */
	public WebDriver newDriver(int width, int height, Path downloads)
	{
		WebDriver driver = start(downloads);
		driver.manage().window().setSize(new Dimension(width, height));
		return driver;
	}

	abstract WebDriver start(Path downloads);

	private static boolean isHeadless()
	{
		return !"false".equals(System.getProperty("wicket.selenium.headless"));
	}
}
