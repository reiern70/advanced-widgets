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
package com.antiliacode.widgets.demo.datepicker;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;

import com.antiliacode.widgets.datepicker.AbstractDatePicker;
import com.antiliacode.widgets.datepicker.DatePicker;
import com.antiliacode.widgets.datepicker.DatePickerBehavior;
import com.antiliacode.widgets.datepicker.DateTimePicker;
import com.antiliacode.widgets.demo.DemoPage;
import com.antiliacode.widgets.demo.frameworks.FrameworkTabs;
import com.antiliacode.widgets.dropdown.Dropdown;
import com.antiliacode.widgets.dropdown.DropdownBehavior;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * {@link DatePicker} and {@link DateTimePicker}: dates, and dates with a time, chosen from a
 * calendar or typed the way the locale writes them, posted as ISO text and checked against their
 * bounds by the server too. A drop-down shows the calendars in other locales: their format, the
 * names of the months and days, the first day of the week and the clock follow it, while the texts
 * of the buttons stay in the language of the session. The values are shown below the form as Ajax
 * reports them. The React and Svelte tabs show the same form in those frameworks.
 */
public class DatePickerPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	/** The locale of the calendars that stands for the session's. */
	private static final String PAGE_LOCALE = "page";

	/** The locales the calendars can be shown in. */
	private static final List<String> LOCALES = List.of(PAGE_LOCALE, "en-US", "en-GB", "de-DE", "es-ES",
		"fr-FR", "ja-JP");

	private LocalDate born = LocalDate.of(1990, 5, 17);

	private LocalDate start;

	private LocalDateTime appointment = LocalDate.now().plusDays(7).atTime(10, 0);

	private LocalDateTime departure;

	private LocalDate disabled = LocalDate.of(2026, 1, 1);

	private String calendarLocale = PAGE_LOCALE;

	/**
	 * Constructor.
	 */
	public DatePickerPage()
	{
		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);

		Form<Void> form = new Form<>("form")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit()
			{
				info(new StringResourceModel("sent", DatePickerPage.this).setParameters(
					describeChoices()).getString());
			}
		};
		form.setOutputMarkupId(true);
		themed.add(form);

		Label choices = new Label("choices", this::describeChoices);
		choices.setOutputMarkupId(true);
		themed.add(choices);

		LocalDate today = LocalDate.now();
		form.add(reportChanges(new DatePicker("born", new PropertyModel<>(this, "born"))
			.setMaximum(today), "born", choices));
		form.add(reportChanges(new DatePicker("start", new PropertyModel<>(this, "start"))
			.setMinimum(today)
			.setMaximum(today.plusYears(1)), "start", choices));
		form.add(reportChanges(new DateTimePicker("appointment",
			new PropertyModel<>(this, "appointment"))
			.setMinuteStep(15)
			.setMinimum(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)), "appointment",
			choices));
		form.add(reportChanges(new DateTimePicker("departure",
			new PropertyModel<>(this, "departure")), "departure", choices));
		form.add(new DatePicker("disabled", new PropertyModel<>(this, "disabled"))
			.setEnabled(false));

		Dropdown<String> locales = new Dropdown<>("calendarLocale",
			new PropertyModel<>(this, "calendarLocale"), LOCALES, new LocaleRenderer());
		locales.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				Locale locale = PAGE_LOCALE.equals(calendarLocale) ? null
					: Locale.forLanguageTag(calendarLocale);
				form.visitChildren(AbstractDatePicker.class, (picker, visit) ->
					((AbstractDatePicker<?>)picker).getDatePickerBehavior().setLocale(locale));
				target.add(form);
			}
		});
		form.add(locales);
	}

	private static <C extends FormComponent<?>> C reportChanges(C picker, String label,
		Label choices)
	{
		picker.setLabel(new ResourceModel(label));
		picker.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				target.add(choices);
			}
		});
		return picker;
	}

	/**
	 * @return what is chosen, in words, the dates as the session's locale writes them
	 */
	private String describeChoices()
	{
		return new StringResourceModel("choices", this).setParameters(
			describe(born, FormatStyle.MEDIUM, null), describe(start, FormatStyle.MEDIUM, null),
			describe(appointment, FormatStyle.MEDIUM, FormatStyle.SHORT),
			describe(departure, FormatStyle.MEDIUM, FormatStyle.SHORT)).getString();
	}

	private String describe(Temporal value, FormatStyle date, FormatStyle time)
	{
		if (value == null)
		{
			return getString("nothing");
		}
		DateTimeFormatter formatter = time == null ? DateTimeFormatter.ofLocalizedDate(date)
			: DateTimeFormatter.ofLocalizedDateTime(date, time);
		return formatter.withChronology(IsoChronology.INSTANCE).withLocale(getLocale())
			.format(value);
	}

	@Override
	protected String getFrameworkDemo()
	{
		return "datePicker";
	}

	/**
	 * @return the texts, those of the pickers in the language of the session, the locales, the
	 *         bounds and the values as the page has them, as ISO text
	 */
	@Override
	protected JSONObject getFrameworkConfig()
	{
		JSONObject texts = FrameworkTabs.texts(this, "dates", "datesAndTimes", "localeTitle",
			"born", "start", "appointment", "departure", "calendarLocale", "disabledTitle", "send",
			"nothing", "choices", "sent");
		JSONArray locales = new JSONArray();
		LocaleRenderer renderer = new LocaleRenderer();
		LOCALES.forEach(tag -> locales.put(new JSONObject().put("value", tag)
			.put("label", renderer.getDisplayValue(tag).toString())));
		LocalDate today = LocalDate.now();
		JSONObject bounds = new JSONObject()
			.put("bornMax", today.toString())
			.put("startMin", today.toString())
			.put("startMax", today.plusYears(1).toString())
			.put("appointmentMin", iso(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)));
		JSONObject values = new JSONObject()
			.put("born", born != null ? born.toString() : "")
			.put("start", start != null ? start.toString() : "")
			.put("appointment", iso(appointment))
			.put("departure", iso(departure))
			.put("disabled", disabled.toString())
			.put("calendarLocale", calendarLocale);
		return new JSONObject()
			.put("texts", FrameworkTabs.withDropdownTexts(this, texts))
			.put("dateTexts", DatePickerBehavior.texts(this, false))
			.put("dateTimeTexts", DatePickerBehavior.texts(this, true))
			.put("language", getLocale().toLanguageTag())
			.put("locales", locales)
			.put("bounds", bounds)
			.put("values", values);
	}

	private static String iso(LocalDateTime value)
	{
		return value != null ? DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm").format(value) : "";
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		DatePickerBehavior.renderHeadItems(response);
		DropdownBehavior.renderHeadItems(response);
		response.render(CssHeaderItem.forReference(
			new CssResourceReference(DatePickerPage.class, "DatePickerPage.css")));
	}

	/**
	 * Shows a locale by its name in the language of the session, {@link #PAGE_LOCALE} as the
	 * page's.
	 */
	private class LocaleRenderer extends ChoiceRenderer<String>
	{
		private static final long serialVersionUID = 1L;

		@Override
		public Object getDisplayValue(String tag)
		{
			return PAGE_LOCALE.equals(tag) ? getString("calendarLocale.page")
				: Locale.forLanguageTag(tag).getDisplayName(getLocale());
		}

		@Override
		public String getIdValue(String tag, int index)
		{
			return tag;
		}
	}

	/**
	 * @return the birthday
	 */
	public LocalDate getBorn()
	{
		return born;
	}

	/**
	 * @param born
	 *            the birthday
	 */
	public void setBorn(LocalDate born)
	{
		this.born = born;
	}

	/**
	 * @return the start of the contract
	 */
	public LocalDate getStart()
	{
		return start;
	}

	/**
	 * @param start
	 *            the start of the contract
	 */
	public void setStart(LocalDate start)
	{
		this.start = start;
	}

	/**
	 * @return the appointment
	 */
	public LocalDateTime getAppointment()
	{
		return appointment;
	}

	/**
	 * @param appointment
	 *            the appointment
	 */
	public void setAppointment(LocalDateTime appointment)
	{
		this.appointment = appointment;
	}

	/**
	 * @return the departure
	 */
	public LocalDateTime getDeparture()
	{
		return departure;
	}

	/**
	 * @param departure
	 *            the departure
	 */
	public void setDeparture(LocalDateTime departure)
	{
		this.departure = departure;
	}

	/**
	 * @return the date of the disabled picker
	 */
	public LocalDate getDisabled()
	{
		return disabled;
	}

	/**
	 * @param disabled
	 *            the date of the disabled picker
	 */
	public void setDisabled(LocalDate disabled)
	{
		this.disabled = disabled;
	}

	/**
	 * @return the locale of the calendars, {@code page} for the session's
	 */
	public String getCalendarLocale()
	{
		return calendarLocale;
	}

	/**
	 * @param calendarLocale
	 *            the locale of the calendars, {@code page} for the session's
	 */
	public void setCalendarLocale(String calendarLocale)
	{
		this.calendarLocale = calendarLocale;
	}
}
