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
package com.antiliacode.widgets.demo.wizard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.apache.wicket.Component;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.util.file.File;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.util.visit.IVisitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.demo.wizard.ImageCardPanel.ImageCard;
import com.antiliacode.widgets.demo.wizard.ImageCardPanel.Way;
import com.antiliacode.widgets.demo.wizard.MosquitoKeyPanel.Identification;
import com.antiliacode.widgets.wizard.Wizard;
import com.antiliacode.widgets.wizard.WizardBreadcrumbPosition;
import com.antiliacode.widgets.wizard.WizardButtonsPosition;

/**
 * Checks {@link WizardPage}: the key of mosquitoes branching on the answers, back and forth, the
 * image card uploaded by Ajax then given a text the button of its step suggests, the settings
 * moving the buttons and the trail, and the texts in German.
 */
class WizardPageTest
{
	private static final String KEY = "themed:key:wizard";

	private static final String CARD = "themed:card:wizard";

	@TempDir
	Path folder;

	private WicketTester tester;

	@BeforeEach
	void start()
	{
		tester = new WicketTester(new WicketApplication());
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	@SuppressWarnings("unchecked")
	private <T> Wizard<T> wizard(String path)
	{
		return (Wizard<T>)tester.getComponentFromLastRenderedPage(path);
	}

	/**
	 * Renders the page again, after an Ajax request, to look at its markup.
	 */
	private void render()
	{
		tester.startPage(tester.getLastRenderedPage());
	}

	private String title(String path)
	{
		return tester.getComponentFromLastRenderedPage(path + ":form:header:title")
			.getDefaultModelObjectAsString();
	}

	/**
	 * @return the tag of a default button of a wizard, by its kind, {@code null} when it is not
	 *         rendered
	 */
	private TagTester button(String path, String kind)
	{
		Wizard<?> wizard = wizard(path);
		return TagTester.createTags(tester.getLastResponseAsString(),
			tag -> "button".equals(tag.getName()) && !tag.isClose() &&
				tag.getAttribute("class") != null &&
				List.of(tag.getAttribute("class").toString().split(" "))
					.contains("antilia-wizard-" + kind), false)
			.stream().filter(tag -> wizard.contains(byMarkupId(tag.getAttribute("id")), true))
			.findFirst().orElse(null);
	}

	private Component byMarkupId(String markupId)
	{
		return tester.getLastRenderedPage().visitChildren(Component.class,
			(IVisitor<Component, Component>)(component, visit) -> {
				if (markupId.equals(component.getMarkupId(false)))
				{
					visit.stop(component);
				}
			});
	}

	/**
	 * Clicks a button of a wizard, then renders the page again.
	 */
	private void click(String path, String kind)
	{
		TagTester tag = button(path, kind);
		assertNotNull(tag, kind + " is not rendered");
		tester.executeAjaxEvent(byMarkupId(tag.getAttribute("id")), "click");
		render();
	}

	/**
	 * Answers the couplet shown with its first lead ("a") or its second one ("b"), then clicks Next.
	 */
	private void answer(String lead)
	{
		FormTester form = tester.newFormTester(KEY + ":form", false);
		form.select("body:content:answer", "a".equals(lead) ? 0 : 1);
		click(KEY, "next");
	}

	private File png(String name, int width, int height) throws IOException
	{
		File file = new File(Files.createDirectories(folder).resolve(name).toFile());
		ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", file);
		return file;
	}

	@Test
	void theKeyBranchesOnTheAnswersUpToAMosquito()
	{
		tester.startPage(WizardPage.class);
		assertEquals("Couplet 1", title(KEY));
		assertNull(button(KEY, "finish"), "no mosquito yet");

		click(KEY, "next");
		assertEquals("Couplet 1", title(KEY), "an answer is required");
		assertFalse(tester.getMessages(FeedbackMessage.ERROR).isEmpty());

		answer("b");
		assertEquals("Couplet 2", title(KEY));
		answer("a");
		assertEquals("Couplet 3", title(KEY));
		answer("b");

		assertEquals("Aedes albopictus", title(KEY));
		tester.assertLabel(KEY + ":form:body:content:route",
			"Your answers: 1b → 2a → 3b");
		assertNull(button(KEY, "next"), "a mosquito is the end of the key");
		assertNull(button(KEY, "last"));
		assertNotNull(button(KEY, "finish"));

		click(KEY, "finish");

		assertEquals(List.of("Identified as Aedes albopictus."), tester.getMessages(
			FeedbackMessage.SUCCESS).stream().map(Object::toString).toList());
		assertEquals("Couplet 1", title(KEY), "the key starts again");
		Wizard<Identification> key = wizard(KEY);
		assertTrue(key.getModelObject().answers.isEmpty());
		assertTrue(key.getVisitedSteps().isEmpty());
	}

	@Test
	void previousGoesBackToTheAnswerGivenAndAnotherOneBranchesElsewhere()
	{
		tester.startPage(WizardPage.class);
		answer("b");
		answer("b");
		assertEquals("Couplet 4", title(KEY));

		click(KEY, "previous");
		assertEquals("Couplet 2", title(KEY));
		click(KEY, "previous");
		assertEquals("Couplet 1", title(KEY));
		assertEquals("b", ((FormComponent<?>)tester.getComponentFromLastRenderedPage(KEY +
			":form:body:content:answer")).getValue(), "the answer given before");

		answer("a");

		assertEquals("Anopheles", title(KEY));
		tester.assertLabel(KEY + ":form:body:content:route", "Your answers: 1a");
	}

	@Test
	void firstAndCancelStartTheKeyAgain()
	{
		tester.startPage(WizardPage.class);
		answer("b");
		answer("a");

		click(KEY, "first");
		assertEquals("Couplet 1", title(KEY));
		assertTrue(wizard(KEY).getVisitedSteps().isEmpty());

		answer("a");
		click(KEY, "cancel");

		assertEquals(List.of("The identification was cancelled."), tester.getMessages(
			FeedbackMessage.INFO).stream().map(Object::toString).toList());
		assertEquals("Couplet 1", title(KEY));
		Wizard<Identification> key = wizard(KEY);
		assertTrue(key.getModelObject().answers.isEmpty());
	}

	@Test
	void theCardUploadsAnImageThenTakesTheTextItsStepSuggests() throws IOException
	{
		tester.startPage(WizardPage.class);
		assertEquals("Upload an image", title(CARD));
		assertTrue(button(CARD, "last").getAttribute("disabled") != null,
			"no way chosen yet: the steps go no further than the next one");

		FormTester form = tester.newFormTester(CARD + ":form", false);
		form.setFile("body:content:file", png("sunset_beach.png", 40, 30), "image/png");
		click(CARD, "next");

		Wizard<ImageCard> card = wizard(CARD);
		assertEquals("sunset_beach.png", card.getModelObject().fileName);
		assertEquals("image/png", card.getModelObject().contentType);
		assertEquals(40, card.getModelObject().width);
		assertEquals("What to do with sunset_beach.png", title(CARD));

		form = tester.newFormTester(CARD + ":form", false);
		form.select("body:content:choice", Way.CAPTION.ordinal());
		click(CARD, "next");
		assertEquals("A text over sunset_beach.png", title(CARD));

		TagTester suggest = TagTester.createTags(tester.getLastResponseAsString(),
			tag -> "button".equals(tag.getName()) && !tag.isClose(), false).stream()
			.filter(tag -> tag.getValue().contains("Suggest a text")).findFirst().orElse(null);
		assertNotNull(suggest, "the step has a button of its own");
		tester.executeAjaxEvent(byMarkupId(suggest.getAttribute("id")), "click");
		render();
		assertTrue(card.getModelObject().caption.startsWith("sunset beach · 40 × 30 px · "),
			card.getModelObject().caption);

		click(CARD, "next");
		assertEquals("Preview of sunset_beach.png", title(CARD));
		tester.assertLabel(CARD + ":form:body:content:text", card.getModelObject().caption);

		click(CARD, "finish");

		assertEquals(List.of("The card of sunset_beach.png is ready."), tester.getMessages(
			FeedbackMessage.SUCCESS).stream().map(Object::toString).toList());
		assertEquals("Upload an image", title(CARD), "a new card");
		assertNull(card.getModelObject().image, "a new object");
	}

	@Test
	void theCardRefusesAFileThatIsNoImage() throws IOException
	{
		tester.startPage(WizardPage.class);
		File text = new File(Files.writeString(Files.createDirectories(folder).resolve(
			"notes.png"), "no image").toFile());

		FormTester form = tester.newFormTester(CARD + ":form", false);
		form.setFile("body:content:file", text, "image/png");
		click(CARD, "next");

		assertEquals("Upload an image", title(CARD));
		assertEquals(List.of("The file is no PNG, JPEG, GIF or WebP image."), tester.getMessages(
			FeedbackMessage.ERROR).stream().map(Object::toString).toList());
		Wizard<ImageCard> card = wizard(CARD);
		assertNull(card.getModelObject().image);
	}

	@Test
	void theSettingsMoveTheButtonsAndTheTrailOfBothWizards()
	{
		tester.startPage(WizardPage.class);
		FormTester settings = tester.newFormTester("themed:settings");
		settings.select("buttonsPosition", WizardButtonsPosition.TOP.ordinal());
		tester.executeAjaxEvent("themed:settings:buttonsPosition", "change");
		tester.assertComponentOnAjaxResponse(KEY);
		tester.assertComponentOnAjaxResponse(CARD);
		settings = tester.newFormTester("themed:settings");
		settings.select("breadcrumbPosition", WizardBreadcrumbPosition.NONE.ordinal());
		tester.executeAjaxEvent("themed:settings:breadcrumbPosition", "change");

		for (String path : List.of(KEY, CARD))
		{
			Wizard<?> wizard = wizard(path);
			assertEquals(WizardButtonsPosition.TOP, wizard.getButtonsPosition());
			assertEquals(WizardBreadcrumbPosition.NONE, wizard.getBreadcrumbPosition());
		}
		render();
		tester.assertVisible(KEY + ":form:buttonsTop");
		tester.assertInvisible(KEY + ":form:buttonsBottom");
		tester.assertInvisible(KEY + ":form:header:breadcrumbBefore");
		tester.assertInvisible(KEY + ":form:header:breadcrumbAfter");
		answer("a");
		assertEquals("Anopheles", title(KEY), "the buttons work above the body too");
	}

	@Test
	void speaksTheLanguageOfTheSession()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		tester.startPage(WizardPage.class);

		assertEquals("Frage 1", title(KEY));
		assertEquals("Weiter", button(KEY, "next").getChild("class", "ant-button__label")
			.getValue());
		assertEquals("Zurück", button(KEY, "previous").getChild("class", "ant-button__label")
			.getValue());
		assertEquals("Abbrechen", button(KEY, "cancel").getChild("class", "ant-button__label")
			.getValue());
	}
}
