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
package com.antiliacode.widgets.demo.split;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.split.SplitOrientation;
import com.antiliacode.widgets.split.SplitPane;
import com.antiliacode.widgets.split.SplitPanel;
import com.antiliacode.widgets.split.SplitSize;
import com.antiliacode.widgets.split.SplitState;
import com.github.openjson.JSONObject;

/**
 * Checks {@link SplitPanelPage}: the layout of two split panels keeping what the user changes, the
 * files' in the session and the editor's in the page, the check boxes switching the third split
 * panel and the configuration of the React and Svelte apps.
 */
class SplitPanelPageTest
{
	private static final String LAYOUT = "themed:layout";

	private static final String EDITOR = LAYOUT + ":split:second";

	private static final String SETTINGS = "themed:settings";

	private static final String START =
		"The files take 220 px of the width, the editor 65 % of the height.";

	private WicketTester tester;

	private SplitPanelPage page;

	@BeforeEach
	void startPage()
	{
		tester = new WicketTester(new WicketApplication());
		page = tester.startPage(SplitPanelPage.class);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	private SplitPanel split(String path)
	{
		return (SplitPanel)tester.getComponentFromLastRenderedPage(path);
	}

	private void send(String path, String position, String collapsed)
	{
		tester.getRequest().setParameter("position", position);
		tester.getRequest().setParameter("collapsed", collapsed);
		tester.executeBehavior(split(path).get("split")
			.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));
	}

	@Test
	void showsTheFilesBesideTheEditorAboveItsConsole()
	{
		tester.assertContains("Wicket.SplitPanel.attach\\(\"" + split(LAYOUT).getSplitMarkupId() +
			"\", \\{\"orientation\":\"horizontal\",\"position\":\"220px\",\"resizable\":true," +
			"\"collapsible\":true,\"min\":\"140px\",\"max\":50");
		tester.assertContains("Wicket.SplitPanel.attach\\(\"" + split(EDITOR).getSplitMarkupId() +
			"\", \\{\"orientation\":\"vertical\",\"position\":65");
		tester.assertContains("\"minSecond\":\"60px\"");
		tester.assertContains("HomePageTest.java");
		tester.assertContains("BUILD SUCCESS");
		tester.assertLabel("themed:sizes", START);
	}

	@Test
	void followsWhatTheUserChanges()
	{
		send(LAYOUT, "300.4px", "");
		tester.assertComponentOnAjaxResponse("themed:sizes");
		send(EDITOR, "50", "");

		assertEquals(new SplitState(SplitSize.pixels(300.4), null), page.getFilesState());
		assertEquals(new SplitState(SplitSize.percent(50), null), page.getEditorState());
		tester.assertContains("The files take 300 px of the width, the editor 50 % of the height\\.");

		send(EDITOR, "50", "second");
		tester.assertComponentOnAjaxResponse("themed:sizes");
		send(LAYOUT, "300.4px", "first");
		tester.assertContains("The files take none of the width, the editor all of the height\\.");
	}

	@Test
	void keepsTheFilesInTheSessionAndTheEditorInThePage()
	{
		send(LAYOUT, "260px", "");
		send(EDITOR, "40", "");

		SplitPanelPage again = tester.startPage(SplitPanelPage.class);

		assertEquals(SplitSize.pixels(260), again.getFilesState().getPosition());
		assertEquals(SplitSize.percent(65), again.getEditorState().getPosition());
		assertEquals(new SplitState(SplitSize.pixels(260), null), tester.getSession()
			.getAttribute(SplitPanelPage.LAYOUT_ATTRIBUTE));
	}

	@Test
	void theCheckBoxesSwitchTheThirdSplitPanel()
	{
		assertTrue(split(SETTINGS).isCollapsible());

		FormTester form = tester.newFormTester("themed:form", false);
		form.setValue("vertical", true);
		tester.executeAjaxEvent("themed:form:vertical", "change");

		assertTrue(page.isVertical());
		assertEquals(SplitOrientation.VERTICAL, split(SETTINGS).getOrientation());
		tester.assertComponentOnAjaxResponse(SETTINGS);
		tester.assertContains("antilia-split-vertical");

		form = tester.newFormTester("themed:form", false);
		form.setValue("vertical", true);
		form.setValue("resizable", false);
		tester.executeAjaxEvent("themed:form:resizable", "change");

		assertFalse(page.isResizable());
		assertFalse(split(SETTINGS).isResizable());
		tester.assertContains("antilia-split-fixed");

		send(SETTINGS, "50", "first");
		form = tester.newFormTester("themed:form", false);
		form.setValue("vertical", true);
		form.setValue("resizable", true);
		form.setValue("collapsible", false);
		tester.executeAjaxEvent("themed:form:collapsible", "change");

		assertFalse(page.isCollapsible());
		assertFalse(split(SETTINGS).isCollapsible());
		assertNull(split(SETTINGS).getCollapsed());
	}

	@Test
	void givesTheAppsTheContentAndWhereTheDividersAre()
	{
		send(LAYOUT, "200px", "");
		send(EDITOR, "70", "second");

		JSONObject config = page.getFrameworkConfig();
		JSONObject layout = config.getJSONObject("layout");

		assertEquals("splitPanel", page.getFrameworkDemo());
		assertEquals("200px", layout.getJSONObject("files").getString("position"));
		assertTrue(layout.getJSONObject("files").isNull("collapsed"));
		assertEquals("50", layout.getJSONObject("files").get("max").toString().replace(".0", ""));
		assertEquals(70, layout.getJSONObject("editor").getDouble("position"));
		assertEquals(SplitPane.SECOND.cssName(), layout.getJSONObject("editor")
			.getString("collapsed"));
		assertEquals("60px", layout.getJSONObject("editor").getString("minSecond"));
		assertEquals(SplitPanelPage.FILES.size(), config.getJSONArray("files").length());
		JSONObject texts = config.getJSONObject("texts");
		assertEquals("Drag, or use the arrow keys, to resize the panes", texts.getString("divider"));
		assertEquals("Hide the left pane", texts.getString("collapseLeft"));
		assertEquals("Show both panes", texts.getString("restore"));
		assertFalse(config.getBoolean("vertical"));
		assertTrue(config.getBoolean("resizable"));
		assertTrue(config.getBoolean("collapsible"));
	}
}
