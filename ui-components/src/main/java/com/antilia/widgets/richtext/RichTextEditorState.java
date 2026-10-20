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
package com.antilia.widgets.richtext;

import java.io.Serializable;
import java.util.Objects;

import com.github.openjson.JSONException;
import com.github.openjson.JSONObject;

import com.antilia.widgets.resizable.ResizableSize;

/**
 * The state of a {@link RichTextEditor} besides its text: what is selected, from its anchor to
 * its head (the cursor), how far the text is scrolled, in pixels, and the size the user gave the
 * editor. The positions are ProseMirror's, which count the places between the paragraphs, the
 * items of lists and the like as well as the characters: they mean something to the editor, which
 * takes them up again, more than to the server. Immutable; the browser sends it as JSON.
 *
 * @since 11.0.0
 */
public final class RichTextEditorState implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** No state: the cursor at the start, no scrolling and the size of the page. */
	public static final RichTextEditorState NONE = new RichTextEditorState(0, 0, 0, 0,
		ResizableSize.NONE);

	private final int anchor;

	private final int head;

	private final int scrollTop;

	private final int scrollLeft;

	private final ResizableSize size;

	/**
	 * @param anchor
	 *            where the selection starts
	 * @param head
	 *            where it ends, the cursor
	 * @param scrollTop
	 *            how far the text is scrolled down, in pixels
	 * @param scrollLeft
	 *            how far it is scrolled to the right, in pixels
	 * @param size
	 *            the size the user gave the editor, {@link ResizableSize#NONE} or {@code null}
	 *            for the page's
	 */
	public RichTextEditorState(int anchor, int head, int scrollTop, int scrollLeft,
		ResizableSize size)
	{
		this.anchor = Math.max(anchor, 0);
		this.head = Math.max(head, 0);
		this.scrollTop = Math.max(scrollTop, 0);
		this.scrollLeft = Math.max(scrollLeft, 0);
		this.size = size == null ? ResizableSize.NONE : size;
	}

	/**
	 * @return a state selecting from an anchor to a head, unscrolled, in the size of the page
	 */
	public static RichTextEditorState selecting(int anchor, int head)
	{
		return new RichTextEditorState(anchor, head, 0, 0, ResizableSize.NONE);
	}

	/**
	 * @return where the selection starts
	 */
	public int getAnchor()
	{
		return anchor;
	}

	/**
	 * @return where the selection ends: the cursor
	 */
	public int getHead()
	{
		return head;
	}

	/**
	 * @return whether nothing is selected: a cursor
	 */
	public boolean isCursor()
	{
		return anchor == head;
	}

	/**
	 * @return how far the text is scrolled down, in pixels
	 */
	public int getScrollTop()
	{
		return scrollTop;
	}

	/**
	 * @return how far the text is scrolled to the right, in pixels
	 */
	public int getScrollLeft()
	{
		return scrollLeft;
	}

	/**
	 * @return the size the user gave the editor, {@link ResizableSize#NONE} for the page's
	 */
	public ResizableSize getSize()
	{
		return size;
	}

	/**
	 * @return this state with another size
	 */
	public RichTextEditorState withSize(ResizableSize size)
	{
		return new RichTextEditorState(anchor, head, scrollTop, scrollLeft, size);
	}

	/**
	 * @return the state as the editor in the browser takes it
	 */
	public JSONObject toJson()
	{
		JSONObject json = new JSONObject()
			.put("selection", new JSONObject().put("anchor", anchor).put("head", head))
			.put("scrollTop", scrollTop).put("scrollLeft", scrollLeft);
		// a dimension left out is the page's
		return size.putInto(json);
	}

	/**
	 * @param json
	 *            the state as the browser sends it
	 * @return the state, {@code null} for none or one that is not JSON; what it leaves out is as
	 *         {@link #NONE} has it
	 */
	public static RichTextEditorState parse(String json)
	{
		if (json == null || json.isBlank())
		{
			return null;
		}
		try
		{
			JSONObject object = new JSONObject(json);
			JSONObject selection = object.optJSONObject("selection");
			int anchor = selection == null ? 0 : pixels(selection, "anchor");
			int head = selection == null ? 0 : pixels(selection, "head");
			return new RichTextEditorState(anchor, head, pixels(object, "scrollTop"),
				pixels(object, "scrollLeft"), ResizableSize.of(optionalPixels(object, "width"),
					optionalPixels(object, "height")));
		}
		catch (JSONException e)
		{
			return null;
		}
	}

	private static int pixels(JSONObject object, String name)
	{
		Integer pixels = optionalPixels(object, name);
		return pixels == null ? 0 : pixels;
	}

	private static Integer optionalPixels(JSONObject object, String name)
	{
		Object value = object.opt(name);
		if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue()))
		{
			return null;
		}
		return (int)Math.round(Math.min(Math.max(number.doubleValue(), 0), Integer.MAX_VALUE));
	}

	@Override
	public boolean equals(Object other)
	{
		return other instanceof RichTextEditorState state && state.anchor == anchor &&
			state.head == head && state.scrollTop == scrollTop && state.scrollLeft == scrollLeft &&
			state.size.equals(size);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(anchor, head, scrollTop, scrollLeft, size);
	}

	@Override
	public String toString()
	{
		return toJson().toString();
	}
}
