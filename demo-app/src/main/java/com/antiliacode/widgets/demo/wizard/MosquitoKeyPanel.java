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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.AbstractChoice.LabelPosition;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.Model;

import com.antiliacode.widgets.wizard.IWizardStep;
import com.antiliacode.widgets.wizard.Wizard;

/**
 * A dichotomous key as a {@link Wizard}: each step a couplet, a question with two answers, the
 * answer leading to the next couplet or to a mosquito. The steps are known only as the user
 * answers, so the wizard branches; Last follows the answers already given as far as they lead.
 * The key is a simplified one of the adult females of five mosquitoes of the Americas, made for
 * the demo, not for identifying mosquitoes.
 */
public class MosquitoKeyPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	/**
	 * A lead of a couplet: its text, under {@code couplet.<key>}, and where it leads, a couplet or
	 * a mosquito.
	 *
	 * @param key
	 *            the couplet and the answer, such as {@code 1a}
	 * @param next
	 *            the couplet it leads to, 0 for a mosquito
	 * @param taxon
	 *            the mosquito it leads to, its key under {@code taxon.}, {@code null} for a
	 *            couplet
	 */
	record Lead(String key, int next, String taxon) implements Serializable
	{
	}

	/**
	 * A couplet: its two leads.
	 */
	record Couplet(Lead a, Lead b) implements Serializable
	{
		Lead lead(String answer)
		{
			return "a".equals(answer) ? a : "b".equals(answer) ? b : null;
		}
	}

	/** The key, by the number of the couplet. */
	static final Map<Integer, Couplet> KEY = Map.of(
		1, new Couplet(new Lead("1a", 0, "anopheles"), new Lead("1b", 2, null)),
		2, new Couplet(new Lead("2a", 3, null), new Lead("2b", 4, null)),
		3, new Couplet(new Lead("3a", 0, "aedesAegypti"), new Lead("3b", 0, "aedesAlbopictus")),
		4, new Couplet(new Lead("4a", 0, "culiseta"), new Lead("4b", 0, "culex")));

	/**
	 * What the wizard fills in: the answer, {@code a} or {@code b}, given to each couplet.
	 */
	static class Identification implements Serializable
	{
		private static final long serialVersionUID = 1L;

		final Map<Integer, String> answers = new LinkedHashMap<>();

		/**
		 * @return the answers on the way from the first couplet, such as {@code 1b}, as far as
		 *         they lead
		 */
		List<String> route()
		{
			List<String> route = new ArrayList<>();
			for (int number = 1; number > 0 && route.size() < KEY.size();)
			{
				Lead lead = KEY.get(number).lead(answers.get(number));
				if (lead == null)
				{
					break;
				}
				route.add(lead.key());
				number = lead.next();
			}
			return route;
		}
	}

	private final Wizard<Identification> wizard;

	/**
	 * @param id
	 *            component id
	 */
	public MosquitoKeyPanel(String id)
	{
		super(id);
		wizard = new Wizard<>("wizard", Model.of(new Identification()), new CoupletStep(1))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onFinish(AjaxRequestTarget target)
			{
				if (getCurrentStep() instanceof ResultStep result)
				{
					success(MosquitoKeyPanel.this.getString("identified").replace("{0}",
						MosquitoKeyPanel.this.getString("taxon." + result.taxon)));
				}
				startAgain(target);
			}

			@Override
			protected void onCancel(AjaxRequestTarget target)
			{
				info(MosquitoKeyPanel.this.getString("cancelled"));
				startAgain(target);
			}
		};
		wizard.setTitleLevel(4);
		add(wizard);
	}

	private void startAgain(AjaxRequestTarget target)
	{
		wizard.getModelObject().answers.clear();
		wizard.restart(target);
	}

	/**
	 * @return the wizard
	 */
	public Wizard<?> getWizard()
	{
		return wizard;
	}

	/**
	 * A couplet: its question and its two answers, one of which the user has to choose.
	 */
	private class CoupletStep implements IWizardStep<Identification>
	{
		private static final long serialVersionUID = 1L;

		private final int number;

		CoupletStep(int number)
		{
			this.number = number;
		}

		@Override
		public IModel<String> getTitle(IModel<Identification> model)
		{
			return () -> getString("couplet").replace("{0}", String.valueOf(number));
		}

		@Override
		public Component newBody(String id, IModel<Identification> model)
		{
			Couplet couplet = KEY.get(number);
			Fragment body = new Fragment(id, "couplet", MosquitoKeyPanel.this);
			body.add(new Label("question", getString("couplet." + number)));
			IModel<String> answer = LambdaModel.of(() -> model.getObject().answers.get(number),
				value -> model.getObject().answers.put(number, value));
			body.add(new RadioChoice<>("answer", answer, List.of("a", "b"),
				new IChoiceRenderer<String>()
				{
					private static final long serialVersionUID = 1L;

					@Override
					public Object getDisplayValue(String shown)
					{
						return getString("couplet." + couplet.lead(shown).key());
					}

					@Override
					public String getIdValue(String shown, int index)
					{
						return shown;
					}
				}).setLabelPosition(LabelPosition.WRAP_AFTER).setRequired(true)
				.setLabel(Model.of(getString("couplet." + number))));
			return body;
		}

		@Override
		public IWizardStep<Identification> getNextStep(IModel<Identification> model)
		{
			Lead lead = KEY.get(number).lead(model.getObject().answers.get(number));
			if (lead == null)
			{
				return null;
			}
			return lead.next() > 0 ? new CoupletStep(lead.next()) : new ResultStep(lead.taxon());
		}

		/**
		 * The answer, still to be chosen, tells the next step: a couplet never finishes the
		 * key.
		 */
		@Override
		public boolean isFinish(IModel<Identification> model)
		{
			return false;
		}
	}

	/**
	 * The mosquito the answers lead to.
	 */
	private class ResultStep implements IWizardStep<Identification>
	{
		private static final long serialVersionUID = 1L;

		private final String taxon;

		ResultStep(String taxon)
		{
			this.taxon = taxon;
		}

		@Override
		public IModel<String> getTitle(IModel<Identification> model)
		{
			return () -> getString("taxon." + taxon);
		}

		@Override
		public Component newBody(String id, IModel<Identification> model)
		{
			Fragment body = new Fragment(id, "result", MosquitoKeyPanel.this);
			body.add(new Label("name", getString("taxon." + taxon)));
			body.add(new Label("note", getString("taxon." + taxon + ".note")));
			body.add(new Label("route", () -> getString("route").replace("{0}",
				String.join(" → ", model.getObject().route()))));
			return body;
		}

		@Override
		public IWizardStep<Identification> getNextStep(IModel<Identification> model)
		{
			return null;
		}

		@Override
		public boolean hasNext(IModel<Identification> model)
		{
			return false;
		}

		@Override
		public boolean hasLast(IModel<Identification> model)
		{
			return false;
		}
	}
}
