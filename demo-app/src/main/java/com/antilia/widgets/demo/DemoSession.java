/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import java.time.Instant;
import java.util.function.Consumer;

import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.lang.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antilia.widgets.demo.data.Access;
import com.antilia.widgets.demo.data.Accesses;
import com.antilia.widgets.demo.data.Role;
import com.antilia.widgets.demo.modal.TeamStore;
import com.antilia.widgets.theme.Theme;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The session of the demo application: remembers the {@link Theme} and the
 * {@link AjaxEngine} the user chose for every page, the account the user signed in with, the failed attempts to sign in
 * and the {@link Access} recording the session.
 * <p>
 * The home page is stateless, so a session is only kept once there is something to keep: a
 * choice, an account or an access; the setters bind it.
 */
public class DemoSession extends WebSession
{
	private static final long serialVersionUID = 1L;

	private Theme theme = Theme.DEFAULT;

	private AjaxEngine ajaxEngine = AjaxEngine.DEFAULT;

	private TeamStore team;

	private static final Logger LOG = LoggerFactory.getLogger(DemoSession.class);

	private String account;

	private Role role;

	private int failedSignIns;

	/** The id of the {@link Access} recording this session, {@code null} until there is one. */
	private Long accessId;

	/** Whether the access knows the visitor's clock. */
	private boolean localTimeRecorded;

	/** Whether the access knows that the session opened the private part. */
	private boolean privatePartRecorded;

	public DemoSession(Request request)
	{
		super(request);
	}

	/**
	 * @return the session of the current request
	 */
	public static DemoSession get()
	{
		return (DemoSession)Session.get();
	}

	/**
	 * @return the theme of every page
	 */
	public Theme getTheme()
	{
		return theme;
	}

	/**
	 * @param theme
	 *            the theme of every page
	 */
	public void setTheme(Theme theme)
	{
		this.theme = Args.notNull(theme, "theme");
		keep();
	}

	/**
	 * @return the script Wicket's Ajax runs on in the pages
	 */
	public AjaxEngine getAjaxEngine()
	{
		return ajaxEngine;
	}

	/**
	 * @param ajaxEngine
	 *            the script Wicket's Ajax runs on in the pages
	 */
	public void setAjaxEngine(AjaxEngine ajaxEngine)
	{
		this.ajaxEngine = Args.notNull(ajaxEngine, "ajaxEngine");
		keep();
	}

	/**
	 * @return the team of the modal dialog example, which this visitor edits in Wicket, React and
	 *         Svelte; marks the session changed, as the team may change
	 */
	public synchronized TeamStore getTeam()
	{
		if (team == null)
		{
			team = new TeamStore();
		}
		keep();
		return team;
	}

	/**
	 * @return the name of the account the user signed in with, {@code null} if not signed in
	 */
	public String getAccount()
	{
		return account;
	}

	/**
	 * @return whether the user signed in
	 */
	public boolean isSignedIn()
	{
		return account != null;
	}

	/**
	 * @return whether the user signed in with an account that sees the accesses
	 */
	public boolean isAdmin()
	{
		return role == Role.ADMIN;
	}

	/**
	 * Signs the user in with an account that opens the demos.
	 *
	 * @param account
	 *            the name of the account
	 * @see #signIn(String, Role)
	 */
	public void signIn(String account)
	{
		signIn(account, Role.USER);
	}

	/**
	 * Signs the user in, under a new session id so that the old one is of no use; the demo the
	 * user was going to stays in the session. The access of the session gets the account.
	 *
	 * @param account
	 *            the name of the account
	 * @param role
	 *            what the account may do
	 */
	public void signIn(String account, Role role)
	{
		bind();
		changeSessionId();
		this.account = Args.notEmpty(account, "account");
		this.role = Args.notNull(role, "role");
		failedSignIns = 0;
		dirty();
		recordAccess(access -> {
			access.setSessionId(getId());
			access.setUserName(account);
		});
	}

	/**
	 * Signs a new session in without asking anything, on a development machine (see
	 * {@link WicketApplication#SIGN_IN_AS_PROPERTY}); unlike {@link #signIn(String, Role)} it keeps
	 * the session temporary, so that stateless pages stay so.
	 *
	 * @param account
	 *            the name of the account
	 * @param role
	 *            what the account may do
	 */
	void signedInLocally(String account, Role role)
	{
		this.account = Args.notEmpty(account, "account");
		this.role = Args.notNull(role, "role");
	}

	/**
	 * Signs the user out, keeping the theme and the other choices.
	 */
	public void signOut()
	{
		account = null;
		role = null;
		dirty();
	}

	/**
	 * @return how often the user failed to sign in since signing in last
	 */
	public int getFailedSignIns()
	{
		return failedSignIns;
	}

	/**
	 * Counts a failed attempt to sign in.
	 */
	public void failedSignIn()
	{
		failedSignIns++;
		keep();
	}

	/**
	 * @return whether the access recording the session lacks the visitor's clock, which the
	 *         browser sends once
	 */
	public boolean needsLocalTime()
	{
		return !localTimeRecorded;
	}

	/**
	 * Records the visitor's clock in the access of the session.
	 *
	 * @param change
	 *            sets the time zone and the local time
	 */
	public void recordLocalTime(Consumer<Access> change)
	{
		localTimeRecorded = true;
		recordAccess(change);
	}

	/**
	 * Records in the access of the session that it opened the private part, the demos; once.
	 */
	public void recordPrivatePart()
	{
		if (!privatePartRecorded)
		{
			privatePartRecorded = true;
			recordAccess(access -> access.setPrivatePart(true));
		}
	}

	/**
	 * Changes the access recording this session, made first if there is none. A failing database
	 * is logged and does not fail the request.
	 *
	 * @param change
	 *            the change
	 */
	public synchronized void recordAccess(Consumer<Access> change)
	{
		keep();
		try
		{
			Access access = Accesses.get().record(accessId, this::newAccess, change);
			accessId = access.getId();
		}
		catch (RuntimeException e)
		{
			LOG.warn("Cannot record the access of session {}", getId(), e);
		}
	}

	/**
	 * @return the id of the access recording this session, {@code null} until there is one
	 */
	public Long getAccessId()
	{
		return accessId;
	}

	private Access newAccess()
	{
		String address = null;
		String userAgent = null;
		RequestCycle cycle = RequestCycle.get();
		if (cycle != null && cycle.getRequest().getContainerRequest() instanceof HttpServletRequest request)
		{
			address = request.getRemoteAddr();
			userAgent = request.getHeader("User-Agent");
		}
		Access access = new Access(getId(), address, userAgent, Instant.now());
		if (account != null)
		{
			access.setUserName(account);
		}
		return access;
	}

	/**
	 * Keeps the session: binds it, so that it outlives a stateless page, and marks it changed.
	 */
	private void keep()
	{
		if (isTemporary())
		{
			bind();
		}
		dirty();
	}
}
