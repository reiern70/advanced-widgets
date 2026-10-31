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
package com.antiliacode.widgets.demo.richtext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
 * Drives {@link RichTextEditorPage} in Chrome and in Firefox, in deployment mode, so through the
 * minified script built from frontend/core and under the demo's strict Content Security Policy, in
 * Wicket and in the React and Svelte apps: formatting from the toolbar and the keys, superscript
 * and subscript among it, the menus of the text style and the table from the keyboard, the form of
 * a link, a paste of unsafe HTML, the notes in Markdown, leaving a list with Escape and Tab,
 * reading only, the text area hidden in the room of the editor until the script makes it, and the
 * text and the selection the server keeps, which the page shows again when opened anew; the code
 * blocks in the colors of the language their menu chooses, and the images uploaded from the form
 * of the image, dropped and pasted: several at once on the zone of the form or of the dialog of the
 * uploads, a picture too large scaled down, and an image taken out of the text deleted on the
 * server once the text is saved.
 * Runs with {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class RichTextEditorBrowserTest extends FrameworkAppsTestCase
{
	/** HTML from anywhere, to paste: a script, handlers and a link to javascript:. */
	private static final String UNSAFE_PASTE = "<p>Pasted <b>bold</b> <img src=\"favicon.svg\" " +
		"onload=\"window.pwned = 1\"><a href=\"javascript:window.pwned = 2\">a link</a>" +
		"<script>window.pwned = 3</script></p><p onclick=\"window.pwned = 4\">Styled</p>";

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void formatsTheTextFromTheToolbarAndTheKeys(SeleniumBrowser browser, String framework)
	{
		openEditor(browser, framework);
		assertEquals("Article", content().getDomAttribute("aria-label"));
		assertEquals("textbox", content().getDomAttribute("role"));
		assertEquals("toolbar", toolbar().getDomAttribute("role"));

		selectWord("oystercatchers");
		wait.until(d -> "false".equals(tool("bold").getDomAttribute("aria-disabled")) &&
			"true".equals(tool("italic").getDomAttribute("aria-pressed")));
		tool("bold").click();
		wait.until(d -> !content().findElements(By.xpath(".//strong[.//text()[contains(., " +
			"'oystercatchers')]]")).isEmpty());
		assertEquals("true", tool("bold").getDomAttribute("aria-pressed"));
		content().sendKeys(Keys.chord(Keys.CONTROL, "u"));
		wait.until(d -> !content().findElements(By.xpath(".//u[contains(., 'oystercatchers')]"))
			.isEmpty());
		wait.until(d -> status().contains("changes not saved"));
		assertEquals("true", editor().getDomAttribute("data-modified"));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void raisesAndLowersTheTextOneOrTheOther(SeleniumBrowser browser, String framework)
	{
		openEditor(browser, framework);
		// the article has some already: H₂S, km²
		assertFalse(content().findElements(By.xpath(".//sub[. = '2']")).isEmpty());
		assertFalse(content().findElements(By.xpath(".//sup[. = '2']")).isEmpty());

		selectWord("flats");
		wait.until(d -> "false".equals(tool("superscript").getDomAttribute("aria-pressed")));
		tool("superscript").click();
		wait.until(d -> !content().findElements(By.xpath(".//sup[. = 'flats']")).isEmpty());
		assertEquals("true", tool("superscript").getDomAttribute("aria-pressed"));
		// Ctrl+Shift+, lowers it instead
		content().sendKeys(Keys.chord(Keys.CONTROL, Keys.SHIFT, ","));
		wait.until(d -> !content().findElements(By.xpath(".//sub[. = 'flats']")).isEmpty());
		assertTrue(content().findElements(By.xpath(".//sup[. = 'flats']")).isEmpty());
		assertEquals("false", tool("superscript").getDomAttribute("aria-pressed"));
		assertEquals("true", tool("subscript").getDomAttribute("aria-pressed"));

		if ("wicket".equals(framework))
		{
			// the server keeps it, and the page shows it so
			content().sendKeys(Keys.chord(Keys.CONTROL, "s"));
			wait.until(d -> !app().findElements(By.xpath("//*[contains(@class, " +
				"'rich-text-demo__view')]//sub[. = 'flats']")).isEmpty());
		}
		content().sendKeys(Keys.chord(Keys.CONTROL, Keys.SHIFT, ","));
		wait.until(d -> content().findElements(By.xpath(".//sub[. = 'flats']")).isEmpty());
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void choosesAStyleAndATableFromTheirMenusWithTheKeys(SeleniumBrowser browser, String framework)
	{
		openEditor(browser, framework);
		clickAtEnd(content().findElement(By.cssSelector("p")));
		// back from the text to the toolbar, on its first tool, the text style
		content().sendKeys(Keys.chord(Keys.SHIFT, Keys.TAB));
		wait.until(d -> tool("heading").equals(d.switchTo().activeElement()));
		assertEquals("Paragraph", tool("heading").getText().trim());

		tool("heading").sendKeys(Keys.ARROW_DOWN);
		wait.until(d -> "Paragraph".equals(d.switchTo().activeElement().getText().trim()));
		driver.switchTo().activeElement().sendKeys(Keys.ARROW_DOWN);
		assertEquals("menuitemradio", driver.switchTo().activeElement().getDomAttribute("role"));
		driver.switchTo().activeElement().sendKeys(Keys.ENTER);
		wait.until(d -> content().findElements(By.cssSelector("h1")).size() == 1);
		assertTrue(content().findElement(By.cssSelector("h1")).getText().startsWith("The tide"));
		assertEquals("Heading 1", tool("heading").getText().trim());

		// the arrow keys along the toolbar, to the table and its menu
		tool("heading").click();
		driver.switchTo().activeElement().sendKeys(Keys.ESCAPE);
		wait.until(d -> tool("heading").equals(d.switchTo().activeElement()));
		while (!tool("table").equals(driver.switchTo().activeElement()))
		{
			driver.switchTo().activeElement().sendKeys(Keys.ARROW_RIGHT);
		}
		tool("table").sendKeys(Keys.ENTER);
		wait.until(d -> "Insert a table".equals(d.switchTo().activeElement().getText().trim()));
		int tables = content().findElements(By.cssSelector("table")).size();
		driver.switchTo().activeElement().sendKeys(Keys.ENTER);
		wait.until(d -> content().findElements(By.cssSelector("table")).size() == tables + 1);
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void colorsTheTextAndItsBackgroundFromTheirPickers(SeleniumBrowser browser, String framework)
	{
		openEditor(browser, framework);
		selectWord("oystercatchers");
		tool("textColor").click();
		WebElement red = wait.until(d -> toolbar().findElements(By.cssSelector(
			"[role=menu]:not([hidden]) [data-item='red']")).stream().findFirst().orElse(null));
		red.click();
		By colored = By.xpath(".//span[@class='antilia-color-red'][contains(., 'oystercatchers')]");
		wait.until(d -> !content().findElements(colored).isEmpty());
		assertEquals("red", tool("textColor").getDomAttribute("data-color"));
		// in a color of the theme, not that of the text around
		assertNotEquals(content().findElement(By.cssSelector("p")).getCssValue("color"),
			content().findElement(colored).getCssValue("color"));
		// the bar under the tool in that color, showing on the toolbar
		WebElement textBar = tool("textColor").findElement(By.cssSelector(
			".antilia-rich-text-tool-bar"));
		assertEquals(content().findElement(colored).getCssValue("color"),
			textBar.getCssValue("background-color"));
		assertTrue(textBar.getSize().getWidth() > 5 && textBar.getSize().getHeight() > 0,
			textBar.getSize().toString());

		// the picker again: red ticked in the grid, and first of the last colors chosen below it
		tool("textColor").click();
		WebElement square = wait.until(d -> toolbar().findElements(By.cssSelector(
			"[role=menu]:not([hidden]) .antilia-rich-text-palette-grid [data-item='red']")).stream()
			.findFirst().orElse(null));
		assertEquals(square, driver.switchTo().activeElement());
		assertEquals("Red", square.getDomAttribute("aria-label"));
		By tick = By.cssSelector(".antilia-rich-text-palette-tick");
		assertTrue(square.findElement(tick).isDisplayed());
		assertFalse(toolbar().findElement(By.cssSelector("[role=menu]:not([hidden]) " +
			".antilia-rich-text-palette-grid [data-item='blue'] .antilia-rich-text-palette-tick"))
			.isDisplayed());
		WebElement recent = toolbar().findElements(By.cssSelector("[role=menu]:not([hidden]) " +
			".antilia-rich-text-palette-recent [role=menuitemradio]")).get(0);
		assertEquals("red", recent.getDomAttribute("data-item"));
		assertTrue(recent.findElement(tick).isDisplayed());
		// squares, in a grid
		assertEquals(square.getSize().getWidth(), square.getSize().getHeight());
		square.sendKeys(Keys.ESCAPE);
		wait.until(d -> toolbar().findElements(By.cssSelector("[role=menu]:not([hidden])"))
			.isEmpty());

		// the background with the keys: the picker opens on None, Y goes to Yellow
		selectWord("oystercatchers");
		tool("textBackground").sendKeys(Keys.ARROW_DOWN);
		wait.until(d -> "None".equals(d.switchTo().activeElement().getText().trim()));
		driver.switchTo().activeElement().sendKeys("y");
		wait.until(d -> "Yellow".equals(d.switchTo().activeElement().getDomAttribute("aria-label")));
		driver.switchTo().activeElement().sendKeys(Keys.ENTER);
		By marked = By.xpath(".//mark[@class='antilia-background-yellow']" +
			"[contains(., 'oystercatchers')]");
		wait.until(d -> !content().findElements(marked).isEmpty());
		// the bar under the tool in the color of the background
		wait.until(d -> "yellow".equals(tool("textBackground").getDomAttribute("data-color")));
		assertEquals(content().findElement(marked).getCssValue("background-color"),
			tool("textBackground").findElement(By.cssSelector(".antilia-rich-text-tool-bar"))
				.getCssValue("background-color"));
		// classes, no style
		assertTrue(value().contains("<span class=\"antilia-color-red\"><mark " +
			"class=\"antilia-background-yellow\">oystercatchers</mark></span>"), value());
		assertFalse(value().contains("style="), value());

		if ("wicket".equals(framework))
		{
			// the server keeps the colors, and the page shows them
			content().sendKeys(Keys.chord(Keys.CONTROL, "s"));
			wait.until(d -> !app().findElements(By.cssSelector(".rich-text-demo__view " +
				".antilia-color-red mark.antilia-background-yellow, .rich-text-demo__view " +
				"mark.antilia-background-yellow .antilia-color-red")).isEmpty());
		}
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void linksTheTextInAFormOfItsOwn(SeleniumBrowser browser, String framework)
	{
		openEditor(browser, framework);
		selectWord("tide");
		content().sendKeys(Keys.chord(Keys.CONTROL, "k"));
		WebElement form = wait.until(d -> d.findElements(By.cssSelector(
			".antilia-popover .antilia-rich-text-form")).stream().findFirst().orElse(null));
		WebElement url = form.findElement(By.cssSelector("input[type=url]"));
		wait.until(d -> url.equals(d.switchTo().activeElement()));

		url.sendKeys("javascript:alert(1)", Keys.ENTER);
		WebElement error = form.findElement(By.cssSelector("[role=alert]"));
		wait.until(d -> error.isDisplayed());
		assertEquals("true", url.getDomAttribute("aria-invalid"));

		url.clear();
		url.sendKeys("https://example.org/tides", Keys.ENTER);
		wait.until(d -> d.findElements(By.cssSelector(".antilia-popover")).isEmpty());
		WebElement link = content().findElement(By.cssSelector("a[href='https://example.org/tides']"));
		assertEquals("tide", link.getText());
		assertEquals("noopener noreferrer nofollow", link.getDomAttribute("rel"));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void keepsWhatAPasteBringsAndDropsTheRest(SeleniumBrowser browser, String framework)
	{
		openEditor(browser, framework);
		clickAtEnd(content().findElement(By.cssSelector("p")));
		paste("<p>Pasted <b>bold</b> <img src=\"favicon.svg\" onload=\"window.pwned = 1\">" +
			"<a href=\"javascript:window.pwned = 2\">a link</a><script>window.pwned = 3</script>" +
			"</p><p style=\"position: fixed\" onclick=\"window.pwned = 4\">Styled</p>");
		wait.until(d -> content().getText().contains("Styled"));

		assertNull(script("return window.pwned;"));
		assertEquals(0L, script("return Array.from(arguments[0].querySelectorAll('p')).filter(" +
			"(p) => /Pasted|Styled/.test(p.textContent)).flatMap((p) => [p, ...p.querySelectorAll(" +
			"'*')]).filter((e) => e.tagName === 'SCRIPT' || Array.from(e.attributes).some((a) => " +
			"a.name === 'style' || a.name.startsWith('on'))).length;", content()));
		assertTrue(content().findElements(By.cssSelector("a[href^='javascript']")).isEmpty());
		assertTrue(content().getText().contains("Pasted bold"));

		if ("wicket".equals(framework))
		{
			// the server cleans what it keeps, and the page shows it so
			content().sendKeys(Keys.chord(Keys.CONTROL, "s"));
			wait.until(d -> app().findElement(By.cssSelector(".rich-text-demo__view")).getText()
				.contains("Styled"));
			assertNull(script("return window.pwned;"));
			assertTrue(app().findElements(By.cssSelector(".rich-text-demo__view :is([onload], " +
				"[onclick], [style], script)")).isEmpty());
		}
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void keepsTheNotesAsMarkdown(SeleniumBrowser browser, String framework)
	{
		openEditor(browser, framework);
		selectCase("Notes in Markdown");
		WebElement notes = app().findElements(By.cssSelector(".rich-text-demo__editor .ProseMirror"))
			.stream().filter(WebElement::isDisplayed).findFirst().orElseThrow();
		clickAtEnd(notes.findElement(By.cssSelector("h1")));
		notes.sendKeys(" for two");
		wait.until(d -> app().findElements(By.cssSelector(".rich-text-demo__saved code")).stream()
			.filter(WebElement::isDisplayed).anyMatch(code -> code.getText()
				.startsWith("# Packing list for two")));
		// Markdown has no underline: no tool for it
		assertTrue(notes.findElement(By.xpath("ancestor::div[contains(@class, " +
			"'antilia-rich-text-editor')]")).findElements(By.cssSelector("[data-tool=underline]"))
			.isEmpty());
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void leavesAListWithEscapeThenTab(SeleniumBrowser browser, String framework)
	{
		openEditor(browser, framework);
		WebElement item = content().findElements(By.cssSelector("ul[data-type=taskList] > li p"))
			.get(1);
		clickAtEnd(item);
		String before = html();
		content().sendKeys(Keys.TAB);
		wait.until(d -> !html().equals(before));
		content().sendKeys(Keys.chord(Keys.SHIFT, Keys.TAB));
		wait.until(d -> html().equals(before));

		content().sendKeys(Keys.ESCAPE);
		content().sendKeys(Keys.TAB);
		wait.until(d -> !content().equals(d.switchTo().activeElement()));
		assertEquals(before, html());
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void readsOnlyWithoutItsToolbar(SeleniumBrowser browser, String framework)
	{
		openEditor(browser, framework);
		checkBox("Read only").click();
		wait.until(d -> editor().getDomAttribute("class").contains(
			"antilia-rich-text-editor-readonly"));
		wait.until(d -> !toolbar().isDisplayed());
		String before = html();
		clickAtEnd(content().findElement(By.cssSelector("p")));
		new Actions(driver).sendKeys("x").perform();
		assertEquals(before, html());
		assertEquals("false", content().getDomAttribute("contenteditable"));
		assertNoErrors();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void hidesTheHtmlUntilTheEditorIsMadeInTheRoomItTakes(SeleniumBrowser browser)
	{
		openEditor(browser, "wicket");
		assertEquals("antilia-rich-text-editor-field", field().getDomAttribute("class"));

		// a text area as the server renders it, which the script has not made an editor of yet
		@SuppressWarnings("unchecked")
		Map<String, Object> waiting = (Map<String, Object>)script("var area = " +
			"document.createElement('textarea'); area.id = 'waiting';" +
			"area.className = 'antilia-rich-text-editor-loading';" +
			"area.value = '<h2>Not to be seen</h2>'; arguments[0].after(area);" +
			"return { visibility: getComputedStyle(area).visibility, width: area.offsetWidth," +
			" height: area.offsetHeight, editor: arguments[0].offsetHeight };", editor());
		assertEquals("hidden", waiting.get("visibility"));
		assertEquals(number(waiting.get("editor")), number(waiting.get("height")), 1);

		// made an editor: shown, in the room the text area kept
		@SuppressWarnings("unchecked")
		Map<String, Object> made = (Map<String, Object>)script("Wicket.RichTextEditor.attach(" +
			"'waiting', {}); var area = document.getElementById('waiting'), editor = " +
			"area.parentElement; return { field: area.className, visibility: " +
			"getComputedStyle(editor).visibility, width: editor.offsetWidth, height: " +
			"editor.offsetHeight };");
		assertEquals("antilia-rich-text-editor-field", made.get("field"));
		assertEquals("visible", made.get("visibility"));
		assertEquals(number(waiting.get("width")), number(made.get("width")), 1);
		assertEquals(number(waiting.get("height")), number(made.get("height")), 1);
		assertNoErrors();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void hearsOfTheEventsTheEditorIsGiven(SeleniumBrowser browser)
	{
		openEditor(browser, "wicket");
		String title = driver.getTitle();

		// the focus got goes to the server alone
		clickAtEnd(content().findElement(By.cssSelector("p")));
		wait.until(d -> heard().contains("entered the article"));

		// the changes not saved mark the title, in the browser alone
		content().sendKeys(" Wading.");
		wait.until(d -> d.getTitle().equals("* " + title));

		// Ctrl+S submits the form with the text, which the server cleans and keeps
		content().sendKeys(Keys.chord(Keys.CONTROL, "s"));
		wait.until(d -> heard().contains("Saved the article"));
		wait.until(d -> d.getTitle().equals(title));
		wait.until(d -> app().findElement(By.cssSelector(".rich-text-demo__view")).getText()
			.contains("Wading."));

		// the size the user gives the editor by the keys of its handle, sent once done: the
		// height given, the width measured
		WebElement handle = editor().findElement(By.cssSelector(".antilia-resizable-handle"));
		int width = editor().getRect().getWidth();
		handle.sendKeys(Keys.ARROW_DOWN);
		wait.until(d -> heard().matches("The server heard you resized the article to " +
			"\\d+ × \\d+ px\\."));
		int heardWidth = Integer.parseInt(heard().replaceAll("\\D*(\\d+) × .*", "$1"));
		assertEquals(width, heardWidth, 1, heard());
		assertNoErrors();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void tellsWhatAPasteLostAndWhatTheServerRemovedOrRejected(SeleniumBrowser browser)
	{
		openEditor(browser, "wicket");
		selectCase("The cleaning on the server");
		wait.until(d -> !modes().findElements(By.cssSelector(".ProseMirror")).isEmpty());
		assertEquals("status", notice().getDomAttribute("role"));
		assertEquals("", notice().getText());

		// a paste of unsafe HTML: the editor says what it lost, and the server hears of it
		WebElement text = modes().findElement(By.cssSelector(".ProseMirror"));
		text.click();
		script("arguments[0].editor.view.pasteHTML(arguments[1]);", text, UNSAFE_PASTE);
		wait.until(d -> notice().getText().startsWith("Part of the paste was left out: scripts"));
		wait.until(d -> Toasts.latest(d).startsWith("The editor left out of the paste: "));
		assertTrue(Toasts.latest(driver).contains("script"), Toasts.latest(driver));
		assertTrue(Toasts.latest(driver).contains("onclick"), Toasts.latest(driver));
		assertNull(script("return window.pwned;"));
		// gone with the next change
		modes().findElement(By.cssSelector(".ProseMirror")).sendKeys("!");
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

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void proofreadsOnTheServerAndSuggestsMentionsAndTerms(SeleniumBrowser browser)
	{
		openEditor(browser, "wicket");
		selectCase("Writing: spelling, mentions, completions");
		WebElement text = wait.until(d -> app().findElements(By.cssSelector(
			".rich-text-demo__editor .ProseMirror")).stream().filter(WebElement::isDisplayed)
			.filter(each -> each.getText().startsWith("Writing")).findFirst().orElse(null));
		assertEquals("false", text.getDomAttribute("spellcheck"), "two underlines else");

		// the demo's proofreader, on the server, underlines what it knows
		WebElement wrong = wait.until(d -> text.findElements(By.cssSelector(
			".antilia-rich-text-issue-spelling")).stream().filter(each -> each.getText()
			.equals("Teh")).findFirst().orElse(null));
		assertFalse(text.findElements(By.cssSelector(".antilia-rich-text-issue-grammar")).isEmpty());
		wrong.click();
		WebElement popover = wait.until(d -> d.findElements(By.cssSelector(
			".antilia-rich-text-issue-popover")).stream().filter(WebElement::isDisplayed).findFirst()
			.orElse(null));
		assertTrue(popover.getText().contains("This word may be spelled wrong."), popover.getText());
		popover.findElement(By.xpath(".//button[normalize-space()='the']")).click();
		wait.until(d -> text.findElement(By.cssSelector("p")).getText().startsWith(
			"the proofreader"));

		// @ and a name: the people, from the server; Enter mentions the one chosen
		clickAtEnd(text.findElements(By.cssSelector("p")).get(1));
		text.sendKeys(" and @lov");
		WebElement list = wait.until(d -> app().findElements(By.cssSelector(
			".antilia-rich-text-suggestions")).stream().filter(each -> each.getText()
			.contains("Ada Lovelace")).findFirst().orElse(null));
		assertEquals(list.getDomAttribute("id"), text.getDomAttribute("aria-controls"));
		text.sendKeys(Keys.ENTER);
		wait.until(d -> text.findElements(By.cssSelector(".antilia-mention")).size() == 2);

		// the start of a term and Ctrl+Space: the terms completing it
		text.sendKeys("Tip");
		text.sendKeys(Keys.chord(Keys.CONTROL, Keys.SPACE));
		wait.until(d -> app().findElements(By.cssSelector(".antilia-rich-text-suggestions"))
			.stream().anyMatch(each -> each.getText().equals("Tiptap")));
		text.sendKeys(Keys.TAB);
		wait.until(d -> text.getText().contains("@Ada Lovelace Tiptap"));

		// the server reads the mentions of the text it keeps
		text.sendKeys(Keys.chord(Keys.CONTROL, "s"));
		wait.until(d -> app().findElements(By.cssSelector(".rich-text-demo__saved p")).stream()
			.anyMatch(each -> each.getText().equals("Mentions the server reads: @Ada Lovelace " +
				"(people: ada), @Ada Lovelace (people: ada)")));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void hasTheAppsCheckedAndSuggestedByThePage(SeleniumBrowser browser, String framework)
	{
		openEditor(browser, framework);
		clickAtEnd(content().findElement(By.cssSelector("p")));
		content().sendKeys(" Teh @lov");
		WebElement list = wait.until(d -> app().findElements(By.cssSelector(
			".antilia-rich-text-suggestions")).stream().filter(each -> each.getText()
			.contains("Ada Lovelace")).findFirst().orElse(null));
		assertEquals(list.getDomAttribute("id"), content().getDomAttribute("aria-controls"));
		content().sendKeys(Keys.ENTER);
		wait.until(d -> !content().findElements(By.cssSelector(".antilia-mention")).isEmpty());
		// the page's proofreader, through the server
		wait.until(d -> content().findElements(By.cssSelector(".antilia-rich-text-issue-spelling"))
			.stream().anyMatch(each -> each.getText().equals("Teh")));
		assertNoErrors();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void hearsOfTheTextSelectedAndOfTheHeadings(SeleniumBrowser browser)
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
	void theServerKeepsTheTextAndTheSelection(SeleniumBrowser browser)
	{
		openEditor(browser, "wicket");
		selectWord("mud");
		wait.until(d -> known().contains("selected from"));
		String selection = known().replaceAll(".*(selected from \\d+ to \\d+).*", "$1");
		@SuppressWarnings("unchecked")
		Map<String, Object> state = (Map<String, Object>)script("return Wicket.RichTextEditor.of(" +
			"arguments[0]).getState().selection;", field().getDomAttribute("id"));

		// a page made anew: the session gives the selection back
		script("return new Promise(function (done) { setTimeout(done, 1500); });");
		driver.navigate().refresh();
		waitForTheEditor();
		assertTrue(known().contains(selection), known());
		@SuppressWarnings("unchecked")
		Map<String, Object> again = (Map<String, Object>)script("return Wicket.RichTextEditor.of(" +
			"arguments[0]).getState().selection;", field().getDomAttribute("id"));
		assertEquals(state, again);
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void highlightsTheCodeBlocksInTheLanguageTheirMenuChooses(SeleniumBrowser browser,
		String framework)
	{
		openEditor(browser, framework);
		if ("wicket".equals(framework))
		{
			// the grammars are a script of their own, which the editor's does not hold
			assertEquals(Boolean.TRUE, script("return !!Wicket.RichTextHighlighter;"));
		}
		wait.until(d -> "Java".equals(block().getDomAttribute("data-antilia-language")) &&
			!block().findElements(By.cssSelector("span.tok-keyword")).isEmpty());
		WebElement keyword = block().findElement(By.cssSelector("span.tok-keyword"));
		// record is a keyword by its place only, which Lezer's grammar of Java leaves plain
		assertEquals("static", keyword.getText());
		assertNotEquals(script("return getComputedStyle(arguments[0]).color;", block()),
			script("return getComputedStyle(arguments[0]).color;", keyword), "in its color");
		// the HTML stays plain
		assertTrue(value().contains("<pre><code class=\"language-java\">"), value());
		assertFalse(value().contains("tok-"), value());

		keyword.click();
		tool("codeBlock").click();
		WebElement typeScript = menuItem("TypeScript");
		assertEquals("true", menuItem("Java").getDomAttribute("aria-checked"));
		typeScript.click();
		wait.until(d -> "TypeScript".equals(block().getDomAttribute("data-antilia-language")));
		wait.until(d -> value().contains("<pre><code class=\"language-typescript\">"));

		checkBox("Highlight the code").click();
		wait.until(d -> block().findElements(By.cssSelector("span[class^='tok-']")).isEmpty() &&
			block().getDomAttribute("data-antilia-language") == null);
		assertNoErrors();
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

			// chosen in the form of the image
			clickAtEnd(content().findElement(By.cssSelector("p")));
			tool("image").click();
			WebElement form = wait.until(d -> d.findElements(By.cssSelector(
				".antilia-popover .antilia-rich-text-form")).stream().findFirst().orElse(null));
			form.findElement(By.cssSelector("input[type=file]")).sendKeys(image.toAbsolutePath()
				.toString());
			wait.until(d -> d.findElements(By.cssSelector(".antilia-popover")).isEmpty());
			wait.until(d -> uploaded().size() == 1);
			wait.until(d -> Boolean.TRUE.equals(script("return arguments[0].complete && " +
				"arguments[0].naturalWidth === 1;", uploaded().get(0))));
		}
		finally
		{
			Files.deleteIfExists(image);
		}

		// dropped into the text, and pasted as a screenshot is
		transfer("drop", "dropped.png", "image/png", PNG);
		wait.until(d -> uploaded().size() == 2);
		transfer("paste", "pasted.png", "image/png", PNG);
		wait.until(d -> uploaded().size() == 3);
		assertTrue(editor().findElements(By.cssSelector(".antilia-rich-text-uploading")).isEmpty());

		// a file of another type: refused before uploading, a message saying why
		transfer("drop", "notes.txt", "text/plain", Base64.getEncoder().encodeToString(
			"notes".getBytes(StandardCharsets.UTF_8)));
		WebElement message = wait.until(d -> editor().findElements(By.cssSelector(
			".antilia-rich-text-message")).stream().findFirst().orElse(null));
		assertEquals("alert", message.getDomAttribute("role"));
		assertTrue(message.getText().startsWith("notes.txt is not an image"), message.getText());
		assertEquals(3, uploaded().size());

		if ("wicket".equals(framework))
		{
			// the server keeps the addresses through its cleaning, and serves the images
			content().sendKeys(Keys.chord(Keys.CONTROL, "s"));
			wait.until(d -> app().findElements(By.cssSelector(".rich-text-demo__view " +
				"img[src*='/rich-text-images/']")).size() == 3);
		}
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void uploadsSeveralImagesOnTheZoneAndDeletesThoseTakenOutOnceSaved(SeleniumBrowser browser,
		String framework)
	{
		openEditor(browser, framework);
		clickAtEnd(content().findElement(By.cssSelector("p")));
		tool("image").click();
		WebElement zone = wait.until(d -> d.findElements(By.cssSelector(
			".antilia-popover .antilia-rich-text-dropzone")).stream().findFirst().orElse(null));
		// once the popover is shown
		wait.until(d -> "Drop images here".equals(zone.findElement(By.cssSelector(
			".antilia-rich-text-dropzone-text")).getText()));

		// two images and a file of another type, dropped at once
		dropFiles(zone, "one.png", "image/png", PNG, "two.png", "image/png", PNG, "notes.txt",
			"text/plain", Base64.getEncoder().encodeToString("notes".getBytes(
				StandardCharsets.UTF_8)));
		wait.until(d -> d.findElements(By.cssSelector(".antilia-popover")).isEmpty());
		WebElement message = wait.until(d -> editor().findElements(By.cssSelector(
			".antilia-rich-text-message")).stream().findFirst().orElse(null));
		assertTrue(message.getText().startsWith("notes.txt is not an image"), message.getText());
		wait.until(d -> uploaded().size() == 2);

		// a picture wider than the page takes: scaled down, once ticked
		script("var content = arguments[0], target = content.querySelector('p');" +
			"var canvas = document.createElement('canvas'); canvas.width = 3000; canvas.height = 20;" +
			"canvas.getContext('2d').fillRect(0, 0, 3000, 20);" +
			"canvas.toBlob((blob) => { var data = new DataTransfer();" +
			"  data.items.add(new File([blob], 'wide.png', { type: 'image/png' }));" +
			"  var rect = target.getBoundingClientRect();" +
			"  target.dispatchEvent(new DragEvent('drop', { bubbles: true, cancelable: true," +
			"    clientX: rect.left + 4, clientY: rect.top + rect.height / 2, dataTransfer: data }));" +
			"}, 'image/png');", content());
		WebElement scale = wait.until(d -> d.findElements(By.cssSelector(
			".antilia-rich-text-scale-dialog")).stream().findFirst().orElse(null));
		assertEquals("Scale wide.png down to 2400 × 16 (3000 × 20)", scale.findElement(
			By.cssSelector("label")).getDomProperty("textContent").trim());
		scale.findElement(By.cssSelector("input[type=checkbox]")).click();
		scale.findElement(By.cssSelector(".modal-dialog-footer button")).click();
		wait.until(d -> uploaded().size() == 3);
		wait.until(d -> uploaded().stream().anyMatch(image -> Long.valueOf(2400).equals(
			script("return arguments[0].naturalWidth;", image))));

		// the menu of the images takes one out of the text; saved, the server deletes it
		String first = uploaded().get(0).getDomProperty("src");
		tool("images").click();
		WebElement delete = wait.until(d -> editor().findElements(By.cssSelector(
			"[role=menu] .antilia-rich-text-images-delete")).stream().findFirst().orElse(null));
		assertTrue(delete.getDomAttribute("aria-label").startsWith("Delete "));
		delete.click();
		wait.until(d -> uploaded().size() == 2);
		assertEquals(200L, served(first));
		content().sendKeys(Keys.chord(Keys.CONTROL, "s"));
		wait.until(d -> Long.valueOf(404).equals(served(first)));
		assertEquals(200L, served(uploaded().get(0).getDomProperty("src")));
		assertNoErrors();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void uploadsInTheDialogTheImagesTheUserKeeps(SeleniumBrowser browser)
	{
		openEditor(browser, "wicket");
		checkBox("Upload images in a dialog").click();
		// the editor made anew, with the dialog
		wait.until(d -> Boolean.TRUE.equals(script("var field = document.querySelector(" +
			"'.rich-text-demo__editor textarea'), editor = field && Wicket.RichTextEditor.of(" +
			"field.id); return !!editor && editor.options.uploadDialog === true;")));
		waitForTheEditor();
		clickAtEnd(content().findElement(By.cssSelector("p")));
		tool("image").click();
		WebElement open = wait.until(d -> d.findElements(By.cssSelector(
			".antilia-popover .antilia-rich-text-form-upload button")).stream().findFirst()
			.orElse(null));
		wait.until(d -> "Upload images…".equals(open.getText()));
		open.click();
		WebElement dialog = wait.until(d -> d.findElements(By.cssSelector(
			".antilia-rich-text-upload-dialog")).stream().findFirst().orElse(null));
		assertEquals("Upload images", dialog.findElement(By.cssSelector(".modal-dialog-title"))
			.getText());
		dropFiles(dialog.findElement(By.cssSelector(".antilia-rich-text-dropzone")), "one.png",
			"image/png", PNG, "two.png", "image/png", PNG);
		wait.until(d -> dialog.findElements(By.cssSelector(".antilia-rich-text-upload-done"))
			.size() == 2);
		// the second taken out of the list: only the first goes into the text
		dialog.findElements(By.cssSelector(".antilia-rich-text-upload-remove")).get(1).click();
		dialog.findElement(By.cssSelector(".modal-dialog-footer button")).click();
		wait.until(d -> d.findElements(By.cssSelector(".antilia-rich-text-upload-dialog"))
			.isEmpty());
		wait.until(d -> uploaded().size() == 1);
		assertNoErrors();
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

	/** A PNG of one pixel. */
	private static final String PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk" +
		"+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

	/**
	 * Drops a file on the first paragraph of the text, or pastes it, as the browser does. Firefox
	 * gives a paste event of a script no data: ProseMirror is told of the paste then, as it tells
	 * the editor's plugins of one.
	 */
	private void transfer(String kind, String name, String type, String base64)
	{
		script("var content = arguments[0], target = content.querySelector('p'), kind = arguments[1];" +
			"var bytes = Uint8Array.from(atob(arguments[4]), (c) => c.charCodeAt(0));" +
			"var data = new DataTransfer();" +
			"data.items.add(new File([bytes], arguments[2], { type: arguments[3] }));" +
			"var rect = target.getBoundingClientRect();" +
			"if (kind === 'drop') {" +
			"  target.dispatchEvent(new DragEvent('drop', { bubbles: true, cancelable: true," +
			"    clientX: rect.left + 4, clientY: rect.top + rect.height / 2, dataTransfer: data }));" +
			"} else if (!arguments[5]) {" +
			"  content.focus();" +
			"  target.dispatchEvent(new ClipboardEvent('paste', { bubbles: true, cancelable: true," +
			"    clipboardData: data }));" +
			"} else {" +
			"  var view = content.editor.view;" +
			"  view.someProp('handlePaste', (handle) => handle(view, { clipboardData: data," +
			"    preventDefault: () => undefined }, view.state.doc.slice(0, 0)));" +
			"}", content(), kind, name, type, base64, browser == SeleniumBrowser.FIREFOX);
	}

	/**
	 * @return the images of the text the page uploaded
	 */
	private List<WebElement> uploaded()
	{
		return content().findElements(By.cssSelector("img[src*='/rich-text-images/']"));
	}

	/**
	 * @return the code block of the article
	 */
	private WebElement block()
	{
		return content().findElement(By.cssSelector("pre"));
	}

	/**
	 * @return an item of the menu of a tool open, by its text
	 */
	private WebElement menuItem(String text)
	{
		return wait.until(d -> editor().findElements(By.cssSelector("[role=menu] " +
			"[role=menuitemradio]")).stream().filter(item -> text.equals(item.getText().trim()))
			.findFirst().orElse(null));
	}

	/**
	 * @return the text as the editor writes it, Tiptap's HTML
	 */
	private String value()
	{
		return (String)script("return arguments[0].editor.getHTML();", content());
	}

	private void openEditor(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "rich-text-editor");
		waitForTheEditor();
	}

	private void waitForTheEditor()
	{
		wait.until(d -> !app().findElements(By.cssSelector(
			".rich-text-demo__editor .antilia-rich-text-editor .ProseMirror h2")).isEmpty());
	}

	/**
	 * Pastes HTML where the cursor is, as the browser does: through ProseMirror's handling of a
	 * paste, which Firefox does not run for a paste event of a script. Tiptap puts its editor on
	 * the element of the text.
	 */
	private void paste(String html)
	{
		script("arguments[0].editor.view.pasteHTML(arguments[1]);", content(), html);
	}

	private Object script(String script, Object... arguments)
	{
		return ((JavascriptExecutor)driver).executeScript(script, arguments);
	}

	private static double number(Object value)
	{
		return ((Number)value).doubleValue();
	}

	private WebElement editor()
	{
		return app().findElement(By.cssSelector(".rich-text-demo__editor .antilia-rich-text-editor"));
	}

	private WebElement field()
	{
		return editor().findElement(By.cssSelector("textarea"));
	}

	private WebElement content()
	{
		return editor().findElement(By.cssSelector(".ProseMirror"));
	}

	private String html()
	{
		return (String)script("return arguments[0].innerHTML;", content());
	}

	private WebElement toolbar()
	{
		return editor().findElement(By.cssSelector(".antilia-rich-text-toolbar"));
	}

	private WebElement tool(String name)
	{
		return toolbar().findElement(By.cssSelector("[data-tool='" + name + "']"));
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
		return "wicket".equals(framework) ? known() : app().findElement(By.cssSelector(
			".rich-text-demo__editor + .rich-text-demo__status")).getText();
	}

	private String heard()
	{
		return app().findElement(By.cssSelector(".rich-text-demo__heard")).getText();
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
		return app().findElement(By.cssSelector(".rich-text-demo__editor + " +
			".rich-text-demo__status")).getText();
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
	 * the editor takes as its selection.
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
