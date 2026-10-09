/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.list;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.ws.api.message.AbortedMessage;
import org.apache.wicket.protocol.ws.api.message.ClosedMessage;
import org.apache.wicket.protocol.ws.api.message.ConnectedMessage;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.resource.loader.ClassStringResourceLoader;

import com.advanced.widgets.demo.DemoPage;
import com.advanced.widgets.demo.DemoWebSocketBehavior;
import com.advanced.widgets.demo.datatable.DatabaseLocator;
import com.advanced.widgets.demo.datatable.ProgressSimulator;
import com.advanced.widgets.demo.datatable.User;
import com.advanced.widgets.demo.datatable.UserDetailsPanel;
import com.advanced.widgets.demo.datatable.UserEditPanel;
import com.advanced.widgets.demo.datatable.UserFilterPanel;
import com.advanced.widgets.demo.datatable.UsersProvider;
import com.advanced.widgets.demo.datatable.UsersRowsResource;
import com.advanced.widgets.demo.frameworks.ChoicesResource;
import com.advanced.widgets.demo.frameworks.FrameworkTabs;
import com.advanced.widgets.dropdown.Dropdown;
import com.advanced.widgets.dynamicdatatable.BaseDynamicDataTable;
import com.advanced.widgets.dynamicdatatable.BaseDynamicDataTable.BodyHeightUnits;
import com.advanced.widgets.jsonlist.IItemAction;
import com.advanced.widgets.jsonlist.JsonBasedInfiniteList;
import com.advanced.widgets.modal.IModalButtonFactory;
import com.advanced.widgets.modal.Modal;
import com.advanced.widgets.progress.ProgressBar;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * A {@link JsonBasedInfiniteList} of the thousand users of the table examples, each one a card,
 * whose buttons are handled by the list itself. The edit form and the confirmation of a deletion
 * are shown in a dialog of the page, and the user clicked on next to the list. The server pushes
 * the progress of the users shown over the page's web socket connection.
 * <p>
 * The React and Svelte tabs show the same cards, from the same template, the users loaded from
 * the {@link UsersRowsResource}; the actions of a card are handled by the app, which keeps what it
 * changes in the browser.
 */
public class JsonListPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	/** The number of users fetched at a time. */
	static final int USERS_PER_PAGE = 24;

	/** The properties the users can be sorted by. */
	private static final List<String> SORTS = List.of("firstName", "lastName", "bornDate");

	private final UsersProvider provider = new UsersProvider();

	private final JsonBasedInfiniteList<User, Long> list;

	private final UserDetailsPanel details;

	private final Modal dialog;

	private String sort = "firstName";

	private User selected;

	/**
	 * Constructor.
	 */
	public JsonListPage()
	{
		provider.setSort(sort, SortOrder.ASCENDING);

		add(new FeedbackPanel("feedback").setOutputMarkupId(true));

		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);

		themed.add(new UserFilterPanel("filter", new PropertyModel<>(provider, "filter"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onFilter(AjaxRequestTarget target)
			{
				startOver(target);
			}
		});
		themed.add(newSortChoice("sort"));
		themed.add(new AjaxLink<Void>("add")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				showUserForm(new User(), new ResourceModel("newUser"), target);
			}
		});

		list = new JsonBasedInfiniteList<>("users", provider, USERS_PER_PAGE)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onItemsSent(List<Long> keys)
			{
				ProgressSimulator.show(getSession().getId(), getPage().getPageId(), keys);
			}

			@Override
			protected void onNotFoundAction(AjaxRequestTarget target, String action, Long key,
				User user)
			{
				warn(getString("userGone"));
				target.add(JsonListPage.this.get("feedback"));
				refresh(target);
			}
		};
		list.setItemTemplate(JsonListPage.class, "UserCard");
		list.setHeight(70, BodyHeightUnits.VIEWPORT_HEIGHT);
		list.setPushEnabled(true);
		list.addAction(IItemAction.of("details", this::showDetails));
		list.addAction(IItemAction.of("edit", (user, target) -> {
			showDetails(user, target);
			showUserForm(user, new StringResourceModel("editUser", this, Model.of(user)), target);
		}));
		list.addAction(IItemAction.of("delete", this::confirmDeletion));
		themed.add(list);

		details = new UserDetailsPanel("details", new PropertyModel<>(this, "selected"));
		themed.add(details);
		themed.add(new Label("noneSelected", new ResourceModel("noneSelected"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(selected == null);
			}
		}.setOutputMarkupPlaceholderTag(true));

		dialog = new Modal("dialog");
		add(dialog);

		add(new DemoWebSocketBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConnect(ConnectedMessage message)
			{
				ProgressSimulator.start(message.getApplication(), message.getSessionId(),
					getPageId(), message.getKey(), list.getMarkupId(),
					DatabaseLocator.getLargeDatabase(), getLocale());
			}

			@Override
			protected void onClose(ClosedMessage message)
			{
				ProgressSimulator.stop(message.getSessionId(), getPageId());
			}

			@Override
			protected void onAbort(AbortedMessage message)
			{
				ProgressSimulator.stop(message.getSessionId(), getPageId());
			}
		});
	}

	/**
	 * @return the user shown next to the list
	 */
	public User getSelected()
	{
		return selected;
	}

	/**
	 * @param selected
	 *            the user shown next to the list
	 */
	public void setSelected(User selected)
	{
		this.selected = selected;
	}

	/**
	 * @return the property the users are sorted by
	 */
	public String getSort()
	{
		return sort;
	}

	/**
	 * @param sort
	 *            the property the users are sorted by
	 */
	public void setSort(String sort)
	{
		this.sort = sort;
	}

	private Component newSortChoice(String id)
	{
		Dropdown<String> choice = new Dropdown<>(id, new PropertyModel<>(this, "sort"),
			SORTS, new ChoiceRenderer<>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public Object getDisplayValue(String property)
				{
					return getString("sort." + property);
				}

				@Override
				public String getIdValue(String property, int index)
				{
					return property;
				}
			});
		choice.setNullValid(false);
		choice.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				provider.setSort(sort, SortOrder.ASCENDING);
				startOver(target);
			}
		});
		return choice;
	}

	/**
	 * Shows the first page of the users again, scrolled to the top.
	 */
	private void startOver(AjaxRequestTarget target)
	{
		list.reset();
		list.refresh(target);
	}

	private void showDetails(User user, AjaxRequestTarget target)
	{
		selected = user;
		target.add(details, getThemed().get("noneSelected"));
	}

	private WebMarkupContainer getThemed()
	{
		return (WebMarkupContainer)get("themed");
	}

	/**
	 * Shows the form of a user in the dialog: a saved user is repainted in place, a new one
	 * refreshes the list.
	 */
	private void showUserForm(User user, IModel<String> title, AjaxRequestTarget target)
	{
		UserEditPanel form = new UserEditPanel(Modal.BODY_ID, user)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSaved(User saved, AjaxRequestTarget target)
			{
				dialog.close(target);
				if (user.getId() == 0)
				{
					JsonListPage.this.info(JsonListPage.this.message("added",
						saved.getFirstName(), saved.getLastName()));
					target.add(JsonListPage.this.get("feedback"));
					list.refresh(target);
				}
				else
				{
					list.updateItem(target, saved);
				}
				showDetails(saved, target);
			}

			@Override
			protected void onCancel(AjaxRequestTarget target)
			{
				dialog.close(target);
			}
		};
		dialog.open(title, form, target);
	}

	/**
	 * Asks in the dialog to confirm the deletion of a user, then deletes it and refreshes the
	 * list.
	 */
	private void confirmDeletion(User user, AjaxRequestTarget target)
	{
		long id = user.getId();
		Label question = new Label(Modal.BODY_ID, new StringResourceModel("delete.confirm", this,
			Model.of(user)));
		dialog.open(new ResourceModel("delete"), question, target,
			IModalButtonFactory.close(new ResourceModel("cancel")),
			IModalButtonFactory.primary(new ResourceModel("delete"), confirmed -> {
				dialog.close(confirmed);
				User current = list.findItem(String.valueOf(id));
				if (current == null)
				{
					warn(getString("userGone"));
				}
				else
				{
					DatabaseLocator.getLargeDatabase().delete(current);
					info(message("deleted", current.getFirstName(), current.getLastName()));
					if (Objects.equals(selected, current))
					{
						selected = null;
						confirmed.add(details, getThemed().get("noneSelected"));
					}
				}
				confirmed.add(get("feedback"));
				list.refresh(confirmed);
			}));
	}

	@Override
	protected String getFrameworkDemo()
	{
		return "jsonList";
	}

	/**
	 * @return the texts, the template of the cards in the language of the session, the URLs of the
	 *         users and of the countries, and how the list loads and sorts them
	 */
	@Override
	protected JSONObject getFrameworkConfig()
	{
		JSONObject texts = FrameworkTabs.texts(this, "users", "sortBy", "sort.firstName",
			"sort.lastName", "sort.bornDate", "filter.placeholder", "newUser",
			"editUser", "delete", "cancel", "delete.confirm", "deleted", "added", "noneSelected",
			"selectedUser", "id", "firstName", "lastName", "homePhone", "cellPhone", "bornDate");
		// the form's labels, from the bundles of the Wicket form
		ClassStringResourceLoader form = new ClassStringResourceLoader(UserEditPanel.class);
		for (String key : new String[] { "country", "city", "address", "save" })
		{
			texts.put(key, form.loadStringResource(UserEditPanel.class, key, getLocale(),
				getStyle(), getVariation()));
		}
		// the label of the filter; the drop-downs' filter is theirs
		texts.put("filterLabel", getString("filter"));
		texts.put("required", getString("Required", Model.ofMap(Map.of("label", "{0}"))));
		texts.put("loading", getString("JsonBasedInfiniteList.loading"));
		texts.put("end", getString("JsonBasedInfiniteList.end"));
		texts.put("empty", getString("JsonBasedInfiniteList.empty"));
		return new JSONObject()
			.put("texts", FrameworkTabs.withDropdownTexts(this, texts))
			.put("template", list.getItemTemplate())
			.put("usersUrl", FrameworkTabs.urlOf(this, UsersRowsResource.REFERENCE))
			.put("choicesUrl", FrameworkTabs.urlOf(this, ChoicesResource.REFERENCE))
			.put("perPage", USERS_PER_PAGE)
			.put("sort", sort)
			.put("sorts", new JSONArray(SORTS))
			.put("height", new JSONObject().put("value", 70).put("unit", "vh"))
			.put("locale", getLocale().toLanguageTag());
	}

	private String message(String key, Object... parameters)
	{
		return new StringResourceModel(key, this).setParameters(parameters).getString();
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(ProgressBar.CSS));
		// the buttons of the user form
		response.render(CssHeaderItem.forReference(
			new CssResourceReference(BaseDynamicDataTable.class, "dynamic-data-table.css")));
		response.render(CssHeaderItem.forReference(
			new CssResourceReference(JsonListPage.class, "JsonListPage.css")));
	}
}
