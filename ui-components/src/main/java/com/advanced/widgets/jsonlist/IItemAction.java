/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.jsonlist;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.util.lang.Args;
import org.danekja.java.util.function.serializable.SerializableBiConsumer;

/**
 * An action on an item of a {@link JsonBasedInfiniteList}, handled on the server when an element
 * of the item carrying it is clicked.
 * <p>
 * The item template has to contain an element with a {@code data-dt-action} attribute whose value
 * is {@link #getActionId()}, for example {@code <button type="button" data-dt-action="edit">Edit
 * </button>}. A click on it, or Enter on it, is sent to the list, which looks the item up by its
 * key and hands it to the action registered with
 * {@link JsonBasedInfiniteList#addAction(IItemAction)}. The element of the item itself can carry
 * an action too, which a click anywhere else in the item sends.
 *
 * @param <T>
 *            the type of the items
 * @since 11.0.0
 */
public interface IItemAction<T> extends IDetachable
{
	/**
	 * @return the value of the {@code data-dt-action} attribute this action handles, unique
	 *         within its list
	 */
	String getActionId();

	/**
	 * Called when the action is clicked on an item that still exists.
	 *
	 * @param item
	 *            the item the action was clicked on, never {@code null}
	 * @param target
	 *            the Ajax request target
	 */
	void onAction(T item, AjaxRequestTarget target);

	@Override
	default void detach()
	{
	}

	/**
	 * @param <T>
	 *            the type of the items
	 * @param actionId
	 *            the value of the {@code data-dt-action} attribute the action handles
	 * @param handler
	 *            called with the item and the Ajax request target when the action is clicked
	 * @return an action calling the handler
	 */
	static <T> IItemAction<T> of(String actionId, SerializableBiConsumer<T, AjaxRequestTarget> handler)
	{
		Args.notEmpty(actionId, "actionId");
		Args.notNull(handler, "handler");
		return new IItemAction<>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getActionId()
			{
				return actionId;
			}

			@Override
			public void onAction(T item, AjaxRequestTarget target)
			{
				handler.accept(item, target);
			}
		};
	}
}
