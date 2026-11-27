/*
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
package com.antiliacode.widgets.demo.admin;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;

import com.antiliacode.widgets.demo.BasePage;
import com.antiliacode.widgets.demo.account.Accounts;
import com.antiliacode.widgets.demo.account.DemosAuthorization;
import com.antiliacode.widgets.demo.account.SignedInUsers;
import com.antiliacode.widgets.demo.account.SignedInUsers.Place;
import com.antiliacode.widgets.demo.stats.StatsResource;
import com.antiliacode.widgets.demo.stats.Visits;
import com.antiliacode.widgets.demo.stats.Visits.DayCount;
import com.antiliacode.widgets.demo.stats.Visits.PageCount;
import com.antiliacode.widgets.demo.stats.Visits.Report;
import com.antiliacode.widgets.demo.stats.Visits.VisitorCount;

/**
 * The administration: how many of the {@value SignedInUsers#MAXIMUM} sessions of
 * {@value Accounts#USER} are signed in, and since when each one, when it made its last request and
 * until when it is sure of its place; nothing tells who is behind a session. Then the
 * {@link Visits} of the last {@value StatsResource#DEFAULT_DAYS} days: how many people came and
 * signed in, the pages they opened and the days, which {@link StatsResource} gives as JSON too.
 * Only for administrators, see {@link DemosAuthorization}.
 */
public class AdminPage extends BasePage
{
	private static final long serialVersionUID = 1L;

	/** How the times are shown: in UTC, to the second. */
	static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
		.withZone(ZoneOffset.UTC);

	/** The most visitors listed, the ones seen last. */
	static final int MAXIMUM_VISITORS = 50;

	/** The visits of the last days, counted again at each render. */
	private final IModel<Report> visits = LoadableDetachableModel
		.of(() -> Visits.get().report(StatsResource.DEFAULT_DAYS));

	public AdminPage()
	{
		add(new Label("lead", () -> new StringResourceModel("lead", this)
			.setParameters(SignedInUsers.get().count(), SignedInUsers.MAXIMUM, Accounts.USER)
			.getString()));
		WebMarkupContainer table = new WebMarkupContainer("table")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(SignedInUsers.get().count() > 0);
			}
		};
		add(table);
		table.add(new ListView<Place>("places", () -> SignedInUsers.get().places())
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<Place> item)
			{
				Place place = item.getModelObject();
				item.add(new Label("number", item.getIndex() + 1));
				item.add(new Label("since", format(place.since())));
				item.add(new Label("seen", format(place.seen())));
				item.add(new Label("sureUntil", format(place.since().plus(SignedInUsers.PLACE_TIME))));
			}
		});
		addVisits();
	}

	/**
	 * Adds the visits of the last {@value StatsResource#DEFAULT_DAYS} days.
	 */
	private void addVisits()
	{
		IModel<Report> report = visits;
		add(new Label("visitsLead", () -> {
			Report counted = report.getObject();
			return new StringResourceModel("visitsLead", this)
				.setParameters(counted.people(), StatsResource.DEFAULT_DAYS, counted.newPeople(),
					counted.signedInPeople(), counted.signIns(), counted.views(),
					counted.viewsWithoutCookie())
				.getString();
		}));
		add(new Label("visitsKept", () -> report.getObject().firstDay() == null
			? getString("visitsNone")
			: new StringResourceModel("visitsKept", this)
				.setParameters(report.getObject().firstDay().toString()).getString()));
		add(new ResourceLink<>("json", StatsResource.REFERENCE));

		WebMarkupContainer pagesTable = new WebMarkupContainer("pagesTable")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(!report.getObject().pages().isEmpty());
			}
		};
		add(pagesTable);
		pagesTable.add(new ListView<PageCount>("pages", () -> report.getObject().pages())
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<PageCount> item)
			{
				PageCount page = item.getModelObject();
				item.add(new Label("page", page.page()));
				item.add(new Label("views", page.views()));
				item.add(new Label("people", page.people()));
			}
		});

		WebMarkupContainer visitorsTable = new WebMarkupContainer("visitorsTable")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(!report.getObject().visitors().isEmpty());
			}
		};
		add(visitorsTable);
		visitorsTable.add(new Label("visitorsLead", () -> new StringResourceModel("visitorsLead",
			this).setParameters(Math.min(MAXIMUM_VISITORS, report.getObject().visitors().size()),
				report.getObject().visitors().size())
			.getString()));
		visitorsTable.add(new ListView<VisitorCount>("visitors", () -> report.getObject()
			.visitors().stream().limit(MAXIMUM_VISITORS).toList())
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<VisitorCount> item)
			{
				VisitorCount visitor = item.getModelObject();
				item.add(new Label("visitor", visitor.visitor()));
				item.add(new Label("firstSeen", visitor.firstSeen() == null ? ""
					: format(visitor.firstSeen())));
				item.add(new Label("lastSeen", visitor.lastSeen() == null ? ""
					: format(visitor.lastSeen())));
				item.add(new Label("days", visitor.days()));
				item.add(new Label("views", visitor.views()));
				item.add(new Label("signIns", visitor.signIns()));
				StringBuilder pages = new StringBuilder();
				visitor.pages().forEach((page, views) -> pages
					.append(pages.isEmpty() ? "" : " · ").append(page).append(' ').append(views));
				item.add(new Label("pages", pages.toString()));
			}
		});

		IModel<List<DayCount>> activeDays = () -> report.getObject().days().stream()
			.filter(day -> day.views() > 0 || day.signIns() > 0)
			.toList();
		WebMarkupContainer daysTable = new WebMarkupContainer("daysTable")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(!activeDays.getObject().isEmpty());
			}
		};
		add(daysTable);
		daysTable.add(new ListView<DayCount>("days", activeDays)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<DayCount> item)
			{
				DayCount day = item.getModelObject();
				item.add(new Label("date", day.date().toString()));
				item.add(new Label("people", day.people()));
				item.add(new Label("newPeople", day.newPeople()));
				item.add(new Label("signedInPeople", day.signedInPeople()));
				item.add(new Label("signIns", day.signIns()));
				item.add(new Label("views", day.views()));
			}
		});
	}

	/**
	 * @return the time as the page shows it
	 */
	static String format(Instant instant)
	{
		return TIME.format(instant);
	}

	@Override
	protected void onConfigure()
	{
		super.onConfigure();
		// shown again after the session stopped being an administrator's, which Wicket refuses
		// without telling where to go
		if (!isActionAuthorized(RENDER))
		{
			DemosAuthorization.turnAway();
		}
	}

	@Override
	protected void onDetach()
	{
		visits.detach();
		super.onDetach();
	}

	@Override
	protected String getPageTitle()
	{
		return getString("title") + " · " + super.getPageTitle();
	}
}
