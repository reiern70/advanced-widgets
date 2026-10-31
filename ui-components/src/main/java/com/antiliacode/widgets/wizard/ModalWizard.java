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
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

import com.antiliacode.widgets.ScriptJson;
import com.antiliacode.widgets.button.ButtonBehavior.Size;
import com.antiliacode.widgets.button.ButtonBehavior.Variant;
import com.antiliacode.widgets.icon.SvgIcon;
import com.antiliacode.widgets.modal.IModalCloseRequestHandler;
import com.antiliacode.widgets.modal.Modal;
import com.antiliacode.widgets.modal.ModalCloseButton;
import com.antiliacode.widgets.modal.ModalContent;
import com.antiliacode.widgets.theme.LookContainer;
import com.antiliacode.widgets.theme.LookPart;

/**
 * A {@link Wizard} in a {@link Modal}, laid out in the parts of the dialog, as a
 * {@link ModalContent} is, so the dialog drags, folds, scrolls and closes as any other:
 * <ul>
 * <li>the title of the step, and the trail of the steps visited, in the header of the dialog,
 * which drags and folds it, with the close button on the right;</li>
 * <li>the body of the step in the body of the dialog, the only part that scrolls, with slim
 * scroll bars;</li>
 * <li>the bar of buttons in the footer, or between the header and the body
 * ({@link WizardButtonsPosition#TOP}).</li>
 * </ul>
 * Cancel, the close button, Escape and a click around the dialog, when it closes it, are one and
 * the same: the user asks to close the dialog. The function of {@link Modal#setCanClose(String)}
 * is asked in the browser, then {@link Modal#canClose(AjaxRequestTarget)} on the server, then
 * {@link #onCancel(AjaxRequestTarget)}, which closes the dialog. Finish submits the form and calls
 * {@link #onFinish(AjaxRequestTarget)}, which closes the dialog too; an override that does not
 * call it keeps the dialog open, when saving failed for example.
 * <p>
 * Past the first step the wizard holds what the user entered in the steps before, which no field
 * of the step shown has: its form is marked {@code data-modified="true"} then, so that
 * {@link Modal#confirmCloseIfModified(IModel)} asks before discarding it, and a tab or a window
 * of a desktop the dialog is in tells of changes not saved.
 *
 * <pre>
 * modal.setSize(Modal.Size.MEDIUM).open(new ModalWizard&lt;&gt;(Model.of(new Order()), new CustomerStep())
 * {
 * 	&#64;Override
 * 	protected void onFinish(AjaxRequestTarget target)
 * 	{
 * 		orders.save(getModelObject());
 * 		super.onFinish(target); // closes the dialog
 * 	}
 * }, target);
 * </pre>
 *
 * A dialog fitting its content changes its width from one step to the next: a
 * {@link Modal#setSize(Modal.Size) size} or a {@link Modal#setWidth(String) width} keeps it.
 *
 * @param <T>
 *            the type of the object the wizard fills in
 * @since 10.11.0
 */
public class ModalWizard<T> extends Wizard<T> implements IModalCloseRequestHandler
{
	private static final long serialVersionUID = 1L;

	/**
	 * The attribute of the form, {@code true} when the wizard holds what was entered in the steps
	 * before, as the tabs and the windows of a desktop tell of changes not saved.
	 */
	public static final String MODIFIED_ATTRIBUTE = "data-modified";

	/**
	 * A wizard to open in a {@link Modal}, with the id of its content, {@link Modal#CONTENT_ID}.
	 *
	 * @param model
	 *            the object the wizard fills in
	 * @param firstStep
	 *            the first step
	 */
	public ModalWizard(IModel<T> model, IWizardStep<T> firstStep)
	{
		this(Modal.CONTENT_ID, model, firstStep);
	}

	/**
	 * @param id
	 *            the component id, {@link Modal#CONTENT_ID} for the content of a {@link Modal}
	 * @param model
	 *            the object the wizard fills in
	 * @param firstStep
	 *            the first step
	 */
	public ModalWizard(String id, IModel<T> model, IWizardStep<T> firstStep)
	{
		super(id, model, firstStep);
		setTitleLevel(1);
		getForm().add(AttributeModifier.replace(MODIFIED_ATTRIBUTE,
			() -> hasPreviousStep() ? "true" : null));
		getForm().add(new LookContainer("scroller", LookPart.SCROLLBAR));
		getHeader().add(ModalContent.newCloseButton("close"));
	}

	/**
	 * The parts of the dialog in place of the wizard's own: its header, title, body and footer,
	 * and no box around them, the dialog being one.
	 */
	@Override
	LookPart[] lookParts(LookPart... parts)
	{
		LookPart[] dialog = new LookPart[parts.length];
		for (int i = 0; i < parts.length; i++)
		{
			dialog[i] = parts[i] == null ? null : switch (parts[i])
			{
				case WIZARD -> null;
				case WIZARD_HEADER -> LookPart.DIALOG_HEADER;
				case WIZARD_TITLE -> LookPart.DIALOG_TITLE;
				case WIZARD_BODY -> LookPart.DIALOG_BODY;
				case WIZARD_BUTTONS_BOTTOM -> LookPart.DIALOG_FOOTER;
				default -> parts[i];
			};
		}
		return dialog;
	}

	/**
	 * Cancel closes the dialog as its close button does, through the browser, so that the function
	 * of {@link Modal#setCanClose(String)} is asked; then {@link #onCloseRequest(Modal,
	 * AjaxRequestTarget)} cancels.
	 */
	@Override
	Component newCancelButton(String id)
	{
		return new ModalCloseButton(id, new ResourceModel("Wizard.cancel"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(getCurrentStep().canCancel(getModel()));
			}
		}.setIcon(SvgIcon.XMARK)
			.setVariant(Variant.GHOST)
			.setSize(Size.MEDIUM)
			.add(AttributeModifier.append("class", WizardButtons.CLASS_PREFIX + "cancel"));
	}

	/**
	 * The user asked to close the dialog, with Cancel, the close button or Escape: cancels.
	 */
	@Override
	public void onCloseRequest(Modal modal, AjaxRequestTarget target)
	{
		cancel(target);
	}

	/**
	 * Called when the user cancels the wizard, with Cancel, the close button or Escape; closes the
	 * dialog by default.
	 *
	 * @param target
	 *            the Ajax request
	 */
	@Override
	protected void onCancel(AjaxRequestTarget target)
	{
		close(target);
	}

	/**
	 * Called when the wizard finishes, the object filled in; closes the dialog by default, so an
	 * override that does not call it keeps it open.
	 *
	 * @param target
	 *            the Ajax request
	 */
	@Override
	protected void onFinish(AjaxRequestTarget target)
	{
		close(target);
	}

	/**
	 * Closes the dialog the wizard is in.
	 *
	 * @param target
	 *            the Ajax request
	 */
	public void close(AjaxRequestTarget target)
	{
		Modal modal = getModal();
		if (modal != null)
		{
			modal.close(target);
		}
	}

	/**
	 * @return the dialog the wizard is in, {@code null} when it is in none
	 */
	public Modal getModal()
	{
		return findParent(Modal.class);
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		Modal modal = getModal();
		if (modal != null && modal.isOpen())
		{
			// the body repainted with the wizard, at each step, gets its scroll bars again
			Modal.renderHeadItems(response);
			response.render(OnDomReadyHeaderItem.forScript("Wicket.Modal.refresh(" +
				ScriptJson.quote(modal.getOverlayMarkupId()) + ");"));
		}
	}
}
