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
package com.antiliacode.widgets.ckeditor;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.wicket.Application;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.UrlResourceReference;
import org.apache.wicket.util.lang.Args;

/**
 * CKEditor 5 as the application brings it, which the {@link CKEditor editors} and the
 * {@link CKEditorView views} of the application use. ui-components holds no code of CKEditor,
 * which is licensed under the GPL 2 or later, or under the commercial terms of CKSource: the
 * application chooses its license, and brings CKEditor and its license key.
 * <ul>
 * <li>The script of CKEditor's browser build, {@code ckeditor5/dist/browser/ckeditor5.umd.js} of
 * the npm package, which defines the global {@code CKEDITOR}, or a build of the application's own
 * that does too.</li>
 * <li>Its style sheet, {@code ckeditor5.css}, and the style sheet of the text as CKEditor writes
 * it, {@code ckeditor5-content.css}, which {@link CKEditorView} renders.</li>
 * <li>The scripts of its translations, {@code ckeditor5/dist/translations/de.umd.js} and so on, by
 * their languages: an editor in a page of one of those languages speaks it.</li>
 * <li>The license key: {@value #GPL} for the open-source distribution, under the GPL, or the key
 * CKSource gave for its commercial terms or its CDN.</li>
 * </ul>
 * The scripts and style sheets are resource references: package resources of the application, or
 * addresses, of a CDN for example ({@link #url(String)}). An application sets the library once, as
 * it starts:
 *
 * <pre>
 * CKEditorLibrary.set(this, CKEditorLibrary.of(
 * 	new JavaScriptResourceReference(MyApplication.class, "ckeditor5/ckeditor5.umd.js"),
 * 	new CssResourceReference(MyApplication.class, "ckeditor5/ckeditor5.css"),
 * 	System.getenv().getOrDefault("CKEDITOR_LICENSE_KEY", CKEditorLibrary.GPL))
 * 	.withContentCss(new CssResourceReference(MyApplication.class, "ckeditor5/ckeditor5-content.css"))
 * 	.withTranslation("de", new JavaScriptResourceReference(MyApplication.class, "ckeditor5/translations/de.umd.js")));
 * </pre>
 *
 * The library is immutable: each {@code with} gives another.
 *
 * @since 11.0.0
 */
public final class CKEditorLibrary implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** The license key of the open-source distribution, under the GPL. */
	public static final String GPL = "GPL";

	/** Where the application keeps its library. */
	private static final MetaDataKey<CKEditorLibrary> KEY = new MetaDataKey<>()
	{
		private static final long serialVersionUID = 1L;
	};

	private final ResourceReference script;

	private final ResourceReference css;

	private final ResourceReference contentCss;

	private final Map<String, ResourceReference> translations;

	private final String licenseKey;

	private CKEditorLibrary(ResourceReference script, ResourceReference css,
		ResourceReference contentCss, Map<String, ResourceReference> translations, String licenseKey)
	{
		this.script = Args.notNull(script, "script");
		this.css = Args.notNull(css, "css");
		this.contentCss = contentCss;
		this.translations = Collections.unmodifiableMap(new LinkedHashMap<>(translations));
		this.licenseKey = Args.notEmpty(licenseKey, "licenseKey");
	}

	/**
	 * @param script
	 *            the script of CKEditor, which defines the global {@code CKEDITOR}
	 * @param css
	 *            its style sheet, {@code ckeditor5.css}
	 * @param licenseKey
	 *            the license key, {@value #GPL} for the open-source distribution
	 * @return the library, without translations nor a style sheet of the text
	 */
	public static CKEditorLibrary of(ResourceReference script, ResourceReference css,
		String licenseKey)
	{
		return new CKEditorLibrary(script, css, null, Map.of(), licenseKey);
	}

	/**
	 * @param scriptUrl
	 *            the address of the script of CKEditor, of a CDN for example
	 * @param cssUrl
	 *            the address of its style sheet
	 * @param licenseKey
	 *            the license key: CKSource's CDN needs one of its own
	 * @return the library, without translations nor a style sheet of the text
	 */
	public static CKEditorLibrary fromUrls(String scriptUrl, String cssUrl, String licenseKey)
	{
		return of(url(scriptUrl), url(cssUrl), licenseKey);
	}

	/**
	 * @param url
	 *            an address, absolute or relative to the application
	 * @return a reference to the script or the style sheet at the address
	 */
	public static ResourceReference url(String url)
	{
		return new UrlResourceReference(Url.parse(Args.notEmpty(url, "url")));
	}

	/**
	 * @param contentCss
	 *            the style sheet of the text as CKEditor writes it, {@code ckeditor5-content.css},
	 *            which {@link CKEditorView} renders; {@code null} for none
	 * @return a library with it
	 */
	public CKEditorLibrary withContentCss(ResourceReference contentCss)
	{
		return new CKEditorLibrary(script, css, contentCss, translations, licenseKey);
	}

	/**
	 * @param language
	 *            a language of CKEditor, {@code de} or {@code pt-br} for example
	 * @param translation
	 *            the script of its translation, {@code translations/de.umd.js} of the browser
	 *            build; {@code null} for none
	 * @return a library with it
	 */
	public CKEditorLibrary withTranslation(String language, ResourceReference translation)
	{
		Map<String, ResourceReference> all = new LinkedHashMap<>(translations);
		String key = normalize(Args.notEmpty(language, "language"));
		if (translation == null)
		{
			all.remove(key);
		}
		else
		{
			all.put(key, translation);
		}
		return new CKEditorLibrary(script, css, contentCss, all, licenseKey);
	}

	/**
	 * @param licenseKey
	 *            the license key, {@value #GPL} for the open-source distribution
	 * @return a library with it
	 */
	public CKEditorLibrary withLicenseKey(String licenseKey)
	{
		return new CKEditorLibrary(script, css, contentCss, translations, licenseKey);
	}

	/**
	 * @return the script of CKEditor, which defines the global {@code CKEDITOR}
	 */
	public ResourceReference getScript()
	{
		return script;
	}

	/**
	 * @return its style sheet
	 */
	public ResourceReference getCss()
	{
		return css;
	}

	/**
	 * @return the style sheet of the text as CKEditor writes it, {@code null} for none
	 */
	public ResourceReference getContentCss()
	{
		return contentCss;
	}

	/**
	 * @return the scripts of the translations, by their languages
	 */
	public Map<String, ResourceReference> getTranslations()
	{
		return translations;
	}

	/**
	 * @param locale
	 *            the locale of a page
	 * @return the language of CKEditor the library has a translation of for the locale: its tag,
	 *         {@code pt-br} for example, or else its language; {@code null} for none, CKEditor
	 *         then speaks English
	 */
	public String getLanguage(Locale locale)
	{
		if (locale == null)
		{
			return null;
		}
		String tag = normalize(locale.toLanguageTag());
		if (translations.containsKey(tag))
		{
			return tag;
		}
		String language = normalize(locale.getLanguage());
		return translations.containsKey(language) ? language : null;
	}

	/**
	 * @param language
	 *            a language of CKEditor
	 * @return the script of its translation, {@code null} for none
	 */
	public ResourceReference getTranslation(String language)
	{
		return language == null ? null : translations.get(normalize(language));
	}

	/**
	 * @return the license key
	 */
	public String getLicenseKey()
	{
		return licenseKey;
	}

	/**
	 * @return whether the license key is that of the open-source distribution, under the GPL
	 */
	public boolean isGpl()
	{
		return GPL.equals(licenseKey);
	}

	/**
	 * Sets the library of an application, which its editors and views use.
	 *
	 * @param application
	 *            the application
	 * @param library
	 *            the library, {@code null} for none
	 */
	public static void set(Application application, CKEditorLibrary library)
	{
		Args.notNull(application, "application").setMetaData(KEY, library);
	}

	/**
	 * @param application
	 *            the application
	 * @return whether the application set a library
	 */
	public static boolean isSet(Application application)
	{
		return application != null && application.getMetaData(KEY) != null;
	}

	/**
	 * @return the library of the application of the thread
	 * @throws IllegalStateException
	 *             when the application set none
	 */
	public static CKEditorLibrary get()
	{
		return get(Application.get());
	}

	/**
	 * @param application
	 *            the application
	 * @return the library of the application
	 * @throws IllegalStateException
	 *             when the application set none
	 */
	public static CKEditorLibrary get(Application application)
	{
		CKEditorLibrary library = Args.notNull(application, "application").getMetaData(KEY);
		if (library == null)
		{
			throw new IllegalStateException("The application brings no CKEditor: ui-components " +
				"holds none, for its license. Set the script of CKEditor's browser build, its " +
				"style sheet and the license key as the application starts, with " +
				"CKEditorLibrary.set(application, CKEditorLibrary.of(script, css, licenseKey)), " +
				"or give the editor one with CKEditor#setLibrary.");
		}
		return library;
	}

	private static String normalize(String language)
	{
		return language.trim().toLowerCase(Locale.ROOT).replace('_', '-');
	}

	@Override
	public boolean equals(Object other)
	{
		return other instanceof CKEditorLibrary library && library.script.equals(script) &&
			library.css.equals(css) && Objects.equals(library.contentCss, contentCss) &&
			library.translations.equals(translations) && library.licenseKey.equals(licenseKey);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(script, css, contentCss, translations, licenseKey);
	}

	/**
	 * @return the library, without its license key
	 */
	@Override
	public String toString()
	{
		return "CKEditorLibrary[" + script + ", " + css + ", translations=" +
			translations.keySet() + ", gpl=" + isGpl() + "]";
	}
}
