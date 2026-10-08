/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.tree;

import static com.advanced.widgets.tree.TestNodes.node;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import com.advanced.widgets.license.Licensing;
import com.advanced.widgets.tree.TestNodes.FileNode;

class TreeTest extends WicketTestCase
{
	@Test
	void rendersTheRootsWithTheIconsOfItsOwn()
	{
		TestTree tree = new TestTree();
		tester.startPage(new TestPage(tree));

		TagTester tag = tester.getTagByWicketId("tree");
		assertTrue(tag.getAttributeContains("class", Tree.CSS_CLASS));
		tester.assertContains("wicket-tree.css");
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
		assertTrue(response.contains("wicket-tree-node--selected"), response);

		click(tree, "notes");
		assertSame(node("notes"), tree.selected);
		assertFalse(tree.isSelected(node("readme")));
		assertEquals(2, countOf(tester.getLastResponseAsString(), "<component id="),
			"the leaf selected before and the new one are repainted");
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
		assertTrue(response.contains("wicket-tree-node--selected"), response);
		assertTrue(table.isSelected(node("readme")));
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
