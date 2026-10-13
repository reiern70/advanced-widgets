/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.looks;

import java.time.LocalDate;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

import com.antilia.widgets.button.AjaxActionButton;
import com.antilia.widgets.button.ButtonBehavior.Size;
import com.antilia.widgets.button.ButtonBehavior.Variant;
import com.antilia.widgets.collapsible.CollapsiblePanel;
import com.antilia.widgets.datepicker.DatePicker;
import com.antilia.widgets.demo.BasePage;
import com.antilia.widgets.dropdown.Dropdown;
import com.antilia.widgets.icon.IIcon;
import com.antilia.widgets.icon.SvgIcon;
import com.antilia.widgets.modal.IModalButtonFactory;
import com.antilia.widgets.modal.Modal;
import com.antilia.widgets.notification.Notification;
import com.antilia.widgets.progress.ProgressBar;

/**
 * The same components in each look of the {@link LooksPage}: a button of every variant and small
 * buttons showing only their icon, a drop-down, a date picker, a progress bar, a collapsible panel
 * and a modal dialog. The look is that of the column the sample is in.
 */
class LookSample extends Panel
{
	private static final long serialVersionUID = 1L;

	private final Modal dialog;

	LookSample(String id)
	{
		super(id);
		add(button("primary", Variant.PRIMARY, Size.MEDIUM, SvgIcon.FLOPPY_DISK, false));
		add(button("secondary", Variant.SECONDARY, Size.MEDIUM, SvgIcon.ARROWS_ROTATE, false));
		add(button("ghost", Variant.GHOST, Size.MEDIUM, null, false));
		add(button("danger", Variant.DANGER, Size.MEDIUM, SvgIcon.TRASH_CAN, false));
		add(button("smallSave", Variant.PRIMARY, Size.SMALL, SvgIcon.FLOPPY_DISK, true));
		add(button("smallSettings", Variant.SECONDARY, Size.SMALL, SvgIcon.GEAR, true));
		add(button("smallDelete", Variant.DANGER, Size.SMALL, SvgIcon.TRASH_CAN, true));

		Form<Void> form = new Form<>("form");
		add(form);
		form.add(new Dropdown<>("fruit", Model.of("Pear"),
			List.of("Apple", "Banana", "Cherry", "Pear", "Plum")));
		form.add(new DatePicker("date", Model.of(LocalDate.of(2026, 10, 12))));

		add(new ProgressBar("progress", Model.of(62)));
		add(new CollapsiblePanel("collapsible", new ResourceModel("collapsible.title"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newBody(String id)
			{
				return new Label(id, new ResourceModel("collapsible.body"));
			}
		}.setExpanded(true));

		dialog = new Modal("dialog");
		add(dialog);
		add(new AjaxActionButton("openDialog", new ResourceModel("dialog.open"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				dialog.open(new ResourceModel("dialog.title"),
					new Label(Modal.BODY_ID, new ResourceModel("dialog.body")), target,
					IModalButtonFactory.close(new ResourceModel("dialog.close")));
			}
		});
	}

	/**
	 * @return a button telling, in a notification, that it was clicked
	 */
	private AjaxActionButton button(String id, Variant variant, Size size, IIcon icon,
		boolean iconOnly)
	{
		AjaxActionButton button = new AjaxActionButton(id, new ResourceModel("button." + id))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				((BasePage)getPage()).getNotifications().show(Notification.info(
					getString("clicked").replace("{0}", getString("button." + id))));
			}
		};
		button.setVariant(variant).setSize(size).setIcon(icon).setIconOnly(iconOnly);
		return button;
	}
}
