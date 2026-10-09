/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.modal;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;

import com.antilia.widgets.demo.DemoPage;
import com.antilia.widgets.demo.DemoSession;
import com.antilia.widgets.demo.frameworks.FrameworkTabs;
import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable;
import com.antilia.widgets.modal.IModalButtonFactory;
import com.antilia.widgets.modal.Modal;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * A team whose members are added, edited and deleted in the {@link Modal} of ui-components:
 * Cancel and Save in its footer, on the right; Escape, its close button and a click around the
 * dialog close it, asking first when the form was modified; its header drags it and folds it on
 * a double click; the focus stays in it while it is open and goes back to the button that opened
 * it. The React and Svelte tabs do the same with the Modal of
 * {@code @antilia/react} and {@code @antilia/svelte}, the same markup and
 * behaviors, and edit the same team through the {@link TeamResource}.
 */
public class ModalDialogPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	private final Modal dialog;

	private final WebMarkupContainer team;

	/**
	 * Constructor.
	 */
	public ModalDialogPage()
	{
		add(new FeedbackPanel("feedback").setOutputMarkupId(true));

		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);
		themed.add(new AjaxLink<Void>("add")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				showForm(new TeamMember(), new ResourceModel("newMember"), target);
			}
		});

		team = new WebMarkupContainer("team");
		team.setOutputMarkupId(true);
		themed.add(team);
		IModel<List<TeamMember>> members = LoadableDetachableModel.of(
			() -> DemoSession.get().getTeam().list());
		team.add(new ListView<>("members", members)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<TeamMember> item)
			{
				TeamMember member = item.getModelObject();
				long id = member.getId();
				item.add(new Label("name", member.getName()));
				item.add(new Label("email", member.getEmail()));
				item.add(new Label("role", getString("role." + member.getRole())));
				item.add(new AjaxLink<Void>("edit")
				{
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target)
					{
						TeamMember current = DemoSession.get().getTeam().find(id);
						if (current == null)
						{
							gone(target);
						}
						else
						{
							showForm(current, new StringResourceModel("editMember", this,
								Model.of(current)), target);
						}
					}
				}.add(AttributeModifier.replace("aria-label",
					getString("edit") + " " + member.getName())));
				item.add(new AjaxLink<Void>("delete")
				{
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target)
					{
						confirmDeletion(id, target);
					}
				}.add(AttributeModifier.replace("aria-label",
					getString("delete") + " " + member.getName())));
			}
		});
		team.add(new WebMarkupContainer("empty")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(members.getObject().isEmpty());
			}
		});

		dialog = new Modal("dialog");
		dialog.setCloseOnClick(true);
		dialog.confirmCloseIfModified(new ResourceModel("discard"));
		add(dialog);
	}

	/**
	 * Shows the form of a member in the dialog, Cancel and Save in its footer; saving it repaints
	 * the team.
	 */
	private void showForm(TeamMember member, IModel<String> title, AjaxRequestTarget target)
	{
		boolean added = member.getId() == 0;
		MemberFormPanel form = new MemberFormPanel(Modal.BODY_ID, member)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSaved(TeamMember saved, AjaxRequestTarget target)
			{
				dialog.close(target);
				if (saved == null)
				{
					gone(target);
					return;
				}
				ModalDialogPage.this.info(ModalDialogPage.this.getString(added ? "added" : "saved")
					.replace("{0}", saved.getName()));
				target.add(team, ModalDialogPage.this.get("feedback"));
			}
		};
		dialog.open(title, form, target, IModalButtonFactory.close(new ResourceModel("cancel")),
			form.saveButton());
	}

	/**
	 * Asks in the dialog to confirm the deletion of a member, then deletes it.
	 */
	private void confirmDeletion(long id, AjaxRequestTarget target)
	{
		TeamMember member = DemoSession.get().getTeam().find(id);
		if (member == null)
		{
			gone(target);
			return;
		}
		Label question = new Label(Modal.BODY_ID, new StringResourceModel("delete.confirm", this,
			Model.of(member)));
		dialog.open(new ResourceModel("deleteMember"), question, target,
			IModalButtonFactory.close(new ResourceModel("cancel")),
			IModalButtonFactory.primary(new ResourceModel("delete"), confirmed -> {
				dialog.close(confirmed);
				if (DemoSession.get().getTeam().delete(id))
				{
					info(getString("deleted").replace("{0}", member.getName()));
					confirmed.add(team, get("feedback"));
				}
				else
				{
					gone(confirmed);
				}
			}));
	}

	private void gone(AjaxRequestTarget target)
	{
		warn(getString("gone"));
		target.add(team, get("feedback"));
	}

	@Override
	protected String getFrameworkDemo()
	{
		return "modalDialog";
	}

	/**
	 * @return the texts, the roles named in the language of the session and the URL of the team
	 */
	@Override
	protected JSONObject getFrameworkConfig()
	{
		JSONObject texts = FrameworkTabs.texts(this, "team", "addMember", "newMember", "editMember",
			"edit", "delete", "deleteMember", "delete.confirm", "save", "cancel", "name", "email",
			"role", "actions", "added", "saved", "deleted", "gone", "empty", "discard", "close");
		JSONArray roles = new JSONArray();
		for (String role : TeamStore.ROLES)
		{
			roles.put(new JSONObject().put("value", role).put("label", getString("role." + role)));
		}
		return new JSONObject()
			.put("texts", FrameworkTabs.withDropdownTexts(this, texts))
			.put("roles", roles)
			.put("teamUrl", FrameworkTabs.urlOf(this, TeamResource.REFERENCE));
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		// the buttons
		response.render(CssHeaderItem.forReference(
			new CssResourceReference(BaseDynamicDataTable.class, "ant-data-table.css")));
		response.render(CssHeaderItem.forReference(
			new CssResourceReference(ModalDialogPage.class, "ModalDialogPage.css")));
	}
}
