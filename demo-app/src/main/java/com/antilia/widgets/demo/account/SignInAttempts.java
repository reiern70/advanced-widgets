/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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
package com.antilia.widgets.demo.account;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.wicket.util.string.Strings;

/**
 * The failed attempts to sign in, by account, so that guessing a password asks for a captcha
 * after {@value #BEFORE_CAPTCHA} failures, even from sessions that start anew each time. Kept in
 * memory: a restart forgets them.
 */
public class SignInAttempts
{
	/** The failed attempts before a captcha is asked. */
	public static final int BEFORE_CAPTCHA = 3;

	/** The most accounts remembered; a robot trying names cannot fill the memory. */
	private static final int MAXIMUM_ACCOUNTS = 10_000;

	private final Map<String, Integer> failures = new ConcurrentHashMap<>();

	/**
	 * Counts a failed attempt to sign in to an account.
	 *
	 * @param name
	 *            the account as typed
	 */
	public void failed(String name)
	{
		if (Strings.isEmpty(name))
		{
			return;
		}
		if (failures.size() >= MAXIMUM_ACCOUNTS)
		{
			failures.clear();
		}
		failures.merge(Accounts.accountName(name), 1, Integer::sum);
	}

	/**
	 * Forgets the failures of an account, which was signed in to.
	 *
	 * @param name
	 *            the account
	 */
	public void succeeded(String name)
	{
		if (!Strings.isEmpty(name))
		{
			failures.remove(Accounts.accountName(name));
		}
	}

	/**
	 * @param name
	 *            the account as typed
	 * @return whether signing in to the account asks for a captcha
	 */
	public boolean needsCaptcha(String name)
	{
		return !Strings.isEmpty(name) &&
			failures.getOrDefault(Accounts.accountName(name), 0) >= BEFORE_CAPTCHA;
	}
}
