/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
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
