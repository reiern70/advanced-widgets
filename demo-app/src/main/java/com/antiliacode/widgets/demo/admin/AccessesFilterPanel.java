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
package com.antiliacode.widgets.demo.admin;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

import com.antiliacode.widgets.datepicker.DateTimePicker;
import com.antiliacode.widgets.demo.data.AccessFilter;
import com.antiliacode.widgets.dropdown.Dropdown;

/**
 * Filters the accesses while the administrator types or chooses: by user, by the part opened,
 * public or private, by the address they came from, and by when they were on the server's clock,
 * in UTC as the table shows it, from a minute until another, both included; shown in front of the
 * navigation of the {@link AccessesPanel}'s table.
 */
abstract class AccessesFilterPanel extends AdminFilterPanel
{
	private static final long serialVersionUID = 1L;

	/** The minute the time until which the accesses are shown is, all of it. */
	private static final Duration MINUTE = Duration.ofMinutes(1);

	AccessesFilterPanel(String id, AccessFilter filter)
	{
		super(id);
		add(filtering(new TextField<>("user", new PropertyModel<String>(filter, "user")), "input"));
		Dropdown<Boolean> part = new Dropdown<>("part", new PropertyModel<>(filter, "privatePart"),
			List.of(Boolean.FALSE, Boolean.TRUE), new ChoiceRenderer<>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public Object getDisplayValue(Boolean privatePart)
				{
					return getString(privatePart ? "part.private" : "part.public");
				}

				@Override
				public String getIdValue(Boolean privatePart, int index)
				{
					return privatePart.toString();
				}
			});
		part.setNullValid(true);
		add(filtering(part, "change"));
		add(filtering(new TextField<>("address", new PropertyModel<String>(filter, "address")),
			"input"));

		DateTimePicker from = new DateTimePicker("accessedFrom", LambdaModel.of(
			() -> utc(filter.getFrom()), time -> filter.setFrom(instant(time))));
		from.getDatePickerBehavior()
			.setPlaceholder(new ResourceModel("filter.accessedFrom.placeholder"));
		add(filtering(from, "change"));
		DateTimePicker until = new DateTimePicker("accessedUntil", LambdaModel.of(
			() -> filter.getTo() == null ? null : utc(filter.getTo().minus(MINUTE)),
			time -> filter.setTo(time == null ? null : instant(time).plus(MINUTE))));
		until.getDatePickerBehavior()
			.setPlaceholder(new ResourceModel("filter.accessedUntil.placeholder"));
		add(filtering(until, "change"));
	}

	private static LocalDateTime utc(Instant instant)
	{
		return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
	}

	private static Instant instant(LocalDateTime time)
	{
		return time == null ? null : time.toInstant(ZoneOffset.UTC);
	}
}
