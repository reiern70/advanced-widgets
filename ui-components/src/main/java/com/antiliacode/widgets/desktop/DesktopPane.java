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
package com.antiliacode.widgets.desktop;

import java.util.Objects;

import org.apache.wicket.util.io.IClusterable;

import com.github.openjson.JSONObject;

/**
 * A pane of a {@link Desktop}: windows shown together, as a workspace of the desktop of an
 * operating system. The desktop shows one pane at a time; the {@link #DEFAULT default pane} is
 * always there and the first.
 *
 * @since 11.0.0
 */
public final class DesktopPane implements IClusterable
{
	private static final long serialVersionUID = 1L;

	/** The id of the default pane, which cannot be removed. */
	public static final String DEFAULT = "0";

	private final String id;

	private String name;

	/**
	 * @param id
	 *            the id of the pane, which its windows have in their {@link WindowState#getPane()}
	 * @param name
	 *            the name of the pane, in the taskbar; {@code null} for the default pane to be named
	 *            as the texts of the desktop name it
	 */
	public DesktopPane(String id, String name)
	{
		this.id = Objects.requireNonNull(id);
		this.name = name;
	}

	/**
	 * @param json
	 *            the pane as the browser sends it: {@code id} and {@code name}
	 * @return the pane, {@code null} when it has no id
	 */
	public static DesktopPane fromJson(JSONObject json)
	{
		String id = json.optString("id", "");
		if (id.isEmpty())
		{
			return null;
		}
		String name = json.optString("name", "");
		return new DesktopPane(id, name.isEmpty() ? null : name);
	}

	/**
	 * @return the id of the pane
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * @return the name of the pane, {@code null} for the default pane named as the texts name it
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @param name
	 *            the name of the pane
	 * @return {@code this}
	 */
	public DesktopPane setName(String name)
	{
		this.name = name;
		return this;
	}

	/**
	 * @return whether this is the default pane
	 */
	public boolean isDefault()
	{
		return DEFAULT.equals(id);
	}

	/**
	 * @return the pane as the script takes it
	 */
	public JSONObject toJson()
	{
		return new JSONObject().put("id", id).put("name", name == null ? "" : name);
	}

	@Override
	public boolean equals(Object other)
	{
		return other instanceof DesktopPane pane && id.equals(pane.id) &&
			Objects.equals(name, pane.name);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, name);
	}

	@Override
	public String toString()
	{
		return toJson().toString();
	}
}
