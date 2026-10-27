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
package com.antiliacode.widgets.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.mock.MockApplication;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.codeeditor.CodeEditor;
import com.antiliacode.widgets.richtext.RichTextEditor;
import com.antiliacode.widgets.template.Templates;

/**
 * The build writes a readable script and a minified one, {@code .min.js}, for every script of the
 * components. Wicket serves the readable one in development mode, so that what fails can be read
 * and debugged, and the minified one in deployment mode
 * ({@link org.apache.wicket.settings.ResourceSettings#setUseMinifiedResources(boolean)}).
 */
class MinifiedScriptsTest
{
	@Test
	void theEditorsServeTheirReadableScriptsInDevelopment() throws IOException
	{
		for (ResourceReference script : new ResourceReference[] { CodeEditor.JS,
				RichTextEditor.JS, RichTextEditor.HIGHLIGHT_JS })
		{
			assertEquals(file(script, ""), serve(RuntimeConfigurationType.DEVELOPMENT, script));
		}
	}

	@Test
	void theEditorsServeTheScriptsTheBuildMinifiedInDeployment() throws IOException
	{
		for (ResourceReference script : new ResourceReference[] { CodeEditor.JS,
				RichTextEditor.JS, RichTextEditor.HIGHLIGHT_JS })
		{
			String minified = file(script, ".min");
			assertTrue(minified.length() < file(script, "").length(), script.getName());
			// as the build wrote it, not stripped again by Wicket's compressor
			assertEquals(minified, serve(RuntimeConfigurationType.DEPLOYMENT, script));
		}
	}

	@Test
	void theOtherWidgetsServeTheirReadableScriptsInDevelopmentAndTheMinifiedOnesInDeployment()
		throws IOException
	{
		String readable = file(Templates.JS, "");
		assertTrue(readable.contains("Licensed under the Apache License"));
		assertEquals(readable, serve(RuntimeConfigurationType.DEVELOPMENT, Templates.JS));

		String deployed = serve(RuntimeConfigurationType.DEPLOYMENT, Templates.JS);
		assertFalse(deployed.contains("Licensed under the Apache License"), deployed);
		assertTrue(deployed.length() < readable.length(), deployed);
	}

	/**
	 * @return what an application of the mode serves for the script
	 */
	private static String serve(RuntimeConfigurationType mode, ResourceReference script)
	{
		WicketTester tester = new WicketTester(new MockApplication()
		{
			@Override
			public RuntimeConfigurationType getConfigurationType()
			{
				return mode;
			}
		});
		try
		{
			tester.startResourceReference(script);
			return tester.getLastResponseAsString();
		}
		finally
		{
			tester.destroy();
		}
	}

	/**
	 * @return the file the build wrote for the script: the readable one, or with {@code .min}
	 */
	private static String file(ResourceReference script, String min) throws IOException
	{
		String name = script.getName().replaceFirst("\\.js$", min + ".js");
		try (InputStream in = ((PackageResourceReference)script).getScope()
			.getResourceAsStream(name))
		{
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
