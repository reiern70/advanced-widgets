/*
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
package com.antiliacode.widgets.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.antiliacode.widgets.demo.Demos.Demo;

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
