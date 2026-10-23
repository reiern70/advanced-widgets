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
package com.antiliacode.widgets.demo.split;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;

import com.antiliacode.widgets.demo.DemoPage;
import com.antiliacode.widgets.demo.frameworks.FrameworkTabs;
import com.antiliacode.widgets.scrollbar.ScrollbarBehavior;
import com.antiliacode.widgets.split.SplitOrientation;
import com.antiliacode.widgets.split.SplitPane;
import com.antiliacode.widgets.split.SplitPanel;
import com.antiliacode.widgets.split.SplitSize;
import com.antiliacode.widgets.split.SplitState;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * {@link SplitPanel}: the layout of an editor, the files side by side with an editor above its
 * console, two split panels one in the other. The files keep their width in pixels, and either
 * split panel collapses a pane with the buttons on its divider. Both send the server what the
 * user changes, and say so below them by Ajax: the files' split panel keeps it in the session, a
 * model of its own, so that the page shows it again when opened anew; the editor's in the page,
 * as by default. A third split panel's orientation, whether its divider moves and whether it
 * collapses its panes, three check boxes switch by Ajax. The React and Svelte tabs show the same
 * in those frameworks. Only the body of a pane scrolls, below its title, with the scroll bars of
 * {@link ScrollbarBehavior}.
 */
public class SplitPanelPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	/** The files of the editor's project. */
	static final List<String> FILES = List.of("pom.xml", "src/main/java/", "  HomePage.java",
		"  HomePage.html", "  WicketApplication.java", "src/test/java/", "  HomePageTest.java");

	/** What the editor shows: how the layout above is made. */
	static final String CODE = """
		SplitPanel layout = new SplitPanel("layout", new SessionModel()) {
		    protected Component newFirstPane(String id) {
		        return new FilesPanel(id);
		    }
		    protected Component newSecondPane(String id) {
		        return new SplitPanel(id, SplitOrientation.VERTICAL) {
		            protected Component newFirstPane(String id) {
		                return new EditorPanel(id);
		            }
		            protected Component newSecondPane(String id) {
		                return new ConsolePanel(id);
		            }
		        }.setPosition(65).setMinSecond(SplitSize.pixels(60))
		            .setCollapsible(true).setSendToServer(true);
		    }
		};
		layout.setPosition(SplitSize.pixels(220))
		    .setMin(SplitSize.pixels(140)).setMax(50)
		    .setCollapsible(true).setSendToServer(true);""";

	/** What the console shows. */
	static final String CONSOLE = """
		[INFO] Building antilia-demo 1.0.0-SNAPSHOT
		[INFO] Compiling 42 source files
		[INFO] Tests run: 120, Failures: 0, Errors: 0
		[INFO] BUILD SUCCESS""";

	/** Where the divider of the files starts, and how far it goes. */
	static final SplitSize FILES_POSITION = SplitSize.pixels(220);

	static final SplitSize FILES_MIN = SplitSize.pixels(140);

	static final SplitSize FILES_MAX = SplitSize.percent(50);

	/** Where the divider of the editor starts, and how far it goes. */
	static final SplitSize EDITOR_POSITION = SplitSize.percent(65);

	static final SplitSize EDITOR_MIN = SplitSize.percent(20);

	static final SplitSize CONSOLE_MIN = SplitSize.pixels(60);

	/** The session attribute keeping the state of the files' split panel. */
	static final String LAYOUT_ATTRIBUTE = "split-panel-demo.layout";

	private final SplitPanel layout;

	private SplitPanel editor;

	private boolean vertical;

	private boolean resizable = true;

	private boolean collapsible = true;

	/**
	 * Constructor.
	 */
	public SplitPanelPage()
	{
		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);

		Label sizes = new Label("sizes", this::describeSizes);
		sizes.setOutputMarkupId(true);
		themed.add(sizes);

		layout = new SplitPanel("layout", new SessionModel())
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newFirstPane(String id)
			{
				Fragment files = new Fragment(id, "files", SplitPanelPage.this);
				WebMarkupContainer body = body();
				files.add(body);
				body.add(new ListView<>("file", FILES)
				{
					private static final long serialVersionUID = 1L;

					@Override
					protected void populateItem(ListItem<String> item)
					{
						item.add(new Label("name", item.getModel()));
					}
				});
				return files;
			}

			@Override
			protected Component newSecondPane(String id)
			{
				editor = new SplitPanel(id, SplitOrientation.VERTICAL)
				{
					private static final long serialVersionUID = 1L;

					@Override
					protected Component newFirstPane(String id)
					{
						return new Fragment(id, "editor", SplitPanelPage.this)
							.add(body().add(new Label("code", CODE)));
					}

					@Override
					protected Component newSecondPane(String id)
					{
						return new Fragment(id, "console", SplitPanelPage.this)
							.add(body().add(new Label("output", CONSOLE)));
					}

					@Override
					protected void onResized(AjaxRequestTarget target)
					{
						target.add(sizes);
					}

					@Override
					protected void onCollapsed(AjaxRequestTarget target)
					{
						target.add(sizes);
					}
				};
				return editor.setPosition(EDITOR_POSITION).setMin(EDITOR_MIN)
					.setMinSecond(CONSOLE_MIN).setCollapsible(true).setSendToServer(true);
			}

			@Override
			protected void onResized(AjaxRequestTarget target)
			{
				target.add(sizes);
			}

			@Override
			protected void onCollapsed(AjaxRequestTarget target)
			{
				target.add(sizes);
			}
		};
		layout.setPosition(FILES_POSITION).setMin(FILES_MIN).setMax(FILES_MAX)
			.setCollapsible(true).setSendToServer(true);
		themed.add(layout);

		SplitPanel settings = new SplitPanel("settings")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setOrientation(vertical ? SplitOrientation.VERTICAL : SplitOrientation.HORIZONTAL);
				setResizable(resizable);
				setCollapsible(collapsible);
			}

			@Override
			protected Component newFirstPane(String id)
			{
				return text(id, "first");
			}

			@Override
			protected Component newSecondPane(String id)
			{
				return text(id, "second");
			}
		};
		settings.setSendToServer(true).setOutputMarkupId(true);
		themed.add(settings);

		Form<Void> form = new Form<>("form");
		themed.add(form);
		form.add(rerender(new CheckBox("vertical", new PropertyModel<>(this, "vertical")),
			settings));
		form.add(rerender(new CheckBox("resizable", new PropertyModel<>(this, "resizable")),
			settings));
		form.add(rerender(new CheckBox("collapsible", new PropertyModel<>(this, "collapsible")),
			settings));
	}

	private Fragment text(String id, String key)
	{
		Fragment text = new Fragment(id, "text", this);
		text.add(body().add(new Label("text", new ResourceModel(key))));
		return text;
	}

	/**
	 * @return the body of a pane, below its title: the only part that scrolls, with the scroll bars
	 *         of ScrollbarBehavior rather than the browser's
	 */
	private static WebMarkupContainer body()
	{
		WebMarkupContainer body = new WebMarkupContainer("body");
		body.add(new ScrollbarBehavior());
		return body;
	}

	private static CheckBox rerender(CheckBox checkBox, Component component)
	{
		checkBox.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				target.add(component);
			}
		});
		return checkBox;
	}

	/**
	 * @return the sizes of the files and of the editor, in words
	 */
	private String describeSizes()
	{
		return new StringResourceModel("sizes", this).setParameters(describe(getFilesState()),
			describe(getEditorState())).getString();
	}

	/**
	 * @return the size of the first pane in words: in its unit, none or all of it
	 */
	private String describe(SplitState state)
	{
		if (state.getCollapsed() != null)
		{
			return getString(state.getCollapsed() == SplitPane.FIRST ? "none" : "all");
		}
		SplitSize position = state.getPosition();
		return Math.round(position.getValue()) + (position.isPixels() ? " px" : " %");
	}

	@Override
	protected String getFrameworkDemo()
	{
		return "splitPanel";
	}

	/**
	 * @return the texts, the content of the panes and where the dividers are as the page has them
	 */
	@Override
	protected JSONObject getFrameworkConfig()
	{
		JSONObject texts = FrameworkTabs.texts(this, "layoutTitle", "settingsTitle", "files",
			"editor", "console", "vertical", "resizable", "collapsible", "sizes", "none", "all",
			"first", "second");
		texts.put("divider", getString("SplitPanel.divider"))
			.put("collapseLeft", getString("SplitPanel.collapseLeft"))
			.put("collapseRight", getString("SplitPanel.collapseRight"))
			.put("collapseTop", getString("SplitPanel.collapseTop"))
			.put("collapseBottom", getString("SplitPanel.collapseBottom"))
			.put("restore", getString("SplitPanel.restore"));
		JSONObject layoutConfig = new JSONObject()
			.put("files", state(getFilesState()).put("min", json(FILES_MIN))
				.put("max", json(FILES_MAX)))
			.put("editor", state(getEditorState()).put("min", json(EDITOR_MIN))
				.put("minSecond", json(CONSOLE_MIN)));
		return new JSONObject()
			.put("texts", texts)
			.put("files", new JSONArray(FILES))
			.put("code", CODE)
			.put("console", CONSOLE)
			.put("layout", layoutConfig)
			.put("vertical", vertical)
			.put("resizable", resizable)
			.put("collapsible", collapsible);
	}

	private static JSONObject state(SplitState state)
	{
		return new JSONObject().put("position", json(state.getPosition()))
			.put("collapsed", state.getCollapsed() == null ? JSONObject.NULL :
				state.getCollapsed().cssName());
	}

	/**
	 * @return the size as the frontend takes it: a number in percent, a string in pixels
	 */
	private static Object json(SplitSize size)
	{
		return size.isPixels() ? size.toString() : size.getValue();
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		SplitPanel.renderHeadItems(response);
		// the React and Svelte apps draw the same scroll bars
		ScrollbarBehavior.renderHeadItems(response);
		response.render(CssHeaderItem.forReference(
			new CssResourceReference(SplitPanelPage.class, "SplitPanelPage.css")));
	}

	/**
	 * @return where the divider of the files is and whether a pane is collapsed
	 */
	public SplitState getFilesState()
	{
		return layout.getState();
	}

	/**
	 * @return where the divider of the editor is and whether a pane is collapsed
	 */
	public SplitState getEditorState()
	{
		return editor != null ? editor.getState() : new SplitState(EDITOR_POSITION, null);
	}

	/**
	 * @return whether the panes of the third split panel are one above the other
	 */
	public boolean isVertical()
	{
		return vertical;
	}

	/**
	 * @param vertical
	 *            whether the panes of the third split panel are one above the other
	 */
	public void setVertical(boolean vertical)
	{
		this.vertical = vertical;
	}

	/**
	 * @return whether the divider of the third split panel moves
	 */
	public boolean isResizable()
	{
		return resizable;
	}

	/**
	 * @param resizable
	 *            whether the divider of the third split panel moves
	 */
	public void setResizable(boolean resizable)
	{
		this.resizable = resizable;
	}

	/**
	 * @return whether the third split panel collapses its panes
	 */
	public boolean isCollapsible()
	{
		return collapsible;
	}

	/**
	 * @param collapsible
	 *            whether the third split panel collapses its panes
	 */
	public void setCollapsible(boolean collapsible)
	{
		this.collapsible = collapsible;
	}

	/**
	 * Keeps the state of the files' split panel in the session, rather than in the page: a page
	 * opened anew shows the divider where the user left it.
	 */
	static class SessionModel implements IModel<SplitState>
	{
		private static final long serialVersionUID = 1L;

		@Override
		public SplitState getObject()
		{
			return (SplitState)Session.get().getAttribute(LAYOUT_ATTRIBUTE);
		}

		@Override
		public void setObject(SplitState state)
		{
			Session.get().setAttribute(LAYOUT_ATTRIBUTE, state);
		}
	}
}
