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
package com.antiliacode.widgets.demo.doublesubmit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Locale;

import org.apache.wicket.extensions.ajax.veil.PageVeilBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.form.DoubleSubmitBulletsProofForm;
import com.antiliacode.widgets.form.FormVersionBehavior;

class DoubleSubmitPageTest
{
	private WicketTester tester;

	@BeforeEach
	void start()
	{
		tester = new WicketTester(new WicketApplication());
		tester.getSession().setLocale(Locale.ENGLISH);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	/**
	 * The page, the server taking no time.
	 */
	private DoubleSubmitPage start(PageParameters parameters)
	{
		return tester.startPage(new DoubleSubmitPage(parameters)
		{
			private static final long serialVersionUID = 1L;

			@Override
			void pause(Duration duration)
			{
			}
		});
	}

	private void issue(DoubleSubmitPage page, String version)
	{
		Form<?> form = (Form<?>)page.get("themed:form");
		if (version != null)
		{
			tester.getRequest().getPostParameters().setParameterValue(
				FormVersionBehavior.PARAMETER_PREFIX + form.getMarkupId(), version);
		}
		post(form, "customer", "Acme Ltd.");
		post(form, "amount", "120.00");
		tester.executeAjaxEvent(page.get("themed:form:issue"), "click");
	}

	private void post(Form<?> form, String field, String value)
	{
		FormComponent<?> component = (FormComponent<?>)form.get(field);
		tester.getRequest().getPostParameters().setParameterValue(component.getInputName(),
			value);
	}

	@Test
	void hasTheVeilAndTheFormsVersionByDefault()
	{
		DoubleSubmitPage page = start(new PageParameters());

		assertEquals(1, page.getBehaviors(PageVeilBehavior.class).size());
		assertInstanceOf(DoubleSubmitBulletsProofForm.class, page.get("themed:form"));
		tester.assertContains("antilia-veil-gaps");
		tester.assertContains("No invoice issued yet.");
	}

	@Test
	void issuesAnInvoice()
	{
		DoubleSubmitPage page = start(new PageParameters());

		issue(page, "0");

		assertEquals(1, page.getInvoices().size());
		assertEquals("Acme Ltd.", page.getInvoices().get(0).customer());
		tester.assertContains("120.00");
	}

	@Test
	void refusesTheFormSentTwiceFromOneVersion()
	{
		DoubleSubmitPage page = start(new PageParameters());
		issue(page, "0");

		issue(page, "0");

		assertEquals(1, page.getInvoices().size());
		tester.assertContains("This form was already sent");
	}

	@Test
	void withoutTheFormsVersionASecondSubmitIssuesASecondInvoice()
	{
		DoubleSubmitPage page = start(new PageParameters().set(DoubleSubmitPage.VERSION, "off")
			.set(DoubleSubmitPage.VEIL, "off"));
		assertTrue(page.getBehaviors(PageVeilBehavior.class).isEmpty());
		assertFalse(page.get("themed:form") instanceof DoubleSubmitBulletsProofForm);

		issue(page, null);
		issue(page, null);

		assertEquals(2, page.getInvoices().size());
	}

	@Test
	void aSwitchShowsThePageAnewWithoutItsProtection()
	{
		DoubleSubmitPage page = start(new PageParameters());
		FormComponent<?> veil = (FormComponent<?>)page.get("themed:switches:veil");
		tester.getRequest().getPostParameters().setParameterValue(veil.getInputName(), "");
		tester.executeAjaxEvent(veil, "change");

		DoubleSubmitPage shown = (DoubleSubmitPage)tester.getLastRenderedPage();
		assertEquals("off", shown.getPageParameters().get(DoubleSubmitPage.VEIL).toString());
		assertTrue(shown.getBehaviors(PageVeilBehavior.class).isEmpty());
		assertInstanceOf(DoubleSubmitBulletsProofForm.class, shown.get("themed:form"));
	}

	@Test
	void theServerChecksTheAmountAndShowsItsTotal()
	{
		DoubleSubmitPage page = start(new PageParameters());
		FormComponent<?> amount = (FormComponent<?>)page.get("themed:form:amount");
		tester.getRequest().getPostParameters().setParameterValue(amount.getInputName(), "100");
		tester.executeAjaxEvent(amount, "change");

		tester.assertComponentOnAjaxResponse("themed:form:total");
		tester.assertLabel("themed:form:total", "121.00 with VAT");
	}
}
