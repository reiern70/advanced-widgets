/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.datatable;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;

/**
 * Shows the data of a user, and nothing while there is none.
 */
public class UserDetailsPanel extends GenericPanel<User>
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param id
	 *            component id
	 * @param model
	 *            the user to show
	 */
	public UserDetailsPanel(String id, IModel<User> model)
	{
		super(id, model);
		setOutputMarkupPlaceholderTag(true);

		add(new Label("id", model.map(User::getId)));
		add(new Label("firstName", model.map(User::getFirstName)));
		add(new Label("lastName", model.map(User::getLastName)));
		add(new Label("homePhone", model.map(User::getHomePhone)));
		add(new Label("cellPhone", model.map(User::getCellPhone)));
		add(new Label("bornDate", model.map(User::getBornDate)));
	}

	@Override
	protected void onConfigure()
	{
		super.onConfigure();
		setVisible(getModelObject() != null);
	}
}
