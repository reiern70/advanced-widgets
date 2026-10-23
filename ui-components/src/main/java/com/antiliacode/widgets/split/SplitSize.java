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
package com.antiliacode.widgets.split;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A size in a {@link SplitPanel}, in percent of the space the divider leaves or in pixels:
 * {@code SplitSize.percent(30)}, {@code SplitSize.pixels(250)}, or {@code SplitSize.parse("250px")}.
 *
 * @since 11.0.0
 */
public final class SplitSize implements Serializable
{
	private static final long serialVersionUID = 1L;

	private static final Pattern PATTERN = Pattern.compile("\\s*(-?\\d*\\.?\\d+)\\s*(px|%)?\\s*");

	private final double value;

	private final boolean pixels;

	private SplitSize(double value, boolean pixels)
	{
		this.value = Math.max(value, 0);
		this.pixels = pixels;
	}

	/**
	 * @param percent
	 *            the size in percent, from 0 to 100
	 * @return the size
	 */
	public static SplitSize percent(double percent)
	{
		return new SplitSize(Math.min(percent, 100), false);
	}

	/**
	 * @param pixels
	 *            the size in pixels
	 * @return the size
	 */
	public static SplitSize pixels(double pixels)
	{
		return new SplitSize(pixels, true);
	}

	/**
	 * @param size
	 *            a size as the script writes it: {@code 30} or {@code 30%} in percent,
	 *            {@code 250px} in pixels
	 * @return the size, {@code null} when it is none
	 */
	public static SplitSize parse(String size)
	{
		if (size == null)
		{
			return null;
		}
		Matcher matcher = PATTERN.matcher(size);
		if (!matcher.matches())
		{
			return null;
		}
		double value = Double.parseDouble(matcher.group(1));
		return "px".equals(matcher.group(2)) ? pixels(value) : percent(value);
	}

	/**
	 * @return the size in its unit
	 */
	public double getValue()
	{
		return value;
	}

	/**
	 * @return whether the size is in pixels, else in percent
	 */
	public boolean isPixels()
	{
		return pixels;
	}

	/**
	 * @return whether both sizes are in the same unit
	 */
	boolean sameUnit(SplitSize other)
	{
		return other != null && other.pixels == pixels;
	}

	/**
	 * @return the size as the script takes it: a number in percent, a string in pixels
	 */
	Object toJson()
	{
		return pixels ? toString() : value;
	}

	/**
	 * @return the size in CSS, {@code 30%} or {@code 250px}
	 */
	@Override
	public String toString()
	{
		String number = value == Math.rint(value) ? String.valueOf((long)value) :
			String.valueOf(value);
		return number + (pixels ? "px" : "%");
	}

	@Override
	public boolean equals(Object other)
	{
		return other instanceof SplitSize size && size.value == value && size.pixels == pixels;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(value, pixels);
	}
}
