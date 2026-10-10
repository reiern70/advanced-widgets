/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.tree;

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

import com.antilia.widgets.demo.WicketApplication;
import com.antilia.widgets.table.ResizableColumnsBehavior.Mode;
import com.antilia.widgets.tree.JsonBasedTree;
import com.antilia.widgets.tree.JsonBasedTreeGrid;
import com.antilia.widgets.tree.TreeGrid;

/**
 * Shows the tree of life in the tree grids of {@link TreeGridPage}, with the scientific names,
 * the ranks, the periods and the times, and the cards of what is selected next to them.
 */
class TreeGridPageTest
{
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
		tester.assertComponent("themed:treeGrid", TreeGrid.class);
		tester.assertComponent("themed:jsonTreeGrid", JsonBasedTreeGrid.class);
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
			"themed:jsonTreeGrid");
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
			"themed:treeGrid");
		JsonBasedTreeGrid<?, ?> jsonTreeGrid = (JsonBasedTreeGrid<?, ?>)tester
			.getComponentFromLastRenderedPage("themed:jsonTreeGrid");
		assertEquals(Mode.FIT, treeGrid.getResizableColumns());
		tester.assertContains(Pattern.quote(">keeping the table&#039;s width<"));

		tester.getRequest().getPostParameters().setParameterValue(
			((FormComponent<?>)tester.getComponentFromLastRenderedPage("themed:resize"))
				.getInputName(), "stretch");
		tester.executeAjaxEvent("themed:resize", "change");

		assertEquals(Mode.STRETCH, treeGrid.getResizableColumns());
		assertEquals(Mode.STRETCH, jsonTreeGrid.getResizableColumns());
		tester.assertComponentOnAjaxResponse("themed:treeGrid");
		tester.assertComponentOnAjaxResponse("themed:jsonTreeGrid");
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
