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

import com.antiliacode.widgets.demo.account.Accounts;

/**
 * The accounts of the tests: {@value Accounts#USER} with the password of the source, and the
 * administrator with the password the build gives it (see the surefire configuration of the pom).
 */
public final class TestAccounts
{
	/** The administrator's password in the tests. */
	public static final String ADMIN_PASSWORD = "admin-test-password";

	private TestAccounts()
	{
	}
}
