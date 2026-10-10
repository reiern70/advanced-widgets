/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.tree;

import java.util.EnumMap;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.tree.AbstractTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;

import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable.BodyHeightUnits;
import com.antilia.widgets.icon.IIcon;
import com.antilia.widgets.scrollbar.ScrollbarBehavior;
import com.github.openjson.JSONObject;

/**
 * What a {@link Tree} and a {@link TreeGrid} share: the node selected, kept as the model the
 * provider gives it, whether folders are selected too, the icons of the application's own and the height within which the tree
 * scrolls.
 *
 * @param <T>
 *            the type of the nodes
 */
final class TreeSupport<T> implements IDetachable
{
	private static final long serialVersionUID = 1L;

	/** The CSS class of a tree of a height, which scrolls itself. */
	static final String SCROLLING_CSS_CLASS = "antilia-tree--scrolling";

	private final Map<TreeIcon, IIcon> icons = new EnumMap<>(TreeIcon.class);

	private IModel<T> selected;

	private boolean selectBranches;

	private int height = -1;

	private BodyHeightUnits heightUnits = BodyHeightUnits.PIXELS;

	private ScrollbarBehavior scrollbar;

	int getHeight()
	{
		return height;
	}

	BodyHeightUnits getHeightUnits()
	{
		return heightUnits;
	}

	/**
	 * Gives a tree a fixed height, within which it scrolls with the scroll bars of a
	 * {@link ScrollbarBehavior}, or takes it away.
	 *
	 * @param tree
	 *            the tree
	 * @param height
	 *            the height in the given units, or a negative number for none
	 * @param units
	 *            the units of the height
	 * @param header
	 *            the CSS selector of the head staying in place above what scrolls, {@code null}
	 *            for none
	 */
	void setHeight(Component tree, int height, BodyHeightUnits units, String header)
	{
		Args.isTrue(height != 0, "the height has to be positive, or negative for none");
		heightUnits = Args.notNull(units, "units");
		this.height = height;
		if (height > 0 && scrollbar == null)
		{
			tree.add(scrollbar = new ScrollbarBehavior().setHeader(header));
		}
		else if (height < 0 && scrollbar != null)
		{
			tree.remove(scrollbar);
			scrollbar = null;
		}
	}

	/**
	 * Gives the tag of a tree of a height the class that lets it scroll.
	 */
	void onComponentTag(ComponentTag tag)
	{
		if (height > 0)
		{
			tag.append("class", SCROLLING_CSS_CLASS, " ");
		}
	}

	/**
	 * Gives a tree of a height that height in the browser, by a script, as the Content Security
	 * Policy of the page may not let a style attribute in.
	 */
	void renderHead(Component tree, IHeaderResponse response)
	{
		if (height > 0)
		{
			response.render(OnDomReadyHeaderItem.forScript("(function (tree) { if (tree) { " +
				"tree.style.height = " + JSONObject.quote(height + heightUnits.getCssUnit()) +
				"; } })(document.getElementById(" + JSONObject.quote(tree.getMarkupId()) + "));"));
		}
	}

	/**
	 * @return the icon of a slot, the application's own or the tree's
	 */
	IIcon getIcon(TreeIcon slot)
	{
		return icons.getOrDefault(slot, slot);
	}

	void setIcon(TreeIcon slot, IIcon icon)
	{
		if (icon == null)
		{
			icons.remove(slot);
		}
		else
		{
			icons.put(slot, icon);
		}
	}

	/**
	 * @return the icon of a node by its kind: a leaf, an expanded or a collapsed folder
	 */
	IIcon getIcon(boolean leaf, boolean expanded)
	{
		return getIcon(leaf ? TreeIcon.LEAF : expanded ? TreeIcon.FOLDER_OPEN : TreeIcon.FOLDER);
	}

	boolean isSelectBranches()
	{
		return selectBranches;
	}

	void setSelectBranches(boolean selectBranches)
	{
		this.selectBranches = selectBranches;
	}

	T getSelected()
	{
		return selected != null ? selected.getObject() : null;
	}

	boolean isSelected(AbstractTree<T> tree, T node)
	{
		if (selected == null || node == null)
		{
			return false;
		}
		IModel<T> model = tree.getProvider().model(node);
		try
		{
			return model.equals(selected);
		}
		finally
		{
			model.detach();
		}
	}

	/**
	 * @param node
	 *            the node to select, {@code null} for none
	 * @return the node selected before, {@code null} if none was
	 */
	T select(ITreeProvider<T> provider, T node)
	{
		T previous = getSelected();
		selected = node != null ? provider.model(node) : null;
		return previous;
	}

	@Override
	public void detach()
	{
		if (selected != null)
		{
			selected.detach();
		}
	}
}
