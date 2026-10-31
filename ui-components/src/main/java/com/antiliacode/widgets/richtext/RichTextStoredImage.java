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
package com.antiliacode.widgets.richtext;

import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.lang.Args;

import com.github.openjson.JSONObject;

/**
 * An image a {@link IRichTextImageStore store} keeps permanently, as it finds it for the tool
 * {@link RichTextTool#STORED_IMAGE insert image from store}: the address it is served at, and the
 * name the user finds it by, the name of the file uploaded for example.
 *
 * @param url
 *            the address the image is served at, as {@link IRichTextImageStore#store(
 *            org.apache.wicket.markup.html.form.upload.FileUpload, IRichTextImageType)} answered it
 * @param name
 *            the name the list of the tool shows; the address when {@code null} or empty
 * @see IRichTextImageStore#findPermanent(String, int)
 * @since 11.0.0
 */
public record RichTextStoredImage(String url, String name) implements IClusterable
{
	/**
	 * @throws IllegalArgumentException
	 *             for an address missing
	 */
	public RichTextStoredImage
	{
		url = Args.notEmpty(url, "url").trim();
		name = name == null || name.isBlank() ? url : name.trim();
	}

	/**
	 * @return the image as the editor in the browser takes it: {@code {"url": ..., "name": ...}}
	 */
	public JSONObject toJson()
	{
		JSONObject json = new JSONObject();
		json.put("url", url);
		json.put("name", name);
		return json;
	}
}
