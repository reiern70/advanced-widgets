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
package com.antiliacode.widgets.demo.wizard;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.CssResourceReference;

import com.antiliacode.widgets.demo.DemoPage;
import com.antiliacode.widgets.dropdown.Dropdown;
import com.antiliacode.widgets.wizard.Wizard;
import com.antiliacode.widgets.wizard.WizardBreadcrumbPosition;
import com.antiliacode.widgets.wizard.WizardButtonsPosition;

/**
 * {@link Wizard}: two wizards whose steps are known only as the user goes. A dichotomous key of
 * mosquitoes, the {@link MosquitoKeyPanel}, each answer leading to the next question or to a
 * mosquito; and an image card, the {@link ImageCardPanel}, an image uploaded by Ajax, then
 * described or given a text over it. The settings put the bar of the buttons above or below the
 * body and the trail of the steps visited around the title, in both.
 */
public class WizardPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	private WizardButtonsPosition buttonsPosition = WizardButtonsPosition.BOTTOM;

	private WizardBreadcrumbPosition breadcrumbPosition = WizardBreadcrumbPosition.ABOVE_TITLE;

	private final MosquitoKeyPanel key;

	private final ImageCardPanel card;

	/**
	 * Constructor.
	 */
	public WizardPage()
	{
		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);

		Form<Void> settings = new Form<>("settings");
		themed.add(settings);
		settings.add(newSetting("buttonsPosition", WizardButtonsPosition.values()));
		settings.add(newSetting("breadcrumbPosition", WizardBreadcrumbPosition.values()));

		key = new MosquitoKeyPanel("key");
		themed.add(key);
		card = new ImageCardPanel("card");
		themed.add(card);
		apply(null);
	}

	/**
	 * @return a drop-down choosing a field of the page, applied to both wizards via Ajax
	 */
	private <E extends Enum<E>> Dropdown<E> newSetting(String property, E[] values)
	{
		Dropdown<E> setting = new Dropdown<>(property, new PropertyModel<E>(this, property),
			Arrays.asList(values), new IChoiceRenderer<>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public Object getDisplayValue(E shown)
				{
					return getString(property + "." + shown.name());
				}

				@Override
				public String getIdValue(E shown, int index)
				{
					return shown.name();
				}
			});
		setting.setRequired(true);
		setting.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				apply(target);
			}
		});
		return setting;
	}

	/**
	 * Puts the settings on both wizards, and repaints them.
	 */
	private void apply(AjaxRequestTarget target)
	{
		for (Wizard<?> wizard : List.of(key.getWizard(), card.getWizard()))
		{
			wizard.setButtonsPosition(buttonsPosition).setBreadcrumbPosition(breadcrumbPosition);
			if (target != null)
			{
				target.add(wizard);
			}
		}
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new CssResourceReference(WizardPage.class,
			"WizardPage.css")));
	}
}
