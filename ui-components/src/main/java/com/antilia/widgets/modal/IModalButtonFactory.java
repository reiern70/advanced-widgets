/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.modal;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.lang.Args;
import org.danekja.java.util.function.serializable.SerializableConsumer;

/**
 * Makes a button of the footer of a {@link Modal}, opened with
 * {@link Modal#open(IModel, Component, AjaxRequestTarget, IModalButtonFactory...)}. The button is
 * rendered on a {@code <button type="button" class="modal-dialog-button">}: a
 * {@link ModalButton}, a {@link ModalSubmitButton} submitting a form of the body, a
 * {@link ModalCloseButton}, or any component fit for that tag. The factories here make the common
 * ones:
 *
 * <pre>
 * modal.open(title, form, target,
 * 	IModalButtonFactory.close(new ResourceModel("cancel")),
 * 	id -&gt; new ModalSubmitButton(id, new ResourceModel("save"), form) {...}.setPrimary(true));
 * </pre>
 *
 * @since 11.0.0
 */
@FunctionalInterface
public interface IModalButtonFactory extends IClusterable
{
	/**
	 * @param id
	 *            the id the button must have
	 * @return the button
	 */
	Component createButton(String id);

	/**
	 * @param label
	 *            the text of the button
	 * @return the factory of a button closing the dialog as its close button does, if the
	 *         function of {@link Modal#setCanClose(String)} lets it, in the browser
	 */
	static IModalButtonFactory close(IModel<String> label)
	{
		Args.notNull(label, "label");
		return id -> new ModalCloseButton(id, label);
	}

	/**
	 * @param label
	 *            the text of the button
	 * @param onClick
	 *            what a click on the button does, in an Ajax request
	 * @return the factory of a button
	 */
	static IModalButtonFactory button(IModel<String> label,
		SerializableConsumer<AjaxRequestTarget> onClick)
	{
		return button(label, false, onClick);
	}

	/**
	 * @param label
	 *            the text of the button
	 * @param onClick
	 *            what a click on the button does, in an Ajax request
	 * @return the factory of the primary button, in the color of the header
	 */
	static IModalButtonFactory primary(IModel<String> label,
		SerializableConsumer<AjaxRequestTarget> onClick)
	{
		return button(label, true, onClick);
	}

	private static IModalButtonFactory button(IModel<String> label, boolean primary,
		SerializableConsumer<AjaxRequestTarget> onClick)
	{
		Args.notNull(label, "label");
		Args.notNull(onClick, "onClick");
		return id -> new ModalButton(id, label)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				onClick.accept(target);
			}
		}.setPrimary(primary);
	}
}
