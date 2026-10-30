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
package com.antiliacode.widgets.richtext;

import org.apache.wicket.Application;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.util.lang.Args;

/**
 * What a rich text editor does when the cleaning of the text the browser sent removed something
 * ({@link AbstractRichTextEditor#setCleaning(RichTextCleaning)}). The model gets clean text
 * whatever the mode: the mode decides whether anyone hears of what was removed.
 * <p>
 * What the editor sends was cleaned in the browser already, so the server finds something mostly in
 * a request made by hand, or when the features of the editor in the browser and on the server
 * differ. In {@link #REPORT} and {@link #REJECT} the editor logs it, at WARN, with its path and the
 * names removed.
 * <p>
 * The application has a default, {@link #SILENT} unless {@link #setDefault(Application,
 * RichTextCleaning) set} otherwise:
 *
 * <pre>
 * RichTextCleaning.setDefault(this, RichTextCleaning.REPORT); // in Application#init()
 * </pre>
 *
 * @since 10.11.0
 */
public enum RichTextCleaning
{
	/** The model gets the clean text, and no one hears of what was removed. */
	SILENT,

	/**
	 * The model gets the clean text, and the editor tells of what was removed:
	 * {@link AbstractRichTextEditor#onCleaned(org.apache.wicket.ajax.AjaxRequestTarget,
	 * RichTextCleaningReport)} and a {@link RichTextCleanedEvent} bubbling to the page.
	 */
	REPORT,

	/**
	 * The text is refused when the cleaning would remove anything: the editor gets an error,
	 * {@code RichTextEditor.rejected}, naming what the text holds, and the model is not updated.
	 * Wicket's {@code onError()} runs, as for any validation error, and the editor keeps what the
	 * user typed.
	 */
	REJECT;

	private static final MetaDataKey<RichTextCleaning> DEFAULT = new MetaDataKey<>()
	{
		private static final long serialVersionUID = 1L;
	};

	/**
	 * Sets what the rich text editors of an application do unless set otherwise.
	 *
	 * @param application
	 *            the application
	 * @param cleaning
	 *            the mode; {@code null} for {@link #SILENT}
	 */
	public static void setDefault(Application application, RichTextCleaning cleaning)
	{
		Args.notNull(application, "application").setMetaData(DEFAULT, cleaning);
	}

	/**
	 * @return what the rich text editors of the application do unless set otherwise:
	 *         {@link #SILENT} unless {@link #setDefault(Application, RichTextCleaning) set}, and
	 *         outside an application
	 */
	public static RichTextCleaning getDefault()
	{
		RichTextCleaning cleaning = Application.exists() ?
			Application.get().getMetaData(DEFAULT) : null;
		return cleaning != null ? cleaning : SILENT;
	}
}
