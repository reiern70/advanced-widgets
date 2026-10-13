/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.theme;

import org.apache.wicket.markup.html.TransparentWebMarkupContainer;

/**
 * An element of the markup of a component that is {@link LookPart parts} of a look and nothing
 * else: a wrapper or a decoration, such as the frame of a panel. It is transparent: the components
 * inside its markup stay children of its parent.
 *
 * @since 11.0.0
 */
public class LookContainer extends TransparentWebMarkupContainer
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param id
	 *            component id
	 * @param parts
	 *            the parts the element is
	 */
	public LookContainer(String id, LookPart... parts)
	{
		super(id);
		add(new LookBehavior(parts));
	}
}
