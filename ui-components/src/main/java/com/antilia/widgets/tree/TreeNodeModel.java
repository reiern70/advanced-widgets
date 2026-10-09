/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.tree;

import java.io.Serializable;
import java.util.Objects;

import org.apache.wicket.model.LoadableDetachableModel;

/**
 * The model of a node of an {@link IJsonTreeProvider}, keeping only its key once detached, and
 * equal to every model of the same node, as the trees of wicket-extensions need to find a node
 * in the set of those expanded.
 *
 * @param <T>
 *            the type of the nodes
 * @param <K>
 *            the type of the keys
 */
class TreeNodeModel<T, K extends Serializable> extends LoadableDetachableModel<T>
{
	private static final long serialVersionUID = 1L;

	private final IJsonTreeProvider<T, K> provider;

	private final K key;

	TreeNodeModel(IJsonTreeProvider<T, K> provider, T node)
	{
		super(node);
		this.provider = provider;
		key = provider.keyOf(node);
	}

	@Override
	protected T load()
	{
		return provider.findByKey(key);
	}

	@Override
	public boolean equals(Object other)
	{
		return other instanceof TreeNodeModel<?, ?> model && Objects.equals(key, model.key);
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(key);
	}
}
