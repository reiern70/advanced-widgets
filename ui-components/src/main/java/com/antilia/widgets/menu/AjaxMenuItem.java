/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.menu;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;
import org.danekja.java.util.function.serializable.SerializableConsumer;

/**
 * An item calling the server via Ajax, by an {@link AjaxLink}: {@link #onClick(AjaxRequestTarget)}
 * does what it does, the page staying as it is but for what the target renders anew.
 *
 * @since 11.0.0
 */
public abstract class AjaxMenuItem extends MenuItem
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param label
	 *            the label of the item
	 */
	public AjaxMenuItem(IModel<String> label)
	{
		super(label);
	}

	/**
	 * Called when the user chose the item.
	 *
	 * @param target
	 *            the Ajax request target
	 */
	protected abstract void onClick(AjaxRequestTarget target);

	@Override
	public AbstractLink getLink(String id)
	{
		return new AjaxLink<Void>(id)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				AjaxMenuItem.this.onClick(target);
			}
		};
	}

	/**
	 * @param label
	 *            the label of the item
	 * @param onClick
	 *            what the item does, given the Ajax request target
	 * @return an item calling the server via Ajax
	 */
	public static AjaxMenuItem of(IModel<String> label, SerializableConsumer<AjaxRequestTarget> onClick)
	{
		Args.notNull(onClick, "onClick");
		return new AjaxMenuItem(label)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onClick(AjaxRequestTarget target)
			{
				onClick.accept(target);
			}
		};
	}
}
