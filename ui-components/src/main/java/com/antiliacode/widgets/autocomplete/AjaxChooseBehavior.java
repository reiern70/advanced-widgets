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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;

/**
 * Tells the server via Ajax the value the user chose in an {@link AbstractAutocomplete}: the
 * autocomplete takes what it holds, its model is updated as by any
 * {@link AjaxFormComponentUpdatingBehavior}, and {@link #onChoose(AjaxRequestTarget, Object)} gets
 * the value, which the {@link IAutocompleteProvider provider} finds by its id. A
 * {@link SearchAutocomplete} has the label of the value as its text then; a page goes to the value
 * chosen, for example, rather than searching for the text.
 * <p>
 * It listens to {@link AbstractAutocomplete#CHOOSE_EVENT}, so it goes with the other behaviors of
 * a choice, such as {@link SubmitOnChooseBehavior}.
 *
 * @param <T>
 *            the type of the values of the autocomplete
 * @since 11.0.0
 */
public abstract class AjaxChooseBehavior<T> extends AjaxFormComponentUpdatingBehavior
{
	private static final long serialVersionUID = 1L;

	private static final String CHOSEN_PARAMETER = "chosen";

	/**
	 * A behavior telling the server the value chosen.
	 */
	public AjaxChooseBehavior()
	{
		super(AbstractAutocomplete.CHOOSE_EVENT);
	}

	@Override
	protected void onBind()
	{
		super.onBind();
		if (!(getComponent() instanceof AbstractAutocomplete<?, ?>))
		{
			throw new WicketRuntimeException(
				"AjaxChooseBehavior goes with an autocomplete, not with " + getComponent());
		}
	}

	@Override
	protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
	{
		super.updateAjaxAttributes(attributes);
		// the detail of the event the script fires: the id of the value chosen
		attributes.getDynamicExtraParameters().add("var detail = (attrs.event.originalEvent " +
			"|| attrs.event).detail || {}; return {" + CHOSEN_PARAMETER + ": detail.id};");
	}

	@Override
	protected void onUpdate(AjaxRequestTarget target)
	{
		String id = getComponent().getRequest().getRequestParameters()
			.getParameterValue(CHOSEN_PARAMETER).toOptionalString();
		T value = id != null ? getAutocomplete().getProvider().fromId(id) : null;
		if (value != null)
		{
			onChoose(target, value);
		}
	}

	/**
	 * @return the autocomplete of the behavior
	 */
	@SuppressWarnings("unchecked")
	protected AbstractAutocomplete<T, ?> getAutocomplete()
	{
		return (AbstractAutocomplete<T, ?>)getComponent();
	}

	/**
	 * Called when the user chose a value, once the model of the autocomplete is updated.
	 *
	 * @param target
	 *            the Ajax request target
	 * @param value
	 *            the value chosen
	 */
	protected abstract void onChoose(AjaxRequestTarget target, T value);
}
