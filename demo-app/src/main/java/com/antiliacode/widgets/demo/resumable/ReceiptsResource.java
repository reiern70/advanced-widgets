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
package com.antiliacode.widgets.demo.resumable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import java.io.File;

import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;

import com.github.openjson.JSONObject;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Where the React and Svelte apps of the demo hand over a file the server stored whole: a
 * {@code POST} with the name it was stored under in the parameter {@value #NAME_PARAMETER}, which
 * the resource told the app, and the name of the file as the browser told it in
 * {@value #CLIENT_NAME_PARAMETER}, answers its {@link Receipt} as JSON and deletes it. The file is looked
 * for among those stored for the apps of the session of the request
 * ({@link DemoResumableUploads#existingClientId()}): any other, or none, is answered 404.
 */
public class ReceiptsResource extends AbstractResource
{
	private static final long serialVersionUID = 1L;

	/** Where the resource is mounted. */
	public static final String PATH = "resumable-upload-receipts";

	/** The parameter of the name the file was stored under. */
	public static final String NAME_PARAMETER = "name";

	/** The parameter of the name of the file, as the browser told it. */
	public static final String CLIENT_NAME_PARAMETER = "clientFileName";

	/** The reference of the resource. */
	public static final ResourceReference REFERENCE = new ResourceReference(
		ReceiptsResource.class, "receipts")
	{
		private static final long serialVersionUID = 1L;

		@Override
		public IResource getResource()
		{
			return new ReceiptsResource();
		}
	};

	@Override
	protected ResourceResponse newResourceResponse(Attributes attributes)
	{
		ResourceResponse response = new ResourceResponse();
		response.disableCaching();
		HttpServletRequest request = (HttpServletRequest)attributes.getRequest()
			.getContainerRequest();
		if (!"POST".equals(request.getMethod()))
		{
			return response.setError(405);
		}
		String name = attributes.getRequest().getPostParameters()
			.getParameterValue(NAME_PARAMETER).toOptionalString();
		String clientId = DemoResumableUploads.existingClientId();
		if (name == null || clientId == null)
		{
			return response.setError(404);
		}
		File file;
		try
		{
			// the file manager refuses a name leading out of the folder of the apps
			file = DemoResumableUploads.get().getFileManager().getFile(clientId, name);
		}
		catch (RuntimeException e)
		{
			return response.setError(404);
		}
		if (!file.isFile())
		{
			return response.setError(404);
		}
		JSONObject receipt;
		try
		{
			String clientName = attributes.getRequest().getPostParameters()
				.getParameterValue(CLIENT_NAME_PARAMETER).toString(name);
			receipt = Receipt.of(clientName, file).toJson();
		}
		catch (IOException e)
		{
			return response.setError(500);
		}
		finally
		{
			file.delete();
		}
		byte[] json = receipt.toString().getBytes(StandardCharsets.UTF_8);
		response.setContentType("application/json");
		response.setTextEncoding("UTF-8");
		response.setContentLength(json.length);
		response.setWriteCallback(new WriteCallback()
		{
			@Override
			public void writeData(Attributes attributes)
			{
				attributes.getResponse().write(json);
			}
		});
		return response;
	}
}
