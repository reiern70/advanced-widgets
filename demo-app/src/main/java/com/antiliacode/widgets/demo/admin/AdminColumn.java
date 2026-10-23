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
package com.antiliacode.widgets.demo.admin;

import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.model.ResourceModel;
import org.danekja.java.util.function.serializable.SerializableFunction;

import com.antiliacode.widgets.dynamicdatatable.IHideableColumn;
import com.antiliacode.widgets.table.IMovableColumn;

/**
 * A value of a row of the administration's tables, written escaped, in a column the
 * administrator can hide and move; sorted by the database when it has a sort property.
 *
 * @param <T>
 *            the type of the rows
 */
class AdminColumn<T> extends LambdaColumn<T, String> implements IHideableColumn, IMovableColumn
{
	private static final long serialVersionUID = 1L;

	private final String headerKey;

	/**
	 * @param headerKey
	 *            the key of the header in the package's bundle
	 * @param sortProperty
	 *            the property of the entity sorted by, {@code null} if the column is not sorted
	 * @param value
	 *            the value of a row
	 */
	AdminColumn(String headerKey, String sortProperty, SerializableFunction<T, ?> value)
	{
		super(new ResourceModel(headerKey), sortProperty, value);
		this.headerKey = headerKey;
	}

	/**
	 * @return the key of the header in the package's bundle
	 */
	String getHeaderKey()
	{
		return headerKey;
	}
}
