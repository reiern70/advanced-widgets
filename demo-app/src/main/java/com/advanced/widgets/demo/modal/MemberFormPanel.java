/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.modal;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
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

/**
 * The form of a member of the team, shown in a modal dialog: the name, the email and the role,
 * validated with the messages the {@link TeamResource} answers the React and Svelte apps with.
 * The form edits a copy of the member, saved into the session's {@link TeamStore} on Save.
 */
public abstract class MemberFormPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param id
	 *            component id
	 * @param member
	 *            the member, new if its id is {@code 0}
	 */
	public MemberFormPanel(String id, TeamMember member)
	{
		super(id);
		Form<TeamMember> form = new Form<>("form", new CompoundPropertyModel<>(member.copy()));
		add(form);
		FeedbackPanel feedback = new FeedbackPanel("feedback");
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

		form.add(new AjaxButton("save")
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
		});
		form.add(new AjaxLink<Void>("cancel")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				onCancel(target);
			}
		});
	}

	/**
	 * Called once the member is saved.
	 *
	 * @param saved
	 *            the member saved, {@code null} if it was deleted meanwhile
	 */
	protected abstract void onSaved(TeamMember saved, AjaxRequestTarget target);

	/**
	 * Called when the user cancels the form.
	 */
	protected abstract void onCancel(AjaxRequestTarget target);
}
