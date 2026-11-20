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
package com.antiliacode.widgets.demo.ckeditor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.antiliacode.widgets.demo.SeleniumBrowser;
import com.antiliacode.widgets.demo.Toasts;
import com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase;

/**
 * Drives {@link CKEditorPage} in Chrome and in Firefox, in deployment mode, under the demo's strict
 * Content Security Policy, in Wicket and in the React and Svelte apps: CKEditor 5 started with the
 * license key the application gives, formatting from its toolbar, with the mouse and from the
 * keyboard, superscript and subscript among it, posting the form, a paste of unsafe HTML, the notes in Markdown, reading only, and the
 * text and the selection the server keeps, through a switch of the tabs and Back. Chrome's console
 * shows no error, so no violation of the policy. Runs with {@code -Pselenium}, see
 * {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class CKEditorBrowserTest extends FrameworkAppsTestCase
{
	/** A PNG of one pixel. */
	private static final String PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk" +
		"+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

	/** HTML from anywhere, to paste: a script, handlers and a link to javascript:. */
	private static final String UNSAFE_PASTE = "<p>Pasted <b>bold</b> <img src=\"favicon.svg\" " +
		"onload=\"window.pwned = 1\"><a href=\"javascript:window.pwned = 2\">a link</a>" +
		"<script>window.pwned = 3</script></p><p onclick=\"window.pwned = 4\">Styled</p>";

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void startsCKEditorWithTheLicenseKeyItIsGiven(SeleniumBrowser browser, String framework)
	{
		openEditor(browser, framework);
		assertEquals("GPL", script("return arguments[0].ckeditorInstance.config.get('licenseKey');",
			content()));
		// CKEditor tells of its help after the label
		assertTrue(content().getDomAttribute("aria-label").startsWith("Article"),
			content().getDomAttribute("aria-label"));
		assertEquals("textbox", content().getDomAttribute("role"));
		// the page says under which license CKEditor runs, and the apps which CKEditor they give
		assertTrue(driver.findElement(By.cssSelector("#framework-panel-wicket " +
			".ckeditor-demo__license")).getDomProperty("textContent").contains("under the GPL 2"));
		assertTrue(app().findElement(By.cssSelector(".ckeditor-demo__license")).getDomProperty(
			"textContent").contains("license key"));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void formatsTheTextFromTheToolbarAndTheKeys(SeleniumBrowser browser, String framework)
	{
		openEditor(browser, framework);
		selectWord("oystercatchers");
		tool("Bold").click();
		wait.until(d -> !content().findElements(By.xpath(".//strong[contains(., " +
			"'oystercatchers')]")).isEmpty());
		assertEquals("true", tool("Bold").getDomAttribute("aria-pressed"));
		content().sendKeys(Keys.chord(Keys.CONTROL, "u"));
		wait.until(d -> !content().findElements(By.xpath(".//u[contains(., 'oystercatchers')]"))
			.isEmpty());
		wait.until(d -> status().contains("changes not saved"));
		assertEquals("true", editor().getDomAttribute("data-modified"));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void raisesAndLowersTheText(SeleniumBrowser browser, String framework)
	{
		openEditor(browser, framework);
		// the article has some already: H₂S, km²
		assertFalse(content().findElements(By.xpath(".//sub[. = '2']")).isEmpty());
		assertFalse(content().findElements(By.xpath(".//sup[. = '2']")).isEmpty());

		selectWord("flats");
		tool("Superscript").click();
		wait.until(d -> !content().findElements(By.xpath(".//sup[. = 'flats']")).isEmpty());
		assertEquals("true", tool("Superscript").getDomAttribute("aria-pressed"));
		// subscript takes superscript away, in one step of undo
		tool("Subscript").click();
		wait.until(d -> !content().findElements(By.xpath(".//sub[. = 'flats']")).isEmpty());
		assertTrue(content().findElements(By.xpath(".//sup[contains(., 'flats')]")).isEmpty());
		assertEquals("false", tool("Superscript").getDomAttribute("aria-pressed"));
		content().sendKeys(Keys.chord(Keys.CONTROL, "z"));
		wait.until(d -> !content().findElements(By.xpath(".//sup[. = 'flats']")).isEmpty());
		assertTrue(content().findElements(By.xpath(".//sub[contains(., 'flats')]")).isEmpty());
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void usesTheToolbarFromTheKeyboard(SeleniumBrowser browser, String framework)
	{
		openEditor(browser, framework);
		selectWord("tide");
		// Alt+F10 goes to CKEditor's toolbar, the arrow keys along it, Enter uses a tool
		content().sendKeys(Keys.chord(Keys.ALT, Keys.F10));
		wait.until(d -> toolbar().findElements(By.cssSelector("*")).contains(
			d.switchTo().activeElement()));
		for (int tries = 0; tries < 20 && !tool("Italic").equals(driver.switchTo()
			.activeElement()); tries++)
		{
			driver.switchTo().activeElement().sendKeys(Keys.ARROW_RIGHT);
		}
		assertEquals(tool("Italic"), driver.switchTo().activeElement());
		driver.switchTo().activeElement().sendKeys(Keys.ENTER);
		wait.until(d -> !content().findElements(By.xpath(".//i[contains(., 'tide')]")).isEmpty());
		// Escape goes back to the text, where it was
		driver.switchTo().activeElement().sendKeys(Keys.ESCAPE);
		wait.until(d -> content().equals(d.switchTo().activeElement()));
		assertNoErrors();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void postsTheFormAndShowsTheHtmlCleaned(SeleniumBrowser browser)
	{
		openEditor(browser, "wicket");
		clickAtEnd(content().findElement(By.cssSelector("p")));
		new Actions(driver).sendKeys(" Posted by the form.").perform();
		wait.until(d -> status().contains("changes not saved"));
		app().findElement(By.cssSelector(".rich-text-demo__actions button[type=submit]")).click();
		wait.until(d -> view().getText().contains("Posted by the form."));
		wait.until(d -> status().contains("all saved"));
		assertEquals("false", editor().getDomAttribute("data-modified"));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void keepsWhatAPasteBringsAndDropsTheRest(SeleniumBrowser browser, String framework)
	{
		openEditor(browser, framework);
		clickAtEnd(content().findElement(By.cssSelector("p")));
		paste("<p>Pasted <b>bold</b> <img src=\"favicon.svg\" width=\"9\" height=\"9\" " +
			"onload=\"window.pwned = 1\"><a href=\"javascript:window.pwned = 2\">a link</a>" +
			"<script>window.pwned = 3</script></p><p style=\"position: fixed\" " +
			"onclick=\"window.pwned = 4\">Styled</p>");
		wait.until(d -> content().getText().contains("Styled"));

		assertNull(script("return window.pwned;"));
		assertEquals(0L, script("return Array.from(arguments[0].querySelectorAll('*')).filter(" +
			"(e) => e.tagName === 'SCRIPT' || Array.from(e.attributes).some((a) => " +
			"a.name.startsWith('on'))).length;", content()));
		assertTrue(content().getText().contains("Pasted bold"));
		if ("wicket".equals(framework))
		{
			// the server cleans what it keeps, and the page shows it so
			content().sendKeys(Keys.chord(Keys.CONTROL, "s"));
			wait.until(d -> view().getText().contains("Styled"));
			assertNull(script("return window.pwned;"));
			assertTrue(view().findElements(By.cssSelector(":is([onload], [onclick], [style], " +
				"[width], script, a[href^='javascript'])")).isEmpty());
		}
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void keepsTheNotesAsMarkdown(SeleniumBrowser browser, String framework)
	{
		openEditor(browser, framework);
		selectCase("Notes in Markdown");
		WebElement notes = app().findElements(By.cssSelector(
			".rich-text-demo__editor .ck-editor__editable")).stream().filter(WebElement::isDisplayed)
			.findFirst().orElseThrow();
		clickAtEnd(notes.findElement(By.cssSelector("h1")));
		new Actions(driver).sendKeys(" for two").perform();
		wait.until(d -> app().findElements(By.cssSelector(".rich-text-demo__saved code")).stream()
			.filter(WebElement::isDisplayed).anyMatch(code -> code.getText()
				.startsWith("# Packing list for two")));
		// Markdown has no underline: no tool for it
		assertTrue(notes.findElement(By.xpath("ancestor::div[contains(@class, " +
			"'antilia-ck-editor')]")).findElements(By.xpath(".//button[.//span[contains(@class, " +
			"'ck-button__label') and text()='Underline']]")).isEmpty());
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void readsOnlyWithoutItsToolbar(SeleniumBrowser browser, String framework)
	{
		openEditor(browser, framework);
		checkBox("Read only").click();
		wait.until(d -> editor().getDomAttribute("class").contains("antilia-ck-editor-readonly"));
		wait.until(d -> !toolbar().isDisplayed());
		wait.until(d -> "false".equals(content().getDomAttribute("contenteditable")));
		String before = html();
		clickAtEnd(content().findElement(By.cssSelector("p")));
		new Actions(driver).sendKeys("x").perform();
		assertEquals(before, html());
		assertNoErrors();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void hearsOfTheTextSelectedAndOfTheHeadingsAsTheTiptapEditorDoes(SeleniumBrowser browser)
	{
		openEditor(browser, "wicket");
		// the built-in type of events and the page's own, which its script registered
		@SuppressWarnings("unchecked")
		List<String> types = (List<String>)script("return Wicket.RichTextEvents.types();");
		assertTrue(types.containsAll(List.of("antilia-rich-text-editor:selected-text",
			"rich-text-demo:outline")), types.toString());

		// the text selected, once the user paused
		selectWord("mud");
		wait.until(d -> status(".rich-text-demo__selection").equals(
			"The server heard you selected “mud”."));

		// the headings, as one of them changes
		clickAtEnd(content().findElement(By.cssSelector("h3")));
		content().sendKeys(" here");
		wait.until(d -> status(".rich-text-demo__outline").startsWith(
			"The outline the server heard: Field notes from the coast · What we counted here · "));
		wait.until(d -> status(".rich-text-demo__selection").equals(
			"The server heard nothing is selected."));
		assertNoErrors();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void theServerKeepsTheSelectionThroughTheTabsAndBack(SeleniumBrowser browser)
	{
		openEditor(browser, "wicket");
		selectWord("mud");
		wait.until(d -> known().contains("selected from"));
		String selection = known().replaceAll(".*(selected from \\d+ to \\d+).*", "$1");
		Object state = selection();

		// another tab and back: the editor, hidden meanwhile, keeps it
		tab("react").click();
		wait.until(d -> !d.findElements(By.cssSelector("[data-framework-app='react'] " +
			".ck-editor__editable")).isEmpty());
		tab("wicket").click();
		wait.until(d -> content().isDisplayed());
		assertEquals(state, selection());

		// another page and Back: the session gives the selection back
		script("return new Promise(function (done) { setTimeout(done, 1500); });");
		driver.findElement(By.cssSelector("a[href*='rich-text-editor'], .demo__nav a")).click();
		wait.until(d -> !d.getCurrentUrl().contains("/ckeditor"));
		driver.navigate().back();
		waitForTheEditor();
		assertTrue(known().contains(selection), known());
		assertEquals(state, selection());
		assertNoErrors();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void hidesTheHtmlUntilTheEditorIsMadeInTheRoomItTakes(SeleniumBrowser browser)
	{
		openEditor(browser, "wicket");
		assertEquals("antilia-ck-editor-field", field().getDomAttribute("class"));
		@SuppressWarnings("unchecked")
		Map<String, Object> waiting = (Map<String, Object>)script("var area = " +
			"document.createElement('textarea'); area.className = 'antilia-ck-editor-loading';" +
			"area.value = '<h2>Not to be seen</h2>'; arguments[0].after(area);" +
			"return { visibility: getComputedStyle(area).visibility, height: area.offsetHeight," +
			" editor: arguments[0].offsetHeight };", editor());
		assertEquals("hidden", waiting.get("visibility"));
		assertEquals(((Number)waiting.get("editor")).doubleValue(),
			((Number)waiting.get("height")).doubleValue(), 1);
		assertNoErrors();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void tellsWhatAPasteLostAndWhatTheServerRemovedOrRejected(SeleniumBrowser browser)
	{
		openEditor(browser, "wicket");
		selectCase("The cleaning on the server");
		wait.until(d -> !modes().findElements(By.cssSelector(".ck-editor__editable")).isEmpty());
		assertEquals("status", notice().getDomAttribute("role"));
		assertEquals("", notice().getText());

		// a paste of unsafe HTML: the editor says what it lost, and the server hears of it
		modes().findElement(By.cssSelector(".ck-editor__editable")).click();
		paste(UNSAFE_PASTE);
		wait.until(d -> notice().getText().startsWith("Part of the paste was left out: scripts"));
		wait.until(d -> Toasts.latest(d).startsWith("The editor left out of the paste: "));
		assertTrue(Toasts.latest(driver).contains("script"), Toasts.latest(driver));
		assertTrue(Toasts.latest(driver).contains("onclick"), Toasts.latest(driver));
		assertNull(script("return window.pwned;"));
		// gone with the next change
		modes().findElement(By.cssSelector(".ck-editor__editable")).sendKeys("!");
		wait.until(d -> notice().getText().isEmpty());

		// the HTML from anywhere sent as the text of the editor: the server tells what it removed
		WebElement sendUnsafe = modes().findElement(By.xpath(".//button[contains(., " +
			"'Send the HTML from anywhere')]"));
		sendUnsafe.click();
		wait.until(d -> Toasts.latest(d).startsWith("The server removed what the editor does " +
			"not allow: "));
		wait.until(d -> kept().contains("A paragraph styled to cover the page"));
		String kept = kept();
		assertFalse(kept.contains("<script") || kept.contains("onclick") || kept.contains("style="),
			kept);

		// or rejects the text, which it then does not keep
		modes().findElement(By.xpath(".//label[normalize-space() = 'Reject the text']")).click();
		modes().findElement(By.xpath(".//button[contains(., 'Send the HTML from anywhere')]"))
			.click();
		wait.until(d -> Toasts.latest(d).startsWith("The text holds what the editor does not " +
			"allow: "));
		assertEquals(kept, kept());
		assertNull(script("return window.pwned;"));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void startsCKEditorAgainWithItsTextAndSelectionWhenACommandThrows(SeleniumBrowser browser,
		String framework)
	{
		openEditor(browser, framework);
		selectWord("oystercatchers");
		// CKEditor's ids of the items of lists are new each time it reads the text
		String before = html().replaceAll(" data-list-item-id=\"[^\"]*\"", "");
		// the Wicket editor of the hidden tab, which CKEditor gives the error as one of too (#144)
		WebElement wicket = driver.findElement(By.cssSelector("#framework-panel-wicket " +
			".rich-text-demo__editor .antilia-ck-editor"));
		if (!"wicket".equals(framework))
		{
			script("var editor = arguments[0], content = editor.querySelector('.ck-editor__editable');" +
				"content.ckeditorInstance.antiliaTestKept = true; window.antiliaTestErrors = 0;" +
				"editor.addEventListener('antilia-ck-editor:error', function () {" +
				" window.antiliaTestErrors++; });", wicket);
		}
		// Bold throws, as a plugin failing would: the error escapes CKEditor, which tells of it
		script("var editor = arguments[0].ckeditorInstance; editor.antiliaTestCrashed = true;" +
			"editor.commands.get('bold').on('execute', function () {" +
			" throw new Error('A test of the editor failing'); }, { priority: 'highest' });",
			content());
		tool("Bold").click();
		wait.until(d -> Boolean.FALSE.equals(script("var content = arguments[0]" +
			".querySelector('.ck-editor__editable'); return !!(content && content.ckeditorInstance" +
			" && content.ckeditorInstance.antiliaTestCrashed);", editor())));
		waitForTheEditor();

		// CKEditor started again with the text and the selection, and says so below the text
		WebElement line = editor().findElement(By.cssSelector(".antilia-ck-editor-error"));
		wait.until(d -> line.getText().equals(
			"Something went wrong in the editor. It started again, with your text."));
		assertEquals("status", line.getDomAttribute("role"));
		assertEquals(before, html().replaceAll(" data-list-item-id=\"[^\"]*\"", ""));
		wait.until(d -> "oystercatchers".equals(script("return getSelection().toString();")));
		if ("wicket".equals(framework))
		{
			// CKEditor's error wrapping the one thrown
			wait.until(d -> Toasts.latest(d).startsWith("CKEditor ran into an error, which the " +
				"server heard of and logged: unexpected-error"));
			assertTrue(Toasts.latest(driver).contains("Original error: Error: A test of the editor " +
				"failing"), Toasts.latest(driver));
		}
		else
		{
			// the Wicket editor neither told of the error nor started its CKEditor again
			assertEquals(0L, script("return window.antiliaTestErrors;"));
			assertEquals(Boolean.TRUE, script("var content = arguments[0]" +
				".querySelector('.ck-editor__editable'); return !!(content &&" +
				" content.ckeditorInstance && content.ckeditorInstance.antiliaTestKept);", wicket));
			assertTrue(wicket.findElements(By.cssSelector(".antilia-ck-editor-error")).isEmpty());
		}

		// the new CKEditor works, and the line goes with the next change
		tool("Bold").click();
		wait.until(d -> !content().findElements(By.xpath(".//strong[contains(., " +
			"'oystercatchers')]")).isEmpty());
		wait.until(d -> editor().findElements(By.cssSelector(".antilia-ck-editor-error"))
			.isEmpty());
		assertNoPolicyViolations();
	}

	/**
	 * Fails on a violation of the Content Security Policy in Chrome's console: the errors of a test
	 * making CKEditor fail are there on purpose.
	 */
	private void assertNoPolicyViolations()
	{
		if (browser != SeleniumBrowser.CHROME)
		{
			return;
		}
		List<String> violations = driver.manage().logs().get("browser").getAll().stream()
			.map(entry -> entry.getMessage())
			.filter(message -> message.contains("Content Security Policy") ||
				message.contains("Content-Security-Policy"))
			.toList();
		assertEquals(List.of(), violations, "violations of the policy in the console");
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void uploadsAnImageChosenDroppedOrPasted(SeleniumBrowser browser, String framework)
		throws IOException
	{
		openEditor(browser, framework);
		// in the build's folder: Firefox, a snap, cannot read the temporary folder of the system
		Path image = Files.createTempFile(Path.of("target").toAbsolutePath(), "bird", ".png");
		try
		{
			Files.write(image, Base64.getDecoder().decode(PNG));
			// chosen by the button of the toolbar, CKEditor's
			clickAtEnd(content().findElement(By.cssSelector("p")));
			toolbar().findElement(By.cssSelector("input[type=file]")).sendKeys(image.toAbsolutePath()
				.toString());
			wait.until(d -> uploaded().size() == 1);
			wait.until(d -> Boolean.TRUE.equals(script("return arguments[0].complete && " +
				"arguments[0].naturalWidth === 1;", uploaded().get(0))));
		}
		finally
		{
			Files.deleteIfExists(image);
		}

		// dropped into the text, and pasted as a screenshot is
		transfer("drop", "dropped.png", "image/png", PNG, 0);
		wait.until(d -> uploaded().size() == 2);
		// the image dropped is selected, which a paste would replace: the cursor in the text
		clickAtEnd(content().findElement(By.cssSelector("p")));
		transfer("paste", "pasted.png", "image/png", PNG, 0);
		wait.until(d -> uploaded().size() == 3);
		// no bar of the progress left, which grew by classes
		wait.until(d -> content().findElements(By.cssSelector(".ck-progress-bar")).isEmpty());

		// a file of a type neither of the images nor of the files attached, and one too large:
		// refused before uploading, saying why
		transfer("drop", "tool.exe", "application/x-msdownload", Base64.getEncoder()
			.encodeToString("MZ".getBytes(StandardCharsets.UTF_8)), 0);
		wait.until(d -> message().startsWith("tool.exe is not a file of a type the editor " +
			"takes: PDF"));
		assertEquals("alert", editor().findElement(By.cssSelector(".antilia-ck-editor-message"))
			.getDomAttribute("role"));
		transfer("drop", "large.png", "image/png", PNG, 3 * 1024 * 1024);
		wait.until(d -> message().equals("large.png is larger than 2 MB."));
		assertEquals(3, uploaded().size());

		if ("wicket".equals(framework))
		{
			// the server keeps the addresses through its cleaning, and serves the images
			content().sendKeys(Keys.chord(Keys.CONTROL, "s"));
			wait.until(d -> view().findElements(By.cssSelector("img[src*='/rich-text-images/']"))
				.size() == 3);
		}
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void checksTheImagesChosenAndDeletesThoseTakenOutOnceSaved(SeleniumBrowser browser,
		String framework) throws IOException
	{
		openEditor(browser, framework);
		clickAtEnd(content().findElement(By.cssSelector("p")));
		// six images and a file of another type, chosen in CKEditor's dialog of files ("All
		// files"): five at most at a time, and the file told of, which CKEditor leaves out
		List<Path> files = new java.util.ArrayList<>();
		try
		{
			for (int index = 1; index <= 6; index++)
			{
				files.add(file("bird" + index + "-", ".png", Base64.getDecoder().decode(PNG)));
			}
			files.add(file("notes", ".txt", "notes".getBytes(StandardCharsets.UTF_8)));
			toolbar().findElement(By.cssSelector("input[type=file]")).sendKeys(String.join("\n",
				files.stream().map(each -> each.toAbsolutePath().toString()).toList()));
			wait.until(d -> uploaded().size() == 5);
		}
		finally
		{
			for (Path each : files)
			{
				Files.deleteIfExists(each);
			}
		}
		wait.until(d -> message().contains("5 images at most at a time: 1 left out."));
		assertTrue(message().startsWith("notes") && message().contains(".txt is not an image of " +
			"a type the editor takes: PNG"), message());

		// the list of the images takes one out of the text; saved, the page deletes it
		String first = uploaded().get(0).getDomProperty("src");
		tool("Images in the text").click();
		WebElement delete = wait.until(d -> toolbar().findElements(By.cssSelector(
			".antilia-ck-editor-panel-images .antilia-ck-editor-panel-delete")).stream()
			.findFirst().orElse(null));
		assertEquals(5, toolbar().findElements(By.cssSelector(
			".antilia-ck-editor-panel-images img.antilia-rich-text-images-thumbnail")).size());
		assertTrue(delete.getDomAttribute("aria-label").startsWith("Delete "));
		delete.click();
		wait.until(d -> uploaded().size() == 4);
		assertEquals(200L, served(first));
		content().sendKeys(Keys.chord(Keys.CONTROL, "s"));
		wait.until(d -> Long.valueOf(404).equals(served(first)));
		assertEquals(200L, served(uploaded().get(0).getDomProperty("src")));
		assertNoPolicyViolations();
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void scalesDownAPictureTooLargeOnceTicked(SeleniumBrowser browser, String framework)
	{
		openEditor(browser, framework);
		clickAtEnd(content().findElement(By.cssSelector("p")));
		// a picture wider than the page takes, dropped into the text
		script("var content = arguments[0], target = content.querySelector('p');" +
			"var canvas = document.createElement('canvas'); canvas.width = 3000; canvas.height = 20;" +
			"canvas.getContext('2d').fillRect(0, 0, 3000, 20);" +
			"canvas.toBlob((blob) => { var data = new DataTransfer();" +
			"  data.items.add(new File([blob], 'wide.png', { type: 'image/png' }));" +
			"  var rect = target.getBoundingClientRect();" +
			"  var at = { clientX: rect.left + 4, clientY: rect.top + rect.height / 2 };" +
			"  for (var type of ['dragenter', 'dragover', 'drop']) {" +
			"    target.dispatchEvent(new DragEvent(type, Object.assign({ bubbles: true," +
			"      cancelable: true, dataTransfer: data }, at)));" +
			"  }" +
			"}, 'image/png');", content());
		WebElement scale = wait.until(d -> d.findElements(By.cssSelector(
			".antilia-rich-text-scale-dialog")).stream().findFirst().orElse(null));
		assertEquals("Scale wide.png down to 2400 × 16 (3000 × 20)", scale.findElement(
			By.cssSelector("label")).getDomProperty("textContent").trim());
		scale.findElement(By.cssSelector("input[type=checkbox]")).click();
		scale.findElement(By.cssSelector(".modal-dialog-footer button")).click();
		wait.until(d -> uploaded().size() == 1);
		wait.until(d -> Long.valueOf(2400).equals(script("return arguments[0].naturalWidth;",
			uploaded().get(0))));
		assertNoPolicyViolations();
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void uploadsInTheDialogAndInsertsAnImageKeptPermanentlyFromTheStore(SeleniumBrowser browser,
		String framework)
	{
		openEditor(browser, framework);
		checkBox("Upload images in a dialog").click();
		// CKEditor made anew, the dialog in place of its own button and its field of files
		wait.until(d -> !toolbar().findElements(By.cssSelector(".ck-toolbar")).isEmpty() &&
			toolbar().findElements(By.cssSelector("input[type=file]")).isEmpty());
		waitForTheEditor();
		clickAtEnd(content().findElement(By.cssSelector("p")));
		tool("Upload images…").click();
		WebElement dialog = wait.until(d -> d.findElements(By.cssSelector(
			".antilia-rich-text-upload-dialog")).stream().findFirst().orElse(null));
		assertEquals("Upload images", dialog.findElement(By.cssSelector(".modal-dialog-title"))
			.getText());
		String name = "kept-" + framework + "-" + browser.name().toLowerCase() + ".png";
		dropFiles(dialog.findElement(By.cssSelector(".antilia-rich-text-dropzone")), name,
			"image/png", PNG, "other.png", "image/png", PNG);
		wait.until(d -> dialog.findElements(By.cssSelector(".antilia-rich-text-upload-done"))
			.size() == 2);
		// the first kept permanently, the second taken out of the list
		WebElement keep = dialog.findElement(By.cssSelector(
			".antilia-rich-text-upload-done .antilia-rich-text-upload-permanent"));
		assertEquals("Store permanently", keep.getText().trim());
		keep.findElement(By.cssSelector("input")).click();
		dialog.findElements(By.cssSelector(".antilia-rich-text-upload-remove")).get(1).click();
		dialog.findElement(By.cssSelector(".modal-dialog-footer button")).click();
		wait.until(d -> d.findElements(By.cssSelector(".antilia-rich-text-upload-dialog"))
			.isEmpty());
		wait.until(d -> uploaded().size() == 1);
		String src = uploaded().get(0).getDomProperty("src");

		// taken out of the text and saved: kept all the same
		tool("Images in the text").click();
		wait.until(d -> toolbar().findElements(By.cssSelector(
			".antilia-ck-editor-panel-images .antilia-ck-editor-panel-delete")).stream()
			.findFirst().orElse(null)).click();
		wait.until(d -> uploaded().isEmpty());
		content().sendKeys(Keys.chord(Keys.CONTROL, "s"));

		// found by its name, and inserted
		clickAtEnd(content().findElement(By.cssSelector("p")));
		tool("Insert image from store").click();
		WebElement field = wait.until(d -> toolbar().findElements(By.cssSelector(
			".antilia-ck-editor-panel-storedImage input[role=combobox]")).stream().findFirst()
			.orElse(null));
		wait.until(d -> field.equals(d.switchTo().activeElement()));
		field.sendKeys(name);
		wait.until(d -> toolbar().findElements(By.cssSelector(
			".antilia-ck-editor-panel-storedImage [role=option]")).stream()
			.anyMatch(option -> name.equals(option.getText().trim())));
		field.sendKeys(Keys.ENTER);
		wait.until(d -> uploaded().size() == 1);
		assertEquals(src, uploaded().get(0).getDomProperty("src"));
		assertEquals(200L, served(src));
		assertNoPolicyViolations();
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void attachesFilesAndDeletesThoseTakenOutOnceSaved(SeleniumBrowser browser, String framework)
		throws IOException
	{
		openEditor(browser, framework);
		clickAtEnd(content().findElement(By.cssSelector("p")));
		Path pdf = file("report", ".pdf", "%PDF-1.7\n%%EOF".getBytes(StandardCharsets.US_ASCII));
		try
		{
			// the field of the tool Attach a file, hidden
			editor().findElement(By.cssSelector("input.antilia-ck-editor-attach-picker")).sendKeys(
				pdf.toAbsolutePath().toString());
			wait.until(d -> !attached().isEmpty());
		}
		finally
		{
			Files.deleteIfExists(pdf);
		}
		WebElement file = attached().get(0);
		assertEquals(pdf.getFileName().toString(), file.getText().trim());
		assertTrue(file.getDomAttribute("class").contains("antilia-attachment-pdf"));
		String href = file.getDomProperty("href");
		assertTrue(html().contains("data-size=\"14 B\""), html());

		// a file dropped of a type the editor takes neither as an image nor as a file
		transfer("drop", "tool.exe", "application/x-msdownload", Base64.getEncoder()
			.encodeToString("MZ".getBytes(StandardCharsets.UTF_8)), 0);
		wait.until(d -> message().startsWith("tool.exe is not a file of a type the editor " +
			"takes: PDF"));

		// the list of the files takes it out; saved, the page deletes it
		tool("Files of the text").click();
		WebElement line = wait.until(d -> toolbar().findElements(By.cssSelector(
			".antilia-ck-editor-panel-attachments .antilia-ck-editor-panel-item")).stream()
			.findFirst().orElse(null));
		assertTrue(line.getText().contains(pdf.getFileName().toString()), line.getText());
		line.findElement(By.cssSelector(".antilia-ck-editor-panel-delete")).click();
		wait.until(d -> attached().isEmpty());
		assertEquals(200L, served(href));
		content().sendKeys(Keys.chord(Keys.CONTROL, "s"));
		wait.until(d -> Long.valueOf(404).equals(served(href)));
		assertNoPolicyViolations();
		assertNoErrors();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void knowsAgainTheFilesOfTheNotesInMarkdown(SeleniumBrowser browser) throws IOException
	{
		openEditor(browser, "wicket");
		pill("Notes in Markdown").click();
		wait.until(d -> notesKept().contains("Packing"));
		clickAtEnd(content().findElement(By.cssSelector("p")));
		Path pdf = file("notes", ".pdf", "%PDF-1.7\n%%EOF".getBytes(StandardCharsets.US_ASCII));
		try
		{
			editor().findElement(By.cssSelector("input.antilia-ck-editor-attach-picker")).sendKeys(
				pdf.toAbsolutePath().toString());
			wait.until(d -> !attached().isEmpty());
		}
		finally
		{
			Files.deleteIfExists(pdf);
		}
		// a plain link in Markdown, which the server keeps
		String name = pdf.getFileName().toString();
		content().sendKeys(Keys.chord(Keys.CONTROL, "s"));
		wait.until(d -> notesKept().contains("[" + name + "](") && notesKept().contains(".pdf)"));

		// read again, a file attached, not a link
		driver.navigate().refresh();
		waitForTheEditor();
		pill("Notes in Markdown").click();
		wait.until(d -> !attached().isEmpty());
		assertEquals(name, attached().get(0).getText().trim());
		assertNoPolicyViolations();
		assertNoErrors();
	}

	/**
	 * @return a file of some bytes in the build's folder: Firefox, a snap, cannot read the
	 *         temporary folder of the system
	 */
	private static Path file(String prefix, String suffix, byte[] bytes) throws IOException
	{
		Path file = Files.createTempFile(Path.of("target").toAbsolutePath(), prefix, suffix);
		Files.write(file, bytes);
		return file;
	}

	/**
	 * @return the files attached to the text, their links
	 */
	private List<WebElement> attached()
	{
		return content().findElements(By.cssSelector("a.antilia-attachment"));
	}

	/**
	 * @return the pill of a case of the demo
	 */
	private WebElement pill(String title)
	{
		return app().findElements(By.xpath(".//*[normalize-space(text())='" + title + "']"))
			.stream().filter(WebElement::isDisplayed).findFirst().orElseThrow();
	}

	/**
	 * @return the Markdown the server keeps of the notes
	 */
	private String notesKept()
	{
		return app().findElements(By.cssSelector(".rich-text-demo__markdown code")).stream()
			.map(each -> each.getDomProperty("textContent")).findFirst().orElse("");
	}

	/**
	 * Drops files on an element, as the browser does: name, type and base64 of each in turn.
	 */
	private void dropFiles(WebElement target, String... files)
	{
		script("var target = arguments[0], files = arguments[1], data = new DataTransfer();" +
			"for (var i = 0; i < files.length; i += 3) {" +
			"  var bytes = Uint8Array.from(atob(files[i + 2]), (c) => c.charCodeAt(0));" +
			"  data.items.add(new File([bytes], files[i], { type: files[i + 1] }));" +
			"}" +
			"target.dispatchEvent(new DragEvent('dragover', { bubbles: true, cancelable: true," +
			"  dataTransfer: data }));" +
			"target.dispatchEvent(new DragEvent('drop', { bubbles: true, cancelable: true," +
			"  dataTransfer: data }));", target, List.of(files));
	}

	/**
	 * @return the status the server answers an address with, asked from here: a 404 in the
	 *         browser would be an error of its console
	 */
	private static Long served(String url)
	{
		try
		{
			return (long)java.net.http.HttpClient.newHttpClient().send(java.net.http.HttpRequest
				.newBuilder(java.net.URI.create(url)).GET().build(),
				java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode();
		}
		catch (IOException e)
		{
			return -1L;
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			return -1L;
		}
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void tellsWhyAnImageIsNotUploadedInTheLanguageOfThePage(SeleniumBrowser browser)
	{
		openEditor(browser, "wicket");
		chooseLanguage("Deutsch");
		waitForTheEditor();
		transfer("drop", "tool.exe", "application/x-msdownload", Base64.getEncoder()
			.encodeToString("MZ".getBytes(StandardCharsets.UTF_8)), 0);
		wait.until(d -> message().startsWith("tool.exe ist keine Datei eines Typs, den der " +
			"Editor annimmt: PDF"));
		transfer("drop", "large.png", "image/png", PNG, 3 * 1024 * 1024);
		wait.until(d -> message().equals("large.png ist größer als 2 MB."));

		chooseLanguage("Español");
		waitForTheEditor();
		transfer("drop", "large.png", "image/png", PNG, 3 * 1024 * 1024);
		wait.until(d -> message().equals("large.png pesa más de 2 MB."));
		assertTrue(uploaded().isEmpty());
		assertNoErrors();
	}

	/**
	 * Chooses a language in the top bar, which the page is loaded anew in, and shows the tab of the
	 * framework again.
	 */
	private void chooseLanguage(String name)
	{
		// a mark of the page before, which the page loaded anew does not have
		script("window.antiliaTestBefore = true; var s = arguments[0]; s.selectedIndex = " +
			"Array.from(s.options).findIndex(o => o.text === arguments[1]);" +
			"s.dispatchEvent(new Event('change', {bubbles: true}));",
			driver.findElement(By.cssSelector(".topbar__setting select[name$='language']")), name);
		wait.until(d -> Boolean.TRUE.equals(script("return !window.antiliaTestBefore && " +
			"document.readyState === 'complete';")));
		tab(framework).click();
		wait.until(d -> app().isDisplayed() && !app().findElements(By.xpath("*")).isEmpty());
	}

	/**
	 * @return what the line above the text says of an image not uploaded, '' for nothing
	 */
	private String message()
	{
		return editor().findElements(By.cssSelector(".antilia-ck-editor-message")).stream()
			.findFirst().map(WebElement::getText).orElse("");
	}

	/**
	 * @return the images of the text the page uploaded
	 */
	private List<WebElement> uploaded()
	{
		return content().findElements(By.cssSelector("img[src*='/rich-text-images/']"));
	}

	/**
	 * Drops a file on the first paragraph of the text, or pastes it, as the browser does, of the
	 * bytes of a base64 and as many more of 0. Firefox gives a paste event of a script no data:
	 * there the file goes into CKEditor's clipboard as the paste event would bring it.
	 */
	private void transfer(String kind, String name, String type, String base64, int padding)
	{
		script("var content = arguments[0], target = content.querySelector('p'), kind = arguments[1];" +
			"var head = Uint8Array.from(atob(arguments[4]), (c) => c.charCodeAt(0));" +
			"var bytes = new Uint8Array(head.length + arguments[5]); bytes.set(head);" +
			"var file = new File([bytes], arguments[2], { type: arguments[3] });" +
			"var data = new DataTransfer(); data.items.add(file);" +
			"var rect = target.getBoundingClientRect();" +
			"var at = { clientX: rect.left + 4, clientY: rect.top + rect.height / 2 };" +
			"if (kind === 'drop') {" +
			"  target.dispatchEvent(new DragEvent('dragenter', Object.assign({ bubbles: true," +
			"    cancelable: true, dataTransfer: data }, at)));" +
			"  target.dispatchEvent(new DragEvent('dragover', Object.assign({ bubbles: true," +
			"    cancelable: true, dataTransfer: data }, at)));" +
			"  target.dispatchEvent(new DragEvent('drop', Object.assign({ bubbles: true," +
			"    cancelable: true, dataTransfer: data }, at)));" +
			"} else if (!arguments[6]) {" +
			"  content.focus();" +
			"  target.dispatchEvent(new ClipboardEvent('paste', { bubbles: true, cancelable: true," +
			"    clipboardData: data }));" +
			"} else {" +
			"  content.focus();" +
			"  var transfer = { types: ['Files'], files: [file], effectAllowed: 'all'," +
			"    dropEffect: 'copy', setData: function () {}, getData: function () { return ''; } };" +
			"  content.ckeditorInstance.editing.view.document.fire('clipboardInput'," +
			"    { dataTransfer: transfer, method: 'paste' });" +
			"}", content(), kind, name, type, base64, padding, browser == SeleniumBrowser.FIREFOX);
	}

	private WebElement modes()
	{
		return app().findElement(By.cssSelector(".rich-text-demo__modes"));
	}

	/**
	 * @return the line under the toolbar telling what a paste lost
	 */
	private WebElement notice()
	{
		return modes().findElement(By.cssSelector(".antilia-rich-text-notice"));
	}

	/**
	 * @return the HTML the server kept of the text of the editor of the cleaning
	 */
	private String kept()
	{
		return modes().findElement(By.cssSelector(".rich-text-demo__saved code"))
			.getDomProperty("textContent");
	}

	private void openEditor(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "ckeditor");
		waitForTheEditor();
	}

	private void waitForTheEditor()
	{
		wait.until(d -> !app().findElements(By.cssSelector(
			".rich-text-demo__editor .antilia-ck-editor .ck-editor__editable h2")).isEmpty());
	}

	/**
	 * Pastes HTML where the cursor is, as the browser does: a paste event with the HTML, which
	 * CKEditor's clipboard takes. Firefox gives a paste event of a script no data: there the HTML
	 * goes into CKEditor's clipboard as the paste event would bring it.
	 */
	private void paste(String html)
	{
		if (browser == SeleniumBrowser.FIREFOX)
		{
			script("var html = arguments[1], text = html.replace(/<[^>]*>/g, '');" +
				"var transfer = { types: ['text/html', 'text/plain'], files: [], effectAllowed: 'all'," +
				" dropEffect: 'copy', setData: function () {}, getData: function (type) {" +
				" return type === 'text/html' ? html : type === 'text/plain' ? text : ''; } };" +
				"arguments[0].ckeditorInstance.editing.view.document.fire('clipboardInput'," +
				" { dataTransfer: transfer, content: html, method: 'paste' });", content(), html);
			return;
		}
		script("var data = new DataTransfer(); data.setData('text/html', arguments[1]);" +
			"data.setData('text/plain', arguments[1].replace(/<[^>]*>/g, ''));" +
			"arguments[0].dispatchEvent(new ClipboardEvent('paste', { clipboardData: data," +
			" bubbles: true, cancelable: true }));", content(), html);
	}

	private Object script(String script, Object... arguments)
	{
		return ((JavascriptExecutor)driver).executeScript(script, arguments);
	}

	private Object selection()
	{
		return script("var editor = arguments[0].closest('.antilia-ck-editor');" +
			"return (Wicket.CKEditor.of(editor.querySelector('textarea').id)).getState().selection;",
			content());
	}

	private WebElement editor()
	{
		return app().findElements(By.cssSelector(".rich-text-demo__editor .antilia-ck-editor"))
			.stream().filter(WebElement::isDisplayed).findFirst().orElseThrow();
	}

	private WebElement field()
	{
		return editor().findElement(By.cssSelector("textarea"));
	}

	private WebElement content()
	{
		return editor().findElement(By.cssSelector(".ck-editor__editable"));
	}

	private String html()
	{
		return (String)script("return arguments[0].ckeditorInstance.getData();", content());
	}

	private WebElement toolbar()
	{
		return editor().findElement(By.cssSelector(".ck-editor__top"));
	}

	/**
	 * @return the button of CKEditor's toolbar of a name, as its label has it
	 */
	private WebElement tool(String label)
	{
		return toolbar().findElement(By.xpath(".//button[.//span[contains(@class, " +
			"'ck-button__label') and text()='" + label + "']]"));
	}

	private WebElement view()
	{
		return app().findElement(By.cssSelector(".rich-text-demo__view"));
	}

	private WebElement checkBox(String label)
	{
		return app().findElements(By.cssSelector(".rich-text-demo__controls label")).stream()
			.filter(each -> each.getText().trim().equals(label)).findFirst().orElseThrow()
			.findElement(By.cssSelector("input"));
	}

	/**
	 * @return what the server, or the app, says it keeps
	 */
	private String status()
	{
		return app().findElements(By.cssSelector(".rich-text-demo__editor + .rich-text-demo__status"))
			.stream().filter(WebElement::isDisplayed).findFirst().orElseThrow().getText();
	}

	/**
	 * @return the text of a status of the page, which the server rendered
	 */
	private String status(String css)
	{
		return app().findElement(By.cssSelector(css)).getText();
	}

	private String known()
	{
		return status();
	}

	/**
	 * Clicks an element at the end of its text, scrolled into sight.
	 */
	private void clickAtEnd(WebElement element)
	{
		script("arguments[0].scrollIntoView({block: 'center'});", element);
		int width = element.getRect().getWidth();
		new Actions(driver).moveToElement(element, width / 2 - 2, 0).click().sendKeys(Keys.END)
			.perform();
	}

	/**
	 * Selects a word of the article, as the user does with the mouse: a range of the page, which
	 * CKEditor takes as its selection.
	 */
	private void selectWord(String word)
	{
		script("var content = arguments[0], word = arguments[1];" +
			"content.focus();" +
			"var walker = document.createTreeWalker(content, NodeFilter.SHOW_TEXT), node;" +
			"while ((node = walker.nextNode()) && node.data.indexOf(word) < 0) {}" +
			"var start = node.data.indexOf(word), range = document.createRange();" +
			"range.setStart(node, start); range.setEnd(node, start + word.length);" +
			"var selection = getSelection(); selection.removeAllRanges(); selection.addRange(range);",
			content(), word);
		wait.until(d -> word.equals(script("return getSelection().toString();")));
	}
}
