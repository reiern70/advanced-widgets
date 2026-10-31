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
package com.antiliacode.widgets.wizard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.serialize.java.JavaSerializer;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.apache.wicket.util.visit.IVisitor;
import org.danekja.java.util.function.serializable.SerializableFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A wizard of four steps filling in a person, which is not serializable: the name (required),
 * the kind, the company (required) only for a company, and the summary, which finishes.
 */
class WizardTest extends WicketTestCase
{
	/** What the wizard of the tests was told: finished, cancelled, steps changed. */
	private static final List<String> EVENTS = new ArrayList<>();

	/** The object the wizard fills in, held here since it is not serializable. */
	private static Person person;

	private static Person person()
	{
		return person;
	}

	@BeforeEach
	void reset()
	{
		EVENTS.clear();
		person = new Person();
	}

	/** Not serializable on purpose: the wizard holds it in a loadable model. */
	static class Person
	{
		String name;

		String kind;

		String company;
	}

	/**
	 * A step with a text field for a property of the person.
	 */
	static class FieldStep implements IWizardStep<Person>
	{
		private static final long serialVersionUID = 1L;

		private final String name;

		private final String property;

		private final boolean required;

		private final SerializableFunction<Person, IWizardStep<Person>> next;

		FieldStep(String name, String property, boolean required,
			SerializableFunction<Person, IWizardStep<Person>> next)
		{
			this.name = name;
			this.property = property;
			this.required = required;
			this.next = next;
		}

		@Override
		public IModel<String> getTitle(IModel<Person> model)
		{
			return () -> model.getObject().name != null ? name + " of " + model.getObject().name :
				name;
		}

		@Override
		public Component newBody(String id, IModel<Person> model)
		{
			return new FieldPanel(id, new PropertyModel<>(model, property), required);
		}

		@Override
		public IWizardStep<Person> getNextStep(IModel<Person> model)
		{
			return next != null ? next.apply(model.getObject()) : null;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	static IWizardStep<Person> summary()
	{
		return new FieldStep("Summary", "name", false, null);
	}

	static IWizardStep<Person> company()
	{
		return new FieldStep("Company", "company", true, person -> summary());
	}

	static IWizardStep<Person> kind()
	{
		return new FieldStep("Kind", "kind", false,
			person -> "company".equals(person.kind) ? company() : summary());
	}

	static IWizardStep<Person> name()
	{
		return new FieldStep("Name", "name", true, person -> kind());
	}

	/**
	 * A panel holding a text field.
	 */
	static class FieldPanel extends Panel implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		FieldPanel(String id, IModel<String> model, boolean required)
		{
			super(id);
			add(new TextField<>("field", model).setRequired(required));
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream(
				"<wicket:panel><input type=\"text\" wicket:id=\"field\"/></wicket:panel>");
		}
	}

	/**
	 * A wizard telling {@link #EVENTS} what happens.
	 */
	static class TestWizard extends Wizard<Person>
	{
		private static final long serialVersionUID = 1L;

		TestWizard(IWizardStep<Person> first)
		{
			super("wizard", LoadableDetachableModel.of(WizardTest::person), first);
		}

		@Override
		protected void onFinish(org.apache.wicket.ajax.AjaxRequestTarget target)
		{
			EVENTS.add("finish " + getModelObject().name);
		}

		@Override
		protected void onCancel(org.apache.wicket.ajax.AjaxRequestTarget target)
		{
			EVENTS.add("cancel");
		}

		@Override
		protected void onStepChanged(org.apache.wicket.ajax.AjaxRequestTarget target,
			IWizardStep<Person> from, IWizardStep<Person> to)
		{
			EVENTS.add(from + " > " + to);
		}
	}

	private TestWizard start(IWizardStep<Person> first)
	{
		TestWizard wizard = new TestWizard(first);
		tester.startPage(new TestPage(wizard));
		return wizard;
	}

	/**
	 * Renders the page again, after an Ajax request, to look at its markup.
	 */
	private void render()
	{
		tester.startPage(tester.getLastRenderedPage());
	}

	/**
	 * @return the tag of a default button, {@code null} when it is not rendered
	 */
	private TagTester buttonTag(String kind)
	{
		List<TagTester> tags = TagTester.createTags(tester.getLastResponseAsString(),
			tag -> "button".equals(tag.getName()) && !tag.isClose() && classOf(tag.getAttribute(
				"class")).contains(WizardButtons.CLASS_PREFIX + kind), false);
		return tags.isEmpty() ? null : tags.get(0);
	}

	private static List<String> classOf(Object css)
	{
		return css == null ? List.of() : List.of(css.toString().split(" "));
	}

	/**
	 * @return the component rendered with the markup id
	 */
	private Component byMarkupId(String markupId)
	{
		Component found = tester.getLastRenderedPage().visitChildren(Component.class,
			(IVisitor<Component, Component>)(component, visit) -> {
				if (markupId.equals(component.getMarkupId(false)))
				{
					visit.stop(component);
				}
			});
		assertNotNull(found, markupId);
		return found;
	}

	private void click(String kind)
	{
		TagTester tag = buttonTag(kind);
		assertNotNull(tag, kind + " is not rendered");
		tester.executeAjaxEvent(byMarkupId(tag.getAttribute("id")), "click");
	}

	/**
	 * Types into the field of the step shown, then clicks a button.
	 */
	private void type(String value, String kind)
	{
		FormTester form = tester.newFormTester("wizard:form", false);
		form.setValue("body:content:field", value);
		click(kind);
	}

	private static boolean disabled(TagTester tag)
	{
		return tag.getAttribute("disabled") != null;
	}

	@Test
	void rendersTheFirstStep()
	{
		TestWizard wizard = start(name());

		TagTester form = tester.getTagByWicketId("form");
		assertEquals("form", form.getName());
		assertTrue(classOf(form.getAttribute("class")).contains("antilia-wizard"));
		TagTester title = tester.getTagByWicketId("title");
		assertEquals("Name", title.getValue());
		assertEquals("heading", title.getAttribute("role"));
		assertEquals("2", title.getAttribute("aria-level"));
		assertEquals("-1", title.getAttribute("tabindex"));
		TagTester body = tester.getTagByWicketId("body");
		assertEquals("group", body.getAttribute("role"));
		assertEquals(title.getAttribute("id"), body.getAttribute("aria-labelledby"));
		tester.assertContains("antilia-wizard.*\\.css");

		assertTrue(disabled(buttonTag("first")), "nothing to go back to");
		assertTrue(disabled(buttonTag("previous")));
		assertFalse(disabled(buttonTag("next")));
		assertFalse(disabled(buttonTag("last")));
		assertFalse(disabled(buttonTag("cancel")));
		assertNull(buttonTag("finish"), "the step has a next one");
		assertEquals("Next", buttonTag("next").getChild("class", "ant-button__label").getValue());
		assertSame(wizard.getFirstStep(), wizard.getCurrentStep());
		assertFalse(wizard.hasPreviousStep());
	}

	@Test
	void nextValidatesTheBodyFirst()
	{
		TestWizard wizard = start(name());

		type("", "next");

		assertEquals("Name", wizard.getCurrentStep().toString(), "the name is required");
		assertFalse(tester.getMessages(FeedbackMessage.ERROR).isEmpty());
		tester.assertComponentOnAjaxResponse(wizard);
		assertTrue(EVENTS.isEmpty());
	}

	@Test
	void nextGoesToTheStepTheStepShownAnswersWithATitleFromTheObject()
	{
		TestWizard wizard = start(name());

		type("Ann", "next");

		assertEquals("Ann", person.name);
		assertEquals("Kind", wizard.getCurrentStep().toString());
		assertEquals(List.of("Name > Kind"), EVENTS);
		tester.assertComponentOnAjaxResponse(wizard);
		render();
		assertEquals("Kind of Ann", tester.getTagByWicketId("title").getValue());
		assertFalse(disabled(buttonTag("previous")));
		assertFalse(disabled(buttonTag("first")));
	}

	@Test
	void theStepsBranchOnWhatTheUserEntered()
	{
		TestWizard wizard = start(name());
		type("Ann", "next");
		render();

		type("company", "next");
		assertEquals("Company", wizard.getCurrentStep().toString());

		render();
		click("previous");
		render();
		type("person", "next");
		assertEquals("Summary", wizard.getCurrentStep().toString());
	}

	@Test
	void previousSubmitsNothing()
	{
		TestWizard wizard = start(name());
		type("Ann", "next");
		render();

		type("company", "previous");

		assertEquals("Name", wizard.getCurrentStep().toString());
		assertNull(person.kind, "what was typed in the step left is lost");
		render();
		assertEquals("Ann", tester.getTagByWicketId("field").getAttribute("value"),
			"the body is made anew from the object");
	}

	@Test
	void lastJumpsToTheLastStepWithoutValidatingThoseBetween()
	{
		TestWizard wizard = start(name());
		person.kind = "company";

		type("Ann", "last");

		assertEquals("Summary", wizard.getCurrentStep().toString());
		assertNull(person.company, "the company, required, was never validated");
		assertEquals(List.of("Name", "Kind", "Company"),
			wizard.getVisitedSteps().stream().map(Object::toString).toList());
		render();
		click("previous");
		assertEquals("Company", wizard.getCurrentStep().toString(),
			"previous goes back through the steps jumped over");
	}

	@Test
	void lastValidatesTheStepShown()
	{
		TestWizard wizard = start(name());

		type("", "last");

		assertEquals("Name", wizard.getCurrentStep().toString());
		assertFalse(tester.getMessages(FeedbackMessage.ERROR).isEmpty());
	}

	@Test
	void firstGoesBackToTheFirstStep()
	{
		TestWizard wizard = start(name());
		type("Ann", "last");
		render();

		click("first");

		assertEquals("Name", wizard.getCurrentStep().toString());
		assertTrue(wizard.getVisitedSteps().isEmpty());
	}

	@Test
	void finishShowsOnTheStepThatFinishes()
	{
		start(name());
		type("Ann", "last");
		render();

		assertNotNull(buttonTag("finish"));
		assertTrue(disabled(buttonTag("next")), "there is no next step");
		assertTrue(disabled(buttonTag("last")));

		type("Ann Smith", "finish");

		assertEquals(List.of("Name > Summary", "finish Ann Smith"), EVENTS);
	}

	@Test
	void cancelSubmitsNothing()
	{
		start(name());

		type("Ann", "cancel");

		assertEquals(List.of("cancel"), EVENTS);
		assertNull(person.name);
	}

	@Test
	void theStepSaysWhichButtonsShowAndWhichMayBeClicked()
	{
		start(new FieldStep("Only", "name", false, person -> summary())
		{
			private static final long serialVersionUID = 1L;

			@Override
			public boolean hasFirst(IModel<Person> model)
			{
				return false;
			}

			@Override
			public boolean hasLast(IModel<Person> model)
			{
				return false;
			}

			@Override
			public boolean isNextEnabled(IModel<Person> model)
			{
				return false;
			}

			@Override
			public boolean canCancel(IModel<Person> model)
			{
				return false;
			}

			@Override
			public boolean isFinish(IModel<Person> model)
			{
				return true;
			}
		});

		assertNull(buttonTag("first"));
		assertNull(buttonTag("last"));
		assertNull(buttonTag("cancel"));
		assertNotNull(buttonTag("previous"));
		assertTrue(disabled(buttonTag("next")));
		assertNotNull(buttonTag("finish"), "the step may finish though it has a next one");
	}

	@Test
	void aStepGivesButtonsOfItsOwn()
	{
		start(new FieldStep("Own", "name", false, null)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public List<IWizardButtonFactory<Person>> getButtons()
			{
				return List.of(IWizardButtonFactory.finish(), IWizardButtonFactory.button(
					Model.of("Shout"), null, (target, wizard) -> EVENTS.add("shout")));
			}
		});

		assertNull(buttonTag("next"));
		assertNotNull(buttonTag("finish"));
		tester.assertContains("Shout");
	}

	@Test
	void theButtonsAreBelowTheBodyOrAboveIt()
	{
		TestWizard wizard = start(name());
		tester.assertVisible("wizard:form:buttonsBottom");
		tester.assertInvisible("wizard:form:buttonsTop");

		wizard.setButtonsPosition(WizardButtonsPosition.TOP);
		render();

		tester.assertVisible("wizard:form:buttonsTop");
		tester.assertInvisible("wizard:form:buttonsBottom");
		assertNotNull(buttonTag("next"));
		assertTrue(tester.getLastResponseAsString().indexOf("antilia-wizard-next") < tester
			.getLastResponseAsString().indexOf("wicket:id=\"body\""), "the bar is above the body");
	}

	@Test
	void theTrailOfTheStepsVisitedIsAboveTheTitleByDefault()
	{
		TestWizard wizard = start(name());
		tester.assertInvisible("wizard:form:header:breadcrumbBefore");
		tester.assertInvisible("wizard:form:header:breadcrumbAfter");

		type("Ann", "next");
		render();

		tester.assertVisible("wizard:form:header:breadcrumbBefore");
		tester.assertInvisible("wizard:form:header:breadcrumbAfter");
		List<TagTester> links = TagTester.createTags(tester.getLastResponseAsString(),
			tag -> !tag.isClose() && classOf(tag.getAttribute("class")).contains(
				"antilia-breadcrumb-link"), false);
		assertEquals(1, links.size(), "the step shown is the last crumb, no link");
		tester.assertContains("aria-current=\"page\"");

		tester.executeAjaxEvent(byMarkupId(links.get(0).getAttribute("id")), "click");

		assertEquals("Name", wizard.getCurrentStep().toString());
	}

	@Test
	void theTrailInPlaceOfTheTitleHidesTheTitleButForScreenReaders()
	{
		TestWizard wizard = start(name());
		wizard.setBreadcrumbPosition(WizardBreadcrumbPosition.IN_TITLE);
		render();
		assertFalse(classOf(tester.getTagByWicketId("title").getAttribute("class")).contains(
			"antilia-wizard-title-hidden"), "no trail on the first step: the title shows");
		tester.assertInvisible("wizard:form:header:breadcrumbBefore");

		type("Ann", "next");
		render();

		assertTrue(classOf(tester.getTagByWicketId("title").getAttribute("class")).contains(
			"antilia-wizard-title-hidden"));
		assertTrue(classOf(tester.getTagByWicketId("breadcrumbBefore").getAttribute("class"))
			.contains("antilia-wizard-breadcrumb-in-title"));

		wizard.setBreadcrumbPosition(WizardBreadcrumbPosition.BELOW_TITLE);
		render();
		tester.assertInvisible("wizard:form:header:breadcrumbBefore");
		tester.assertVisible("wizard:form:header:breadcrumbAfter");

		wizard.setBreadcrumbPosition(WizardBreadcrumbPosition.NONE);
		render();
		tester.assertInvisible("wizard:form:header:breadcrumbBefore");
		tester.assertInvisible("wizard:form:header:breadcrumbAfter");
		assertFalse(classOf(tester.getTagByWicketId("title").getAttribute("class")).contains(
			"antilia-wizard-title-hidden"));
	}

	@Test
	void theObjectNeedsNotBeSerializable()
	{
		start(name());
		type("Ann", "next");
		render();

		byte[] page = new JavaSerializer("wizard").serialize(tester.getLastRenderedPage());

		assertNotNull(page, "the page serializes, the person being in a loadable model");
	}

	@Test
	void stepsLeadingToEachOtherStopLast()
	{
		IWizardStep<Person> loop = new FieldStep("Loop", "name", false, null)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public IWizardStep<Person> getNextStep(IModel<Person> model)
			{
				return this;
			}
		};
		TestWizard wizard = start(loop);

		assertThrows(IllegalStateException.class, () -> wizard.last(null));
	}

	@Test
	void theBodyMustHaveTheIdItWasGiven()
	{
		IWizardStep<Person> wrong = new FieldStep("Wrong", "name", false, null)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public Component newBody(String id, IModel<Person> model)
			{
				return super.newBody("other", model);
			}
		};

		RuntimeException e = assertThrows(RuntimeException.class, () -> start(wrong));
		Throwable cause = e;
		while (cause != null && !(cause instanceof IllegalStateException))
		{
			cause = cause.getCause();
		}
		assertNotNull(cause, "the request fails with the IllegalStateException of the wizard");
	}

	/**
	 * A page with the wizard.
	 */
	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		TestPage(Wizard<?> wizard)
		{
			add(wizard);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body>" +
				"<div wicket:id=\"wizard\"></div></body></html>");
		}
	}
}
