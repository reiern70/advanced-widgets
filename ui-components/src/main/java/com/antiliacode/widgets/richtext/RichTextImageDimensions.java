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

import java.io.Serializable;

/**
 * The width and the height of an image, in pixels, as the browser shows it: a photo its camera
 * turned shows turned.
 *
 * @param width
 *            the width, 0 or more
 * @param height
 *            the height, 0 or more
 * @since 11.0.0
 */
public record RichTextImageDimensions(int width, int height) implements Serializable
{
	/**
	 * @param width
	 *            the width, 0 or more
	 * @param height
	 *            the height, 0 or more
	 */
	public RichTextImageDimensions
	{
		if (width < 0 || height < 0)
		{
			throw new IllegalArgumentException("An image is never " + width + " × " + height);
		}
	}

	/**
	 * @return the width and the height swapped, of an image turned a quarter
	 */
	public RichTextImageDimensions turned()
	{
		return new RichTextImageDimensions(height, width);
	}

	/**
	 * @param max
	 *            the widest and the highest an image may be, {@code null} for no limit
	 * @return whether the image is no wider and no higher than that
	 */
	public boolean fits(RichTextImageDimensions max)
	{
		return max == null || width <= max.width && height <= max.height;
	}

	/**
	 * @param max
	 *            the widest and the highest an image may be
	 * @return the dimensions of the image made smaller to fit, its proportions kept, each at least
	 *         one pixel; the same when it fits
	 */
	public RichTextImageDimensions scaledToFit(RichTextImageDimensions max)
	{
		if (fits(max))
		{
			return this;
		}
		double scale = Math.min((double)max.width / width, (double)max.height / height);
		return new RichTextImageDimensions(Math.max(1, (int)Math.floor(width * scale)),
			Math.max(1, (int)Math.floor(height * scale)));
	}

	/**
	 * @return the dimensions as the messages say them: 640 × 480
	 */
	@Override
	public String toString()
	{
		return width + " × " + height;
	}
}
