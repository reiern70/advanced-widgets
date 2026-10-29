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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.DemoSession;
import com.antiliacode.widgets.demo.WicketApplication;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * The {@link TeamResource} reads at most {@value TeamResource#MAX_BODY} bytes of a request, and a
 * team has at most {@value TeamStore#MAX_MEMBERS} members.
 */
class TeamLimitsTest
{
	private static final String MEMBER = "{\"name\":\"Kim\",\"email\":\"kim@example.com\"," +
		"\"role\":\"tester\"}";

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

	@Test
	void readsABodyUpToTheLimit()
	{
		assertEquals(MEMBER, TeamResource.readBody(request("POST", MEMBER.getBytes(
			StandardCharsets.UTF_8), MEMBER.length(), new AtomicInteger())));
		assertEquals("", TeamResource.readBody(request("GET", new byte[10], 10,
			new AtomicInteger())), "only the requests that add or update read it");
	}

	@Test
	void refusesALongerBodyWithoutReadingIt()
	{
		AtomicInteger read = new AtomicInteger();
		assertNull(TeamResource.readBody(request("PUT", new byte[0], 1024 * 1024 * 1024, read)));
		assertEquals(0, read.get(), "nothing read");
	}

	@Test
	void stopsReadingALongerBodyAtTheLimit()
	{
		AtomicInteger read = new AtomicInteger();
		byte[] body = new byte[10 * TeamResource.MAX_BODY];

		assertNull(TeamResource.readBody(request("POST", body, -1, read)));
		assertTrue(read.get() <= TeamResource.MAX_BODY + 4096, "read " + read.get());
	}

	@Test
	void aTeamHasAtMostTheMaximumOfMembers()
	{
		TeamStore team = new TeamStore();
		while (team.list().size() < TeamStore.MAX_MEMBERS)
		{
			assertEquals(201, TeamResource.answer("POST", 0, MEMBER, team, Locale.ENGLISH)
				.status());
		}

		TeamResource.Answer refused = TeamResource.answer("POST", 0, MEMBER, team,
			Locale.ENGLISH);
		assertEquals(400, refused.status());
		assertEquals("The team has at most 50 members: delete one first.",
			refused.json().getJSONObject("errors").getString("name"));
		assertEquals(TeamStore.MAX_MEMBERS, team.list().size());
		TeamMember first = team.list().get(0);
		first.setName("Ada King");
		assertEquals("Ada King", team.save(first).getName(), "the members still change");
		assertThrows(IllegalStateException.class, () -> team.save(new TeamMember()));
	}

	@Test
	void theFormOfTheWicketPageSaysTheTeamIsFull()
	{
		tester.startPage(ModalDialogPage.class);
		TeamStore team = DemoSession.get().getTeam();
		while (team.list().size() < TeamStore.MAX_MEMBERS)
		{
			team.save(member());
		}

		tester.clickLink("themed:add");
		FormTester form = tester.newFormTester("dialog:overlay:dialog:content:body:form");
		form.setValue("name", "Margaret Hamilton");
		form.setValue("email", "margaret@example.com");
		form.select("role", 0);
		tester.executeAjaxEvent("dialog:overlay:dialog:content:footer:buttons:2", "click");

		tester.assertErrorMessages("The team has at most 50 members: delete one first.");
		assertEquals(TeamStore.MAX_MEMBERS, team.list().size());
	}

	private static TeamMember member()
	{
		TeamMember member = new TeamMember();
		member.setName("Kim");
		member.setEmail("kim@example.com");
		member.setRole("tester");
		return member;
	}

	/**
	 * @return a request with a body, which counts the bytes read of it
	 */
	private HttpServletRequest request(String method, byte[] body, long contentLength,
		AtomicInteger read)
	{
		ByteArrayInputStream in = new ByteArrayInputStream(body);
		ServletInputStream stream = new ServletInputStream()
		{
			@Override
			public int read(byte[] buffer, int offset, int length)
			{
				int count = in.read(buffer, offset, length);
				read.addAndGet(Math.max(count, 0));
				return count;
			}

			@Override
			public int read()
			{
				int b = in.read();
				read.addAndGet(b >= 0 ? 1 : 0);
				return b;
			}

			@Override
			public boolean isFinished()
			{
				return in.available() == 0;
			}

			@Override
			public boolean isReady()
			{
				return true;
			}

			@Override
			public void setReadListener(ReadListener listener)
			{
			}
		};
		return new HttpServletRequestWrapper(tester.getRequest())
		{
			@Override
			public String getMethod()
			{
				return method;
			}

			@Override
			public long getContentLengthLong()
			{
				return contentLength;
			}

			@Override
			public ServletInputStream getInputStream()
			{
				return stream;
			}
		};
	}
}
