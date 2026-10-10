/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.data;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.wicket.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.antilia.widgets.demo.WicketApplication;

/**
 * Who accessed the site: one {@link Access} per session, which only an administrator sees.
 */
public class Accesses
{
	private final IAccessRepository accesses;

	/**
	 * @param accesses
	 *            the table of the accesses
	 */
	public Accesses(IAccessRepository accesses)
	{
		this.accesses = accesses;
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
	 * @param pageable
	 *            the page and the order
	 * @return a page of the accesses
	 */
	public Page<Access> page(Pageable pageable)
	{
		return accesses.findAll(pageable);
	}

	/**
	 * @return how many accesses there are
	 */
	public long count()
	{
		return accesses.count();
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
}
