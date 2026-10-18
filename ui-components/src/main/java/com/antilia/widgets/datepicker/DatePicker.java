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
package com.antilia.widgets.datepicker;

import java.time.LocalDate;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import org.apache.wicket.model.IModel;

/**
 * A text field holding a {@link LocalDate}, shown as a {@link DatePickerBehavior date picker}.
 * Attach it to an {@code <input type="text">}.
 *
 * @since 11.0.0
 */
public class DatePicker extends AbstractDatePicker<LocalDate>
{
	private static final long serialVersionUID = 1L;

	/**
	 * A date picker whose model is inherited, for example from a {@code CompoundPropertyModel}.
	 *
	 * @param id
	 *            component id
	 */
	public DatePicker(String id)
	{
		this(id, null);
	}

	/**
	 * @param id
	 *            component id
	 * @param model
	 *            the date
	 */
	public DatePicker(String id, IModel<LocalDate> model)
	{
		super(id, model, LocalDate.class, false);
	}

	@Override
	public DatePicker setMinimum(LocalDate minimum)
	{
		super.setMinimum(minimum);
		return this;
	}

	@Override
	public DatePicker setMaximum(LocalDate maximum)
	{
		super.setMaximum(maximum);
		return this;
	}

	@Override
	protected String toIso(LocalDate value)
	{
		return DateTimeFormatter.ISO_LOCAL_DATE.format(value);
	}

	@Override
	protected LocalDate fromIso(String text)
	{
		return LocalDate.parse(text);
	}

	@Override
	protected DateTimeFormatter displayFormatter(Locale locale)
	{
		return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
			.withChronology(IsoChronology.INSTANCE)
			.withLocale(locale);
	}

	@Override
	protected String invalidKey()
	{
		return "DatePicker.invalid";
	}
}
