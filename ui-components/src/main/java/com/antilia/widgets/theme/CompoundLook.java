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
package com.antilia.widgets.theme;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.util.lang.Args;

/**
 * A look made of others, an {@link LookAspect aspect} from each: the parts of an aspect have the
 * classes of the look given for it, the others those of the base. Bootstrap's classes with the
 * colors and the sizes of Antilia's:
 *
 * <pre>
 * Look.BOOTSTRAP.with(LookAspect.COLOR, Look.ANTILIA).with(LookAspect.SIZE, Look.ANTILIA)
 * </pre>
 *
 * Its name is the one {@code data-antilia-look} has, by which the browser makes the same look:
 * the base's, then each aspect taken from another look, as in
 * {@code bootstrap color:antilia size:antilia}. The looks it is made of are simple ones, which the
 * browser knows by their names.
 *
 * @since 11.0.0
 */
public final class CompoundLook implements ILook
{
	private static final long serialVersionUID = 1L;

	private final ILook base;

	private final Map<LookAspect, ILook> aspects;

	/**
	 * @param base
	 *            the look of the aspects no other look is given for, a simple one
	 */
	public CompoundLook(ILook base)
	{
		this(simple(base, "base"), new EnumMap<>(LookAspect.class));
	}

	private CompoundLook(ILook base, Map<LookAspect, ILook> aspects)
	{
		this.base = base;
		this.aspects = aspects;
	}

	private static ILook simple(ILook look, String name)
	{
		Args.notNull(look, name);
		Args.isTrue(look.getLooks().size() == 1 && look.getLooks().get(0) == look,
			"The {} is a simple look, not one made of others: {}", name, look.getName());
		return look;
	}

	/**
	 * @param aspect
	 *            an aspect of the parts
	 * @param look
	 *            the look giving the parts of the aspect their classes, a simple one
	 * @return a look like this one, with the parts of the aspect in the other look
	 */
	@Override
	public CompoundLook with(LookAspect aspect, ILook look)
	{
		Args.notNull(aspect, "aspect");
		Map<LookAspect, ILook> taken = new EnumMap<>(LookAspect.class);
		taken.putAll(aspects);
		if (simple(look, "look").equals(base))
		{
			taken.remove(aspect);
		}
		else
		{
			taken.put(aspect, look);
		}
		return new CompoundLook(base, taken);
	}

	/**
	 * @param aspect
	 *            an aspect of the parts
	 * @return the look giving the parts of the aspect their classes
	 */
	public ILook getLook(LookAspect aspect)
	{
		return aspects.getOrDefault(aspect, base);
	}

	@Override
	public String getName()
	{
		StringBuilder name = new StringBuilder(base.getName());
		aspects.forEach(
			(aspect, look) -> name.append(' ').append(aspect.getName()).append(':').append(
				look.getName()));
		return name.toString();
	}

	@Override
	public String getCssClass(LookPart part)
	{
		return getLook(part.getAspect()).getCssClass(part);
	}

	@Override
	public List<ILook> getLooks()
	{
		List<ILook> looks = new ArrayList<>();
		looks.add(base);
		for (ILook look : aspects.values())
		{
			if (looks.contains(look) == false)
			{
				looks.add(look);
			}
		}
		return looks;
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		getLooks().forEach(look -> look.renderHead(response));
	}

	@Override
	public void onComponentTag(ComponentTag tag, Theme theme)
	{
		getLooks().forEach(look -> look.onComponentTag(tag, theme));
	}

	@Override
	public boolean equals(Object other)
	{
		return other instanceof CompoundLook compound && base.equals(compound.base) &&
			aspects.equals(compound.aspects);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(base, aspects);
	}

	@Override
	public String toString()
	{
		return getName();
	}
}
