/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
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
