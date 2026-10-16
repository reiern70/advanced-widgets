/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.nio.file.Path;

import org.apache.wicket.IPageManagerProvider;
import org.apache.wicket.pageStore.DelegatingPageStore;
import org.apache.wicket.pageStore.IPageStore;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.antilia.widgets.demo.admin.AdminPage;
import com.antilia.widgets.demo.data.Role;

/**
 * The folder of the page store comes from outside, so production can keep it on a volume. The
 * pages in it last one run of the application, as the sessions do.
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

	@Test
	public void thePagesLastOneRunOfTheApplication(@TempDir Path folder)
	{
		System.setProperty(WicketApplication.PAGE_STORE_PROPERTY, folder.toString());
		WicketTester run = new WicketTester(new WicketApplication())
		{
			@Override
			protected IPageManagerProvider newTestPageManagerProvider()
			{
				// the application's page manager, which keeps the pages on disk
				return null;
			}
		};
		String name = run.getApplication().getName();
		IPageStore store = run.getSession().getPageManager().getPageStore();
		while (store instanceof DelegatingPageStore delegating)
		{
			store = delegating.getDelegate();
		}
		assertInstanceOf(TransientDiskPageStore.class, store);

		DemoSession.get().signIn("admin", Role.ADMIN);
		run.startPage(AdminPage.class);
		run.assertRenderedPage(AdminPage.class);
		run.destroy();

		assertFalse(TransientDiskPageStore.pagesFolder(folder.toFile(), name).exists());
	}
}
