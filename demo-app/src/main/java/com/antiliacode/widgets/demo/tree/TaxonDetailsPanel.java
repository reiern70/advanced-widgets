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
package com.antiliacode.widgets.demo.tree;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * The group or the species selected in a tree of the tree examples, in the language of the
 * session: the groups it belongs to, the kingdom first, then the card of the group, the one
 * selected or that of the species selected, and below it the card of the species. While nothing
 * is selected, an invitation to select something. The trees repaint it through Ajax when the user
 * selects a group or a species.
 */
public class TaxonDetailsPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param id
	 *            component id
	 * @param taxon
	 *            the taxon selected, {@code null} for none
	 */
	public TaxonDetailsPanel(String id, IModel<Taxon> taxon)
	{
		super(id, taxon);
		setOutputMarkupId(true);

		WebMarkupContainer selection = new WebMarkupContainer("selection")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(getTaxon() != null);
			}
		};
		add(selection);
		selection.add(new ListView<>("lineage", this::lineage)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<Taxon> item)
			{
				Taxon each = item.getModelObject();
				item.add(new Label("name", TreeOfLife.name(each, getLocale())));
				if (each.equals(getTaxon()))
				{
					item.add(AttributeModifier.replace("aria-current", "true"));
				}
			}
		});
		selection.add(new TaxonCard("group", () -> {
			Taxon selected = getTaxon();
			return selected == null || !selected.isLeaf() ? selected
				: TreeOfLife.get().parent(selected);
		}, "group"));
		selection.add(new TaxonCard("species", () -> {
			Taxon selected = getTaxon();
			return selected != null && selected.isLeaf() ? selected : null;
		}, "species"));
		add(new WebMarkupContainer("none")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(getTaxon() == null);
			}
		});
	}

	private Taxon getTaxon()
	{
		return (Taxon)getDefaultModelObject();
	}

	private List<Taxon> lineage()
	{
		Taxon taxon = getTaxon();
		return taxon != null ? TreeOfLife.get().lineage(taxon) : List.of();
	}
}
