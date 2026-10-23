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
package com.antiliacode.widgets.notification;

/**
 * Where {@link Notifications} stacks notifications: a corner of the browser window, or the middle
 * of its top or bottom edge.
 *
 * @since 11.0.0
 */
public enum NotificationPosition
{
	/** The top left corner. */
	TOP_LEFT,

	/** The middle of the top edge. */
	TOP_CENTER,

	/** The top right corner, the default. */
	TOP_RIGHT,

	/** The bottom left corner. */
	BOTTOM_LEFT,

	/** The middle of the bottom edge. */
	BOTTOM_CENTER,

	/** The bottom right corner. */
	BOTTOM_RIGHT;

	/**
	 * @return the name the script takes, such as {@code top-right}
	 */
	public String cssName()
	{
		return name().toLowerCase().replace('_', '-');
	}
}
