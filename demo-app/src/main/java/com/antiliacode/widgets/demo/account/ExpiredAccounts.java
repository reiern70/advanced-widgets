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
package com.antiliacode.widgets.demo.account;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionOperations;

import com.antiliacode.widgets.demo.data.FormerAccount;
import com.antiliacode.widgets.demo.data.IAccessRepository;
import com.antiliacode.widgets.demo.data.IFormerAccountRepository;
import com.antiliacode.widgets.demo.data.IOldAccessRepository;
import com.antiliacode.widgets.demo.data.IUserRepository;
import com.antiliacode.widgets.demo.data.Role;
import com.antiliacode.widgets.demo.data.User;

/**
 * Deletes the accounts that expired, those that visitors registered {@link Accounts#KEPT} ago (see
 * {@link User#getExpires()}), every night at a quarter to four, UTC: each with its accesses, recent
 * and old, after keeping a {@link FormerAccount} of it for the statistics, which tells nothing of
 * who it was. Each account goes in a transaction of its own. Whoever is signed in with an account
 * deleted is signed out at the next request, see {@link Accounts#isStillSignedIn}.
 * <p>
 * Administrators and the accounts the server is started with are never deleted, even with a time
 * to expire.
 */
public class ExpiredAccounts
{
	private static final Logger LOG = LoggerFactory.getLogger(ExpiredAccounts.class);

	private final IUserRepository users;

	private final IAccessRepository accesses;

	private final IOldAccessRepository oldAccesses;

	private final IFormerAccountRepository formerAccounts;

	private final TransactionOperations transactions;

	private final Clock clock;

	/**
	 * @param users
	 *            the table of the accounts
	 * @param accesses
	 *            the table of the accesses
	 * @param oldAccesses
	 *            the table of the old accesses
	 * @param formerAccounts
	 *            the table of what is kept of the accounts deleted
	 * @param transactions
	 *            runs the deleting of each account in one transaction
	 * @param clock
	 *            the clock telling which accounts expired
	 */
	public ExpiredAccounts(IUserRepository users, IAccessRepository accesses,
		IOldAccessRepository oldAccesses, IFormerAccountRepository formerAccounts,
		TransactionOperations transactions, Clock clock)
	{
		this.users = users;
		this.accesses = accesses;
		this.oldAccesses = oldAccesses;
		this.formerAccounts = formerAccounts;
		this.transactions = transactions;
		this.clock = clock;
	}

	/**
	 * Deletes the accounts that expired, every night at a quarter to four, UTC, once the accesses
	 * of the day before moved to the old accesses.
	 */
	@Scheduled(cron = "0 45 3 * * *", zone = "UTC")
	public void deleteExpired()
	{
		int deleted = delete(clock.instant());
		LOG.info("Deleted {} accounts that expired, with their accesses", deleted);
	}

	/**
	 * Deletes the accounts that expired before a time, with their accesses, each in a transaction
	 * of its own, after keeping a {@link FormerAccount} of it.
	 *
	 * @param now
	 *            the time on the server's clock
	 * @return how many accounts were deleted
	 */
	public int delete(Instant now)
	{
		LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
		int deleted = 0;
		for (User user : users.findByExpiresBefore(now))
		{
			if (user.getRole() != Role.USER || user.isConfigured())
			{
				LOG.warn("Account {} expired, but is kept: administrators and the accounts the " +
					"server is started with are never deleted", user.getId());
				continue;
			}
			transactions.executeWithoutResult(status -> {
				String name = user.getName();
				long all = accesses.countByUserName(name) + oldAccesses.countByUserName(name);
				long demos = accesses.countByUserNameAndPrivatePartTrue(name) +
					oldAccesses.countByUserNameAndPrivatePartTrue(name);
				formerAccounts.save(new FormerAccount(user, today, Math.toIntExact(all),
					Math.toIntExact(demos)));
				accesses.removeByUserName(name);
				oldAccesses.removeByUserName(name);
				users.deleteById(user.getId());
			});
			deleted++;
		}
		if (deleted > 0)
		{
			Accounts.changed();
		}
		return deleted;
	}
}
