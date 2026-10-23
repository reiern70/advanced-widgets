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
package com.antiliacode.widgets.popover;

import java.util.Locale;

/**
 * Where a {@link Popover} opens: on a side of its trigger, centered on it ({@link #TOP_CENTER}), or
 * along it from one of its edges: {@link #TOP_LEFT} starts at the left edge of the trigger and
 * grows to the right, {@link #LEFT_TOP} starts at its top edge and grows down; with a trigger
 * smaller than the arrow, the popover starts a bit before the edge so that its arrow points at the
 * middle of the trigger. Where it does not fit in the window, the popover opens where it does: on
 * the opposite side, else on the side across with the most room; without room anywhere, it stays
 * in the window over its trigger.
 *
 * @since 11.0.0
 */
public enum PopoverPlacement
{
	/** Above the trigger, from its left edge. */
	TOP_LEFT,

	/** Above the trigger, centered on it. */
	TOP_CENTER,

	/** Above the trigger, up to its right edge. */
	TOP_RIGHT,

	/** Right of the trigger, from its top edge. */
	RIGHT_TOP,

	/** Right of the trigger, centered on it. */
	RIGHT_CENTER,

	/** Right of the trigger, up to its bottom edge. */
	RIGHT_BOTTOM,

	/** Below the trigger, up to its right edge. */
	BOTTOM_RIGHT,

	/** Below the trigger, centered on it. */
	BOTTOM_CENTER,

	/** Below the trigger, from its left edge. */
	BOTTOM_LEFT,

	/** Left of the trigger, up to its bottom edge. */
	LEFT_BOTTOM,

	/** Left of the trigger, centered on it. */
	LEFT_CENTER,

	/** Left of the trigger, from its top edge. */
	LEFT_TOP,

	/** On the side with the most room, centered. */
	AUTO;

	/**
	 * @return the name of the placement in the script, such as {@code top-left}
	 */
	public String cssName()
	{
		return name().toLowerCase(Locale.ROOT).replace('_', '-');
	}
}
