/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
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
