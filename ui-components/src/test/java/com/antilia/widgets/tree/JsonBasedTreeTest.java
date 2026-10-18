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

import java.util.regex.Pattern;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable.BodyHeightUnits;
import com.antilia.widgets.jsonlist.IItemAction;
import com.antilia.widgets.license.Licensing;
import com.antilia.widgets.table.ResizableColumnsBehavior.Mode;
import com.antilia.widgets.tree.TestNodes.FileNode;
import com.antilia.widgets.tree.TreeTest.TestPage;

class JsonBasedTreeTest extends WicketTestCase
{
	private TestTree tree;

	private FileNode acted;

	@BeforeEach
	void startTree()
	{
		acted = null;
		tree = new TestTree();
		tree.setNodeTemplate("<span>{{name}}</span>");
		tree.setLabel(Model.of("Files"));
		tree.addAction(IItemAction.of("open", (node, target) -> acted = node));
		tester.startPage(new TestPage(tree));
	}

	@Test
	void rendersAnEmptyDivAndItsScriptWithTheRoots()
	{
		TagTester tag = tester.getTagByWicketId("tree");
		assertEquals("div", tag.getName());
		assertTrue(tag.getAttributeContains("class", JsonBasedTree.CSS_CLASS));
		assertTrue(tag.getAttributeContains("class", Tree.CSS_CLASS));
		assertEquals("", tag.getValue(), "the tree has no children");
		tester.assertContains("json-tree.js");
		tester.assertContains("antilia-tree.css");
		tester.assertContains(Pattern.quote("new Wicket.JsonTree({"));
		tester.assertContains(Pattern.quote("\"template\":\"\\u003cspan\\u003e{{name}}"));
		tester.assertContains(Pattern.quote("\"label\":\"Files\""));
		tester.assertContains(Pattern.quote("\"texts\":{\"loading\":\"Loading…\"," +
			"\"empty\":\"Nothing to show\"}"));
		tester.assertContains(Pattern.quote("[{\"key\":\"docs\",\"leaf\":false,\"data\":" +
			"{\"id\":\"docs\",\"name\":\"docs\"}},{\"key\":\"notes\",\"leaf\":true,"));
		tester.assertContainsNot(Pattern.quote("\"children\""));
		tester.assertContainsNot(Pattern.quote("\"icons\""));
	}

	@Test
	void sendsTheChildrenOfTheFoldersExpandedAndTheLeafSelected()
	{
		tree.expand(node("docs")).setSelected(node("readme"));
		tester.startPage(new TestPage(tree));

		tester.assertContains(Pattern.quote("\"expanded\":[\"docs\"]"));
		tester.assertContains(Pattern.quote("\"selected\":\"readme\""));
		tester.assertContains(Pattern.quote("\"children\":[{\"key\":\"src\",\"leaf\":false"));
	}

	@Test
	void servesTheRootsAndTheChildrenOfAFolder()
	{
		tester.executeBehavior(dataBehavior());
		assertEquals("{\"nodes\":[{\"key\":\"docs\",\"leaf\":false,\"data\":{\"id\":\"docs\"," +
			"\"name\":\"docs\"}},{\"key\":\"notes\",\"leaf\":true,\"data\":{\"id\":\"notes\"," +
			"\"name\":\"notes.txt\"}}]}", tester.getLastResponseAsString());

		tester.getRequest().getPostParameters().setParameterValue("parent", "src");
		tester.executeBehavior(dataBehavior());
		assertEquals("{\"nodes\":[{\"key\":\"a\",\"leaf\":true,\"data\":{\"id\":\"a\"," +
			"\"name\":\"a.ts\"}},{\"key\":\"b\",\"leaf\":true,\"data\":{\"id\":\"b\"," +
			"\"name\":\"b.ts\"}}]}", tester.getLastResponseAsString());

		tester.getRequest().getPostParameters().setParameterValue("parent", "gone");
		tester.executeBehavior(dataBehavior());
		assertEquals("{\"nodes\":[]}", tester.getLastResponseAsString());
	}

	@Test
	void theSelectionOfALeafIsReported()
	{
		act(JsonBasedTree.SELECT_ACTION, "a");

		assertSame(node("a"), tree.selected);
		assertSame(node("a"), tree.getSelected());
		assertNull(tree.notFoundAction);
	}

	@Test
	void aFolderOrAGoneNodeIsNotSelected()
	{
		act(JsonBasedTree.SELECT_ACTION, "docs");
		assertNull(tree.selected);
		assertEquals(JsonBasedTree.SELECT_ACTION, tree.notFoundAction);

		act(JsonBasedTree.SELECT_ACTION, "gone");
		assertNull(tree.selected);
		assertNull(tree.notFoundNode);
	}

	@Test
	void aFolderIsSelectedWhenFoldersAreSelectedToo()
	{
		tree.setSelectBranches(true);
		tester.startPage(new TestPage(tree));
		tester.assertContains(Pattern.quote("\"selectBranches\":true"));

		act(JsonBasedTree.SELECT_ACTION, "docs");
		assertSame(node("docs"), tree.selected);
		assertNull(tree.notFoundAction);
	}

	@Test
	void theTreeRemembersTheFoldersTheUserExpands()
	{
		act(JsonBasedTree.EXPAND_ACTION, "docs");
		act(JsonBasedTree.EXPAND_ACTION, "src");
		act(JsonBasedTree.COLLAPSE_ACTION, "src");

		assertTrue(tree.isExpanded(node("docs")));
		assertFalse(tree.isExpanded(node("src")));
		tester.startPage(new TestPage(tree));
		tester.assertContains(Pattern.quote("\"expanded\":[\"docs\"]"));
	}

	@Test
	void anActionIsDispatchedWithTheNodeOfItsKey()
	{
		act("open", "b");
		assertSame(node("b"), acted);

		act("unknown", "b");
		assertEquals("unknown", tree.notFoundAction);
		assertSame(node("b"), tree.notFoundNode);
	}

	@Test
	void refreshingAndUpdatingANodeReachTheBrowser()
	{
		TestPage page = new TestPage(tree);
		page.add(new RefreshBehavior());
		tester.startPage(page);
		tester.executeBehavior(page.getBehaviors(RefreshBehavior.class).get(0));

		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("Wicket.JsonTree.instances[\"" + tree.getMarkupId() +
			"\"].reload()"), response);
		assertTrue(response.contains(".renderNode(\"a\", {\"id\":\"a\",\"name\":\"a.ts\"})"),
			response);
	}

	@Test
	void takesIconsOfTheApplicationAndOfANode()
	{
		TestTree withIcons = new TestTree()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected com.antilia.widgets.icon.IIcon getIcon(FileNode node)
			{
				return "notes".equals(node.getId()) ? () -> "<b>note</b>" : null;
			}
		};
		withIcons.setNodeTemplate("{{name}}");
		withIcons.setIcon(TreeIcon.FOLDER, () -> "<i>folder</i>");
		tester.startPage(new TestPage(withIcons));

		tester.assertContains(Pattern.quote("\"folder\":\"\\u003ci\\u003efolder"));
		tester.assertContains(Pattern.quote("\"icon\":\"\\u003cb\\u003enote"));
	}

	@Test
	void aTreeGridSendsItsColumns()
	{
		JsonBasedTreeGrid<FileNode, String> table = new JsonBasedTreeGrid<>("tree",
			new TestNodes.Provider());
		table.setNodeTemplate("{{name}}");
		table.setTreeHeader(Model.of("Name"));
		table.addColumn(Model.of("Id"), "{{id}}", "id-column");
		tester.startPage(new TestPage(table));

		assertTrue(tester.getTagByWicketId("tree").getAttributeContains("class",
			TreeGrid.CSS_CLASS));
		tester.assertContains(Pattern.quote("\"table\":{\"header\":\"Name\",\"columns\":" +
			"[{\"header\":\"Id\",\"template\":\"{{id}}\",\"cssClass\":\"id-column\"}]}"));
		tester.assertContainsNot(Pattern.quote("\"resizableColumns\""));
		tester.assertContainsNot("antilia-resizable-columns.*\\.js");
	}

	@Test
	void aTreeGridOfAHeightWithResizableColumnsLoadsTheirScripts()
	{
		JsonBasedTreeGrid<FileNode, String> table = new JsonBasedTreeGrid<>("tree",
			new TestNodes.Provider());
		table.setNodeTemplate("{{name}}");
		table.setHeight(40, BodyHeightUnits.PERCENT);
		table.setResizableColumns(Mode.STRETCH);
		tester.startPage(new TestPage(table));

		assertEquals(40, table.getHeight());
		assertEquals(BodyHeightUnits.PERCENT, table.getHeightUnits());
		assertEquals(Mode.STRETCH, table.getResizableColumns());
		tester.assertContains(Pattern.quote("\"height\":{\"value\":40,\"unit\":\"%\"}"));
		tester.assertContains(Pattern.quote("\"resizableColumns\":\"stretch\""));
		tester.assertContains(Pattern.quote("\"resizeColumn\":\"Resize column\""));
		tester.assertContains("antilia-scrollbar.*\\.js");
		tester.assertContains("antilia-resizable-columns.*\\.js");
	}

	@Test
	void aTreeWithoutAHeightLeavesTheScrollBarsOut()
	{
		assertEquals(-1, tree.getHeight());
		tester.assertContainsNot(Pattern.quote("\"height\""));
		tester.assertContainsNot("antilia-scrollbar.*\\.js");
	}

	@Test
	void rendersAPlaceholderWithoutALicenseKey()
	{
		Licensing.install(tester.getApplication(), null);
		tester.startPage(new TestPage(tree));

		String page = tester.getLastResponseAsString();
		TagTester placeholder = TagTester.createTagByAttribute(page, "class",
			Licensing.PLACEHOLDER_CSS_CLASS);
		assertEquals("TestTree", placeholder.getAttribute("data-component"));
		assertFalse(page.contains("new Wicket.JsonTree("), page);
	}

	@Test
	void rendersWithoutALicenseKeyWhileTheKeyIsNotEnforced()
	{
		String enforced = System.clearProperty(Licensing.ENFORCED_PROPERTY);
		try
		{
			Licensing.install(tester.getApplication(), null);
			tester.startPage(new TestPage(tree));

			String page = tester.getLastResponseAsString();
			assertFalse(page.contains(Licensing.PLACEHOLDER_CSS_CLASS), page);
			assertTrue(page.contains("new Wicket.JsonTree("), page);
		}
		finally
		{
			if (enforced != null)
			{
				System.setProperty(Licensing.ENFORCED_PROPERTY, enforced);
			}
		}
	}

	private void act(String action, String key)
	{
		tester.getRequest().getPostParameters().setParameterValue("action", action);
		tester.getRequest().getPostParameters().setParameterValue("key", key);
		tester.executeBehavior(tree.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));
	}

	private AbstractAjaxBehavior dataBehavior()
	{
		return tree.getBehaviors(AbstractAjaxBehavior.class)
			.stream()
			.filter(behavior -> !(behavior instanceof AbstractDefaultAjaxBehavior))
			.findFirst()
			.orElseThrow();
	}

	/**
	 * Refreshes the tree and repaints the node a.
	 */
	static class RefreshBehavior extends AbstractDefaultAjaxBehavior
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected void respond(AjaxRequestTarget target)
		{
			@SuppressWarnings("unchecked")
			JsonBasedTree<FileNode, String> tree = (JsonBasedTree<FileNode, String>)getComponent()
				.get("tree");
			tree.refresh(target);
			tree.updateNode(target, node("a"));
		}
	}

	static class TestTree extends JsonBasedTree<FileNode, String>
	{
		private static final long serialVersionUID = 1L;

		FileNode selected;

		String notFoundAction;

		FileNode notFoundNode;

		TestTree()
		{
			super("tree", new TestNodes.Provider());
		}

		@Override
		protected void onSelect(AjaxRequestTarget target, FileNode node)
		{
			selected = node;
		}

		@Override
		protected void onNotFoundAction(AjaxRequestTarget target, String action, String key,
			FileNode node)
		{
			notFoundAction = action;
			notFoundNode = node;
		}
	}
}
