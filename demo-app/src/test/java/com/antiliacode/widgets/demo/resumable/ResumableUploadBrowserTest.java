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
package com.antiliacode.widgets.demo.resumable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.antiliacode.widgets.demo.Dropdowns;
import com.antiliacode.widgets.demo.JettyTestCase;
import com.antiliacode.widgets.demo.SeleniumBrowser;
import com.antiliacode.widgets.throttle.UploadSpeed;

/**
 * Drives {@link ResumableUploadPage} in Chrome and in Firefox, in deployment mode, so through the
 * minified script built from frontend/core: a file sent in chunks over tus to the server reading
 * them slowly, paused and resumed, and a file paused, left with a reload of the page and chosen
 * again, going on where the server stopped; the server tells the SHA-256 of what it received
 * whole. Runs with {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class ResumableUploadBrowserTest extends JettyTestCase
{
	/** The SHA-256 of the file of the tests, 3 MB of zeros. */
	private static final String SHA256 =
		"bbd05cf6097ac9b1f89ea29d2542c1b7b67ee46848393895f5a9e43fa1f621e5";

	/** The files to upload, in target: a browser of a snap does not see the /tmp of the JVM. */
	private final Path folder = Path.of(System.getProperty("basedir", "."), "target",
		"resumable-upload-browser-test");

	private WebDriver driver;

	private WebDriverWait wait;

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
		driver = browser.newDriver(1200, 1000);
		wait = new WebDriverWait(driver, Duration.ofSeconds(30), Duration.ofMillis(20));
		wait.ignoring(StaleElementReferenceException.class);
		driver.get("http://localhost:" + localPort + "/resumable-upload");
		wait.until(d -> d.findElements(By.cssSelector(".resumable-demo__uploads " +
			".antilia-file-picker")).size() == 1);
		// the server reads the chunks at 256 kB/s
		String setting = ".resumable-demo .antilia-throttle-setting:nth-of-type(2) ";
		Dropdowns.chooseByValue(driver, driver.findElement(By.cssSelector(setting + "select")),
			UploadSpeed.QUARTER_MEGABYTE.name());
		wait.until(d -> d.findElement(By.cssSelector(setting + ".antilia-dropdown-value"))
			.getText().equals("256 kB/s"));
	}

	private String file(String name) throws IOException
	{
		Path path = Files.createDirectories(folder).resolve(name).toAbsolutePath();
		try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "rw"))
		{
			file.setLength(3 * 1024 * 1024);
		}
		return path.toString();
	}

	private void choose(String path)
	{
		driver.findElement(By.cssSelector(".resumable-demo__uploads input[type=file]"))
			.sendKeys(path);
	}

	private WebElement row()
	{
		return driver.findElement(By.cssSelector(".resumable-demo__uploads " +
			".antilia-resumable-upload-item"));
	}

	private String state()
	{
		return row().getDomAttribute("data-state");
	}

	private String status()
	{
		return row().findElement(By.cssSelector(".antilia-upload-status")).getText();
	}

	private int percent()
	{
		String value = row().findElement(By.tagName("progress")).getDomAttribute("value");
		return value != null ? Integer.parseInt(value) : -1;
	}

	private String received()
	{
		return driver.findElements(By.cssSelector(".resumable-demo__table tbody tr")).stream()
			.map(WebElement::getText).reduce((a, b) -> a + "\n" + b).orElse("");
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void pausesAndResumesAFileSentInChunks(SeleniumBrowser browser) throws IOException
	{
		open(browser);
		choose(file("film.mp4"));
		wait.until(d -> "uploading".equals(state()) && percent() >= 20 &&
			status().endsWith(" left"));
		assertTrue(status().matches(".* of 3 MB · .*/s · .* left"), status());

		row().findElement(By.cssSelector(".antilia-resumable-upload-pause")).click();
		// at what the server holds, as it tells
		wait.until(d -> "paused".equals(state()) && status().startsWith("Paused at ") &&
			percent() > 0);
		int paused = percent();
		assertTrue(paused > 0 && paused < 100, "paused at " + paused);

		row().findElement(By.cssSelector(".antilia-resumable-upload-play")).click();
		wait.until(d -> "done".equals(state()));
		assertEquals("Uploaded 3 MB", status());
		wait.until(d -> !received().isEmpty());
		assertEquals("film.mp4 3 MB " + SHA256, received());
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void goesOnAfterAReloadWithTheSameFileChosenAgain(SeleniumBrowser browser) throws IOException
	{
		open(browser);
		String path = file("backup.zip");
		choose(path);
		wait.until(d -> "uploading".equals(state()) && percent() >= 20 &&
			status().endsWith(" left"));
		// paused, so that the guard lets the page go
		row().findElement(By.cssSelector(".antilia-resumable-upload-pause")).click();
		wait.until(d -> "paused".equals(state()) && percent() > 0);

		driver.navigate().refresh();
		wait.until(d -> "interrupted".equals(state()) && status().startsWith("Stopped at ") &&
			percent() > 0);
		assertTrue(status().endsWith("of 3 MB: choose the file again to go on"), status());

		choose(path);
		wait.until(d -> "done".equals(state()));
		wait.until(d -> !received().isEmpty());
		assertEquals("backup.zip 3 MB " + SHA256, received());
	}
}
