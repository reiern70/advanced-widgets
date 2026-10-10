/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.antilia.widgets.demo.Demos.Demo;

/**
 * Checks that the home page tells the truth about every demo.
 */
class DemosTest
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

	static Iterable<Demo> demos()
	{
		return Demos.all();
	}

	@ParameterizedTest
	@MethodSource("demos")
	void hasReactAndSvelteAppsWhenTheHomePageSaysSo(Demo demo)
	{
		tester.startPage(demo.page());

		DemoPage page = (DemoPage)tester.getLastRenderedPage();
		assertEquals(demo.frameworkApps(), page.getFrameworkDemo() != null, demo.id());
	}

	@ParameterizedTest
	@MethodSource("demos")
	void hasAScreenshot(Demo demo)
	{
		String basedir = System.getProperty("basedir");
		File image = new File((basedir != null ? basedir + "/" : "") + "src/main/webapp/" +
			demo.imagePath());
		assertTrue(image.isFile(), image + " is missing");
	}
}
