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
package com.antiliacode.widgets.demo.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.DemoCases;
import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.table.ResizableColumnsBehavior.Mode;
import com.antiliacode.widgets.tree.JsonBasedTree;
import com.antiliacode.widgets.tree.JsonBasedTreeGrid;
import com.antiliacode.widgets.tree.TreeGrid;

/**
 * Shows the tree of life in the tree grids of {@link TreeGridPage}, with the scientific names,
 * the ranks, the periods and the times, and the cards of what is selected next to them.
 */
class TreeGridPageTest
{
	/** The tree grid of Wicket components, in its case. */
	private static final String TREE_GRID = "themed:cases:" +
		DemoCases.pathOf("case.treeGrid") + ":treeGrid";

	/** The tree grid rendered from JSON, in its case. */
	private static final String JSON_TREE_GRID = "themed:cases:" +
		DemoCases.pathOf("case.jsonTreeGrid") + ":jsonTreeGrid";

	private WicketTester tester;

	@BeforeEach
	void startPage()
	{
		tester = new WicketTester(new WicketApplication());
		tester.startPage(TreeGridPage.class);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	@Test
	void showsTheTreeOfLifeInBothTreeGridsWithTheirColumns()
	{
		tester.assertComponent(TREE_GRID, TreeGrid.class);
		tester.assertComponent(JSON_TREE_GRID, JsonBasedTreeGrid.class);
		tester.assertContains(">Scientific name<");
		tester.assertContains(">Period<");
		tester.assertContains(">Theropods<");
		tester.assertContains(">Dinosauria<");
		tester.assertContains(Pattern.quote("\"table\":{\"header\":\"Name\",\"columns\":" +
			"[{\"header\":\"Scientific name\",\"template\":\"{{scientific}}\"," +
			"\"cssClass\":\"tree-demo-scientific\"},{\"header\":\"Rank\"," +
			"\"template\":\"{{rank}}\"}"));
	}

	@Test
	void aSpeciesSelectedInTheJsonTreeGridIsShownNextToIt()
	{
		JsonBasedTree<?, ?> table = (JsonBasedTree<?, ?>)tester.getComponentFromLastRenderedPage(
			JSON_TREE_GRID);
		tester.getRequest().getPostParameters().setParameterValue("action",
			JsonBasedTree.SELECT_ACTION);
		tester.getRequest().getPostParameters().setParameterValue("key", "triceratops");
		tester.executeBehavior(table.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));

		tester.assertComponentOnAjaxResponse("themed:details");
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains(">Triceratops horridus<"), response);
		assertTrue(response.contains(">Ornithischia<"), "the card of its group: " + response);
	}

	@Test
	void theDropdownChoosesHowTheColumnsOfBothTreeGridsAreResized()
	{
		TreeGrid<?, ?> treeGrid = (TreeGrid<?, ?>)tester.getComponentFromLastRenderedPage(
			TREE_GRID);
		JsonBasedTreeGrid<?, ?> jsonTreeGrid = (JsonBasedTreeGrid<?, ?>)tester
			.getComponentFromLastRenderedPage(JSON_TREE_GRID);
		assertEquals(Mode.FIT, treeGrid.getResizableColumns());
		tester.assertContains(Pattern.quote(">keeping the table&#039;s width<"));

		tester.getRequest().getPostParameters().setParameterValue(
			((FormComponent<?>)tester.getComponentFromLastRenderedPage("themed:resize"))
				.getInputName(), "stretch");
		tester.executeAjaxEvent("themed:resize", "change");

		assertEquals(Mode.STRETCH, treeGrid.getResizableColumns());
		assertEquals(Mode.STRETCH, jsonTreeGrid.getResizableColumns());
		tester.assertComponentOnAjaxResponse(TREE_GRID);
		tester.assertComponentOnAjaxResponse(JSON_TREE_GRID);
		tester.assertContains(Pattern.quote("\"resizableColumns\":\"stretch\""));

		tester.getRequest().getPostParameters().setParameterValue(
			((FormComponent<?>)tester.getComponentFromLastRenderedPage("themed:resize"))
				.getInputName(), "none");
		tester.executeAjaxEvent("themed:resize", "change");

		assertNull(treeGrid.getResizableColumns());
		assertNull(jsonTreeGrid.getResizableColumns());
	}

	@Test
	void theColumnsAreInGermanToo()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		tester.startPage(TreeGridPage.class);

		tester.assertContains(">Wissenschaftlicher Name<");
		tester.assertContains(">Rang<");
		tester.assertContains(">Trias – heute<");
		tester.assertContains(Pattern.quote("\"header\":\"Periode\""));
	}
}
