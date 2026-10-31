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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

import com.antiliacode.widgets.button.AjaxActionButton;
import com.antiliacode.widgets.button.ButtonBehavior.Variant;
import com.antiliacode.widgets.icon.IIcon;
import com.antiliacode.widgets.icon.SvgIcon;

/**
 * The default buttons of a {@link Wizard}, which {@link IWizardButtonFactory} makes. Each has the
 * class {@code antilia-wizard-<kind>}, such as {@code antilia-wizard-next}, and its text under
 * {@code Wizard.<kind>} in the bundle of {@code Initializer}. Next and Finish are the primary
 * buttons; Cancel stands apart, at the start of the bar.
 */
final class WizardButtons
{
	/** The prefix of the class each default button has, followed by its kind. */
	static final String CLASS_PREFIX = "antilia-wizard-";

	private WizardButtons()
	{
	}

	static <T> Component first(String id, Wizard<T> wizard)
	{
		return new ActionButton<>(id, "first", SvgIcon.ANGLES_LEFT, wizard)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				wizard.first(target);
			}

			@Override
			boolean isShown(IWizardStep<T> step, IModel<T> model)
			{
				return step.hasFirst(model);
			}

			@Override
			boolean isClickable(IWizardStep<T> step, IModel<T> model)
			{
				return wizard.hasPreviousStep() && step.isFirstEnabled(model);
			}
		};
	}

	static <T> Component previous(String id, Wizard<T> wizard)
	{
		return new ActionButton<>(id, "previous", SvgIcon.ANGLE_LEFT, wizard)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				wizard.previous(target);
			}

			@Override
			boolean isShown(IWizardStep<T> step, IModel<T> model)
			{
				return step.hasPrevious(model);
			}

			@Override
			boolean isClickable(IWizardStep<T> step, IModel<T> model)
			{
				return wizard.hasPreviousStep() && step.isPreviousEnabled(model);
			}
		};
	}

	static <T> Component cancel(String id, Wizard<T> wizard)
	{
		return new ActionButton<>(id, "cancel", SvgIcon.XMARK, wizard)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				wizard.cancel(target);
			}

			@Override
			boolean isShown(IWizardStep<T> step, IModel<T> model)
			{
				return step.canCancel(model);
			}
		}.setVariant(Variant.GHOST);
	}

	static <T> Component next(String id, Wizard<T> wizard)
	{
		return new SubmitButton<>(id, "next", SvgIcon.ANGLE_RIGHT, wizard)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target)
			{
				wizard.next(target);
			}

			@Override
			boolean isShown(IWizardStep<T> step, IModel<T> model)
			{
				return step.hasNext(model);
			}

			@Override
			boolean isClickable(IWizardStep<T> step, IModel<T> model)
			{
				return step.isNextEnabled(model);
			}
		}.setVariant(Variant.PRIMARY);
	}

	static <T> Component last(String id, Wizard<T> wizard)
	{
		return new SubmitButton<>(id, "last", SvgIcon.ANGLES_RIGHT, wizard)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target)
			{
				wizard.last(target);
			}

			@Override
			boolean isShown(IWizardStep<T> step, IModel<T> model)
			{
				return step.hasLast(model);
			}

			@Override
			boolean isClickable(IWizardStep<T> step, IModel<T> model)
			{
				return step.isLastEnabled(model);
			}
		};
	}

	static <T> Component finish(String id, Wizard<T> wizard)
	{
		return new SubmitButton<>(id, "finish", SvgIcon.CHECK, wizard)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target)
			{
				wizard.finish(target);
			}

			@Override
			boolean isShown(IWizardStep<T> step, IModel<T> model)
			{
				return step.isFinish(model);
			}
		}.setVariant(Variant.PRIMARY);
	}

	/**
	 * A default button submitting nothing, shown and enabled as the step shown says.
	 */
	private abstract static class ActionButton<T> extends AjaxActionButton
	{
		private static final long serialVersionUID = 1L;

		private final Wizard<T> wizard;

		ActionButton(String id, String kind, IIcon icon, Wizard<T> wizard)
		{
			super(id, new ResourceModel("Wizard." + kind));
			this.wizard = wizard;
			setIcon(icon);
			add(AttributeModifier.append("class", CLASS_PREFIX + kind));
		}

		abstract boolean isShown(IWizardStep<T> step, IModel<T> model);

		boolean isClickable(IWizardStep<T> step, IModel<T> model)
		{
			return true;
		}

		@Override
		protected void onConfigure()
		{
			super.onConfigure();
			IWizardStep<T> step = wizard.getCurrentStep();
			setVisible(isShown(step, wizard.getModel()));
			setEnabled(isVisible() && isClickable(step, wizard.getModel()));
		}
	}

	/**
	 * A default button submitting the form of the body, shown and enabled as the step shown says;
	 * the first of them shown and enabled is the one Enter in a field clicks.
	 */
	private abstract static class SubmitButton<T> extends WizardSubmitButton
	{
		private static final long serialVersionUID = 1L;

		private final Wizard<T> wizard;

		SubmitButton(String id, String kind, IIcon icon, Wizard<T> wizard)
		{
			super(id, new ResourceModel("Wizard." + kind), wizard.getForm());
			this.wizard = wizard;
			setIcon(icon);
			add(AttributeModifier.append("class", CLASS_PREFIX + kind));
		}

		abstract boolean isShown(IWizardStep<T> step, IModel<T> model);

		boolean isClickable(IWizardStep<T> step, IModel<T> model)
		{
			return true;
		}

		@Override
		protected void onConfigure()
		{
			super.onConfigure();
			IWizardStep<T> step = wizard.getCurrentStep();
			setVisible(isShown(step, wizard.getModel()));
			setEnabled(isVisible() && isClickable(step, wizard.getModel()));
			// the wizard clears the default button of its form, a root one, before its buttons
			// are configured; that of a form the wizard is nested in stays the page's
			if (getForm().isRootForm() && isVisible() && isEnabled() &&
				getForm().getDefaultButton() == null)
			{
				getForm().setDefaultButton(this);
			}
		}
	}
}
