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
package com.antiliacode.widgets.split;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

class SplitPanelTest extends WicketTestCase
{
	@Test
	void rendersTwoPanesSideBySideWithASeparatorBetweenThem()
	{
		TestPanel panel = new TestPanel(SplitOrientation.HORIZONTAL);
		tester.startPage(new TestPage(panel));

		TagTester split = tester.getTagByWicketId("split");
		assertEquals("antilia-split antilia-split-horizontal", split.getAttribute("class"));
		assertEquals("One", tester.getTagByWicketId("first").getValue());
		assertEquals("Two", tester.getTagByWicketId("second").getValue());

		TagTester divider = tester.getTagByWicketId("divider");
		assertEquals("separator", divider.getAttribute("role"));
		assertEquals("vertical", divider.getAttribute("aria-orientation"));
		assertEquals(tester.getTagByWicketId("firstPane").getAttribute("id"),
			divider.getAttribute("aria-controls"));
		assertEquals("50", divider.getAttribute("aria-valuenow"));
		assertEquals("0", divider.getAttribute("aria-valuemin"));
		assertEquals("100", divider.getAttribute("aria-valuemax"));
		assertEquals("0", divider.getAttribute("tabindex"));
		assertEquals("Drag, or use the arrow keys, to resize the panes",
			divider.getAttribute("aria-label"));

		tester.assertContains("antilia-split-panel.*\\.js");
		tester.assertContains("antilia-split-panel.*\\.css");
		tester.assertContains("Wicket.SplitPanel.attach\\(\"" + panel.getSplitMarkupId() +
			"\", \\{\"orientation\":\"horizontal\",\"position\":50,\"resizable\":true," +
			"\"collapsible\":false,");
		assertFalse(tester.getLastResponseAsString().contains("style="), "no inline style");
		assertFalse(tester.getLastResponseAsString().contains("onChange"));
		assertFalse(tester.getLastResponseAsString().contains("collapseFirstLabel"));
		assertNull(panel.getModelObject());
		assertEquals(SplitState.DEFAULT, panel.getState());
	}

	@Test
	void rendersThePanesOneAboveTheOtherWhenVertical()
	{
		TestPanel panel = new TestPanel(SplitOrientation.VERTICAL);
		panel.setPosition(30).setMin(20).setMax(70.5).setStep(10);
		tester.startPage(new TestPage(panel));

		assertEquals("antilia-split antilia-split-vertical",
			tester.getTagByWicketId("split").getAttribute("class"));
		TagTester divider = tester.getTagByWicketId("divider");
		assertEquals("horizontal", divider.getAttribute("aria-orientation"));
		assertEquals("30", divider.getAttribute("aria-valuenow"));
		assertEquals("20", divider.getAttribute("aria-valuemin"));
		assertEquals("70.5", divider.getAttribute("aria-valuemax"));
		tester.assertContains("\"orientation\":\"vertical\",\"position\":30");
		tester.assertContains("\"min\":20,\"max\":70.5,\"step\":10");
	}

	@Test
	void keepsThePositionWithinItsLimits()
	{
		TestPanel panel = new TestPanel(SplitOrientation.HORIZONTAL);
		panel.setMax(60).setMinSecond(45).setPosition(150);
		assertEquals(SplitSize.percent(100), panel.getPosition());
		tester.startPage(new TestPage(panel));

		assertEquals("55", tester.getTagByWicketId("divider").getAttribute("aria-valuenow"));
		assertEquals("55", tester.getTagByWicketId("divider").getAttribute("aria-valuemax"));
		tester.assertContains("\"position\":55");
		tester.assertContains("\"minSecond\":45");
	}

	@Test
	void takesSizesInPixels()
	{
		TestPanel panel = new TestPanel(SplitOrientation.HORIZONTAL);
		panel.setPosition(SplitSize.pixels(250)).setMin(SplitSize.pixels(300))
			.setMax(SplitSize.percent(50)).setMinSecond(SplitSize.pixels(200))
			.setStep(SplitSize.pixels(20));
		tester.startPage(new TestPage(panel));

		assertEquals("antilia-split antilia-split-horizontal antilia-split-pixels",
			tester.getTagByWicketId("split").getAttribute("class"));
		TagTester divider = tester.getTagByWicketId("divider");
		// the script works the separator's value out from the space the panes have
		assertNull(divider.getAttribute("aria-valuenow"));
		assertNull(divider.getAttribute("aria-valuemin"));
		tester.assertContains("\"position\":\"300px\"");
		tester.assertContains("\"min\":\"300px\",\"max\":50,\"minSecond\":\"200px\"," +
			"\"step\":\"20px\"");
		assertFalse(tester.getLastResponseAsString().contains("style="), "no inline style");
	}

	@Test
	void rendersAFixedDivider()
	{
		TestPanel panel = new TestPanel(SplitOrientation.HORIZONTAL);
		panel.setResizable(false).setSendToServer(true).setCollapsible(true);
		tester.startPage(new TestPage(panel));

		assertEquals("antilia-split antilia-split-horizontal antilia-split-fixed " +
			"antilia-split-collapsible", tester.getTagByWicketId("split").getAttribute("class"));
		TagTester divider = tester.getTagByWicketId("divider");
		assertNull(divider.getAttribute("tabindex"));
		assertNull(divider.getAttribute("aria-valuemin"));
		assertEquals("true", divider.getAttribute("aria-disabled"));
		tester.assertContains("\"resizable\":false");
		assertFalse(tester.getLastResponseAsString().contains("onChange"));
	}

	@Test
	void labelsTheDividerInTheSessionsLanguage()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		tester.startPage(new TestPage(new TestPanel(SplitOrientation.VERTICAL)
			.setCollapsible(true)));

		assertEquals("Ziehen oder die Pfeiltasten benutzen, um die Bereiche anzupassen",
			tester.getTagByWicketId("divider").getAttribute("aria-label"));
		tester.assertContains("\"collapseFirstLabel\":\"Oberen Bereich ausblenden\"");
		tester.assertContains("\"collapseSecondLabel\":\"Unteren Bereich ausblenden\"");
		tester.assertContains("\"restoreLabel\":\"Beide Bereiche zeigen\"");
	}

	@Test
	void stacksThePanesSideBySideBelowAWidth()
	{
		TestPanel panel = new TestPanel(SplitOrientation.HORIZONTAL);
		panel.setStackBelow(720).setCollapsible(true);
		assertEquals(720, panel.getStackBelow());
		tester.startPage(new TestPage(panel));

		// the browser stacks them, from the width the split panel has there
		assertEquals("antilia-split antilia-split-horizontal antilia-split-collapsible",
			tester.getTagByWicketId("split").getAttribute("class"));
		tester.assertContains("\"stackBelow\":720");
		tester.assertContains("\"collapseFirstLabel\":\"Hide the left pane\"");
		tester.assertContains("\"stackedCollapseFirstLabel\":\"Hide the top pane\"");
		tester.assertContains("\"stackedCollapseSecondLabel\":\"Hide the bottom pane\"");

		panel.setStackBelow(0);
		tester.startPage(new TestPage(panel));
		assertFalse(tester.getLastResponseAsString().contains("stackBelow"));
		assertFalse(tester.getLastResponseAsString().contains("stackedCollapseFirstLabel"));

		assertThrows(IllegalArgumentException.class, () -> panel.setStackBelow(-1));
	}

	@Test
	void stacksNoPanesOneAboveTheOtherAlready()
	{
		TestPanel panel = new TestPanel(SplitOrientation.VERTICAL);
		panel.setStackBelow(720);
		tester.startPage(new TestPage(panel));

		assertFalse(tester.getLastResponseAsString().contains("stackBelow"));
	}

	@Test
	void rendersACollapsedPane()
	{
		TestPanel panel = new TestPanel(SplitOrientation.HORIZONTAL);
		panel.setCollapsible(true).setCollapsed(SplitPane.SECOND).setPosition(30);
		tester.startPage(new TestPage(panel));

		assertEquals("antilia-split antilia-split-horizontal antilia-split-collapsible " +
			"antilia-split-collapsed-second", tester.getTagByWicketId("split").getAttribute("class"));
		TagTester divider = tester.getTagByWicketId("divider");
		assertEquals("100", divider.getAttribute("aria-valuenow"));
		assertEquals("0", divider.getAttribute("aria-valuemin"));
		assertEquals("100", divider.getAttribute("aria-valuemax"));
		tester.assertContains("\"position\":30,\"resizable\":true,\"collapsible\":true,");
		tester.assertContains("\"collapsed\":\"second\"");
		tester.assertContains("\"collapseFirstLabel\":\"Hide the left pane\"");
		tester.assertContains("\"collapseSecondLabel\":\"Hide the right pane\"");

		panel.setCollapsible(false);
		assertNull(panel.getCollapsed());
	}

	@Test
	void keepsWhatTheBrowserSendsInItsModel()
	{
		TestPanel panel = new TestPanel(SplitOrientation.HORIZONTAL);
		panel.setMax(80).setCollapsible(true).setSendToServer(true);
		tester.startPage(new TestPage(panel));
		tester.assertContains("\"onChange\":function ?\\(position,collapsed\\)");

		send(panel, "33.5", "");
		assertEquals(SplitSize.percent(33.5), panel.getPosition());
		assertEquals(new SplitState(SplitSize.percent(33.5), null), panel.getModelObject());
		assertEquals(1, panel.resizes);
		assertEquals(0, panel.collapses);

		send(panel, "95", "first");
		assertEquals(SplitSize.percent(80), panel.getPosition());
		assertEquals(SplitPane.FIRST, panel.getCollapsed());
		assertEquals(2, panel.resizes);
		assertEquals(1, panel.collapses);

		// nothing changed, or nothing to read: nothing to tell
		send(panel, "80", "first");
		send(panel, "not a number", "first");
		assertEquals(2, panel.resizes);
		assertEquals(1, panel.collapses);

		send(panel, "80", "");
		assertNull(panel.getCollapsed());
		assertEquals(2, panel.collapses);
	}

	@Test
	void collapsesAPaneFromTheServerWithoutRenderingItAgain()
	{
		TestPanel panel = new TestPanel(SplitOrientation.HORIZONTAL);
		panel.setCollapsible(true);
		CollapsePage page = new CollapsePage(panel);
		tester.startPage(page);
		String id = panel.getSplitMarkupId();

		page.collapse = SplitPane.SECOND;
		tester.clickLink("act");
		assertEquals(SplitPane.SECOND, panel.getCollapsed());
		assertEquals(new SplitState(SplitSize.percent(50), SplitPane.SECOND), panel.getModelObject());
		tester.assertContains("split.setCollapsed\\(\"second\"\\); \\} \\}\\)\\(" +
			"Wicket.SplitPanel.of\\(\"" + id + "\"\\)\\);");
		assertFalse(tester.getLastResponseAsString().contains("Wicket.SplitPanel.attach"),
			"the split panel is not rendered again");
		assertEquals(0, panel.collapses);

		// the same again: nothing to tell
		tester.clickLink("act");
		assertFalse(tester.getLastResponseAsString().contains("setCollapsed"));

		page.collapse = null;
		tester.clickLink("act");
		assertNull(panel.getCollapsed());
		tester.assertContains("split.setCollapsed\\(null\\)");

		// a split panel that is not collapsible collapses none
		panel.setCollapsible(false);
		page.collapse = SplitPane.FIRST;
		tester.clickLink("act");
		assertNull(panel.getCollapsed());
		assertFalse(tester.getLastResponseAsString().contains("setCollapsed"));
	}

	@Test
	void keepsSizesInPixelsTheBrowserSends()
	{
		TestPanel panel = new TestPanel(SplitOrientation.HORIZONTAL);
		panel.setPosition(SplitSize.pixels(250)).setMin(SplitSize.pixels(100))
			.setMax(SplitSize.pixels(400)).setSendToServer(true);
		tester.startPage(new TestPage(panel));

		send(panel, "320px", "first");
		assertEquals(SplitSize.pixels(320), panel.getPosition());
		// a split panel that is not collapsible collapses none
		assertNull(panel.getCollapsed());

		send(panel, "20px", "");
		assertEquals(SplitSize.pixels(100), panel.getPosition());
		send(panel, "900px", "");
		assertEquals(SplitSize.pixels(400), panel.getPosition());
	}

	@Test
	void ignoresWhatTheBrowserSendsWhenTheDividerIsFixed()
	{
		TestPanel panel = new TestPanel(SplitOrientation.HORIZONTAL);
		panel.setSendToServer(true);
		tester.startPage(new TestPage(panel));
		panel.setResizable(false);

		send(panel, "20", "");
		assertNull(panel.getModelObject());
	}

	@Test
	void rendersAgainWhereTheUserPutTheDivider()
	{
		TestPanel panel = new TestPanel(SplitOrientation.HORIZONTAL);
		panel.setSendToServer(true).setCollapsible(true);
		tester.startPage(new TestPage(panel));
		send(panel, "25", "first");

		tester.startPage(tester.getLastRenderedPage());

		assertEquals("0", tester.getTagByWicketId("divider").getAttribute("aria-valuenow"));
		tester.assertContains("\"position\":25");
		tester.assertContains("\"collapsed\":\"first\"");
	}

	@Test
	void keepsItsStateInTheModelItIsGiven()
	{
		StoredModel model = new StoredModel();
		model.stored = new SplitState(SplitSize.pixels(180), null);
		TestPanel panel = new TestPanel(model);
		panel.setPosition(40).setSendToServer(true);
		tester.startPage(new TestPage(panel));

		tester.assertContains("\"position\":\"180px\"");
		send(panel, "210px", "");

		assertEquals(new SplitState(SplitSize.pixels(210), null), model.stored);
		// a new panel on the same model starts where the user left the divider
		tester.startPage(new TestPage(new TestPanel(model).setPosition(40)));
		tester.assertContains("\"position\":\"210px\"");

		panel.setModelObject(null);
		assertEquals(SplitSize.percent(40), panel.getPosition());
	}

	@Test
	void splitPanelsGoInEachOthersPanes()
	{
		tester.startPage(new TestPage(new TestPanel(SplitOrientation.HORIZONTAL)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newSecondPane(String id)
			{
				return new TestPanel(id, SplitOrientation.VERTICAL).setSendToServer(true);
			}
		}));

		SplitPanel inner = (SplitPanel)tester.getComponentFromLastRenderedPage(
			"panel:split:second");
		assertTrue(inner.isVisibleInHierarchy());
		tester.assertContains("Wicket.SplitPanel.attach\\(\"" + inner.getSplitMarkupId() +
			"\", \\{\"orientation\":\"vertical\"");
		// the Ajax goes out only when its element is in the page: that of the panes, whose tag is
		// rendered when the panel's is not
		tester.assertContains("\"c\":\"" + inner.getSplitMarkupId() + "\"");
	}

	@Test
	void readsAndWritesSizes()
	{
		assertEquals(SplitSize.pixels(250), SplitSize.parse(" 250px "));
		assertEquals(SplitSize.percent(30), SplitSize.parse("30%"));
		assertEquals(SplitSize.percent(12.5), SplitSize.parse("12.5"));
		assertEquals(SplitSize.percent(100), SplitSize.parse("150"));
		assertNull(SplitSize.parse("wide"));
		assertNull(SplitSize.parse(null));
		assertEquals("250px", SplitSize.pixels(250).toString());
		assertEquals("12.5%", SplitSize.percent(12.5).toString());
		assertEquals(0, SplitSize.pixels(-5).getValue());
		assertTrue(SplitSize.pixels(1).isPixels());
		assertNotEquals(SplitSize.pixels(30), SplitSize.percent(30));
		assertEquals(SplitSize.pixels(30).hashCode(), SplitSize.parse("30px").hashCode());

		SplitState state = new SplitState(SplitSize.pixels(30), SplitPane.FIRST);
		assertEquals("30px, first collapsed", state.toString());
		assertEquals("30px", state.withCollapsed(null).toString());
		assertEquals(new SplitState(SplitSize.percent(40), SplitPane.FIRST),
			state.withPosition(SplitSize.percent(40)));
		assertEquals(state.hashCode(), new SplitState(SplitSize.pixels(30), SplitPane.FIRST)
			.hashCode());
		assertNotEquals(state, "30px");
		assertEquals(SplitPane.SECOND, SplitPane.fromCssName("second"));
		assertNull(SplitPane.fromCssName("third"));
	}

	private void send(SplitPanel panel, String position, String collapsed)
	{
		AbstractDefaultAjaxBehavior behavior = panel.get("split")
			.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0);
		tester.getRequest().setParameter("position", position);
		tester.getRequest().setParameter("collapsed", collapsed);
		tester.executeBehavior(behavior);
	}

	/**
	 * A model storing the state elsewhere than in the page, as one storing it in the session or
	 * with the user's preferences would.
	 */
	private static class StoredModel implements IModel<SplitState>
	{
		private static final long serialVersionUID = 1L;

		SplitState stored;

		@Override
		public SplitState getObject()
		{
			return stored;
		}

		@Override
		public void setObject(SplitState object)
		{
			stored = object;
		}
	}

	private static class TestPanel extends SplitPanel
	{
		private static final long serialVersionUID = 1L;

		int resizes;

		int collapses;

		TestPanel(SplitOrientation orientation)
		{
			super("panel", orientation);
		}

		TestPanel(String id, SplitOrientation orientation)
		{
			super(id, orientation);
		}

		TestPanel(IModel<SplitState> model)
		{
			super("panel", model);
		}

		@Override
		protected Component newFirstPane(String id)
		{
			return new Label(id, "One");
		}

		@Override
		protected Component newSecondPane(String id)
		{
			return new Label(id, "Two");
		}

		@Override
		protected void onResized(AjaxRequestTarget target)
		{
			resizes++;
		}

		@Override
		protected void onCollapsed(AjaxRequestTarget target)
		{
			collapses++;
		}
	}

	/**
	 * A page with a link collapsing a pane of its split panel.
	 */
	public static class CollapsePage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		SplitPane collapse;

		CollapsePage(SplitPanel panel)
		{
			add(panel);
			add(new AjaxLink<Void>("act")
			{
				private static final long serialVersionUID = 1L;

				@Override
				public void onClick(AjaxRequestTarget target)
				{
					panel.collapse(collapse, target);
				}
			});
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body>" +
				"<div wicket:id=\"panel\"></div><a wicket:id=\"act\">Act</a></body></html>");
		}
	}

	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		TestPage(SplitPanel panel)
		{
			add(panel);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body>" +
				"<div wicket:id=\"panel\"></div></body></html>");
		}
	}
}
