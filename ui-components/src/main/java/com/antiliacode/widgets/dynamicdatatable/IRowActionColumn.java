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
package com.antiliacode.widgets.dynamicdatatable;

import java.util.List;

/**
 * A column whose cells carry row actions, such as a {@link BeanActionsColumn} or an
 * {@link ActionLinkColumn}: a table given the column registers its
 * {@link #getActionContributors() contributors}, as with
 * {@link BaseDynamicDataTable#addActionContributor(IAjaxActionColumnContributor)}, and dispatches
 * their clicks to them by the row's key.
 *
 * @param <T>
 *            the type of the rows
 * @since 11.0.0
 */
public interface IRowActionColumn<T>
{
	/**
	 * @return the actions the table registers for the column, empty for a column using actions
	 *         registered otherwise
	 */
	List<? extends IAjaxActionColumnContributor<T>> getActionContributors();
}
