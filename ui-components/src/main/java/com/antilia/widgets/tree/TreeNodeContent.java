/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.tree;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.tree.AbstractTree.State;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.antilia.widgets.theme.LookBehavior;
import com.antilia.widgets.theme.LookPart;

/**
 * The content of a node of a {@link Tree} or a {@link TreeGrid}: a button holding its icon and
 * its text, which selects a leaf and expands or collapses a folder, or selects it when the tree
 * selects folders too.
 *
 * @param <T>
 *            the type of the nodes
 */
class TreeNodeContent<T> extends Panel
{
	private static final long serialVersionUID = 1L;

	private final INodeRenderer<T> renderer;

	TreeNodeContent(String id, INodeRenderer<T> renderer, IModel<T> model)
	{
		super(id, model);
		this.renderer = renderer;

		AjaxLink<Void> link = new AjaxLink<>("link")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				renderer.onNodeClick(target, getNode());
			}

			@Override
			protected void onComponentTag(ComponentTag tag)
			{
				super.onComponentTag(tag);
				T node = getNode();
				if (renderer.isSelectable(node) && renderer.isSelected(node))
				{
					tag.put("aria-current", "true");
				}
				if (!renderer.isLeaf(node))
				{
					tag.put("aria-expanded", String.valueOf(isExpanded()));
				}
			}
		};
		link.setOutputMarkupId(true);
		link.add(new LookBehavior(LookPart.TREE_NODE_BUTTON));
		add(link);
		link.add(new Label("icon", () -> {
			T node = getNode();
			return renderer.getIcon(node, renderer.isLeaf(node), isExpanded()).getMarkup();
		}).setEscapeModelStrings(false).add(new LookBehavior(LookPart.TREE_ICON)));
		link.add(new Label("label", renderer.newLabelModel(model))
			.add(new LookBehavior(LookPart.TREE_CONTENT)));
	}

	@SuppressWarnings("unchecked")
	private T getNode()
	{
		return (T)getDefaultModelObject();
	}

	private boolean isExpanded()
	{
		return renderer.asTree().getState(getNode()) == State.EXPANDED;
	}
}
