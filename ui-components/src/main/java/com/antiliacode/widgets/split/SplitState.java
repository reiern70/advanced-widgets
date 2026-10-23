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
package com.antiliacode.widgets.split;

import java.io.Serializable;
import java.util.Objects;

/**
 * What the user changes in a {@link SplitPanel}: where its divider is and which pane, if any, is
 * collapsed. The split panel keeps it in its model and gives the model a new one each time the
 * user changes it, so that a model may store it anywhere: in the page, by default, in the session
 * or with the user's preferences.
 *
 * @since 11.0.0
 */
public final class SplitState implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** The divider in the middle, no pane collapsed. */
	public static final SplitState DEFAULT = new SplitState(null, null);

	private final SplitSize position;

	private final SplitPane collapsed;

	/**
	 * @param position
	 *            the size of the first pane, in the middle when {@code null}
	 * @param collapsed
	 *            the collapsed pane, {@code null} for none
	 */
	public SplitState(SplitSize position, SplitPane collapsed)
	{
		this.position = position == null ? SplitSize.percent(50) : position;
		this.collapsed = collapsed;
	}

	/**
	 * @return the size of the first pane
	 */
	public SplitSize getPosition()
	{
		return position;
	}

	/**
	 * @return the collapsed pane, {@code null} for none
	 */
	public SplitPane getCollapsed()
	{
		return collapsed;
	}

	/**
	 * @return the state with another position
	 */
	public SplitState withPosition(SplitSize position)
	{
		return new SplitState(position, collapsed);
	}

	/**
	 * @return the state with another collapsed pane, {@code null} for none
	 */
	public SplitState withCollapsed(SplitPane collapsed)
	{
		return new SplitState(position, collapsed);
	}

	@Override
	public boolean equals(Object other)
	{
		return other instanceof SplitState state && state.position.equals(position) &&
			state.collapsed == collapsed;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(position, collapsed);
	}

	@Override
	public String toString()
	{
		return position + (collapsed == null ? "" : ", " + collapsed.cssName() + " collapsed");
	}
}
