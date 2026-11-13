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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.dynamicdatatable.TextRowActionTest.Branch;
import com.antiliacode.widgets.dynamicdatatable.TextRowActionTest.BranchesProvider;
import com.antiliacode.widgets.dynamicdatatable.TextRowActionTest.TablePage;

/**
 * The value of a cell as the link of a row action: {@link BeanActionLinkColumn} in a bean based
 * table and {@link ActionLinkColumn} in a JSON based one.
 */
class ActionLinkColumnTest extends WicketTestCase
{
	private BranchesProvider provider;

	private BaseDynamicDataTable<Branch, Long, ?> table;

	private final List<String> notFound = new ArrayList<>();

	private Filter filter;

	@Test
	void aBeanCellShowsTheValueOfTheRowAsALinkRunningTheAction()
	{
		startBean(new BeanActionLinkColumn<>(Model.of("Branch"), Branch::getName, "only"));

		assertTrue(rowMarkup("2").contains("<button type=\"button\" " +
			"class=\"ant-data-table-action-link\" data-dt-action=\"only\" " +
			"data-dt-unavailable=\"text\">north</button>"), rowMarkup("2"));

		executeAction("only", "2");
		assertEquals(List.of("north"), filter.done);
	}

	@Test
	void theValueIsEscaped()
	{
		provider = new BranchesProvider();
		provider.branches.set(0, new Branch(1, "<main>"));
		startBean(new BeanActionLinkColumn<>(Model.of("Branch"), Branch::getName, "only"));

		assertTrue(rowMarkup("1").contains(">&lt;main&gt;</button>"), rowMarkup("1"));
	}

	@Test
	void onARowTheActionIsHiddenOrDisabledOnTheValueIsText()
	{
		startBean(new BeanActionLinkColumn<>(Model.of("Branch"), Branch::getName, "only"));

		assertFalse(rowMarkup("3").contains("data-dt-action"), rowMarkup("3"));
		assertTrue(rowMarkup("3").contains(">south<"), rowMarkup("3"));
		assertFalse(rowMarkup("4").contains("ant-data-table-action-link"), rowMarkup("4"));

		executeAction("only", "3");
		executeAction("only", "4");
		assertTrue(filter.done.isEmpty());
		assertEquals(List.of("only 3", "only 4"), notFound);
	}

	@Test
	void aColumnGivenTheActionRegistersIt()
	{
		filter = new Filter();
		BeanActionLinkColumn<Branch, String> column = new BeanActionLinkColumn<>(
			Model.of("Branch"), Branch::getName, filter);
		startBean(column, false);

		assertSame(filter, table.getActionContributor("only"));
		assertEquals(List.of(filter), column.getActionContributors());
		executeAction("only", "1");
		assertEquals(List.of("main"), filter.done);
	}

	@Test
	void anActionThatIsNotRegisteredLeavesTheValueAsText()
	{
		filter = new Filter();
		List<IColumn<Branch, String>> columns = new ArrayList<>();
		columns.add(new BeanActionLinkColumn<>(Model.of("Branch"), Branch::getName, "nowhere"));
		start(new BeanBasedDynamicDataTable<>("table", columns, new BranchesProvider(), 10));

		assertFalse(rowMarkup("1").contains("data-dt-action"), rowMarkup("1"));
	}

	@Test
	void theBeanColumnIsExportedAndSortedByTheValue()
	{
		BeanActionLinkColumn<Branch, String> column = new BeanActionLinkColumn<Branch, String>(
			Model.of("Branch"), Branch::getName, "only").setSortProperty("name");
		startBean(column);

		assertTrue(column.isSortable());
		assertEquals("name", column.getSortProperty());
		@SuppressWarnings("unchecked")
		BeanBasedDynamicDataTable<Branch, Long, String> beanTable =
			(BeanBasedDynamicDataTable<Branch, Long, String>)table;
		assertTrue(beanTable.isColumnExportable(column));
		assertEquals("north", beanTable.getColumnExportValue(column, provider.findByKey(2L)));
	}

	@Test
	void theJsonColumnWritesALinkTemplateTheBrowserTurnsToTextWhereTheActionIsUnavailable()
	{
		filter = new Filter();
		ActionLinkColumn<Branch> column = new ActionLinkColumn<>(Model.of("Branch"), "name",
			filter);
		column.setExportValue(Branch::getName);
		provider = new BranchesProvider();
		start(new JsonBasedDynamicDataTable<Branch, Long>("table",
			List.<IDynamicColumn<Branch>> of(column), provider, 10)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onNotFoundAction(AjaxRequestTarget target, String action, Long key,
				Branch row)
			{
				notFound.add(action + " " + key);
			}
		});

		assertEquals("<button type=\"button\" class=\"ant-data-table-action-link\" " +
			"data-dt-action=\"only\" data-dt-unavailable=\"text\">{{name}}</button>",
			column.getTemplate());
		assertSame(filter, table.getActionContributor("only"));
		assertEquals(List.of("only"), table.getHiddenActions(provider.findByKey(3L)));
		assertEquals("north", column.getExportValue(provider.findByKey(2L)));

		executeAction("only", "2");
		executeAction("only", "3");
		assertEquals(List.of("north"), filter.done);
		assertEquals(List.of("only 3"), notFound);
	}

	@Test
	void aJsonColumnOfAnActionRegisteredOtherwiseRegistersNothing()
	{
		ActionLinkColumn<Branch> column = new ActionLinkColumn<>(Model.of("Branch"), "name",
			"only");

		assertEquals("only", column.getActionId());
		assertTrue(column.getActionContributors().isEmpty());
	}

	private void startBean(BeanActionLinkColumn<Branch, String> column)
	{
		startBean(column, true);
	}

	private void startBean(BeanActionLinkColumn<Branch, String> column, boolean actionsColumn)
	{
		if (provider == null)
		{
			provider = new BranchesProvider();
		}
		if (filter == null)
		{
			filter = new Filter();
		}
		List<IColumn<Branch, String>> columns = new ArrayList<>();
		columns.add(column);
		columns.add(new PropertyColumn<>(Model.of("Id"), "id"));
		if (actionsColumn)
		{
			columns.add(new BeanActionsColumn<>(Model.of("Actions"), List.of(filter)));
		}
		start(new BeanBasedDynamicDataTable<Branch, Long, String>("table", columns, provider, 10)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onNotFoundAction(AjaxRequestTarget target, String action, Long key,
				Branch row)
			{
				notFound.add(action + " " + key);
			}
		});
	}

	private void start(BaseDynamicDataTable<Branch, Long, ?> table)
	{
		this.table = table;
		tester.startPage(new TablePage(table));
	}

	private String rowMarkup(String key)
	{
		return org.apache.wicket.util.tester.TagTester.createTagByAttribute(
			tester.getLastResponseAsString(), "data-key", key).getValue();
	}

	private void executeAction(String action, String key)
	{
		tester.getRequest().getPostParameters().setParameterValue("action", action);
		tester.getRequest().getPostParameters().setParameterValue("key", key);
		tester.executeBehavior(table.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));
	}

	private static class Filter extends TextRowAction<Branch>
	{
		private static final long serialVersionUID = 1L;

		final List<String> done = new ArrayList<>();

		Filter()
		{
			super("only", Model.of("Show only this one"));
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
		protected void onRowAction(Branch row, AjaxRequestTarget target)
		{
			done.add(row.getName());
		}
	}
}
