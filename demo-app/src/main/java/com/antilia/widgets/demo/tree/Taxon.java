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
