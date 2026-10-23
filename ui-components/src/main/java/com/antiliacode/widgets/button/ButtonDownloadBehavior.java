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
package com.antiliacode.widgets.button;

import java.time.Instant;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.extensions.ajax.AjaxDownloadBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.lang.Args;

import com.github.openjson.JSONObject;

import jakarta.servlet.http.Cookie;

/**
 * Wicket's {@link AjaxDownloadBehavior} for a button: the button it is added to is busy while the
 * download runs, from the moment the download is {@link #initiate(IPartialPageRequestHandler)
 * initiated} until the browser tells the server how it ended. A busy button shows a spinner
 * instead of its icon, or before its text, screen readers hear it is busy ({@code aria-busy}), and
 * it ignores clicks, so that nobody starts the same download twice; it keeps the focus. Any
 * component drawn as a button takes the behavior, such as a form's
 * {@link org.apache.wicket.ajax.markup.html.form.AjaxButton} with a {@link ButtonBehavior},
 * which starts the download once the form is valid:
 *
 * <pre>
 * ButtonDownloadBehavior download = new ButtonDownloadBehavior(new ReportResource());
 * form.add(new AjaxButton("download")
 * {
 * 	&#64;Override
 * 	protected void onSubmit(AjaxRequestTarget target)
 * 	{
 * 		download.initiate(target);
 * 	}
 * }.add(new ButtonBehavior(Variant.PRIMARY, Size.MEDIUM), download));
 * </pre>
 *
 * {@link AjaxDownloadButton} is a button with one. Where the browser gets the file is Wicket's
 * {@link Location}: into a blob, the default, in a hidden iframe, in a new window or in the same
 * window. The resource answers with the file as an attachment, but for a new window, which shows
 * it inline: {@link #getContentDisposition()} says which. A resource of a
 * {@link #ButtonDownloadBehavior(ResourceReference) reference} calls
 * {@link AjaxDownloadBehavior#markCompleted(org.apache.wicket.request.resource.IResource.Attributes)}
 * when it answers, so the browser can tell it was downloaded.
 * <p>
 * A resource answering with an error, a status of 400 or more, is a failure the browser tells in
 * every location but the same window: in a hidden iframe or a new window too, where Wicket's
 * behavior alone takes the error for the file, as it marks the download completed before the
 * resource answers.
 * <p>
 * The button stays busy as long as the browser waits for the file. A browser that blocks the new
 * window, or a download in the same window whose resource fails, which takes the window to the
 * error, never tells the server: the button is done once the page is rendered again.
 *
 * @see AjaxDownloadButton
 * @since 11.0.0
 */
public class ButtonDownloadBehavior extends AjaxDownloadBehavior
{
	private static final long serialVersionUID = 1L;

	/**
	 * Download of a resource.
	 *
	 * @param resource
	 *            the resource to download
	 */
	public ButtonDownloadBehavior(IResource resource)
	{
		this(new FailureAwareResource(resource));
	}

	private ButtonDownloadBehavior(FailureAwareResource resource)
	{
		super(resource);
		resource.download = this;
	}

	/**
	 * Download of the resource of a reference, which calls
	 * {@link AjaxDownloadBehavior#markCompleted(org.apache.wicket.request.resource.IResource.Attributes)}
	 * when it answers.
	 *
	 * @param reference
	 *            the reference of the resource to download
	 */
	public ButtonDownloadBehavior(ResourceReference reference)
	{
		super(reference);
	}

	/**
	 * Download of the resource of a reference, which calls
	 * {@link AjaxDownloadBehavior#markCompleted(org.apache.wicket.request.resource.IResource.Attributes)}
	 * when it answers.
	 *
	 * @param reference
	 *            the reference of the resource to download
	 * @param parameters
	 *            the parameters of the resource
	 */
	public ButtonDownloadBehavior(ResourceReference reference, PageParameters parameters)
	{
		super(reference, parameters);
	}

	@Override
	protected void onBind()
	{
		super.onBind();
		getComponent().setOutputMarkupId(true);
	}

	/**
	 * Marks the button busy. An override calls this method.
	 */
	@Override
	protected void onBeforeDownload(IPartialPageRequestHandler handler)
	{
		super.onBeforeDownload(handler);
		handler.appendJavaScript(busy(getComponent(), true));
	}

	/**
	 * Marks the button done, then tells {@link #onDownloadSuccess(AjaxRequestTarget)} or
	 * {@link #onDownloadFailed(AjaxRequestTarget)}, and
	 * {@link #onDownloadCompleted(AjaxRequestTarget)}, how the download ended.
	 */
	@Override
	protected void respond(AjaxRequestTarget target)
	{
		target.appendJavaScript(busy(getComponent(), false));
		super.respond(target);
	}

	/**
	 * @return how the resource answers with the file: {@link ContentDisposition#INLINE inline} for
	 *         a download in a {@link Location#NewWindow new window}, which the window shows, and
	 *         as an {@link ContentDisposition#ATTACHMENT attachment}, which the browser saves,
	 *         otherwise
	 */
	public ContentDisposition getContentDisposition()
	{
		return getLocation() == Location.NewWindow ? ContentDisposition.INLINE
			: ContentDisposition.ATTACHMENT;
	}

	@Override
	public void renderHead(Component component, IHeaderResponse response)
	{
		super.renderHead(component, response);
		response.render(JavaScriptHeaderItem.forReference(ButtonBehavior.JS));
		response.render(CssHeaderItem.forReference(ButtonBehavior.CSS));
	}

	/**
	 * @return the name of the cookie telling the browser the file came, which
	 *         {@link AjaxDownloadBehavior} gives it
	 */
	private String getCookieName()
	{
		return "wicket-ajaxdownload-" + getComponent().getMarkupId() + "-" +
			getComponent().getBehaviorId(this);
	}

	/**
	 * @param component
	 *            a component drawn as a button
	 * @param busy
	 *            whether the button is busy, else done
	 * @return the script marking the button busy, or done
	 */
	public static String busy(Component component, boolean busy)
	{
		return "Wicket.Button.busy(" + JSONObject.quote(component.getMarkupId()) + "," + busy +
			");";
	}

	/**
	 * The resource of a download, whose answer tells the browser the file came only when it came.
	 * {@link AjaxDownloadBehavior} sets the cookie saying so before the resource answers, so that
	 * the browser takes an error for the file in an iframe or a new window: an answer with an
	 * error clears the cookie again, and the browser tells the failure.
	 */
	private static class FailureAwareResource implements IResource
	{
		private static final long serialVersionUID = 1L;

		private final IResource resource;

		private ButtonDownloadBehavior download;

		FailureAwareResource(IResource resource)
		{
			this.resource = Args.notNull(resource, "resource");
		}

		@Override
		public void respond(Attributes attributes)
		{
			resource.respond(new Attributes(attributes.getRequest(), new FailureAwareResponse(
				(WebResponse)attributes.getResponse(), download.getCookieName()),
				attributes.getParameters()));
		}
	}

	/**
	 * A response clearing the cookie of a download when it is an error, and doing what the
	 * response it wraps does otherwise.
	 */
	private static class FailureAwareResponse extends WebResponse
	{
		private final WebResponse response;

		private final String cookieName;

		FailureAwareResponse(WebResponse response, String cookieName)
		{
			this.response = response;
			this.cookieName = cookieName;
		}

		private void failed(int status)
		{
			if (status >= 400)
			{
				Cookie cookie = new Cookie(cookieName, "");
				cookie.setPath("/");
				response.clearCookie(cookie);
			}
		}

		@Override
		public void setStatus(int sc)
		{
			failed(sc);
			response.setStatus(sc);
		}

		@Override
		public void sendError(int sc, String msg)
		{
			failed(sc);
			response.sendError(sc, msg);
		}

		@Override
		public void addCookie(Cookie cookie)
		{
			response.addCookie(cookie);
		}

		@Override
		public void clearCookie(Cookie cookie)
		{
			response.clearCookie(cookie);
		}

		@Override
		public boolean isHeaderSupported()
		{
			return response.isHeaderSupported();
		}

		@Override
		public void setHeader(String name, String value)
		{
			response.setHeader(name, value);
		}

		@Override
		public void addHeader(String name, String value)
		{
			response.addHeader(name, value);
		}

		@Override
		public void setDateHeader(String name, Instant date)
		{
			response.setDateHeader(name, date);
		}

		@Override
		public void setContentLength(long length)
		{
			response.setContentLength(length);
		}

		@Override
		public void setContentType(String mimeType)
		{
			response.setContentType(mimeType);
		}

		@Override
		public String encodeRedirectURL(CharSequence url)
		{
			return response.encodeRedirectURL(url);
		}

		@Override
		public void sendRedirect(String url)
		{
			response.sendRedirect(url);
		}

		@Override
		public boolean isRedirect()
		{
			return response.isRedirect();
		}

		@Override
		public void flush()
		{
			response.flush();
		}

		@Override
		public void write(CharSequence sequence)
		{
			response.write(sequence);
		}

		@Override
		public void write(byte[] array)
		{
			response.write(array);
		}

		@Override
		public void write(byte[] array, int offset, int length)
		{
			response.write(array, offset, length);
		}

		@Override
		public void close()
		{
			response.close();
		}

		@Override
		public void reset()
		{
			response.reset();
		}

		@Override
		public String encodeURL(CharSequence url)
		{
			return response.encodeURL(url);
		}

		@Override
		public Object getContainerResponse()
		{
			return response.getContainerResponse();
		}
	}
}
