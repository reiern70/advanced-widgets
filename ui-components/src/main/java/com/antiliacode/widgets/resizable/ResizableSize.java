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
package com.antiliacode.widgets.resizable;

import java.io.Serializable;
import java.util.Objects;

import com.github.openjson.JSONObject;

/**
 * The size the user gave a resizable element: its width and its height, in pixels, those of its
 * border box; {@code null} for a dimension left as the style sheets of the page make it. A
 * {@link ResizableBehavior} keeps it in its model, and gives the model a new one each time the user
 * resizes the element, so that a model may store it anywhere: in the page, by default, in the
 * session or with the user's preferences.
 *
 * @since 11.0.0
 */
public final class ResizableSize implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** Both dimensions as the page makes them. */
	public static final ResizableSize NONE = new ResizableSize(null, null);

	private final Integer width;

	private final Integer height;

	/**
	 * @param width
	 *            the width in pixels, {@code null} for the page's
	 * @param height
	 *            the height in pixels, {@code null} for the page's
	 */
	public ResizableSize(Integer width, Integer height)
	{
		this.width = width == null ? null : Math.max(width, 0);
		this.height = height == null ? null : Math.max(height, 0);
	}

	/**
	 * @param width
	 *            the width in pixels, {@code null} for the page's
	 * @param height
	 *            the height in pixels, {@code null} for the page's
	 * @return the size
	 */
	public static ResizableSize of(Integer width, Integer height)
	{
		return width == null && height == null ? NONE : new ResizableSize(width, height);
	}

	/**
	 * @param height
	 *            the height in pixels
	 * @return the size with that height, the width the page's
	 */
	public static ResizableSize height(int height)
	{
		return new ResizableSize(null, height);
	}

	/**
	 * @param width
	 *            the width in pixels
	 * @return the size with that width, the height the page's
	 */
	public static ResizableSize width(int width)
	{
		return new ResizableSize(width, null);
	}

	/**
	 * @return the width in pixels, {@code null} for the page's
	 */
	public Integer getWidth()
	{
		return width;
	}

	/**
	 * @return the height in pixels, {@code null} for the page's
	 */
	public Integer getHeight()
	{
		return height;
	}

	/**
	 * @return whether both dimensions are the page's
	 */
	public boolean isNone()
	{
		return width == null && height == null;
	}

	/**
	 * @param axis
	 *            the dimensions the user resizes in
	 * @return the size in those dimensions, the others the page's
	 */
	public ResizableSize along(ResizeAxis axis)
	{
		return of(axis.isHorizontal() ? width : null, axis.isVertical() ? height : null);
	}

	/**
	 * @param width
	 *            the width in pixels, sent by the browser; empty or not a number for none
	 * @param height
	 *            the height in pixels, sent by the browser; empty or not a number for none
	 * @return the size
	 */
	public static ResizableSize parse(String width, String height)
	{
		return of(parsePixels(width), parsePixels(height));
	}

	/**
	 * @return the pixels of a number the browser sent, rounded; {@code null} for none
	 */
	static Integer parsePixels(Object value)
	{
		if (value instanceof Number number)
		{
			double pixels = number.doubleValue();
			return Double.isFinite(pixels) ? (int)Math.round(pixels) : null;
		}
		if (value == null)
		{
			return null;
		}
		try
		{
			return parsePixels(Double.valueOf(value.toString().trim()));
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	/**
	 * Puts the size into the options of a script: {@code width} and {@code height}, those that
	 * are not the page's.
	 *
	 * @param options
	 *            the options
	 * @return the options
	 */
	public JSONObject putInto(JSONObject options)
	{
		if (width != null)
		{
			options.put("width", width.intValue());
		}
		if (height != null)
		{
			options.put("height", height.intValue());
		}
		return options;
	}

	@Override
	public boolean equals(Object other)
	{
		return other instanceof ResizableSize size && Objects.equals(size.width, width) &&
			Objects.equals(size.height, height);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(width, height);
	}

	@Override
	public String toString()
	{
		return (width == null ? "auto" : width + "px") + " × " +
			(height == null ? "auto" : height + "px");
	}
}
