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
package com.antiliacode.widgets.palette;

/**
 * The matchers the filter of a {@link PaletteBehavior palette} has built in. They ignore case and
 * underline the parts of the label they matched.
 *
 * @since 11.0.0
 */
public enum PaletteMatcher implements IPaletteMatcher
{
	/** The label starts with the text. */
	START("start"),

	/** The label contains the text, anywhere. */
	ANYWHERE("anywhere"),

	/** The label ends with the text. */
	END("end"),

	/**
	 * Each word of the text starts a word of the label, in their order: "s ge" matches "South
	 * Georgia", underlining "S" and "Ge".
	 */
	WORDS("words");

	private final String name;

	PaletteMatcher(String name)
	{
		this.name = name;
	}

	@Override
	public String getName()
	{
		return name;
	}
}
