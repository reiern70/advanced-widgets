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

import java.time.temporal.WeekFields;
import java.util.Locale;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.model.IComponentAssignedModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;

import com.github.openjson.JSONObject;

/**
 * Turns an {@code <input>} holding a date as ISO text, 2026-10-09, or a date and time,
 * 2026-10-09T14:30, into a date picker that takes the colors of the
 * {@link com.antiliacode.widgets.theme.Theme theme} it is in.
 * <p>
 * Everything happens in the browser: {@code Wicket.DatePicker} from {@link #JS} hides the input and
 * puts a text field, a button and a calendar next to it. The text field shows the date the way the
 * {@link #setLocale(Locale) locale} writes it, 09.10.2026 in Germany and 10/09/2026 in the United
 * States, and reads it back typed so; the calendar names the months and days in the language of the
 * locale and starts the week on its first day, which this behavior takes from {@link WeekFields}
 * when the locale has a country; for a language alone, such as German, the script takes the first
 * day of its likeliest country (Monday for German and Spanish, where Java answers Sunday), as the
 * React and Svelte pickers do. A time has the clock of the locale. The texts of the buttons come
 * from the bundles, in the language of the session. The input stays in the page and holds the value
 * as ISO text: a form posts it as always, and a change fires the input's {@code change} event, so
 * an {@code AjaxFormComponentUpdatingBehavior} on it is notified. Text that is no date is posted as
 * typed, for the converter of the component to refuse.
 * <p>
 * The calendar is used with the mouse, touch or the keyboard: the arrow keys move by a day or a
 * week, Page Up and Page Down by a month, with Shift by a year, Enter chooses and Escape closes; its
 * title shows the months of the year, and then the years, to choose from. The script needs no
 * inline style, so the picker works under a strict Content Security Policy. {@link DatePicker} and
 * {@link DateTimePicker} are the form components with this behavior.
 *
 * @since 11.0.0
 */
public class DatePickerBehavior extends Behavior
{
	private static final long serialVersionUID = 1L;

	/** The script of the date picker, {@code Wicket.DatePicker}. */
	public static final ResourceReference JS = new JavaScriptResourceReference(
		DatePickerBehavior.class, "antilia-date-picker.js");

	/** The style sheet of the date picker. */
	public static final ResourceReference CSS = new CssResourceReference(DatePickerBehavior.class,
		"antilia-date-picker.css");

	private final boolean time;

	private Locale locale;

	private String minimum;

	private String maximum;

	private int minuteStep = 1;

	private IModel<String> placeholder;

	/**
	 * A picker of dates.
	 */
	public DatePickerBehavior()
	{
		this(false);
	}

	/**
	 * @param time
	 *            whether the picker takes a time too
	 */
	public DatePickerBehavior(boolean time)
	{
		this.time = time;
	}

	/**
	 * @return whether the picker takes a time too
	 */
	public boolean isTime()
	{
		return time;
	}

	/**
	 * @return the locale the picker writes and reads dates in, {@code null} for the component's
	 */
	public Locale getLocale()
	{
		return locale;
	}

	/**
	 * @param locale
	 *            the locale the picker writes and reads dates in, names the months and days in and
	 *            starts the week by; {@code null} for the locale of the component
	 * @return {@code this}
	 */
	public DatePickerBehavior setLocale(Locale locale)
	{
		this.locale = locale;
		return this;
	}

	/**
	 * @return the first date or date and time that can be chosen, as ISO text
	 */
	public String getMinimum()
	{
		return minimum;
	}

	/**
	 * @param minimum
	 *            the first date or date and time that can be chosen, as ISO text, {@code null} for
	 *            none
	 * @return {@code this}
	 */
	public DatePickerBehavior setMinimum(String minimum)
	{
		this.minimum = minimum;
		return this;
	}

	/**
	 * @return the last date or date and time that can be chosen, as ISO text
	 */
	public String getMaximum()
	{
		return maximum;
	}

	/**
	 * @param maximum
	 *            the last date or date and time that can be chosen, as ISO text, {@code null} for
	 *            none
	 * @return {@code this}
	 */
	public DatePickerBehavior setMaximum(String maximum)
	{
		this.maximum = maximum;
		return this;
	}

	/**
	 * @return the minutes the minute field steps by
	 */
	public int getMinuteStep()
	{
		return minuteStep;
	}

	/**
	 * @param minuteStep
	 *            the minutes the minute field steps by, and the time chosen with "Now" is rounded
	 *            down to
	 * @return {@code this}
	 */
	public DatePickerBehavior setMinuteStep(int minuteStep)
	{
		this.minuteStep = Math.max(1, Math.min(59, minuteStep));
		return this;
	}

	/**
	 * @return the text of the empty field, {@code null} for the format of the locale
	 */
	public IModel<String> getPlaceholder()
	{
		return placeholder;
	}

	/**
	 * @param placeholder
	 *            the text of the empty field, {@code null} for the format of the locale, written
	 *            with symbols such as dd.mm.yyyy
	 * @return {@code this}
	 */
	public DatePickerBehavior setPlaceholder(IModel<String> placeholder)
	{
		this.placeholder = placeholder;
		return this;
	}

	@Override
	public void bind(Component component)
	{
		super.bind(component);
		component.setOutputMarkupId(true);
	}

	@Override
	public void onComponentTag(Component component, ComponentTag tag)
	{
		super.onComponentTag(component, tag);
		if (!"input".equalsIgnoreCase(tag.getName()))
		{
			throw new WicketRuntimeException(
				"A DatePickerBehavior can only be attached to an <input>, not to <" +
					tag.getName() + "> of " + component);
		}
	}

	@Override
	public void renderHead(Component component, IHeaderResponse response)
	{
		super.renderHead(component, response);
		renderHeadItems(response);
		response.render(OnDomReadyHeaderItem.forScript("Wicket.DatePicker.attach(" +
			JSONObject.quote(component.getMarkupId()) + ", " + newOptions(component) + ");"));
	}

	@Override
	public void detach(Component component)
	{
		super.detach(component);
		if (placeholder != null)
		{
			placeholder.detach();
		}
	}

	/**
	 * @return the locale the picker follows: its own, else the component's
	 */
	protected Locale resolveLocale(Component component)
	{
		return locale != null ? locale : component.getLocale();
	}

	/**
	 * @return the options of the script: the locale, the dates taken and the texts
	 */
	protected JSONObject newOptions(Component component)
	{
		Locale chosen = resolveLocale(component);
		JSONObject options = new JSONObject();
		options.put("locale", chosen.toLanguageTag());
		if (!chosen.getCountry().isEmpty())
		{
			options.put("firstDayOfWeek",
				WeekFields.of(chosen).getFirstDayOfWeek().getValue() % 7);
		}
		options.put("time", time);
		if (minimum != null)
		{
			options.put("min", minimum);
		}
		if (maximum != null)
		{
			options.put("max", maximum);
		}
		if (time)
		{
			options.put("minuteStep", minuteStep);
		}
		if (placeholder != null)
		{
			IModel<String> text = placeholder instanceof IComponentAssignedModel<String> assigned
				? assigned.wrapOnAssignment(component) : placeholder;
			String value = text.getObject();
			text.detach();
			if (value != null)
			{
				options.put("placeholder", value);
			}
		}
		JSONObject texts = texts(component, time);
		for (String name : JSONObject.getNames(texts))
		{
			options.put(name, texts.get(name));
		}
		return options;
	}

	/**
	 * The texts of a date picker in the language of the session, as the options of the script and
	 * of the React and Svelte pickers name them: the symbols of the format, the labels of the
	 * buttons and of the time fields.
	 *
	 * @param component
	 *            the component whose bundles have the texts
	 * @param time
	 *            whether the picker takes a time too
	 * @return the texts, by their names
	 */
	public static JSONObject texts(Component component, boolean time)
	{
		JSONObject texts = new JSONObject();
		texts.put("open", time
			? component.getString("DateTimePicker.open", null, "Choose date and time")
			: component.getString("DatePicker.open", null, "Choose date"));
		text(component, texts, "symbols", "dd mm yyyy hh mm");
		text(component, texts, "previousMonth", "Previous month");
		text(component, texts, "nextMonth", "Next month");
		text(component, texts, "previousYear", "Previous year");
		text(component, texts, "nextYear", "Next year");
		text(component, texts, "previousYears", "Previous years");
		text(component, texts, "nextYears", "Next years");
		text(component, texts, "chooseMonth", "Choose month");
		text(component, texts, "chooseYear", "Choose year");
		text(component, texts, "today", "Today");
		text(component, texts, "now", "Now");
		text(component, texts, "clear", "Clear");
		text(component, texts, "close", "Close");
		text(component, texts, "hour", "Hour");
		text(component, texts, "minute", "Minute");
		return texts;
	}

	private static void text(Component component, JSONObject texts, String name, String fallback)
	{
		texts.put(name, component.getString("DatePicker." + name, null, fallback));
	}

	/**
	 * Renders what the date picker needs in the browser: {@link #JS} and {@link #CSS}.
	 *
	 * @param response
	 *            the header response
	 */
	public static void renderHeadItems(IHeaderResponse response)
	{
		CoreLibrariesContributor.contribute(Application.get(), response);
		response.render(JavaScriptHeaderItem.forReference(JS));
		response.render(CssHeaderItem.forReference(CSS));
	}
}
