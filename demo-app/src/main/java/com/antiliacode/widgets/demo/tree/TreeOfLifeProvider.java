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
package com.antiliacode.widgets.demo.tree;

import java.util.Iterator;

import com.antiliacode.widgets.tree.IJsonTreeProvider;

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
