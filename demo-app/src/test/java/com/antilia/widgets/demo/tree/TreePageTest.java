/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.tree;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.util.visit.IVisit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.demo.WicketApplication;
import com.antilia.widgets.tree.JsonBasedTree;
import com.antilia.widgets.tree.Tree;

/**
 * Shows the files of antilia-widgets in the trees of {@link TreePage}, and the file selected in
 * either next to them.
 */
class TreePageTest
{
	private WicketTester tester;

	@BeforeEach
	void startPage()
	{
		tester = new WicketTester(new WicketApplication());
		tester.startPage(TreePage.class);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	@Test
	void showsTheFilesInBothTreesTheFolderFrontendExpanded()
	{
		tester.assertComponent("themed:tree", Tree.class);
		tester.assertComponent("themed:jsonTree", JsonBasedTree.class);
		// the children of frontend, in the tree of components and in the JSON
		tester.assertContains(">package.json<");
		tester.assertContains(Pattern.quote("\"expanded\":[\"" + FilesDemoPage.expanded().id() +
			"\"]"));
		tester.assertContains(Pattern.quote("\"name\":\"package.json\""));
		tester.assertContains(Pattern.quote("\"label\":\"Project files\""));
		tester.assertContains(Pattern.quote("Select a file in a tree to see it here."));
	}

	@Test
	void aFileSelectedInTheTreeOfComponentsIsShownNextToIt()
	{
		ProjectFile readme = ProjectFiles.get().byPath("frontend/README.md");
		Component link = linkOf(readme);
		tester.executeAjaxEvent(link, "click");

		tester.assertComponentOnAjaxResponse("themed:details");
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("frontend/README.md"), response);
		assertTrue(response.contains(">Markdown<"), response);
	}

	@Test
	void aFileSelectedInTheJsonTreeIsShownNextToIt()
	{
		JsonBasedTree<?, ?> tree = (JsonBasedTree<?, ?>)tester.getComponentFromLastRenderedPage(
			"themed:jsonTree");
		ProjectFile pom = ProjectFiles.get().byPath("pom.xml");
		tester.getRequest().getPostParameters().setParameterValue("action",
			JsonBasedTree.SELECT_ACTION);
		tester.getRequest().getPostParameters().setParameterValue("key",
			String.valueOf(pom.id()));
		tester.executeBehavior(tree.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));

		tester.assertComponentOnAjaxResponse("themed:details");
		assertTrue(tester.getLastResponseAsString().contains(">pom.xml<"));
	}

	@Test
	void theTextsAndTheFilesAreInSpanishToo()
	{
		tester.getSession().setLocale(Locale.of("es"));
		tester.startPage(TreePage.class);

		tester.assertContains(Pattern.quote("Ejemplo de Tree y JsonBasedTree"));
		tester.assertContains(Pattern.quote("\"kind\":\"Carpeta\""));
		tester.assertContains(Pattern.quote("\"loading\":\"Cargando…\""));
		tester.assertContains(Pattern.quote("Archivos del proyecto"));
	}

	@Test
	void theAppsGetTheFilesTheTextsAndTheLicenseKey()
	{
		String page = tester.getLastResponseAsString();
		assertTrue(page.contains("data-demo=\"tree\""), page);
		assertTrue(page.contains("api/files"), page);
		assertTrue(page.contains("&quot;licenseKey&quot;:&quot;AW1."), page);
		assertTrue(page.contains("&quot;unlicensed&quot;"), page);
	}

	/**
	 * @return the button of a node of the tree of components
	 */
	private Component linkOf(ProjectFile file)
	{
		Tree<?> tree = (Tree<?>)tester.getComponentFromLastRenderedPage("themed:tree");
		return tree.visitChildren(Component.class, (Component component,
			IVisit<Component> visit) -> {
			if ("link".equals(component.getId()) && file.equals(component.getParent()
				.getDefaultModelObject()))
			{
				visit.stop(component);
			}
		});
	}
}
