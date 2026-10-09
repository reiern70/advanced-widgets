/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.apache.wicket.Page;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.antilia.widgets.demo.progress.ProgressBarPage;
import com.antilia.widgets.demo.theme.ThemeEditorPage;
import com.antilia.widgets.theme.Theme;
import com.antilia.widgets.theme.ThemeEditorPanel;

/**
 * Every page is in the theme the user chooses with the floating button.
 */
class ThemedDemosTest extends WicketTestCase
{
	@Override
	protected WebApplication newApplication()
	{
		return new WicketApplication();
	}

	@ParameterizedTest
	@ValueSource(classes = { HomePage.class, ProgressBarPage.class, ThemeEditorPage.class })
	void theThemeChosenAppliesToTheWholePage(Class<? extends Page> page)
	{
		tester.startPage(page);
		assertTrue(bodyClass().contains(Theme.DEFAULT.getCssClass()), page.getSimpleName());

		choose("settings:theme", Theme.DARK.ordinal());

		assertEquals(Theme.DARK, DemoSession.get().getTheme());
		tester.startPage(page);
		assertTrue(bodyClass().contains(Theme.DARK.getCssClass()), page.getSimpleName());
	}

	@Test
	void theThemeEditorListsEveryProperty()
	{
		tester.startPage(ThemeEditorPage.class);

		String response = tester.getLastResponseAsString();
		for (String property : ThemeEditorPanel.PROPERTIES)
		{
			List<TagTester> fields = TagTester.createTagsByAttribute(response,
				"data-theme-property", "--antilia-theme-" + property, false);
			assertEquals(1, fields.size(), property);
		}
		assertEquals("color", TagTester.createTagByAttribute(response, "data-theme-property",
			"--antilia-theme-primary").getAttribute("type"));
		tester.assertContains("Wicket.ThemeEditor.init");
	}

	private String bodyClass()
	{
		return TagTester.createTagByName(tester.getLastResponseAsString(), "body")
			.getAttribute("class");
	}

	@SuppressWarnings("unchecked")
	private void choose(String path, int index)
	{
		DropDownChoice<Object> choice = (DropDownChoice<Object>)tester
			.getComponentFromLastRenderedPage(path);
		tester.getRequest().getPostParameters().setParameterValue(choice.getInputName(),
			choice.getChoiceRenderer().getIdValue(choice.getChoices().get(index), index));
		tester.executeAjaxEvent(choice, "change");
	}
}
