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
package com.antiliacode.widgets.demo.richtext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;

import com.antiliacode.widgets.richtext.RichTextImageType;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Serves the images users uploaded into the rich text editors of the demo, which
 * {@link RichTextImages} keeps: mounted by the application at {@value #PATH}, the name of an image
 * after it. Only names the store gave, each as the type of image its bytes said, with
 * {@code nosniff}: a file can never be served as a page. Any other name is not found.
 */
public class RichTextImageResource extends AbstractResource
{
	private static final long serialVersionUID = 1L;

	/** The path the resource is mounted at. */
	public static final String PATH = RichTextImages.PATH + "/${name}";

	/** The reference of the resource, to mount it. */
	public static final ResourceReference REFERENCE = new ResourceReference(
		RichTextImageResource.class, "images")
	{
		private static final long serialVersionUID = 1L;

		@Override
		public IResource getResource()
		{
			return new RichTextImageResource(RichTextImages.get());
		}
	};

	private final RichTextImages images;

	/**
	 * @param images
	 *            the store of the images
	 */
	RichTextImageResource(RichTextImages images)
	{
		this.images = images;
	}

	@Override
	protected ResourceResponse newResourceResponse(Attributes attributes)
	{
		ResourceResponse response = new ResourceResponse();
		Path file = images.find(attributes.getParameters().get("name").toOptionalString());
		RichTextImageType type = file == null ? null : RichTextImages.typeOf(file);
		long size;
		Instant modified;
		try
		{
			size = file == null ? -1 : Files.size(file);
			modified = file == null ? null : Files.getLastModifiedTime(file).toInstant();
		}
		catch (IOException e)
		{
			size = -1;
			modified = null;
		}
		if (type == null || size < 0)
		{
			response.setError(HttpServletResponse.SC_NOT_FOUND);
			return response;
		}
		response.setContentType(type.getMimeType());
		response.setContentLength(size);
		response.setLastModified(modified);
		response.setCacheDuration(Duration.ofHours(1));
		response.setCacheScope(WebResponse.CacheScope.PRIVATE);
		response.getHeaders().addHeader("X-Content-Type-Options", "nosniff");
		if (response.dataNeedsToBeWritten(attributes))
		{
			response.setWriteCallback(new WriteCallback()
			{
				@Override
				public void writeData(Attributes attributes) throws IOException
				{
					Files.copy(file, attributes.getResponse().getOutputStream());
				}
			});
		}
		return response;
	}
}
