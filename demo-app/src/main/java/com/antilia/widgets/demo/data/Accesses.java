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
package com.antilia.widgets.demo.data;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.wicket.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionOperations;

import com.antilia.widgets.demo.WicketApplication;

/**
 * Who accessed the site: one {@link Access} per session, which only an administrator sees. Every
 * night the accesses older than {@link #KEPT} move to the {@link OldAccess old accesses}, so that
 * the table every session writes to stays small.
 */
public class Accesses
{
	/** How long an access stays among the accesses before it moves to the old accesses. */
	public static final Duration KEPT = Duration.ofDays(10);

	private static final Logger LOG = LoggerFactory.getLogger(Accesses.class);

	private final IAccessRepository accesses;

	private final IOldAccessRepository oldAccesses;

	private final TransactionOperations transactions;

	/**
	 * @param accesses
	 *            the table of the accesses
	 * @param oldAccesses
	 *            the table of the old accesses
	 * @param transactions
	 *            runs the moving of the old accesses in one transaction
	 */
	public Accesses(IAccessRepository accesses, IOldAccessRepository oldAccesses,
		TransactionOperations transactions)
	{
		this.accesses = accesses;
		this.oldAccesses = oldAccesses;
		this.transactions = transactions;
	}

	/**
	 * @return the accesses of the running application
	 */
	public static Accesses get()
	{
		return ((WicketApplication)Application.get()).getAccesses();
	}

	/**
	 * Changes an access, made first if there is none yet.
	 *
	 * @param id
	 *            the id of the access, {@code null} if there is none yet
	 * @param newAccess
	 *            makes the access if there is none by that id
	 * @param change
	 *            the change
	 * @return the access as kept
	 */
	public Access record(Long id, Supplier<Access> newAccess, Consumer<Access> change)
	{
		Access access = (id == null ? null : accesses.findById(id).orElse(null));
		if (access == null)
		{
			access = newAccess.get();
		}
		change.accept(access);
		return accesses.save(access);
	}

	/**
	 * @param filter
	 *            which accesses
	 * @param pageable
	 *            the page and the order
	 * @return a page of the accesses the filter lets through
	 */
	public Page<Access> page(AccessFilter filter, Pageable pageable)
	{
		return accesses.findAll(filter.toSpecification(), pageable);
	}

	/**
	 * @return how many accesses there are
	 */
	public long count()
	{
		return accesses.count();
	}

	/**
	 * @param filter
	 *            which accesses
	 * @return how many accesses the filter lets through
	 */
	public long count(AccessFilter filter)
	{
		return accesses.count(filter.toSpecification());
	}

	/**
	 * @param id
	 *            the id of an access
	 * @return the access, {@code null} if there is none by that id
	 */
	public Access find(Long id)
	{
		return accesses.findById(id).orElse(null);
	}

	/**
	 * @param filter
	 *            which old accesses
	 * @param pageable
	 *            the page and the order
	 * @return a page of the old accesses the filter lets through
	 */
	public Page<OldAccess> pageOld(AccessFilter filter, Pageable pageable)
	{
		return oldAccesses.findAll(filter.toSpecification(), pageable);
	}

	/**
	 * @return how many old accesses there are
	 */
	public long countOld()
	{
		return oldAccesses.count();
	}

	/**
	 * @param filter
	 *            which old accesses
	 * @return how many old accesses the filter lets through
	 */
	public long countOld(AccessFilter filter)
	{
		return oldAccesses.count(filter.toSpecification());
	}

	/**
	 * @param id
	 *            the id of an old access, the one it had as an access
	 * @return the old access, {@code null} if there is none by that id
	 */
	public OldAccess findOld(Long id)
	{
		return oldAccesses.findById(id).orElse(null);
	}

	/**
	 * Moves the accesses older than {@link #KEPT} to the old accesses, every night at half past
	 * three, UTC.
	 */
	@Scheduled(cron = "0 30 3 * * *", zone = "UTC")
	public void archiveOld()
	{
		int moved = archive(Instant.now().minus(KEPT));
		LOG.info("Moved {} accesses older than {} days to the old accesses", moved, KEPT.toDays());
	}

	/**
	 * Moves the accesses before a time to the old accesses, in one transaction: each is copied
	 * with its id, then removed from the accesses only once it is among the old accesses, so that
	 * none is lost.
	 *
	 * @param before
	 *            the time on the server's clock
	 * @return how many accesses were moved
	 */
	public int archive(Instant before)
	{
		return transactions.execute(status -> {
			accesses.copyToOldAccesses(before);
			return accesses.removeCopiedToOldAccesses(before);
		});
	}
}
