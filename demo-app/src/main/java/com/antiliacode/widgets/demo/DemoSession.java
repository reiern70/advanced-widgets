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

import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.lang.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antiliacode.widgets.demo.account.Role;
import com.antiliacode.widgets.demo.account.SignedInUsers;
import com.antiliacode.widgets.demo.modal.TeamStore;
import com.antiliacode.widgets.theme.Theme;
import com.antiliacode.widgets.throttle.Throttle;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The session of the demo application: remembers the {@link Theme} and the {@link AjaxEngine} the
 * user chose for every page, the team of the modal demo and the account the user signed in with.
 * It records nothing about the visitor.
 * <p>
 * The public pages are stateless, so a session is only kept once there is something to keep: a
 * choice or an account; the setters bind it. A visitor who only looks gets none.
 */
public class DemoSession extends WebSession
{
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(DemoSession.class);

	private Theme theme = Theme.DEFAULT;

	private AjaxEngine ajaxEngine = AjaxEngine.DEFAULT;

	private TeamStore team;

	private String account;

	private Role role;

	/** Whether the session holds one of the places of {@link SignedInUsers}. */
	private boolean placeHeld;

	/** Whether the session was signed out because another one took its place, not told yet. */
	private boolean placeLost;

	/**
	 * Constructor.
	 *
	 * @param request
	 *            the request
	 */
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
	 * @return the team of the modal demo, made the first time
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
	 * @return whether the user signed in as the administrator
	 */
	public boolean isAdmin()
	{
		return role == Role.ADMIN;
	}

	/**
	 * Signs the user in, under a new session id so that the old one is of no use, unless
	 * {@value SignedInUsers#MAXIMUM} other sessions are signed in already, none of them for
	 * longer than {@link SignedInUsers#PLACE_TIME} (see {@link SignedInUsers}); the administrator
	 * always signs in.
	 *
	 * @param account
	 *            the name of the account
	 * @param role
	 *            what the account may do
	 * @return whether the user signed in; {@code false} when too many users are
	 */
	public boolean signIn(String account, Role role)
	{
		Args.notEmpty(account, "account");
		Args.notNull(role, "role");
		boolean signedIn = SignedInUsers.get().signIn(role, () -> {
			bind();
			changeSessionId();
			this.account = account;
			this.role = role;
			placeLost = false;
			dirty();
			return getId();
		});
		if (signedIn)
		{
			placeHeld = SignedInUsers.get().holdsPlace(getId());
		}
		return signedIn;
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
	 * Signs the session out if another session took its place.
	 */
	public void checkPlace()
	{
		if (account != null && placeHeld && !SignedInUsers.get().holdsPlace(getId()))
		{
			LOG.info("Another session took the place of session {}: it is signed out", getId());
			signOut();
			placeLost = true;
		}
	}

	/**
	 * Signs the user out, giving the place back.
	 */
	public void signOut()
	{
		if (getId() != null)
		{
			SignedInUsers.get().signedOut(getId());
		}
		account = null;
		role = null;
		placeHeld = false;
		dirty();
		RequestCycle cycle = RequestCycle.get();
		if (cycle != null &&
			cycle.getRequest().getContainerRequest() instanceof HttpServletRequest request)
		{
			Throttle.reset(request);
		}
	}

	/**
	 * @return whether the session was signed out because another one took its place, once: the
	 *         sign-in page tells it, then it is forgotten
	 */
	public boolean tellPlaceLost()
	{
		if (!placeLost)
		{
			return false;
		}
		placeLost = false;
		dirty();
		return true;
	}

	/**
	 * @return what went wrong, in a short line
	 */
	static String firstLine(Throwable e)
	{
		String line = e.getClass().getSimpleName() + ": " +
			String.valueOf(e.getMessage()).lines().findFirst().orElse("");
		return line.length() > 200 ? line.substring(0, 200) + "..." : line;
	}

	/**
	 * Keeps the session: binds it if it is temporary, so that what was set lasts.
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
