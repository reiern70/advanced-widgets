/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.datatable;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.danekja.java.util.function.serializable.SerializableConsumer;
import org.danekja.java.util.function.serializable.SerializableFunction;
import org.danekja.java.util.function.serializable.SerializableSupplier;

import com.antilia.widgets.dropdown.Dropdown;
import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable;
import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable.BodyHeightUnits;
import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable.OverlayPlacement;
import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable.ToolbarPosition;
import com.antilia.widgets.dynamicdatatable.ColumnState;
import com.antilia.widgets.dynamicdatatable.IColumnStateStore;
import com.antilia.widgets.dynamicdatatable.JsonBasedDynamicDataTable;
import com.antilia.widgets.dynamicdatatable.JsonBasedDynamicDataTable.RowsSource;
import com.antilia.widgets.dynamicdatatable.SelectionMode;
import com.antilia.widgets.dynamicdatatable.SelectionCountToolbarAction;
import com.antilia.widgets.dynamicdatatable.SessionColumnStateStore;
import com.antilia.widgets.table.ResizableColumnsBehavior;

/**
 * The settings of the table of a dynamic table example, the same for the JSON based and the bean
 * based table. Every change is posted via Ajax and renders the table again. Each setting has an
 * (i) whose tooltip says what it does.
 */
public abstract class DynamicTableOptionsPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	private final BaseDynamicDataTable<User, Long, ?> table;

	private final ResizableColumnsBehavior resizableColumns;

	/**
	 * @param id
	 *            component id
	 * @param table
	 *            the table the settings are of
	 * @param resizableColumns
	 *            the behavior resizing the columns, added to the table or not
	 * @param icons
	 *            the icons of the actions
	 * @param proportions
	 *            the proportions of the columns offered by the setting, one per column of the
	 *            table
	 * @param selectionCount
	 *            the count of the selected rows among the actions of the table, shown or not
	 */
	public DynamicTableOptionsPanel(String id, BaseDynamicDataTable<User, Long, ?> table,
		ResizableColumnsBehavior resizableColumns, IModel<ActionIcons> icons, double[] proportions,
		SelectionCountToolbarAction<User, Long> selectionCount)
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
			set -> proportions(set ? proportions : null),
			set -> set ? "proportions.set" : "proportions.content"));
		add(updatingChoice("navigation", List.of(ToolbarPosition.values()),
			table::getNavigationPosition, table::setNavigationPosition,
			position -> "navigation." + position.name()));
		WebMarkupContainer infiniteScrollOption = new WebMarkupContainer("infiniteScrollOption");
		add(infiniteScrollOption);
		WebMarkupContainer rowsSourceOption = new WebMarkupContainer("rowsSourceOption");
		add(rowsSourceOption);
		if (table instanceof JsonBasedDynamicDataTable<?, ?> jsonTable)
		{
			infiniteScrollOption.add(updatingChoice("infiniteScroll", List.of(false, true),
				jsonTable::isInfiniteScroll, jsonTable::setInfiniteScroll,
				infinite -> infinite ? "infiniteScroll.on" : "infiniteScroll.off"));
			rowsSourceOption.add(updatingChoice("rowsSource", List.of(RowsSource.values()),
				jsonTable::getRowsSource, jsonTable::setRowsSource,
				source -> "rowsSource." + source.name()));
			rowsSourceOption.setVisible(jsonTable.getRowsResource() != null);
		}
		else
		{
			infiniteScrollOption.setVisible(false);
			infiniteScrollOption.add(new WebMarkupContainer("infiniteScroll"));
			rowsSourceOption.setVisible(false);
			rowsSourceOption.add(new WebMarkupContainer("rowsSource"));
		}
		add(updatingChoice("selection", List.of(SelectionMode.values()), table::getSelectionMode,
			table::setSelectionMode, mode -> "selection." + mode.name()));
		add(updatingChoice("selectionCount", List.of(true, false), selectionCount::isShown,
			selectionCount::setShown,
			shown -> shown ? "selectionCount.shown" : "selectionCount.hidden"));
		add(updatingChoice("clearSelectionOnNavigation", List.of(false, true),
			table::isClearSelectionOnNavigation, table::setClearSelectionOnNavigation,
			clear -> clear ? "clearSelectionOnNavigation.on" : "clearSelectionOnNavigation.off"));
		add(updatingChoice("selectionBar", List.of(ToolbarPosition.values()),
			table::getSelectionToolbarPosition, table::setSelectionToolbarPosition,
			position -> "navigation." + position.name()));
		Dropdown<BodyHeightUnits> units = choice("bodyHeightUnits",
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
		WebMarkupContainer overlayOption = new WebMarkupContainer("overlayOption")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(isOverlayUsed());
			}
		};
		add(overlayOption);
		overlayOption.add(updatingChoice("overlay", List.of(OverlayPlacement.values()),
			table::getOverlayPlacement, table::setOverlayPlacement,
			placement -> "overlay." + placement.name()));
		add(updatingChoice("icons", List.of(ActionIcons.values()), icons::getObject,
			icons::setObject, set -> "icons." + set.name()));
		add(updatingChoice("columnState", List.of(false, true),
			() -> table.getColumnStateStore() == FileColumnStateStore.INSTANCE,
			this::keepColumnStateInFile,
			file -> file ? "columnState.file" : "columnState.session"));
	}

	/**
	 * Gives the columns the order, visibility and widths the page starts with, keeps that in the
	 * store of the column state and renders the table again.
	 *
	 * @param target
	 *            the Ajax request target
	 */
	public void resetColumns(AjaxRequestTarget target)
	{
		table.setColumnState(getInitialColumnState()).setColumnWidths((double[])null);
		table.getColumnStateStore().save(table.getColumnStateKey(), table.getColumnState());
		onTableChanged(target);
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
	 * @return whether the page shows the edit form and the confirmation in the table's overlay,
	 *         so that the setting of its placement applies; {@code true} by default
	 */
	protected boolean isOverlayUsed()
	{
		return true;
	}

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

	private <V> Dropdown<V> updatingChoice(String id, List<V> values,
		SerializableSupplier<V> getter, SerializableConsumer<V> setter,
		SerializableFunction<V, String> key)
	{
		Dropdown<V> choice = choice(id, values, getter, setter, key);
		choice.add(updating());
		return choice;
	}

	/**
	 * @return a drop-down choosing one of the values of a table setting
	 */
	private <V> Dropdown<V> choice(String id, List<V> values, SerializableSupplier<V> getter,
		SerializableConsumer<V> setter, SerializableFunction<V, String> key)
	{
		Dropdown<V> choice = new Dropdown<>(id, LambdaModel.of(getter, setter), values,
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
