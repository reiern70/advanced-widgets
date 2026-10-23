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

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.tree.AbstractTree.State;
import org.apache.wicket.extensions.markup.html.repeater.tree.Node;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.model.IModel;

import com.antiliacode.widgets.theme.LookPart;
import com.antiliacode.widgets.theme.Looks;
import com.antiliacode.widgets.theme.LookContainer;

/**
 * A node of a {@link Tree} or a {@link TreeGrid}: the row of the trees rendered in the browser,
 * with the arrow of a folder, the junction, as a button, and the {@link TreeNodeContent content}.
 * The junction is for the mouse: the content, a button too, is the one stop of the node for the
 * keyboard and screen readers, and tells whether a folder is expanded.
 *
 * @param <T>
 *            the type of the nodes
 */
class TreeNode<T> extends Node<T>
{
	private static final long serialVersionUID = 1L;

	private final INodeRenderer<T> renderer;

	TreeNode(String id, INodeRenderer<T> renderer, IModel<T> model)
	{
		super(id, renderer.asTree(), model);
		this.renderer = renderer;
		setOutputMarkupId(true);
		add(new LookContainer("row", LookPart.TREE_ROW));
	}

	@Override
	protected Component createContent(String id, IModel<T> model)
	{
		return new TreeNodeContent<>(id, renderer, model);
	}

	@Override
	protected MarkupContainer createJunctionComponent(String id)
	{
		return new AjaxLink<Void>(id)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				toggle();
			}

			@Override
			public boolean isEnabled()
			{
				return !renderer.isLeaf(TreeNode.this.getModelObject());
			}

			@Override
			protected void onComponentTag(ComponentTag tag)
			{
				super.onComponentTag(tag);
				tag.put("aria-hidden", "true");
				tag.put("tabindex", "-1");
				if (isEnabled())
				{
					tag.put("title", getString(isExpanded() ? "Tree.collapse" : "Tree.expand",
						null, isExpanded() ? "Collapse" : "Expand"));
				}
			}

			@Override
			public void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag)
			{
				replaceComponentTagBody(markupStream, openTag,
					isEnabled() ? renderer.getIcon(TreeIcon.TOGGLE).getMarkup() : "");
			}
		};
	}

	private boolean isExpanded()
	{
		return renderer.asTree().getState(getModelObject()) == State.EXPANDED;
	}

	@Override
	protected String getExpandedStyleClass(T node)
	{
		return junction() + " antilia-tree-toggle--expanded";
	}

	@Override
	protected String getCollapsedStyleClass()
	{
		return junction();
	}

	@Override
	protected String getOtherStyleClass()
	{
		return junction() + " antilia-tree-junction--none";
	}

	/**
	 * @return the classes of the junction in the look of the tree
	 */
	private String junction()
	{
		return Looks.of(this).getPartClasses(LookPart.TREE_JUNCTION);
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		super.onComponentTag(tag);
		T node = getModelObject();
		Looks.addClasses(tag, Looks.of(this), LookPart.TREE_NODE,
			renderer.isLeaf(node) ? LookPart.TREE_NODE_LEAF : null,
			renderer.isSelectable(node) && renderer.isSelected(node) ? LookPart.TREE_NODE_SELECTED
				: null);
	}
}
