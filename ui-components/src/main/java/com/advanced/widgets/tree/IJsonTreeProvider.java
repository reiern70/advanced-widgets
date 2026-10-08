/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.tree;

import java.io.Serializable;
import java.util.Locale;

import org.apache.wicket.Application;
import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.ConversionException;

/**
 * The provider of the nodes of a {@link JsonBasedTree}, which knows the key of every node, unique
 * in the tree: the key ties a node in the browser to one on the server, for example when the
 * browser asks for the children of a folder or a leaf is selected.
 * <p>
 * Being an {@link ITreeProvider}, it also provides the nodes of a {@link Tree} and a
 * {@link TreeGrid}; the {@link #model(Object) model} of a node keeps only its key once detached,
 * and is equal to every other model of the node.
 *
 * @param <T>
 *            the type of the nodes
 * @param <K>
 *            the type of the keys
 * @since 11.0.0
 */
public interface IJsonTreeProvider<T, K extends Serializable> extends ITreeProvider<T>
{
	/**
	 * @return the key of a node
	 */
	K keyOf(T node);

	/**
	 * @return the node of a key, {@code null} if there is none, for example after it was deleted
	 */
	T findByKey(K key);

	/**
	 * @return the type of the keys, which {@link #parseKey(String)} converts a key to
	 */
	Class<K> getKeyType();

	/**
	 * @return the key as the browser shows it
	 */
	default String formatKey(K key)
	{
		return String.valueOf(key);
	}

	/**
	 * Converts a key as the browser sends it, with the converter of the application for
	 * {@link #getKeyType()}.
	 *
	 * @param key
	 *            the key as the browser sends it, possibly not a key at all
	 * @return the key, {@code null} if the text is not one
	 */
	default K parseKey(String key)
	{
		if (key == null)
		{
			return null;
		}
		Class<K> type = getKeyType();
		if (type == String.class)
		{
			return type.cast(key);
		}
		try
		{
			return Application.get()
				.getConverterLocator()
				.getConverter(type)
				.convertToObject(key, Locale.ROOT);
		}
		catch (ConversionException e)
		{
			return null;
		}
	}

	@Override
	default IModel<T> model(T node)
	{
		return new TreeNodeModel<>(this, node);
	}

	@Override
	default void detach()
	{
	}
}
