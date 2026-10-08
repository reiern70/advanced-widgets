/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.tree;

import java.util.Iterator;

import com.advanced.widgets.tree.IJsonTreeProvider;

/**
 * Provides the {@link ProjectFiles} to the trees, keyed by their ids.
 */
public class ProjectFilesProvider implements IJsonTreeProvider<ProjectFile, Integer>
{
	private static final long serialVersionUID = 1L;

	@Override
	public Iterator<? extends ProjectFile> getRoots()
	{
		return ProjectFiles.get().roots().iterator();
	}

	@Override
	public boolean hasChildren(ProjectFile file)
	{
		return !file.isLeaf();
	}

	@Override
	public Iterator<? extends ProjectFile> getChildren(ProjectFile file)
	{
		return ProjectFiles.get().children(file).iterator();
	}

	@Override
	public Integer keyOf(ProjectFile file)
	{
		return file.id();
	}

	@Override
	public ProjectFile findByKey(Integer key)
	{
		return ProjectFiles.get().byId(key);
	}

	@Override
	public Class<Integer> getKeyType()
	{
		return Integer.class;
	}
}
