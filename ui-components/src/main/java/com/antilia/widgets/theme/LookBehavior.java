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

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.util.lang.Args;

/**
 * Gives the tag of a component the classes of the {@link LookPart parts} it is, in the look the
 * component is in ({@link Looks#of(Component)}): its own classes, then those the look adds. The
 * parts may depend on the state of the component, by {@link #getParts(Component)}.
 *
 * @since 11.0.0
 */
public class LookBehavior extends Behavior
{
	private static final long serialVersionUID = 1L;

	private final LookPart[] parts;

	/**
	 * @param parts
	 *            the parts the component is
	 */
	public LookBehavior(LookPart... parts)
	{
		this.parts = Args.notNull(parts, "parts").clone();
	}

	/**
	 * @param component
	 *            the component
	 * @return the parts the component is, {@code null}s left out; those given by default
	 */
	protected LookPart[] getParts(Component component)
	{
		return parts;
	}

	@Override
	public void onComponentTag(Component component, ComponentTag tag)
	{
		Looks.addClasses(tag, Looks.of(component), getParts(component));
	}
}
