/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.codeeditor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.antilia.widgets.demo.Dropdowns;
import com.antilia.widgets.demo.SeleniumBrowser;
import com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase;

/**
 * Drives {@link CodeEditorPage} in Chrome and in Firefox, in deployment mode, so through the
 * minified script built from frontend/core, in Wicket and in the React and Svelte apps: the grammars
 * highlighting the samples, the text, the cursor and the size the user gives the editor, which the
 * server keeps in Wicket and the page shows again when opened anew, the languages and the options
 * switched, the notes in Markdown beside their preview, which follows the text as the user types
 * and a button hides and shows again, and the files in tabs, marked while they have changes not
 * saved and saved with Ctrl+S.
 * Runs with {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class CodeEditorBrowserTest extends FrameworkAppsTestCase
{
	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void highlightsTheSampleAndFollowsTheCursor(SeleniumBrowser browser, String framework)
	{
		openEditor(browser, framework);

		assertEquals("Order.java", app().findElement(By.cssSelector(".code-editor-demo__name"))
			.getText());
		assertTrue(tokens("tok-keyword").containsAll(List.of("package", "import", "public")));
		assertTrue(tokens("tok-comment").contains("/**"));
		assertEquals("Order.java", content().getDomAttribute("aria-label"));
		// CodeMirror's style element got the nonce of the page, under its policy
		assertEquals("flex", script("return getComputedStyle(arguments[0]).display;",
			editor().findElement(By.cssSelector(".cm-editor"))));

		click(line(9));
		content().sendKeys(Keys.END, " // the order");
		wait.until(d -> known().contains("the cursor on line 9, column") &&
			known().endsWith("changes not saved."));
		assertEquals("true", editor().getDomAttribute("data-modified"));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void plugsInTheGrammarOfALog(SeleniumBrowser browser, String framework)
	{
		openEditor(browser, framework);

		Dropdowns.chooseByText(driver, controls().findElement(By.cssSelector("select")),
			"Log (the page's own grammar)");
		wait.until(d -> tokens("tok-invalid").contains("ERROR"));
		assertTrue(tokens("tok-keyword").contains("INFO"));
		assertTrue(tokens("tok-meta").contains("WARN"));
		assertTrue(tokens("tok-number").contains("2026-10-14 08:12:03.114"));
		assertTrue(tokens("tok-string").contains("\"jdbc:postgresql://db/shop\""));
		assertTrue(tokens("tok-typeName").contains("IllegalStateException"));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void showsALanguageAgainWhereTheUserLeftIt(SeleniumBrowser browser, String framework)
	{
		openEditor(browser, framework);
		click(line(5));
		wait.until(d -> known().contains("the cursor on line 5,"));

		Dropdowns.chooseByText(driver, controls().findElement(By.cssSelector("select")), "Markdown");
		wait.until(d -> tokens("tok-heading").contains("Shop front"));
		assertTrue(known().startsWith("The app knows: README.md"));

		Dropdowns.chooseByText(driver, controls().findElement(By.cssSelector("select")), "Java");
		wait.until(d -> known().startsWith("The app knows: Order.java"));
		assertTrue(known().contains("the cursor on line 5,"), known());
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void switchesTheOptionsKeepingTheCursor(SeleniumBrowser browser, String framework)
	{
		openEditor(browser, framework);
		click(line(3));
		wait.until(d -> known().contains("the cursor on line 3,"));

		checkBox("Line numbers").click();
		wait.until(d -> editor().findElements(By.cssSelector(".cm-lineNumbers")).isEmpty());
		checkBox("Read only").click();
		wait.until(d -> editor().getDomAttribute("class").contains("antilia-code-editor-readonly"));
		Dropdowns.chooseByText(driver, controls().findElements(By.cssSelector("select")).get(1),
			"Not at all");
		wait.until(d -> editor().findElements(By.cssSelector(".antilia-resizable-handle"))
			.isEmpty());

		content().sendKeys("x");
		assertEquals("import java.math.BigDecimal;", line(3).getText(), "read only");
		assertTrue(known().contains("the cursor on line 3,"), known());
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void resizesByTheBarAlongItsBottom(SeleniumBrowser browser, String framework)
	{
		openEditor(browser, framework);
		WebElement handle = editor().findElement(By.cssSelector(".antilia-resizable-handle"));
		long before = Math.round(height(editor()));
		assertEquals("separator", handle.getDomAttribute("role"));
		assertEquals("Drag, or use the arrow keys, to resize the editor",
			handle.getDomAttribute("aria-label"));

		script("arguments[0].scrollIntoView({block: 'center'});", handle);
		new Actions(driver).moveToElement(handle).clickAndHold().moveByOffset(0, 30)
			.moveByOffset(0, 30).release().perform();
		wait.until(d -> Math.abs(height(editor()) - (before + 60)) <= 2);
		wait.until(d -> known().contains("auto × " + (before + 60) + " px"));
		handle.sendKeys(Keys.ARROW_UP);
		wait.until(d -> known().contains("auto × " + (before + 50) + " px"));
		assertNoErrors();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void theServerKeepsTheTextTheCursorAndTheSize(SeleniumBrowser browser)
	{
		openEditor(browser, "wicket");
		click(line(9));
		content().sendKeys(Keys.END, " // kept");
		WebElement handle = editor().findElement(By.cssSelector(".antilia-resizable-handle"));
		handle.sendKeys(Keys.ARROW_DOWN, Keys.ARROW_DOWN);
		long height = Math.round(height(editor()));
		wait.until(d -> known().contains("the cursor on line 9,") && known().contains("auto × " +
			height + " px"));
		String sentence = known();

		// a page made anew: the session gives it all back
		driver.get("http://localhost:" + localPort + "/code-editor");
		waitForTheEditors();
		assertEquals(sentence, known());
		assertTrue(line(9).getText().endsWith("// kept"));
		assertEquals(height, Math.round(height(editor())));
		assertEquals("true", editor().getDomAttribute("data-modified"));
		assertEquals(9L, script("var e = Wicket.CodeEditor.of(arguments[0].id); return e.view" +
			".state.doc.lineAt(e.getState().selection.ranges[0].head).number;",
			editor().findElement(By.cssSelector("textarea"))));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void marksTheTabOfAFileWithChangesAndSavesItWithCtrlS(SeleniumBrowser browser,
		String framework)
	{
		openEditor(browser, framework);
		selectCase("Files in tabs");
		click(fileContent().findElement(By.cssSelector(".cm-line")));
		fileContent().sendKeys("// edited", Keys.ENTER);

		wait.until(d -> fileTab("Order.java").getDomAttribute("class")
			.contains("antilia-tab-modified"));
		assertFalse(casePill("Files in tabs").getDomAttribute("class")
			.contains("antilia-tab-modified"), "a case has no changes of its own");

		fileContent().sendKeys(Keys.chord(Keys.CONTROL, "s"));
		wait.until(d -> !fileTab("Order.java").getDomAttribute("class")
			.contains("antilia-tab-modified"));
		wait.until(d -> app().findElements(By.cssSelector(".code-editor-demo__status")).stream()
			.anyMatch(status -> status.getText().equals("Saved Order.java.")));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void asksBeforeClosingAFileWithChanges(SeleniumBrowser browser, String framework)
	{
		openEditor(browser, framework);
		selectCase("Files in tabs");
		click(fileContent().findElement(By.cssSelector(".cm-line")));
		fileContent().sendKeys("// not saved");
		wait.until(d -> fileTab("Order.java").getDomAttribute("class")
			.contains("antilia-tab-modified"));

		// another file keeps where the cursor of the first was
		fileTab("words.js").findElement(By.cssSelector(".antilia-tab-button")).click();
		wait.until(d -> fileContent().getText().contains("countWords"));
		fileTab("Order.java").findElement(By.cssSelector(".antilia-tab-button")).click();
		wait.until(d -> fileContent().getText().startsWith("// not saved"));

		fileTab("Order.java").findElement(By.cssSelector(".antilia-tab-close")).click();
		WebElement confirm = wait.until(d -> d.findElement(By.cssSelector(
			".antilia-tabs-confirm .modal-dialog")));
		assertTrue(confirm.getText().contains("Close Order.java?"));
		confirm.findElement(By.cssSelector("[data-confirm='discard']")).click();
		wait.until(d -> fileTabs().stream().noneMatch(tab -> tab.isDisplayed() &&
			tab.getText().contains("Order.java")));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void previewsTheNotesAsTheUserTypes(SeleniumBrowser browser, String framework)
	{
		openNotes(browser, framework);
		WebElement preview = preview();
		assertEquals("Release notes", preview.findElement(By.cssSelector("h1")).getText());
		assertEquals("region", preview.getDomAttribute("role"));
		assertEquals("Preview", preview.getDomAttribute("aria-label"));
		assertEquals(List.of("true", "true", "false"), preview.findElements(By.cssSelector(
			"li input[type=checkbox]")).stream().map(box -> String.valueOf(box.isSelected()))
			.toList());
		assertEquals("antilia-markdown-preview-center", preview.findElements(By.cssSelector(
			"td")).get(1).getDomAttribute("class"));
		wait.until(d -> !preview().findElements(By.cssSelector("pre .tok-typeName")).isEmpty());
		assertEquals("BigDecimal", preview().findElement(By.cssSelector("pre .tok-typeName"))
			.getText());
		// HTML shows as text, and the page's own links open in a new tab
		assertTrue(preview.findElements(By.cssSelector("b")).isEmpty());
		assertTrue(preview.findElement(By.cssSelector("blockquote")).getText()
			.contains("<b>this</b>"));
		assertEquals("_blank", preview.findElement(By.cssSelector("a")).getDomAttribute("target"));

		click(notesContent().findElement(By.cssSelector(".cm-line")));
		notesContent().sendKeys(Keys.HOME, "## Typed", Keys.ENTER, Keys.ENTER);
		wait.until(d -> preview().findElement(By.cssSelector(":scope > :first-child")).getText()
			.equals("Typed"));
		assertEquals("h2", preview().findElement(By.cssSelector(":scope > :first-child"))
			.getTagName());
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void hidesThePreviewAndShowsItAgain(SeleniumBrowser browser, String framework)
	{
		openNotes(browser, framework);
		click(notesContent().findElement(By.cssSelector(".cm-line")));
		notesContent().sendKeys(Keys.HOME, "kept ");
		WebElement split = notes().findElement(By.cssSelector(".antilia-split"));
		assertEquals("Hide the preview", togglePreview().getText().trim());

		togglePreview().click();
		wait.until(d -> notes().findElement(By.cssSelector(".antilia-split"))
			.getDomAttribute("class").contains("antilia-split-collapsed-second"));
		wait.until(d -> togglePreview().getText().trim().equals("Show the preview"));
		wait.until(d -> notesStatus().equals("The preview is hidden: the editor has all the room."));
		assertFalse(preview().isDisplayed());
		// the editor has all the room, and what the user typed
		assertTrue(width(notes().findElement(By.cssSelector(".antilia-code-editor"))) >
			width(split) - 20);
		assertTrue(notesContent().getText().startsWith("kept # Release notes"));

		togglePreview().click();
		wait.until(d -> !notes().findElement(By.cssSelector(".antilia-split"))
			.getDomAttribute("class").contains("antilia-split-collapsed"));
		wait.until(d -> togglePreview().getText().trim().equals("Hide the preview"));
		wait.until(d -> notesStatus().equals("The editor and the preview share the room."));
		assertTrue(preview().isDisplayed());
		assertTrue(preview().getText().startsWith("kept # Release notes"));
		assertNoErrors();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void theServerKeepsTheNotesAndHowTheyShow(SeleniumBrowser browser)
	{
		openNotes(browser, "wicket");
		click(notesContent().findElement(By.cssSelector(".cm-line")));
		notesContent().sendKeys(Keys.HOME, "kept ");
		togglePreview().click();
		wait.until(d -> notesStatus().equals("The preview is hidden: the editor has all the room."));
		WebElement handle = notes().findElement(By.cssSelector(
			".code-editor-demo__split > .antilia-resizable-handle"));
		long height = Math.round(height(notes().findElement(By.cssSelector(
			".code-editor-demo__split")))) + 20;
		handle.sendKeys(Keys.ARROW_DOWN, Keys.ARROW_DOWN);
		wait.until(d -> Math.round(height(notes().findElement(By.cssSelector(
			".code-editor-demo__split")))) == height);

		// a page made anew: the session gives it all back, once the server has the text
		script("return new Promise(function (done) { setTimeout(done, 1500); });");
		driver.get("http://localhost:" + localPort + "/code-editor");
		waitForTheEditors();
		selectCase("Markdown, with its preview");
		wait.until(d -> notesContent().getText().startsWith("kept # Release notes"));
		assertTrue(notes().findElement(By.cssSelector(".antilia-split")).getDomAttribute("class")
			.contains("antilia-split-collapsed-second"));
		assertEquals("Show the preview", togglePreview().getText().trim());
		assertEquals(height, Math.round(height(notes().findElement(By.cssSelector(
			".code-editor-demo__split")))));
		assertNoErrors();
	}

	private void openNotes(SeleniumBrowser browser, String framework)
	{
		openEditor(browser, framework);
		selectCase("Markdown, with its preview");
		wait.until(d -> !preview().findElements(By.cssSelector("h1")).isEmpty());
	}

	private WebElement notes()
	{
		return app().findElement(By.cssSelector(".code-editor-demo__notes"));
	}

	private WebElement notesContent()
	{
		return notes().findElement(By.cssSelector(".cm-content"));
	}

	private WebElement preview()
	{
		return notes().findElement(By.cssSelector(".antilia-markdown-preview"));
	}

	private WebElement togglePreview()
	{
		return notes().findElement(By.cssSelector(".code-editor-demo__actions button[type=button]"));
	}

	private String notesStatus()
	{
		return app().findElement(By.cssSelector(".code-editor-demo__notes + " +
			".code-editor-demo__status")).getText();
	}

	private double width(WebElement element)
	{
		return ((Number)script("return arguments[0].getBoundingClientRect().width;", element))
			.doubleValue();
	}

	private void openEditor(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "code-editor");
		waitForTheEditors();
	}

	/**
	 * Waits for the editors and the grammar of the first.
	 */
	private void waitForTheEditors()
	{
		wait.until(d -> !app().findElements(By.cssSelector(
			".code-editor-demo__editor .cm-editor .tok-keyword")).isEmpty());
	}

	private Object script(String script, Object... arguments)
	{
		return ((JavascriptExecutor)driver).executeScript(script, arguments);
	}

	private WebElement editor()
	{
		return app().findElement(By.cssSelector(".code-editor-demo__editor .antilia-code-editor"));
	}

	private WebElement content()
	{
		return editor().findElement(By.cssSelector(".cm-content"));
	}

	private WebElement controls()
	{
		return app().findElement(By.cssSelector(".code-editor-demo__controls"));
	}

	private WebElement checkBox(String label)
	{
		return controls().findElements(By.cssSelector("label")).stream()
			.filter(each -> each.getText().trim().equals(label)).findFirst().orElseThrow()
			.findElement(By.cssSelector("input"));
	}

	/**
	 * @return a line of the editor of the samples, laid out
	 */
	private WebElement line(int number)
	{
		return content().findElements(By.cssSelector(".cm-line")).get(number - 1);
	}

	private List<String> tokens(String css)
	{
		return editor().findElements(By.cssSelector("." + css)).stream()
			.map(token -> token.getText().trim()).toList();
	}

	private String known()
	{
		return app().findElement(By.cssSelector(".code-editor-demo__editor + " +
			".code-editor-demo__status")).getText();
	}

	private double height(WebElement element)
	{
		return ((Number)script("return arguments[0].getBoundingClientRect().height;", element))
			.doubleValue();
	}

	/**
	 * Clicks an element, at its start, scrolled into sight.
	 */
	private void click(WebElement element)
	{
		script("arguments[0].scrollIntoView({block: 'center'});", element);
		int width = element.getRect().getWidth();
		new Actions(driver).moveToElement(element, -width / 2 + 4, 0).click().perform();
	}

	private List<WebElement> fileTabs()
	{
		return app().findElements(By.cssSelector(".code-editor-demo__files .antilia-tab"));
	}

	private WebElement fileTab(String name)
	{
		return fileTabs().stream().filter(tab -> tab.getText().contains(name)).findFirst()
			.orElseThrow();
	}

	private WebElement casePill(String title)
	{
		return app().findElements(By.cssSelector(".antilia-tab")).stream()
			.filter(tab -> tab.getText().trim().equals(title)).findFirst().orElseThrow();
	}

	/**
	 * @return the text of the editor of the file shown
	 */
	private WebElement fileContent()
	{
		return app().findElements(By.cssSelector(".code-editor-demo__files .cm-content")).stream()
			.filter(WebElement::isDisplayed).findFirst().orElseThrow();
	}
}
