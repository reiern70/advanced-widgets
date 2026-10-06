/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.theme;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.lang.Args;

import com.advanced.widgets.clipboard.ClipboardCopyBehavior;
import com.github.openjson.JSONObject;

/**
 * Generates a {@link Theme}: a picker per theme property, applied live to a preview component, and
 * the CSS class defining the edited theme, to copy into an application's style sheet.
 * <p>
 * The pickers start from the theme the preview is rendered in, for example by a
 * {@link ThemeBehavior}; call {@link #reload(IPartialPageRequestHandler)} after re-rendering the
 * preview in another theme. The edits are made in the browser through the CSS object model, so
 * the panel needs no inline style and works under a strict Content Security Policy.
 */
public class ThemeEditorPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	/**
	 * The properties a theme sets, in the order of wicket-theme.css.
	 */
	public static final List<String> PROPERTIES = List.of("primary", "primary-dark", "on-primary",
		"accent", "text", "surface", "surface-alt", "hover", "selected", "highlight", "toolbar",
		"border", "progress", "progress-dark", "track", "veil", "color-scheme");

	/** The script of the editor, {@code Wicket.ThemeEditor}. */
	public static final ResourceReference JS = new JavaScriptResourceReference(
		ThemeEditorPanel.class, "theme-editor.js");

	/** The style sheet of the editor. */
	public static final ResourceReference CSS = new CssResourceReference(ThemeEditorPanel.class,
		"theme-editor.css");

	private final Component preview;

	private final WebMarkupContainer editor;

	/**
	 * @param id
	 *            component id
	 * @param preview
	 *            the component the edited theme is applied to while editing
	 */
	public ThemeEditorPanel(String id, Component preview)
	{
		super(id);
		this.preview = Args.notNull(preview, "preview");
		preview.setOutputMarkupId(true);

		editor = new WebMarkupContainer("editor");
		editor.setOutputMarkupId(true);
		add(editor);
		editor.add(new ListView<>("properties", PROPERTIES)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<String> item)
			{
				String property = item.getModelObject();
				item.add(new Label("name", property).add(
					AttributeModifier.replace("title", "--wicket-theme-" + property)));
				item.add(new Label("description", new ResourceModel("property." + property)));
				item.add(newInput("input", property));
			}
		});
	}

	/**
	 * Drops the edits and reads the values of the preview's theme into the pickers again, for
	 * example after the preview was re-rendered in another theme.
	 *
	 * @param target
	 *            the request handler the preview was re-rendered with
	 */
	public void reload(IPartialPageRequestHandler target)
	{
		target.appendJavaScript(
			"Wicket.ThemeEditor.load(" + JSONObject.quote(editor.getMarkupId()) + ");");
	}

	private Component newInput(String id, String property)
	{
		Fragment input;
		if ("veil".equals(property))
		{
			input = new Fragment(id, "text", this);
		}
		else if ("color-scheme".equals(property))
		{
			input = new Fragment(id, "scheme", this);
		}
		else
		{
			input = new Fragment(id, "color", this);
		}
		WebMarkupContainer field = new WebMarkupContainer("field");
		field.add(AttributeModifier.replace("data-theme-property", "--wicket-theme-" + property));
		field.add(AttributeModifier.replace("aria-label", "--wicket-theme-" + property));
		input.add(field);
		return input;
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		ClipboardCopyBehavior.renderHeadItems(response);
		response.render(CssHeaderItem.forReference(CSS));
		response.render(JavaScriptHeaderItem.forReference(JS));
		JSONObject config = new JSONObject();
		config.put("editor", editor.getMarkupId());
		config.put("preview", preview.getMarkupId());
		response.render(OnDomReadyHeaderItem.forScript("Wicket.ThemeEditor.init(" + config + ");"));
	}
}
