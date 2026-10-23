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
package com.antiliacode.widgets.theme;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.util.io.IClusterable;

/**
 * How the components are drawn, apart from their colors: a {@link Theme} gives the colors, a look
 * the CSS classes of the {@link LookPart parts}. {@link Look#ANTILIA} draws the components with
 * their own style sheets; {@link Look#BOOTSTRAP} gives their parts the classes of Bootstrap too,
 * whose style sheet the page brings, so they look as Bootstrap does in the colors of the theme. The
 * behavior stays the components' own.
 * <p>
 * A look can be made of others, an {@link LookAspect aspect} from each, by
 * {@link #with(LookAspect, ILook)}: {@code Look.BOOTSTRAP.with(LookAspect.COLOR, Look.ANTILIA)}
 * gives the parts Bootstrap's classes but Antilia's colors.
 * <p>
 * A {@link ThemeBehavior} puts a look on a component and everything inside it, as the CSS classes
 * {@code antilia-look-<name>} and the attribute {@code data-antilia-look}, by which the scripts of
 * the components find it too; {@link Looks#of(org.apache.wicket.Component)} finds the look of a
 * component on the server, and {@link LookBehavior} gives an element the classes of its parts. An
 * application may have a look of its own, for another framework of CSS, by implementing this
 * interface and the same in the browser, with {@code registerLook}.
 *
 * @since 11.0.0
 */
public interface ILook extends IClusterable
{
	/**
	 * @return the name of the look, as {@code data-antilia-look} has it: a word for a simple look,
	 *         such as {@code bootstrap}
	 */
	String getName();

	/**
	 * @param part
	 *            a part of a component
	 * @return the CSS classes the look gives the part, separated by spaces; {@code null} or empty
	 *         for none
	 */
	String getCssClass(LookPart part);

	/**
	 * @param parts
	 *            the parts of a component, {@code null}s left out
	 * @return the CSS classes the look gives the parts, in their order, separated by spaces, each
	 *         once: the classes of a button, which has none of its own
	 */
	default String getCssClasses(LookPart... parts)
	{
		return Looks.distinct(Arrays.stream(parts)
			.filter(Objects::nonNull)
			.map(this::getCssClass)
			.toList());
	}

	/**
	 * @param parts
	 *            the parts an element is, {@code null}s left out
	 * @return the CSS classes of the element: its own, those of {@link Look#ANTILIA}, then those
	 *         the look adds, separated by spaces, each once
	 */
	default String getPartClasses(LookPart... parts)
	{
		return Looks.distinct(Arrays.stream(parts)
			.filter(Objects::nonNull)
			.flatMap(part -> Stream.of(Look.ANTILIA.getCssClass(part),
				getCssClass(part)))
			.toList());
	}

	/**
	 * @param aspect
	 *            an aspect of the parts
	 * @param look
	 *            the look giving the parts of the aspect their classes, a simple one
	 * @return a look made of this one, with the parts of the aspect in the other look
	 */
	default ILook with(LookAspect aspect, ILook look)
	{
		return new CompoundLook(this).with(aspect, look);
	}

	/**
	 * @return the simple looks this one is made of: itself for a simple look
	 */
	default List<ILook> getLooks()
	{
		return List.of(this);
	}

	/**
	 * Renders what the look needs in the page, such as the style sheet bridging it to the
	 * components; never the style sheet of a framework of CSS, which the application brings.
	 *
	 * @param response
	 *            the header response
	 */
	default void renderHead(IHeaderResponse response)
	{
	}

	/**
	 * Adds what the look needs to the tag of the element a {@link ThemeBehavior} puts it on, such
	 * as Bootstrap's {@code data-bs-theme} for a dark theme.
	 *
	 * @param tag
	 *            the tag of the element in the look
	 * @param theme
	 *            the theme of the element, {@code null} for none
	 */
	default void onComponentTag(ComponentTag tag, Theme theme)
	{
	}
}
