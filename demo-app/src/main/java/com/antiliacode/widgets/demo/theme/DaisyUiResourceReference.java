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
package com.antiliacode.widgets.demo.theme;

import org.apache.wicket.markup.head.HeaderItem;

/**
 * Serves the style sheet of the daisyUI web jar, for the pages in the daisyUI look: its prebuilt
 * {@code daisyui.css}, every component of daisyUI and its light and dark themes, which needs no
 * build of Tailwind. The application mounts it at {@value #PATH}.
 */
public class DaisyUiResourceReference extends WebJarStyleSheetReference
{
	private static final long serialVersionUID = 1L;

	public static final String PATH = "daisyui";

	public static final DaisyUiResourceReference INSTANCE = new DaisyUiResourceReference();

	private DaisyUiResourceReference()
	{
		super(PATH, "daisyui", "", "daisyui.css");
	}

	/**
	 * @return the header item of daisyUI's style sheet, before every other one, so that the style
	 *         sheets of the components and of the page come after it
	 */
	public static HeaderItem styleSheet()
	{
		return INSTANCE.headerItem();
	}
}
