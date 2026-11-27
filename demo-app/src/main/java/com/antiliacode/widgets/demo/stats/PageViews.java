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
package com.antiliacode.widgets.demo.stats;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.application.IComponentOnAfterRenderListener;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;

import com.antiliacode.widgets.demo.DemoSession;

/**
 * Counts each page shown whole, not its Ajax updates, in the {@link Visits}, and gives the
 * browser its {@link VisitorCookie} with the first one. The pages of the administrator's sessions
 * are not counted.
 */
public class PageViews implements IComponentOnAfterRenderListener
{
	@Override
	public void onAfterRender(Component component)
	{
		if (!(component instanceof Page page))
		{
			return;
		}
		RequestCycle cycle = RequestCycle.get();
		if (cycle == null || cycle.find(IPartialPageRequestHandler.class).isPresent())
		{
			return;
		}
		String visitor = VisitorCookie.read(cycle.getRequest());
		VisitorCookie.give(cycle);
		if (!DemoSession.get().isAdmin())
		{
			Visits.get().viewed(pathOf(cycle, page.getClass()), visitor);
		}
	}

	/**
	 * @return the mount path of the page, {@code /} for the home page, or the name of its class
	 *         when it has none
	 */
	static String pathOf(RequestCycle cycle, Class<? extends Page> page)
	{
		try
		{
			Url url = cycle.mapUrlFor(page, null);
			String path = url.getPath();
			if (!path.startsWith("wicket/"))
			{
				return "/" + path;
			}
		}
		catch (RuntimeException e)
		{
			// none: the name of the class
		}
		return page.getSimpleName();
	}
}
