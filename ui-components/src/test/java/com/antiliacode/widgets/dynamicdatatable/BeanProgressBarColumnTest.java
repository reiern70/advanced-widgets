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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.progress.ProgressBar;
import com.antiliacode.widgets.theme.Look;
import com.antiliacode.widgets.theme.Looks;

class BeanProgressBarColumnTest extends WicketTestCase
{
	private TasksProvider provider;

	private BeanBasedDynamicDataTable<Task, Long, String> table;

	private void start(BeanProgressBarColumn<Task, String> column, Task... tasks)
	{
		provider = new TasksProvider(List.of(tasks));
		List<IColumn<Task, String>> columns = new ArrayList<>();
		columns.add(new PropertyColumn<>(Model.of("Name"), "name"));
		columns.add(column);
		table = new BeanBasedDynamicDataTable<>("table", columns, provider, 10);
		tester.startPage(new TablePage(table));
	}

	@Test
	void rendersAProgressBarPerRowWithItsValue()
	{
		start(new BeanProgressBarColumn<>(Model.of("Done"), Task::getDone),
			new Task(1, "Paint", 40), new Task(2, "Plaster", 75.4));

		List<TagTester> progress = TagTester.createTagsByAttribute(
			tester.getLastResponseAsString(), "max", "100", false);
		assertEquals(2, progress.size());
		assertEquals("40", progress.get(0).getAttribute("value"));
		assertEquals("75", progress.get(1).getAttribute("value"), "rounded");
		assertTrue(rowMarkup("1").contains(">40%</span>"), rowMarkup("1"));
		assertTrue(tester.getComponentFromLastRenderedPage(
			"table:body:rows:1:cells:2:cell:bar") instanceof ProgressBar);
	}

	@Test
	void aValueBelow0OrAbove100IsClamped()
	{
		start(new BeanProgressBarColumn<>(Model.of("Done"), Task::getDone),
			new Task(1, "Paint", -5), new Task(2, "Plaster", 120));

		assertEquals("0", progressOf("1").getAttribute("value"));
		assertEquals("100", progressOf("2").getAttribute("value"));
	}

	@Test
	void aNullValueShowsAnIndeterminateBar()
	{
		start(new BeanProgressBarColumn<>(Model.of("Done"), Task::getDone),
			new Task(1, "Paint", null));

		assertNull(progressOf("1").getAttribute("value"));
	}

	@Test
	void theBarIsNamedAfterTheHeaderAndShowsTheLabelOfTheRow()
	{
		start(new BeanProgressBarColumn<Task, String>(Model.of("Done"), Task::getDone)
			.setLabel(task -> task.getName() + " <" + task.getDone() + ">"),
			new Task(1, "Paint", 40));

		assertEquals("Done", progressOf("1").getAttribute("aria-label"));
		assertTrue(rowMarkup("1").contains("aria-hidden=\"true\">Paint &lt;40.0&gt;</span>"),
			rowMarkup("1"));
	}

	@Test
	void readsAPropertyExpression()
	{
		start(new BeanProgressBarColumn<>(Model.of("Done"), "done"), new Task(1, "Paint", 33));

		assertEquals("33", progressOf("1").getAttribute("value"));
	}

	@Test
	void theCsvValueIsTheNumberAsItIs()
	{
		BeanProgressBarColumn<Task, String> column = new BeanProgressBarColumn<>(Model.of("Done"),
			Task::getDone);
		start(column, new Task(1, "Paint", 120.5));

		assertEquals(120.5, column.getDataModel(Model.of(new Task(1, "Paint", 120.5))).getObject());
		assertTrue(table.isColumnExportable(column));
		assertEquals(120.5, table.getColumnExportValue(column, provider.tasks.get(0)));
	}

	@Test
	void sortsOnceItHasASortProperty()
	{
		BeanProgressBarColumn<Task, String> column = new BeanProgressBarColumn<>(Model.of("Done"),
			Task::getDone);
		assertFalse(column.isSortable());
		assertNull(column.getSortProperty());

		start(column.setSortProperty("done"), new Task(1, "Paint", 70), new Task(2, "Plaster", 20));
		assertTrue(column.isSortable());
		tester.clickLink("table:headersRow:headers:1:sort");

		assertEquals("done", provider.getSort().getProperty());
		String rows = tester.getLastResponseAsString();
		assertTrue(rows.indexOf("data-key=\"2\"") < rows.indexOf("data-key=\"1\""), rows);
	}

	@Test
	void contributesTheStyleSheetOfTheBarWithoutRows()
	{
		start(new BeanProgressBarColumn<>(Model.of("Done"), Task::getDone));

		tester.assertContains("antilia-progress-bar.*\\.css");
	}

	@Test
	void theBarIsInTheLookOfTheTable()
	{
		Looks.setSessionLook(Look.BOOTSTRAP);
		start(new BeanProgressBarColumn<>(Model.of("Done"), Task::getDone),
			new Task(1, "Paint", 40));

		TagTester bar = TagTester.createTagByAttribute(rowMarkup("1"), "wicket:id", "bar");
		assertTrue(bar.getAttribute("class").contains("progress"), bar.getAttribute("class"));
		assertEquals(ProgressBar.markup("40", Looks.of(table)).replaceAll("^<div[^>]*>|</div>$", ""),
			bar.getValue().replace(" aria-label=\"Done\"", ""));
	}

	private TagTester progressOf(String key)
	{
		return TagTester.createTagByName(rowMarkup(key), "progress");
	}

	private String rowMarkup(String key)
	{
		return TagTester.createTagByAttribute(tester.getLastResponseAsString(), "data-key", key)
			.getValue();
	}

	public static class Task implements Serializable, IIdentifiable<Long>
	{
		private static final long serialVersionUID = 1L;

		private final long id;

		private final String name;

		private final Double done;

		Task(long id, String name, Number done)
		{
			this.id = id;
			this.name = name;
			this.done = done != null ? done.doubleValue() : null;
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

		public Double getDone()
		{
			return done;
		}
	}

	private static class TasksProvider extends SortableDynamicDataProvider<Task, Long, String>
		implements
			IIdentifiableDynamicDataProvider<Task, Long>
	{
		private static final long serialVersionUID = 1L;

		final List<Task> tasks;

		TasksProvider(List<Task> tasks)
		{
			super(Long.class);
			this.tasks = new ArrayList<>(tasks);
		}

		@Override
		public Iterator<Task> iterator(long first, long count)
		{
			List<Task> sorted = new ArrayList<>(tasks);
			if (getSort() != null)
			{
				Comparator<Task> byDone = Comparator.comparing(Task::getDone);
				sorted.sort(getSort().isAscending() ? byDone : byDone.reversed());
			}
			return sorted.subList((int)first, (int)Math.min(sorted.size(), first + count))
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
