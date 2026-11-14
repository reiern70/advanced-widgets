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
package com.antiliacode.widgets.ckeditor;

import java.time.Duration;

import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.lang.Args;

/**
 * How {@link CKEditor} starts CKEditor again in the browser after an error escaped it, as
 * CKEditor's Watchdog did before CKEditor 49 removed it: with the text it last had, which its text
 * area holds, and the selection, saying so in a line below the text. After each of the first
 * {@code crashes}, however close they come; after more, only while the last of them came further
 * apart than {@code interval} on average. Errors coming more often, the editor gives up and leaves
 * CKEditor running as it is, telling the user to save and reload.
 *
 * @param crashes
 *            how many crashes in a row CKEditor starts again after, however close they come, the
 *            Watchdog's {@code crashNumberLimit}; 0 to give up at once
 * @param interval
 *            how far apart the crashes after those come at least on average for CKEditor to start
 *            again, the Watchdog's {@code minimumNonErrorTimePeriod}
 * @since 11.0.0
 */
public record CKEditorRecovery(int crashes, Duration interval) implements IClusterable
{
	/** As the Watchdog did by default: after 3 crashes, then while they come 5 s apart on average. */
	public static final CKEditorRecovery DEFAULT = new CKEditorRecovery(3, Duration.ofSeconds(5));

	/**
	 * @throws IllegalArgumentException
	 *             for crashes below 0, or an interval {@code null} or below zero
	 */
	public CKEditorRecovery
	{
		Args.withinRange(0, Integer.MAX_VALUE, crashes, "crashes");
		Args.notNull(interval, "interval");
		if (interval.isNegative())
		{
			throw new IllegalArgumentException("The interval is negative: " + interval);
		}
	}
}
