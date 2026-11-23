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

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.StringResourceModel;

import com.antiliacode.widgets.demo.BasePage;
import com.antiliacode.widgets.demo.account.Accounts;
import com.antiliacode.widgets.demo.account.DemosAuthorization;
import com.antiliacode.widgets.demo.account.SignedInUsers;
import com.antiliacode.widgets.demo.account.SignedInUsers.Place;

/**
 * The administration: how many of the {@value SignedInUsers#MAXIMUM} sessions of
 * {@value Accounts#USER} are signed in, and since when each one, when it made its last request and
 * until when it is sure of its place. Nothing tells who is behind a session: the site keeps no
 * record of the people who visit it. Only for administrators, see {@link DemosAuthorization}.
 */
public class AdminPage extends BasePage
{
	private static final long serialVersionUID = 1L;

	/** How the times are shown: in UTC, to the second. */
	static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
		.withZone(ZoneOffset.UTC);

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
	protected String getPageTitle()
	{
		return getString("title") + " · " + super.getPageTitle();
	}
}
