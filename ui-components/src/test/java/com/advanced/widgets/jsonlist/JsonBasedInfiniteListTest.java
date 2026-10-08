/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.jsonlist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.advanced.widgets.dynamicdatatable.BaseDynamicDataTable.BodyHeightUnits;
import com.advanced.widgets.dynamicdatatable.IDynamicDataProvider;
import com.advanced.widgets.dynamicdatatable.JacksonJsonSerializer;
import com.advanced.widgets.license.Licensing;

class JsonBasedInfiniteListTest extends WicketTestCase
{
	private static final List<Person> PEOPLE = new ArrayList<>();

	private TestList list;

	private Person acted;

	@BeforeEach
	void startList()
	{
		PEOPLE.clear();
		for (int i = 1; i <= 5; i++)
		{
			PEOPLE.add(new Person(String.valueOf(i), "Person " + i));
		}
		acted = null;
		list = new TestList("list");
		list.addAction(IItemAction.of("open", (person, target) -> acted = person));
		tester.startPage(new TestPage(list));
	}

	@Test
	void rendersAnEmptyFeedAndItsScript()
	{
		TagTester tag = tester.getTagByWicketId("list");
		assertEquals("div", tag.getName());
		assertEquals("feed", tag.getAttribute("role"));
		assertTrue(tag.getAttributeContains("class", JsonBasedInfiniteList.CSS_CLASS));
		assertEquals("", tag.getValue(), "the list has no children");
		tester.assertContains(Pattern.quote("new Wicket.JsonInfiniteList({"));
		tester.assertContains(Pattern.quote("\"template\":\"<p data-dt-action=\\\"open\\\">{{name}}<\\/p>\""));
		tester.assertContains(Pattern.quote("\"m\":\"POST\""));
		tester.assertContains(Pattern.quote("\"end\":\"All ${count} shown\""));
		tester.assertContainsNot(Pattern.quote("\"height\""));
	}

	@Test
	void rendersAPlaceholderWithoutALicenseKey()
	{
		Licensing.install(tester.getApplication(), null);
		tester.startPage(new TestPage(list));

		String page = tester.getLastResponseAsString();
		TagTester placeholder = TagTester.createTagByAttribute(page, "class",
			Licensing.PLACEHOLDER_CSS_CLASS);
		assertEquals(list.getMarkupId(), placeholder.getAttribute("id"));
		assertEquals("TestList", placeholder.getAttribute("data-component"));
		assertFalse(page.contains("new Wicket.JsonInfiniteList("), page);
	}

	@Test
	void servesTheFirstPageThenOnePageMoreAtATimeUntilThereAreNoMore()
	{
		tester.executeBehavior(dataBehavior());
		assertEquals("{\"items\":[{\"key\":\"1\",\"number\":1,\"data\":{\"id\":\"1\"," +
			"\"name\":\"Person 1\"}},{\"key\":\"2\",\"number\":2,\"data\":{\"id\":\"2\"," +
			"\"name\":\"Person 2\"}}],\"itemCount\":5}", tester.getLastResponseAsString());

		more();
		assertEquals("{\"items\":[{\"key\":\"3\",\"number\":3,\"data\":{\"id\":\"3\"," +
			"\"name\":\"Person 3\"}},{\"key\":\"4\",\"number\":4,\"data\":{\"id\":\"4\"," +
			"\"name\":\"Person 4\"}}],\"itemCount\":5}", tester.getLastResponseAsString());
		assertEquals(4, list.getShownItemCount());

		more();
		assertTrue(tester.getLastResponseAsString().startsWith(
			"{\"items\":[{\"key\":\"5\",\"number\":5,"), tester.getLastResponseAsString());
		more();
		assertEquals("{\"items\":[],\"itemCount\":5}", tester.getLastResponseAsString());
		assertEquals(List.of("1", "2", "3", "4", "5"), list.sent);
	}

	@Test
	void reloadingServesEveryItemShown()
	{
		more();
		tester.getRequest().getPostParameters().setParameterValue("more", "false");
		tester.executeBehavior(dataBehavior());

		assertEquals(4, countOf("\"key\""));
	}

	@Test
	void anItemAddedAfterTheLastPageIsServedWithTheNextOne()
	{
		more();
		more();
		PEOPLE.add(new Person("6", "Person 6"));
		PEOPLE.add(new Person("7", "Person 7"));

		more();
		assertTrue(tester.getLastResponseAsString().startsWith(
			"{\"items\":[{\"key\":\"6\",\"number\":6,"), tester.getLastResponseAsString());
		assertEquals(7, list.getShownItemCount());
	}

	@Test
	void resettingStartsOverFromTheTop()
	{
		more();
		list.reset();
		tester.executeBehavior(dataBehavior());

		assertEquals(2, countOf("\"key\""));
		tester.assertContains(Pattern.quote("\"top\":true"));
		tester.executeBehavior(dataBehavior());
		tester.assertContainsNot(Pattern.quote("\"top\""));
	}

	@Test
	void anActionIsDispatchedWithTheItemOfItsKey()
	{
		act("open", "3");

		assertSame(PEOPLE.get(2), acted);
		assertNull(list.notFoundAction);
	}

	@Test
	void anActionOnAGoneItemOrWithoutHandlerIsNotFound()
	{
		act("open", "42");
		assertNull(acted);
		assertEquals("open", list.notFoundAction);
		assertEquals("42", list.notFoundKey);

		act("unknown", "1");
		assertEquals("unknown", list.notFoundAction);
		assertSame(PEOPLE.get(0), list.notFoundItem);
	}

	@Test
	void anotherActionWithTheSameIdIsRejected()
	{
		assertThrows(IllegalArgumentException.class,
			() -> list.addAction(IItemAction.of("open", (person, target) -> {
			})));
	}

	@Test
	void refreshingAndUpdatingAnItemReachTheBrowserInTheAjaxResponse()
	{
		tester.executeBehavior(tester.getLastRenderedPage()
			.getBehaviors(RefreshBehavior.class).get(0));

		String response = tester.getLastResponseAsString();
		assertTrue(response.contains(".update({\"items\":[{\"key\":\"1\""), response);
		assertTrue(response.contains(".renderItem(\"2\", {\"id\":\"2\",\"name\":\"\\u003cb\\u003e\"})"),
			response);
	}

	@Test
	void theHeightIsSentWithItsUnitAndDrawsTheSlimScrollBar()
	{
		list.setHeight(30, BodyHeightUnits.VIEWPORT_HEIGHT);
		tester.startPage(new TestPage(list));

		tester.assertContains(Pattern.quote("\"height\":{\"value\":30,\"unit\":\"vh\"}"));
		tester.assertContains("wicket-scrollbar.js");
		assertThrows(IllegalArgumentException.class,
			() -> list.setHeight(0, BodyHeightUnits.PIXELS));
	}

	@Test
	void theTemplateIsReadFromAFileNamedAfterTheListByDefault()
	{
		TestList named = new NamedList("list");
		tester.startPage(new TestPage(named));

		tester.assertContains(Pattern.quote("\"template\":\"<span>{{name}}<\\/span>\""));
	}

	@Test
	void theUpdateItemMessageIsThatOfTheTable()
	{
		String json = JacksonJsonSerializer.INSTANCE.toJson(
			list.newUpdateItemMessage(PEOPLE.get(0)));

		assertTrue(json.contains("\"tableId\":\"" + list.getMarkupId() + "\""), json);
		assertTrue(json.contains("\"typeId\":\"updateRow\""), json);
		assertTrue(json.contains("\"key\":\"1\""), json);
		assertTrue(JacksonJsonSerializer.INSTANCE.toJson(list.newRefreshMessage())
			.contains("\"typeId\":\"refresh\""));
	}

	private void more()
	{
		tester.getRequest().getPostParameters().setParameterValue("more", "true");
		tester.executeBehavior(dataBehavior());
	}

	private void act(String action, String key)
	{
		tester.getRequest().getPostParameters().setParameterValue("action", action);
		tester.getRequest().getPostParameters().setParameterValue("key", key);
		tester.executeBehavior(list.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));
	}

	private int countOf(String text)
	{
		return tester.getLastResponseAsString().split(Pattern.quote(text), -1).length - 1;
	}

	private AbstractAjaxBehavior dataBehavior()
	{
		return list.getBehaviors(AbstractAjaxBehavior.class)
			.stream()
			.filter(behavior -> !(behavior instanceof AbstractDefaultAjaxBehavior))
			.findFirst()
			.orElseThrow();
	}

	public static class Person implements Serializable
	{
		private static final long serialVersionUID = 1L;

		private final String id;

		private final String name;

		Person(String id, String name)
		{
			this.id = id;
			this.name = name;
		}

		public String getId()
		{
			return id;
		}

		public String getName()
		{
			return name;
		}
	}

	private static class PeopleProvider implements IDynamicDataProvider<Person, String>
	{
		private static final long serialVersionUID = 1L;

		@Override
		public Iterator<? extends Person> iterator(long first, long count)
		{
			return new ArrayList<>(PEOPLE.subList((int)first,
				(int)Math.min(PEOPLE.size(), first + count))).iterator();
		}

		@Override
		public long size()
		{
			return PEOPLE.size();
		}

		@Override
		public String keyOf(Person row)
		{
			return row.getId();
		}

		@Override
		public Person findByKey(String key)
		{
			return PEOPLE.stream().filter(person -> person.getId().equals(key)).findFirst()
				.orElse(null);
		}

		@Override
		public Class<String> getKeyType()
		{
			return String.class;
		}
	}

	static class TestList extends JsonBasedInfiniteList<Person, String>
	{
		private static final long serialVersionUID = 1L;

		final List<String> sent = new ArrayList<>();

		String notFoundAction;

		String notFoundKey;

		Person notFoundItem;

		TestList(String id)
		{
			this(id, true);
		}

		TestList(String id, boolean inlineTemplate)
		{
			super(id, new PeopleProvider(), 2);
			if (inlineTemplate)
			{
				setItemTemplate("<p data-dt-action=\"open\">{{name}}</p>");
			}
		}

		@Override
		protected void onItemsSent(List<String> keys)
		{
			sent.clear();
			sent.addAll(keys);
		}

		@Override
		protected void onNotFoundAction(AjaxRequestTarget target, String action, String key,
			Person item)
		{
			notFoundAction = action;
			notFoundKey = key;
			notFoundItem = item;
		}
	}

	/**
	 * Reads its template from JsonBasedInfiniteListTest$NamedList.html.
	 */
	static class NamedList extends TestList
	{
		private static final long serialVersionUID = 1L;

		NamedList(String id)
		{
			super(id, false);
		}
	}

	private static class RefreshBehavior extends AbstractDefaultAjaxBehavior
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected void respond(AjaxRequestTarget target)
		{
			TestList list = (TestList)((MarkupContainer)getComponent()).get("list");
			list.refresh(target);
			list.updateItem(target, new Person("2", "<b>"));
		}
	}

	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		TestPage(JsonBasedInfiniteList<Person, String> list)
		{
			add(list);
			add(new RefreshBehavior());
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream(
				"<html><head></head><body><div wicket:id=\"list\"></div></body></html>");
		}
	}
}
