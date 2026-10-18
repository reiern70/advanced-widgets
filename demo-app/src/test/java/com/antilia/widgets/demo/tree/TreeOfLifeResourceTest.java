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
package com.antilia.widgets.demo.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.demo.WicketApplication;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * Reads the {@link TreeOfLife} and serves it from the {@link TreeOfLifeResource}, group by group.
 */
class TreeOfLifeResourceTest
{
	private static final Locale SPANISH = Locale.of("es");

	private WicketTester tester;

	@BeforeEach
	void start()
	{
		tester = new WicketTester(new WicketApplication());
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	@Test
	void theKingdomsHoldTheGroupsAndTheSpecies()
	{
		TreeOfLife tree = TreeOfLife.get();
		assertEquals(List.of("fungi", "animals", "plants"),
			tree.roots().stream().map(Taxon::id).toList());

		Taxon rex = tree.byId("tyrannosaurus");
		assertTrue(rex.isLeaf());
		assertEquals("Tyrannosaurus rex", rex.scientific());
		assertEquals(List.of("animals", "vertebrates", "reptiles", "dinosaurs", "theropods",
			"tyrannosaurus"), tree.lineage(rex).stream().map(Taxon::id).toList());
		assertFalse(tree.byId("birds").isLeaf());
		assertNull(tree.byId("ediacaran").scientific());
		assertNull(tree.byId("nothing"));
	}

	@Test
	void everyTaxonHasANameAndADescriptionInEveryLanguage()
	{
		for (Locale locale : List.of(Locale.ENGLISH, Locale.GERMAN, SPANISH))
		{
			for (Taxon taxon : all(TreeOfLife.get().roots()))
			{
				Map<String, Object> card = TreeOfLife.card(taxon, locale);
				assertNotEquals(taxon.id(), card.get("name"), locale + " " + taxon.id());
				assertNotEquals(taxon.id() + ".description", card.get("description"),
					locale + " " + taxon.id());
				assertFalse(card.get("rank").toString().startsWith("rank."), taxon.id());
				assertFalse(card.get("period").toString().contains("period."), taxon.id());
			}
		}
	}

	@Test
	void theTimesAndThePeriodsAreWrittenInTheLanguageAsked()
	{
		TreeOfLife tree = TreeOfLife.get();
		Map<String, Object> rex = TreeOfLife.card(tree.byId("tyrannosaurus"), Locale.ENGLISH);
		assertEquals("68–66 Ma", rex.get("lived"));
		assertEquals("Cretaceous", rex.get("period"));
		assertEquals("Species", rex.get("rank"));

		Map<String, Object> dinosaurs = TreeOfLife.card(tree.byId("dinosaurs"), SPANISH);
		assertEquals("Dinosaurios", dinosaurs.get("name"));
		assertEquals("233 Ma – hoy", dinosaurs.get("lived"));
		assertEquals("Triásico – hoy", dinosaurs.get("period"));

		Map<String, Object> mammoth = TreeOfLife.card(tree.byId("mammoth"), Locale.GERMAN);
		assertEquals("0,8 Ma – vor 4.000 Jahren", mammoth.get("lived"));
		assertEquals("Quartär", mammoth.get("period"));
		assertEquals("Ediacarium – heute", TreeOfLife.card(tree.byId("arthropods"),
			Locale.GERMAN).get("period"));

		assertEquals("until 340 years ago", TreeOfLife.card(tree.byId("dodo"), Locale.ENGLISH)
			.get("lived"));
		assertEquals("living today", TreeOfLife.card(tree.byId("ostrich"), Locale.ENGLISH)
			.get("lived"));
		assertEquals("Cambrian", TreeOfLife.card(tree.byId("cambrian"), Locale.ENGLISH)
			.get("period"));
		assertEquals("Ediacaran", TreeOfLife.card(tree.byId("ediacaran"), Locale.ENGLISH)
			.get("period"));
	}

	@Test
	void aSpeciesCarriesItsLineageAndTheCardOfItsGroup()
	{
		Map<String, Object> rex = TreeOfLife.get().toJson(TreeOfLife.get().byId("tyrannosaurus"),
			Locale.ENGLISH);
		assertEquals(false, rex.get("group"));
		assertEquals(List.of("Animals", "Vertebrates", "Reptiles", "Dinosaurs", "Theropods",
			"Tyrannosaurus rex"), rex.get("lineage"));
		assertEquals("Theropods", ((Map<?, ?>)rex.get("parent")).get("name"));

		Map<String, Object> dinosaurs = TreeOfLife.get().toJson(
			TreeOfLife.get().byId("dinosaurs"), Locale.ENGLISH);
		assertEquals(true, dinosaurs.get("group"));
		assertFalse(dinosaurs.containsKey("parent"));
	}

	@Test
	void servesTheKingdomsAndTheChildrenOfAGroup()
	{
		JSONArray roots = TreeOfLifeResource.nodes(null, Locale.ENGLISH).getJSONArray("nodes");
		JSONObject fungi = roots.getJSONObject(0);
		assertEquals("fungi", fungi.getString("key"));
		assertFalse(fungi.getBoolean("leaf"));
		assertEquals("Fungi", fungi.getJSONObject("data").getString("name"));
		assertEquals("Kingdom", fungi.getJSONObject("data").getString("rank"));

		JSONArray theropods = TreeOfLifeResource.nodes("theropods", Locale.ENGLISH)
			.getJSONArray("nodes");
		assertEquals(TreeOfLife.get().byId("theropods").children().size(), theropods.length());
		assertEquals("Theropods", theropods.getJSONObject(0).getJSONObject("data")
			.getJSONObject("parent").getString("name"));

		assertEquals(0, TreeOfLifeResource.nodes("nothing", Locale.ENGLISH).getJSONArray("nodes")
			.length());
	}

	@Test
	void isMountedAtApiTreeOfLife()
	{
		tester.executeUrl("api/tree-of-life?parent=birds");
		assertTrue(tester.getLastResponseAsString().startsWith(
			"{\"nodes\":[{\"key\":\"archaeopteryx\""), tester.getLastResponseAsString());
	}

	private static List<Taxon> all(List<Taxon> taxa)
	{
		return taxa.stream().flatMap(taxon -> Stream.concat(Stream.of(taxon),
			all(TreeOfLife.get().children(taxon)).stream())).toList();
	}
}
