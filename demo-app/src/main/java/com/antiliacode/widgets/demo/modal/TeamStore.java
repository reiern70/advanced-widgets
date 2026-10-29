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
package com.antiliacode.widgets.demo.modal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.wicket.Application;
import org.apache.wicket.model.Model;
import org.apache.wicket.resource.loader.PackageStringResourceLoader;

/**
 * The team of the modal dialog example, kept in the session of each visitor: the Wicket page and
 * the React and Svelte apps, through the {@link TeamResource}, add, edit and delete the same
 * members, validated the same way.
 */
public class TeamStore implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** The roles of a member, the keys of their names under {@code role.} in the package bundle. */
	public static final List<String> ROLES = List.of("developer", "designer", "tester", "manager");

	/** The longest name and email a member takes. */
	public static final int MAX_LENGTH = 80;

	/** The most members a team has. */
	public static final int MAX_MEMBERS = 50;

	/** What Wicket's EmailAddressValidator takes for an email address, roughly. */
	private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

	private final List<TeamMember> members = new ArrayList<>();

	private long lastId;

	public TeamStore()
	{
		add("Ada Lovelace", "ada@example.com", "developer");
		add("Grace Hopper", "grace@example.com", "manager");
		add("Alan Turing", "alan@example.com", "tester");
		add("Hedy Lamarr", "hedy@example.com", "designer");
	}

	private void add(String name, String email, String role)
	{
		members.add(new TeamMember(++lastId, name, email, role));
	}

	/**
	 * @return copies of the members, in the order they were added
	 */
	public synchronized List<TeamMember> list()
	{
		return members.stream().map(TeamMember::copy).toList();
	}

	/**
	 * @return a copy of the member of an id, {@code null} if there is none
	 */
	public synchronized TeamMember find(long id)
	{
		return members.stream().filter(member -> member.getId() == id).findFirst()
			.map(TeamMember::copy).orElse(null);
	}

	/**
	 * Adds a new member, or saves the fields of one already there.
	 *
	 * @param member
	 *            the member, new if its id is {@code 0}
	 * @return a copy of the member saved, {@code null} if it was deleted meanwhile
	 * @throws IllegalStateException
	 *             if the member is new and the team has {@value #MAX_MEMBERS} members already
	 */
	public synchronized TeamMember save(TeamMember member)
	{
		if (member.getId() == 0)
		{
			if (members.size() >= MAX_MEMBERS)
			{
				throw new IllegalStateException("The team has " + MAX_MEMBERS + " members");
			}
			TeamMember added = member.copy();
			added.setId(++lastId);
			members.add(added);
			return added.copy();
		}
		for (int i = 0; i < members.size(); i++)
		{
			if (members.get(i).getId() == member.getId())
			{
				members.set(i, member.copy());
				return member.copy();
			}
		}
		return null;
	}

	/**
	 * @return whether there was a member of the id, deleted now
	 */
	public synchronized boolean delete(long id)
	{
		return members.removeIf(member -> member.getId() == id);
	}

	/**
	 * Validates a member as the Wicket form does, with the same messages: the name and the email
	 * required, the email an address, the role one of {@link #ROLES}.
	 *
	 * @return the messages, by the field they are about; none for a valid member
	 */
	public static Map<String, String> validate(TeamMember member, Locale locale)
	{
		Map<String, String> errors = new LinkedHashMap<>();
		for (String field : new String[] { "name", "email" })
		{
			String value = "name".equals(field) ? member.getName() : member.getEmail();
			if (value == null || value.isBlank())
			{
				errors.put(field, wicketMessage("Required", field, locale));
			}
			else if (value.length() > MAX_LENGTH)
			{
				errors.put(field, wicketMessage("StringValidator.maximum", field, locale)
					.replace("${maximum}", String.valueOf(MAX_LENGTH)));
			}
		}
		if (!errors.containsKey("email") && !EMAIL.matcher(member.getEmail().strip()).matches())
		{
			errors.put("email", wicketMessage("EmailAddressValidator", "email", locale));
		}
		if (!ROLES.contains(member.getRole()))
		{
			errors.put("role", wicketMessage("Required", "role", locale));
		}
		return errors;
	}

	/**
	 * @return the message saying that the team has as many members as it takes, in a language
	 */
	public static String full(Locale locale)
	{
		return text("full", locale).replace("{0}", String.valueOf(MAX_MEMBERS));
	}

	/**
	 * @return a message of Wicket's bundles about a field, its label in the language
	 */
	private static String wicketMessage(String key, String field, Locale locale)
	{
		return Application.get().getResourceSettings().getLocalizer().getString(key, null,
			Model.ofMap(Map.of("label", text(field, locale))), locale, null, (String)null);
	}

	/**
	 * @return a text of the bundle of this package, {@code wicket-package}, in a language
	 */
	public static String text(String key, Locale locale)
	{
		String text = new PackageStringResourceLoader().loadStringResource(TeamStore.class, key,
			locale, null, null);
		return text != null ? text : key;
	}
}
