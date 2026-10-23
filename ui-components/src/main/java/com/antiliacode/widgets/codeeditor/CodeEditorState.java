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
package com.antiliacode.widgets.codeeditor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONException;
import com.github.openjson.JSONObject;

import com.antiliacode.widgets.resizable.ResizableSize;

/**
 * The state of a {@link CodeEditor} besides its text: what is selected, a cursor where nothing is,
 * how far the text is scrolled and the size the user gave the editor. The editor keeps it in its
 * state model and gives the model a new one each time the browser tells it the user changed it, so
 * that a model may store it anywhere: in the page, by default, in the session or with the user's
 * preferences. Shown again, after the user came back to the page for example, the editor takes it
 * up again.
 * <p>
 * Positions are those of the characters of the text, as JavaScript counts them, the lines
 * separated by a single {@code \n}.
 *
 * @since 11.0.0
 */
public final class CodeEditorState implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** The cursor at the start, the text not scrolled, the size the page's. */
	public static final CodeEditorState NONE = new CodeEditorState(List.of(), 0, 0, 0,
		ResizableSize.NONE);

	private final List<Range> ranges;

	private final int main;

	private final int scrollTop;

	private final int scrollLeft;

	private final ResizableSize size;

	/**
	 * A range of the text: from where the user started selecting, the anchor, to where the cursor
	 * is, its head; a cursor where both are the same.
	 *
	 * @param anchor
	 *            where the selection starts
	 * @param head
	 *            where the cursor is
	 */
	public record Range(int anchor, int head) implements Serializable
	{
		/**
		 * @param anchor
		 *            where the selection starts
		 * @param head
		 *            where the cursor is
		 */
		public Range
		{
			anchor = Math.max(anchor, 0);
			head = Math.max(head, 0);
		}

		/**
		 * @param position
		 *            where the cursor is
		 * @return a cursor, selecting nothing
		 */
		public static Range cursor(int position)
		{
			return new Range(position, position);
		}

		/**
		 * @return where the range starts
		 */
		public int from()
		{
			return Math.min(anchor, head);
		}

		/**
		 * @return where the range ends
		 */
		public int to()
		{
			return Math.max(anchor, head);
		}

		/**
		 * @return whether the range selects nothing: a cursor
		 */
		public boolean isEmpty()
		{
			return anchor == head;
		}
	}

	/**
	 * @param ranges
	 *            what is selected, a cursor where nothing is
	 * @param main
	 *            the index of the main range, that of the cursor the user moves
	 * @param scrollTop
	 *            how far the text is scrolled down, in pixels
	 * @param scrollLeft
	 *            how far the text is scrolled right, in pixels
	 * @param size
	 *            the size the user gave the editor, {@code null} for the page's
	 */
	public CodeEditorState(List<Range> ranges, int main, int scrollTop, int scrollLeft,
		ResizableSize size)
	{
		this.ranges = ranges == null ? List.of() : List.copyOf(ranges);
		this.main = main >= 0 && main < this.ranges.size() ? main : 0;
		this.scrollTop = Math.max(scrollTop, 0);
		this.scrollLeft = Math.max(scrollLeft, 0);
		this.size = size == null ? ResizableSize.NONE : size;
	}

	/**
	 * @param anchor
	 *            where the selection starts
	 * @param head
	 *            where the cursor is
	 * @return the state selecting a range, the text not scrolled, the size the page's
	 */
	public static CodeEditorState selecting(int anchor, int head)
	{
		return new CodeEditorState(List.of(new Range(anchor, head)), 0, 0, 0, ResizableSize.NONE);
	}

	/**
	 * @return what is selected, a cursor where nothing is; none at the start
	 */
	public List<Range> getRanges()
	{
		return ranges;
	}

	/**
	 * @return the index of the main range
	 */
	public int getMain()
	{
		return main;
	}

	/**
	 * @return the main range, that of the cursor the user moves; a cursor at the start for none
	 */
	public Range getMainRange()
	{
		return ranges.isEmpty() ? Range.cursor(0) : ranges.get(main);
	}

	/**
	 * @return where the cursor is
	 */
	public int getCursor()
	{
		return getMainRange().head();
	}

	/**
	 * @param text
	 *            the text of the editor
	 * @return the text the main range selects, empty for a cursor
	 */
	public String getSelectedText(String text)
	{
		String normalized = text == null ? "" : text.replace("\r\n", "\n");
		Range range = getMainRange();
		int length = normalized.length();
		return normalized.substring(Math.min(range.from(), length), Math.min(range.to(), length));
	}

	/**
	 * @return how far the text is scrolled down, in pixels
	 */
	public int getScrollTop()
	{
		return scrollTop;
	}

	/**
	 * @return how far the text is scrolled right, in pixels
	 */
	public int getScrollLeft()
	{
		return scrollLeft;
	}

	/**
	 * @return the size the user gave the editor
	 */
	public ResizableSize getSize()
	{
		return size;
	}

	/**
	 * @return the state with another size
	 */
	public CodeEditorState withSize(ResizableSize size)
	{
		return new CodeEditorState(ranges, main, scrollTop, scrollLeft, size);
	}

	/**
	 * @return the state selecting other ranges
	 */
	public CodeEditorState withRanges(List<Range> ranges, int main)
	{
		return new CodeEditorState(ranges, main, scrollTop, scrollLeft, size);
	}

	/**
	 * @return the state as the script takes it, {@code CodeEditorState} of the browser
	 */
	public JSONObject toJson()
	{
		JSONObject json = new JSONObject();
		if (!ranges.isEmpty())
		{
			JSONArray array = new JSONArray();
			ranges.forEach(range -> array.put(new JSONObject().put("anchor", range.anchor())
				.put("head", range.head())));
			json.put("selection", new JSONObject().put("ranges", array).put("main", main));
		}
		json.put("scrollTop", scrollTop).put("scrollLeft", scrollLeft);
		// a dimension left out is the page's
		return size.putInto(json);
	}

	/**
	 * @param json
	 *            a state as the browser sends it, maybe with parts missing
	 * @return the state, {@code null} when the text is no state
	 */
	public static CodeEditorState parse(String json)
	{
		if (json == null || json.isBlank())
		{
			return null;
		}
		try
		{
			JSONObject object = new JSONObject(json);
			List<Range> ranges = new ArrayList<>();
			int main = 0;
			JSONObject selection = object.optJSONObject("selection");
			if (selection != null)
			{
				JSONArray array = selection.optJSONArray("ranges");
				for (int i = 0; array != null && i < array.length(); i++)
				{
					JSONObject range = array.optJSONObject(i);
					if (range != null && range.has("anchor") && range.has("head"))
					{
						ranges.add(new Range(range.optInt("anchor"), range.optInt("head")));
					}
				}
				main = selection.optInt("main", 0);
			}
			return new CodeEditorState(ranges, main, pixels(object, "scrollTop"),
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
		return (int)Math.round(number.doubleValue());
	}

	@Override
	public boolean equals(Object other)
	{
		return other instanceof CodeEditorState state && state.ranges.equals(ranges) &&
			state.main == main && state.scrollTop == scrollTop && state.scrollLeft == scrollLeft &&
			state.size.equals(size);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(ranges, main, scrollTop, scrollLeft, size);
	}

	@Override
	public String toString()
	{
		return toJson().toString();
	}
}
