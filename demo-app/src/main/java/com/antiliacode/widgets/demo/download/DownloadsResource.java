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

import org.apache.wicket.extensions.ajax.AjaxDownloadBehavior;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;

/**
 * The files of the downloads demo at URLs of their own, which can be bookmarked and shared,
 * mounted by the application at {@value #PATH}: {@code files/countries.csv} for example, for each
 * {@link DemoFile}, all countries in the language of the session. With {@value #OPEN} the file is
 * sent inline, for the browser to show, else as an attachment, which it saves. Any other name is
 * not found.
 * <p>
 * The resource marks the download completed when an {@link AjaxDownloadBehavior} of its
 * reference asks for it, as such a resource must.
 */
public class DownloadsResource extends DemoFileResource
{
	private static final long serialVersionUID = 1L;

	/** The path the resource is mounted at. */
	public static final String PATH = "files/${file}";

	/** The parameter of the name of the file, {@code countries.csv} for example. */
	public static final String FILE = "file";

	/** The parameter asking for the file inline. */
	public static final String OPEN = "open";

	/** The parameter naming the download of an {@link AjaxDownloadBehavior}, if it asks. */
	private static final String AJAX_DOWNLOAD = "wicket-ajaxdownload";

	/** The reference of the resource, to mount it and to link to it. */
	public static final ResourceReference REFERENCE = new ResourceReference(
		DownloadsResource.class, "files")
	{
		private static final long serialVersionUID = 1L;

		@Override
		public IResource getResource()
		{
			return new DownloadsResource();
		}
	};

	/**
	 * @param file
	 *            a kind of file
	 * @param open
	 *            whether the browser is to show the file, else to save it
	 * @return the parameters of the link to the file
	 */
	public static PageParameters parameters(DemoFile file, boolean open)
	{
		PageParameters parameters = new PageParameters().set(FILE, file.fileName("countries"));
		return open ? parameters.set(OPEN, true) : parameters;
	}

	@Override
	protected DemoFile getFile(Attributes attributes)
	{
		String name = attributes.getParameters().get(FILE).toString("");
		DemoFile file = DemoFile.ofFileName(name);
		return file != null && file.fileName(getName()).equals(name) ? file : null;
	}

	@Override
	protected ContentDisposition getContentDisposition(Attributes attributes)
	{
		return attributes.getParameters().get(OPEN).toBoolean(false) ? ContentDisposition.INLINE
			: ContentDisposition.ATTACHMENT;
	}

	@Override
	protected ResourceResponse newResourceResponse(Attributes attributes)
	{
		if (!attributes.getParameters().get(AJAX_DOWNLOAD).isEmpty())
		{
			AjaxDownloadBehavior.markCompleted(attributes);
		}
		return super.newResourceResponse(attributes);
	}
}
