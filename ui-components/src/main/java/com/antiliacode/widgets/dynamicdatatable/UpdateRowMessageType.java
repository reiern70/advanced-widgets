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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.util.lang.Args;

/**
 * Messages of this type repaint a single row of the table from the data they carry, leaving the
 * other rows alone. The row is found by its key; a message for a row the browser does not show,
 * for example one on another page, is ignored. Every {@link JsonBasedDynamicDataTable} registers
 * it; {@link JsonBasedDynamicDataTable#newUpdateRowMessage(Object)} builds such a message, with
 * the actions that are not {@link IAjaxActionColumnContributor#isVisible(Object) visible} or not
 * {@link IAjaxActionColumnContributor#isEnabled(Object) enabled} on the row, and
 * {@link #newMessage(String, String, Object, List, Map)} builds one without the table, for example
 * on a background thread.
 *
 * @since 11.0.0
 */
public final class UpdateRowMessageType implements IWebSocketMessageType
{
	private static final long serialVersionUID = 1L;

	/** The shared instance. */
	public static final UpdateRowMessageType INSTANCE = new UpdateRowMessageType();

	/** The id of the type. */
	public static final String TYPE_ID = "updateRow";

	private UpdateRowMessageType()
	{
	}

	/**
	 * Builds a message repainting a row with all of its actions, sent as {@code tableId},
	 * {@code typeId}, {@code key}, {@code data}, an empty {@code hiddenActions} and an empty
	 * {@code disabledActions}. Every property the serializer produces for {@code data} reaches the
	 * browser.
	 *
	 * @param tableId
	 *            the markup id of the table
	 * @param key
	 *            the key of the row as the browser shows it, see
	 *            {@link JsonBasedDynamicDataTable#getRowKey(Object)}
	 * @param data
	 *            the row, serialized when the message is sent
	 * @return the message
	 * @see #newMessage(String, String, Object, List, Map)
	 */
	public static IWebSocketLightWeightMessage newMessage(String tableId, String key, Object data)
	{
		return newMessage(tableId, key, data, List.of(), Map.of());
	}

	/**
	 * Builds a message repainting a row, sent as {@code tableId}, {@code typeId}, {@code key},
	 * {@code data}, {@code hiddenActions} and {@code disabledActions}. Every property the
	 * serializer produces for {@code data} reaches the browser, which removes the elements of the
	 * row carrying the hidden actions and disables those carrying the disabled ones.
	 *
	 * @param tableId
	 *            the markup id of the table
	 * @param key
	 *            the key of the row as the browser shows it, see
	 *            {@link JsonBasedDynamicDataTable#getRowKey(Object)}
	 * @param data
	 *            the row, serialized when the message is sent
	 * @param hiddenActions
	 *            the actions left out of the row, see
	 *            {@link JsonBasedDynamicDataTable#getHiddenActions(Object)}
	 * @param disabledActions
	 *            the actions disabled on the row, with the reason of each, empty for none, see
	 *            {@link JsonBasedDynamicDataTable#getDisabledActions(Object)}
	 * @return the message
	 */
	public static IWebSocketLightWeightMessage newMessage(String tableId, String key, Object data,
		List<String> hiddenActions, Map<String, String> disabledActions)
	{
		return new JsonBasedDynamicDataTable.UpdateRowMessage(Args.notEmpty(tableId, "tableId"),
			Args.notNull(key, "key"), data,
			List.copyOf(Args.notNull(hiddenActions, "hiddenActions")),
			new LinkedHashMap<>(Args.notNull(disabledActions, "disabledActions")));
	}

	@Override
	public String getTypeId()
	{
		return TYPE_ID;
	}

	@Override
	public String getTypeFunction()
	{
		return "function (message) { " +
			"Wicket.DynamicDataTable.instances[message.tableId].renderRow(message.key, " +
			"message.data, message); }";
	}
}
