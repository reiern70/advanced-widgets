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
 * Shows the files of antilia-widgets in the tree grids of {@link TreeGridPage}, with their
 * kinds, sizes and the days they were changed, and the file selected next to them.
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
	void showsTheFilesInBothTreeGridsWithTheirColumns()
	{
		tester.assertComponent("themed:treeGrid", TreeGrid.class);
		tester.assertComponent("themed:jsonTreeGrid", JsonBasedTreeGrid.class);
		tester.assertContains(">Kind<");
		tester.assertContains(">Modified<");
		tester.assertContains(">package.json<");
		tester.assertContains(Pattern.quote("\"table\":{\"header\":\"Name\",\"columns\":" +
			"[{\"header\":\"Kind\",\"template\":\"{{kind}}\"},{\"header\":\"Size\"," +
			"\"template\":\"{{size}}\",\"cssClass\":\"antilia-tree-number\"}"));
	}

	@Test
	void aFileSelectedInTheJsonTreeGridIsShownNextToIt()
	{
		JsonBasedTree<?, ?> table = (JsonBasedTree<?, ?>)tester.getComponentFromLastRenderedPage(
			"themed:jsonTreeGrid");
		ProjectFile file = ProjectFiles.get().byPath("frontend/package.json");
		tester.getRequest().getPostParameters().setParameterValue("action",
			JsonBasedTree.SELECT_ACTION);
		tester.getRequest().getPostParameters().setParameterValue("key",
			String.valueOf(file.id()));
		tester.executeBehavior(table.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));

		tester.assertComponentOnAjaxResponse("themed:details");
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("frontend/package.json"), response);
		assertTrue(response.contains(">JSON<"), response);
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

		tester.assertContains(">Art<");
		tester.assertContains(">Geändert<");
		tester.assertContains(">Ordner<");
		tester.assertContains(Pattern.quote("\"header\":\"Größe\""));
	}
}
