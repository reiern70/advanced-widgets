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
package com.antiliacode.widgets.desktop;

import java.util.Locale;

/**
 * The side of a {@link Desktop} its taskbar is on.
 *
 * @since 11.0.0
 */
public enum TaskbarPosition
{
	/** Below the windows, as by default. */
	BOTTOM,

	/** Above the windows. */
	TOP,

	/** On the left of the windows. */
	LEFT,

	/** On the right of the windows. */
	RIGHT;

	/**
	 * @return the name of the position in the script and in the CSS classes
	 */
	public String cssName()
	{
		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * @param cssName
	 *            the name of a position in the script and in the CSS classes
	 * @return the position of that name, {@code null} when there is none
	 */
	public static TaskbarPosition of(String cssName)
	{
		for (TaskbarPosition position : values())
		{
			if (position.cssName().equals(cssName))
			{
				return position;
			}
		}
		return null;
	}
}
