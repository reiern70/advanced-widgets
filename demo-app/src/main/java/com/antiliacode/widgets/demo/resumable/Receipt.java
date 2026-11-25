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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import com.github.openjson.JSONObject;

/**
 * What the server tells of a file it received whole, before deleting it: its name, its size and
 * its SHA-256, which the user compares with that of the file sent ({@code sha256sum} on Linux,
 * {@code shasum -a 256} on macOS, {@code Get-FileHash} on Windows).
 *
 * @param name
 *            the name of the file
 * @param size
 *            its bytes
 * @param sha256
 *            its SHA-256, in hexadecimal
 */
public record Receipt(String name, long size, String sha256) implements Serializable
{
	/**
	 * @param name
	 *            the name of the file, as the browser told it
	 * @param file
	 *            the file the server stored whole
	 * @return what the server tells of it
	 * @throws IOException
	 *             when the file cannot be read
	 */
	public static Receipt of(String name, File file) throws IOException
	{
		try (InputStream in = new DigestInputStream(Files.newInputStream(file.toPath()),
			MessageDigest.getInstance("SHA-256")))
		{
			long size = 0;
			byte[] buffer = new byte[64 * 1024];
			for (int read = in.read(buffer); read >= 0; read = in.read(buffer))
			{
				size += read;
			}
			MessageDigest digest = ((DigestInputStream)in).getMessageDigest();
			return new Receipt(name, size, HexFormat.of().formatHex(digest.digest()));
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new IllegalStateException("Every JVM has SHA-256", e);
		}
	}

	/**
	 * @return the receipt as JSON
	 */
	public JSONObject toJson()
	{
		return new JSONObject().put("name", name).put("size", size).put("sha256", sha256);
	}
}
