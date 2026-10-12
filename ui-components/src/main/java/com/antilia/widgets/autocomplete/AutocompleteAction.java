/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.autocomplete;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;

import com.github.openjson.JSONObject;

/**
 * A button at the end of every row of the suggestions of an {@link AbstractAutocomplete}, such as
 * an (i) telling more about the value. Its content is a template evaluated against the value's
 * JSON as the row is, by the same engine: an icon, for example
 * {@code <i class="fa-solid fa-circle-info"></i>}.
 * <p>
 * With a {@link #setTooltip(String) tooltip}, a template too, the button shows it while hovered or
 * clicked, and F1 toggles it for the active row. Without one, a click is handed to
 * {@link AbstractAutocomplete#onAction(org.apache.wicket.ajax.AjaxRequestTarget, String, Object)}
 * via Ajax, the row not being chosen.
 * <p>
 * The templates are the developer's and written into the page as they are; the values are escaped
 * by {@code {{ }}}.
 *
 * @since 11.0.0
 */
public class AutocompleteAction implements IDetachable
{
	private static final long serialVersionUID = 1L;

	private final String name;

	private final String template;

	private IModel<String> label;

	private String tooltip;

	/**
	 * @param name
	 *            the name the action is reported with
	 * @param template
	 *            the content of its button, evaluated against the value's JSON
	 */
	public AutocompleteAction(String name, String template)
	{
		this.name = Args.notEmpty(name, "name");
		this.template = Args.notEmpty(template, "template");
	}

	/**
	 * @return the name the action is reported with
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @return the template of the content of its button
	 */
	public String getTemplate()
	{
		return template;
	}

	/**
	 * @return what the button does, for screen readers and as its title, if set
	 */
	public IModel<String> getLabel()
	{
		return label;
	}

	/**
	 * @param label
	 *            what the button does, for screen readers and as its title; {@code {0}} stands for
	 *            the label of the value
	 * @return {@code this}
	 */
	public AutocompleteAction setLabel(IModel<String> label)
	{
		this.label = label;
		return this;
	}

	/**
	 * @return the template of the tooltip, if set
	 */
	public String getTooltip()
	{
		return tooltip;
	}

	/**
	 * @param tooltip
	 *            the template of the tooltip of the button, evaluated against the value's JSON;
	 *            the button shows it instead of being reported
	 * @return {@code this}
	 */
	public AutocompleteAction setTooltip(String tooltip)
	{
		this.tooltip = tooltip;
		return this;
	}

	/**
	 * @return whether a click is reported, rather than showing the tooltip
	 */
	public boolean isReported()
	{
		return tooltip == null || tooltip.isEmpty();
	}

	/**
	 * @return the action as the script reads it
	 */
	JSONObject toJson()
	{
		JSONObject json = new JSONObject().put("name", name).put("template", template);
		String text = label != null ? label.getObject() : null;
		if (text != null)
		{
			json.put("label", text);
		}
		if (!isReported())
		{
			json.put("tooltip", tooltip);
		}
		return json;
	}

	@Override
	public void detach()
	{
		if (label != null)
		{
			label.detach();
		}
	}
}
