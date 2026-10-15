/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.data;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

import com.antilia.widgets.dynamicdatatable.IIdentifiable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A session on the site: who it was, from where, when on the server's and on the visitor's clock,
 * and whether it opened the private part, the demos. One per session.
 */
@Entity
@Table(name = "accesses")
public class Access implements Serializable, IIdentifiable<Long>
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

	@Column(name = "session_id", length = 100)
	private String sessionId;

	@Column(name = "user_name", nullable = false, length = 254)
	private String userName = UNKNOWN;

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
		this.sessionId = sessionId;
		this.address = address;
		this.userAgent = userAgent;
		this.accessed = accessed;
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
