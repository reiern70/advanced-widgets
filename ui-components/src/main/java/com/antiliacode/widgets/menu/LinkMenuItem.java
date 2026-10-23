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
package com.antiliacode.widgets.menu;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;
import org.danekja.java.misc.serializable.SerializableRunnable;

/**
 * An item calling the server with a request of its own, by a {@link Link}: {@link #onClick()} does
 * what it does, then the page renders again, or the one it set as the response.
 *
 * @since 11.0.0
 */
public abstract class LinkMenuItem extends MenuItem
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param label
	 *            the label of the item
	 */
	public LinkMenuItem(IModel<String> label)
	{
		super(label);
	}

	/**
	 * Called when the user chose the item.
	 */
	protected abstract void onClick();

	@Override
	public Component getLink(String id)
	{
		return new Link<Void>(id)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick()
			{
				LinkMenuItem.this.onClick();
			}
		};
	}

	/**
	 * @param label
	 *            the label of the item
	 * @param onClick
	 *            what the item does
	 * @return an item calling the server with a request of its own
	 */
	public static LinkMenuItem of(IModel<String> label, SerializableRunnable onClick)
	{
		Args.notNull(onClick, "onClick");
		return new LinkMenuItem(label)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onClick()
			{
				onClick.run();
			}
		};
	}
}
