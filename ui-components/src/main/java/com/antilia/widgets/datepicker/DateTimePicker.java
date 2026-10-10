/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.datepicker;

import java.time.LocalDateTime;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import org.apache.wicket.model.IModel;

/**
 * A text field holding a {@link LocalDateTime} to the minute, shown as a
 * {@link DatePickerBehavior date picker} with a time. Attach it to an {@code <input type="text">}.
 *
 * @since 11.0.0
 */
public class DateTimePicker extends AbstractDatePicker<LocalDateTime>
{
	private static final long serialVersionUID = 1L;

	private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm");

	/**
	 * A date and time picker whose model is inherited, for example from a
	 * {@code CompoundPropertyModel}.
	 *
	 * @param id
	 *            component id
	 */
	public DateTimePicker(String id)
	{
		this(id, null);
	}

	/**
	 * @param id
	 *            component id
	 * @param model
	 *            the date and time
	 */
	public DateTimePicker(String id, IModel<LocalDateTime> model)
	{
		super(id, model, LocalDateTime.class, true);
	}

	@Override
	public DateTimePicker setMinimum(LocalDateTime minimum)
	{
		super.setMinimum(minimum != null ? minimum.truncatedTo(ChronoUnit.MINUTES) : null);
		return this;
	}

	@Override
	public DateTimePicker setMaximum(LocalDateTime maximum)
	{
		super.setMaximum(maximum != null ? maximum.truncatedTo(ChronoUnit.MINUTES) : null);
		return this;
	}

	/**
	 * @param minuteStep
	 *            the minutes the minute field steps by
	 * @return {@code this}
	 */
	public DateTimePicker setMinuteStep(int minuteStep)
	{
		getDatePickerBehavior().setMinuteStep(minuteStep);
		return this;
	}

	@Override
	protected String toIso(LocalDateTime value)
	{
		return ISO.format(value);
	}

	@Override
	protected LocalDateTime fromIso(String text)
	{
		return LocalDateTime.parse(text).truncatedTo(ChronoUnit.MINUTES);
	}

	@Override
	protected DateTimeFormatter displayFormatter(Locale locale)
	{
		return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
			.withChronology(IsoChronology.INSTANCE)
			.withLocale(locale);
	}

	@Override
	protected String invalidKey()
	{
		return "DateTimePicker.invalid";
	}
}
