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
package com.antiliacode.widgets.table;

/**
 * Marks a column the user can move, in a table with a {@link MovableColumnsBehavior}: its header
 * gets a handle to drag it to another place, or to move it with the arrow keys. Columns without
 * the mark keep their places. In a
 * {@link
 * com.antiliacode.widgets.dynamicdatatable.BaseDynamicDataTable} a
 * {@link
 * com.antiliacode.widgets.dynamicdatatable.SortableDynamicColumn}
 * is movable if the column it wraps is.
 *
 * @since 11.0.0
 */
public interface IMovableColumn
{
}
