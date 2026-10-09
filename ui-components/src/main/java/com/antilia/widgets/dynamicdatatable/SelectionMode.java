/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.dynamicdatatable;

/**
 * How many rows of a {@link BaseDynamicDataTable} the user can select with its selection column,
 * a {@link SelectionColumn} or a {@link BeanSelectionColumn}.
 */
public enum SelectionMode
{
	/** No row: the selection column is not shown, and neither is the selection bar. */
	NONE,

	/**
	 * One row at a time: selecting a row deselects the one selected before. The header has no
	 * checkbox and the selection bar is not shown.
	 */
	ONE,

	/**
	 * Any number of rows: the header checkbox selects the rows of the page, and the selection bar
	 * offers to select all rows of the provider.
	 */
	MANY
}
