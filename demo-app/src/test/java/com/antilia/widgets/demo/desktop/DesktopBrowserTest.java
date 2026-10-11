/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.antilia.widgets.demo.Dropdowns;
import com.antilia.widgets.demo.JettyTestCase;
import com.antilia.widgets.demo.SeleniumBrowser;

/**
 * Drives {@link DesktopPage} in Chrome and in Firefox, in deployment mode, so through the minified
 * script built from frontend/core: the JSON and bean grids of Wicket and the JSON grids of React
 * and Svelte in their windows, their columns moved by drag and drop, bringing a window to the
 * front, dragging, folding, resizing, minimizing and maximizing it, F11 and Ctrl+F6, the taskbar,
 * dragging it to another side, filling the page, tiling, closing and opening the keyboard window,
 * the menu of the taskbar opening the windows closed, the dialog asking before a window with
 * changes closes, the states the server keeps, and the desktops of the React and Svelte tabs. Runs with {@code -Pselenium}, see
 * {@link SeleniumBrowser}. With {@code -Ddesktop.screenshots=<folder>} it saves screenshots there.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class DesktopBrowserTest extends JettyTestCase
{
	/** The index of the keyboard window, the last one. */
	private static final int KEYBOARD = 7;

	private WebDriver driver;

	private WebDriverWait wait;

	private SeleniumBrowser browser;

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

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void showsTheGridsOfTheGridDemosInWindows(SeleniumBrowser browser) throws IOException
	{
		open(browser);

		// all open
		assertEquals(List.of("JSON grid in Wicket", "Bean grid in Wicket", "JSON grid in React",
			"JSON grid in Svelte", "Dropdowns in Wicket", "Dropdowns in React", "Dropdowns in Svelte",
			"Keyboard"), driver.findElements(
				By.cssSelector(".desktop-demo__desktop > .antilia-desktop .antilia-desktop-task"))
				.stream().filter(WebElement::isDisplayed).map(DesktopBrowserTest::title).toList());
		for (int index = 0; index < 4; index++)
		{
			int window = index;
			wait.until(d -> !window(window).findElements(By.cssSelector("table tbody tr td"))
				.isEmpty());
			assertFalse(window(window).findElements(By.className("antilia-col-mover")).isEmpty(),
				"columns moving by drag and drop");
		}
		assertTrue(window(6).getDomAttribute("data-active") != null, "the last one in front");
		assertTrue(window(5).isDisplayed());
		assertFalse(window(KEYBOARD).isDisplayed(), "the keyboard minimized");
		assertTrue(task(KEYBOARD).getDomAttribute("data-minimized") != null);
		screenshot("wicket");
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void movesAColumnOfAGridInAWindowByDragAndDrop(SeleniumBrowser browser) throws IOException
	{
		open(browser);
		for (int index : new int[] { 0, 1, 2 })
		{
			WebElement window = window(index);
			// in front, from the taskbar: the window brought forward before covers its title bar
			click(task(index));
			wait.until(d -> window.getDomAttribute("data-active") != null);
			wait.until(d -> headers(window).contains("Last Name"));
			assertTrue(headers(window).indexOf("First Name") < headers(window).indexOf("Last Name"));

			WebElement mover = header(window, "First Name").findElement(
				By.className("antilia-col-mover"));
			Rectangle last = header(window, "Last Name").getRect();
			Rectangle from = mover.getRect();
			drag(mover, last.getX() + last.getWidth() - 4 - (from.getX() + from.getWidth() / 2),
				0);

			wait.until(d -> headers(window).indexOf("First Name") >
				headers(window).indexOf("Last Name"));
			assertEquals(index * 40 + 16, left(window), 1, "the window stays where it is");
		}
		screenshot("columns");
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void bringsTheWindowClickedToTheFront(SeleniumBrowser browser)
	{
		open(browser);
		int z = z(window(3));

		click(window(0).findElement(By.cssSelector(".antilia-window-title")));

		wait.until(d -> window(0).getDomAttribute("data-active") != null);
		assertTrue(z(window(0)) > z);
		assertEquals("true", task(0).getDomAttribute("aria-pressed"));
		wait.until(d -> states().contains("JSON grid in Wicket at 16, 16, 900 × 460"));
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void movesAWindowByItsTitleBarFoldsItAndResizesIt(SeleniumBrowser browser)
	{
		open(browser);

		drag(window(0).findElement(By.cssSelector(".antilia-window-title")), 100, 50);
		wait.until(d -> states().contains("JSON grid in Wicket at 116, 66, 900 × 460"));
		assertEquals(116, left(window(0)), 1);

		new Actions(driver).doubleClick(window(0).findElement(By.cssSelector(
			".antilia-window-title"))).perform();
		wait.until(d -> states().contains("JSON grid in Wicket at 116, 66, 900 × 460, folded"));
		assertTrue(height(window(0)) < 60, "only the title bar");
		new Actions(driver).doubleClick(window(0).findElement(By.cssSelector(
			".antilia-window-title"))).perform();
		wait.until(d -> !states().contains("folded"));

		drag(window(0).findElement(By.cssSelector(".antilia-window-handle-se")), -100, -80);
		wait.until(d -> states().contains("JSON grid in Wicket at 116, 66, 800 × 380"));
		assertEquals(800, width(window(0)), 1);
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void minimizesToTheTaskbarAndBringsBackFromIt(SeleniumBrowser browser)
	{
		open(browser);

		click(window(6).findElement(By.cssSelector(".antilia-window-control-minimize")));
		wait.until(d -> !window(6).isDisplayed());
		assertTrue(window(5).getDomAttribute("data-active") != null, "the one behind");
		wait.until(d -> states().contains("Dropdowns in Svelte at 320, 140, 700 × 460, minimized"));

		click(task(6));
		wait.until(d -> window(6).isDisplayed());
		assertTrue(window(6).getDomAttribute("data-active") != null);
		// the button of the window in front minimizes it
		click(task(6));
		wait.until(d -> !window(6).isDisplayed());
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void maximizesWithItsButtonOrF11AndTheTaskbarKeepsItMaximized(SeleniumBrowser browser)
		throws IOException
	{
		open(browser);
		WebElement area = area();

		click(window(0).findElement(By.cssSelector(".antilia-window-control-maximize")));
		wait.until(d -> window(0).getDomAttribute("data-maximized") != null);
		assertEquals(width(area), width(window(0)), 1);
		wait.until(d -> states().contains("JSON grid in Wicket at 16, 16, 900 × 460, maximized"));

		// another window brought to the front from the taskbar takes its place, maximized
		click(task(1));
		wait.until(d -> window(1).getDomAttribute("data-maximized") != null);
		assertTrue(window(1).getDomAttribute("data-active") != null);

		// F11 in the window the focus is in restores it
		focusIn(window(1)).sendKeys(Keys.F11);
		wait.until(d -> window(1).getDomAttribute("data-maximized") == null);
		focusIn(window(1)).sendKeys(Keys.F11);
		wait.until(d -> window(1).getDomAttribute("data-maximized") != null);
		screenshot("maximized");
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void goesRoundTheWindowsWithCtrlF6(SeleniumBrowser browser)
	{
		open(browser);
		WebElement field = focusIn(window(6));

		// to the keyboard, minimized
		field.sendKeys(Keys.chord(Keys.CONTROL, Keys.F6));
		wait.until(d -> window(KEYBOARD).getDomAttribute("data-active") != null);
		assertTrue(window(KEYBOARD).isDisplayed(), "restored");

		driver.switchTo().activeElement().sendKeys(Keys.chord(Keys.CONTROL, Keys.F6));
		wait.until(d -> window(0).getDomAttribute("data-active") != null);
		driver.switchTo().activeElement().sendKeys(Keys.chord(Keys.CONTROL, Keys.SHIFT, Keys.F6));
		wait.until(d -> window(KEYBOARD).getDomAttribute("data-active") != null);
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void tilesTheWindowsShown(SeleniumBrowser browser) throws IOException
	{
		open(browser);
		WebElement area = area();

		click(tile("columns"));
		wait.until(d -> left(window(0)) == 0);
		double seventh = width(area) / 7;
		assertEquals(seventh, width(window(0)), 1);
		assertEquals(seventh, left(window(1)), 1);
		assertEquals(6 * seventh, left(window(6)), 1);
		assertFalse(window(KEYBOARD).isDisplayed(), "minimized, left out");

		// three in the first two rows, one in the last
		click(tile("grid"));
		wait.until(d -> Math.abs(width(window(6)) - width(area)) < 2);
		assertEquals(width(area) / 3, width(window(0)), 1);
		assertEquals(0, left(window(3)), 1);
		assertEquals(width(area) / 3, left(window(4)), 1);
		assertEquals(0, left(window(6)), 1);
		screenshot("tiled");
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void closesTheKeyboardWindowAndOpensItAgain(SeleniumBrowser browser)
	{
		open(browser);
		click(task(KEYBOARD));
		wait.until(d -> window(KEYBOARD).isDisplayed());
		// big enough for all the shortcuts
		WebElement body = window(KEYBOARD).findElement(By.className("antilia-window-body"));
		assertEquals(Boolean.FALSE, ((JavascriptExecutor)driver).executeScript(
			"return arguments[0].scrollHeight > arguments[0].clientHeight;", body),
			"the shortcuts fit");

		// nothing to lose: it closes at once
		click(window(KEYBOARD).findElement(By.cssSelector(".antilia-window-control-close")));
		wait.until(d -> !window(KEYBOARD).isDisplayed());
		assertFalse(task(KEYBOARD).isDisplayed());
		assertTrue(driver.findElements(By.className("antilia-desktop-confirm")).isEmpty());
		wait.until(d -> states().contains("Keyboard at 560, 40, 600 × 480, closed"));

		click(driver.findElement(By.cssSelector("form.desktop-demo__options button")));
		wait.until(d -> window(KEYBOARD).isDisplayed());
		assertTrue(window(KEYBOARD).getDomAttribute("data-active") != null);
		wait.until(d -> !states().contains("Keyboard at 560, 40, 600 × 480, closed"));
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void keepsTheStatesWhenTheDesktopIsRenderedAgain(SeleniumBrowser browser)
	{
		open(browser);
		drag(window(0).findElement(By.cssSelector(".antilia-window-title")), 60, 40);
		wait.until(d -> states().contains("JSON grid in Wicket at 76, 56"));

		new org.openqa.selenium.support.ui.Select(driver.findElement(By.cssSelector(
			"form.desktop-demo__options select"))).selectByIndex(2);
		wait.until(d -> driver.findElement(By.cssSelector(".desktop-demo__desktop > .antilia-desktop"))
			.getDomAttribute("class").contains("antilia-desktop-taskbar-left"));
		wait.until(d -> !driver.findElements(By.cssSelector(
			".desktop-demo__desktop .antilia-desktop-task")).isEmpty());

		assertEquals(76, left(window(0)), 1);
		assertTrue(window(0).getDomAttribute("data-active") != null, "in front, as it was");

		driver.navigate().refresh();
		waitForTheScript();
		assertEquals(76, left(window(0)), 1);
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void dragsTheTaskbarToAnotherSideTheWindowsStayingOnTheDesktop(SeleniumBrowser browser)
		throws IOException
	{
		open(browser);
		WebElement grip = desktop().findElement(By.className("antilia-desktop-grip"));
		double height = rect(desktop()).get("height");

		drag(grip, 0, (int)-(height - 30));

		wait.until(d -> desktop().getDomAttribute("class").contains("antilia-desktop-taskbar-top"));
		// the server knows, and the choice above the desktop follows
		wait.until(d -> "top".equals(new org.openqa.selenium.support.ui.Select(driver.findElement(
			By.cssSelector("form.desktop-demo__options select"))).getFirstSelectedOption()
			.getDomAttribute("value")));
		wait.until(d -> states().contains("taskbar above;"));
		screenshot("taskbar-top");

		focusOn(desktop().findElement(By.className("antilia-desktop-grip"))).sendKeys(Keys.ARROW_RIGHT);
		wait.until(d -> desktop().getDomAttribute("class").contains("antilia-desktop-taskbar-right"));
		WebElement area = area();
		for (int index = 0; index < 4; index++)
		{
			assertTrue(left(window(index)) + width(window(index)) <= width(area) + 1,
				"window " + index + " on the desktop");
		}
		wait.until(d -> states().contains("taskbar on the right;"));

		driver.navigate().refresh();
		waitForTheScript();
		assertTrue(desktop().getDomAttribute("class").contains("antilia-desktop-taskbar-right"));
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void fillsThePageAndGoesBackIntoIt(SeleniumBrowser browser) throws IOException
	{
		open(browser);
		WebElement expand = desktop().findElement(By.className("antilia-desktop-expand"));

		click(expand);

		wait.until(d -> desktop().getDomAttribute("data-expanded") != null);
		@SuppressWarnings("unchecked")
		Map<String, Number> page = (Map<String, Number>)((JavascriptExecutor)driver).executeScript(
			"var r = arguments[0].getBoundingClientRect();" +
				"return {left: r.left, top: r.top, width: r.width, height: r.height," +
				" innerWidth: document.documentElement.clientWidth, innerHeight: window.innerHeight};",
			desktop());
		assertEquals(0, page.get("left").doubleValue(), 1);
		assertEquals(0, page.get("top").doubleValue(), 1);
		assertEquals(page.get("innerWidth").doubleValue(), page.get("width").doubleValue(), 1);
		assertEquals(page.get("innerHeight").doubleValue(), page.get("height").doubleValue(), 1);
		screenshot("expanded");

		// as the user left it, after a reload too
		wait.until(d -> states().contains("filling the page"));
		driver.navigate().refresh();
		waitForTheScript();
		assertTrue(desktop().getDomAttribute("data-expanded") != null);

		focusOn(desktop().findElement(By.className("antilia-desktop-expand")))
			.sendKeys(Keys.ESCAPE);
		wait.until(d -> desktop().getDomAttribute("data-expanded") == null);
		assertEquals(Boolean.FALSE, ((JavascriptExecutor)driver).executeScript(
			"return document.documentElement.classList.contains('antilia-desktop-expanded');"));
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void aDialogOfTheGridInAWindowShowsOverEverything(SeleniumBrowser browser) throws IOException
	{
		open(browser);
		WebElement react = window(2);
		wait.until(d -> !react.findElements(By.cssSelector("table.ant-data-table tbody tr"))
			.isEmpty());

		// in front first, from the taskbar: the windows after it cover the button
		click(task(2));
		wait.until(d -> react.getDomAttribute("data-active") != null);
		click(react.findElement(By.cssSelector("button[title='New user']")));
		WebElement dialog = wait.until(d -> d.findElement(By.cssSelector(".modal-dialog")));
		wait.until(d -> dialog.isDisplayed());

		assertTrue(react.getDomAttribute("data-active") != null);
		assertEquals(Boolean.TRUE, ((JavascriptExecutor)driver).executeScript(
			"var r = arguments[0].getBoundingClientRect();" +
				"var top = document.elementFromPoint(r.left + r.width / 2, r.top + 20);" +
				"return arguments[0].contains(top);", dialog), "nothing covers the dialog");
		screenshot("dialog");
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void theReactAndSvelteTabsHaveADesktopOfTheirOwn(SeleniumBrowser browser) throws IOException
	{
		open(browser);
		for (String framework : new String[] { "react", "svelte" })
		{
			driver.findElement(By.id("framework-tab-" + framework)).click();
			WebElement panel = driver.findElement(By.id("framework-panel-" + framework));
			wait.until(d -> panel.findElements(By.cssSelector(".antilia-desktop-task")).size() == 10);
			wait.until(d -> !panel.findElements(By.cssSelector(
				".antilia-window table.ant-data-table tbody tr")).isEmpty());
			// six more demos of the framework, each in a window of its own
			assertEquals(List.of(framework.equals("react") ? "JSON grid in React" :
				"JSON grid in Svelte", "Notes", framework.equals("react") ? "Dropdowns in React" :
					"Dropdowns in Svelte", "Autocomplete", "Buttons", "Date and time pickers",
				"JSON Infinite List", "Modal Dialog", "Progress Bar", "Keyboard"),
				panel.findElements(By.cssSelector(".antilia-desktop-task")).stream()
					.map(DesktopBrowserTest::title).toList());
			for (String demo : new String[] { "autocomplete", "buttons", "datePicker", "jsonList",
				"modalDialog", "progressBar" })
			{
				WebElement window = panel.findElement(By.id(framework + "-desktop-" + demo));
				assertTrue(window.isDisplayed(), demo);
				wait.withMessage(demo).until(d -> !window.findElements(By.cssSelector(
					".antilia-window-body button, .antilia-window-body input, .antilia-window-body a"))
					.isEmpty());
			}
			WebElement progress = panel.findElement(By.id(framework + "-desktop-progressBar"));
			assertTrue(progress.getDomAttribute("data-active") != null, "the last one in front");
			screenshot(framework + "-windows");
			WebElement users = panel.findElement(By.id(framework + "-desktop-users"));
			WebElement dropdowns = panel.findElement(By.id(framework + "-desktop-dropdowns"));

			// in front from the taskbar: the windows after it cover its title bar
			click(panel.findElement(By.cssSelector(".antilia-desktop-task[data-window='" +
				framework + "-desktop-users']")));
			wait.until(d -> users.getDomAttribute("data-active") != null);
			drag(users.findElement(By.cssSelector(".antilia-window-title")), 40, 30);
			assertEquals(56, left(users), 1);

			WebElement keyboard = panel.findElement(By.id(framework + "-desktop-keyboard"));
			assertFalse(keyboard.isDisplayed());
			click(panel.findElement(By.cssSelector(".desktop-demo__options button")));
			wait.until(d -> keyboard.isDisplayed());

			// choices not sent are changes; the keyboard came in front, the drop-downs back
			click(panel.findElement(By.cssSelector(".antilia-desktop-task[data-window='" +
				framework + "-desktop-dropdowns']")));
			wait.until(d -> dropdowns.getDomAttribute("data-active") != null);
			WebElement size = wait.until(d -> dropdowns.findElement(By.name("size")));
			Dropdowns.choose(driver, size, 2);
			wait.until(d -> "true".equals(dropdowns.findElement(By.className("dropdown-demo"))
				.getDomAttribute("data-modified")));
			click(dropdowns.findElement(By.cssSelector(".antilia-window-control-close")));
			click(wait.until(d -> d.findElement(By.cssSelector(
				".antilia-desktop-confirm [data-confirm='discard']"))));
			wait.until(d -> !dropdowns.isDisplayed());
			// the choices are back as they were
			assertEquals("false", dropdowns.findElement(By.className("dropdown-demo"))
				.getDomAttribute("data-modified"));
			// open again from the menu of the taskbar
			click(panel.findElement(By.className("antilia-desktop-menu-button")));
			click(menuItem(panel, "Dropdowns in " + (framework.equals("react") ? "React" : "Svelte")));
			wait.until(d -> dropdowns.isDisplayed());
			screenshot(framework);
		}
		// the remembered tab would open the next demo there
		driver.findElement(By.id("framework-tab-wicket")).click();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void opensTheWindowsClosedFromTheMenuOfTheTaskbar(SeleniumBrowser browser) throws IOException
	{
		open(browser);
		// closed without asking, nothing changed in it
		click(task(5));
		wait.until(d -> window(5).getDomAttribute("data-active") != null);
		click(window(5).findElement(By.cssSelector(".antilia-window-control-close")));
		wait.until(d -> !window(5).isDisplayed());
		assertTrue(driver.findElements(By.className("antilia-desktop-confirm")).isEmpty());
		assertFalse(task(5).isDisplayed(), "off the taskbar");
		WebElement menuButton = desktop().findElement(By.className("antilia-desktop-menu-button"));

		click(menuButton);

		WebElement menu = wait.until(d -> desktop().findElement(By.className("antilia-desktop-menu")));
		assertEquals("true", menuButton.getDomAttribute("aria-expanded"));
		assertEquals(List.of("JSON grid in Wicket", "Bean grid in Wicket", "JSON grid in React",
			"JSON grid in Svelte", "Dropdowns in Wicket", "Dropdowns in Svelte", "Keyboard"),
			items(menu, "open"));
		assertEquals(List.of("Dropdowns in React"), items(menu, "closed"));
		assertTrue(menu.isDisplayed());
		assertEquals(Boolean.TRUE, ((JavascriptExecutor)driver).executeScript(
			"var r = arguments[0].getBoundingClientRect();" +
				"return arguments[0].contains(document.elementFromPoint(r.left + 10, r.top + 10));",
			menu), "nothing covers the menu");
		screenshot("menu");

		click(menuItem(desktop(), "Dropdowns in React"));

		wait.until(d -> window(5).isDisplayed());
		assertTrue(desktop().findElements(By.className("antilia-desktop-menu")).isEmpty());
		assertTrue(window(5).getDomAttribute("data-active") != null);
		assertTrue(task(5).isDisplayed());
		wait.until(d -> !window(5).findElements(By.cssSelector(".antilia-dropdown-toggle")).isEmpty());
		wait.until(d -> !states().contains("Dropdowns in React at 280, 100, 700 × 460, closed"));

		// with the keyboard: Escape closes it again, giving its button the focus
		focusOn(desktop().findElement(By.className("antilia-desktop-menu-button")))
			.sendKeys(Keys.ARROW_DOWN);
		wait.until(d -> !desktop().findElements(By.className("antilia-desktop-menu")).isEmpty());
		driver.switchTo().activeElement().sendKeys(Keys.ESCAPE);
		wait.until(d -> desktop().findElements(By.className("antilia-desktop-menu")).isEmpty());
		assertEquals("antilia-desktop-menu-button",
			driver.switchTo().activeElement().getDomAttribute("class"));
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void asksBeforeClosingAWindowWithChoicesNotSent(SeleniumBrowser browser) throws IOException
	{
		open(browser);
		WebElement dropdowns = window(4);
		click(task(4));
		wait.until(d -> dropdowns.getDomAttribute("data-active") != null);
		WebElement size = dropdowns.findElement(By.name("size"));
		Dropdowns.choose(driver, size, 2);
		wait.until(d -> choices(dropdowns).startsWith("Size L,"));

		click(dropdowns.findElement(By.cssSelector(".antilia-window-control-close")));

		WebElement dialog = wait.until(d -> d.findElement(By.cssSelector(
			".antilia-desktop-confirm .modal-dialog")));
		assertEquals("Close Dropdowns in Wicket?", dialog.findElement(
			By.className("modal-dialog-title")).getText());
		assertEquals(Boolean.TRUE, ((JavascriptExecutor)driver).executeScript(
			"var r = arguments[0].getBoundingClientRect();" +
				"return arguments[0].contains(document.elementFromPoint(r.left + r.width / 2," +
				" r.top + 20));", dialog), "nothing covers the dialog");
		screenshot("confirm");

		click(dialog.findElement(By.cssSelector("[data-confirm='keep']")));
		wait.until(d -> d.findElements(By.className("antilia-desktop-confirm")).isEmpty());
		assertTrue(dropdowns.isDisplayed(), "kept open, with its choices");
		// the third size, L
		assertEquals("2", size.getDomProperty("value"));

		click(dropdowns.findElement(By.cssSelector(".antilia-window-control-close")));
		click(wait.until(d -> d.findElement(By.cssSelector(
			".antilia-desktop-confirm [data-confirm='discard']"))));
		wait.until(d -> !dropdowns.isDisplayed());
		wait.until(d -> states().contains("Dropdowns in Wicket at 240, 60, 700 × 460, closed"));

		// open again, its choices as they were: the server knows too
		click(desktop().findElement(By.className("antilia-desktop-menu-button")));
		click(menuItem(desktop(), "Dropdowns in Wicket"));
		wait.until(d -> dropdowns.isDisplayed());
		assertEquals("1", size.getDomProperty("value"));
		wait.until(d -> choices(dropdowns).startsWith("Size M,"));

		// the grids lose nothing but a user being edited: searching closes at once
		WebElement bean = window(1);
		click(task(1));
		WebElement filter = bean.findElement(By.cssSelector("input[type='search']"));
		filter.sendKeys("an");
		click(bean.findElement(By.cssSelector(".antilia-window-control-close")));
		wait.until(d -> !bean.isDisplayed());
		assertTrue(driver.findElements(By.className("antilia-desktop-confirm")).isEmpty());
	}

	private void open(SeleniumBrowser browser)
	{
		this.browser = browser;
		driver = browser.newDriver(1280, 1100);
		wait = new WebDriverWait(driver, Duration.ofSeconds(15), Duration.ofMillis(20));
		wait.ignoring(StaleElementReferenceException.class);
		driver.get("http://localhost:" + localPort + "/desktop");
		waitForTheScript();
	}

	/**
	 * Waits for the script to have filled the taskbar of the Wicket desktop.
	 */
	private void waitForTheScript()
	{
		wait.until(d -> d.findElements(By.cssSelector(
			".desktop-demo__desktop > .antilia-desktop .antilia-desktop-task")).size() == 8);
	}

	/**
	 * @return the titles of the windows of a group of the menu of the taskbar, open or closed
	 */
	private static List<String> items(WebElement menu, String group)
	{
		return menu.findElements(By.cssSelector(".antilia-desktop-menu-group[data-group='" + group +
			"'] .antilia-desktop-menu-item")).stream().map(DesktopBrowserTest::title).toList();
	}

	/**
	 * @return the title of a button of the taskbar or of an item of its menu, without the letter
	 *         of the icon
	 */
	private static String title(WebElement element)
	{
		return element.findElement(By.className("antilia-desktop-task-title")).getText();
	}

	private WebElement menuItem(WebElement container, String title)
	{
		return wait.until(d -> container.findElements(By.className("antilia-desktop-menu-item"))
			.stream().filter(item -> title(item).equals(title)).findFirst().orElse(null));
	}

	private static String choices(WebElement window)
	{
		return window.findElement(By.className("dropdown-demo__choices")).getText();
	}

	/**
	 * @return a window of the Wicket desktop, in the order of the page
	 */
	private WebElement window(int index)
	{
		return driver.findElements(By.cssSelector(
			".desktop-demo__desktop > .antilia-desktop > .antilia-desktop-area > .antilia-window"))
			.get(index);
	}

	private WebElement task(int index)
	{
		return driver.findElements(By.cssSelector(
			".desktop-demo__desktop > .antilia-desktop .antilia-desktop-task")).get(index);
	}

	private WebElement desktop()
	{
		return driver.findElement(By.cssSelector(".desktop-demo__desktop > .antilia-desktop"));
	}

	/**
	 * @return the texts of the headers of the grid in a window
	 */
	private List<String> headers(WebElement window)
	{
		return window.findElements(By.cssSelector("tr.ant-data-table-headers > th")).stream()
			.map(header -> header.getText().replaceAll("[▲▼]", "").trim())
			.toList();
	}

	private WebElement header(WebElement window, String text)
	{
		return window.findElements(By.cssSelector("tr.ant-data-table-headers > th")).stream()
			.filter(header -> header.getText().replaceAll("[▲▼]", "").trim().equals(text))
			.findFirst()
			.orElseThrow();
	}

	private WebElement area()
	{
		return driver.findElement(By.cssSelector(".desktop-demo__desktop .antilia-desktop-area"));
	}

	private WebElement tile(String layout)
	{
		return driver.findElement(By.cssSelector(".desktop-demo__desktop .antilia-desktop-tile-" +
			layout));
	}

	private String states()
	{
		return driver.findElement(By.cssSelector(".desktop-demo__states")).getText();
	}

	private void click(WebElement element)
	{
		((JavascriptExecutor)driver).executeScript(
			"arguments[0].scrollIntoView({block: 'nearest'});", element);
		new Actions(driver).moveToElement(element).click().perform();
	}

	private void drag(WebElement element, int dx, int dy)
	{
		((JavascriptExecutor)driver).executeScript(
			"arguments[0].scrollIntoView({block: 'center'});", element);
		new Actions(driver).moveToElement(element)
			.clickAndHold()
			.moveByOffset(dx / 2, dy / 2)
			.moveByOffset(dx - dx / 2, dy - dy / 2)
			.release()
			.perform();
	}

	/**
	 * Gives the focus to the window itself, as a click in it would to a field.
	 *
	 * @return the element having the focus
	 */
	private WebElement focusIn(WebElement window)
	{
		return focusOn(window);
	}

	private WebElement focusOn(WebElement element)
	{
		((JavascriptExecutor)driver).executeScript("arguments[0].focus();", element);
		return driver.switchTo().activeElement();
	}

	private int z(WebElement window)
	{
		return Integer.parseInt(window.getCssValue("z-index"));
	}

	private double left(WebElement window)
	{
		return rect(window).get("left");
	}

	private double width(WebElement element)
	{
		return rect(element).get("width");
	}

	private double height(WebElement element)
	{
		return rect(element).get("height");
	}

	/**
	 * @return where an element is, from the left of the area it is in, and its size, in pixels
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Double> rect(WebElement element)
	{
		Map<String, Number> rect = (Map<String, Number>)((JavascriptExecutor)driver).executeScript(
			"var r = arguments[0].getBoundingClientRect();" +
				"var area = arguments[0].closest('.antilia-desktop-area');" +
				"var a = area && area !== arguments[0] ? area.getBoundingClientRect() : {left: 0};" +
				"return {left: r.left - a.left - (area && area !== arguments[0] ? area.clientLeft : 0)," +
				" width: r.width, height: r.height};", element);
		return Map.of("left", rect.get("left").doubleValue(), "width",
			rect.get("width").doubleValue(), "height", rect.get("height").doubleValue());
	}

	private void screenshot(String name) throws IOException
	{
		String folder = System.getProperty("desktop.screenshots");
		if (folder == null)
		{
			return;
		}
		Path path = Path.of(folder, "desktop-" + name + "-" + browser.name().toLowerCase() +
			".png");
		Files.createDirectories(path.getParent());
		Files.write(path, ((TakesScreenshot)driver).getScreenshotAs(OutputType.BYTES));
	}
}
