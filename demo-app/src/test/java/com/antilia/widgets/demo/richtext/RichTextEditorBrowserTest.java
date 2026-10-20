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
package com.antilia.widgets.demo.richtext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import com.antilia.widgets.demo.SeleniumBrowser;
import com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase;

/**
 * Drives {@link RichTextEditorPage} in Chrome and in Firefox, in deployment mode, so through the
 * minified script built from frontend/core and under the demo's strict Content Security Policy, in
 * Wicket and in the React and Svelte apps: formatting from the toolbar and the keys, the menus of
 * the text style and the table from the keyboard, the form of a link, a paste of unsafe HTML, the
 * notes in Markdown, leaving a list with Escape and Tab, reading only, the text area hidden in the
 * room of the editor until the script makes it, and the text and the selection the server keeps,
 * which the page shows again when opened anew.
 * Runs with {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class RichTextEditorBrowserTest extends FrameworkAppsTestCase
{
	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
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
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
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
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
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
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
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
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
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
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
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
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
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
