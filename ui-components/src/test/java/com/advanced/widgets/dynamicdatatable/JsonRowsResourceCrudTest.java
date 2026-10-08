/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.dynamicdatatable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.tester.WicketTestCase;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.openjson.JSONObject;

/**
 * Creates, reads, changes and deletes single rows through a {@link JsonRowsResource}, and answers
 * 405 for a resource that only reads.
 */
class JsonRowsResourceCrudTest extends WicketTestCase
{
	/** The notes of the resource, by id. */
	private static final Map<Long, Note> NOTES = new ConcurrentHashMap<>();

	private static final AtomicLong IDS = new AtomicLong();

	/** The body the next request sends. */
	private static String body = "";

	private static final ResourceReference NOTES_REFERENCE = new ResourceReference(
		JsonRowsResourceCrudTest.class, "notes")
	{
		private static final long serialVersionUID = 1L;

		@Override
		public IResource getResource()
		{
			return new NotesResource();
		}
	};

	private static final ResourceReference READ_ONLY = new ResourceReference(
		JsonRowsResourceCrudTest.class, "readOnly")
	{
		private static final long serialVersionUID = 1L;

		@Override
		public IResource getResource()
		{
			return new ReadOnlyResource();
		}
	};

	@Override
	protected WicketTester newWicketTester(WebApplication app)
	{
		WicketTester tester = super.newWicketTester(app);
		app.mountResource("api/notes", NOTES_REFERENCE);
		app.mountResource("api/read-only", READ_ONLY);
		return tester;
	}

	@BeforeEach
	void someNotes()
	{
		NOTES.clear();
		IDS.set(0);
		for (String text : List.of("one", "two"))
		{
			Note note = new Note(IDS.incrementAndGet(), text);
			NOTES.put(note.getId(), note);
		}
	}

	@Test
	void readsOneRowByItsKey()
	{
		JSONObject json = request("GET", "api/notes?key=2", 200);

		assertEquals("2", json.getString("key"));
		assertEquals("two", json.getJSONObject("data").getString("text"));
		assertFalse(json.has("number"), "a single row has no number");
	}

	@Test
	void createsARow()
	{
		body = "{\"text\":\"three\"}";
		JSONObject json = request("POST", "api/notes", 201);

		assertEquals("3", json.getString("key"));
		assertEquals("three", json.getJSONObject("data").getString("text"));
		assertEquals("three", NOTES.get(3L).getText());
	}

	@Test
	void changesARow()
	{
		body = "{\"text\":\"TWO\"}";
		JSONObject json = request("PUT", "api/notes?key=2", 200);

		assertEquals("TWO", json.getJSONObject("data").getString("text"));
		assertEquals("TWO", NOTES.get(2L).getText());
	}

	@Test
	void deletesARow()
	{
		tester.getRequest().setMethod("DELETE");
		tester.executeUrl("api/notes?key=1");

		assertEquals(204, tester.getLastResponse().getStatus());
		assertNull(NOTES.get(1L));
	}

	@Test
	void answers404ForARowThatIsNotThere()
	{
		assertEquals("No row 9", request("GET", "api/notes?key=9", 404).getString("error"));
		body = "{\"text\":\"nine\"}";
		request("PUT", "api/notes?key=9", 404);
		request("DELETE", "api/notes?key=9", 404);
	}

	@Test
	void answers400ForARowThatIsNotValid()
	{
		body = "{\"text\":\"\"}";
		JSONObject json = request("PUT", "api/notes?key=1", 400);

		assertEquals("The text is required", json.getString("error"));
		assertEquals("one", NOTES.get(1L).getText(), "the row is left as it was");
		request("PUT", "api/notes", 400);
	}

	@Test
	void answers405WhenTheResourceOnlyReads()
	{
		body = "{\"text\":\"three\"}";
		request("POST", "api/read-only", 405);
		assertEquals("GET, HEAD, POST, PUT, DELETE", tester.getLastResponse().getHeader("Allow"));
		request("DELETE", "api/read-only?key=1", 405);
		request("PATCH", "api/notes?key=1", 405);
		assertEquals(2, NOTES.size());
	}

	private JSONObject request(String method, String path, int status)
	{
		tester.getRequest().setMethod(method);
		tester.executeUrl(path);
		assertEquals(status, tester.getLastResponse().getStatus(), tester.getLastResponseAsString());
		assertEquals("application/json", tester.getLastResponse().getContentType().split(";")[0]);
		return new JSONObject(tester.getLastResponseAsString());
	}

	public static class Note implements Serializable, IIdentifiable<Long>
	{
		private static final long serialVersionUID = 1L;

		private final long id;

		private String text;

		Note(long id, String text)
		{
			this.id = id;
			this.text = text;
		}

		@Override
		public Long getId()
		{
			return id;
		}

		public String getText()
		{
			return text;
		}
	}

	private static class NotesProvider extends SortableDynamicDataProvider<Note, Long, String>
		implements
			IIdentifiableDynamicDataProvider<Note, Long>
	{
		private static final long serialVersionUID = 1L;

		NotesProvider()
		{
			super(Long.class);
		}

		@Override
		public Iterator<Note> iterator(long first, long count)
		{
			List<Note> notes = new ArrayList<>(NOTES.values());
			return notes.subList((int)first, (int)Math.min(notes.size(), first + count))
				.iterator();
		}

		@Override
		public long size()
		{
			return NOTES.size();
		}

		@Override
		public Note findByKey(Long key)
		{
			return NOTES.get(key);
		}
	}

	private static class ReadOnlyResource extends JsonRowsResource<Note, Long>
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected IDynamicDataProvider<Note, Long> newProvider(PageParameters parameters)
		{
			return new NotesProvider();
		}

		@Override
		protected String readBody(Attributes attributes)
		{
			return body;
		}
	}

	private static class NotesResource extends ReadOnlyResource
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected Note createRow(String json)
		{
			Note note = new Note(IDS.incrementAndGet(), text(json));
			NOTES.put(note.getId(), note);
			return note;
		}

		@Override
		protected Note updateRow(Note note, String json)
		{
			note.text = text(json);
			return note;
		}

		@Override
		protected void deleteRow(Note note)
		{
			NOTES.remove(note.getId());
		}

		private static String text(String json)
		{
			String text = new JSONObject(json).optString("text", "");
			if (text.isBlank())
			{
				throw new IllegalArgumentException("The text is required");
			}
			return text;
		}
	}
}
