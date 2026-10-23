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
package com.antiliacode.widgets.demo.contacts;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;

import com.antiliacode.widgets.autocomplete.FindAutocomplete;
import com.antiliacode.widgets.autocomplete.IAutocompleteProvider;
import com.antiliacode.widgets.button.AjaxActionButton;
import com.antiliacode.widgets.collapsible.CollapsiblePanel;
import com.antiliacode.widgets.icon.SvgIcon;
import com.antiliacode.widgets.scrollbar.ScrollbarBehavior;

/**
 * The filters of a {@link Facet}, in a {@link CollapsiblePanel}: on top a find bar, a
 * {@link FindAutocomplete}, finding any value of the facet not chosen yet; below, as check boxes,
 * the values chosen, those found among them, then the most common others with how many contacts
 * have each, scrolling when they are higher than the panel allows. A value is deselected with its
 * check box. The header counts the values chosen, and its action, an eraser, deselects them all.
 */
public abstract class FacetPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	/** How many values the check boxes offer, besides those chosen. */
	static final int LISTED = 6;

	private final Facet facet;

	private final ContactFilter filter;

	private final CollapsiblePanel collapsible;

	/** The values of the facet among the contacts the other filters leave, the most common first. */
	private final IModel<List<FacetValue>> values;

	private WebMarkupContainer list;

	/**
	 * @param id
	 *            component id
	 * @param facet
	 *            the facet whose values the panel chooses
	 * @param filter
	 *            the filters, which the panel changes
	 */
	public FacetPanel(String id, Facet facet, ContactFilter filter)
	{
		super(id);
		this.facet = facet;
		this.filter = filter;
		values = LoadableDetachableModel.of(() -> facet.values(filter, getLocale()));

		collapsible = new CollapsiblePanel("collapsible", () -> {
			int chosen = filter.get(facet).size();
			return chosen == 0 ? getName() : getName() + " (" + chosen + ")";
		})
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newBody(String id)
			{
				return newFilters(id);
			}
		};
		collapsible.setExpanded(true).setRememberExpanded(true);
		collapsible.setOutputMarkupId(true);
		add(collapsible);

		AjaxActionButton clear = collapsible.addAction(
			() -> new StringResourceModel("facet.clear", this).setParameters(getName())
				.getString(),
			SvgIcon.ERASER,
			target -> {
				filter.clear(facet);
				onFilterChanged(target, false);
			});
		clear.add(new Behavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onConfigure(Component component)
			{
				component.setVisible(filter.get(facet).isEmpty() == false);
			}
		});
	}

	/**
	 * @return the name of the facet, in the language of the session
	 */
	private String getName()
	{
		return getString("facet." + facet.key());
	}

	/**
	 * @return the facet whose values the panel chooses
	 */
	public Facet getFacet()
	{
		return facet;
	}

	/**
	 * Renders the panel again after the filters changed.
	 *
	 * @param target
	 *            the Ajax request target
	 * @param keepSearch
	 *            whether the autocomplete is left as it is, because the user just chose in it
	 */
	public void update(AjaxRequestTarget target, boolean keepSearch)
	{
		// counted before the change, while the request was read
		values.detach();
		if (keepSearch)
		{
			target.add(list);
			collapsible.updateHeader(target);
		}
		else
		{
			target.add(collapsible);
		}
	}

	/**
	 * Called when the user chose a value of the facet, deselected one, or cleared them all.
	 *
	 * @param target
	 *            the Ajax request target
	 * @param fromSearch
	 *            whether the change was made in the autocomplete
	 */
	protected abstract void onFilterChanged(AjaxRequestTarget target, boolean fromSearch);

	private Component newFilters(String id)
	{
		Fragment filters = new Fragment(id, "filters", this);

		FindAutocomplete<FacetValue> search = new FindAutocomplete<>("search", new ValuesProvider())
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onPick(AjaxRequestTarget target, FacetValue value)
			{
				filter.choose(facet, value.id(), true);
				onFilterChanged(target, true);
			}
		};
		search.setTemplate("<span class=\"contacts-facet__suggestion\">{{label}}" +
			"<span class=\"contacts-facet__count\">{{count}}</span></span>");
		search.setMinLength(0);
		search.setPlaceholder(() -> getString("facet." + facet.key() + ".find"));
		search.setLabel(this::getName);
		filters.add(search);

		// at most as high as the style sheet says, with the slim scroll bars when they are needed
		list = new WebMarkupContainer("list");
		list.add(new ScrollbarBehavior(ScrollbarBehavior.Axis.Y));
		filters.add(list);
		list.add(new ListView<>("value", this::getListed)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<FacetValue> item)
			{
				FacetValue value = item.getModelObject();
				item.add(new AjaxCheckBox("chosen", LambdaModel.of(
					() -> filter.isChosen(facet, value.id()),
					chosen -> filter.choose(facet, value.id(), chosen)))
				{
					private static final long serialVersionUID = 1L;

					@Override
					protected void onUpdate(AjaxRequestTarget target)
					{
						onFilterChanged(target, false);
					}
				});
				item.add(new Label("label", value.label()));
				item.add(new Label("count", value.count()));
			}
		}.setReuseItems(false));
		list.add(new Label("more", () -> new StringResourceModel("facet.more", this)
			.setParameters(values.getObject().size() - getListed().size()).getString())
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(values.getObject().size() > getListed().size());
			}
		});
		return filters;
	}

	/**
	 * @return the values the check boxes offer: those chosen, then the most common others, up to
	 *         {@value #LISTED} of them
	 */
	private List<FacetValue> getListed()
	{
		List<FacetValue> listed = new ArrayList<>();
		List<FacetValue> others = new ArrayList<>();
		for (FacetValue value : values.getObject())
		{
			(filter.isChosen(facet, value.id()) ? listed : others).add(value);
		}
		listed.addAll(others.subList(0, Math.min(LISTED, others.size())));
		return listed;
	}

	@Override
	protected void onDetach()
	{
		values.detach();
		super.onDetach();
	}

	/**
	 * The values of the facet whose label contains what the user typed, ignoring case and
	 * accents; those that start with it first. The values chosen are not suggested again.
	 */
	private class ValuesProvider implements IAutocompleteProvider<FacetValue>
	{
		private static final long serialVersionUID = 1L;

		@Override
		public List<FacetValue> search(String term, int max)
		{
			String typed = plain(term);
			List<FacetValue> starting = new ArrayList<>();
			List<FacetValue> containing = new ArrayList<>();
			for (FacetValue value : values.getObject())
			{
				String label = plain(value.label());
				if (filter.isChosen(facet, value.id()) == false && label.contains(typed))
				{
					(label.startsWith(typed) ? starting : containing).add(value);
				}
			}
			starting.addAll(containing);
			return starting.subList(0, Math.min(max, starting.size()));
		}

		@Override
		public String getId(FacetValue value)
		{
			return value.id();
		}

		@Override
		public FacetValue fromId(String id)
		{
			return values.getObject().stream().filter(value -> value.id().equals(id))
				.findFirst().orElse(null);
		}

		@Override
		public String getLabel(FacetValue value)
		{
			return value.label();
		}

		private String plain(String text)
		{
			return ContactFilter.plain(text);
		}
	}
}
