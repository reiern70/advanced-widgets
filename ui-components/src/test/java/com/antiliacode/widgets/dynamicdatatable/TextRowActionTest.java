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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

class TextRowActionTest extends WicketTestCase
{
	private static final String CONFIRM = "table:overlay:window:content:body:confirm";

	private static final String CANCEL = "table:overlay:window:content:body:cancel";

	private BranchesProvider provider;

	private Table table;

	private Only only;

	@Test
	void rendersItsTextEscapedInAButtonCarryingTheAction()
	{
		Only action = new Only();

		assertEquals("<button type=\"button\" class=\"ant-data-table-action " +
			"ant-data-table-text-action\" data-dt-action=\"only\"><span>Show only &lt;this&gt; " +
			"one</span></button>", action.getTemplate());
	}

	@Test
	void anIconGoesBeforeTheTextAndTheLinkStyleAddsItsClass()
	{
		TextRowAction<Branch> action = new Only().setIcon(IconBasedRowAction.Icon.ADD)
			.setLinkStyle(true);

		String template = action.getTemplate();
		assertTrue(template.startsWith("<button type=\"button\" class=\"ant-data-table-action " +
			"ant-data-table-text-action ant-data-table-link-action\" data-dt-action=\"only\"><svg"),
			template);
		assertTrue(template.endsWith("</svg><span>Show only &lt;this&gt; one</span></button>"),
			template);
		assertTrue(action.isLinkStyle());
	}

	@Test
	void sitsNextToIconActionsAndRunsWithItsRow()
	{
		start(null);

		String row = rowMarkup("2");
		assertTrue(row.contains("ant-data-table-icon-action"), row);
		assertTrue(row.indexOf("data-dt-action=\"edit\"") < row.indexOf("data-dt-action=\"only\""),
			"in the order of the column: " + row);

		executeAction("only", "2");

		assertEquals(List.of("north"), only.done);
	}

	@Test
	void aTextResourceIsResolvedAgainstTheTable()
	{
		TextRowAction<Branch> action = new TextRowAction<>("x",
			new ResourceModel("DynamicDataTable.confirm"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onRowAction(Branch row, AjaxRequestTarget target)
			{
			}
		};
		start(null);

		assertTrue(action.getTemplate(table).contains("<span>Confirm</span>"),
			action.getTemplate(table));
	}

	@Test
	void isLeftOutOfTheRowsItDoesNotApplyToAndRefusedThere()
	{
		start(null);

		assertFalse(rowMarkup("3").contains("data-dt-action=\"only\""), rowMarkup("3"));
		executeAction("only", "3");

		assertTrue(only.done.isEmpty());
		assertEquals(List.of("only 3"), table.notFound);
	}

	@Test
	void aDisabledActionIsNamedOnTheRowAndRefused()
	{
		start(null);

		TagTester row = TagTester.createTagByAttribute(tester.getLastResponseAsString(),
			"data-key", "4");
		assertEquals("{\"only\":\"Archived\"}", row.getAttribute("data-dt-disabled-actions"));
		executeAction("only", "4");

		assertTrue(only.done.isEmpty());
		assertEquals(List.of("only 4"), table.notFound);
	}

	@Test
	void asksForTheConfirmationFirst()
	{
		start(Model.of("Hide the others?"));
		executeAction("only", "1");

		assertTrue(table.isOverlayShown());
		assertTrue(only.done.isEmpty());
		tester.clickLink(CONFIRM);

		assertEquals(List.of("main"), only.done);
		assertFalse(table.isOverlayShown());
	}

	@Test
	void cancellingTheConfirmationCallsOnCancel()
	{
		start(Model.of("Hide the others?"));
		executeAction("only", "1");

		tester.clickLink(CANCEL);

		assertTrue(only.done.isEmpty());
		assertEquals(1, only.cancelled);
		assertFalse(table.isOverlayShown());
	}

	private void start(Model<String> confirmation)
	{
		provider = new BranchesProvider();
		only = new Only();
		only.setConfirmation(confirmation);
		List<IColumn<Branch, String>> columns = new ArrayList<>();
		columns.add(new PropertyColumn<>(Model.of("Name"), "name"));
		columns.add(new BeanActionsColumn<>(Model.of("Actions"), List.of(new Edit(), only)));
		table = new Table(columns, provider);
		tester.startPage(new TablePage(table));
	}

	private String rowMarkup(String key)
	{
		return TagTester.createTagByAttribute(tester.getLastResponseAsString(), "data-key", key)
			.getValue();
	}

	private void executeAction(String action, String key)
	{
		tester.getRequest().getPostParameters().setParameterValue("action", action);
		tester.getRequest().getPostParameters().setParameterValue("key", key);
		tester.executeBehavior(table.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));
	}

	private static class Only extends TextRowAction<Branch>
	{
		private static final long serialVersionUID = 1L;

		final List<String> done = new ArrayList<>();

		int cancelled;

		Only()
		{
			super("only", Model.of("Show only <this> one"));
		}

		@Override
		public boolean isVisible(Branch row)
		{
			return row.getId() != 3;
		}

		@Override
		public boolean isEnabled(Branch row)
		{
			return row.getId() != 4;
		}

		@Override
		public Model<String> getDisabledReason(Branch row)
		{
			return Model.of("Archived");
		}

		@Override
		protected void onRowAction(Branch row, AjaxRequestTarget target)
		{
			done.add(row.getName());
		}

		@Override
		protected void onCancel(AjaxRequestTarget target)
		{
			cancelled++;
		}
	}

	private static class Edit extends IconBasedRowAction<Branch>
	{
		private static final long serialVersionUID = 1L;

		Edit()
		{
			super("edit", IconBasedRowAction.Icon.EDIT, Model.of("Edit"));
		}

		@Override
		protected void onRowAction(Branch row, AjaxRequestTarget target)
		{
		}
	}

	private static class Table extends BeanBasedDynamicDataTable<Branch, Long, String>
	{
		private static final long serialVersionUID = 1L;

		final List<String> notFound = new ArrayList<>();

		Table(List<IColumn<Branch, String>> columns, BranchesProvider provider)
		{
			super("table", columns, provider, 10);
		}

		@Override
		protected void onNotFoundAction(AjaxRequestTarget target, String action, Long key,
			Branch row)
		{
			notFound.add(action + " " + key);
		}
	}

	public static class Branch implements Serializable, IIdentifiable<Long>
	{
		private static final long serialVersionUID = 1L;

		private final long id;

		private final String name;

		Branch(long id, String name)
		{
			this.id = id;
			this.name = name;
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
	}

	static class BranchesProvider extends SortableDynamicDataProvider<Branch, Long, String>
		implements
			IIdentifiableDynamicDataProvider<Branch, Long>
	{
		private static final long serialVersionUID = 1L;

		final List<Branch> branches = new ArrayList<>(List.of(new Branch(1, "main"),
			new Branch(2, "north"), new Branch(3, "south"), new Branch(4, "old")));

		BranchesProvider()
		{
			super(Long.class);
		}

		@Override
		public Iterator<Branch> iterator(long first, long count)
		{
			return branches.subList((int)first, (int)Math.min(branches.size(), first + count))
				.iterator();
		}

		@Override
		public long size()
		{
			return branches.size();
		}

		@Override
		public Branch findByKey(Long key)
		{
			return branches.stream().filter(branch -> branch.getId().equals(key)).findFirst()
				.orElse(null);
		}
	}

	public static class TablePage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		TablePage(BaseDynamicDataTable<Branch, Long, ?> table)
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
