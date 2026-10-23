/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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
package com.antiliacode.widgets.dynamicdatatable;

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
