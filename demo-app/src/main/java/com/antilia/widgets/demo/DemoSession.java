/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.Request;
import org.apache.wicket.util.lang.Args;

import com.antilia.widgets.demo.modal.TeamStore;
import com.antilia.widgets.theme.Theme;

/**
 * The session of the demo application: remembers the {@link Theme} and the {@link AjaxEngine} the
 * user chose for every page, and the account the user signed in with.
 */
public class DemoSession extends WebSession
{
	private static final long serialVersionUID = 1L;

	private Theme theme = Theme.DEFAULT;

	private AjaxEngine ajaxEngine = AjaxEngine.DEFAULT;

	private TeamStore team;

	private String account;

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
		dirty();
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
		dirty();
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
		dirty();
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
	 * Signs the user in, under a new session id so that the old one is of no use; the demo the
	 * user was going to stays in the session.
	 *
	 * @param account
	 *            the name of the account
	 */
	public void signIn(String account)
	{
		changeSessionId();
		this.account = Args.notEmpty(account, "account");
		dirty();
	}

	/**
	 * Signs the user out, keeping the theme and the other choices.
	 */
	public void signOut()
	{
		account = null;
		dirty();
	}
}
