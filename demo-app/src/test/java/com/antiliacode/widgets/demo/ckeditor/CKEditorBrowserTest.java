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
