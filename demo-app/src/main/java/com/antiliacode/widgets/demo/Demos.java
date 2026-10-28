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
package com.antiliacode.widgets.demo;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import org.apache.wicket.Page;

import com.antiliacode.widgets.demo.autocomplete.AutocompletePage;
import com.antiliacode.widgets.demo.button.ButtonsPage;
import com.antiliacode.widgets.demo.ckeditor.CKEditorPage;
import com.antiliacode.widgets.demo.codeeditor.CodeEditorPage;
import com.antiliacode.widgets.demo.contacts.ContactsPage;
import com.antiliacode.widgets.demo.datatable.BeanDynamicDataTablePage;
import com.antiliacode.widgets.demo.datatable.DynamicDataTablePage;
import com.antiliacode.widgets.demo.datatable.MasterDetailPage;
import com.antiliacode.widgets.demo.datepicker.DatePickerPage;
import com.antiliacode.widgets.demo.desktop.DesktopPage;
import com.antiliacode.widgets.demo.download.DownloadsPage;
import com.antiliacode.widgets.demo.dropdown.DropdownPage;
import com.antiliacode.widgets.demo.list.JsonListPage;
import com.antiliacode.widgets.demo.menu.MenusPage;
import com.antiliacode.widgets.demo.looks.LooksPage;
import com.antiliacode.widgets.demo.modal.ModalDialogPage;
import com.antiliacode.widgets.demo.notification.NotificationsPage;
import com.antiliacode.widgets.demo.palette.PalettePage;
import com.antiliacode.widgets.demo.popover.PopoversPage;
import com.antiliacode.widgets.demo.progress.ProgressBarPage;
import com.antiliacode.widgets.demo.resizable.ResizablePage;
import com.antiliacode.widgets.demo.richtext.RichTextEditorPage;
import com.antiliacode.widgets.demo.scrollbar.ScrollbarPage;
import com.antiliacode.widgets.demo.split.SplitPanelPage;
import com.antiliacode.widgets.demo.tabs.TabbedPanelPage;
import com.antiliacode.widgets.demo.theme.ThemeEditorPage;
import com.antiliacode.widgets.demo.tree.TreePage;
import com.antiliacode.widgets.demo.tree.TreeGridPage;
import com.antiliacode.widgets.demo.upload.UploadPage;

/**
 * The demo pages, in the order the home page lists them and the previous and next links of a demo
 * follow.
 */
public final class Demos
{
	/**
	 * A demo page. Its texts are in the bundle of {@link BasePage}: the name under
	 * {@code demo.<id>}, what it shows under {@code demo.<id>.description} and its category under
	 * {@code category.<category>}.
	 *
	 * @param id
	 *            the id of the demo in the keys of its texts
	 * @param category
	 *            the category shown on the home page, a key under {@code category.}
	 * @param page
	 *            the page of the demo
	 * @param frameworkApps
	 *            whether the demo shows the component in React and Svelte too, not only in Wicket
	 */
	public record Demo(String id, String category, Class<? extends Page> page,
		boolean frameworkApps) implements Serializable
	{
		/**
		 * @return the path of the screenshot of the demo, in the web application
		 */
		public String imagePath()
		{
			return "images/demos/" + id + ".png";
		}

		/**
		 * @return the key of the name shown on the home page and in the links of the demos
		 */
		public String nameKey()
		{
			return "demo." + id;
		}

		/**
		 * @return the key of what the demo shows
		 */
		public String descriptionKey()
		{
			return "demo." + id + ".description";
		}

		/**
		 * @return the key of the category
		 */
		public String categoryKey()
		{
			return "category." + category;
		}
	}

	private static final List<Demo> ALL = List.of(
		new Demo("contacts", "examples", ContactsPage.class, false),
		new Demo("jsonTable", "data", DynamicDataTablePage.class, true),
		new Demo("beanTable", "data", BeanDynamicDataTablePage.class, false),
		new Demo("jsonList", "data", JsonListPage.class, true),
		new Demo("tree", "data", TreePage.class, true),
		new Demo("treeGrid", "data", TreeGridPage.class, true),
		new Demo("masterDetail", "data", MasterDetailPage.class, false),
		new Demo("progressBar", "feedback", ProgressBarPage.class, true),
		new Demo("notifications", "feedback", NotificationsPage.class, true),
		new Demo("popovers", "feedback", PopoversPage.class, true),
		new Demo("dropdown", "forms", DropdownPage.class, true),
		new Demo("autocomplete", "forms", AutocompletePage.class, true),
		new Demo("palette", "forms", PalettePage.class, true),
		new Demo("datePicker", "forms", DatePickerPage.class, true),
		new Demo("buttons", "forms", ButtonsPage.class, true),
		new Demo("downloads", "forms", DownloadsPage.class, false),
		new Demo("upload", "forms", UploadPage.class, false),
		new Demo("codeEditor", "forms", CodeEditorPage.class, true),
		new Demo("richTextEditor", "forms", RichTextEditorPage.class, true),
		new Demo("ckEditor", "forms", CKEditorPage.class, true),
		new Demo("scrollbar", "layout", ScrollbarPage.class, true),
		new Demo("splitPanel", "layout", SplitPanelPage.class, true),
		new Demo("resizable", "layout", ResizablePage.class, true),
		new Demo("tabs", "layout", TabbedPanelPage.class, true),
		new Demo("menus", "layout", MenusPage.class, true),
		new Demo("modalDialog", "layout", ModalDialogPage.class, true),
		new Demo("desktop", "layout", DesktopPage.class, true),
		new Demo("themeEditor", "themes", ThemeEditorPage.class, false),
		new Demo("looks", "themes", LooksPage.class, false));

	private Demos()
	{
	}

	/**
	 * @return all demos, in order
	 */
	public static List<Demo> all()
	{
		return ALL;
	}

	/**
	 * @return the demo of the page, if it is one
	 */
	public static Optional<Demo> of(Class<? extends Page> page)
	{
		int index = indexOf(page);
		return index >= 0 ? Optional.of(ALL.get(index)) : Optional.empty();
	}

	/**
	 * @return the demo before the one of the page, if there is one
	 */
	public static Optional<Demo> previous(Class<? extends Page> page)
	{
		int index = indexOf(page);
		return index > 0 ? Optional.of(ALL.get(index - 1)) : Optional.empty();
	}

	/**
	 * @return the demo after the one of the page, if there is one
	 */
	public static Optional<Demo> next(Class<? extends Page> page)
	{
		int index = indexOf(page);
		return index >= 0 && index < ALL.size() - 1 ? Optional.of(ALL.get(index + 1))
			: Optional.empty();
	}

	private static int indexOf(Class<? extends Page> page)
	{
		for (int i = 0; i < ALL.size(); i++)
		{
			if (ALL.get(i).page().isAssignableFrom(page))
			{
				return i;
			}
		}
		return -1;
	}
}
