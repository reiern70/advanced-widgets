/*
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
package com.antiliacode.widgets.tree;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The nodes of the tests of the trees: docs (src (a.ts, b.ts), readme.md), notes.txt.
 */
final class TestNodes
{
	/** A node: a file, or a folder with children. */
	public static final class FileNode implements Serializable
	{
		private static final long serialVersionUID = 1L;

		private final String id;

		private final String name;

		private final String parent;

		FileNode(String id, String name, String parent)
		{
			this.id = id;
			this.name = name;
			this.parent = parent;
		}

		public String getId()
		{
			return id;
		}

		public String getName()
		{
			return name;
		}

		String getParent()
		{
			return parent;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	static final List<FileNode> ALL = List.of(new FileNode("docs", "docs", null),
		new FileNode("src", "src", "docs"), new FileNode("a", "a.ts", "src"),
		new FileNode("b", "b.ts", "src"), new FileNode("readme", "readme.md", "docs"),
		new FileNode("notes", "notes.txt", null));

	static final Map<String, FileNode> BY_ID = ALL.stream()
		.collect(Collectors.toMap(FileNode::getId, Function.identity()));

	private TestNodes()
	{
	}

	static FileNode node(String id)
	{
		return BY_ID.get(id);
	}

	/** The provider of the nodes. */
	static class Provider implements IJsonTreeProvider<FileNode, String>
	{
		private static final long serialVersionUID = 1L;

		@Override
		public Iterator<? extends FileNode> getRoots()
		{
			return childrenOf(null);
		}

		@Override
		public boolean hasChildren(FileNode node)
		{
			return childrenOf(node.getId()).hasNext();
		}

		@Override
		public Iterator<? extends FileNode> getChildren(FileNode node)
		{
			return childrenOf(node.getId());
		}

		private static Iterator<FileNode> childrenOf(String parent)
		{
			return ALL.stream().filter(node -> parent == null ? node.getParent() == null
				: parent.equals(node.getParent())).iterator();
		}

		@Override
		public String keyOf(FileNode node)
		{
			return node.getId();
		}

		@Override
		public FileNode findByKey(String key)
		{
			return BY_ID.get(key);
		}

		@Override
		public Class<String> getKeyType()
		{
			return String.class;
		}
	}
}
