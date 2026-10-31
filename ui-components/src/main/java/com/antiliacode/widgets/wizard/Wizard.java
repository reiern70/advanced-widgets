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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.lang.Args;

import com.antiliacode.widgets.breadcrumb.Breadcrumb;
import com.antiliacode.widgets.menu.AjaxMenuItem;
import com.antiliacode.widgets.menu.IMenuItem;
import com.antiliacode.widgets.theme.LookBehavior;
import com.antiliacode.widgets.theme.LookPart;

/**
 * A wizard: it leads the user through {@link IWizardStep steps}, one at a time, each with its title
 * in the header, its body, and a bar of buttons to move on, all via Ajax. The steps fill in an
 * object, the model of the wizard, typed by {@code T}, which needs not be serializable.
 * <p>
 * The wizard knows its first step only: it asks each step for the next one
 * ({@link IWizardStep#getNextStep(IModel)}), so the steps may branch on what the user entered. It
 * keeps the steps visited, which Previous, First and the crumbs of the trail go back through.
 * <ul>
 * <li>Next submits the form of the body, the fields validated and their values put into the object,
 * then goes to the next step.</li>
 * <li>Last submits it too, then follows the next steps up to one with no next step, without showing
 * nor validating those in between, which Previous then goes back through.</li>
 * <li>Finish submits it, then calls {@link #onFinish(AjaxRequestTarget)}.</li>
 * <li>Previous, First and Cancel submit nothing: what was typed in the step left is lost. Cancel
 * calls {@link #onCancel(AjaxRequestTarget)}.</li>
 * </ul>
 * Which of them show and which may be clicked is the step's to say; see {@link IWizardStep} and
 * {@link IWizardButtonFactory}, which makes buttons of one's own too. The bar is below the body or
 * between the header and the body ({@link #setButtonsPosition(WizardButtonsPosition)}), and the
 * trail of the steps visited above the title, in its place, below it or nowhere
 * ({@link #setBreadcrumbPosition(WizardBreadcrumbPosition)}); on the first step, with no step to
 * go back to, there is no trail.
 * <p>
 * The wizard is a form of its own, nested when it is inside another one, and repaints itself as
 * the step changes, putting the focus on the title of the new step. A field not valid repaints it
 * too ({@link #onError(AjaxRequestTarget)}); the feedback messages go wherever the page shows them,
 * its {@link com.antiliacode.widgets.notification.Notifications Notifications} for example. In the
 * colors of the {@link com.antiliacode.widgets.theme.Theme theme} it is in and with the classes of
 * its {@link com.antiliacode.widgets.theme.ILook look}, a card in Bootstrap's.
 *
 * <pre>
 * add(new Wizard&lt;&gt;("wizard", Model.of(new Order()), new CustomerStep())
 * {
 * 	&#64;Override
 * 	protected void onFinish(AjaxRequestTarget target)
 * 	{
 * 		orders.save(getModelObject());
 * 		setResponsePage(OrdersPage.class);
 * 	}
 * }.setButtonsPosition(WizardButtonsPosition.BOTTOM));
 * </pre>
 *
 * @param <T>
 *            the type of the object the wizard fills in
 * @since 10.11.0
 */
public class Wizard<T> extends GenericPanel<T>
{
	private static final long serialVersionUID = 1L;

	/** The style sheet of the wizards. */
	public static final ResourceReference CSS = new CssResourceReference(Wizard.class,
		"antilia-wizard.css");

	/** How many steps Last follows at most, so that steps leading to each other end. */
	public static final int MAX_STEPS = 1000;

	/** The id of the body of the step, which {@link IWizardStep#newBody(String, IModel)} gets. */
	public static final String BODY_ID = "content";

	private final IWizardStep<T> firstStep;

	/** The steps visited before the one shown, the first one first. */
	private final List<IWizardStep<T>> visited = new ArrayList<>();

	private IWizardStep<T> currentStep;

	private WizardButtonsPosition buttonsPosition = WizardButtonsPosition.BOTTOM;

	private WizardBreadcrumbPosition breadcrumbPosition = WizardBreadcrumbPosition.ABOVE_TITLE;

	private int titleLevel = 2;

	private final Form<Void> form;

	private final Label title;

	private final WebMarkupContainer body;

	private final RepeatingView topButtons;

	private final RepeatingView bottomButtons;

	/** Whether the body and the buttons were made, once the wizard was initialized. */
	private boolean shown;

	/**
	 * @param id
	 *            component id
	 * @param model
	 *            the object the wizard fills in
	 * @param firstStep
	 *            the first step
	 */
	public Wizard(String id, IModel<T> model, IWizardStep<T> firstStep)
	{
		super(id, model);
		this.firstStep = Args.notNull(firstStep, "firstStep");
		currentStep = firstStep;
		setOutputMarkupId(true);

		form = new Form<>("form");
		form.add(new LookBehavior(LookPart.WIZARD));
		add(form);

		WebMarkupContainer header = new WebMarkupContainer("header");
		header.add(new LookBehavior(LookPart.WIZARD_HEADER));
		form.add(header);

		title = new Label("title")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onComponentTag(ComponentTag tag)
			{
				super.onComponentTag(tag);
				tag.put("role", "heading");
				tag.put("aria-level", titleLevel);
			}
		};
		title.setOutputMarkupId(true);
		title.add(AttributeModifier.replace("tabindex", "-1"));
		title.add(new LookBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected LookPart[] getParts(Component component)
			{
				return new LookPart[] { LookPart.WIZARD_TITLE,
					breadcrumbPosition == WizardBreadcrumbPosition.IN_TITLE && hasPreviousStep() ?
						LookPart.WIZARD_TITLE_HIDDEN : null };
			}
		});
		header.add(newBreadcrumb("breadcrumbBefore", true));
		header.add(title);
		header.add(newBreadcrumb("breadcrumbAfter", false));

		topButtons = new RepeatingView("buttons");
		form.add(newBar("buttonsTop", WizardButtonsPosition.TOP, topButtons));

		body = new WebMarkupContainer("body");
		body.add(new LookBehavior(LookPart.WIZARD_BODY));
		body.add(AttributeModifier.replace("aria-labelledby", title::getMarkupId));
		form.add(body);

		bottomButtons = new RepeatingView("buttons");
		form.add(newBar("buttonsBottom", WizardButtonsPosition.BOTTOM, bottomButtons));
	}

	/**
	 * @return a bar of buttons, shown at its position
	 */
	private WebMarkupContainer newBar(String id, WizardButtonsPosition position,
		RepeatingView buttons)
	{
		WebMarkupContainer bar = new WebMarkupContainer(id)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(buttonsPosition == position);
			}
		};
		bar.add(new LookBehavior(LookPart.WIZARD_BUTTONS, position == WizardButtonsPosition.TOP ?
			LookPart.WIZARD_BUTTONS_TOP : LookPart.WIZARD_BUTTONS_BOTTOM));
		bar.add(buttons);
		return bar;
	}

	/**
	 * @return the trail of the steps visited, before the title or after it, shown at its position
	 *         once there is a step visited
	 */
	private Breadcrumb newBreadcrumb(String id, boolean beforeTitle)
	{
		IModel<List<IMenuItem>> crumbs = LoadableDetachableModel.of(this::crumbs);
		Breadcrumb breadcrumb = new Breadcrumb(id, crumbs)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				WizardBreadcrumbPosition at = breadcrumbPosition;
				boolean before = at == WizardBreadcrumbPosition.ABOVE_TITLE ||
					at == WizardBreadcrumbPosition.IN_TITLE;
				// a trail of the step shown only would repeat the title
				setVisible(hasPreviousStep() &&
					(beforeTitle ? before : at == WizardBreadcrumbPosition.BELOW_TITLE));
			}
		};
		breadcrumb.setLabel(new ResourceModel("Wizard.steps"));
		breadcrumb.add(new LookBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected LookPart[] getParts(Component component)
			{
				return new LookPart[] { LookPart.WIZARD_BREADCRUMB,
					breadcrumbPosition == WizardBreadcrumbPosition.IN_TITLE ?
						LookPart.WIZARD_BREADCRUMB_IN_TITLE : null };
			}
		});
		return breadcrumb;
	}

	/**
	 * @return the crumbs: the steps visited, going back to them when the step shown lets go back,
	 *         then the step shown
	 */
	private List<IMenuItem> crumbs()
	{
		IModel<T> model = getModel();
		boolean back = currentStep.isPreviousEnabled(model);
		List<IMenuItem> items = new ArrayList<>();
		for (int i = 0; i < visited.size(); i++)
		{
			int index = i;
			items.add(AjaxMenuItem.of(visited.get(i).getTitle(model), target -> back(index, target))
				.setEnabled(back));
		}
		items.add(AjaxMenuItem.of(currentStep.getTitle(model), target -> {
		}));
		return items;
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		showStep();
	}

	/**
	 * Makes the body, the title and the buttons of the step shown.
	 */
	private void showStep()
	{
		IModel<T> model = getModel();
		Component content = currentStep.newBody(BODY_ID, model);
		if (content == null || !BODY_ID.equals(content.getId()))
		{
			throw new IllegalStateException("The body of the step " + currentStep +
				" must have the id it was given, '" + BODY_ID + "'");
		}
		body.addOrReplace(content);
		title.setDefaultModel(currentStep.getTitle(model));
		populateButtons();
		shown = true;
	}

	/**
	 * Makes the buttons of the step shown, in the bar at their position.
	 */
	private void populateButtons()
	{
		topButtons.removeAll();
		bottomButtons.removeAll();
		RepeatingView buttons = buttonsPosition == WizardButtonsPosition.TOP ? topButtons :
			bottomButtons;
		for (IWizardButtonFactory<T> factory : currentStep.getButtons())
		{
			String id = buttons.newChildId();
			Component button = factory.createButton(id, this);
			if (button == null || !id.equals(button.getId()))
			{
				throw new IllegalStateException("A button of the step " + currentStep +
					" must have the id it was given");
			}
			buttons.add(button);
		}
	}

	/**
	 * Shows another step: its body, made anew, its title and its buttons; with an Ajax request, the
	 * wizard repaints itself and puts the focus on the title.
	 */
	private void changeStep(IWizardStep<T> step, AjaxRequestTarget target)
	{
		IWizardStep<T> from = currentStep;
		currentStep = step;
		showStep();
		onStepChanged(target, from, step);
		if (target != null)
		{
			target.add(this);
			target.focusComponent(title);
		}
	}

	/**
	 * Goes to the next step, the one the step shown answers. The Next button calls it once the form
	 * was submitted and its fields were valid; called otherwise, nothing is submitted. With no next
	 * step the wizard stays where it is.
	 *
	 * @param target
	 *            the Ajax request, {@code null} for none
	 */
	public void next(AjaxRequestTarget target)
	{
		IWizardStep<T> next = currentStep.getNextStep(getModel());
		if (next == null)
		{
			if (target != null)
			{
				target.add(this);
			}
			return;
		}
		visited.add(currentStep);
		changeStep(next, target);
	}

	/**
	 * Goes to the last step: follows the next steps from the step shown up to one with no next
	 * step, without showing nor validating those in between, which are visited all the same, so
	 * Previous goes back through them. The Last button calls it once the form was submitted and
	 * its fields were valid.
	 *
	 * @param target
	 *            the Ajax request, {@code null} for none
	 * @throws IllegalStateException
	 *             when there are more than {@link #MAX_STEPS} steps, as when they lead to each
	 *             other
	 */
	public void last(AjaxRequestTarget target)
	{
		IModel<T> model = getModel();
		List<IWizardStep<T>> passed = new ArrayList<>();
		IWizardStep<T> step = currentStep;
		for (IWizardStep<T> next = step.getNextStep(model); next != null; next = step
			.getNextStep(model))
		{
			if (passed.size() >= MAX_STEPS)
			{
				throw new IllegalStateException("The steps from " + currentStep +
					" go on for more than " + MAX_STEPS + " steps: do they lead to each other?");
			}
			passed.add(step);
			step = next;
		}
		if (passed.isEmpty())
		{
			if (target != null)
			{
				target.add(this);
			}
			return;
		}
		visited.addAll(passed);
		changeStep(step, target);
	}

	/**
	 * Goes back to the step before, without submitting anything: what was typed in the step shown
	 * is lost. Nothing happens on the first step.
	 *
	 * @param target
	 *            the Ajax request, {@code null} for none
	 */
	public void previous(AjaxRequestTarget target)
	{
		if (!visited.isEmpty())
		{
			changeStep(visited.remove(visited.size() - 1), target);
		}
	}

	/**
	 * Goes back to the first step, without submitting anything, forgetting the steps visited.
	 * Nothing happens on the first step.
	 *
	 * @param target
	 *            the Ajax request, {@code null} for none
	 */
	public void first(AjaxRequestTarget target)
	{
		if (!visited.isEmpty())
		{
			visited.clear();
			changeStep(firstStep, target);
		}
	}

	/**
	 * Goes back to a step visited, without submitting anything, forgetting those after it; as a
	 * crumb of the trail does.
	 *
	 * @param index
	 *            the place of the step among those {@link #getVisitedSteps() visited}
	 * @param target
	 *            the Ajax request, {@code null} for none
	 */
	public void back(int index, AjaxRequestTarget target)
	{
		Args.withinRange(0, visited.size() - 1, index, "index");
		IWizardStep<T> step = visited.get(index);
		visited.subList(index, visited.size()).clear();
		changeStep(step, target);
	}

	/**
	 * Starts again from the first step, its body made anew from the object, forgetting the steps
	 * visited: after the application reset the object, for example once the wizard finished.
	 *
	 * @param target
	 *            the Ajax request, {@code null} for none
	 */
	public void restart(AjaxRequestTarget target)
	{
		visited.clear();
		changeStep(firstStep, target);
	}

	/**
	 * Makes the body of the step shown anew from the object, and its buttons, and repaints the
	 * wizard: after a button of one's own changed the object, for example, as the fields would show
	 * what was typed in them otherwise.
	 *
	 * @param target
	 *            the Ajax request, {@code null} for none
	 */
	public void refresh(AjaxRequestTarget target)
	{
		showStep();
		if (target != null)
		{
			target.add(this);
		}
	}

	/**
	 * Finishes the wizard: the Finish button calls it once the form was submitted and its fields
	 * were valid.
	 *
	 * @param target
	 *            the Ajax request
	 */
	public void finish(AjaxRequestTarget target)
	{
		onFinish(target);
	}

	/**
	 * Cancels the wizard: the Cancel button calls it, without submitting anything.
	 *
	 * @param target
	 *            the Ajax request
	 */
	public void cancel(AjaxRequestTarget target)
	{
		onCancel(target);
	}

	/**
	 * Called when the wizard finishes, the object filled in; nothing by default.
	 *
	 * @param target
	 *            the Ajax request
	 */
	protected void onFinish(AjaxRequestTarget target)
	{
	}

	/**
	 * Called when the user cancels the wizard; nothing by default.
	 *
	 * @param target
	 *            the Ajax request
	 */
	protected void onCancel(AjaxRequestTarget target)
	{
	}

	/**
	 * Called when a field of the body is not valid, as a button submitted the form; repaints the
	 * wizard by default, so the fields show their errors.
	 *
	 * @param target
	 *            the Ajax request
	 */
	protected void onError(AjaxRequestTarget target)
	{
		target.add(this);
	}

	/**
	 * Called when another step is shown, before the wizard repaints itself; nothing by default.
	 *
	 * @param target
	 *            the Ajax request, {@code null} for none
	 * @param from
	 *            the step shown before
	 * @param to
	 *            the step shown now
	 */
	protected void onStepChanged(AjaxRequestTarget target, IWizardStep<T> from, IWizardStep<T> to)
	{
	}

	/**
	 * @return the step shown
	 */
	public IWizardStep<T> getCurrentStep()
	{
		return currentStep;
	}

	/**
	 * @return the first step
	 */
	public IWizardStep<T> getFirstStep()
	{
		return firstStep;
	}

	/**
	 * @return the steps visited before the one shown, the first one first
	 */
	public List<IWizardStep<T>> getVisitedSteps()
	{
		return Collections.unmodifiableList(visited);
	}

	/**
	 * @return whether there is a step to go back to
	 */
	public boolean hasPreviousStep()
	{
		return !visited.isEmpty();
	}

	/**
	 * @return the form of the wizard, which the buttons submit
	 */
	public Form<?> getForm()
	{
		return form;
	}

	/**
	 * @return the title of the step shown, the heading of the wizard
	 */
	public Component getTitle()
	{
		return title;
	}

	/**
	 * @return where the bar of the buttons is, {@link WizardButtonsPosition#BOTTOM} by default
	 */
	public WizardButtonsPosition getButtonsPosition()
	{
		return buttonsPosition;
	}

	/**
	 * @param position
	 *            where the bar of the buttons is
	 * @return {@code this}
	 */
	public Wizard<T> setButtonsPosition(WizardButtonsPosition position)
	{
		WizardButtonsPosition old = buttonsPosition;
		buttonsPosition = Args.notNull(position, "position");
		if (shown && old != position)
		{
			populateButtons();
		}
		return this;
	}

	/**
	 * @return where the trail of the steps visited is,
	 *         {@link WizardBreadcrumbPosition#ABOVE_TITLE} by default
	 */
	public WizardBreadcrumbPosition getBreadcrumbPosition()
	{
		return breadcrumbPosition;
	}

	/**
	 * @param position
	 *            where the trail of the steps visited is, {@link WizardBreadcrumbPosition#NONE}
	 *            for none
	 * @return {@code this}
	 */
	public Wizard<T> setBreadcrumbPosition(WizardBreadcrumbPosition position)
	{
		breadcrumbPosition = Args.notNull(position, "position");
		return this;
	}

	/**
	 * @return the level of the heading of the title, its {@code aria-level}, 2 by default
	 */
	public int getTitleLevel()
	{
		return titleLevel;
	}

	/**
	 * @param level
	 *            the level of the heading of the title, from 1 to 6, after the headings around
	 *            the wizard
	 * @return {@code this}
	 */
	public Wizard<T> setTitleLevel(int level)
	{
		titleLevel = Args.withinRange(1, 6, level, "level");
		return this;
	}

	@Override
	protected void onConfigure()
	{
		super.onConfigure();
		if (form.isRootForm())
		{
			// the buttons make the first submitting one of them the default, as they are
			// configured after this
			form.setDefaultButton(null);
		}
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(CSS));
	}

	@Override
	protected void onDetach()
	{
		visited.forEach(IWizardStep::detach);
		currentStep.detach();
		firstStep.detach();
		super.onDetach();
	}
}
