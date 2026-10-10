/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.tree;

import java.util.Iterator;

import com.antilia.widgets.tree.IJsonTreeProvider;

/**
 * Provides the {@link TreeOfLife} to the trees, keyed by the ids of the taxa.
 */
public class TreeOfLifeProvider implements IJsonTreeProvider<Taxon, String>
{
	private static final long serialVersionUID = 1L;

	@Override
	public Iterator<? extends Taxon> getRoots()
	{
		return TreeOfLife.get().roots().iterator();
	}

	@Override
	public boolean hasChildren(Taxon taxon)
	{
		return !taxon.isLeaf();
	}

	@Override
	public Iterator<? extends Taxon> getChildren(Taxon taxon)
	{
		return TreeOfLife.get().children(taxon).iterator();
	}

	@Override
	public String keyOf(Taxon taxon)
	{
		return taxon.id();
	}

	@Override
	public Taxon findByKey(String key)
	{
		return TreeOfLife.get().byId(key);
	}

	@Override
	public Class<String> getKeyType()
	{
		return String.class;
	}
}
