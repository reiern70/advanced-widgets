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
package com.antilia.widgets.button;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.AjaxDownloadBehavior;
import org.apache.wicket.extensions.ajax.AjaxDownloadBehavior.Location;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.cookies.CookieDefaults;

import com.antilia.widgets.button.ButtonBehavior.Size;
import com.antilia.widgets.button.ButtonBehavior.Variant;
import com.antilia.widgets.icon.IIcon;

/**
 * A button in the colors of the theme downloading a file via Ajax, with Wicket's
 * {@link AjaxDownloadBehavior}: the page stays as it is, and hears whether the file was
 * downloaded. While the download runs, the button is busy: it shows a spinner instead of its
 * icon, or before its text, and ignores clicks (see {@link ButtonDownloadBehavior}). Like an
 * {@link AjaxActionButton}, it shows an icon, a text or both, in a {@link Variant} and a
 * {@link Size}; attach it to a {@code <button>}.
 *
 * <pre>
 * add(new AjaxDownloadButton("report", Model.of("Report"), new ReportResource())
 * {
 * 	&#64;Override
 * 	protected void onDownloadSuccess(AjaxRequestTarget target)
 * 	{
 * 		...
 * 	}
 * }.setLocation(Location.IFrame).setIcon(SvgIcon.DOWNLOAD));
 * </pre>
 *
 * The {@link #setLocation(Location) location} says where the browser gets the file: into a blob,
 * which it saves, the default; in a hidden iframe, which keeps the page and its WebSockets as
 * they are; in a new window, which shows it; or in the same window, which saves it but fires the
 * page's unload. The resource answers with the file as {@link #getContentDisposition()} says:
 * inline for a new window, as an attachment otherwise.
 *
 * @see ButtonDownloadBehavior
 * @since 11.0.0
 */
public class AjaxDownloadButton extends AjaxActionButton
{
	private static final long serialVersionUID = 1L;

	private final ButtonDownloadBehavior download;

	/**
	 * @param id
	 *            component id
	 * @param label
	 *            the text of the button; with only an icon shown, its tooltip and accessible name
	 * @param resource
	 *            the resource to download
	 */
	public AjaxDownloadButton(String id, IModel<String> label, IResource resource)
	{
		this(id, label, new DownloadBehavior(resource));
	}

	/**
	 * @param id
	 *            component id
	 * @param label
	 *            the text of the button; with only an icon shown, its tooltip and accessible name
	 * @param reference
	 *            the reference of the resource to download, which calls
	 *            {@link AjaxDownloadBehavior#markCompleted(org.apache.wicket.request.resource.IResource.Attributes)}
	 *            when it answers
	 */
	public AjaxDownloadButton(String id, IModel<String> label, ResourceReference reference)
	{
		this(id, label, reference, null);
	}

	/**
	 * @param id
	 *            component id
	 * @param label
	 *            the text of the button; with only an icon shown, its tooltip and accessible name
	 * @param reference
	 *            the reference of the resource to download, which calls
	 *            {@link AjaxDownloadBehavior#markCompleted(org.apache.wicket.request.resource.IResource.Attributes)}
	 *            when it answers
	 * @param parameters
	 *            the parameters of the resource
	 */
	public AjaxDownloadButton(String id, IModel<String> label, ResourceReference reference,
		PageParameters parameters)
	{
		this(id, label, new DownloadBehavior(reference, parameters));
	}

	private AjaxDownloadButton(String id, IModel<String> label, DownloadBehavior download)
	{
		super(id, label);
		this.download = download;
		add(download);
	}

	/**
	 * Starts the download. An override may do something first, or start the download with
	 * parameters of the moment through {@link #getDownloadBehavior()}.
	 */
	@Override
	public void onClick(AjaxRequestTarget target)
	{
		download.initiate(target);
	}

	/**
	 * @return the behavior downloading the file
	 */
	public ButtonDownloadBehavior getDownloadBehavior()
	{
		return download;
	}

	/**
	 * @return where the browser gets the file, {@link Location#Blob} by default
	 */
	public Location getLocation()
	{
		return download.getLocation();
	}

	/**
	 * @param location
	 *            where the browser gets the file
	 * @return this
	 */
	public AjaxDownloadButton setLocation(Location location)
	{
		download.setLocation(location);
		return this;
	}

	/**
	 * @return the SameSite attribute of the cookie telling the browser the file was downloaded
	 */
	public CookieDefaults.SameSite getSameSite()
	{
		return download.getSameSite();
	}

	/**
	 * @param sameSite
	 *            the SameSite attribute of the cookie telling the browser the file was downloaded
	 * @return this
	 */
	public AjaxDownloadButton setSameSite(CookieDefaults.SameSite sameSite)
	{
		download.setSameSite(sameSite);
		return this;
	}

	/**
	 * @return how the resource answers with the file: inline for a download in a new window, as an
	 *         attachment otherwise
	 * @see ButtonDownloadBehavior#getContentDisposition()
	 */
	public ContentDisposition getContentDisposition()
	{
		return download.getContentDisposition();
	}

	@Override
	public AjaxDownloadButton setIcon(IIcon icon)
	{
		super.setIcon(icon);
		return this;
	}

	@Override
	public AjaxDownloadButton setVariant(Variant variant)
	{
		super.setVariant(variant);
		return this;
	}

	@Override
	public AjaxDownloadButton setSize(Size size)
	{
		super.setSize(size);
		return this;
	}

	@Override
	public AjaxDownloadButton setIconOnly(boolean iconOnly)
	{
		super.setIconOnly(iconOnly);
		return this;
	}

	/**
	 * Called when the file was downloaded. Does nothing by default.
	 *
	 * @param target
	 *            the Ajax request target
	 */
	protected void onDownloadSuccess(AjaxRequestTarget target)
	{
	}

	/**
	 * Called when the download failed, because the resource answered with an error for example.
	 * Never called for a download in the {@link Location#SameWindow same window}, whose failure
	 * the browser cannot tell. Does nothing by default.
	 *
	 * @param target
	 *            the Ajax request target
	 */
	protected void onDownloadFailed(AjaxRequestTarget target)
	{
	}

	/**
	 * Called when the download ended, after {@link #onDownloadSuccess(AjaxRequestTarget)} or
	 * {@link #onDownloadFailed(AjaxRequestTarget)}. Does nothing by default.
	 *
	 * @param target
	 *            the Ajax request target
	 */
	protected void onDownloadCompleted(AjaxRequestTarget target)
	{
	}

	/**
	 * The download of the button, telling the button how it ended.
	 */
	private static class DownloadBehavior extends ButtonDownloadBehavior
	{
		private static final long serialVersionUID = 1L;

		DownloadBehavior(IResource resource)
		{
			super(resource);
		}

		DownloadBehavior(ResourceReference reference, PageParameters parameters)
		{
			super(reference, parameters);
		}

		private AjaxDownloadButton button()
		{
			return (AjaxDownloadButton)getComponent();
		}

		@Override
		protected void onDownloadSuccess(AjaxRequestTarget target)
		{
			button().onDownloadSuccess(target);
		}

		@Override
		protected void onDownloadFailed(AjaxRequestTarget target)
		{
			button().onDownloadFailed(target);
		}

		@Override
		protected void onDownloadCompleted(AjaxRequestTarget target)
		{
			button().onDownloadCompleted(target);
		}
	}
}
