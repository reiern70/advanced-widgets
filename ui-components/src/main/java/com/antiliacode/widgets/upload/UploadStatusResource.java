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
package com.antiliacode.widgets.upload;

import java.time.Duration;

import org.apache.wicket.protocol.http.servlet.MultipartServletWebRequestImpl;
import org.apache.wicket.protocol.http.servlet.UploadInfo;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.SharedResourceReference;

import com.github.openjson.JSONObject;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Tells how far an upload of the session got, as JSON, for the {@link UploadProgressBar}s whose
 * upload's own request gives no progress: a form posted the usual way, or sent by Wicket's Ajax on
 * jQuery. While Wicket reads the upload of the id in the parameter {@value #UPLOAD_PARAMETER},
 * the answer is
 *
 * <pre>
 * {"state": "uploading", "bytesUploaded": 1048576, "totalBytes": 4194304, "percent": 25}
 * </pre>
 *
 * and {@code {"state": "none"}} before and after. Wicket keeps the progress of an upload only with
 * {@link org.apache.wicket.settings.ApplicationSettings#setUploadProgressUpdatesEnabled(boolean)}:
 * a form's under the id of its page, an {@link UploadToResourceField}'s under its markup id.
 * <p>
 * The resource is shared: the {@link com.antiliacode.widgets.Initializer} of ui-components registers
 * it under {@link #REFERENCE}. It never makes a session.
 *
 * @since 11.0.0
 */
public class UploadStatusResource extends AbstractResource
{
	private static final long serialVersionUID = 1L;

	/** The name the resource is shared under. */
	public static final String NAME = UploadStatusResource.class.getName();

	/** The reference of the shared resource. */
	public static final ResourceReference REFERENCE = new SharedResourceReference(NAME);

	/** The parameter naming the upload. */
	public static final String UPLOAD_PARAMETER = "upload";

	/**
	 * @param upload
	 *            the id of an upload
	 * @return the parameters asking for the status of an upload
	 */
	public static PageParameters parametersOf(String upload)
	{
		return new PageParameters().add(UPLOAD_PARAMETER, upload);
	}

	@Override
	protected ResourceResponse newResourceResponse(Attributes attributes)
	{
		String upload = attributes.getParameters().get(UPLOAD_PARAMETER).toOptionalString();
		HttpServletRequest request = (HttpServletRequest)attributes.getRequest()
			.getContainerRequest();
		UploadInfo info = upload != null && request.getSession(false) != null
			? MultipartServletWebRequestImpl.getUploadInfo(request, upload) : null;

		JSONObject status = new JSONObject();
		if (info == null || info.getTotalBytes() < 1)
		{
			status.put("state", "none");
		}
		else
		{
			status.put("state", "uploading");
			status.put("bytesUploaded", info.getBytesUploaded());
			status.put("totalBytes", info.getTotalBytes());
			status.put("percent", info.getPercentageComplete());
		}

		ResourceResponse response = new ResourceResponse();
		response.setContentType("application/json");
		response.setTextEncoding("UTF-8");
		response.setCacheDuration(Duration.ZERO);
		response.disableCaching();
		String json = status.toString();
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
