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
package com.antiliacode.widgets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.breadcrumb.Breadcrumb;
import com.antiliacode.widgets.codeeditor.CodeEditor;
import com.antiliacode.widgets.codeeditor.MarkdownPreview;
import com.antiliacode.widgets.datepicker.DatePicker;
import com.antiliacode.widgets.desktop.Desktop;
import com.antiliacode.widgets.dropdown.Dropdown;
import com.antiliacode.widgets.menu.Menu;
import com.antiliacode.widgets.notification.Notification;
import com.antiliacode.widgets.notification.Notifications;
import com.antiliacode.widgets.popover.Popover;
import com.antiliacode.widgets.richtext.RichTextEditor;
import com.antiliacode.widgets.tabs.Tab;
import com.antiliacode.widgets.tabs.TabbedPanel;

/**
 * Values with markup in them, written into the inline scripts of the components, leave every
 * script of the page ending where it should.
 */
class InlineScriptsTest extends WicketTestCase
{
	/** A value that, written as it is, would turn the rest of the page into script. */
	private static final String VALUE = "<!--<script>";

	private static final String ESCAPED = ScriptJson.escape(VALUE);

	private static final Pattern SCRIPT = Pattern.compile("<script\\b[^>]*>(.*?)</script",
		Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	@Test
	void escapesTheValuesOfTheScriptsOfAPage()
	{
		tester.startPage(new TestPage());

		String page = tester.getLastResponseAsString();
		List<String> scripts = blocks(SCRIPT, page);
		assertCleanScripts(scripts, page);
		// each component wrote the value, escaped
		for (String component : List.of("Wicket.Notifications.show(", "Wicket.CodeEditor.attach(",
			"Wicket.MarkdownPreview.attach(", "Wicket.RichTextEditor.attach(", "Wicket.Menu.attach(",
			"Wicket.Tabs.attach(", "Wicket.Breadcrumb.attach(", "Wicket.Desktop.attach(",
			"Wicket.Dropdown.attach(", "Wicket.DatePicker.attach("))
		{
			String script = scripts.stream().filter(each -> each.contains(component)).findFirst()
				.orElseThrow(() -> new AssertionError("no script calls " + component));
			// the call, up to the next one of the script
			String call = script.substring(script.indexOf(component) + component.length());
			int next = call.indexOf("Wicket.");
			call = next < 0 ? call : call.substring(0, next);
			assertTrue(call.contains(ESCAPED), component + " writes the value: " + call);
		}
	}

	@Test
	void escapesTheValuesOfTheScriptsOfAnAjaxResponse()
	{
		TestPage page = tester.startPage(new TestPage());

		tester.clickLink(page.act);

		// the evaluations of an Ajax response hold their scripts in script elements
		String response = tester.getLastResponseAsString();
		List<String> scripts = blocks(SCRIPT, response);
		assertCleanScripts(scripts, response);
		for (String component : List.of("Wicket.Notifications.show(", "Wicket.CodeEditor.of(",
			"Wicket.RichTextEditor.of(", "Wicket.Popover.show("))
		{
			assertTrue(scripts.stream().anyMatch(each -> each.contains(component) &&
				each.contains(ESCAPED)), component + " writes the value: " + response);
		}
	}

	/**
	 * Without {@code <!--} in it, a script cannot enter the escaped states of the HTML parser, so it
	 * ends at the first {@code </script}, where the pattern ends it too.
	 */
	private static void assertCleanScripts(List<String> scripts, String page)
	{
		assertFalse(scripts.isEmpty(), page);
		for (String script : scripts)
		{
			assertFalse(script.contains("<!--"), script);
			assertFalse(script.toLowerCase(Locale.ROOT).contains("<script"), script);
		}
		assertFalse(page.contains(VALUE), "the value as it is: " + page);
	}

	private static List<String> blocks(Pattern pattern, String text)
	{
		List<String> blocks = new ArrayList<>();
		Matcher matcher = pattern.matcher(text);
		while (matcher.find())
		{
			blocks.add(matcher.group(1));
		}
		return blocks;
	}

	private static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		final Notifications notifications = new Notifications("notifications");

		final CodeEditor code = new CodeEditor("code", Model.of(VALUE));

		final RichTextEditor text = new RichTextEditor("text", Model.of(VALUE));

		final AjaxLink<Void> act;

		TestPage()
		{
			add(notifications);
			notifications.show(Notification.error(VALUE).setTitle(VALUE));
			Form<Void> form = new Form<>("form");
			add(form);
			form.add(code.setSavedModel(Model.of(VALUE)).setPlaceholder(Model.of(VALUE))
				.setLabel(Model.of(VALUE)));
			form.add(text.setSavedModel(Model.of(VALUE)).setPlaceholder(Model.of(VALUE))
				.setLabel(Model.of(VALUE)));
			Dropdown<String> dropdown = new Dropdown<>("dropdown", Model.of(), List.of(VALUE));
			dropdown.getDropdownBehavior().setPlaceholder(Model.of(VALUE));
			DatePicker date = new DatePicker("date", Model.of(LocalDate.of(2026, 10, 29)));
			date.getDatePickerBehavior().setPlaceholder(Model.of(VALUE));
			form.add(dropdown, date);
			add(new MarkdownPreview("preview", Model.of(VALUE)).setLabel(Model.of(VALUE))
				.setPlaceholder(Model.of(VALUE)));
			add(new Menu("menu", List.of()).setLabel(Model.of(VALUE)));
			add(new Breadcrumb("breadcrumb", List.of()).setLabel(Model.of(VALUE)));
			add(new TabbedPanel("tabs").setLabel(Model.of(VALUE)).addTab(new Tab("one",
				Model.of(VALUE))
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected Component newPanel(String id)
				{
					return new Label(id, VALUE);
				}
			}));
			Desktop desktop = new Desktop("desktop");
			desktop.addPane(VALUE);
			add(desktop);
			act = new AjaxLink<>("act")
			{
				private static final long serialVersionUID = 1L;

				@Override
				public void onClick(AjaxRequestTarget target)
				{
					notifications.show(Notification.info(VALUE).setTitle(VALUE));
					code.markSaved(target);
					text.markSaved(target);
					new Popover(VALUE).setTitle(VALUE).show(target, this);
				}
			};
			add(act.setOutputMarkupId(true));
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body>" +
				"<div wicket:id=\"notifications\"></div><form wicket:id=\"form\">" +
				"<textarea wicket:id=\"code\"></textarea><textarea wicket:id=\"text\"></textarea>" +
				"<select wicket:id=\"dropdown\"></select><input type=\"text\" wicket:id=\"date\"/>" +
				"</form><div wicket:id=\"preview\"></div><nav wicket:id=\"menu\"></nav>" +
				"<div wicket:id=\"breadcrumb\"></div><div wicket:id=\"tabs\"></div>" +
				"<div wicket:id=\"desktop\"></div><a wicket:id=\"act\">Act</a></body></html>");
		}
	}
}
