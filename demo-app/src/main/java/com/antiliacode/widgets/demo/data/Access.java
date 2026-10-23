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
package com.antiliacode.widgets.demo.data;

import java.time.Instant;
import java.util.regex.Pattern;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A recent session on the site: who it was, from where, when on the server's and on the visitor's
 * clock, and whether it opened the private part, the demos. One per session, until it becomes an
 * {@link OldAccess}.
 */
@Entity
@Table(name = "accesses")
public class Access extends AbstractAccess
{
	private static final long serialVersionUID = 1L;

	/** The user of a session in which nobody signed in. */
	public static final String UNKNOWN = "unknown";

	/** The user of a session of a robot, such as a search engine's, in which nobody signed in. */
	public static final String ROBOT = "robot";

	/** What the browser of a robot tells, such as Googlebot/2.1, Applebot or HeadlessChrome. */
	private static final Pattern ROBOT_AGENT = Pattern.compile(
		"bot/|bot;|crawl|spider|slurp|headless|facebookexternalhit|preview|curl/|wget/|python|" +
			"java/|go-http-client|okhttp|scrapy|lighthouse|pingdom|uptime",
		Pattern.CASE_INSENSITIVE);

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	protected Access()
	{
	}

	/**
	 * @param sessionId
	 *            the id of the session
	 * @param address
	 *            the address the visitor came from
	 * @param userAgent
	 *            the visitor's browser
	 * @param accessed
	 *            when, on the server's clock
	 */
	public Access(String sessionId, String address, String userAgent, Instant accessed)
	{
		super(sessionId, address, userAgent, accessed);
	}

	/**
	 * @param userAgent
	 *            what the browser tells it is, may be {@code null}
	 * @return whether the browser is a robot's
	 */
	public static boolean isRobot(String userAgent)
	{
		return userAgent != null && ROBOT_AGENT.matcher(userAgent).find();
	}

	@Override
	public Long getId()
	{
		return id;
	}
}
