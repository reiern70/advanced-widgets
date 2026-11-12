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
package com.antiliacode.widgets.demo.richtext;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;

import com.antiliacode.widgets.event.AbstractClientEvent;
import com.antiliacode.widgets.richtext.RichTextEditorEvents;

/**
 * An event of the demo's own staying in the browser: as a rich text editor starts, and each time
 * its text changes, it shows how many characters the text has in another element of the page, the
 * counter. No request goes to the server: its handler takes the text from the editor,
 * {@code editor.getText()}, and where and what to write from the properties of the event,
 * {@code options}, which go to the browser by its public getters, {@link #getCounterId()} and
 * {@link #getMessage()}. It takes no {@code send}: it has nothing to send.
 *
 * <pre>
 * editor.addEvent(new CharacterCountEvent(counter, Model.of("{0} characters")));
 * </pre>
 */
public class CharacterCountEvent extends AbstractClientEvent<CharacterCountEvent>
{
	private static final long serialVersionUID = 1L;

	/** What {@link #getMessage()} puts the number of characters in place of. */
	public static final String COUNT = "{0}";

	/**
	 * What the browser runs: writes the number of characters of the text, without the line breaks
	 * between its blocks, into the counter.
	 */
	static final String CLIENT_FUNCTION = "function(event,editor,options){" +
		"var counter=document.getElementById(options.counterId);" +
		"if(counter){counter.textContent=options.message.replace('" + COUNT + "'," +
		"editor.getText().replace(/\\n/g,'').length);}}";

	private final Component counter;

	private final IModel<String> message;

	/**
	 * @param counter
	 *            the component showing the number of characters, which the event gives a markup
	 *            id
	 * @param message
	 *            what it shows, the number in place of {@link #COUNT}, read as the page renders:
	 *            a model keeping nothing, {@code () -> getString("count")} for example
	 */
	public CharacterCountEvent(Component counter, IModel<String> message)
	{
		super(RichTextEditorEvents.TEXT.getEventName(), null, CLIENT_FUNCTION);
		this.counter = Args.notNull(counter, "counter").setOutputMarkupId(true);
		this.message = Args.notNull(message, "message");
	}

	/**
	 * @return the markup id of the counter: {@code options.counterId} in the browser
	 */
	public String getCounterId()
	{
		return counter.getMarkupId();
	}

	/**
	 * @return what the counter shows, the number in place of {@link #COUNT}:
	 *         {@code options.message} in the browser
	 */
	public String getMessage()
	{
		return message.getObject();
	}
}
