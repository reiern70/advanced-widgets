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
package com.antiliacode.widgets.datepicker;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.Locale;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

/**
 * A text field holding a date, or a date and time, as ISO text, shown as a
 * {@link DatePickerBehavior date picker}. It converts the ISO text the picker posts and refuses
 * other text, and dates outside its {@link #setMinimum(Temporal) minimum} and
 * {@link #setMaximum(Temporal) maximum}, which the calendar does not offer either.
 *
 * @param <T>
 *            the type of the value, {@code LocalDate} or {@code LocalDateTime}
 * @since 11.0.0
 */
public abstract class AbstractDatePicker<T extends Temporal & Comparable<? super T>>
	extends TextField<T>
{
	private static final long serialVersionUID = 1L;

	private final DatePickerBehavior picker;

	private T minimum;

	private T maximum;

	/**
	 * @param id
	 *            component id
	 * @param model
	 *            the value, {@code null} for an inherited model
	 * @param type
	 *            the type of the value
	 * @param time
	 *            whether the picker takes a time too
	 */
	protected AbstractDatePicker(String id, IModel<T> model, Class<T> type, boolean time)
	{
		super(id, model, type);
		picker = new DatePickerBehavior(time);
		add(picker);
		add(new RangeValidator());
	}

	/**
	 * @return the behavior drawing the picker, to set its locale, placeholder or minute step
	 */
	public DatePickerBehavior getDatePickerBehavior()
	{
		return picker;
	}

	/**
	 * @return the first value taken, {@code null} for none
	 */
	public T getMinimum()
	{
		return minimum;
	}

	/**
	 * @param minimum
	 *            the first value taken, {@code null} for none
	 * @return {@code this}
	 */
	public AbstractDatePicker<T> setMinimum(T minimum)
	{
		this.minimum = minimum;
		picker.setMinimum(minimum != null ? toIso(minimum) : null);
		return this;
	}

	/**
	 * @return the last value taken, {@code null} for none
	 */
	public T getMaximum()
	{
		return maximum;
	}

	/**
	 * @param maximum
	 *            the last value taken, {@code null} for none
	 * @return {@code this}
	 */
	public AbstractDatePicker<T> setMaximum(T maximum)
	{
		this.maximum = maximum;
		picker.setMaximum(maximum != null ? toIso(maximum) : null);
		return this;
	}

	/**
	 * @return the value as ISO text
	 */
	protected abstract String toIso(T value);

	/**
	 * @return the ISO text as a value
	 * @throws DateTimeParseException
	 *             when the text is no such value
	 */
	protected abstract T fromIso(String text);

	/**
	 * @return how a minimum or maximum is shown in the message refusing a value, in the locale of
	 *         the session
	 */
	protected abstract DateTimeFormatter displayFormatter(Locale locale);

	/**
	 * @return the key of the message refusing text that is no value
	 */
	protected abstract String invalidKey();

	@Override
	protected IConverter<?> createConverter(Class<?> type)
	{
		return new IsoConverter();
	}

	/**
	 * Converts the ISO text the picker posts.
	 */
	private class IsoConverter implements IConverter<T>
	{
		private static final long serialVersionUID = 1L;

		@Override
		public T convertToObject(String value, Locale locale)
		{
			if (value == null || value.isBlank())
			{
				return null;
			}
			try
			{
				return fromIso(value.trim());
			}
			catch (DateTimeParseException e)
			{
				throw new ConversionException(e).setResourceKey(invalidKey())
					.setSourceValue(value)
					.setLocale(locale);
			}
		}

		@Override
		public String convertToString(T value, Locale locale)
		{
			return value != null ? toIso(value) : null;
		}
	}

	/**
	 * Refuses values before the minimum or after the maximum.
	 */
	private class RangeValidator implements IValidator<T>
	{
		private static final long serialVersionUID = 1L;

		@Override
		public void validate(IValidatable<T> validatable)
		{
			T value = validatable.getValue();
			DateTimeFormatter formatter = displayFormatter(getLocale());
			if (minimum != null && value.compareTo(minimum) < 0)
			{
				validatable.error(new ValidationError().addKey("DatePicker.minimum")
					.setVariable("minimum", formatter.format(minimum)));
			}
			else if (maximum != null && value.compareTo(maximum) > 0)
			{
				validatable.error(new ValidationError().addKey("DatePicker.maximum")
					.setVariable("maximum", formatter.format(maximum)));
			}
		}
	}
}
