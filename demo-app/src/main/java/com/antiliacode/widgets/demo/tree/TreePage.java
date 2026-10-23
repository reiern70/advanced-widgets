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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

import com.antiliacode.widgets.dynamicdatatable.BaseDynamicDataTable.BodyHeightUnits;
import com.antiliacode.widgets.tree.JsonBasedTree;
import com.antiliacode.widgets.tree.Tree;

/**
 * The {@link TreeOfLife} in a {@link Tree} of Wicket components and in a {@link JsonBasedTree}
 * rendered in the browser from JSON, side by side, both selecting groups as well as species; the
 * cards of what is selected in either are shown next to them. The React and Svelte tabs show the
 * same tree in the Tree of {@code @antilia/react} and {@code @antilia/svelte}.
 */
public class TreePage extends TreeOfLifeDemoPage
{
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 */
	public TreePage()
	{
		TreeOfLifeProvider provider = new TreeOfLifeProvider();

		Tree<Taxon> tree = new Tree<>("tree", provider)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public IModel<?> newLabelModel(IModel<Taxon> taxon)
			{
				return () -> TreeOfLife.name(taxon.getObject(), getLocale());
			}

			@Override
			protected void onSelect(AjaxRequestTarget target, Taxon taxon)
			{
				show(taxon, target);
			}
		};
		tree.setSelectBranches(true);
		expanded().forEach(tree::expand);
		tree.setHeight(getTreeHeight(), BodyHeightUnits.PIXELS);
		withIcons(tree::setIcon);
		getThemed().add(tree);

		JsonBasedTree<Taxon, String> jsonTree = new JsonBasedTree<>("jsonTree", provider)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Object getNodeData(Taxon taxon)
			{
				return TreeOfLife.get().toJson(taxon, getLocale());
			}

			@Override
			protected void onSelect(AjaxRequestTarget target, Taxon taxon)
			{
				show(taxon, target);
			}
		};
		jsonTree.setNodeTemplate(TEMPLATE);
		jsonTree.setLabel(new ResourceModel("treeOfLife"));
		jsonTree.setSelectBranches(true);
		expanded().forEach(jsonTree::expand);
		jsonTree.setHeight(getTreeHeight(), BodyHeightUnits.PIXELS);
		withIcons(jsonTree::setIcon);
		getThemed().add(jsonTree);
	}

	@Override
	protected String getFrameworkDemo()
	{
		return "tree";
	}
}
