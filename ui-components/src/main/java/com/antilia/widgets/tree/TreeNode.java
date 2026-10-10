/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.tree;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.tree.AbstractTree.State;
import org.apache.wicket.extensions.markup.html.repeater.tree.Node;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.model.IModel;

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
		return "antilia-tree-junction antilia-tree-toggle--expanded";
	}

	@Override
	protected String getCollapsedStyleClass()
	{
		return "antilia-tree-junction";
	}

	@Override
	protected String getOtherStyleClass()
	{
		return "antilia-tree-junction antilia-tree-junction--none";
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		super.onComponentTag(tag);
		T node = getModelObject();
		tag.append("class", "antilia-tree-node", " ");
		if (renderer.isLeaf(node))
		{
			tag.append("class", "antilia-tree-node--leaf", " ");
		}
		if (renderer.isSelectable(node) && renderer.isSelected(node))
		{
			tag.append("class", "antilia-tree-node--selected", " ");
		}
	}
}
