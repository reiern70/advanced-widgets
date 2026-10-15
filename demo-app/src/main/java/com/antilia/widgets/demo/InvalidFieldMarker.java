/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import org.apache.wicket.Component;
import org.apache.wicket.application.IComponentInitializationListener;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.FormComponent;

/**
 * Marks a field that failed its validation with {@code aria-invalid="true"}: assistive
 * technologies tell the user it is wrong, and the style sheet of the page draws it in the color of
 * errors. The drop-downs of ui-components pass the mark on to the toggle the user reaches.
 * {@link #install(Component)}, an {@link IComponentInitializationListener}, gives it to every
 * field of the application.
 */
public class InvalidFieldMarker extends Behavior
{
	private static final long serialVersionUID = 1L;

	/**
	 * Gives the marker to a component that is a field.
	 *
	 * @param component
	 *            a component being initialized
	 */
	public static void install(Component component)
	{
		if (component instanceof FormComponent)
		{
			component.add(new InvalidFieldMarker());
		}
	}

	@Override
	public void onComponentTag(Component component, ComponentTag tag)
	{
		super.onComponentTag(component, tag);
		if (((FormComponent<?>)component).isValid() == false)
		{
			tag.put("aria-invalid", "true");
		}
	}
}
