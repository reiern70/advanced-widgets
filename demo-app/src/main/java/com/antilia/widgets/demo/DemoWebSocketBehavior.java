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
package com.antilia.widgets.demo;

import java.util.Map;

import org.apache.wicket.protocol.ws.api.WebSocketBehavior;

/**
 * The web socket behavior of the demos, which connects with either {@link AjaxEngine}.
 * <p>
 * The setup script of Wicket's web socket support calls {@code jQuery.extend(...)}, so with the
 * plain JavaScript engine, where there is no jQuery, the page never connects. This behavior sets
 * the same values with {@code Object.assign(...)}, which needs no library.
 */
public abstract class DemoWebSocketBehavior extends WebSocketBehavior
{
	private static final long serialVersionUID = 1L;

	@Override
	protected String getWebSocketSetupScript(Map<String, Object> parameters)
	{
		return super.getWebSocketSetupScript(parameters).replace("jQuery.extend(",
			"Object.assign(");
	}
}
