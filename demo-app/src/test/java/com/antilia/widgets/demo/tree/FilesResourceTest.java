/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;

import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.demo.WicketApplication;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * Reads the {@link ProjectFiles} and serves them from the {@link FilesResource}, folder by folder.
 */
class FilesResourceTest
{
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
	void theRootsAreTheFoldersThenTheFilesOfTheProject()
	{
		List<String> roots = ProjectFiles.get().roots().stream().map(ProjectFile::name).toList();

		assertEquals(List.of("demo-app", "frontend", "ui-components"), roots.subList(0, 3));
		assertTrue(roots.contains("pom.xml"), roots.toString());
		ProjectFile frontend = ProjectFiles.get().byPath("frontend");
		assertTrue(frontend.folder());
		assertEquals(ProjectFiles.get().children(frontend).stream().mapToLong(ProjectFile::bytes)
			.sum(), frontend.bytes());
		assertFalse(ProjectFiles.get().byPath("frontend/package.json").folder());
	}

	@Test
	void servesTheRootsAndTheChildrenOfAFolder()
	{
		JSONArray roots = FilesResource.nodes(null, Locale.ENGLISH).getJSONArray("nodes");
		JSONObject first = roots.getJSONObject(0);
		assertFalse(first.getBoolean("leaf"));
		assertEquals("demo-app", first.getJSONObject("data").getString("name"));
		assertEquals("Folder", first.getJSONObject("data").getString("kind"));

		ProjectFile frontend = ProjectFiles.get().byPath("frontend");
		JSONArray children = FilesResource.nodes(String.valueOf(frontend.id()), Locale.ENGLISH)
			.getJSONArray("nodes");
		assertEquals(frontend.children().size(), children.length());

		assertEquals(0, FilesResource.nodes("nothing", Locale.ENGLISH).getJSONArray("nodes")
			.length());
	}

	@Test
	void theTextsAreInTheLanguageAsked()
	{
		ProjectFile java = ProjectFiles.get().byPath(
			"ui-components/src/main/java/com/antilia/widgets/Initializer.java");

		assertEquals("Código Java", ProjectFiles.toJson(java, Locale.of("es")).get("kind"));
		assertEquals("Java-Quelltext", ProjectFiles.toJson(java, Locale.GERMAN).get("kind"));
		assertEquals("1,5 KB", ProjectFiles.formatSize(1536, Locale.GERMAN));
		assertEquals("1.5 KB", ProjectFiles.formatSize(1536, Locale.ENGLISH));
		assertEquals("12 B", ProjectFiles.formatSize(12, Locale.ENGLISH));
		assertEquals("3 items", ProjectFiles.toJson(new ProjectFile(0, "f", "f", true, 0,
			java.modified(), "folder", List.of(1, 2, 3)), Locale.ENGLISH).get("items"));
	}

	@Test
	void isMountedAtApiFiles()
	{
		tester.executeUrl("api/files");
		assertTrue(tester.getLastResponseAsString().startsWith("{\"nodes\":[{\"key\":\""),
			tester.getLastResponseAsString());
	}
}
