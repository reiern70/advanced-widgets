/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.tree;

import java.io.Serializable;
import java.util.List;

/**
 * A group or a species of the {@link TreeOfLife}.
 *
 * @param id
 *            the id, unique in the tree, the key of the trees and of the texts of the taxon in
 *            the bundle {@code TreeOfLife}
 * @param scientific
 *            the scientific name, {@code null} for an informal group
 * @param rank
 *            the rank, such as {@code kingdom} or {@code species}: the key of its name, under
 *            {@code rank.} in the bundle of the package
 * @param from
 *            since when it lived, in millions of years ago, {@code null} when not told
 * @param to
 *            until when it lived, in millions of years ago, 0 when it still lives
 * @param parent
 *            the id of the group it belongs to, {@code null} for a kingdom
 * @param children
 *            the ids of the groups and the species it holds, in the order of the trees
 */
public record Taxon(String id, String scientific, String rank, Double from, double to,
	String parent, List<String> children) implements Serializable
{
	/**
	 * @return whether it is a leaf of the trees: a species, or a genus shown without species
	 */
	public boolean isLeaf()
	{
		return children.isEmpty();
	}

	/**
	 * @return whether it still lives
	 */
	public boolean isLiving()
	{
		return to == 0;
	}

	@Override
	public String toString()
	{
		return id;
	}
}
