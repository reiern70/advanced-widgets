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
package com.antilia.widgets.theme;

import org.apache.wicket.markup.html.TransparentWebMarkupContainer;

/**
 * An element of the markup of a component that is {@link LookPart parts} of a look and nothing
 * else: a wrapper or a decoration, such as the frame of a panel. It is transparent: the components
 * inside its markup stay children of its parent.
 *
 * @since 11.0.0
 */
public class LookContainer extends TransparentWebMarkupContainer
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param id
	 *            component id
	 * @param parts
	 *            the parts the element is
	 */
	public LookContainer(String id, LookPart... parts)
	{
		super(id);
		add(new LookBehavior(parts));
	}
}
