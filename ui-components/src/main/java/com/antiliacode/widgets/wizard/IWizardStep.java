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

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;

/**
 * A step of a {@link Wizard}: its title, in the header of the wizard, its body, and the step that
 * follows it. The steps are known only as the user goes: the wizard asks each for the next one,
 * which may depend on what the user entered, so a wizard can branch, as a dichotomous key does.
 * <p>
 * A step is typed by {@code T}, the object the wizard fills in, which needs not be
 * {@link java.io.Serializable}: the wizard holds it in a model, a
 * {@link org.apache.wicket.model.LoadableDetachableModel LoadableDetachableModel} for example, and
 * hands that model to the step each time, so the step keeps no copy of its own.
 * <p>
 * The step says which buttons show and which may be clicked, by default all of them: First,
 * Previous, Next and Last ({@link #hasFirst(IModel)}, {@link #isFirstEnabled(IModel)} and the
 * like), Cancel ({@link #canCancel(IModel)}) and Finish ({@link #isFinish(IModel)}). The wizard
 * disables First and Previous on its first step, as there is nothing to go back to. A step may
 * also give buttons of its own, {@link #getButtons()}.
 * <p>
 * Next, Last and Finish submit the form of the body, so its fields are validated and their values
 * put into the object before the wizard moves on; First, Previous and Cancel are plain Ajax
 * actions that submit nothing, so what the user typed in the step left is lost. Last follows
 * {@link #getNextStep(IModel)} from the step shown up to a step with no next one, without showing
 * nor validating the steps in between.
 *
 * <pre>
 * class NameStep implements IWizardStep&lt;Person&gt;
 * {
 * 	public IModel&lt;String&gt; getTitle(IModel&lt;Person&gt; model)
 * 	{
 * 		return new ResourceModel("name");
 * 	}
 *
 * 	public Component newBody(String id, IModel&lt;Person&gt; model)
 * 	{
 * 		return new NamePanel(id, model);
 * 	}
 *
 * 	public IWizardStep&lt;Person&gt; getNextStep(IModel&lt;Person&gt; model)
 * 	{
 * 		return model.getObject().isCompany() ? new CompanyStep() : new AddressStep();
 * 	}
 * }
 * </pre>
 *
 * @param <T>
 *            the type of the object the wizard fills in
 * @since 10.11.0
 */
public interface IWizardStep<T> extends IDetachable
{
	/**
	 * @param model
	 *            the object the wizard fills in
	 * @return the title of the step, in the header of the wizard; it may depend on the object
	 */
	IModel<String> getTitle(IModel<T> model);

	/**
	 * Creates the body of the step, each time the wizard shows it: coming back to a step shows
	 * what the object holds, not what was typed in it before.
	 *
	 * @param id
	 *            the id the component must have
	 * @param model
	 *            the object the wizard fills in
	 * @return the body, a component fit for a {@code div}
	 */
	Component newBody(String id, IModel<T> model);

	/**
	 * The step after this one. The wizard asks for it once the form of the body was submitted
	 * and its fields were valid, to go to it, and again as it renders, to tell whether this step
	 * {@link #isFinish(IModel) finishes} the wizard: it may be asked more than once and should be
	 * cheap.
	 *
	 * @param model
	 *            the object the wizard fills in
	 * @return the next step, {@code null} for none
	 */
	IWizardStep<T> getNextStep(IModel<T> model);

	/**
	 * Whether the wizard may finish at this step, showing the Finish button. By default when there
	 * is no next step; a step whose next one depends on what the user still has to enter in it, a
	 * question of a dichotomous key for example, answers {@code false}.
	 *
	 * @param model
	 *            the object the wizard fills in
	 * @return whether the Finish button shows
	 */
	default boolean isFinish(IModel<T> model)
	{
		return getNextStep(model) == null;
	}

	/**
	 * @param model
	 *            the object the wizard fills in
	 * @return whether the First button shows, {@code true} by default
	 */
	default boolean hasFirst(IModel<T> model)
	{
		return true;
	}

	/**
	 * @param model
	 *            the object the wizard fills in
	 * @return whether the First button may be clicked, {@code true} by default; the wizard
	 *         disables it anyway on its first step
	 */
	default boolean isFirstEnabled(IModel<T> model)
	{
		return true;
	}

	/**
	 * @param model
	 *            the object the wizard fills in
	 * @return whether the Previous button shows, {@code true} by default
	 */
	default boolean hasPrevious(IModel<T> model)
	{
		return true;
	}

	/**
	 * @param model
	 *            the object the wizard fills in
	 * @return whether the Previous button, and the crumbs of the steps before, may be clicked,
	 *         {@code true} by default; the wizard disables it anyway on its first step
	 */
	default boolean isPreviousEnabled(IModel<T> model)
	{
		return true;
	}

	/**
	 * @param model
	 *            the object the wizard fills in
	 * @return whether the Next button shows, {@code true} by default
	 */
	default boolean hasNext(IModel<T> model)
	{
		return true;
	}

	/**
	 * @param model
	 *            the object the wizard fills in
	 * @return whether the Next button may be clicked; by default when the step does not
	 *         {@link #isFinish(IModel) finish} the wizard
	 */
	default boolean isNextEnabled(IModel<T> model)
	{
		return !isFinish(model);
	}

	/**
	 * @param model
	 *            the object the wizard fills in
	 * @return whether the Last button shows, {@code true} by default
	 */
	default boolean hasLast(IModel<T> model)
	{
		return true;
	}

	/**
	 * @param model
	 *            the object the wizard fills in
	 * @return whether the Last button may be clicked; by default when the step does not
	 *         {@link #isFinish(IModel) finish} the wizard
	 */
	default boolean isLastEnabled(IModel<T> model)
	{
		return !isFinish(model);
	}

	/**
	 * @param model
	 *            the object the wizard fills in
	 * @return whether the Cancel button shows, {@code true} by default
	 */
	default boolean canCancel(IModel<T> model)
	{
		return true;
	}

	/**
	 * The buttons of the step, in order, in the bar of the wizard. By default
	 * {@link IWizardButtonFactory#defaults()}: Cancel, First, Previous, Next, Last and Finish,
	 * which follow the methods above. A step may give other buttons, or add its own to these.
	 *
	 * @return the factories of the buttons
	 */
	default List<IWizardButtonFactory<T>> getButtons()
	{
		return IWizardButtonFactory.defaults();
	}

	@Override
	default void detach()
	{
	}
}
