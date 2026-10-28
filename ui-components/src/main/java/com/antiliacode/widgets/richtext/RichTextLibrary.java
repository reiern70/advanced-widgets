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
package com.antiliacode.widgets.richtext;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * The JavaScript library a {@link RichTextEditor} is based on: Tiptap, on ProseMirror. Its name,
 * the version {@link RichTextEditor#JS} bundles and its site, as the build of the script wrote
 * them, for a page telling what it runs.
 *
 * @param name
 *            the name of the library, Tiptap
 * @param version
 *            the version of {@code @tiptap/core} the script bundles, 3.32.0 for example
 * @param url
 *            the site of the library
 * @since 11.0.0
 */
public record RichTextLibrary(String name, String version, String url) implements Serializable
{
	/** Written by the build of the script, beside it. */
	private static final String RESOURCE = "antilia-rich-text-library.properties";

	/** The library of the script. */
	private static final RichTextLibrary BUNDLED = read();

	/**
	 * @return the library {@link RichTextEditor#JS} bundles
	 */
	public static RichTextLibrary get()
	{
		return BUNDLED;
	}

	private static RichTextLibrary read()
	{
		Properties properties = new Properties();
		try (InputStream in = RichTextLibrary.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
			{
				throw new IllegalStateException(RESOURCE + " is missing: build frontend/core");
			}
			properties.load(in);
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
		return new RichTextLibrary(properties.getProperty("name"), properties.getProperty(
			"version"), properties.getProperty("url"));
	}

	@Override
	public String toString()
	{
		return name + " " + version;
	}
}
