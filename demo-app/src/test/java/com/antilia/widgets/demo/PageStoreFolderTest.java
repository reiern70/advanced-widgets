/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The folder of the page store comes from outside, so production can keep it on a volume.
 */
public class PageStoreFolderTest
{
	private WicketTester tester;

	@AfterEach
	public void tearDown()
	{
		System.clearProperty(WicketApplication.PAGE_STORE_PROPERTY);
		if (tester != null)
		{
			tester.destroy();
		}
	}

	@Test
	public void thePropertySetsTheFolderOfThePageStore(@TempDir Path folder)
	{
		System.setProperty(WicketApplication.PAGE_STORE_PROPERTY, folder.toString());

		tester = new WicketTester(new WicketApplication());

		assertEquals(folder.toFile(),
			tester.getApplication().getStoreSettings().getFileStoreFolder());
	}

	@Test
	public void withoutThePropertyTheFolderIsTheServletContainersTemporaryFolder()
	{
		tester = new WicketTester(new WicketApplication());

		assertEquals(tester.getServletContext().getAttribute("jakarta.servlet.context.tempdir"),
			tester.getApplication().getStoreSettings().getFileStoreFolder());
	}
}
