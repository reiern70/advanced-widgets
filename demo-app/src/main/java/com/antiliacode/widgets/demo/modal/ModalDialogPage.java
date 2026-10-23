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
package com.antiliacode.widgets.demo.modal;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.AbstractChoice.LabelPosition;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.validation.validator.PatternValidator;

import com.antiliacode.widgets.button.AjaxActionButton;
import com.antiliacode.widgets.button.ButtonBehavior.Size;
import com.antiliacode.widgets.button.ButtonBehavior.Variant;
import com.antiliacode.widgets.demo.DemoPage;
import com.antiliacode.widgets.demo.DemoSession;
import com.antiliacode.widgets.demo.frameworks.FrameworkTabs;
import com.antiliacode.widgets.icon.SvgIcon;
import com.antiliacode.widgets.modal.IModalButtonFactory;
import com.antiliacode.widgets.modal.Modal;
import com.antiliacode.widgets.modal.ModalButton;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * A team whose members are added, edited and deleted in the {@link Modal} of ui-components:
 * Cancel and Save in its footer, on the right; Escape, its close button and a click around the
 * dialog close it, asking first when the form was modified; its header drags it and folds it on
 * a double click; the focus stays in it while it is open and goes back to the button that opened
 * it. Below the team, a form sets the {@link DialogOptions options} of a dialog, what closes it,
 * dragging, folding and its size, and opens it; dialogs nest in it, three deep. The buttons of
 * the footers are those of the theme, small, with icons. The React and Svelte tabs do the same
 * with the Modal of {@code @antilia/react} and {@code @antilia/svelte}, the same markup and
 * behaviors, and edit the same team through the {@link TeamResource}.
 */
public class ModalDialogPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	private final Modal dialog;

	private final WebMarkupContainer team;

	private final DialogOptions options = new DialogOptions();

	/**
	 * Constructor.
	 */
	public ModalDialogPage()
	{
		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);
		themed.add(new AjaxActionButton("add", new ResourceModel("addMember"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				showForm(new TeamMember(), new ResourceModel("newMember"), target);
			}
		}.setIcon(SvgIcon.USER_PLUS).setVariant(Variant.PRIMARY).setSize(Size.SMALL));

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
				item.add(new AjaxActionButton("edit", new ResourceModel("edit"))
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
				}.setIcon(SvgIcon.PEN).setSize(Size.SMALL).add(AttributeModifier.replace(
					"aria-label", getString("edit") + " " + member.getName())));
				item.add(new AjaxActionButton("delete", new ResourceModel("delete"))
				{
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target)
					{
						confirmDeletion(id, target);
					}
				}.setIcon(SvgIcon.TRASH_CAN).setSize(Size.SMALL).add(AttributeModifier.replace(
					"aria-label", getString("delete") + " " + member.getName())));
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

		Modal optionsDialog = new Modal("optionsDialog");
		add(optionsDialog);
		themed.add(newOptionsForm("options", optionsDialog));
	}

	/**
	 * @return the form setting the options of a dialog, each change kept at once, and the button
	 *         opening it
	 */
	private Form<DialogOptions> newOptionsForm(String id, Modal optionsDialog)
	{
		Form<DialogOptions> form = new Form<>(id, new CompoundPropertyModel<>(options));
		for (String option : List.of("closeOnEscape", "closeButton", "closeOnClick", "draggable",
			"foldable"))
		{
			form.add(keptOnChange(new CheckBox(option)));
		}
		RadioChoice<String> size = new RadioChoice<>("size", DialogOptions.SIZES,
			new ChoiceRenderer<>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public Object getDisplayValue(String value)
				{
					return getString("size." + value);
				}

				@Override
				public String getIdValue(String value, int index)
				{
					return value;
				}
			});
		size.setLabelPosition(LabelPosition.WRAP_AFTER);
		size.add(new AjaxFormChoiceComponentUpdatingBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
			}
		});
		form.add(size);
		form.add(keptOnChange(new TextField<String>("width")
			.add(new PatternValidator(DialogOptions.WIDTH_PATTERN))
			.setLabel(new ResourceModel("width"))));
		form.add(new AjaxActionButton("open", new ResourceModel("open"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				NestedDialogPanel.open(optionsDialog, options, 1, target);
			}
		}.setIcon(SvgIcon.WINDOW_RESTORE).setVariant(Variant.PRIMARY));
		return form;
	}

	/**
	 * @return the field, its value kept in the options as soon as it changes; a width not valid
	 *         shows its message
	 */
	private FormComponent<?> keptOnChange(FormComponent<?> field)
	{
		field.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				// kept in the options; nothing to repaint
			}

			@Override
			protected void onError(AjaxRequestTarget target, RuntimeException e)
			{
				// the message shows as a notification
			}
		});
		return field;
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
				target.add(team);
			}
		};
		dialog.setSize(Modal.Size.MEDIUM);
		dialog.open(title, form, target,
			IModalButtonFactory.close(new ResourceModel("cancel"), SvgIcon.XMARK),
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
		dialog.setSize(Modal.Size.SMALL);
		dialog.open(new ResourceModel("deleteMember"), question, target,
			IModalButtonFactory.close(new ResourceModel("cancel"), SvgIcon.XMARK),
			buttonId -> new ModalButton(buttonId, new ResourceModel("delete"))
			{
				private static final long serialVersionUID = 1L;

				@Override
				public void onClick(AjaxRequestTarget confirmed)
				{
					delete(id, member, confirmed);
				}
			}.setIcon(SvgIcon.TRASH_CAN).setVariant(Variant.DANGER));
	}

	private void delete(long id, TeamMember member, AjaxRequestTarget target)
	{
		dialog.close(target);
		if (DemoSession.get().getTeam().delete(id))
		{
			info(getString("deleted").replace("{0}", member.getName()));
			target.add(team);
		}
		else
		{
			gone(target);
		}
	}

	private void gone(AjaxRequestTarget target)
	{
		warn(getString("gone"));
		target.add(team);
	}

	@Override
	protected String getFrameworkDemo()
	{
		return "modalDialog";
	}

	/**
	 * @return the texts, the roles named in the language of the session, the URL of the team and
	 *         the icons of the buttons, as inline SVG
	 */
	@Override
	protected JSONObject getFrameworkConfig()
	{
		JSONObject texts = FrameworkTabs.texts(this, "team", "addMember", "newMember", "editMember",
			"edit", "delete", "deleteMember", "delete.confirm", "save", "cancel", "name", "email",
			"role", "actions", "added", "saved", "deleted", "gone", "empty", "discard", "close",
			"options", "options.behavior", "option.closeOnEscape", "option.closeButton",
			"option.closeOnClick", "option.draggable", "option.foldable", "size", "size.fit",
			"size.small", "size.medium", "size.large", "width", "open", "dialog.title",
			"dialog.text", "dialog.deepest", "dialog.nested", "dialog.ok");
		JSONObject icons = new JSONObject()
			.put("add", SvgIcon.USER_PLUS.getMarkup())
			.put("edit", SvgIcon.PEN.getMarkup())
			.put("delete", SvgIcon.TRASH_CAN.getMarkup())
			.put("cancel", SvgIcon.XMARK.getMarkup())
			.put("save", SvgIcon.FLOPPY_DISK.getMarkup())
			.put("open", SvgIcon.WINDOW_RESTORE.getMarkup())
			.put("nested", SvgIcon.LAYER_GROUP.getMarkup())
			.put("ok", SvgIcon.CHECK.getMarkup());
		JSONArray roles = new JSONArray();
		for (String role : TeamStore.ROLES)
		{
			roles.put(new JSONObject().put("value", role).put("label", getString("role." + role)));
		}
		return new JSONObject()
			.put("texts", FrameworkTabs.withDropdownTexts(this, texts))
			.put("roles", roles)
			.put("teamUrl", FrameworkTabs.urlOf(this, TeamResource.REFERENCE))
			.put("icons", icons)
			.put("maxLevel", DialogOptions.MAX_LEVEL);
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(
			new CssResourceReference(ModalDialogPage.class, "ModalDialogPage.css")));
	}
}
