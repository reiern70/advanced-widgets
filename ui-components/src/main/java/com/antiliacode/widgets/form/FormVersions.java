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
package com.antiliacode.widgets.form;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.wicket.MetaDataKey;
import org.apache.wicket.Session;

/**
 * The versions of the forms of a session, by the key of each form's {@link FormVersionBehavior}.
 * <p>
 * They are kept in the session and not in the page, because Wicket keeps the older versions of a
 * page: a submit that changes the page gives it a new version, and a second normal submit of the
 * same form reaches the copy stored before the first, whose own counter would still match. The key
 * of a form is the same in every stored version of its page, and the session's version is the one
 * that counts.
 * <p>
 * Only the forms used last are kept, {@value #MAX_FORMS} at most; a form forgotten starts again at
 * version 0, and its next submit, carrying a higher version, is refused once.
 */
final class FormVersions implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** How many forms a session keeps the versions of: those used last. */
	static final int MAX_FORMS = 500;

	private static final MetaDataKey<FormVersions> KEY = new MetaDataKey<>()
	{
		private static final long serialVersionUID = 1L;
	};

	/** The versions by the forms' keys, the one used last at the end. */
	private final Map<String, Integer> versions = new LinkedHashMap<>(16, 0.75f, true)
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest)
		{
			return size() > MAX_FORMS;
		}
	};

	/**
	 * @param session
	 *            a session
	 * @return the versions of its forms, null while none was raised
	 */
	static FormVersions of(Session session)
	{
		return session.getMetaData(KEY);
	}

	/**
	 * @param key
	 *            the key of a form
	 * @return its version: 0 until a submit of it was taken
	 */
	synchronized int current(String key)
	{
		Integer version = versions.get(key);
		return version == null ? 0 : version;
	}

	/**
	 * Takes a submit of a form: refuses it when it carries another version than the form's, raises
	 * the form's version otherwise.
	 *
	 * @param session
	 *            the session of the form, bound
	 * @param key
	 *            the key of the form
	 * @param sent
	 *            the version the submit carries
	 * @return the form's new version, or -1 when the submit carried another version
	 */
	static int take(Session session, String key, String sent)
	{
		FormVersions forms;
		synchronized (session)
		{
			forms = of(session);
			if (forms == null)
			{
				forms = new FormVersions();
				session.setMetaData(KEY, forms);
			}
		}
		int next;
		synchronized (forms)
		{
			int current = forms.current(key);
			if (!Integer.toString(current).equals(sent))
			{
				return -1;
			}
			next = current + 1;
			forms.versions.put(key, next);
		}
		// marks the session dirty, so that a cluster replicates it
		session.setMetaData(KEY, forms);
		return next;
	}
}
