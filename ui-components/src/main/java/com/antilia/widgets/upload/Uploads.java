/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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
package com.antilia.widgets.upload;

import org.apache.wicket.Application;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;

import com.antilia.widgets.button.ButtonBehavior;
import com.antilia.widgets.progress.ProgressBar;

/**
 * What the uploads need in the browser: the script {@code Wicket.Upload}, holding the file picker,
 * the progress of uploads and the guard of the page while an upload runs, and their style sheet.
 * The pickers' buttons are those of {@link ButtonBehavior}, the progress a {@link ProgressBar}, so
 * their style sheets come along.
 *
 * @see FilePickerBehavior
 * @see UploadProgressBar
 * @see UploadToResourceField
 * @see UploadGuard
 * @since 11.0.0
 */
public final class Uploads
{
	/** The script of the uploads, {@code Wicket.Upload}. */
	public static final ResourceReference JS = new JavaScriptResourceReference(Uploads.class,
		"antilia-upload.js");

	/** The style sheet of the file pickers and the progress of uploads. */
	public static final ResourceReference CSS = new CssResourceReference(Uploads.class,
		"antilia-upload.css");

	private Uploads()
	{
	}

	/**
	 * Renders what the uploads need in the browser: Wicket's Ajax, whose requests the guard of an
	 * upload stops, {@link #JS}, {@link #CSS} and the style sheets of the buttons and the progress
	 * bars.
	 *
	 * @param response
	 *            the header response
	 */
	public static void renderHeadItems(IHeaderResponse response)
	{
		CoreLibrariesContributor.contributeAjax(Application.get(), response);
		response.render(JavaScriptHeaderItem.forReference(JS));
		response.render(CssHeaderItem.forReference(ButtonBehavior.CSS));
		response.render(CssHeaderItem.forReference(ProgressBar.CSS));
		response.render(CssHeaderItem.forReference(CSS));
	}
}
