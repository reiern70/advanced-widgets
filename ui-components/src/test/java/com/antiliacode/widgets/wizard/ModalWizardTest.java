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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.apache.wicket.util.visit.IVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.modal.Modal;
import com.antiliacode.widgets.modal.ModalCloseButton;
import com.antiliacode.widgets.theme.Look;
import com.antiliacode.widgets.theme.Looks;
import com.antiliacode.widgets.wizard.WizardTest.Person;

/**
 * The wizard of {@link WizardTest}, its name, kind, company and summary, in a {@link Modal}.
 */
class ModalWizardTest extends WicketTestCase
{
	private static final String FORM = "modal:overlay:dialog:content:form";

	/** What the wizard was told: finished, cancelled. */
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

	/**
	 * A wizard in a dialog telling {@link #EVENTS} what happens, closing the dialog as by default
	 * unless told to keep it open.
	 */
	static class TestWizard extends ModalWizard<Person>
	{
		private static final long serialVersionUID = 1L;

		boolean keepOpen;

		TestWizard(IWizardStep<Person> first)
		{
			super(LoadableDetachableModel.of(ModalWizardTest::person), first);
		}

		@Override
		protected void onFinish(AjaxRequestTarget target)
		{
			EVENTS.add("finish " + getModelObject().name);
			if (!keepOpen)
			{
				super.onFinish(target);
			}
		}

		@Override
		protected void onCancel(AjaxRequestTarget target)
		{
			EVENTS.add("cancel");
			super.onCancel(target);
		}
	}

	private TestPage page;

	private TestWizard open(IWizardStep<Person> first)
	{
		page = tester.startPage(new TestPage());
		TestWizard wizard = new TestWizard(first);
		page.modal.open(wizard, null);
		tester.startPage(page);
		return wizard;
	}

	/**
	 * Renders the page again, after an Ajax request, to look at its markup.
	 */
	private void render()
	{
		tester.startPage(tester.getLastRenderedPage());
	}

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

	private void type(String value, String kind)
	{
		FormTester form = tester.newFormTester(FORM, false);
		form.setValue("body:content:field", value);
		click(kind);
	}

	/** The user asks to close the dialog: Escape, its close button, or Cancel. */
	private void askToClose()
	{
		tester.executeBehavior(page.modal.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));
	}

	@Test
	void laysTheWizardOutInThePartsOfTheDialog()
	{
		TestWizard wizard = open(WizardTest.name());

		assertSame(wizard, page.modal.getContent());
		TagTester form = tester.getTagByWicketId("form");
		assertEquals("form", form.getName());
		assertTrue(classOf(form.getAttribute("class")).contains("modal-dialog-form"));
		assertFalse(classOf(form.getAttribute("class")).contains("antilia-wizard"),
			"the dialog is the box, not the wizard");
		assertNull(form.getAttribute(ModalWizard.MODIFIED_ATTRIBUTE));

		TagTester header = tester.getTagByWicketId("header");
		assertTrue(classOf(header.getAttribute("class")).contains("modal-dialog-header"));
		TagTester title = header.getChild("wicket:id", "title");
		assertTrue(classOf(title.getAttribute("class")).contains("modal-dialog-title"));
		assertEquals("Name", title.getValue());
		assertEquals("1", title.getAttribute("aria-level"));
		TagTester close = header.getChild("wicket:id", "close");
		assertTrue(classOf(close.getAttribute("class")).contains("modal-dialog-close"));
		assertEquals("Close", close.getAttribute("aria-label"));

		TagTester scroller = tester.getTagByWicketId("scroller");
		assertTrue(classOf(scroller.getAttribute("class")).contains("antilia-scrollbar"));
		TagTester body = scroller.getChild("wicket:id", "body");
		assertTrue(classOf(body.getAttribute("class")).contains("modal-dialog-body"));
		assertEquals(title.getAttribute("id"), body.getAttribute("aria-labelledby"));

		TagTester footer = tester.getTagByWicketId("buttonsBottom");
		assertTrue(classOf(footer.getAttribute("class")).contains("modal-dialog-footer"));
		assertNull(tester.getTagByWicketId("buttonsTop"));
		assertNotNull(buttonTag("next"));

		TagTester cancel = buttonTag("cancel");
		assertNotNull(cancel.getAttribute(ModalCloseButton.CLOSE_ATTRIBUTE),
			"Cancel closes the dialog as its close button does");
		tester.assertContains("antilia-wizard.*\\.css");
		tester.assertContains("Wicket\\.Modal\\.attach\\(\"" + page.modal.getOverlayMarkupId());
	}

	@Test
	void movesThroughTheStepsInTheDialog()
	{
		TestWizard wizard = open(WizardTest.name());

		type("Ann", "next");

		assertEquals("Kind", wizard.getCurrentStep().toString());
		assertTrue(page.modal.isOpen());
		tester.assertComponentOnAjaxResponse(wizard);
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("Wicket.Modal.refresh(\"" +
			page.modal.getOverlayMarkupId() + "\")"), "the new body gets its scroll bars");
		render();
		assertEquals("true", tester.getTagByWicketId("form")
			.getAttribute(ModalWizard.MODIFIED_ATTRIBUTE), "what the name step had is held");
		assertEquals("Kind of Ann", tester.getTagByWicketId("title").getValue());

		click("previous");

		assertEquals("Name", wizard.getCurrentStep().toString());
		render();
		assertNull(tester.getTagByWicketId("form").getAttribute(ModalWizard.MODIFIED_ATTRIBUTE));
	}

	@Test
	void aFieldNotValidKeepsTheStep()
	{
		TestWizard wizard = open(WizardTest.name());

		type("", "next");

		assertEquals("Name", wizard.getCurrentStep().toString());
		assertFalse(tester.getMessages(FeedbackMessage.ERROR).isEmpty());
		assertTrue(page.modal.isOpen());
	}

	@Test
	void finishClosesTheDialog()
	{
		open(WizardTest.name());
		type("Ann", "next");
		type("person", "next");

		render();
		click("finish");

		assertEquals(List.of("finish Ann"), EVENTS);
		assertFalse(page.modal.isOpen());
		tester.assertComponentOnAjaxResponse(page.modal);
	}

	@Test
	void finishNotCallingTheDefaultKeepsTheDialogOpen()
	{
		TestWizard wizard = open(WizardTest.name());
		wizard.keepOpen = true;
		type("Ann", "next");
		type("person", "next");

		render();
		click("finish");

		assertEquals(List.of("finish Ann"), EVENTS);
		assertTrue(page.modal.isOpen());
	}

	@Test
	void askingToCloseTheDialogCancels()
	{
		open(WizardTest.name());
		type("Ann", "next");

		askToClose();

		assertEquals(List.of("cancel"), EVENTS);
		assertFalse(page.modal.isOpen());
		assertFalse(page.modal.getContent() instanceof ModalWizard<?>, "the wizard is removed");
	}

	@Test
	void theDialogMayRefuseToClose()
	{
		open(WizardTest.name());
		page.closable = false;

		askToClose();

		assertTrue(EVENTS.isEmpty(), "the wizard is not cancelled");
		assertTrue(page.modal.isOpen());
	}

	@Test
	void theButtonsMayBeAboveTheBody()
	{
		TestWizard wizard = open(WizardTest.name());
		wizard.setButtonsPosition(WizardButtonsPosition.TOP);

		render();

		assertNull(tester.getTagByWicketId("buttonsBottom"), "no footer");
		TagTester top = tester.getTagByWicketId("buttonsTop");
		assertTrue(classOf(top.getAttribute("class")).contains("antilia-wizard-buttons-top"));
		assertNotNull(buttonTag("next"));
	}

	@Test
	void inBootstrapsLookTheWizardHasTheClassesOfTheDialog()
	{
		Looks.setSessionLook(Look.BOOTSTRAP);
		open(WizardTest.name());

		List<String> form = classOf(tester.getTagByWicketId("form").getAttribute("class"));
		assertFalse(form.contains("card"), "the dialog is the box");
		List<String> header = classOf(tester.getTagByWicketId("header").getAttribute("class"));
		assertTrue(header.contains("modal-header"));
		assertFalse(header.contains("card-header"));
		List<String> title = classOf(tester.getTagByWicketId("title").getAttribute("class"));
		assertTrue(title.contains("modal-title"));
		assertFalse(title.contains("card-title"));
		List<String> body = classOf(tester.getTagByWicketId("body").getAttribute("class"));
		assertTrue(body.contains("modal-body"));
		assertFalse(body.contains("card-body"));
		List<String> footer = classOf(tester.getTagByWicketId("buttonsBottom")
			.getAttribute("class"));
		assertTrue(footer.contains("modal-footer"));
		assertFalse(footer.contains("card-footer"));
		assertTrue(classOf(tester.getTagByWicketId("close").getAttribute("class"))
			.contains("btn-close"));
	}

	@Test
	void aPlainWizardKeepsItsOwnLook()
	{
		Looks.setSessionLook(Look.BOOTSTRAP);
		tester.startPage(new WizardTest.TestPage(new Wizard<>("wizard",
			LoadableDetachableModel.of(ModalWizardTest::person), WizardTest.name())));

		assertTrue(classOf(tester.getTagByWicketId("form").getAttribute("class"))
			.contains("card"));
		assertTrue(classOf(tester.getTagByWicketId("header").getAttribute("class"))
			.contains("card-header"));
		assertNull(buttonTag("cancel").getAttribute(ModalCloseButton.CLOSE_ATTRIBUTE));
	}

	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		final Modal modal;

		boolean closable = true;

		TestPage()
		{
			modal = new Modal("modal")
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected boolean canClose(AjaxRequestTarget target)
				{
					return closable;
				}
			};
			add(modal);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body>" +
				"<div wicket:id=\"modal\"></div></body></html>");
		}
	}
}
