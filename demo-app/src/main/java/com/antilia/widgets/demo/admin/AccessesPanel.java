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
package com.antilia.widgets.demo.admin;

import java.util.Iterator;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import com.antilia.widgets.demo.account.Accounts;
import com.antilia.widgets.demo.data.AbstractAccess;
import com.antilia.widgets.demo.data.AccessFilter;
import com.antilia.widgets.demo.data.Accesses;
import com.antilia.widgets.demo.data.User;
import com.antilia.widgets.dynamicdatatable.IIdentifiableDynamicDataProvider;
import com.antilia.widgets.dynamicdatatable.SortableDynamicDataProvider;
import com.antilia.widgets.popover.Popover;
import com.antilia.widgets.popover.PopoverPlacement;

/**
 * Who accessed the site, one row per session, the latest first: who, whether the session opened
 * the demos, from where, when on the server's clock and on the visitor's, and the browser; the
 * recent accesses or, once {@link Accesses#archive moved}, the old ones. Filtered by user, part,
 * address and the server's time (see {@link AccessesFilterPanel}), the private part at first,
 * where the visitors who signed in are. A button beside a user who is an account tells all about
 * it in a popover.
 */
class AccessesPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	private final AccessFilter filter = new AccessFilter();

	/** The id of the account the popover tells about, {@code null} before one is asked about. */
	private Long account;

	/**
	 * @param id
	 *            component id
	 * @param old
	 *            whether the panel shows the old accesses, those moved out of the recent ones
	 */
	AccessesPanel(String id, boolean old)
	{
		super(id);
		filter.setPrivatePart(Boolean.TRUE);

		UserInfoPanel userInfo = Popover.asContent(new UserInfoPanel("userInfo",
			LoadableDetachableModel.of(() -> account == null ? null : Accounts.get().find(account))));
		add(userInfo);

		List<IColumn<AbstractAccess, String>> columns = List.of(
			new AdminColumn<AbstractAccess>("user", "userName", AbstractAccess::getUserName)
			{
				private static final long serialVersionUID = 1L;

				@Override
				public void populateItem(Item<ICellPopulator<AbstractAccess>> item,
					String componentId, IModel<AbstractAccess> rowModel)
				{
					item.add(new UserCell(componentId, rowModel.getObject().getUserName())
					{
						private static final long serialVersionUID = 1L;

						@Override
						protected void onInfo(AjaxRequestTarget target, Component trigger,
							Long account)
						{
							showAccount(target, trigger, account, userInfo);
						}
					});
				}
			},
			new AdminColumn<AbstractAccess>("part", "privatePart",
				access -> getString(access.isPrivatePart() ? "part.private" : "part.public")),
			new AdminColumn<>("address", "address", AbstractAccess::getAddress),
			new AdminColumn<>("accessed", "accessed",
				access -> AdminTables.format(access.getAccessed())),
			new AdminColumn<>("localTime", "localTime",
				access -> AdminTables.format(access.getLocalTime())),
			new AdminColumn<>("timeZone", "timeZone", AbstractAccess::getTimeZone),
			new AdminColumn<>("browser", null, AbstractAccess::getUserAgent));

		AccessesProvider provider = new AccessesProvider(filter, old);
		add(new Label("lead", () -> old
			? getString("oldAccesses.lead").replace("{0}", String.valueOf(Accesses.KEPT.toDays()))
			: getString("accesses.lead")));
		add(new Label("count", () -> provider.size()).setOutputMarkupId(true));
		FilteredAdminTable<AbstractAccess> table = new FilteredAdminTable<>("table", columns,
			provider)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected AdminFilterPanel newFilter(String id)
			{
				return new AccessesFilterPanel(id, filter)
				{
					private static final long serialVersionUID = 1L;

					@Override
					protected void onFilter(AjaxRequestTarget target)
					{
						filtered(target);
						target.add(AccessesPanel.this.get("count"));
					}
				};
			}
		};
		// wide enough for the emails of the users, the addresses and the times, the rest for the
		// browser
		table.setColumnProportions(2.2, 0.8, 1.4, 2, 1.6, 1.4, 3.6);
		add(AdminTables.configure(table, old ? "old-accesses.csv" : "accesses.csv"));
	}

	/**
	 * Shows all about an account in a popover pointing at the button that asked.
	 */
	private void showAccount(AjaxRequestTarget target, Component trigger, Long account,
		UserInfoPanel userInfo)
	{
		this.account = account;
		userInfo.getModel().detach();
		target.add(userInfo);
		User user = userInfo.getModelObject();
		new Popover(userInfo).setTitle(user == null ? getString("userInfo") : user.getName())
			.setPlacement(PopoverPlacement.RIGHT_TOP)
			.setMaxWidth("28rem")
			.show(target, trigger);
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		Popover.renderHeadItems(response);
	}

	/**
	 * The recent or the old accesses, a page at a time from the database, sorted there.
	 */
	private static class AccessesProvider
		extends
			SortableDynamicDataProvider<AbstractAccess, Long, String>
		implements
			IIdentifiableDynamicDataProvider<AbstractAccess, Long>
	{
		private static final long serialVersionUID = 1L;

		private final AccessFilter filter;

		private final boolean old;

		AccessesProvider(AccessFilter filter, boolean old)
		{
			super(Long.class);
			this.filter = filter;
			this.old = old;
			setSort("accessed", SortOrder.DESCENDING);
		}

		@Override
		public Iterator<? extends AbstractAccess> iterator(long first, long count)
		{
			Accesses accesses = Accesses.get();
			return (old ? accesses.pageOld(filter, AdminTables.pageable(first, count, getSort()))
				: accesses.page(filter, AdminTables.pageable(first, count, getSort()))).iterator();
		}

		@Override
		public long size()
		{
			return old ? Accesses.get().countOld(filter) : Accesses.get().count(filter);
		}

		@Override
		public AbstractAccess findByKey(Long key)
		{
			return old ? Accesses.get().findOld(key) : Accesses.get().find(key);
		}
	}
}
