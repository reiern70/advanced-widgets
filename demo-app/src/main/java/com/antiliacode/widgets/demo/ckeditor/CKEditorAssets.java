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
package com.antiliacode.widgets.demo.ckeditor;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.Properties;

import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.util.string.Strings;

import com.antiliacode.widgets.ckeditor.CKEditorLibrary;
import com.antiliacode.widgets.resource.AlreadyCompressedJavaScriptResourceReference;

/**
 * CKEditor 5 as the demo application brings it: the browser build of the npm package ckeditor5,
 * which the build of frontend/demo copies into this package, under {@code ckeditor5/}, with its
 * style sheets, its German and Spanish translations and its license, the GPL 2 or later. The
 * components hold no code of CKEditor: an application brings it so, with its license key.
 * <p>
 * The license key is a setting of the application: the system property
 * {@value #LICENSE_KEY_PROPERTY}, else the environment variable {@value #LICENSE_KEY_VARIABLE},
 * else {@link CKEditorLibrary#GPL}.
 */
public final class CKEditorAssets
{
	/** The system property of CKEditor's license key. */
	public static final String LICENSE_KEY_PROPERTY = "antilia-widgets.ckeditor-license-key";

	/** The environment variable of CKEditor's license key. */
	public static final String LICENSE_KEY_VARIABLE = "CKEDITOR_LICENSE_KEY";

	/** Where the build put CKEditor, in this package. */
	private static final String FOLDER = "ckeditor5/";

	private CKEditorAssets()
	{
	}

	/**
	 * @return CKEditor as the demo brings it, with the license key of the application; its script
	 *         and translations minified already, which Wicket's compressor leaves as they are
	 */
	public static CKEditorLibrary library()
	{
		return CKEditorLibrary.of(
			new AlreadyCompressedJavaScriptResourceReference(CKEditorAssets.class,
				FOLDER + "ckeditor5.umd.js"),
			new CssResourceReference(CKEditorAssets.class, FOLDER + "ckeditor5.css"),
			licenseKey())
			.withContentCss(new CssResourceReference(CKEditorAssets.class,
				FOLDER + "ckeditor5-content.css"))
			.withTranslation("de", new AlreadyCompressedJavaScriptResourceReference(
				CKEditorAssets.class, FOLDER + "translations/de.umd.js"))
			.withTranslation("es", new AlreadyCompressedJavaScriptResourceReference(
				CKEditorAssets.class, FOLDER + "translations/es.umd.js"));
	}

	/**
	 * @return the license key of the application: the system property, the environment variable,
	 *         or GPL
	 */
	static String licenseKey()
	{
		String key = System.getProperty(LICENSE_KEY_PROPERTY);
		if (Strings.isEmpty(key))
		{
			key = System.getenv(LICENSE_KEY_VARIABLE);
		}
		return Strings.isEmpty(key) ? CKEditorLibrary.GPL : key.trim();
	}

	/**
	 * CKEditor as the build of frontend/demo copied it.
	 *
	 * @param name
	 *            its name, CKEditor 5
	 * @param version
	 *            the version of the npm package ckeditor5
	 * @param url
	 *            its site
	 */
	record Release(String name, String version, String url) implements Serializable
	{
	}

	/**
	 * @return CKEditor, as the build wrote its name, version and site beside it
	 */
	static Release release()
	{
		Properties properties = new Properties();
		try (InputStream in = CKEditorAssets.class.getResourceAsStream(FOLDER +
			"ckeditor5.properties"))
		{
			if (in == null)
			{
				throw new IllegalStateException("No " + FOLDER + "ckeditor5.properties beside " +
					CKEditorAssets.class.getName() + ": build frontend/demo");
			}
			properties.load(in);
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
		return new Release(properties.getProperty("name"), properties.getProperty("version"),
			properties.getProperty("url"));
	}
}
