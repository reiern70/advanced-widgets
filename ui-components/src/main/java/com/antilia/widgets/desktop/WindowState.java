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
package com.antilia.widgets.desktop;

import java.util.Objects;

import org.apache.wicket.util.io.IClusterable;

import com.github.openjson.JSONObject;

/**
 * Where a {@link DesktopWindow} is, its size, and what the user made of it: minimized, maximized,
 * folded or closed, where it is stacked, and the {@link DesktopPane pane} it is in. Where it is and its size are in pixels, from the top
 * left corner of the desktop; {@code null} for those the browser works out, cascading the windows
 * and giving them the size they have by default.
 *
 * @since 11.0.0
 */
public final class WindowState implements IClusterable
{
	private static final long serialVersionUID = 1L;

	private final Integer x;

	private final Integer y;

	private final Integer width;

	private final Integer height;

	private final boolean minimized;

	private final boolean maximized;

	private final boolean folded;

	private final boolean closed;

	private final int order;

	private final String pane;

	/**
	 * A window where the browser puts it, as large as by default, neither minimized, maximized,
	 * folded nor closed.
	 */
	public WindowState()
	{
		this(null, null, null, null, false, false, false, false, 0, null);
	}

	/**
	 * @param x
	 *            where the window is, from the left of the desktop, {@code null} for the browser
	 *            to work it out
	 * @param y
	 *            where the window is, from the top of the desktop, {@code null} for the browser
	 *            to work it out
	 * @param width
	 *            the width of the window, {@code null} for the one by default
	 * @param height
	 *            the height of the window, {@code null} for the one by default
	 * @param minimized
	 *            whether the window is minimized to the taskbar
	 * @param maximized
	 *            whether the window fills the desktop
	 * @param folded
	 *            whether the window is folded to its title bar
	 * @param closed
	 *            whether the window is closed
	 * @param order
	 *            where the window is stacked, 1 for the one at the back; 0 for the order of the
	 *            windows in the page, the last one in front
	 */
	public WindowState(Integer x, Integer y, Integer width, Integer height, boolean minimized,
		boolean maximized, boolean folded, boolean closed, int order)
	{
		this(x, y, width, height, minimized, maximized, folded, closed, order, null);
	}

	/**
	 * @param x
	 *            where the window is, from the left of the desktop, {@code null} for the browser
	 *            to work it out
	 * @param y
	 *            where the window is, from the top of the desktop, {@code null} for the browser
	 *            to work it out
	 * @param width
	 *            the width of the window, {@code null} for the one by default
	 * @param height
	 *            the height of the window, {@code null} for the one by default
	 * @param minimized
	 *            whether the window is minimized to the taskbar
	 * @param maximized
	 *            whether the window fills the desktop
	 * @param folded
	 *            whether the window is folded to its title bar
	 * @param closed
	 *            whether the window is closed
	 * @param order
	 *            where the window is stacked, 1 for the one at the back; 0 for the order of the
	 *            windows in the page, the last one in front
	 * @param pane
	 *            the id of the pane the window is in, {@code null} for the pane shown when it is
	 *            rendered
	 */
	public WindowState(Integer x, Integer y, Integer width, Integer height, boolean minimized,
		boolean maximized, boolean folded, boolean closed, int order, String pane)
	{
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.minimized = minimized;
		this.maximized = maximized;
		this.folded = folded;
		this.closed = closed;
		this.order = Math.max(order, 0);
		this.pane = pane == null || pane.isEmpty() ? null : pane;
	}

	/**
	 * @param json
	 *            the state as the browser sends it: {@code x}, {@code y}, {@code width},
	 *            {@code height}, {@code minimized}, {@code maximized}, {@code folded},
	 *            {@code closed}, {@code order} and {@code pane}
	 * @return the state
	 */
	public static WindowState fromJson(JSONObject json)
	{
		return new WindowState(integer(json, "x"), integer(json, "y"), integer(json, "width"),
			integer(json, "height"), json.optBoolean("minimized"), json.optBoolean("maximized"),
			json.optBoolean("folded"), json.optBoolean("closed"), json.optInt("order"),
			json.optString("pane", null));
	}

	private static Integer integer(JSONObject json, String name)
	{
		double value = json.optDouble(name);
		return Double.isFinite(value) ? (int)Math.round(value) : null;
	}

	/**
	 * @return where the window is, from the left of the desktop, {@code null} when the browser
	 *         works it out
	 */
	public Integer getX()
	{
		return x;
	}

	/**
	 * @return where the window is, from the top of the desktop, {@code null} when the browser
	 *         works it out
	 */
	public Integer getY()
	{
		return y;
	}

	/**
	 * @return the width of the window, {@code null} for the one by default
	 */
	public Integer getWidth()
	{
		return width;
	}

	/**
	 * @return the height of the window, {@code null} for the one by default
	 */
	public Integer getHeight()
	{
		return height;
	}

	/**
	 * @return whether the window is minimized to the taskbar
	 */
	public boolean isMinimized()
	{
		return minimized;
	}

	/**
	 * @return whether the window fills the desktop
	 */
	public boolean isMaximized()
	{
		return maximized;
	}

	/**
	 * @return whether the window is folded to its title bar
	 */
	public boolean isFolded()
	{
		return folded;
	}

	/**
	 * @return whether the window is closed
	 */
	public boolean isClosed()
	{
		return closed;
	}

	/**
	 * @return where the window is stacked, 1 for the one at the back; 0 for the order of the page
	 */
	public int getOrder()
	{
		return order;
	}

	/**
	 * @return the id of the pane the window is in, {@code null} for the pane shown when it is
	 *         rendered
	 */
	public String getPane()
	{
		return pane;
	}

	/**
	 * @return this state with the window elsewhere
	 */
	public WindowState withPosition(Integer x, Integer y)
	{
		return new WindowState(x, y, width, height, minimized, maximized, folded, closed, order,
			pane);
	}

	/**
	 * @return this state with the window of another size
	 */
	public WindowState withSize(Integer width, Integer height)
	{
		return new WindowState(x, y, width, height, minimized, maximized, folded, closed, order,
			pane);
	}

	/**
	 * @return this state with the window minimized or not
	 */
	public WindowState withMinimized(boolean minimized)
	{
		return new WindowState(x, y, width, height, minimized, maximized, folded, closed, order,
			pane);
	}

	/**
	 * @return this state with the window maximized or not
	 */
	public WindowState withMaximized(boolean maximized)
	{
		return new WindowState(x, y, width, height, minimized, maximized, folded, closed, order,
			pane);
	}

	/**
	 * @return this state with the window folded or not
	 */
	public WindowState withFolded(boolean folded)
	{
		return new WindowState(x, y, width, height, minimized, maximized, folded, closed, order,
			pane);
	}

	/**
	 * @return this state with the window closed or not
	 */
	public WindowState withClosed(boolean closed)
	{
		return new WindowState(x, y, width, height, minimized, maximized, folded, closed, order,
			pane);
	}

	/**
	 * @return this state with the window stacked elsewhere
	 */
	public WindowState withOrder(int order)
	{
		return new WindowState(x, y, width, height, minimized, maximized, folded, closed, order,
			pane);
	}

	/**
	 * @return this state with the window in another pane
	 */
	public WindowState withPane(String pane)
	{
		return new WindowState(x, y, width, height, minimized, maximized, folded, closed, order,
			pane);
	}

	/**
	 * @return the state as the script takes it
	 */
	public JSONObject toJson()
	{
		JSONObject json = new JSONObject();
		json.putOpt("x", x).putOpt("y", y).putOpt("width", width).putOpt("height", height);
		return json.put("minimized", minimized)
			.put("maximized", maximized)
			.put("folded", folded)
			.put("closed", closed)
			.put("order", order)
			.putOpt("pane", pane);
	}

	@Override
	public boolean equals(Object other)
	{
		return other instanceof WindowState state && Objects.equals(x, state.x) &&
			Objects.equals(y, state.y) && Objects.equals(width, state.width) &&
			Objects.equals(height, state.height) && minimized == state.minimized &&
			maximized == state.maximized && folded == state.folded && closed == state.closed &&
			order == state.order && Objects.equals(pane, state.pane);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(x, y, width, height, minimized, maximized, folded, closed, order,
			pane);
	}

	@Override
	public String toString()
	{
		return toJson().toString();
	}
}
