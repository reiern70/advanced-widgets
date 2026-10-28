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
package com.antiliacode.widgets.event;

import org.apache.wicket.util.lang.Args;

/**
 * An event staying in the browser, which runs its function there: an {@link IClientEvent} made
 * ready.
 *
 * <pre>
 * editor.addEvent(new ClientEvent(RichTextEditorEvents.MODIFIED,
 * 	"function(event) { document.title = (event.detail.modified ? '* ' : '') + 'Article'; }"));
 * </pre>
 *
 * @since 11.0.0
 */
public class ClientEvent extends AbstractClientEvent<ClientEvent>
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param id
	 *            the name of the event in the browser
	 * @param clientFunction
	 *            the JavaScript function the browser runs on the event,
	 *            {@code function(event, component, send) {...}}
	 */
	public ClientEvent(String id, String clientFunction)
	{
		super(id, Args.notEmpty(clientFunction, "clientFunction"));
	}

	@Override
	public ClientEvent setClientFunction(String clientFunction)
	{
		return super.setClientFunction(Args.notEmpty(clientFunction, "clientFunction"));
	}
}
