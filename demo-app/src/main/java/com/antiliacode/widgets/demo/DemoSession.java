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

import java.time.Instant;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.lang.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antiliacode.widgets.demo.account.SignedInUsers;
import com.antiliacode.widgets.demo.data.Access;
import com.antiliacode.widgets.demo.data.Accesses;
import com.antiliacode.widgets.demo.data.Role;
import com.antiliacode.widgets.demo.modal.TeamStore;
import com.antiliacode.widgets.theme.Theme;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The session of the demo application: remembers the {@link Theme} and the
 * {@link AjaxEngine} the user chose for every page, the account the user signed in with, the failed attempts to sign in,
 * who a visitor who does not sign in told it is (see {@link #VISITOR_PARAMETER}) and the
 * {@link Access} recording the session, whose user is the account, the visitor, a robot or
 * unknown.
 * <p>
 * The home page is stateless, so a session is only kept once there is something to keep: a
 * choice, an account or an access; the setters bind it.
 */
public class DemoSession extends WebSession
{
	private static final long serialVersionUID = 1L;

	/**
	 * The query parameter with which a visitor who does not sign in, such as a script checking
	 * the site, tells who it is: {@code ?visitor=claude-dev} on any page records the access of the
	 * session as {@code user-claude-dev}.
	 */
	public static final String VISITOR_PARAMETER = "visitor";

	/** What a visitor may tell it is: a few letters, digits, dots and dashes. */
	private static final Pattern VISITOR = Pattern.compile("[a-z0-9][a-z0-9.-]{0,39}");

	private Theme theme = Theme.DEFAULT;

	private AjaxEngine ajaxEngine = AjaxEngine.DEFAULT;

	private TeamStore team;

	private static final Logger LOG = LoggerFactory.getLogger(DemoSession.class);

	private String account;

	private Role role;

	private int failedSignIns;

	/** Who the visitor told it is, see {@link #VISITOR_PARAMETER}; {@code null} if it did not. */
	private String visitor;

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
	 * @return whether the user signed in
	 * @see #signIn(String, Role)
	 */
	public boolean signIn(String account)
	{
		return signIn(account, Role.USER);
	}

	/**
	 * Signs the user in, under a new session id so that the old one is of no use, unless
	 * {@value SignedInUsers#MAXIMUM} other users are signed in already (see
	 * {@link SignedInUsers}); the demo the user was going to stays in the session. The access of
	 * the session gets the account.
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
		boolean signedIn = SignedInUsers.get().signIn(account, role, () -> {
			bind();
			changeSessionId();
			this.account = account;
			this.role = role;
			failedSignIns = 0;
			dirty();
			return getId();
		});
		if (signedIn)
		{
			recordAccess(access -> {
				access.setSessionId(getId());
				access.setUserName(account);
			});
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
	 * Signs the user out, keeping the theme and the other choices.
	 */
	public void signOut()
	{
		if (getId() != null)
		{
			SignedInUsers.get().signedOut(getId());
		}
		account = null;
		role = null;
		dirty();
	}

	/**
	 * Records the access of the session as the visitor's, unless somebody signed in: the
	 * visitor tells who it is with {@link #VISITOR_PARAMETER}. A name with other characters than
	 * letters, digits, dots and dashes is ignored.
	 *
	 * @param name
	 *            what the visitor tells it is, such as {@code claude-dev}
	 */
	public void identify(String name)
	{
		if (name == null || !VISITOR.matcher(name).matches() || ("user-" + name).equals(visitor))
		{
			return;
		}
		visitor = "user-" + name;
		recordAccess(access -> {
			if (account == null)
			{
				access.setUserName(visitor);
			}
		});
	}

	/**
	 * @return who the visitor told it is, as the accesses show it, such as
	 *         {@code user-claude-dev}; {@code null} if it did not
	 */
	public String getVisitor()
	{
		return visitor;
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
		else if (visitor != null)
		{
			access.setUserName(visitor);
		}
		else if (Access.isRobot(userAgent))
		{
			access.setUserName(Access.ROBOT);
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
