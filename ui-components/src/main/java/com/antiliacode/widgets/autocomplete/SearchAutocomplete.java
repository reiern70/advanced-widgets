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

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.Strings;

/**
 * An autocomplete as a search box: its value is the text typed, a {@code String}, which the form
 * sends, and the suggestions only complete it. Up and Down put the label of the suggestion they
 * reach into the text field, Up above the first one and Escape the text typed back; choosing a
 * suggestion, by Enter or a click, puts its label there, the
 * {@link IAutocompleteProvider#getLabel(Object) provider's}. No suggestion is active until Up or
 * Down reaches one, and Enter without one sends the form, by its
 * {@link org.apache.wicket.markup.html.form.Form#setDefaultButton(org.apache.wicket.markup.html.form.IFormSubmittingComponent)
 * default button}, as in any text field. See {@link AbstractAutocomplete}.
 * <p>
 * Choosing a suggestion fills the field and fires {@code change}, which an
 * {@link org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior} hears, and then
 * {@link #CHOOSE_EVENT}. What else follows is plugged in by behaviors:
 * {@link SubmitOnChooseBehavior} sends the form, {@link AjaxChooseBehavior} tells the server the
 * value chosen, to go to it for example. Its suggestions take the template, the
 * {@link #setHighlightMatch(boolean) marks of the match} and the {@link #addAction(AutocompleteAction)
 * actions}, such as an (i) with a tooltip, of the other autocompletes.
 *
 * @param <T>
 *            the type of the values suggested
 * @since 11.0.0
 */
public class SearchAutocomplete<T> extends AbstractAutocomplete<T, String>
{
	private static final long serialVersionUID = 1L;

	/**
	 * A search box using the model of a parent with a
	 * {@link org.apache.wicket.model.CompoundPropertyModel}.
	 *
	 * @param id
	 *            component id, attached to an {@code <input type="hidden">}
	 * @param provider
	 *            supplies the suggestions
	 */
	public SearchAutocomplete(String id, IAutocompleteProvider<T> provider)
	{
		this(id, null, provider);
	}

	/**
	 * @param id
	 *            component id, attached to an {@code <input type="hidden">}
	 * @param model
	 *            the text
	 * @param provider
	 *            supplies the suggestions
	 */
	public SearchAutocomplete(String id, IModel<String> model, IAutocompleteProvider<T> provider)
	{
		super(id, model, provider);
		// the text is written into the value of the input, whose attributes are escaped anyway
		setEscapeModelStrings(false);
	}

	@Override
	protected boolean isMultiple()
	{
		return false;
	}

	@Override
	protected boolean isFreeText()
	{
		return true;
	}

	/**
	 * @return none: the value is the text, shown in the text field rather than as a chip
	 */
	@Override
	protected List<T> getChosen()
	{
		return new ArrayList<>();
	}

	/**
	 * @return the label of the value, as choosing it puts it into the text field
	 */
	@Override
	protected String toModelObject(List<T> values)
	{
		return values.isEmpty() ? null : getProvider().getLabel(values.get(0));
	}

	/**
	 * @return the text of the model, empty if there is none
	 */
	@Override
	protected String getModelValue()
	{
		String text = getModelObject();
		return text != null ? text : "";
	}

	/**
	 * Takes the text the browser sent, trimmed; none if it is empty.
	 */
	@Override
	public void convertInput()
	{
		String text = getInput();
		setConvertedInput(Strings.isEmpty(text) ? null : text);
	}
}
