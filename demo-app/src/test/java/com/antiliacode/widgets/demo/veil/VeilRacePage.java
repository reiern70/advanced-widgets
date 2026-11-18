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
package com.antiliacode.widgets.demo.veil;

import java.util.Iterator;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.antiliacode.widgets.veil.PageVeilBehavior;

/**
 * The page of the browser tests of the veil: actions that wait at a gate the test holds
 * ({@link VeilRace}), so that a second click, or a second key, comes while the first request is
 * under way, whatever the speed of the machine.
 * <p>
 * The page parameter {@code veil=on} adds the library's {@link PageVeilBehavior}.
 * <ul>
 * <li>{@code approve}, a button in a row of a {@link RefreshingView}, with a markup id of its
 * own, as one made from the row's key: its answer renders the rows again, and the view makes the
 * row anew, so the button is another component, at another path, under the same markup id.</li>
 * <li>{@code hide} hides itself.</li>
 * <li>{@code save} stays on the page.</li>
 * <li>{@code field} sends its value as it changes.</li>
 * <li>{@code slow}, a link the veil leaves alone, as a filter or a timer.</li>
 * </ul>
 */
public class VeilRacePage extends WebPage
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param parameters
	 *            {@code veil=on} for a page veil
	 */
	public VeilRacePage(PageParameters parameters)
	{
		super(parameters);
		if ("on".equals(parameters.get("veil").toOptionalString()))
		{
			add(new PageVeilBehavior());
		}

		Form<Void> form = new Form<>("form");
		add(form);

		FeedbackPanel feedback = new FeedbackPanel("feedback");
		feedback.setOutputMarkupId(true);
		form.add(feedback);

		TextField<String> field = new TextField<>("field", Model.of(""));
		field.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				VeilRace.run("field");
			}
		});
		form.add(field);

		WebMarkupContainer table = new WebMarkupContainer("table");
		table.setOutputMarkupId(true);
		form.add(table);
		table.add(new RefreshingView<Integer>("rows")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Iterator<IModel<Integer>> getItemModels()
			{
				return List.<IModel<Integer>> of(Model.of(1)).iterator();
			}

			@Override
			protected void populateItem(Item<Integer> item)
			{
				AjaxButton approve = new AjaxButton("approve")
				{
					private static final long serialVersionUID = 1L;

					@Override
					protected void onSubmit(AjaxRequestTarget target)
					{
						VeilRace.run("approve");
						target.add(table);
					}
				};
				approve.setMarkupId("approve-" + item.getModelObject());
				item.add(approve);
			}
		});

		form.add(new AjaxButton("hide")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target)
			{
				VeilRace.run("hide");
				setVisible(false);
				target.add(this);
			}
		}.setOutputMarkupPlaceholderTag(true));

		form.add(new AjaxButton("save")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target)
			{
				VeilRace.run("save");
				target.add(feedback);
			}

			@Override
			protected void onError(AjaxRequestTarget target)
			{
				target.add(feedback);
			}
		});

		add(new AjaxLink<Void>("slow")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
			{
				super.updateAjaxAttributes(attributes);
				PageVeilBehavior.noVeil(attributes);
			}

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				VeilRace.run("slow");
			}
		});
	}
}
