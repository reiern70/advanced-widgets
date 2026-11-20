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
package com.antiliacode.widgets.autocomplete;

import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;

import com.antiliacode.widgets.ScriptJson;

/**
 * Sends the form of an {@link AbstractAutocomplete} when the user chose a suggestion, once the
 * choice is in the field: as Enter in a field of the form does, by a click on its
 * {@link org.apache.wicket.markup.html.form.Form#setDefaultButton(org.apache.wicket.markup.html.form.IFormSubmittingComponent)
 * default button}, Ajax or not, or by the first of its buttons sending it, unless that one is
 * disabled. With a {@link SearchAutocomplete}, Enter then sends the form with the suggestion it
 * chose as with the text typed.
 * <p>
 * It listens to {@link AbstractAutocomplete#CHOOSE_EVENT}, so it goes with the other behaviors of
 * a choice, such as {@link AjaxChooseBehavior}.
 *
 * @since 11.0.0
 */
public class SubmitOnChooseBehavior extends Behavior
{
	private static final long serialVersionUID = 1L;

	@Override
	public void bind(Component component)
	{
		super.bind(component);
		if (!(component instanceof AbstractAutocomplete<?, ?>))
		{
			throw new WicketRuntimeException(
				"SubmitOnChooseBehavior goes with an autocomplete, not with " + component);
		}
	}

	@Override
	public void renderHead(Component component, IHeaderResponse response)
	{
		super.renderHead(component, response);
		response.render(JavaScriptHeaderItem.forReference(AbstractAutocomplete.JS));
		response.render(OnDomReadyHeaderItem.forScript("Wicket.Autocomplete.submitOnChoose(" +
			ScriptJson.quote(component.getMarkupId()) + ");"));
	}
}
