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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.apache.wicket.Application;
import org.apache.wicket.markup.html.form.upload.resource.AbstractFileUploadResource;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.string.StringValueConversionException;

/**
 * The id of an upload field and the limits of its uploads, which the field signs and its script
 * sends along with every request, as the query parameters of
 * {@link org.apache.wicket.markup.html.form.upload.resource.FileUploadToResourceField} are.
 *
 * @param uploadFieldId
 *            the unique id of the field
 * @param maxSize
 *            the size of the files sent at once at most, in bytes
 * @param fileMaxSize
 *            the size of a file at most, in bytes, -1 for none
 * @param fileCountMax
 *            the number of files sent at once at most, -1 for any
 */
record UploadGrant(String uploadFieldId, long maxSize, long fileMaxSize, long fileCountMax)
{
	/** Keeps the tokens of these fields apart from those of other uploads. */
	private static final String PURPOSE = "resumable-upload";

	/** The largest number a script holds exactly, {@code Number.MAX_SAFE_INTEGER}. */
	private static final long MAX_SAFE_INTEGER = (1L << 53) - 1;

	/**
	 * @return the grant of a field, its sizes no larger than a script holds exactly: it sends them
	 *         back as it got them
	 */
	static UploadGrant of(String uploadFieldId, Bytes maxSize, Bytes fileMaxSize,
		long fileCountMax)
	{
		return new UploadGrant(uploadFieldId, Math.min(maxSize.bytes(), MAX_SAFE_INTEGER),
			fileMaxSize != null ? Math.min(fileMaxSize.bytes(), MAX_SAFE_INTEGER) : -1,
			fileCountMax);
	}

	/**
	 * @return the size of a file at most, in bytes
	 */
	long maxFileSize()
	{
		return fileMaxSize >= 0 ? Math.min(maxSize, fileMaxSize) : maxSize;
	}

	/**
	 * @return the grant signed, for the browser to send it back
	 */
	String token()
	{
		return Application.get()
			.getSecuritySettings()
			.getCryptFactory()
			.newCrypt()
			.encryptUrlSafe(serialized());
	}

	/**
	 * @return the query of the grant, as the script of
	 *         {@link org.apache.wicket.markup.html.form.upload.resource.FileUploadToResourceField}
	 *         writes it
	 */
	String query()
	{
		return AbstractFileUploadResource.UPLOAD_ID + '=' + encoded(uploadFieldId) + '&' +
			AbstractFileUploadResource.MAX_SIZE + '=' + maxSize + '&' +
			AbstractFileUploadResource.FILE_MAX_SIZE + '=' + fileMaxSize + '&' +
			AbstractFileUploadResource.FILE_COUNT_MAX + '=' + fileCountMax + '&' +
			AbstractFileUploadResource.UPLOAD_TOKEN + '=' + encoded(token());
	}

	private static String encoded(String value)
	{
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private String serialized()
	{
		return PURPOSE + '\n' + uploadFieldId + '\n' + maxSize + '\n' + fileMaxSize + '\n' +
			fileCountMax;
	}

	/**
	 * @param parameters
	 *            the query parameters of a request
	 * @return the grant the parameters carry, {@code null} when they carry none, or one whose
	 *         token does not match
	 */
	static UploadGrant of(IRequestParameters parameters)
	{
		String token = parameters.getParameterValue(AbstractFileUploadResource.UPLOAD_TOKEN)
			.toOptionalString();
		String uploadFieldId = parameters.getParameterValue(AbstractFileUploadResource.UPLOAD_ID)
			.toOptionalString();
		if (token == null || uploadFieldId == null)
		{
			return null;
		}
		UploadGrant grant;
		try
		{
			grant = new UploadGrant(uploadFieldId,
				parameters.getParameterValue(AbstractFileUploadResource.MAX_SIZE).toLong(),
				parameters.getParameterValue(AbstractFileUploadResource.FILE_MAX_SIZE).toLong(-1),
				parameters.getParameterValue(AbstractFileUploadResource.FILE_COUNT_MAX).toLong(-1));
		}
		catch (StringValueConversionException e)
		{
			return null;
		}
		String decrypted = Application.get()
			.getSecuritySettings()
			.getCryptFactory()
			.newCrypt()
			.decryptUrlSafe(token);
		if (decrypted == null || !MessageDigest.isEqual(
			grant.serialized().getBytes(StandardCharsets.UTF_8),
			decrypted.getBytes(StandardCharsets.UTF_8)))
		{
			return null;
		}
		return grant;
	}
}
