/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.datatable;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;

/**
 * Shows the data of a contact, and nothing while there is none.
 */
public class ContactDetailsPanel extends GenericPanel<Contact>
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param id
	 *            component id
	 * @param model
	 *            the contact to show
	 */
	public ContactDetailsPanel(String id, IModel<Contact> model)
	{
		super(id, model);
		setOutputMarkupPlaceholderTag(true);

		add(new Label("id", model.map(Contact::getId)));
		add(new Label("firstName", model.map(Contact::getFirstName)));
		add(new Label("lastName", model.map(Contact::getLastName)));
		add(new Label("homePhone", model.map(Contact::getHomePhone)));
		add(new Label("cellPhone", model.map(Contact::getCellPhone)));
		add(new Label("bornDate", model.map(Contact::getBornDate)));
	}

	@Override
	protected void onConfigure()
	{
		super.onConfigure();
		setVisible(getModelObject() != null);
	}
}
