/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.menu;

import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;
import org.danekja.java.misc.serializable.SerializableRunnable;

/**
 * An item calling the server with a request of its own, by a {@link Link}: {@link #onClick()} does
 * what it does, then the page renders again, or the one it set as the response.
 *
 * @since 11.0.0
 */
public abstract class LinkMenuItem extends MenuItem
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param label
	 *            the label of the item
	 */
	public LinkMenuItem(IModel<String> label)
	{
		super(label);
	}

	/**
	 * Called when the user chose the item.
	 */
	protected abstract void onClick();

	@Override
	public AbstractLink getLink(String id)
	{
		return new Link<Void>(id)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick()
			{
				LinkMenuItem.this.onClick();
			}
		};
	}

	/**
	 * @param label
	 *            the label of the item
	 * @param onClick
	 *            what the item does
	 * @return an item calling the server with a request of its own
	 */
	public static LinkMenuItem of(IModel<String> label, SerializableRunnable onClick)
	{
		Args.notNull(onClick, "onClick");
		return new LinkMenuItem(label)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onClick()
			{
				onClick.run();
			}
		};
	}
}
