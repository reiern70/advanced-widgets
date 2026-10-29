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
package com.antiliacode.widgets.demo.datatable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.junit.jupiter.api.Test;

/**
 * The users of the demos, at most {@link UsersDatabase#MAX_USERS}, in each order sorted once
 * until one changes.
 */
class UsersDatabaseTest
{
	private static User user(String firstName, String lastName)
	{
		User user = new User();
		user.setFirstName(firstName);
		user.setLastName(lastName);
		return user;
	}

	@Test
	void sortsOnceUntilAUserChanges()
	{
		UsersDatabase database = new UsersDatabase(0);
		User ada = user("Ada", "Lovelace");
		User grace = user("Grace", "Hopper");
		database.save(grace);
		database.save(ada);
		SortParam<String> byLastName = new SortParam<>("lastName", true);

		List<User> index = database.getIndex(byLastName);
		assertEquals(List.of(grace, ada), index);
		assertSame(index, database.getIndex(byLastName), "sorted once");
		assertEquals(List.of(ada, grace), database.getIndex(null));
		assertEquals(List.of(ada, grace), database.getIndex(new SortParam<>("lastName", false)));

		ada.setLastName("Byron");
		database.update(ada);
		assertEquals(List.of(ada, grace), database.getIndex(byLastName));

		User alan = user("Alan", "Turing");
		database.save(alan);
		assertEquals(List.of(ada, alan, grace), database.getIndex(null));
		database.delete(ada);
		assertEquals(List.of(alan, grace), database.getIndex(null));
		assertEquals(List.of(alan), database.find(0, 1, null));
	}

	@Test
	void keepsAtMostTheMaximumOfUsers()
	{
		UsersDatabase database = new UsersDatabase(UsersDatabase.MAX_USERS - 1);
		assertFalse(database.isFull());
		database.save(user("Ada", "Lovelace"));
		assertTrue(database.isFull());

		assertThrows(IllegalStateException.class, () -> database.save(user("Grace", "Hopper")));
		assertEquals(UsersDatabase.MAX_USERS, database.getCount());

		database.delete(database.get(1));
		assertFalse(database.isFull(), "a user deleted makes room");
	}
}
