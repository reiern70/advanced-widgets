/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo;

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

import com.advanced.widgets.demo.progress.ProgressBarPage;
import com.advanced.widgets.demo.theme.ThemeEditorPage;
import com.advanced.widgets.theme.Theme;
import com.advanced.widgets.theme.ThemeEditorPanel;

/**
 * The demos of the components show them in a theme the user chooses.
 */
class ThemedDemosTest extends WicketTestCase
{
	@Override
	protected WebApplication newApplication()
	{
		return new WicketApplication();
	}

	@ParameterizedTest
	@ValueSource(classes = { ProgressBarPage.class, ThemeEditorPage.class })
	void theThemeCanBeChosen(Class<? extends Page> page)
	{
		tester.startPage(page);
		String themed = page == ThemeEditorPage.class ? "preview" : "themed";
		assertTrue(classOf(themed).contains(Theme.DEFAULT.getCssClass()), page.getSimpleName());

		choose("theme", Theme.DARK.ordinal());

		tester.assertComponentOnAjaxResponse(themed);
		tester.assertComponentOnAjaxResponse("explanation");
		assertTrue(classOf(themed).contains(Theme.DARK.getCssClass()), page.getSimpleName());
	}

	@Test
	void theThemeEditorListsEveryProperty()
	{
		tester.startPage(ThemeEditorPage.class);

		String response = tester.getLastResponseAsString();
		for (String property : ThemeEditorPanel.PROPERTIES)
		{
			List<TagTester> fields = TagTester.createTagsByAttribute(response,
				"data-theme-property", "--wicket-theme-" + property, false);
			assertEquals(1, fields.size(), property);
		}
		assertEquals("color", TagTester.createTagByAttribute(response, "data-theme-property",
			"--wicket-theme-primary").getAttribute("type"));
		tester.assertContains("Wicket.ThemeEditor.init");
	}

	@Test
	void theThemeEditorReloadsAfterTheThemeChanged()
	{
		tester.startPage(ThemeEditorPage.class);

		choose("theme", Theme.DARK.ordinal());

		tester.assertContains("Wicket.ThemeEditor.load");
	}

	private String classOf(String path)
	{
		return tester.getTagById(tester.getComponentFromLastRenderedPage(path).getMarkupId())
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
