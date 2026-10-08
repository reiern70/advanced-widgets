/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.modal;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.EmailTextField;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.validator.StringValidator;

import com.advanced.widgets.demo.DemoSession;
import com.advanced.widgets.dropdown.Dropdown;
import com.advanced.widgets.modal.IModalButtonFactory;
import com.advanced.widgets.modal.ModalSubmitButton;

/**
 * The form of a member of the team, the body of a modal dialog: the name, the email and the role,
 * validated with the messages the {@link TeamResource} answers the React and Svelte apps with.
 * The form edits a copy of the member, saved into the session's {@link TeamStore} by the Save
 * button of the footer of the dialog, {@link #saveButton()}.
 */
public abstract class MemberFormPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	private final Form<TeamMember> form;

	private final FeedbackPanel feedback;

	/**
	 * @param id
	 *            component id
	 * @param member
	 *            the member, new if its id is {@code 0}
	 */
	public MemberFormPanel(String id, TeamMember member)
	{
		super(id);
		form = new Form<>("form", new CompoundPropertyModel<>(member.copy()));
		form.setOutputMarkupId(true);
		add(form);
		feedback = new FeedbackPanel("feedback");
		form.add(feedback.setOutputMarkupId(true));

		form.add(new TextField<String>("name").setRequired(true)
			.add(StringValidator.maximumLength(TeamStore.MAX_LENGTH))
			.setLabel(new ResourceModel("name")));
		form.add(new EmailTextField("email").setRequired(true)
			.add(StringValidator.maximumLength(TeamStore.MAX_LENGTH))
			.setLabel(new ResourceModel("email")));
		Dropdown<String> role = new Dropdown<>("role", TeamStore.ROLES, new ChoiceRenderer<>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public Object getDisplayValue(String value)
			{
				return getString("role." + value);
			}

			@Override
			public String getIdValue(String value, int index)
			{
				return value;
			}
		});
		role.setNullValid(false);
		form.add(role.setRequired(true).setLabel(new ResourceModel("role")));
	}

	/**
	 * @return the factory of the Save button of the footer of the dialog, submitting the form,
	 *         also on Enter in a field
	 */
	public IModalButtonFactory saveButton()
	{
		return id -> new ModalSubmitButton(id, new ResourceModel("save"), form)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target)
			{
				TeamMember saved = DemoSession.get().getTeam().save(form.getModelObject());
				onSaved(saved, target);
			}

			@Override
			protected void onError(AjaxRequestTarget target)
			{
				target.add(feedback);
			}
		}.setPrimary(true).setDefault(true);
	}

	/**
	 * Called once the member is saved.
	 *
	 * @param saved
	 *            the member saved, {@code null} if it was deleted meanwhile
	 */
	protected abstract void onSaved(TeamMember saved, AjaxRequestTarget target);
}
