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
package com.antiliacode.widgets.wizard;

/**
 * Where a {@link Wizard} shows the trail of the steps visited, a
 * {@link com.antiliacode.widgets.breadcrumb.Breadcrumb Breadcrumb} whose crumbs go back to them.
 *
 * @since 10.11.0
 */
public enum WizardBreadcrumbPosition
{
	/** No trail. */
	NONE,

	/** Above the title of the step, the trail ending with the step shown; the default. */
	ABOVE_TITLE,

	/**
	 * In place of the title, as large: the steps visited, then the step shown. The heading stays
	 * for screen readers, hidden.
	 */
	IN_TITLE,

	/** Below the title of the step, the trail ending with the step shown. */
	BELOW_TITLE
}
