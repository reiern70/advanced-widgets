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
package com.antiliacode.widgets.dynamicdatatable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

/**
 * The columns of checkboxes bound to a boolean of the row: {@link BeanCheckBoxColumn} in a bean
 * based table and {@link CheckBoxColumn} in a JSON based one.
 */
class CheckBoxColumnTest extends WicketTestCase
{
	private static final String CONFIRM = "table:overlay:window:content:body:confirm";

	private static final String CANCEL = "table:overlay:window:content:body:cancel";

	private TasksProvider provider;

	private BaseDynamicDataTable<Task, Long, ?> table;

	private final List<String> updates = new ArrayList<>();

	private final List<String> notFound = new ArrayList<>();

	@Test
	void aBeanCheckboxIsCheckedAsTheRowSaysAndNamedAfterTheHeaderAndTheRow()
	{
		startBean(new DoneColumn().setRowLabel(Task::getName));

		TagTester first = checkbox("1");
		assertEquals("ant-data-table-check", first.getAttribute("class"));
		assertEquals("done", first.getAttribute("data-dt-action"));
		assertEquals("done-unchecked", first.getAttribute("data-dt-action-unchecked"));
		assertEquals("Done: Paint <walls>", first.getAttribute("aria-label"));
		assertTrue(first.getAttributeIs("checked", "checked") || first.hasAttribute("checked"));
		assertFalse(checkbox("2").hasAttribute("checked"));
		assertTrue(tester.getLastResponseAsString().contains("aria-label=\"Done: Paint &lt;walls&gt;\""),
			"escaped");
	}

	@Test
	void checkingAndUncheckingCallOnUpdateWithTheRowAndTheValue()
	{
		startBean(new DoneColumn());

		executeAction("done", "2");
		executeAction("done-unchecked", "1");

		assertEquals(List.of("Plaster true", "Paint <walls> false"), updates);
		assertTrue(notFound.isEmpty());
	}

	@Test
	void aRowThatIsGoneReachesOnNotFoundAction()
	{
		startBean(new DoneColumn());

		executeAction("done", "9");

		assertTrue(updates.isEmpty());
		assertEquals(List.of("done 9"), notFound);
	}

	@Test
	void aDisabledCheckboxShowsItsReasonAndTheServerRefusesAChange()
	{
		startBean(new DoneColumn());

		TagTester locked = TagTester.createTagByAttribute(tester.getLastResponseAsString(),
			"data-key", "3");
		assertTrue(locked.getAttribute("data-dt-disabled-actions").contains("\"done\":\"Locked\""),
			locked.getAttribute("data-dt-disabled-actions"));

		executeAction("done", "3");
		executeAction("done-unchecked", "3");

		assertTrue(updates.isEmpty());
		assertEquals(List.of("done 3", "done-unchecked 3"), notFound);
	}

	@Test
	void aConfirmedChangeIsMadeACancelledOneRendersTheRowsAgain()
	{
		startBean(new DoneColumn().setConfirmation(Model.of("Sure?")));

		executeAction("done", "2");
		assertTrue(table.isOverlayShown());
		assertTrue(updates.isEmpty());
		tester.clickLink(CONFIRM);
		assertEquals(List.of("Plaster true"), updates);

		executeAction("done-unchecked", "1");
		tester.clickLink(CANCEL);
		assertEquals(List.of("Plaster true"), updates);
		assertFalse(table.isOverlayShown());
		assertTrue(tester.getLastResponseAsString().contains("data-key=\"1\""),
			"the rows again: " + tester.getLastResponseAsString());
	}

	@Test
	void escapeCancelsTheConfirmationToo()
	{
		startBean(new DoneColumn().setConfirmation(Model.of("Sure?")));
		executeAction("done", "2");

		executeAction(BaseDynamicDataTable.CLOSE_OVERLAY_ACTION, null);

		assertTrue(updates.isEmpty());
		assertFalse(table.isOverlayShown());
		assertTrue(tester.getLastResponseAsString().contains("data-key=\"2\""),
			"the rows again: " + tester.getLastResponseAsString());
	}

	@Test
	void theConfirmationCanDependOnTheRowAndTheValue()
	{
		startBean(new DoneColumn()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected IModel<String> getConfirmation(Task row, boolean value)
			{
				return value ? null : Model.of("Undo " + row.getName() + "?");
			}
		});

		executeAction("done", "2");
		assertEquals(List.of("Plaster true"), updates);
		assertFalse(table.isOverlayShown());

		executeAction("done-unchecked", "1");
		assertTrue(table.isOverlayShown());
	}

	@Test
	void theCsvValueIsYesOrNoInTheLanguageOfTheSession()
	{
		DoneColumn column = new DoneColumn();
		startBean(column);

		assertEquals("Yes", column.getDataModel(Model.of(provider.findByKey(1L))).getObject());
		assertEquals("No", column.getDataModel(Model.of(provider.findByKey(2L))).getObject());

		tester.getSession().setLocale(Locale.FRENCH);
		assertEquals("Oui", column.getDataModel(Model.of(provider.findByKey(1L))).getObject());
		tester.getSession().setLocale(new Locale("es"));
		assertEquals("No", column.getDataModel(Model.of(provider.findByKey(2L))).getObject());
		assertEquals("Sí", column.getDataModel(Model.of(provider.findByKey(1L))).getObject());
	}

	@Test
	void readsAPropertyExpressionAndSortsByItsSortProperty()
	{
		BeanCheckBoxColumn<Task, String> column = new BeanCheckBoxColumn<Task, String>(
			Model.of("Done"), "done")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(Task row, boolean value, AjaxRequestTarget target)
			{
				updates.add(row.getName() + " " + value);
			}
		};
		assertFalse(column.isSortable());
		startBean(column.setSortProperty("done"));

		assertEquals("done", column.getActionId());
		assertTrue(column.isSortable());
		assertTrue(checkbox("1").hasAttribute("checked"));
		executeAction("done", "2");
		assertEquals(List.of("Plaster true"), updates);
	}

	@Test
	void theJsonCheckboxIsATemplateWithTheStateForTheBrowser()
	{
		JsonDoneColumn column = new JsonDoneColumn();
		column.setRowLabelPath("name");
		startJson(column);

		assertEquals("<input type=\"checkbox\" class=\"ant-data-table-check\" " +
			"data-dt-action=\"done\" data-dt-action-unchecked=\"done-unchecked\" " +
			"aria-label=\"Done: {{name}}\" data-dt-checked=\"{{done}}\">",
			column.getTemplate(jsonTable()));
		assertEquals("No", column.getExportValue(provider.findByKey(2L)));
		assertEquals(Map.of("done", "Locked", "done-unchecked", "Locked"),
			table.getDisabledActions(provider.findByKey(3L)));

		executeAction("done", "2");
		executeAction("done", "3");
		assertEquals(List.of("Plaster true"), updates);
		assertEquals(List.of("done 3"), notFound);
	}

	@Test
	void theJsonCheckboxWithoutARowLabelIsNamedAfterTheHeader()
	{
		JsonDoneColumn column = new JsonDoneColumn();
		startJson(column);

		assertTrue(column.getTemplate(jsonTable()).contains("aria-label=\"Done\""),
			column.getTemplate(jsonTable()));
		assertEquals("done", column.getActionId());
		assertEquals(2, column.getActionContributors().size());
	}

	private JsonBasedDynamicDataTable<Task, Long> jsonTable()
	{
		return (JsonBasedDynamicDataTable<Task, Long>)table;
	}

	private TagTester checkbox(String key)
	{
		return TagTester.createTagByName(TagTester.createTagByAttribute(
			tester.getLastResponseAsString(), "data-key", key).getValue(), "input");
	}

	private void startBean(BeanCheckBoxColumn<Task, String> column)
	{
		provider = new TasksProvider();
		List<IColumn<Task, String>> columns = new ArrayList<>();
		columns.add(new PropertyColumn<>(Model.of("Name"), "name"));
		columns.add(column);
		start(new BeanBasedDynamicDataTable<Task, Long, String>("table", columns, provider, 10)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onNotFoundAction(AjaxRequestTarget target, String action, Long key,
				Task row)
			{
				notFound.add(action + " " + key);
			}
		});
	}

	private void startJson(CheckBoxColumn<Task> column)
	{
		provider = new TasksProvider();
		start(new JsonBasedDynamicDataTable<Task, Long>("table",
			List.<IDynamicColumn<Task>> of(column), provider, 10)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onNotFoundAction(AjaxRequestTarget target, String action, Long key,
				Task row)
			{
				notFound.add(action + " " + key);
			}
		});
	}

	private void start(BaseDynamicDataTable<Task, Long, ?> table)
	{
		this.table = table;
		tester.startPage(new TablePage(table));
	}

	private void executeAction(String action, String key)
	{
		tester.getRequest().getPostParameters().setParameterValue("action", action);
		if (key != null)
		{
			tester.getRequest().getPostParameters().setParameterValue("key", key);
		}
		tester.executeBehavior(table.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));
	}

	private class DoneColumn extends BeanCheckBoxColumn<Task, String>
	{
		private static final long serialVersionUID = 1L;

		DoneColumn()
		{
			super(Model.of("Done"), "done", Task::isDone);
		}

		@Override
		public boolean isEnabled(Task row)
		{
			return row.getId() != 3;
		}

		@Override
		public IModel<String> getDisabledReason(Task row)
		{
			return Model.of("Locked");
		}

		@Override
		protected void onUpdate(Task row, boolean value, AjaxRequestTarget target)
		{
			updates.add(row.getName() + " " + value);
		}
	}

	private class JsonDoneColumn extends CheckBoxColumn<Task>
	{
		private static final long serialVersionUID = 1L;

		JsonDoneColumn()
		{
			super(Model.of("Done"), "done", Task::isDone);
		}

		@Override
		public boolean isEnabled(Task row)
		{
			return row.getId() != 3;
		}

		@Override
		public IModel<String> getDisabledReason(Task row)
		{
			return Model.of("Locked");
		}

		@Override
		protected void onUpdate(Task row, boolean value, AjaxRequestTarget target)
		{
			updates.add(row.getName() + " " + value);
		}
	}

	public static class Task implements Serializable, IIdentifiable<Long>
	{
		private static final long serialVersionUID = 1L;

		private final long id;

		private final String name;

		private final boolean done;

		Task(long id, String name, boolean done)
		{
			this.id = id;
			this.name = name;
			this.done = done;
		}

		@Override
		public Long getId()
		{
			return id;
		}

		public String getName()
		{
			return name;
		}

		public boolean isDone()
		{
			return done;
		}
	}

	private static class TasksProvider extends SortableDynamicDataProvider<Task, Long, String>
		implements
			IIdentifiableDynamicDataProvider<Task, Long>
	{
		private static final long serialVersionUID = 1L;

		final List<Task> tasks = new ArrayList<>(List.of(new Task(1, "Paint <walls>", true),
			new Task(2, "Plaster", false), new Task(3, "Wire", true)));

		TasksProvider()
		{
			super(Long.class);
		}

		@Override
		public Iterator<Task> iterator(long first, long count)
		{
			return tasks.subList((int)first, (int)Math.min(tasks.size(), first + count))
				.iterator();
		}

		@Override
		public long size()
		{
			return tasks.size();
		}

		@Override
		public Task findByKey(Long key)
		{
			return tasks.stream().filter(task -> task.getId().equals(key)).findFirst()
				.orElse(null);
		}
	}

	public static class TablePage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		TablePage(BaseDynamicDataTable<Task, Long, ?> table)
		{
			add(table);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream(
				"<html><head></head><body><table wicket:id=\"table\"></table></body></html>");
		}
	}
}
