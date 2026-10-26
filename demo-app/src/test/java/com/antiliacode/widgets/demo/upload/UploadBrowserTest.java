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
package com.antiliacode.widgets.demo.upload;

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
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.antiliacode.widgets.demo.Cases;
import com.antiliacode.widgets.demo.Dropdowns;
import com.antiliacode.widgets.demo.JettyTestCase;
import com.antiliacode.widgets.demo.SeleniumBrowser;
import com.antiliacode.widgets.demo.Toasts;
import com.antiliacode.widgets.throttle.UploadSpeed;

/**
 * Drives {@link UploadPage} in Chrome and in Firefox, in deployment mode, so through the minified
 * script built from frontend/core: the files chosen in the pickers, the bars following the server
 * while it reads them slowly, a file breaking the limits held back until taken out of the list,
 * and the files dropped on the zone of a form. Runs with {@code -Pselenium}, see
 * {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class UploadBrowserTest extends JettyTestCase
{
	/** The files to upload, in target: a browser of a snap does not see the /tmp of the JVM. */
	private final Path folder = Path.of(System.getProperty("basedir", "."), "target",
		"upload-browser-test");

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

	private void open(SeleniumBrowser browser, UploadSpeed speed)
	{
		driver = browser.newDriver(1200, 1000);
		wait = new WebDriverWait(driver, Duration.ofSeconds(20), Duration.ofMillis(20));
		wait.ignoring(StaleElementReferenceException.class);
		driver.get("http://localhost:" + localPort + "/upload");
		wait.until(d -> d.findElements(By.cssSelector(".antilia-file-picker")).size() == 4);
		// the upload speed of the throttle above the cases
		String setting = ".upload-demo .antilia-throttle-setting:nth-of-type(2) ";
		Dropdowns.chooseByValue(driver, driver.findElement(By.cssSelector(setting + "select")),
			speed.name());
		wait.until(d -> d.findElement(By.cssSelector(setting + ".antilia-dropdown-value"))
			.getText().equals(speed == UploadSpeed.SLOW ? "64 kB/s" : "256 kB/s"));
		wait.until(d -> d.findElement(By.cssSelector(".antilia-throttle-state")).getText()
			.startsWith("Uploads "));
	}

	private String file(String name, int size) throws IOException
	{
		Path path = Files.createDirectories(folder).resolve(name).toAbsolutePath();
		try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "rw"))
		{
			file.setLength(size);
		}
		return path.toString();
	}

	/**
	 * @return the box of the bar in the case shown
	 */
	private WebElement progress()
	{
		return driver.findElements(By.cssSelector(".antilia-upload-progress")).stream()
			.filter(WebElement::isDisplayed).findFirst().orElse(null);
	}

	private String state()
	{
		WebElement progress = progress();
		return progress != null ? progress.getDomAttribute("data-state") : null;
	}

	private String status()
	{
		WebElement progress = progress();
		return progress != null ? progress.findElement(By.cssSelector(".antilia-upload-status"))
			.getText() : "";
	}

	private int percent()
	{
		String value = progress().findElement(By.tagName("progress")).getDomAttribute("value");
		return value != null ? Integer.parseInt(value) : -1;
	}

	private String received()
	{
		return driver.findElements(By.cssSelector(".upload-demo__table tbody tr")).stream()
			.map(WebElement::getText).reduce((a, b) -> a + "\n" + b).orElse("");
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void sendsTheFormViaAjaxFollowingTheServer(SeleniumBrowser browser) throws IOException
	{
		open(browser, UploadSpeed.QUARTER_MEGABYTE);
		driver.findElement(By.cssSelector("input[type=file][name=files]"))
			.sendKeys(file("report.pdf", 600 * 1024));
		WebElement summary = driver.findElement(By.cssSelector(".antilia-file-picker-summary"));
		wait.until(d -> summary.getText().equals("report.pdf (600 kB)"));

		driver.findElement(By.cssSelector("button[name=ajaxSubmit]")).click();
		wait.until(d -> "running".equals(state()) && percent() > 0 && percent() < 100);
		assertTrue(status().matches(".* of 600[.,]\\d kB · .*/s · .* left"), status());
		wait.until(d -> "done".equals(state()));
		assertEquals("Uploaded 600 kB", status());
		wait.until(d -> Toasts.latest(d).equals("Files received: 1."));
		assertEquals("report.pdf 600 kB application/pdf Form, via Ajax", received());
		wait.until(d -> summary.getText().equals("No file chosen"));
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void sendsNoFileBreakingTheLimitsUntilItIsTakenOut(SeleniumBrowser browser) throws IOException
	{
		open(browser, UploadSpeed.QUARTER_MEGABYTE);
		WebElement input = driver.findElement(By.cssSelector("input[type=file][name=files]"));
		input.sendKeys(file("small.txt", 1024));
		input.sendKeys(file("huge.bin", 11 * 1024 * 1024));
		WebElement picker = input.findElement(By.xpath("following-sibling::div[1]"));
		WebElement summary = picker.findElement(By.cssSelector(".antilia-file-picker-summary"));
		wait.until(d -> summary.getText().equals("2 files, 11 MB"));
		assertEquals("huge.bin: larger than 10 MB, the most for a file",
			picker.findElement(By.cssSelector(".antilia-file-picker-error")).getText());

		driver.findElement(By.cssSelector("button[name=ajaxSubmit]")).click();
		wait.until(d -> "failed".equals(state()));
		assertEquals("huge.bin: larger than 10 MB, the most for a file", status());
		assertEquals("", received());

		picker.findElements(By.cssSelector(".antilia-file-picker-remove")).get(1).click();
		wait.until(d -> summary.getText().equals("small.txt (1 kB)"));
		assertTrue(picker.findElements(By.cssSelector(".antilia-file-picker-error")).stream()
			.noneMatch(WebElement::isDisplayed));
		driver.findElement(By.cssSelector("button[name=ajaxSubmit]")).click();
		wait.until(d -> "done".equals(state()));
		wait.until(d -> received().equals("small.txt 1 kB text/plain Form, via Ajax"));
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void sendsTheImagesDroppedOnTheZoneOfTheForm(SeleniumBrowser browser)
	{
		open(browser, UploadSpeed.QUARTER_MEGABYTE);
		Cases.selectInWicket(wait, driver, "Drag and drop");

		// what a drop gives the picker: files the browser made, here in the page; the zone of the
		// form takes images only
		((JavascriptExecutor)driver).executeScript("const input = document.querySelector(" +
			"'.antilia-file-picker-drop-zone').previousElementSibling;" +
			"Wicket.Upload.FilePicker.of(input).drop([new File(['hi'], 'notes.txt', " +
			"{type: 'text/plain'}), new File([new Uint8Array(2048)], 'logo.svg', " +
			"{type: 'image/svg+xml'})]);");
		WebElement zone = driver.findElement(By.cssSelector(".antilia-file-picker-drop-zone"));
		wait.until(d -> zone.findElement(By.cssSelector(".antilia-file-picker-summary")).getText()
			.equals("logo.svg (2 kB) · Not accepted: notes.txt"));
		driver.findElement(By.cssSelector("button[name=send]")).click();
		wait.until(d -> received().equals("logo.svg 2 kB image/svg+xml Dropped into a form"));
	}
}
