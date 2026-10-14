/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.download;

import java.util.Locale;

import org.apache.wicket.resource.loader.PackageStringResourceLoader;

/**
 * The texts of the files of the downloads demo, from the bundle of the package,
 * {@code wicket-package}: the files are made outside of any page too, by the
 * {@link DownloadsResource}.
 */
final class DownloadTexts
{
	private DownloadTexts()
	{
	}

	/**
	 * @return a text of the bundle of the package in a language, the key when there is none
	 */
	static String text(String key, Locale locale)
	{
		String text = new PackageStringResourceLoader().loadStringResource(DownloadTexts.class,
			key, locale, null, null);
		return text != null ? text : key;
	}
}
