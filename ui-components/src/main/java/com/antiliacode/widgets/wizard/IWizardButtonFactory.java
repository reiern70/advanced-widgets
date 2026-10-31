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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.lang.Args;
import org.danekja.java.util.function.serializable.SerializableBiConsumer;

import com.antiliacode.widgets.button.AjaxActionButton;
import com.antiliacode.widgets.icon.IIcon;

/**
 * Makes a button of the bar of a {@link Wizard}, for the step it shows, each time the wizard
 * shows a step: those its {@link IWizardStep#getButtons()} lists. The button is rendered on a
 * {@code <button type="button">} inside the form of the wizard: an
 * {@link com.antiliacode.widgets.button.AjaxActionButton AjaxActionButton}, which submits nothing,
 * a {@link WizardSubmitButton}, which submits the form of the body, or any component fit for that
 * tag. The factories here make the default buttons, which follow what the step says of them, and
 * buttons of one's own:
 *
 * <pre>
 * public List&lt;IWizardButtonFactory&lt;Order&gt;&gt; getButtons()
 * {
 * 	List&lt;IWizardButtonFactory&lt;Order&gt;&gt; buttons =
 * 		new ArrayList&lt;&gt;(IWizardButtonFactory.defaults());
 * 	buttons.add(IWizardButtonFactory.submit(new ResourceModel("saveDraft"), SvgIcon.FLOPPY_DISK,
 * 		(target, wizard) -&gt; drafts.save(wizard.getModelObject())));
 * 	return buttons;
 * }
 * </pre>
 *
 * @param <T>
 *            the type of the object the wizard fills in
 * @since 10.11.0
 */
@FunctionalInterface
public interface IWizardButtonFactory<T> extends IClusterable
{
	/**
	 * @param id
	 *            the id the button must have
	 * @param wizard
	 *            the wizard the button is in
	 * @return the button
	 */
	Component createButton(String id, Wizard<T> wizard);

	/**
	 * @param <T>
	 *            the type of the object the wizard fills in
	 * @return the default buttons, in order: Cancel, First, Previous, Next, Last and Finish
	 */
	static <T> List<IWizardButtonFactory<T>> defaults()
	{
		return List.of(IWizardButtonFactory.<T> cancel(), IWizardButtonFactory.<T> first(),
			IWizardButtonFactory.<T> previous(), IWizardButtonFactory.<T> next(),
			IWizardButtonFactory.<T> last(), IWizardButtonFactory.<T> finish());
	}

	/**
	 * @param <T>
	 *            the type of the object the wizard fills in
	 * @return the factory of the First button, back to the first step without submitting the form;
	 *         shown as {@link IWizardStep#hasFirst(IModel)} says, enabled as
	 *         {@link IWizardStep#isFirstEnabled(IModel)} says and when there is a step before
	 */
	static <T> IWizardButtonFactory<T> first()
	{
		return WizardButtons::first;
	}

	/**
	 * @param <T>
	 *            the type of the object the wizard fills in
	 * @return the factory of the Previous button, back to the step before without submitting the
	 *         form; shown as {@link IWizardStep#hasPrevious(IModel)} says, enabled as
	 *         {@link IWizardStep#isPreviousEnabled(IModel)} says and when there is a step before
	 */
	static <T> IWizardButtonFactory<T> previous()
	{
		return WizardButtons::previous;
	}

	/**
	 * @param <T>
	 *            the type of the object the wizard fills in
	 * @return the factory of the Next button, submitting the form and going to the next step;
	 *         shown as {@link IWizardStep#hasNext(IModel)} says and enabled as
	 *         {@link IWizardStep#isNextEnabled(IModel)} says
	 */
	static <T> IWizardButtonFactory<T> next()
	{
		return WizardButtons::next;
	}

	/**
	 * @param <T>
	 *            the type of the object the wizard fills in
	 * @return the factory of the Last button, submitting the form and going to the last step
	 *         without showing nor validating those in between; shown as
	 *         {@link IWizardStep#hasLast(IModel)} says and enabled as
	 *         {@link IWizardStep#isLastEnabled(IModel)} says
	 */
	static <T> IWizardButtonFactory<T> last()
	{
		return WizardButtons::last;
	}

	/**
	 * @param <T>
	 *            the type of the object the wizard fills in
	 * @return the factory of the Finish button, submitting the form and calling
	 *         {@link Wizard#onFinish(AjaxRequestTarget)}; shown when
	 *         {@link IWizardStep#isFinish(IModel)}
	 */
	static <T> IWizardButtonFactory<T> finish()
	{
		return WizardButtons::finish;
	}

	/**
	 * @param <T>
	 *            the type of the object the wizard fills in
	 * @return the factory of the Cancel button, calling {@link Wizard#onCancel(AjaxRequestTarget)}
	 *         without submitting the form; shown when {@link IWizardStep#canCancel(IModel)}
	 */
	static <T> IWizardButtonFactory<T> cancel()
	{
		return WizardButtons::cancel;
	}

	/**
	 * @param <T>
	 *            the type of the object the wizard fills in
	 * @param label
	 *            the text of the button
	 * @param icon
	 *            the icon before the text, {@code null} for none
	 * @param onClick
	 *            what a click does, in an Ajax request that submits nothing
	 * @return the factory of a button of one's own
	 */
	static <T> IWizardButtonFactory<T> button(IModel<String> label, IIcon icon,
		SerializableBiConsumer<AjaxRequestTarget, Wizard<T>> onClick)
	{
		Args.notNull(label, "label");
		Args.notNull(onClick, "onClick");
		return (id, wizard) -> new AjaxActionButton(id, label)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				onClick.accept(target, wizard);
			}
		}.setIcon(icon);
	}

	/**
	 * @param <T>
	 *            the type of the object the wizard fills in
	 * @param label
	 *            the text of the button
	 * @param icon
	 *            the icon before the text, {@code null} for none
	 * @param onSubmit
	 *            what a click does once the form of the body was submitted and its fields were
	 *            valid, in an Ajax request
	 * @return the factory of a button of one's own submitting the form
	 */
	static <T> IWizardButtonFactory<T> submit(IModel<String> label, IIcon icon,
		SerializableBiConsumer<AjaxRequestTarget, Wizard<T>> onSubmit)
	{
		Args.notNull(label, "label");
		Args.notNull(onSubmit, "onSubmit");
		return (id, wizard) -> new WizardSubmitButton(id, label, wizard.getForm())
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target)
			{
				onSubmit.accept(target, wizard);
			}
		}.setIcon(icon);
	}
}
