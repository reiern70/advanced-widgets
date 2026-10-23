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

import java.io.Serializable;

/**
 * An object that knows its own key, for example the primary key of an entity.
 * <p>
 * The rows of an {@link IIdentifiableDynamicDataProvider} implement it, which takes their key from
 * {@link #getId()}. The object itself does not have to be serializable, its key does.
 *
 * @param <K>
 *            the type of the key
 * @since 11.0.0
 */
public interface IIdentifiable<K extends Serializable>
{
	/**
	 * @return the key, never {@code null}, unique among the objects of its kind and the same for
	 *         as long as the object exists
	 */
	K getId();
}
