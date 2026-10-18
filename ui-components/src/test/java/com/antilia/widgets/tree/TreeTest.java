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
package com.antilia.widgets.tree;

import static com.antilia.widgets.tree.TestNodes.node;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.tree.AbstractTree.State;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.TreeColumn;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.apache.wicket.util.visit.IVisit;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable.BodyHeightUnits;
import com.antilia.widgets.license.Licensing;
import com.antilia.widgets.table.IResizableColumnsTable;
import com.antilia.widgets.table.ResizableColumnsBehavior.Mode;
import com.antilia.widgets.tree.TestNodes.FileNode;

class TreeTest extends WicketTestCase
{
	@Test
	void rendersTheRootsWithTheIconsOfItsOwn()
	{
		TestTree tree = new TestTree();
		tester.startPage(new TestPage(tree));

		TagTester tag = tester.getTagByWicketId("tree");
		assertTrue(tag.getAttributeContains("class", Tree.CSS_CLASS));
		tester.assertContains("antilia-tree.css");
		tester.assertContains(Pattern.quote(TreeIcon.FOLDER.getMarkup()));
		tester.assertContains(Pattern.quote(TreeIcon.LEAF.getMarkup()));
		tester.assertContains(Pattern.quote(TreeIcon.TOGGLE.getMarkup()));
		tester.assertContains(">docs<");
		tester.assertContains(">notes.txt<");
		tester.assertContainsNot(">readme.md<");
		tester.assertContains(Pattern.quote("aria-expanded=\"false\""));
		tester.assertContains(Pattern.quote("title=\"Expand\""));
	}

	@Test
	void aClickOnAFolderExpandsItAndOnALeafSelectsIt()
	{
		TestTree tree = new TestTree();
		tester.startPage(new TestPage(tree));

		click(tree, "docs");
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains(">readme.md<"), response);
		assertTrue(response.contains("aria-expanded=\"true\""), response);
		assertTrue(response.contains(TreeIcon.FOLDER_OPEN.getMarkup()), response);

		click(tree, "readme");
		assertSame(node("readme"), tree.selected);
		assertSame(node("readme"), tree.getSelected());
		response = tester.getLastResponseAsString();
		assertTrue(response.contains("aria-current=\"true\""), response);
		assertTrue(response.contains("antilia-tree-node--selected"), response);

		click(tree, "notes");
		assertSame(node("notes"), tree.selected);
		assertFalse(tree.isSelected(node("readme")));
		assertEquals(2, countOf(tester.getLastResponseAsString(), "<component id="),
			"the leaf selected before and the new one are repainted");
	}

	@Test
	void aClickOnAFolderSelectsItWhenFoldersAreSelectedToo()
	{
		TestTree tree = new TestTree();
		tree.setSelectBranches(true);
		tester.startPage(new TestPage(tree));

		click(tree, "docs");
		assertSame(node("docs"), tree.selected);
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains(">readme.md<"), "the folder selected is expanded");
		assertTrue(response.contains("aria-current=\"true\""), response);
		assertTrue(response.contains("antilia-tree-node--selected"), response);

		click(tree, "docs");
		assertEquals(State.EXPANDED, tree.getState(node("docs")),
			"a click on the folder selected leaves it expanded");

		click(tree, "readme");
		assertSame(node("readme"), tree.selected);
		assertFalse(tree.isSelected(node("docs")));
	}

	@Test
	void takesIconsOfTheApplication()
	{
		TestTree tree = new TestTree();
		tree.setIcon(TreeIcon.LEAF, () -> "<i class=\"my-leaf\"></i>");
		tester.startPage(new TestPage(tree));

		tester.assertContains(Pattern.quote("<i class=\"my-leaf\"></i>"));
		tester.assertContainsNot(Pattern.quote(TreeIcon.LEAF.getMarkup()));
	}

	@Test
	void rendersAPlaceholderWithoutALicenseKey()
	{
		Licensing.install(tester.getApplication(), null);
		tester.startPage(new TestPage(new TestTree()));

		TagTester placeholder = TagTester.createTagByAttribute(tester.getLastResponseAsString(),
			"class", Licensing.PLACEHOLDER_CSS_CLASS);
		assertEquals("TestTree", placeholder.getAttribute("data-component"));
		tester.assertContainsNot(">docs<");
	}

	@Test
	void aTreeGridRendersHeadersAndSelectsTheRowOfALeaf()
	{
		List<IColumn<FileNode, String>> columns = new ArrayList<>();
		columns.add(new TreeColumn<>(Model.of("Name")));
		columns.add(new PropertyColumn<>(Model.of("Id"), "id"));
		List<FileNode> selected = new ArrayList<>();
		TreeGrid<FileNode, String> table = new TreeGrid<>("tree", columns,
			new TestNodes.Provider())
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSelect(AjaxRequestTarget target, FileNode node)
			{
				selected.add(node);
			}
		};
		tester.startPage(new TestPage(table));

		assertTrue(tester.getTagByWicketId("tree").getAttributeContains("class",
			TreeGrid.CSS_CLASS));
		tester.assertContains(">Name<");
		tester.assertContains(">Id<");
		tester.assertContains(">docs<");

		click(table, "docs");
		tester.assertContains(">readme.md<");
		click(table, "readme");

		assertEquals(List.of(node("readme")), selected);
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("antilia-tree-node--selected"), response);
		assertTrue(table.isSelected(node("readme")));
	}

	@Test
	void aTreeOfAHeightScrollsWithTheScrollBars()
	{
		TestTree tree = new TestTree();
		tree.setHeight(300, BodyHeightUnits.PIXELS);
		tester.startPage(new TestPage(tree));

		TagTester tag = tester.getTagByWicketId("tree");
		assertTrue(tag.getAttributeContains("class", TreeSupport.SCROLLING_CSS_CLASS));
		assertNull(tag.getAttribute("style"), "a Content Security Policy may not let it in");
		tester.assertContains(Pattern.quote("tree.style.height = \"300px\";"));
		tester.assertContains("antilia-scrollbar.*\\.js");
		tester.assertContains(Pattern.quote("Wicket.Scrollbar.attach(\"" + tree.getMarkupId() +
			"\", {});"));

		tree.setHeight(-1, BodyHeightUnits.PIXELS);
		tester.startPage(tester.getLastRenderedPage());
		tester.assertContainsNot("Wicket.Scrollbar.attach");
		assertFalse(tester.getTagByWicketId("tree").getAttributeContains("class",
			TreeSupport.SCROLLING_CSS_CLASS));
	}

	@Test
	void aTreeGridOfAHeightKeepsItsHeadAboveTheRowsAndRepaintsOnlyItsTable()
	{
		TreeGrid<FileNode, String> table = new TreeGrid<>("tree", gridColumns(),
			new TestNodes.Provider());
		table.setHeight(20, BodyHeightUnits.VIEWPORT_HEIGHT);
		table.setResizableColumns(Mode.FIT);
		tester.startPage(new TestPage(table));

		tester.assertContains(Pattern.quote("tree.style.height = \"20vh\";"));
		tester.assertContains(Pattern.quote("\"header\":\":scope > table > thead\""));
		tester.assertContains(Pattern.quote("\"keepWidths\":true"));
		tester.assertContains(Pattern.quote("Wicket.ResizableColumns.attach(\"" +
			table.getTable().getMarkupId() + "\""));
		assertEquals(Mode.FIT, table.getResizableColumns());

		click(table, "docs");
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("<component id=\"" + table.getTable().getMarkupId() + "\""),
			response);
		assertFalse(response.contains("<component id=\"" + table.getMarkupId() + "\""), response);

		IResizableColumnsTable grid = (IResizableColumnsTable)table.getTable();
		grid.columnsResized(null, new double[] { 200, 100 });
		tester.startPage(tester.getLastRenderedPage());
		tester.assertContains(Pattern.quote("options.widths = [200,100];"));

		table.setResizableColumns(null);
		assertNull(table.getResizableColumns());
	}

	private static List<IColumn<FileNode, String>> gridColumns()
	{
		List<IColumn<FileNode, String>> columns = new ArrayList<>();
		columns.add(new TreeColumn<>(Model.of("Name")));
		columns.add(new PropertyColumn<>(Model.of("Id"), "id"));
		return columns;
	}

	/**
	 * Clicks the content of a node, as the user does.
	 */
	private void click(MarkupContainer tree, String id)
	{
		Component link = tree.visitChildren(TreeNodeContent.class,
			(TreeNodeContent<?> content, IVisit<Component> visit) -> {
				if (content.getDefaultModelObject() == node(id))
				{
					visit.stop(content.get("link"));
				}
			});
		assertTrue(link != null, "no node " + id);
		tester.executeAjaxEvent(link, "click");
	}

	private static int countOf(String text, String part)
	{
		return text.split(Pattern.quote(part), -1).length - 1;
	}

	static class TestTree extends Tree<FileNode>
	{
		private static final long serialVersionUID = 1L;

		FileNode selected;

		TestTree()
		{
			super("tree", new TestNodes.Provider());
		}

		@Override
		protected void onSelect(AjaxRequestTarget target, FileNode node)
		{
			selected = node;
		}
	}

	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		TestPage(Component tree)
		{
			add(tree);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream(
				"<html><head></head><body><div wicket:id=\"tree\"></div></body></html>");
		}
	}
}
