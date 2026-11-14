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

import java.io.IOException;
import java.util.Locale;

import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.util.io.IClusterable;

/**
 * Scans a file a user uploaded before a store keeps it: an antivirus such as ClamAV, or a
 * sanitizer of the application's own. The file passed the checks of the upload already, its size
 * and its format as its bytes say; the scanner accepts it, refuses it with a message in the
 * language of the user, or answers a cleaned copy, which the store keeps instead, checked again.
 * The library ships the interface; the application brings the scanner.
 * <p>
 * It runs on every upload of the rich text editors that has one: the images and the files attached,
 * whether they go to a behavior of the editor or to the resource of the uploads.
 *
 * @see UploadScan
 * @since 10.11.0
 */
@FunctionalInterface
public interface IUploadScanner extends IClusterable
{
	/**
	 * @param file
	 *            the file uploaded; its name and its content type are the browser's, not to be
	 *            trusted
	 * @param format
	 *            its format, as its bytes say
	 * @param locale
	 *            the language of the user, for the message of a refusal
	 * @return what to do with the file: {@link UploadScan#accept()}, {@link UploadScan#refuse(
	 *         String)} or {@link UploadScan#clean(byte[])}
	 * @throws IOException
	 *             when the file could not be scanned: it is refused
	 */
	UploadScan scan(FileUpload file, IUploadFileFormat format, Locale locale) throws IOException;
}
