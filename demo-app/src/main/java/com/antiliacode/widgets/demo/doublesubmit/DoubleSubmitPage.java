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

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;

import com.antiliacode.widgets.button.ButtonBehavior;
import com.antiliacode.widgets.button.ButtonBehavior.Size;
import com.antiliacode.widgets.button.ButtonBehavior.Variant;
import com.antiliacode.widgets.demo.DemoPage;
import com.antiliacode.widgets.demo.FormErrors;
import com.antiliacode.widgets.form.DoubleSubmitBulletsProofForm;
import com.antiliacode.widgets.veil.PageVeilBehavior;

/**
 * A form issuing invoices, where the server takes a second for each: a double click, Enter pressed
 * twice, or a click on "Issue" leaving the amount, whose change the server checks, issue one
 * invoice. Two switches take away what keeps them from issuing two: the library's
 * {@link PageVeilBehavior} and the form's version of a {@link DoubleSubmitBulletsProofForm}. They are the page parameters {@value #VEIL} and
 * {@value #VERSION}, {@code off} for off.
 */
public class DoubleSubmitPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	/** The page parameter switching the page veil off. */
	static final String VEIL = "veil";

	/** The page parameter switching the form's version off. */
	static final String VERSION = "version";

	/** How long the server takes to issue an invoice. */
	static final Duration ISSUING = Duration.ofSeconds(1);

	/** How long the server takes to check an amount. */
	static final Duration CHECKING = Duration.ofMillis(600);

	/** The VAT the total adds. */
	private static final BigDecimal VAT = new BigDecimal("1.21");

	/** An invoice issued. */
	record Invoice(int number, String customer, BigDecimal amount) implements Serializable
	{
	}

	private final List<Invoice> invoices = new ArrayList<>();

	private boolean veiled;

	private boolean versioned;

	private String customer;

	private BigDecimal amount = new BigDecimal("120.00");

	/**
	 * @param parameters
	 *            {@value #VEIL}{@code =off} without the page veil, {@value #VERSION}{@code =off}
	 *            without the form's version
	 */
	public DoubleSubmitPage(PageParameters parameters)
	{
		super(parameters);
		veiled = !"off".equals(parameters.get(VEIL).toOptionalString());
		versioned = !"off".equals(parameters.get(VERSION).toOptionalString());
		if (veiled)
		{
			add(new PageVeilBehavior());
		}

		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);

		Form<Void> switches = new Form<>("switches");
		themed.add(switches);
		switches.add(newSwitch(VEIL, LambdaModel.of(() -> veiled, on -> veiled = on)));
		switches.add(newSwitch(VERSION, LambdaModel.of(() -> versioned, on -> versioned = on)));

		Form<Void> form = versioned ? new DoubleSubmitBulletsProofForm<>("form") :
			new Form<>("form");
		themed.add(form);
		FormErrors errors = new FormErrors("errors");
		form.add(errors.setOutputMarkupPlaceholderTag(true));

		form.add(new TextField<>("customer", new PropertyModel<>(this, "customer"))
			.setRequired(true));
		Label total = new Label("total", () -> amount == null ? "" :
			getString("total").replace("{0}",
				amount.multiply(VAT).setScale(2, RoundingMode.HALF_UP).toPlainString()));
		form.add(total.setOutputMarkupId(true));
		NumberTextField<BigDecimal> amountField = new NumberTextField<>("amount",
			new PropertyModel<>(this, "amount"), BigDecimal.class);
		amountField.setMinimum(new BigDecimal("0.01")).setStep(new BigDecimal("0.01"))
			.setRequired(true);
		amountField.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				// the server checks the amount: the page is veiled meanwhile
				pause(CHECKING);
				target.add(total);
			}

			@Override
			protected void onError(AjaxRequestTarget target, RuntimeException e)
			{
				target.add(errors);
			}
		});
		form.add(amountField);

		WebMarkupContainer issued = new WebMarkupContainer("issued");
		themed.add(issued.setOutputMarkupId(true));
		issued.add(new WebMarkupContainer("none")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(invoices.isEmpty());
			}
		});
		WebMarkupContainer table = new WebMarkupContainer("table")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(!invoices.isEmpty());
			}
		};
		issued.add(table);
		table.add(new ListView<>("invoices", invoices)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<Invoice> item)
			{
				Invoice invoice = item.getModelObject();
				item.add(new Label("number", invoice.number()));
				item.add(new Label("customer", invoice.customer()));
				item.add(new Label("amount", invoice.amount().toPlainString()));
			}
		});

		form.add(new AjaxButton("issue")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target)
			{
				pause(ISSUING);
				invoices.add(0, new Invoice(invoices.size() + 1, customer, amount));
				target.add(issued, errors);
			}

			@Override
			protected void onError(AjaxRequestTarget target)
			{
				target.add(errors);
			}
		}.add(new ButtonBehavior(Variant.PRIMARY, Size.MEDIUM)));
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		if (customer == null)
		{
			customer = getString("customer.sample");
		}
	}

	/**
	 * @return a check box turning a protection on or off, which shows the page anew with it
	 */
	private CheckBox newSwitch(String id, IModel<Boolean> model)
	{
		CheckBox check = new CheckBox(id, model);
		check.add(new OnChangeAjaxBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				PageParameters parameters = new PageParameters();
				if (!veiled)
				{
					parameters.set(VEIL, "off");
				}
				if (!versioned)
				{
					parameters.set(VERSION, "off");
				}
				setResponsePage(DoubleSubmitPage.class, parameters);
			}
		});
		return check;
	}

	/**
	 * @return the invoices issued, the latest first
	 */
	List<Invoice> getInvoices()
	{
		return invoices;
	}

	/**
	 * Takes the time the server would, issuing an invoice or checking an amount.
	 *
	 * @param duration
	 *            how long
	 */
	void pause(Duration duration)
	{
		try
		{
			Thread.sleep(duration.toMillis());
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new CssResourceReference(
			DoubleSubmitPage.class, "DoubleSubmitPage.css")));
	}
}
