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
package com.antiliacode.widgets.demo.contacts;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;

/**
 * The card of a contact, shown in a dialog: initials, name and profession, then how to reach
 * them, where they live and when they were born.
 */
public class ContactCardPanel extends GenericPanel<Contact>
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param id
	 *            component id
	 * @param contact
	 *            the contact
	 */
	public ContactCardPanel(String id, IModel<Contact> contact)
	{
		super(id, contact);

		Contact person = contact.getObject();
		add(new Label("initials", person.getInitials()));
		add(new Label("name", person.getName()));
		add(new Label("profession", person.getProfessionName()));
		add(new WebMarkupContainer("email")
			.add(new Label("address", person.getEmail()))
			.add(AttributeModifier.replace("href", "mailto:" + person.getEmail())));
		add(new WebMarkupContainer("phone")
			.add(new Label("number", person.getPhone()))
			.add(AttributeModifier.replace("href", "tel:" + person.getPhone().replace(' ', '-'))));
		add(new Label("lives", person.getCity() + ", " + person.getCountryName()));
		add(new Label("born", person.getBorn()));
		add(new Label("age", person.getAge()));
	}
}
