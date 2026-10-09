/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.tree;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A file or a folder of {@link ProjectFiles}.
 *
 * @param id
 *            the id, unique among the files, the key of the trees
 * @param name
 *            the name
 * @param path
 *            the path from the root of the project, folders separated by {@code /}
 * @param folder
 *            whether it is a folder
 * @param bytes
 *            the size, of a folder the sum of the sizes of its files
 * @param modified
 *            when it was changed last, a folder when its last file was
 * @param kind
 *            the kind of file, {@code folder} for a folder: the key of its name, under
 *            {@code kind.} in the bundle of the package
 * @param children
 *            the ids of the children of a folder, folders first, by name
 */
public record ProjectFile(int id, String name, String path, boolean folder, long bytes,
	LocalDateTime modified, String kind, List<Integer> children) implements Serializable
{
	/**
	 * @return whether it is a leaf of the trees: a file, or an empty folder
	 */
	public boolean isLeaf()
	{
		return children.isEmpty();
	}

	@Override
	public String toString()
	{
		return name;
	}
}
