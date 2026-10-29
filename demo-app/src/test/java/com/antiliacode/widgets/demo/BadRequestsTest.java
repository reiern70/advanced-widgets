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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.string.StringValueConversionException;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.openjson.JSONException;

import tools.jackson.databind.json.JsonMapper;

/**
 * A request whose parameters or body cannot be read is answered 400, not 500, see
 * {@link BadRequests}.
 */
class BadRequestsTest
{
	private WicketTester tester;

	@BeforeEach
	void start()
	{
		TestDatabase.clear();
		tester = new WicketTester(new WicketApplication()
		{
			@Override
			public RuntimeConfigurationType getConfigurationType()
			{
				return RuntimeConfigurationType.DEPLOYMENT;
			}
		});
		tester.startPage(HomePage.class);
		DemoSession.get().signIn("wicket");
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	private int status(String method, String path)
	{
		tester.getRequest().setMethod(method);
		tester.executeUrl(path);
		return tester.getLastResponse().getStatus();
	}

	@Test
	void aSortTheUsersDoNotHave()
	{
		assertEquals(400, status("GET", "api/users?sort=password"));
		assertEquals(200, status("GET", "api/users?sort="), "not sorted");
		assertEquals(200, status("GET", "api/users?sort=bornDate&ascending=false"));
		assertEquals(200, status("GET", "api/users?first=25&count=25&sort=lastName"));
	}

	@Test
	void aParameterThatIsNotANumberTakesItsDefault()
	{
		assertEquals(200, status("GET", "api/users?first=abc&count=1e9x&ascending=maybe"));
		assertEquals(404, status("DELETE", "api/team?id=abc"));
		assertEquals(200, status("GET", "files/countries.csv?open=maybe"));
	}

	@Test
	void tellsTheCausesOfARequestThatIsWrong()
	{
		StringValueConversionException conversion = assertThrowsConversion();
		assertSame(conversion, BadRequests.cause(new WicketRuntimeException("wrapped",
			conversion)));
		JSONException json = new JSONException("not JSON");
		assertSame(json, BadRequests.cause(json));
		Exception jackson = assertThrowsJackson("{\"a\":" + "[".repeat(2000) + "]".repeat(2000) +
			"}");
		assertSame(jackson, BadRequests.cause(jackson), jackson.toString());

		assertNull(BadRequests.cause(new IllegalStateException("the server's own")));
		assertNull(BadRequests.cause(new RuntimeException(new NullPointerException())));
	}

	private static StringValueConversionException assertThrowsConversion()
	{
		try
		{
			StringValue.valueOf("abc").toLong();
		}
		catch (StringValueConversionException e)
		{
			return e;
		}
		throw new AssertionError("abc is a number");
	}

	private static Exception assertThrowsJackson(String json)
	{
		try
		{
			JsonMapper.builder().build().readTree(json);
		}
		catch (RuntimeException e)
		{
			return e;
		}
		throw new AssertionError("read " + json);
	}
}
