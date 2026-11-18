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
package com.antiliacode.widgets.demo.dropdown;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * The units of measure an article of an invoice is sold in, the long list of the dialog of the
 * {@link DropdownPage}: the few used most first, in a group of their own, then the others grouped
 * by what they measure. A unit is shown by its name, its symbol and what it is used for, a text of
 * the page's bundles under {@code unit.} and its code, mostly too long for one line of the list.
 */
final class UnitsOfMeasure
{
	/** The unit chosen until the visitor chooses another one. */
	static final String DEFAULT = "kg";

	/**
	 * The groups, in the order of the list: the key of their label in the page's bundles under
	 * {@code units.group.}, and the codes of their units.
	 */
	private static final List<Group> GROUPS = List.of(
		new Group("common", List.of("pc", "kg", "m", "l", "h")),
		new Group("count", List.of("pair", "dz", "box", "pallet")),
		new Group("mass", List.of("mg", "g", "t", "oz", "lb")),
		new Group("length", List.of("mm", "cm", "km", "in", "ft", "yd", "mi")),
		new Group("area", List.of("m2", "ha", "ft2", "ac")),
		new Group("volume", List.of("ml", "cl", "m3", "gal")),
		new Group("time", List.of("s", "min", "d", "wk", "mo")),
		new Group("energy", List.of("Wh", "kWh", "MJ")));

	private UnitsOfMeasure()
	{
	}

	/**
	 * @return the codes of the units, in the order of the list
	 */
	static List<String> codes()
	{
		return GROUPS.stream().flatMap(group -> group.units().stream()).toList();
	}

	/**
	 * @param component
	 *            the component whose bundles have the texts
	 * @param code
	 *            the code of a unit
	 * @return the unit in words: its name, its symbol and what it is used for
	 */
	static String name(Component component, String code)
	{
		return component.getString("unit." + code);
	}

	/**
	 * @param component
	 *            the component whose bundles have the texts
	 * @param code
	 *            the code of a unit
	 * @return the label of the group of the unit
	 */
	static String group(Component component, String code)
	{
		return GROUPS.stream()
			.filter(group -> group.units().contains(code))
			.findFirst()
			.map(group -> component.getString("units.group." + group.key()))
			.orElse(null);
	}

	/**
	 * @param component
	 *            the component whose bundles have the texts
	 * @return the renderer showing a unit in words, its code the value of its option
	 */
	static IChoiceRenderer<String> renderer(Component component)
	{
		return new ChoiceRenderer<>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public Object getDisplayValue(String code)
			{
				return name(component, code);
			}

			@Override
			public String getIdValue(String code, int index)
			{
				return code;
			}
		};
	}

	/**
	 * @param component
	 *            the component whose bundles have the texts
	 * @return the units as the options of the drop-downs of React and Svelte: the code, the unit
	 *         in words and the label of its group
	 */
	static JSONArray options(Component component)
	{
		JSONArray options = new JSONArray();
		codes().forEach(code -> options.put(new JSONObject()
			.put("value", code)
			.put("label", name(component, code))
			.put("group", group(component, code))));
		return options;
	}

	/**
	 * The units of a group: those used most, or those measuring one thing.
	 *
	 * @param key
	 *            the key of the label of the group, under {@code units.group.}
	 * @param units
	 *            the codes of the units
	 */
	private record Group(String key, List<String> units)
	{
	}
}
