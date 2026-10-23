/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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
package com.antiliacode.widgets.demo.account;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

import com.github.openjson.JSONObject;

/**
 * Gives a password field a button with an eye that shows what is typed in it, and hides it again.
 * The script {@code show-password.js} adds the button in the browser, so without scripts the field
 * stays a plain password field. The button's label comes from the resources
 * {@code showPassword} and {@code hidePassword} of the page, written into the attributes
 * {@code data-show-password} and {@code data-hide-password} of the field. Submitting the form
 * hides the password again, so that the browser still offers to save it.
 */
public class ShowPasswordBehavior extends Behavior
{
	private static final long serialVersionUID = 1L;

	/** The attribute holding the label of the button while the password is hidden. */
	public static final String SHOW_ATTRIBUTE = "data-show-password";

	/** The attribute holding the label of the button while the password is shown. */
	public static final String HIDE_ATTRIBUTE = "data-hide-password";

	private static final ResourceReference SCRIPT = new JavaScriptResourceReference(
		ShowPasswordBehavior.class, "show-password.js");

	@Override
	public void bind(Component component)
	{
		super.bind(component);
		component.setOutputMarkupId(true);
	}

	@Override
	public void onComponentTag(Component component, ComponentTag tag)
	{
		super.onComponentTag(component, tag);
		tag.put(SHOW_ATTRIBUTE, component.getString("showPassword"));
		tag.put(HIDE_ATTRIBUTE, component.getString("hidePassword"));
	}

	@Override
	public void renderHead(Component component, IHeaderResponse response)
	{
		super.renderHead(component, response);
		response.render(JavaScriptHeaderItem.forReference(SCRIPT));
		response.render(OnDomReadyHeaderItem.forScript(
			"AntiliaShowPassword.attach(" + JSONObject.quote(component.getMarkupId()) + ");"));
	}
}
