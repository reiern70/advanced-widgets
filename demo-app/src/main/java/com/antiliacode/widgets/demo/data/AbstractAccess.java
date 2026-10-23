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

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;

import com.antiliacode.widgets.dynamicdatatable.IIdentifiable;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * What is kept of a session on the site: who it was, from where, when on the server's and on the
 * visitor's clock, and whether it opened the private part, the demos. An {@link Access} while it is
 * recent, an {@link OldAccess} once {@link Accesses#archive(Instant) archived}.
 */
@MappedSuperclass
public abstract class AbstractAccess implements Serializable, IIdentifiable<Long>
{
	private static final long serialVersionUID = 1L;

	@Column(name = "session_id", length = 100)
	private String sessionId;

	@Column(name = "user_name", nullable = false, length = 254)
	private String userName = Access.UNKNOWN;

	@Column(name = "private_part", nullable = false)
	private boolean privatePart;

	@Column(length = 100)
	private String address;

	@Column(name = "user_agent", length = 500)
	private String userAgent;

	@Column(nullable = false)
	private Instant accessed;

	@Column(name = "time_zone", length = 100)
	private String timeZone;

	@Column(name = "local_time")
	private LocalDateTime localTime;

	protected AbstractAccess()
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
	protected AbstractAccess(String sessionId, String address, String userAgent, Instant accessed)
	{
		this.sessionId = sessionId;
		this.address = address;
		this.userAgent = userAgent;
		this.accessed = accessed;
	}

	public String getSessionId()
	{
		return sessionId;
	}

	public void setSessionId(String sessionId)
	{
		this.sessionId = sessionId;
	}

	public String getUserName()
	{
		return userName;
	}

	public void setUserName(String userName)
	{
		this.userName = userName;
	}

	public boolean isPrivatePart()
	{
		return privatePart;
	}

	public void setPrivatePart(boolean privatePart)
	{
		this.privatePart = privatePart;
	}

	public String getAddress()
	{
		return address;
	}

	public String getUserAgent()
	{
		return userAgent;
	}

	public Instant getAccessed()
	{
		return accessed;
	}

	public String getTimeZone()
	{
		return timeZone;
	}

	public void setTimeZone(String timeZone)
	{
		this.timeZone = timeZone;
	}

	public LocalDateTime getLocalTime()
	{
		return localTime;
	}

	public void setLocalTime(LocalDateTime localTime)
	{
		this.localTime = localTime;
	}
}
