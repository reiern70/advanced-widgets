/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.tree;

import java.util.Map;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * The card of a group or a species of the {@link TreeOfLife}, in the language of the session: its
 * rank, name and scientific name, its description, the periods and the times it lived. Hidden
 * while there is no taxon.
 */
public class TaxonCard extends Panel
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param id
	 *            component id
	 * @param taxon
	 *            the taxon shown, {@code null} for none
	 * @param kind
	 *            the kind of card, {@code group} or {@code species}, its CSS modifier
	 */
	public TaxonCard(String id, IModel<Taxon> taxon, String kind)
	{
		super(id, taxon);

		WebMarkupContainer card = new WebMarkupContainer("card");
		card.add(AttributeModifier.append("class", "tree-demo-card--" + kind));
		add(card);
		for (String key : new String[] { "rank", "name", "description", "period", "lived" })
		{
			card.add(new Label(key, () -> json().get(key)));
		}
		card.add(new Label("scientific", () -> json().get("scientific"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(!"".equals(getDefaultModelObject()));
			}
		});
	}

	@Override
	protected void onConfigure()
	{
		super.onConfigure();
		setVisible(getDefaultModelObject() != null);
	}

	private Map<String, Object> json()
	{
		return TreeOfLife.card((Taxon)getDefaultModelObject(), getLocale());
	}
}
