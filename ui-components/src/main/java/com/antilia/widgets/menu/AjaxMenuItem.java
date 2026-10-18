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
package com.antilia.widgets.menu;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;
import org.danekja.java.util.function.serializable.SerializableConsumer;

/**
 * An item calling the server via Ajax, by an {@link AjaxLink}: {@link #onClick(AjaxRequestTarget)}
 * does what it does, the page staying as it is but for what the target renders anew.
 *
 * @since 11.0.0
 */
public abstract class AjaxMenuItem extends MenuItem
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param label
	 *            the label of the item
	 */
	public AjaxMenuItem(IModel<String> label)
	{
		super(label);
	}

	/**
	 * Called when the user chose the item.
	 *
	 * @param target
	 *            the Ajax request target
	 */
	protected abstract void onClick(AjaxRequestTarget target);

	@Override
	public AbstractLink getLink(String id)
	{
		return new AjaxLink<Void>(id)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				AjaxMenuItem.this.onClick(target);
			}
		};
	}

	/**
	 * @param label
	 *            the label of the item
	 * @param onClick
	 *            what the item does, given the Ajax request target
	 * @return an item calling the server via Ajax
	 */
	public static AjaxMenuItem of(IModel<String> label, SerializableConsumer<AjaxRequestTarget> onClick)
	{
		Args.notNull(onClick, "onClick");
		return new AjaxMenuItem(label)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onClick(AjaxRequestTarget target)
			{
				onClick.accept(target);
			}
		};
	}
}
