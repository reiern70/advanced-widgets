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
package com.antiliacode.widgets.demo.modal;

import java.util.List;

import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.string.Strings;

import com.antiliacode.widgets.modal.Modal;

/**
 * The options of the dialogs of the options example, as its form sets them: what closes them,
 * whether they drag and fold, and how wide they are.
 */
public class DialogOptions implements IClusterable
{
	private static final long serialVersionUID = 1L;

	/** The sizes the form offers: fitting the content, then those of {@link Modal.Size}. */
	public static final List<String> SIZES = List.of("fit", "small", "medium", "large");

	/** How deep the dialogs nest: a dialog of this level opens no other. */
	public static final int MAX_LEVEL = 3;

	/** The widths the form takes: a number and a CSS unit. */
	public static final String WIDTH_PATTERN = "\\d+(\\.\\d+)?(em|rem|px|%|vw|ch)";

	private boolean closeOnEscape = true;

	private boolean closeButton = true;

	private boolean closeOnClick;

	private boolean draggable = true;

	private boolean foldable = true;

	private String size = "medium";

	private String width;

	/**
	 * Gives a dialog these options.
	 *
	 * @param modal
	 *            the dialog
	 * @return the dialog
	 */
	public Modal applyTo(Modal modal)
	{
		return modal.setCloseOnEscape(closeOnEscape)
			.setCloseButton(closeButton)
			.setCloseOnClick(closeOnClick)
			.setDraggable(draggable)
			.setFoldable(foldable)
			.setSize("fit".equals(size) ? null : Modal.Size.valueOf(size.toUpperCase()))
			.setWidth(Strings.isEmpty(width) ? null : width);
	}

	public boolean isCloseOnEscape()
	{
		return closeOnEscape;
	}

	public void setCloseOnEscape(boolean closeOnEscape)
	{
		this.closeOnEscape = closeOnEscape;
	}

	public boolean isCloseButton()
	{
		return closeButton;
	}

	public void setCloseButton(boolean closeButton)
	{
		this.closeButton = closeButton;
	}

	public boolean isCloseOnClick()
	{
		return closeOnClick;
	}

	public void setCloseOnClick(boolean closeOnClick)
	{
		this.closeOnClick = closeOnClick;
	}

	public boolean isDraggable()
	{
		return draggable;
	}

	public void setDraggable(boolean draggable)
	{
		this.draggable = draggable;
	}

	public boolean isFoldable()
	{
		return foldable;
	}

	public void setFoldable(boolean foldable)
	{
		this.foldable = foldable;
	}

	public String getSize()
	{
		return size;
	}

	public void setSize(String size)
	{
		this.size = size;
	}

	public String getWidth()
	{
		return width;
	}

	public void setWidth(String width)
	{
		this.width = width;
	}
}
