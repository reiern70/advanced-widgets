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
package com.antiliacode.widgets.demo.download;

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
