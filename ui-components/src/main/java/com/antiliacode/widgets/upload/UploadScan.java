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

import org.apache.wicket.util.lang.Args;

/**
 * What {@link IUploadScanner a scanner} found of a file uploaded: that it may be kept, that it may
 * not, and why, or a cleaned copy to keep instead.
 *
 * @since 11.0.0
 */
public final class UploadScan
{
	private static final UploadScan ACCEPTED = new UploadScan(null, null);

	private final String refusal;

	private final byte[] cleaned;

	private UploadScan(String refusal, byte[] cleaned)
	{
		this.refusal = refusal;
		this.cleaned = cleaned;
	}

	/**
	 * @return the file may be kept as it is
	 */
	public static UploadScan accept()
	{
		return ACCEPTED;
	}

	/**
	 * @param message
	 *            why not, in the language of the user, which the editor shows
	 * @return the file may not be kept
	 */
	public static UploadScan refuse(String message)
	{
		return new UploadScan(Args.notEmpty(message, "message"), null);
	}

	/**
	 * @param content
	 *            the bytes of the file cleaned, of the same format, which the store keeps instead;
	 *            checked again, its size and its format
	 * @return the file may be kept, cleaned
	 */
	public static UploadScan clean(byte[] content)
	{
		return new UploadScan(null, Args.notNull(content, "content").clone());
	}

	/**
	 * @return whether the file, or its cleaned copy, may be kept
	 */
	public boolean isAccepted()
	{
		return refusal == null;
	}

	/**
	 * @return why the file may not be kept, {@code null} when it may
	 */
	public String getRefusal()
	{
		return refusal;
	}

	/**
	 * @return the bytes of the cleaned copy, {@code null} for none: the file is kept as it is
	 */
	public byte[] getCleaned()
	{
		return cleaned == null ? null : cleaned.clone();
	}
}
