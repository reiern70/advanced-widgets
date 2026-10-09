/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.tree;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.io.IClusterable;

import com.antilia.widgets.demo.DemoPage;
import com.antilia.widgets.demo.datatable.ActionIcons;
import com.antilia.widgets.demo.datatable.FontAwesomeResourceReference;
import com.antilia.widgets.demo.frameworks.FrameworkTabs;
import com.antilia.widgets.dropdown.Dropdown;
import com.antilia.widgets.icon.FontAwesomeIcon;
import com.antilia.widgets.icon.IIcon;
import com.antilia.widgets.icon.SvgIcon;
import com.antilia.widgets.tree.TreeIcon;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * What the tree examples share: the {@link ProjectFiles} shown in a tree of Wicket components and
 * in one rendered from JSON, the folder {@code frontend} expanded, and the file selected in either
 * shown next to them, repainted through Ajax. A drop-down above the trees chooses their icons:
 * the trees' own, Font Awesome's as inline SVG, or the same drawn by the Font Awesome font. Their
 * React and Svelte apps load the files from the {@link FilesResource}, show the file selected
 * themselves and choose the icons with a drop-down of their own.
 */
public abstract class FilesDemoPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	/** The template of a node of the trees rendered from JSON: the name of the file. */
	static final String TEMPLATE = "{{name}}";

	/** The folder expanded at first. */
	static final String EXPANDED = "frontend";

	private static final ResourceReference CSS = new CssResourceReference(FilesDemoPage.class,
		"tree-demo.css");

	private final WebMarkupContainer themed;

	private final FileDetailsPanel details;

	/** The trees, given the icons chosen. */
	private final List<IconTarget> trees = new ArrayList<>();

	/** The id of the file selected, {@code null} while none is. */
	private Integer selected;

	/** The icons of the trees. */
	private ActionIcons icons = ActionIcons.BUILT_IN;

	protected FilesDemoPage()
	{
		themed = newThemedContainer("themed");
		add(themed);
		details = new FileDetailsPanel("details",
			() -> selected != null ? ProjectFiles.get().byId(selected) : null);
		themed.add(details);
		themed.add(iconsChoice("icons"));
	}

	/**
	 * A tree, as {@link #withIcons(IconTarget)} gives it the icons chosen: its
	 * {@code setIcon(TreeIcon, IIcon)}.
	 */
	@FunctionalInterface
	protected interface IconTarget extends IClusterable
	{
		/**
		 * Gives the tree an icon, {@code null} for its own.
		 */
		void setIcon(TreeIcon slot, IIcon icon);
	}

	/**
	 * Gives a tree the icons chosen above the trees, now and whenever they are chosen anew.
	 */
	protected void withIcons(IconTarget tree)
	{
		trees.add(tree);
		applyIcons(tree);
	}

	private void applyIcons(IconTarget tree)
	{
		for (TreeIcon slot : TreeIcon.values())
		{
			tree.setIcon(slot, icon(icons, slot));
		}
	}

	/**
	 * @return the icon of a slot of the trees in a set, {@code null} for the trees' own
	 */
	private static IIcon icon(ActionIcons set, TreeIcon slot)
	{
		String name = switch (slot)
		{
			case TOGGLE -> "CHEVRON_RIGHT";
			case FOLDER -> "FOLDER";
			case FOLDER_OPEN -> "FOLDER_OPEN";
			case LEAF -> "FILE_LINES";
		};
		return set.pick(null, SvgIcon.valueOf(name), FontAwesomeIcon.valueOf(name));
	}

	/**
	 * @return the drop-down choosing the icons of the trees; the page is rendered anew with them
	 */
	private Dropdown<ActionIcons> iconsChoice(String id)
	{
		Dropdown<ActionIcons> choice = new Dropdown<>(id, LambdaModel.of(() -> icons, set -> {
			icons = set;
			trees.forEach(this::applyIcons);
		}), List.of(ActionIcons.values()), new EnumChoiceRenderer<>(this));
		choice.setRequired(true);
		choice.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				setResponsePage(getPage());
			}
		});
		return choice;
	}

	/**
	 * @return the container of the trees and the details, in the session's theme
	 */
	protected WebMarkupContainer getThemed()
	{
		return themed;
	}

	/**
	 * Shows a file next to the trees, as when the user selects it in one of them.
	 */
	protected void show(ProjectFile file, AjaxRequestTarget target)
	{
		selected = file.id();
		target.add(details);
	}

	/**
	 * @return the folder expanded at first
	 */
	protected static ProjectFile expanded()
	{
		return ProjectFiles.get().byPath(EXPANDED);
	}

	/**
	 * @return the height of the trees, in pixels, within which they scroll
	 */
	protected int getTreeHeight()
	{
		return 480;
	}

	/**
	 * @return the configuration the React and Svelte apps share: the texts in the language of the
	 *         session, those of the trees and of the drop-downs too, the URL of the files, the
	 *         template of a node, the folder expanded at first, the height of the trees, and the
	 *         markup of the icons of each set, by the name of the set, with the set chosen
	 */
	@Override
	protected JSONObject getFrameworkConfig()
	{
		JSONObject texts = FrameworkTabs.texts(this, "files", "selectedFile", "noneSelected", "name",
			"path", "kind", "size", "modified");
		JSONObject tree = new JSONObject()
			.put("loading", getString("Tree.loading"))
			.put("empty", getString("Tree.empty"))
			.put("unlicensed", getString("License.placeholder"));
		texts.put("icons", getString("icons"));
		JSONObject iconSets = new JSONObject();
		for (ActionIcons set : ActionIcons.values())
		{
			texts.put("icons." + set.name(), getString(ActionIcons.class.getSimpleName() + "." +
				set.name()));
			JSONObject markup = new JSONObject();
			for (TreeIcon slot : TreeIcon.values())
			{
				IIcon icon = icon(set, slot);
				if (icon != null)
				{
					markup.put(key(slot), icon.getMarkup());
				}
			}
			iconSets.put(set.name(), markup);
		}
		FrameworkTabs.withDropdownTexts(this, texts);
		return new JSONObject()
			.put("texts", texts.put("tree", tree))
			.put("filesUrl", FrameworkTabs.urlOf(this, FilesResource.REFERENCE))
			.put("template", TEMPLATE)
			.put("expanded", new JSONArray().put(String.valueOf(expanded().id())))
			.put("height", new JSONObject().put("value", getTreeHeight()).put("unit", "px"))
			.put("iconSets", iconSets)
			.put("icons", icons.name());
	}

	/**
	 * @return the key of the icon of a slot among the icons of the trees in the browser
	 *         ({@code TreeIcons} of {@code tree.ts})
	 */
	private static String key(TreeIcon slot)
	{
		return switch (slot)
		{
			case TOGGLE -> "toggle";
			case FOLDER -> "folder";
			case FOLDER_OPEN -> "folderOpen";
			case LEAF -> "leaf";
		};
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(CSS));
		response.render(FontAwesomeResourceReference.styleSheet());
	}
}
