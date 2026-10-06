/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.datatable;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.danekja.java.util.function.serializable.SerializableConsumer;
import org.danekja.java.util.function.serializable.SerializableFunction;
import org.danekja.java.util.function.serializable.SerializableSupplier;

import com.advanced.widgets.dynamicdatatable.BaseDynamicDataTable;
import com.advanced.widgets.dynamicdatatable.BaseDynamicDataTable.BodyHeightUnits;
import com.advanced.widgets.dynamicdatatable.BaseDynamicDataTable.OverlayPlacement;
import com.advanced.widgets.dynamicdatatable.BaseDynamicDataTable.ToolbarPosition;
import com.advanced.widgets.dynamicdatatable.ColumnState;
import com.advanced.widgets.dynamicdatatable.IColumnStateStore;
import com.advanced.widgets.dynamicdatatable.SessionColumnStateStore;
import com.advanced.widgets.table.ResizableColumnsBehavior;
import com.advanced.widgets.theme.Theme;
import com.advanced.widgets.theme.ThemeChoice;

/**
 * The settings of the table of a dynamic table example, the same for the JSON based and the bean
 * based table. Every change is posted via Ajax and renders the table again. Each setting has an
 * (i) whose tooltip says what it does.
 */
public abstract class DynamicTableOptionsPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	private static final double[] PROPORTIONS = { 0.4, 0.4, 1.1, 0.5, 1.1, 1.1, 0.9, 2.2, 1.8,
			1.3, 1.1, 1.1, 1.0, 1.0 };

	private final BaseDynamicDataTable<Contact, Long, ?> table;

	private final ResizableColumnsBehavior resizableColumns;

	/**
	 * @param id
	 *            component id
	 * @param table
	 *            the table the settings are of
	 * @param resizableColumns
	 *            the behavior resizing the columns, added to the table or not
	 * @param theme
	 *            the theme of the table
	 * @param icons
	 *            the icons of the actions
	 */
	public DynamicTableOptionsPanel(String id, BaseDynamicDataTable<Contact, Long, ?> table,
		ResizableColumnsBehavior resizableColumns, IModel<Theme> theme, IModel<ActionIcons> icons)
	{
		super(id);
		this.table = table;
		this.resizableColumns = resizableColumns;
		setOutputMarkupId(true);

		add(updatingChoice("resize", List.of(false, true),
			() -> table.getBehaviors().contains(resizableColumns), this::resizable,
			resizable -> resizable ? "resize.STRETCH" : "resize.NONE"));
		add(updatingChoice("width", List.of(false, true), table::isAdjustToParentWidth,
			this::adjustToParentWidth, parent -> parent ? "width.parent" : "width.content"));
		add(updatingChoice("proportions", List.of(false, true),
			() -> table.getColumnProportions() != null,
			set -> proportions(set ? PROPORTIONS : null),
			set -> set ? "proportions.set" : "proportions.content"));
		add(updatingChoice("navigation", List.of(ToolbarPosition.values()),
			table::getNavigationPosition, table::setNavigationPosition,
			position -> "navigation." + position.name()));
		add(updatingChoice("selectionBar", List.of(ToolbarPosition.values()),
			table::getSelectionToolbarPosition, table::setSelectionToolbarPosition,
			position -> "navigation." + position.name()));
		DropDownChoice<BodyHeightUnits> units = choice("bodyHeightUnits",
			List.of(BodyHeightUnits.values()), table::getBodyHeightUnits,
			unit -> table.setBodyHeight(defaultHeight(unit), unit),
			unit -> "bodyHeightUnits." + unit.name());
		add(units);
		NumberTextField<Integer> height = new NumberTextField<>("bodyHeight",
			LambdaModel.of(table::getBodyHeight, value -> table.setBodyHeight(
				value == 0 ? -1 : value, table.getBodyHeightUnits())), Integer.class);
		add(height.setRequired(true).setOutputMarkupId(true).add(updating()));
		units.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				target.add(height);
				onTableChanged(target);
			}
		});
		add(updatingChoice("overlay", List.of(OverlayPlacement.values()),
			table::getOverlayPlacement, table::setOverlayPlacement,
			placement -> "overlay." + placement.name()));
		add(updatingChoice("icons", List.of(ActionIcons.values()), icons::getObject,
			icons::setObject, set -> "icons." + set.name()));
		add(updatingChoice("columnState", List.of(false, true),
			() -> table.getColumnStateStore() == FileColumnStateStore.INSTANCE,
			this::keepColumnStateInFile,
			file -> file ? "columnState.file" : "columnState.session"));
		add(new ThemeChoice("theme", theme)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onThemeChanged(AjaxRequestTarget target)
			{
				DynamicTableOptionsPanel.this.onThemeChanged(target);
			}
		});
		add(new AjaxLink<Void>("resetColumns")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				table.setColumnState(getInitialColumnState()).setColumnWidths((double[])null);
				table.getColumnStateStore().save(table.getColumnStateKey(), table.getColumnState());
				onTableChanged(target);
			}
		});
	}

	/**
	 * @return the height of the body when its unit is chosen: 800 pixels, or all of the parent or
	 *         the window
	 */
	static int defaultHeight(BodyHeightUnits units)
	{
		return units == BodyHeightUnits.PIXELS ? 800 : 100;
	}

	/**
	 * @return the order, visibility and widths of the columns the page starts with
	 */
	protected abstract ColumnState getInitialColumnState();

	/**
	 * Renders the table again after a setting changed.
	 *
	 * @param target
	 *            the Ajax request target
	 */
	protected abstract void onTableChanged(AjaxRequestTarget target);

	/**
	 * Renders what takes the colors of the theme again after the theme changed.
	 *
	 * @param target
	 *            the Ajax request target
	 */
	protected abstract void onThemeChanged(AjaxRequestTarget target);

	private void resizable(boolean resizable)
	{
		if (resizable && table.getBehaviors().contains(resizableColumns) == false)
		{
			table.add(resizableColumns);
		}
		else if (resizable == false && table.getBehaviors().contains(resizableColumns))
		{
			table.remove(resizableColumns);
		}
	}

	private void adjustToParentWidth(boolean adjust)
	{
		table.setAdjustToParentWidth(adjust).setColumnWidths((double[])null);
	}

	private void proportions(double[] proportions)
	{
		table.setColumnProportions(proportions).setColumnWidths((double[])null);
	}

	/**
	 * Switches the store of the table's column state, taking the state the new store holds, if
	 * any, or else saving the current one in it.
	 */
	private void keepColumnStateInFile(boolean file)
	{
		IColumnStateStore store = file ? FileColumnStateStore.INSTANCE
			: SessionColumnStateStore.INSTANCE;
		table.setColumnStateStore(store);
		ColumnState state = store.load(table.getColumnStateKey());
		if (state != null)
		{
			table.setColumnState(state);
		}
		else
		{
			store.save(table.getColumnStateKey(), table.getColumnState());
		}
	}

	private AjaxFormComponentUpdatingBehavior updating()
	{
		return new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				onTableChanged(target);
			}
		};
	}

	private <V> DropDownChoice<V> updatingChoice(String id, List<V> values,
		SerializableSupplier<V> getter, SerializableConsumer<V> setter,
		SerializableFunction<V, String> key)
	{
		DropDownChoice<V> choice = choice(id, values, getter, setter, key);
		choice.add(updating());
		return choice;
	}

	/**
	 * @return a drop-down choosing one of the values of a table setting
	 */
	private <V> DropDownChoice<V> choice(String id, List<V> values, SerializableSupplier<V> getter,
		SerializableConsumer<V> setter, SerializableFunction<V, String> key)
	{
		DropDownChoice<V> choice = new DropDownChoice<>(id, LambdaModel.of(getter, setter), values,
			new ChoiceRenderer<>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public Object getDisplayValue(V value)
				{
					return getString(key.apply(value));
				}

				@Override
				public String getIdValue(V value, int index)
				{
					return String.valueOf(index);
				}
			});
		choice.setRequired(true);
		return choice;
	}
}
