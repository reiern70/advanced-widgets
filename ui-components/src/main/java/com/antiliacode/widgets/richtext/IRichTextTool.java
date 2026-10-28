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
package com.antiliacode.widgets.richtext;

import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.lang.Args;

/**
 * A tool of the toolbar of a {@link RichTextEditor}, by its {@link #getName() name}: the
 * {@link RichTextTool built-in ones}, or one a {@link IRichTextFeature feature} registers with its
 * script. A tool needs its {@link #getFeature() feature}: without it, it does not show. The
 * toolbar names a tool by the translation of {@code RichTextEditor.} followed by its name, else as
 * its script does.
 *
 * @since 11.0.0
 */
public interface IRichTextTool extends IClusterable
{
	/**
	 * @return the name of the tool in the browser
	 */
	String getName();

	/**
	 * @return the feature the tool needs, {@code null} for none
	 */
	IRichTextFeature getFeature();

	/**
	 * @param name
	 *            the name of a tool a feature's script registers
	 * @param feature
	 *            the feature it needs, {@code null} for none
	 * @return the tool, the same as another of that name and feature
	 */
	static IRichTextTool of(String name, IRichTextFeature feature)
	{
		return new NamedRichTextTool(Args.notEmpty(name, "name"), feature);
	}
}
