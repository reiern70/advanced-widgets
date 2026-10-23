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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Page;
import org.apache.wicket.serialize.java.JavaSerializer;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.antiliacode.widgets.demo.Demos.Demo;
import com.antiliacode.widgets.demo.account.SignInPage;
import com.antiliacode.widgets.demo.admin.AdminPage;
import com.antiliacode.widgets.demo.data.Role;
import com.antiliacode.widgets.demo.register.RegisterPage;

/**
 * Every page survives the page store: Wicket serializes the pages of a session and reads them
 * back when the user goes back to one, or uses one in another tab, after it left the session's
 * cache.
 */
class PageStoreTest
{
	private WicketTester tester;

	@BeforeEach
	void start()
	{
		tester = new WicketTester(new WicketApplication());
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	static List<Class<? extends Page>> pages()
	{
		List<Class<? extends Page>> pages = new ArrayList<>();
		pages.add(HomePage.class);
		Demos.all().stream().map(Demo::page).forEach(pages::add);
		pages.add(SignInPage.class);
		pages.add(RegisterPage.class);
		pages.add(AdminPage.class);
		return pages;
	}

	@ParameterizedTest
	@MethodSource("pages")
	void isReadBackFromThePageStore(Class<? extends Page> pageClass)
	{
		DemoSession.get().signIn("admin", Role.ADMIN);
		Page page = tester.startPage(pageClass);
		tester.assertRenderedPage(pageClass);

		JavaSerializer serializer = new JavaSerializer(tester.getApplication().getApplicationKey());
		byte[] data = serializer.serialize(page);
		assertNotNull(data, pageClass.getSimpleName() + " could not be serialized");
		Object read = serializer.deserialize(data);

		assertEquals(pageClass, read.getClass());
		assertEquals(page.getPageId(), ((Page)read).getPageId());
	}
}
