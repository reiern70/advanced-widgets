/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.tree;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * Shows the tree of life in the trees of {@link TreePage}, and the cards of the group or the
 * species selected in either next to them.
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
	void showsTheTreeOfLifeInBothTreesTheWayToTheDinosaursExpanded()
	{
		tester.assertComponent("themed:tree", Tree.class);
		tester.assertComponent("themed:jsonTree", JsonBasedTree.class);
		// the children of the dinosaurs, in the tree of components and in the JSON
		tester.assertContains(">Theropods<");
		tester.assertContains(Pattern.quote("\"expanded\":[\"animals\",\"vertebrates\"," +
			"\"reptiles\",\"dinosaurs\"]"));
		tester.assertContains(Pattern.quote("\"name\":\"Theropods\""));
		tester.assertContains(Pattern.quote("\"selectBranches\":true"));
		tester.assertContains(Pattern.quote("\"label\":\"Tree of life\""));
		tester.assertContains(Pattern.quote("Select a group or a species in a tree"));
	}

	@Test
	void aGroupSelectedInTheTreeOfComponentsShowsItsCardAndExpands()
	{
		tester.executeAjaxEvent(linkOf(TreeOfLife.get().byId("theropods")), "click");

		tester.assertComponentOnAjaxResponse("themed:details");
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("tree-demo-card--group"), response);
		assertTrue(response.contains(">Theropoda<"), response);
		assertTrue(response.contains(">Tyrannosaurus rex<"), "the theropods are expanded");
		assertFalse(response.contains("tree-demo-card--species"), response);
	}

	@Test
	void aSpeciesSelectedShowsItsLineageTheCardOfItsGroupAndItsOwn()
	{
		tester.executeAjaxEvent(linkOf(TreeOfLife.get().byId("theropods")), "click");
		tester.executeAjaxEvent(linkOf(TreeOfLife.get().byId("tyrannosaurus")), "click");

		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("<span>Animals</span>"), response);
		assertTrue(response.contains("aria-current=\"true\"><span>Tyrannosaurus rex</span>"),
			response);
		assertTrue(response.contains(">Theropoda<"), "the card of its group: " + response);
		assertTrue(response.contains("tree-demo-card--species"), response);
		assertTrue(response.contains(">68–66 Ma<"), response);
		assertTrue(response.contains(">Cretaceous<"), response);
	}

	@Test
	void aGroupSelectedInTheJsonTreeIsShownNextToIt()
	{
		JsonBasedTree<?, ?> tree = (JsonBasedTree<?, ?>)tester.getComponentFromLastRenderedPage(
			"themed:jsonTree");
		tester.getRequest().getPostParameters().setParameterValue("action",
			JsonBasedTree.SELECT_ACTION);
		tester.getRequest().getPostParameters().setParameterValue("key", "fungi");
		tester.executeBehavior(tree.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));

		tester.assertComponentOnAjaxResponse("themed:details");
		assertTrue(tester.getLastResponseAsString().contains(">Fungi<"));
	}

	@Test
	void theTextsAndTheTreeAreInSpanishToo()
	{
		tester.getSession().setLocale(Locale.of("es"));
		tester.startPage(TreePage.class);

		tester.assertContains(Pattern.quote("Ejemplo de Tree y JsonBasedTree"));
		tester.assertContains(Pattern.quote("\"name\":\"Terópodos\""));
		tester.assertContains(Pattern.quote("\"loading\":\"Cargando…\""));
		tester.assertContains(Pattern.quote("Árbol de la vida"));
	}

	@Test
	void theAppsGetTheTreeTheTextsAndTheLicenseKey()
	{
		String page = tester.getLastResponseAsString();
		assertTrue(page.contains("data-demo=\"tree\""), page);
		assertTrue(page.contains("api/tree-of-life"), page);
		assertTrue(page.contains("&quot;licenseKey&quot;:&quot;AW1."), page);
		assertTrue(page.contains("&quot;unlicensed&quot;"), page);
	}

	/**
	 * @return the button of a node of the tree of components
	 */
	private Component linkOf(Taxon taxon)
	{
		Tree<?> tree = (Tree<?>)tester.getComponentFromLastRenderedPage("themed:tree");
		return tree.visitChildren(Component.class, (Component component,
			IVisit<Component> visit) -> {
			if ("link".equals(component.getId()) && taxon.equals(component.getParent()
				.getDefaultModelObject()))
			{
				visit.stop(component);
			}
		});
	}
}
