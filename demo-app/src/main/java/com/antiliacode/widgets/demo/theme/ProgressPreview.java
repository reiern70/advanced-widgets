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
package com.antiliacode.widgets.demo.theme;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

import com.antiliacode.widgets.progress.ProgressBar;

/**
 * Progress bars of the theme editor's preview: running, finished and of unknown progress.
 */
class ProgressPreview extends Panel
{
	private static final long serialVersionUID = 1L;

	ProgressPreview(String id)
	{
		super(id);
		add(new ProgressBar("running", Model.of(62)));
		add(new ProgressBar("finished", Model.of(100)));
		add(new ProgressBar("unknown", Model.of((Integer)null)));
	}
}
